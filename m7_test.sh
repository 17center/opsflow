#!/bin/bash
# M7 知识库接口测试（动态获取 ID）
BASE=http://localhost:8080/api
TOKEN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.accessToken')
AUTH="Authorization: Bearer $TOKEN"
echo "TOKEN_LEN=${#TOKEN}"

echo "== 1 创建标签 =="
curl -s -X POST $BASE/kb/tags -H "$AUTH" -H 'Content-Type: application/json' -d '{"name":"Redis"}' | jq -c '{code,message}'
echo "== 2 重复标签(应40101) =="
curl -s -X POST $BASE/kb/tags -H "$AUTH" -H 'Content-Type: application/json' -d '{"name":"Redis"}' | jq -c '{code,message}'
echo "== 3 创建标签2 =="
curl -s -X POST $BASE/kb/tags -H "$AUTH" -H 'Content-Type: application/json' -d '{"name":"MySQL"}' | jq -c '{code,message}'
echo "== 4 标签列表 =="
TAGS=$(curl -s "$BASE/kb/tags" -H "$AUTH")
echo "$TAGS" | jq -c '{code,count:(.data|length)}'
TAG1=$(echo "$TAGS" | jq -r '.data[] | select(.name=="Redis") | .id')
TAG2=$(echo "$TAGS" | jq -r '.data[] | select(.name=="MySQL") | .id')
echo "TAG1=$TAG1 TAG2=$TAG2"

echo "== 5 创建文章 =="
curl -s -X POST $BASE/kb/articles -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"title\":\"Redis 内存溢出排查手册\",\"content\":\"## 现象\nRedis 报错 OOM\n## 排查步骤\n1. 执行 redis-cli info memory\n2. 检查 maxmemory 配置\",\"category\":1,\"tagIds\":[$TAG1],\"status\":0}" | jq -c '{code,message}'
echo "== 6 创建文章2 =="
curl -s -X POST $BASE/kb/articles -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"title\":\"MySQL 慢查询优化\",\"content\":\"## 现象\n查询缓慢\n## 排查\n1. 查看慢查询日志\n2. 使用 EXPLAIN 分析\",\"category\":2,\"tagIds\":[$TAG2],\"status\":1}" | jq -c '{code,message}'
echo "== 7 文章列表 =="
curl -s "$BASE/kb/articles?current=1&size=10" -H "$AUTH" | jq -c '{code,total:.data.total}'
echo "== 8 关键词搜索 =="
curl -s "$BASE/kb/articles?current=1&size=10&keyword=Redis" -H "$AUTH" | jq -c '{code,count:(.data.records|length),titles:[.data.records[].title]}'
ART1=$(curl -s "$BASE/kb/articles?current=1&size=10&keyword=Redis" -H "$AUTH" | jq -r '.data.records[0].id')
ART2=$(curl -s "$BASE/kb/articles?current=1&size=10&keyword=MySQL" -H "$AUTH" | jq -r '.data.records[0].id')
echo "ART1=$ART1 ART2=$ART2"
echo "== 9 文章详情(浏览量+1) =="
curl -s "$BASE/kb/articles/$ART1" -H "$AUTH" | jq -c '{code,title:.data.title,viewCount:.data.viewCount,tags:.data.tagNames}'
echo "== 10 修改文章 =="
curl -s -X PUT "$BASE/kb/articles/$ART1" -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"title\":\"Redis 内存溢出排查手册(改)\",\"content\":\"## 现象\nRedis OOM\n## 排查\n1. info memory\",\"category\":1,\"tagIds\":[$TAG1,$TAG2],\"status\":0}" | jq -c '{code,message}'
echo "== 11 发布文章 =="
curl -s -X POST "$BASE/kb/articles/$ART1/status" -H "$AUTH" -H 'Content-Type: application/json' -d '{"status":1}' | jq -c '{code,message}'
echo "== 12 智能问答(Redis) =="
curl -s -X POST "$BASE/kb/qa" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"question":"Redis 内存满了怎么处理？"}' | jq -c '{code,conversationId:.data.conversationId,sources:(.data.sources|length),answerHead:(.data.answer|.[0:40])}'
echo "== 13 智能问答(无相关) =="
curl -s -X POST "$BASE/kb/qa" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"question":"如何配置Kubernetes集群"}' | jq -c '{code,sources:(.data.sources|length)}'
echo "== 14 删除标签(被引用,应40103) =="
curl -s -X DELETE "$BASE/kb/tags/$TAG1" -H "$AUTH" | jq -c '{code,message}'
echo "== 15 删除文章 =="
curl -s -X DELETE "$BASE/kb/articles/$ART2" -H "$AUTH" | jq -c '{code,message}'
echo "== 16 删除标签(现在可删) =="
curl -s -X DELETE "$BASE/kb/tags/$TAG1" -H "$AUTH" | jq -c '{code,message}'
echo "== 17 未认证(应401) =="
curl -s "$BASE/kb/articles" | jq -c '{code}'