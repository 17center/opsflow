#!/bin/bash
# ============================================================
# OpsFlow 全链路（端到端）测试脚本 v2
# 贯穿 M01 认证 → M05 资产 → M04 自动化 → M03 工作流
#      → M02 工单 → M07 知识库 → M06 告警 → M08 报表
# 创建类接口返回 data:null，故用列表查询取最新 id
# ============================================================
BASE="http://127.0.0.1:8080/api"
PASS=0; FAIL=0; FAILED_DETAILS=()

req() { local m=$1 p=$2 d=$3; if [ -n "$d" ]; then curl -s -X "$m" "$BASE$p" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d "$d"; else curl -s -X "$m" "$BASE$p" -H "Authorization: Bearer $TOKEN"; fi; }
code() { echo "$1" | sed -n 's/.*"code":\([0-9]*\).*/\1/p' | head -1; }
firstid() { echo "$1" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1; }
check() { if [ "$2" = "$3" ]; then PASS=$((PASS+1)); echo "  [PASS] $1 (code=$2)"; else FAIL=$((FAIL+1)); FAILED_DETAILS+=("$1: 期望$3 实际$2"); echo "  [FAIL] $1 (期望$3 实际$2)"; fi; }
json_escape() { sed -e 's/\\/\\\\/g' -e 's/"/\\"/g' | awk '{printf "%s\\n", $0}' | sed 's/\\n$//'; }

echo "===== 阶段0: 认证 (M01) ====="
LOGIN=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}')
TOKEN=$(echo "$LOGIN" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
check "登录获取JWT" "$(code "$LOGIN")" "200"
check "未认证访问返回401" "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/tickets")" "401"
check "获取当前用户" "$(code "$(req GET /auth/me '')")" "200"

echo "===== 阶段1: 目标主机与服务 (M05) ====="
# 新建一台可达的测试主机（127.0.0.1 + root 密钥），确保连接测试通过
KEYJSON=$(cat /root/.ssh/id_ed25519 | json_escape)
NH=$(req POST /automation/hosts "{\"hostname\":\"E2E-目标主机\",\"ipAddress\":\"127.0.0.1\",\"sshPort\":22,\"osType\":\"CentOS\",\"authType\":2,\"credential\":\"$KEYJSON\",\"groupName\":\"E2E组\"}")
check "新建目标主机" "$(code "$NH")" "200"
HOSTID=$(mysql -uopsflow -pOpsflow@123 opsflow -N -e "SELECT id FROM cmdb_host WHERE hostname='E2E-目标主机' AND ip_address='127.0.0.1' ORDER BY id DESC LIMIT 1" 2>/dev/null)
echo "  目标主机ID=$HOSTID"
check "主机连接测试" "$(code "$(req POST /automation/hosts/$HOSTID/test '')")" "200"
SVC=$(req POST /cmdb/services "{\"name\":\"e2e-web\",\"serviceType\":\"WEB\",\"hostId\":$HOSTID,\"status\":1}")
check "录入服务资产" "$(code "$SVC")" "200"

echo "===== 阶段2: 自动化脚本-创建/执行 (M04) ====="
SCR=$(req POST /automation/scripts '{"name":"E2E-状态检查","description":"全链路","scriptType":1,"content":"#!/bin/bash\necho \"E2E start\"\nhostname\nuptime\necho \"E2E done\"\nexit 0","timeoutSeconds":30,"category":"巡检","changeLog":"v1"}')
check "创建脚本" "$(code "$SCR")" "200"
SID=$(firstid "$(req GET /automation/scripts?current=1\&size=10 '')")
echo "  脚本ID=$SID"
EXEC=$(req POST /automation/exec/start "{\"scriptId\":$SID,\"hostId\":$HOSTID,\"triggerType\":2}")
EID=$(firstid "$EXEC")
check "触发脚本执行" "$(code "$EXEC")" "200"
sleep 3
check "执行详情含日志" "$(code "$(req GET /automation/exec/$EID '')")" "200"

