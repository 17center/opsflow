#!/bin/bash
# M05 CMDB 资产模块接口实测脚本
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

echo "===== 1. 登录 ====="
LOGIN=$(curl -s -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}')
TOKEN=$(echo "$LOGIN" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
if [ -z "$TOKEN" ]; then echo "登录失败"; exit 1; fi
echo "登录OK"

echo "===== 2. 清理历史测试数据 ====="
mysql -uopsflow -pOpsflow@123 opsflow -e "DELETE FROM cmdb_relation WHERE source_name IS NULL OR create_by='admin'; DELETE FROM cmdb_service WHERE name LIKE 'M5测试%'; DELETE FROM cmdb_host WHERE hostname LIKE 'M5测试%';" 2>/dev/null
echo "清理完成"

echo "===== 3. 新增主机(M5测试主机) ====="
KEYJSON=$(cat /root/.ssh/id_ed25519 | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g' | awk '{printf "%s\\n", $0}' | sed 's/\\n$//')
req POST /api/automation/hosts "{\"hostname\":\"M5测试主机\",\"ipAddress\":\"127.0.0.1\",\"sshPort\":22,\"osType\":\"CentOS\",\"authType\":2,\"credential\":\"$KEYJSON\",\"groupName\":\"M5测试组\",\"remark\":\"M5测试\"}"
HOSTID=$(req GET "/api/automation/hosts?current=1&size=50" "" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
echo "主机ID=$HOSTID"

echo "===== 4. 新增服务资产(MySQL) ====="
req POST /api/cmdb/services "{\"name\":\"M5测试-MySQL\",\"serviceType\":\"MySQL\",\"version\":\"8.0.33\",\"hostId\":$HOSTID,\"port\":3306,\"status\":1,\"remark\":\"M5测试\"}"
SID=$(req GET "/api/cmdb/services?current=1&size=50" "" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
echo "服务ID=$SID"

echo "===== 5. 服务列表 ====="
req GET "/api/cmdb/services?current=1&size=10" ""

echo "===== 6. 服务详情 ====="
req GET /api/cmdb/services/$SID ""

echo "===== 7. 修改服务(改端口/版本) ====="
req PUT /api/cmdb/services/$SID '{"name":"M5测试-MySQL","serviceType":"MySQL","version":"8.0.36","hostId":'"$HOSTID"',"port":3306,"status":1,"remark":"M5测试-改"}'

echo "===== 8. 状态变更(维护中) ====="
req POST /api/cmdb/services/$SID/status '{"status":2}'

echo "===== 9. 新增第二个服务(Redis) ====="
req POST /api/cmdb/services "{\"name\":\"M5测试-Redis\",\"serviceType\":\"Redis\",\"version\":\"7.0.15\",\"hostId\":$HOSTID,\"port\":6379,\"status\":1,\"remark\":\"M5测试\"}"
SID2=$(req GET "/api/cmdb/services?current=1&size=50" "" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | tail -1)
echo "服务2ID=$SID2"

echo "===== 10. 建立关联(MySQL 依赖 Redis) ====="
req POST /api/cmdb/relations "{\"sourceType\":\"SERVICE\",\"sourceId\":$SID,\"targetType\":\"SERVICE\",\"targetId\":$SID2,\"relationType\":\"DEPENDS_ON\"}"

echo "===== 11. 关联列表 ====="
req GET /api/cmdb/relations ""

echo "===== 12. 重复关联(预期40092) ====="
req POST /api/cmdb/relations "{\"sourceType\":\"SERVICE\",\"sourceId\":$SID,\"targetType\":\"SERVICE\",\"targetId\":$SID2,\"relationType\":\"DEPENDS_ON\"}"

echo "===== 13. 自关联(预期40094) ====="
req POST /api/cmdb/relations "{\"sourceType\":\"SERVICE\",\"sourceId\":$SID,\"targetType\":\"SERVICE\",\"targetId\":$SID,\"relationType\":\"DEPENDS_ON\"}"

echo "===== 14. 资产不存在关联(预期40093) ====="
req POST /api/cmdb/relations "{\"sourceType\":\"HOST\",\"sourceId\":99999,\"targetType\":\"HOST\",\"targetId\":$HOSTID,\"relationType\":\"CONTAINS\"}"

echo "===== 15. 拓扑图 ====="
req GET /api/cmdb/relations/topology ""

echo "===== 16. 服务状态变更接口(不可用) ====="
req POST /api/cmdb/services/$SID2/status '{"status":0}'

echo "===== 17. 删除关联 ====="
RELD=$(req GET /api/cmdb/relations "" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
req DELETE /api/cmdb/relations/$RELD ""

echo "===== 18. 删除服务 ====="
req DELETE /api/cmdb/services/$SID ""
req DELETE /api/cmdb/services/$SID2 ""

echo "===== 19. 401未认证 ====="
curl -s "$BASE/api/cmdb/services"
echo

echo "===== 20. 清理测试数据 ====="
mysql -uopsflow -pOpsflow@123 opsflow -e "DELETE FROM cmdb_relation WHERE create_by='admin'; DELETE FROM cmdb_service WHERE name LIKE 'M5测试%'; DELETE FROM cmdb_host WHERE hostname LIKE 'M5测试%';" 2>/dev/null
echo "清理完成"