#!/bin/bash
# M03 工作流引擎接口实测脚本
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

echo "===== 2. 创建流程定义(二级审批) ====="
req POST /api/workflow/definitions '{"name":"M3测试-变更审批","key":"m3_test_approval","description":"二级审批流程","nodes":[{"nodeKey":"n1","nodeName":"运维主管审批","nodeType":1,"assigneeId":1,"signType":1},{"nodeKey":"n2","nodeName":"技术总监审批","nodeType":1,"assigneeId":1,"signType":1}]}'
DEFID=$(req GET "/api/workflow/definitions?current=1&size=10" "" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
echo "流程定义ID=$DEFID"

echo "===== 3. 重复key创建(预期40054) ====="
req POST /api/workflow/definitions '{"name":"M3重复","key":"m3_test_approval","nodes":[]}'

echo "===== 4. 流程定义列表 ====="
req GET "/api/workflow/definitions?current=1&size=10" ""

echo "===== 5. 发布流程定义 ====="
req POST /api/workflow/definitions/$DEFID/publish ""

echo "===== 6. 重复发布(预期40057) ====="
req POST /api/workflow/definitions/$DEFID/publish ""

echo "===== 7. 修改已发布(预期40056) ====="
req PUT /api/workflow/definitions/$DEFID '{"name":"M3改","key":"m3_test_approval","nodes":[]}'

echo "===== 8. 创建工单 ====="
TCK=$(req POST /api/tickets '{"title":"M3测试-网络安全加固","description":"修复CVE漏洞","ticketType":1,"priority":1}')
echo "$TCK"
TID=$(echo "$TCK" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
echo "工单ID=$TID"

echo "===== 9. 启动流程实例 ====="
INS=$(req POST /api/workflow/instances "{\"ticketId\":$TID,\"definitionId\":$DEFID}")
echo "$INS"
IID=$(echo "$INS" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
echo "实例ID=$IID"

echo "===== 10. 我的待办(应有一个任务) ====="
req GET "/api/workflow/tasks/todo?current=1&size=10" ""
TASK1=$(req GET "/api/workflow/tasks/todo?current=1&size=10" "" | sed -n 's/.*"taskId":\([0-9]*\).*/\1/p' | head -1)
echo "第一任务ID=$TASK1"

echo "===== 11. 审批通过 任务1(n1) ====="
req POST /api/workflow/tasks/$TASK1/approve '{"comment":"方案可行，同意"}'

echo "===== 12. 待办(应出现第二任务n2) ====="
TASK2=$(req GET "/api/workflow/tasks/todo?current=1&size=10" "" | sed -n 's/.*"taskId":\([0-9]*\).*/\1/p' | head -1)
echo "第二任务ID=$TASK2"

echo "===== 13. 重复审批已完成任务(预期40061) ====="
req POST /api/workflow/tasks/$TASK1/approve '{"comment":"再次审批"}'

echo "===== 14. 审批通过 任务2(n2) → 流程完成 ====="
req POST /api/workflow/tasks/$TASK2/approve '{"comment":"同意执行"}'

echo "===== 15. 流程实例列表 ====="
req GET "/api/workflow/instances?current=1&size=10" ""

echo "===== 16. 流程实例详情(含轨迹) ====="
req GET /api/workflow/instances/$IID ""

echo "===== 17. 工单详情确认状态已APPROVED ====="
req GET /api/tickets/$TID "" | sed -n 's/.*"status":"\([^"]*\)".*/\1/p' | head -1

echo "===== 18. 驳回流程测试 ====="
TCK2=$(req POST /api/tickets '{"title":"M3测试-驳回用例","description":"驳回流程","ticketType":3,"priority":2}')
TID2=$(echo "$TCK2" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
req POST /api/workflow/instances "{\"ticketId\":$TID2,\"definitionId\":$DEFID}" >/dev/null
RTASK=$(req GET "/api/workflow/tasks/todo?current=1&size=10" "" | sed -n 's/.*"taskId":\([0-9]*\).*/\1/p' | head -1)
req POST /api/workflow/tasks/$RTASK/reject '{"comment":"方案不完善，驳回"}'
echo "--- 工单2最终状态 ---"
req GET /api/tickets/$TID2 "" | sed -n 's/.*"status":"\([^"]*\)".*/\1/p' | head -1

echo "===== 19. 未认证401 ====="
curl -s "http://127.0.0.1:8080/api/workflow/definitions"
echo

echo "===== 20. 清理测试数据 ====="
mysql -uopsflow -pOpsflow@123 opsflow -e "UPDATE ticket SET deleted=1 WHERE id IN ($TID,$TID2); DELETE FROM ticket_log WHERE ticket_id IN ($TID,$TID2); DELETE FROM sys_notification WHERE related_type='TASK'; DELETE FROM wf_task WHERE wf_instance_id IN (SELECT id FROM wf_instance WHERE ticket_id IN ($TID,$TID2)); DELETE FROM wf_instance WHERE ticket_id IN ($TID,$TID2); DELETE FROM wf_definition WHERE \`key\`='m3_test_approval';" 2>/dev/null
echo "清理完成"