echo "===== 阶段3: 工作流-定义/发布 (M03) ====="
DEF=$(req POST /workflow/definitions '{"name":"E2E-变更审批","key":"e2e_approval","description":"全链路","nodes":[{"nodeKey":"n1","nodeName":"主管审批","nodeType":1,"assigneeId":1,"signType":1}]}')
check "创建流程定义" "$(code "$DEF")" "200"
DEFID=$(firstid "$(req GET /workflow/definitions?current=1\&size=10 '')")
echo "  流程定义ID=$DEFID"
check "发布流程定义" "$(code "$(req POST /workflow/definitions/$DEFID/publish '')")" "200"

echo "===== 阶段4: 工单全生命周期 (M02+M03+M04) ====="
TICK=$(req POST /tickets '{"title":"E2E-生产环境变更","description":"全链路变更","ticketType":1,"priority":1,"hostId":'"$HOSTID"',"scriptId":'"$SID"'}')
TID=$(firstid "$TICK"); TNO=$(echo "$TICK" | sed -n 's/.*"ticketNo":"\([^"]*\)".*/\1/p' | head -1)
check "创建变更工单" "$(code "$TICK")" "200"
echo "  工单ID=$TID 编号=$TNO"
check "提交工单" "$(code "$(req POST /tickets/$TID/submit '')")" "200"
INS=$(req POST /workflow/instances "{\"ticketId\":$TID,\"definitionId\":$DEFID}")
check "启动流程实例" "$(code "$INS")" "200"
sleep 1
TODO=$(req GET /workflow/tasks/todo?current=1\&size=10 "")
TASK=$(echo "$TODO" | sed -n 's/.*"taskId":\([0-9]*\).*/\1/p' | head -1)
echo "  待办任务ID=$TASK"
if [ -n "$TASK" ]; then PASS=$((PASS+1)); echo "  [PASS] 待办任务生成"; else FAIL=$((FAIL+1)); FAILED_DETAILS+=("待办任务未生成"); echo "  [FAIL] 待办任务未生成"; fi
check "审批通过" "$(code "$(req POST /workflow/tasks/$TASK/approve '{"comment":"同意"}')")" "200"
TSTATUS=$(req GET /tickets/$TID "" | sed -n 's/.*"status":"\([^"]*\)".*/\1/p' | head -1)
echo "  审批后工单状态=$TSTATUS"
if [ "$TSTATUS" = "APPROVED" ]; then PASS=$((PASS+1)); echo "  [PASS] 审批后状态APPROVED"; else FAIL=$((FAIL+1)); FAILED_DETAILS+=("审批后状态=$TSTATUS"); echo "  [FAIL] 审批后状态=$TSTATUS"; fi
check "指派工单" "$(code "$(req POST /tickets/$TID/assign '{"assigneeId":1}')")" "200"
check "解决工单" "$(code "$(req POST /tickets/$TID/resolve '{"resolution":"已完成变更"}')")" "200"
check "关闭工单" "$(code "$(req POST /tickets/$TID/close '')")" "200"

echo "===== 阶段5: 知识库-转知识/发布/问答 (M07) ====="
FT=$(req POST /kb/articles/from-ticket/$TID "")
check "已关闭工单转知识" "$(code "$FT")" "200"
AID=$(firstid "$(req GET '/kb/articles?current=1&size=10' '')")
echo "  知识文章ID=$AID"
check "发布知识文章" "$(code "$(req POST /kb/articles/$AID/status '{"status":1}')")" "200"
check "智能问答" "$(code "$(req POST /kb/qa '{"question":"生产环境变更怎么处理"}')")" "200"

