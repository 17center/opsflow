#!/bin/bash
BASE="http://127.0.0.1:8080"
KEYJSON=$(cat /root/.ssh/id_ed25519 | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g' | awk '{printf "%s\\n", $0}' | sed 's/\\n$//')
LOGIN=$(curl -s -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}')
TOKEN=$(echo "$LOGIN" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
echo "===== 新增主机 ====="
curl -s -X POST "$BASE/api/automation/hosts" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "{\"hostname\":\"M5发现主机\",\"ipAddress\":\"127.0.0.1\",\"sshPort\":22,\"osType\":\"CentOS\",\"authType\":2,\"credential\":\"$KEYJSON\",\"groupName\":\"M5测试组\"}"
echo
HID=$(curl -s "$BASE/api/automation/hosts?current=1&size=50" -H "Authorization: Bearer $TOKEN" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
echo "主机ID=$HID"
echo "===== 自动发现 ====="
curl -s -X POST "$BASE/api/cmdb/services/discover/$HID" -H "Authorization: Bearer $TOKEN"
echo
echo "===== 清理 ====="
mysql -uopsflow -pOpsflow@123 opsflow -e "DELETE FROM cmdb_host WHERE hostname='M5发现主机';" 2>/dev/null
echo done