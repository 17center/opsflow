#!/bin/bash
# M02 工单管理接口实测脚本
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
echo "$LOGIN"
TOKEN=$(echo "$LOGIN" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
if [ -z "$TOKEN" ]; then
  echo "登录失败，无法获取 token"
  exit 1
fi
echo "TOKEN 前 20 位: ${TOKEN:0:20}..."

echo "===== 2. 创建工单(变更-高) ====="
CREATE=$(req POST /api/tickets '{"title":"M02测试-升级Redis至7.2","description":"## 背景\n升级生产Redis","ticketType":1,"priority":1,"scriptParams":{"targetVersion":"7.2.4"}}')
echo "$CREATE"
TID=$(echo "$CREATE" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
TNO=$(echo "$CREATE" | sed -n 's/.*"ticketNo":"\([^"]*\)".*/\1/p')
echo "工单ID=$TID 编号=$TNO"

echo "===== 3. 创建工单(故障-紧急, 无模板) ====="
CREATE2=$(req POST /api/tickets '{"title":"M02测试-数据库连接故障","description":"数据库无法连接","ticketType":2,"priority":0}')
echo "$CREATE2"
TID2=$(echo "$CREATE2" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo "工单2 ID=$TID2"

echo "===== 4. 工单详情 ====="
req GET /api/tickets/$TID ""

echo "===== 5. 提交工单1(有模板字段→待审批) ====="
req POST /api/tickets/$TID/submit ""

echo "===== 6. 提交工单2(无模板→待指派) ====="
req POST /api/tickets/$TID2/submit ""

echo "===== 7. 指派工单1给admin(id=1)→处理中 ====="
req POST /api/tickets/$TID/assign '{"assigneeId":1}'

echo "===== 8. 解决工单1 ====="
req POST /api/tickets/$TID/resolve '{"resolution":"已按方案完成Redis升级至7.2.4"}'

echo "===== 9. 关闭工单1 ====="
req POST /api/tickets/$TID/close ""

echo "===== 10. 重新打开工单1(关闭/已解决→重新打开) ====="
req POST /api/tickets/$TID/reopen '{"reason":"升级后连接超时，需排查"}'

echo "===== 11. 异常分支: 对草稿工单2执行resolve(应从待指派→处理中→已解决, 但草稿下不允许) ====="
# 工单2 已提交到待指派，先解决应报40050? 实际允许 PENDING_ASSIGN→IN_PROGRESS→RESOLVED 需两步。
echo "---- 对工单2(当前待指派)直接resolve，预期40050 状态不允许 ----"
req POST /api/tickets/$TID2/resolve '{"resolution":"x"}'

echo "===== 12. 评论: 添加评论(含@提及) ====="
req POST /api/tickets/$TID/comments '{"content":"请确认方案 **@admin**","mentionedUserIds":[1]}'

echo "===== 13. 评论列表 ====="
req GET /api/tickets/$TID/comments ""

echo "===== 14. 看板统计 ====="
req GET /api/tickets/dashboard ""

echo "===== 15. 工单列表(筛选类型=变更) ====="
req GET "/api/tickets?current=1&size=10&ticketType=1" ""

echo "===== 16. 站内通知列表 ====="
req GET "/api/system/notifications?current=1&size=10" ""

echo "===== 17. 未读通知数 ====="
req GET /api/system/notifications/unread-count ""

echo "===== 18. 全部标记已读 ====="
req PUT /api/system/notifications/read-all ""

echo "===== 19. 未认证401 ====="
curl -s "http://127.0.0.1:8080/api/tickets"
echo

echo "===== 20. 清理测试数据 ====="
mysql -uopsflow -pOpsflow@123 opsflow -e "UPDATE ticket SET deleted=1 WHERE id IN ($TID,$TID2); DELETE FROM sys_notification WHERE related_type='TICKET' AND related_id IN ($TID,$TID2); DELETE FROM ticket_log WHERE ticket_id IN ($TID,$TID2); DELETE FROM ticket_comment WHERE ticket_id IN ($TID,$TID2);" 2>/dev/null
echo "清理完成"