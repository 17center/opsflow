#!/bin/bash
# M7 工单转知识 + 异常分支测试
BASE=http://localhost:8080/api
TOKEN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.accessToken')
AUTH="Authorization: Bearer $TOKEN"

echo "== 18 已关闭工单转知识 =="
curl -s -X POST "$BASE/kb/articles/from-ticket/16" -H "$AUTH" -H 'Content-Type: application/json' | jq -c '{code,id:.data.id,title:.data.title,status:.data.status,relatedTicketId:.data.relatedTicketId,contentHead:(.data.content|.[0:60])}'
echo "== 19 不存在的工单(应40050) =="
curl -s -X POST "$BASE/kb/articles/from-ticket/99999" -H "$AUTH" -H 'Content-Type: application/json' | jq -c '{code,message}'
echo "== 20 未关闭工单转知识(应40104) 先建一个未关闭工单 =="
TID=$(mysql -uroot -p'Root123!' opsflow -N -e "INSERT INTO ticket (ticket_no,title,ticket_type,priority,status,creator_id,create_by,create_time,deleted) VALUES ('OPS-REQ-20260816-888','测试未关闭','2',1,'IN_PROGRESS',1,'admin',NOW(),0); SELECT LAST_INSERT_ID();" 2>/dev/null)
echo "created unclosed ticket=$TID"
curl -s -X POST "$BASE/kb/articles/from-ticket/$TID" -H "$AUTH" -H 'Content-Type: application/json' | jq -c '{code,message}'
echo "== 21 未认证(from-ticket,应401) =="
curl -s -X POST "$BASE/kb/articles/from-ticket/16" | jq -c '{code}'
echo "== 22 文章列表(应含工单转知识文章) =="
curl -s "$BASE/kb/articles?current=1&size=20&keyword=%E6%85%A2%E6%9F%A5%E8%AF%A2" -H "$AUTH" | jq -c '{code,total:.data.total,titles:[.data.records[].title]}'