echo "===== 阶段6: 告警-规则/触发/确认/恢复 (M06) ====="
RULE=$(req POST /alerts/rules "{\"name\":\"E2E-CPU告警\",\"hostId\":$HOSTID,\"metric\":\"cpu_usage\",\"operator\":\">\",\"threshold\":90,\"durationSeconds\":60,\"alertLevel\":1,\"notifyChannels\":\"EMAIL\",\"notifyUsers\":\"1\",\"status\":1}")
check "创建告警规则" "$(code "$RULE")" "200"
RID=$(firstid "$(req GET '/alerts/rules?current=1&size=10' '')")
echo "  规则ID=$RID"
TRIG=$(req POST "/alerts/events/trigger?ruleId=$RID&hostId=$HOSTID&currentValue=95")
EVENT=$(firstid "$TRIG")
check "模拟触发告警" "$(code "$TRIG")" "200"
echo "  事件ID=$EVENT"
check "确认告警" "$(code "$(req POST /alerts/events/$EVENT/confirm '')")" "200"
check "恢复告警" "$(code "$(req POST /alerts/events/$EVENT/recover '')")" "200"

echo "===== 阶段7: 报表-仪表盘/导出 (M08) ====="
check "仪表盘统计" "$(code "$(req GET /reports/dashboard '')")" "200"
EX=$(req POST /reports/export '{"reportType":"ticket_stats","format":"EXCEL"}')
URL=$(echo "$EX" | sed -n 's/.*"downloadUrl":"\([^"]*\)".*/\1/p')
check "导出Excel报表" "$(code "$EX")" "200"
if [ -n "$URL" ]; then
  check "下载Excel文件" "$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:8080$URL" -H "Authorization: Bearer $TOKEN")" "200"
fi

echo "===== 清理测试数据 ====="
mysql -uopsflow -pOpsflow@123 opsflow -e "
UPDATE ticket SET deleted=1 WHERE ticket_no='$TNO';
DELETE FROM ticket_log WHERE ticket_id IN (SELECT id FROM ticket WHERE ticket_no='$TNO');
DELETE FROM ticket_comment WHERE ticket_id IN (SELECT id FROM ticket WHERE ticket_no='$TNO');
DELETE FROM sys_notification WHERE related_type='TICKET' AND related_id IN (SELECT id FROM ticket WHERE ticket_no='$TNO');
DELETE FROM wf_task WHERE wf_instance_id IN (SELECT id FROM wf_instance WHERE ticket_id IN (SELECT id FROM ticket WHERE ticket_no='$TNO'));
DELETE FROM wf_instance WHERE ticket_id IN (SELECT id FROM ticket WHERE ticket_no='$TNO');
DELETE FROM wf_definition WHERE \`key\`='e2e_approval';
DELETE FROM kb_article WHERE title LIKE 'E2E-%';
DELETE FROM auto_exec_log WHERE exec_record_id IN (SELECT id FROM auto_exec_record WHERE script_id IN (SELECT id FROM auto_script WHERE name LIKE 'E2E-%'));
DELETE FROM auto_exec_record WHERE script_id IN (SELECT id FROM auto_script WHERE name LIKE 'E2E-%');
DELETE FROM auto_script_version WHERE script_id IN (SELECT id FROM auto_script WHERE name LIKE 'E2E-%');
DELETE FROM auto_script WHERE name LIKE 'E2E-%';
DELETE FROM alert_event WHERE rule_id IN (SELECT id FROM alert_rule WHERE name LIKE 'E2E-%');
DELETE FROM alert_rule WHERE name LIKE 'E2E-%';
DELETE FROM cmdb_service WHERE name='e2e-web';
DELETE FROM cmdb_host WHERE hostname='E2E-目标主机' AND ip_address='127.0.0.1';
" 2>/dev/null
echo "清理完成"

echo "===== 全链路汇总 ====="
echo "通过: $PASS  失败: $FAIL"
if [ ${#FAILED_DETAILS[@]} -gt 0 ]; then echo "失败明细:"; for d in "${FAILED_DETAILS[@]}"; do echo "  - $d"; done; fi