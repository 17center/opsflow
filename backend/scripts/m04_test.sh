#!/bin/bash
# M04 自动化执行引擎接口实测脚本（密钥认证版）
BASE="http://127.0.0.1:8080"
TOKEN=""

req() {
  local method=$1 path=$2 data=$3
  if [ -n "$data" ]; then
    curl -s -X "$method" "$BASE$path" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$data"
  else
    curl -s -X "$method" "$BASE$path" -H "Authorization: Bearer $TOKEN"
  fi
  echo
}

# 将文件内容转为 JSON 字符串（转义换行/反斜杠/引号）
json_escape() {
  sed -e 's/\\/\\\\/g' -e 's/"/\\"/g' | awk '{printf "%s\\n", $0}' | sed 's/\\n$//'
}

echo "===== 1. 登录 ====="
LOGIN=$(curl -s -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}')
TOKEN=$(echo "$LOGIN" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
if [ -z "$TOKEN" ]; then echo "登录失败"; exit 1; fi
echo "登录OK"

echo "===== 1.1 清理历史M4测试主机(避免唯一索引冲突) ====="
OID=$(req GET "/api/automation/hosts?current=1&size=50" "" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
for id in $OID; do
  req DELETE /api/automation/hosts/$id ""
done
req GET "/api/automation/hosts?current=1&size=50" ""

echo "===== 2. 新增目标主机(127.0.0.1 密钥认证) ====="
KEYJSON=$(cat /root/.ssh/id_ed25519 | json_escape)
req POST /api/automation/hosts "{\"hostname\":\"M4测试本机\",\"ipAddress\":\"127.0.0.1\",\"sshPort\":22,\"osType\":\"CentOS\",\"authType\":2,\"credential\":\"$KEYJSON\",\"groupName\":\"M4测试组\",\"remark\":\"M4测试\"}"
HOSTID=$(req GET "/api/automation/hosts?current=1&size=10" "" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
echo "主机ID=$HOSTID"

echo "===== 3. 主机列表 ====="
req GET "/api/automation/hosts?current=1&size=10" ""

echo "===== 5. 主机连接测试(预期成功) ====="
req POST /api/automation/hosts/$HOSTID/test ""

echo "===== 6. 创建脚本(Shell, 输出多行) ====="
req POST /api/automation/scripts '{"name":"M4测试-系统巡检","description":"返回主机信息与hello","scriptType":1,"content":"#!/bin/bash\necho \"开始巡检\"\nhostname\ndate\nuname -r\necho \"巡检结束 exit=0\"\nexit 0","timeoutSeconds":30,"category":"巡检","changeLog":"初始版本"}'
SID=$(req GET "/api/automation/scripts?current=1&size=10" "" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
echo "脚本ID=$SID"

echo "===== 8. 脚本详情(v1) ====="
req GET /api/automation/scripts/$SID ""

echo "===== 9. 修改脚本(生成v2) ====="
req PUT /api/automation/scripts/$SID '{"name":"M4测试-系统巡检v2","description":"更新版","scriptType":1,"content":"#!/bin/bash\necho v2-ok\nexit 0","timeoutSeconds":30,"category":"巡检","changeLog":"增加版本标记"}'

echo "===== 10. 脚本版本列表(应2个) ====="
req GET /api/automation/scripts/$SID/versions ""

echo "===== 11. 回滚到v1 ====="
req POST /api/automation/scripts/$SID/rollback/1 ""

echo "===== 13. 停用脚本 ====="
req POST /api/automation/scripts/$SID/disable ""

echo "===== 14. 停用后执行(预期40074) ====="
req POST /api/automation/exec/start "{\"scriptId\":$SID,\"hostId\":$HOSTID,\"triggerType\":2}"

echo "===== 15. 启用脚本 ====="
req POST /api/automation/scripts/$SID/enable ""

echo "===== 16. 触发脚本执行(异步) ====="
EXEC=$(req POST /api/automation/exec/start "{\"scriptId\":$SID,\"hostId\":$HOSTID,\"triggerType\":2}")
echo "$EXEC"
EID=$(echo "$EXEC" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
echo "执行记录ID=$EID"

echo "===== 17. 等待3秒后查执行记录列表 ====="
sleep 3
req GET "/api/automation/exec?current=1&size=10" ""

echo "===== 18. 执行详情(含输出日志) ====="
req GET /api/automation/exec/$EID ""

echo "===== 19. 401未认证 ====="
curl -s "$BASE/api/automation/scripts"
echo

echo "===== 20. 清理测试数据 ====="
mysql -uopsflow -pOpsflow@123 opsflow -e "DELETE FROM auto_exec_log WHERE exec_record_id IN (SELECT id FROM auto_exec_record WHERE script_id IN (SELECT id FROM auto_script WHERE name LIKE 'M4测试-%')); DELETE FROM auto_exec_record WHERE script_id IN (SELECT id FROM auto_script WHERE name LIKE 'M4测试-%'); DELETE FROM auto_script_version WHERE script_id IN (SELECT id FROM auto_script WHERE name LIKE 'M4测试-%'); DELETE FROM auto_script WHERE name LIKE 'M4测试-%'; DELETE FROM cmdb_host WHERE ip_address='127.0.0.1' AND hostname LIKE 'M4%';" 2>/dev/null
echo "清理完成"