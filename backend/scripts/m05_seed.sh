#!/bin/bash
BASE="http://127.0.0.1:8080"
req() {
  local method=$1 path=$2 data=$3
  if [ -n "$data" ]; then
    curl -s -X "$method" "$BASE$path" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$data"
  else
    curl -s -X "$method" "$BASE$path" -H "Authorization: Bearer $TOKEN"
  fi
  echo
}
LOGIN=$(curl -s -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}')
TOKEN=$(echo "$LOGIN" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

echo "===== 新增主机 Web-01 ====="
req POST /api/automation/hosts '{"hostname":"Web-01","ipAddress":"192.168.2.101","sshPort":22,"osType":"CentOS","authType":1,"credential":"x","groupName":"前端集群"}'
echo "===== 新增主机 App-01 ====="
req POST /api/automation/hosts '{"hostname":"App-01","ipAddress":"192.168.2.102","sshPort":22,"osType":"Ubuntu","authType":1,"credential":"x","groupName":"后端集群"}'
echo "===== 新增主机 DB-01 ====="
req POST /api/automation/hosts '{"hostname":"DB-01","ipAddress":"192.168.2.103","sshPort":22,"osType":"CentOS","authType":1,"credential":"x","groupName":"数据库集群"}'

# 获取主机ID
HIDS=$(req GET "/api/automation/hosts?current=1&size=10" "")
H1=$(echo "$HIDS" | sed -n 's/.*"id":\([0-9]*\).*"Web-01".*/\1/p' | head -1)
H2=$(echo "$HIDS" | sed -n 's/.*"id":\([0-9]*\).*"App-01".*/\1/p' | head -1)
H3=$(echo "$HIDS" | sed -n 's/.*"id":\([0-9]*\).*"DB-01".*/\1/p' | head -1)
# 兜底：按顺序取
if [ -z "$H1" ]; then H1=$(echo "$HIDS" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | sed -n '1p'); fi
if [ -z "$H2" ]; then H2=$(echo "$HIDS" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | sed -n '2p'); fi
if [ -z "$H3" ]; then H3=$(echo "$HIDS" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | sed -n '3p'); fi
echo "主机ID: H1=$H1 H2=$H2 H3=$H3"

echo "===== 新增服务 ====="
req POST /api/cmdb/services "{\"name\":\"Nginx-网关\",\"serviceType\":\"Nginx\",\"version\":\"1.24\",\"hostId\":$H1,\"port\":80,\"status\":1}"
req POST /api/cmdb/services "{\"name\":\"MySQL-主库\",\"serviceType\":\"MySQL\",\"version\":\"8.0\",\"hostId\":$H3,\"port\":3306,\"status\":1}"
req POST /api/cmdb/services "{\"name\":\"Redis-缓存\",\"serviceType\":\"Redis\",\"version\":\"7.0\",\"hostId\":$H3,\"port\":6379,\"status\":1}"
req POST /api/cmdb/services "{\"name\":\"App-Service\",\"serviceType\":\"Tomcat\",\"version\":\"9.0\",\"hostId\":$H2,\"port\":8080,\"status\":1}"

echo "===== 服务列表 ====="
SERVICES=$(req GET "/api/cmdb/services?current=1&size=10" "")
echo "$SERVICES"
S1=$(echo "$SERVICES" | sed -n 's/.*"id":\([0-9]*\).*"name":"Nginx-网关".*/\1/p' | head -1)
[ -z "$S1" ] && S1=$(echo "$SERVICES" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | sed -n '1p')
S2=$(echo "$SERVICES" | sed -n 's/.*"id":\([0-9]*\).*"name":"MySQL-主库".*/\1/p' | head -1)
[ -z "$S2" ] && S2=$(echo "$SERVICES" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | sed -n '2p')
S3=$(echo "$SERVICES" | sed -n 's/.*"id":\([0-9]*\).*"name":"Redis-缓存".*/\1/p' | head -1)
[ -z "$S3" ] && S3=$(echo "$SERVICES" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | sed -n '3p')
S4=$(echo "$SERVICES" | sed -n 's/.*"id":\([0-9]*\).*"name":"App-Service".*/\1/p' | head -1)
[ -z "$S4" ] && S4=$(echo "$SERVICES" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | sed -n '4p')
echo "服务ID: S1=$S1 S2=$S2 S3=$S3 S4=$S4"

echo "===== 建立关联 ====="
echo "-- App依赖MySQL(S4 依赖 S2)"
req POST /api/cmdb/relations "{\"sourceType\":\"SERVICE\",\"sourceId\":$S4,\"targetType\":\"SERVICE\",\"targetId\":$S2,\"relationType\":\"DEPENDS_ON\"}"
echo "-- App依赖Redis(S4 依赖 S3)"
req POST /api/cmdb/relations "{\"sourceType\":\"SERVICE\",\"sourceId\":$S4,\"targetType\":\"SERVICE\",\"targetId\":$S3,\"relationType\":\"DEPENDS_ON\"}"
echo "-- Nginx包含App(S1 CONTAINS S4)"
req POST /api/cmdb/relations "{\"sourceType\":\"SERVICE\",\"sourceId\":$S1,\"targetType\":\"SERVICE\",\"targetId\":$S4,\"relationType\":\"CONTAINS\"}"

echo "===== 拓扑图 ====="
req GET /api/cmdb/relations/topology ""
echo "种子数据完成"