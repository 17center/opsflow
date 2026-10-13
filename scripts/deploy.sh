#!/usr/bin/env bash
# ============================================================
# OpsFlow 部署脚本（由 GitHub Actions 自托管 runner 以 sudo 调用）
#
# 用法: sudo /opt/opsflow/bin/deploy.sh
#       —— 刻意不接受参数，产物路径固定。
#          这样 sudoers 可以精确到「这一条命令」，不留参数注入面。
#
# 产物来源（由 workflow 的 download-artifact 步骤准备）：
#   /tmp/opsflow-release/jar/    后端 jar
#   /tmp/opsflow-release/dist/   前端 dist
#
# 流程：校验产物 -> 备份当前版本 -> 原子替换 -> 重启 -> 健康检查
#       -> 业务检查 -> 失败自动回滚 -> 清理旧备份
#
# 注意：本脚本以 root 运行。任何路径都写死，不接受外部输入。
# ============================================================
set -euo pipefail

# ---------- 固定路径 ----------
APP_DIR=/opt/opsflow
BACKEND_DIR=$APP_DIR/backend
FRONTEND_DIR=$APP_DIR/frontend
JAR_NAME=opsflow-backend.jar
BACKUP_ROOT=$APP_DIR/backups
KEEP_BACKUPS=5

RELEASE_DIR=/tmp/opsflow-release
JAR_SRC_DIR=$RELEASE_DIR/jar
DIST_SRC_DIR=$RELEASE_DIR/dist

SERVICE=opsflow-backend
HEALTH_URL=http://127.0.0.1/api/health
LOGIN_URL=http://127.0.0.1/api/auth/login
INDEX_URL=http://127.0.0.1/index.html

STAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR=$BACKUP_ROOT/$STAMP

log()  { printf '[deploy %s] %s\n' "$(date '+%H:%M:%S')" "$*"; }
fail() { printf '[deploy %s] ERROR: %s\n' "$(date '+%H:%M:%S')" "$*" >&2; }

# ---------- 0. 校验产物 ----------
# 兼容两种命名：CI 上传的 opsflow-backend.jar，以及 Maven 原生的 opsflow-backend-<ver>.jar
JAR_SRC=$(find "$JAR_SRC_DIR" -maxdepth 1 -type f -name 'opsflow-backend*.jar' 2>/dev/null | head -1)
[ -n "$JAR_SRC" ] || { fail "未在 $JAR_SRC_DIR 找到 opsflow-backend*.jar"; exit 1; }
[ -f "$DIST_SRC_DIR/index.html" ] || { fail "未在 $DIST_SRC_DIR 找到 index.html"; exit 1; }

log "待部署 jar : $(basename "$JAR_SRC") ($(du -h "$JAR_SRC" | cut -f1))"
log "待部署前端 : $DIST_SRC_DIR ($(du -sh "$DIST_SRC_DIR" | cut -f1))"

# ---------- 1. 备份当前版本 ----------
mkdir -p "$BACKUP_DIR"
HAD_PREVIOUS=0
if [ -f "$BACKEND_DIR/$JAR_NAME" ]; then
  cp -a "$BACKEND_DIR/$JAR_NAME" "$BACKUP_DIR/$JAR_NAME"
  HAD_PREVIOUS=1
  log "已备份旧 jar"
fi
if [ -d "$FRONTEND_DIR" ]; then
  mkdir -p "$BACKUP_DIR/frontend"
  cp -a "$FRONTEND_DIR/." "$BACKUP_DIR/frontend/" 2>/dev/null || true
  log "已备份旧前端"
fi

# ---------- 回滚 ----------
rollback() {
  fail "触发回滚"
  if [ "$HAD_PREVIOUS" = "1" ]; then
    cp -a "$BACKUP_DIR/$JAR_NAME" "$BACKEND_DIR/$JAR_NAME"
    if [ -d "$BACKUP_DIR/frontend" ]; then
      rsync -a --delete "$BACKUP_DIR/frontend/" "$FRONTEND_DIR/" || true
      restorecon -R "$FRONTEND_DIR" 2>/dev/null || true
    fi
    log "已恢复旧版本，重启服务"
    systemctl restart "$SERVICE" || true
    for i in $(seq 1 30); do
      if [ "$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "$HEALTH_URL" 2>/dev/null || true)" = "200" ]; then
        log "回滚后服务已恢复（${i} 次探测）"
        return 0
      fi
      sleep 2
    done
    fail "回滚后仍未通过健康检查，需人工介入"
    return 1
  fi
  fail "无可用备份，无法回滚，需人工介入"
  return 1
}

# ---------- 2. 部署后端（原子替换）----------
log "部署后端"
install -m 644 "$JAR_SRC" "$BACKEND_DIR/$JAR_NAME.new"
mv -f "$BACKEND_DIR/$JAR_NAME.new" "$BACKEND_DIR/$JAR_NAME"

# ---------- 3. 部署前端 ----------
log "部署前端"
rsync -a --delete "$DIST_SRC_DIR/" "$FRONTEND_DIR/"
# nginx 以 httpd_t 运行，需正确的 SELinux 标签，否则 403
restorecon -R "$FRONTEND_DIR" 2>/dev/null || true
chmod -R a+rX "$FRONTEND_DIR"

# ---------- 4. 重启 ----------
log "重启 $SERVICE"
systemctl restart "$SERVICE"

# ---------- 5. 健康检查 ----------
READY=0
for i in $(seq 1 45); do
  code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "$HEALTH_URL" 2>/dev/null || true)
  if [ "$code" = "200" ]; then READY=1; log "健康检查通过（约 $((i*2))s）"; break; fi
  sleep 2
done
if [ "$READY" != "1" ]; then
  fail "健康检查失败：$HEALTH_URL 未返回 200"
  rollback || true
  exit 1
fi

# ---------- 6. 业务级检查 ----------
LOGIN_CODE=$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 \
  -X POST "$LOGIN_URL" -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' 2>/dev/null || true)
if [ "$LOGIN_CODE" != "200" ]; then
  fail "登录接口返回 $LOGIN_CODE（期望 200）"
  rollback || true
  exit 1
fi
log "登录接口正常"

INDEX_CODE=$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "$INDEX_URL" 2>/dev/null || true)
if [ "$INDEX_CODE" != "200" ]; then
  fail "前端 index.html 返回 $INDEX_CODE"
  rollback || true
  exit 1
fi
log "前端静态资源正常"

systemctl reload nginx || true

# ---------- 7. 清理旧备份 ----------
if [ -d "$BACKUP_ROOT" ]; then
  mapfile -t OLD < <(find "$BACKUP_ROOT" -maxdepth 1 -mindepth 1 -type d | sort -r | tail -n +$((KEEP_BACKUPS + 1)))
  for d in "${OLD[@]:-}"; do
    [ -n "$d" ] && rm -rf "$d" && log "清理旧备份 $(basename "$d")"
  done
fi

log "部署成功 ✅  jar=$(basename "$JAR_SRC")  备份=$BACKUP_DIR"
exit 0
