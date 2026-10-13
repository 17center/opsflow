#!/usr/bin/env bash
# ============================================================
# OpsFlow 构建产物获取脚本（在 136 自托管 runner 上以 runner 用户运行，无需 root）
#
# 用法: GH_TOKEN=<token> RUN_NUMBER=<n> ./fetch-release.sh
#
# 为什么不用 actions/upload-artifact + download-artifact：
#   实测 136 从 artifact 的 Azure Blob 存储下载只有约 47 KB/s，
#   117MB 的 jar 需约 41 分钟，完全不可用。
#   而同一台机器经 gh-proxy 镜像拉取 GitHub Release 资源可达约 10 MB/s，
#   120MB 约 12 秒。因此改用 Release 资源分发。
#
# 完整性保障（重要）：
#   镜像 gh-proxy.com 是第三方，理论上可篡改内容。
#   因此本脚本先从 api.github.com（可信、直连可达）读取官方 sha256 digest，
#   下载后强制校验，不匹配即失败退出。
#
# 镜像不可用时会自动回退到直连 GitHub。
# ============================================================
set -euo pipefail

REPO=${REPO:-17center/opsflow}
RUN_NUMBER=${RUN_NUMBER:?需要 RUN_NUMBER}
DEST=${DEST:-/tmp/opsflow-release}
TAG="ci-${RUN_NUMBER}"

# 镜像按顺序尝试，最后一项为空字符串代表直连 GitHub
MIRRORS=("https://gh-proxy.com/" "https://ghproxy.net/" "")

log()  { printf '[fetch %s] %s\n' "$(date '+%H:%M:%S')" "$*"; }
fail() { printf '[fetch %s] ERROR: %s\n' "$(date '+%H:%M:%S')" "$*" >&2; exit 1; }

api() {
  if [ -n "${GH_TOKEN:-}" ]; then
    curl -sS --max-time 60 -H "Authorization: Bearer ${GH_TOKEN}" \
         -H "Accept: application/vnd.github+json" "$@"
  else
    curl -sS --max-time 60 -H "Accept: application/vnd.github+json" "$@"
  fi
}

command -v jq >/dev/null || fail "缺少 jq"
command -v sha256sum >/dev/null || fail "缺少 sha256sum"

log "查询 release ${TAG}"
REL_JSON=$(api "https://api.github.com/repos/${REPO}/releases/tags/${TAG}") \
  || fail "无法访问 GitHub API"

if echo "$REL_JSON" | jq -e '.message' >/dev/null 2>&1; then
  fail "release ${TAG} 不存在：$(echo "$REL_JSON" | jq -r '.message')"
fi

rm -rf "$DEST"
mkdir -p "$DEST"

# 逐个资源下载并校验
COUNT=$(echo "$REL_JSON" | jq '.assets | length')
[ "$COUNT" -gt 0 ] || fail "release ${TAG} 没有任何资源"
log "发现 ${COUNT} 个资源"

echo "$REL_JSON" | jq -r '.assets[] | "\(.name)\t\(.browser_download_url)\t\(.digest // "")"' | \
while IFS=$'\t' read -r NAME URL DIGEST; do
  [ -n "$NAME" ] || continue
  OUT="$DEST/$NAME"
  log "下载 $NAME"

  OK=0
  for M in "${MIRRORS[@]}"; do
    SRC="${M}${URL}"
    HOST=$(echo "${M:-direct}" | sed -E 's#https?://##; s#/$##')
    # --retry 让偶发中断自动重试；-f 让 4xx/5xx 直接失败而不是写入错误页
    if curl -fsSL --retry 3 --retry-delay 2 --max-time 900 -o "$OUT" "$SRC" 2>/dev/null; then
      SZ=$(stat -c %s "$OUT" 2>/dev/null || echo 0)
      if [ "$SZ" -gt 0 ]; then
        log "  经 ${HOST} 下载完成（$(awk -v s="$SZ" 'BEGIN{printf "%.1f", s/1024/1024}') MB）"
        OK=1
        break
      fi
    fi
    log "  ${HOST} 失败，尝试下一个源"
  done

  [ "$OK" = "1" ] || fail "$NAME 所有下载源均失败"

  # 强制校验 sha256（digest 来自可信的 api.github.com）
  if [ -n "$DIGEST" ] && [ "$DIGEST" != "null" ]; then
    EXPECT=${DIGEST#sha256:}
    ACTUAL=$(sha256sum "$OUT" | awk '{print $1}')
    if [ "$EXPECT" != "$ACTUAL" ]; then
      rm -f "$OUT"
      fail "$NAME 校验失败！期望 $EXPECT 实际 $ACTUAL（可能被镜像篡改）"
    fi
    log "  sha256 校验通过"
  else
    log "  ⚠️ 该资源无 digest，跳过校验"
  fi
done

log "全部资源就绪："
ls -la "$DEST"
exit 0
