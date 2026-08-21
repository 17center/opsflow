-- 清理 M3 测试残留数据
DELETE FROM wf_task WHERE wf_instance_id IN (SELECT id FROM wf_instance WHERE ticket_id IN (SELECT id FROM ticket WHERE title LIKE 'M3测试-%'));
DELETE FROM sys_notification WHERE related_type='TASK';
DELETE FROM ticket_log WHERE ticket_id IN (SELECT id FROM ticket WHERE title LIKE 'M3测试-%');
DELETE FROM wf_instance WHERE ticket_id IN (SELECT id FROM ticket WHERE title LIKE 'M3测试-%');
UPDATE ticket SET deleted=1 WHERE title LIKE 'M3测试-%';
UPDATE wf_definition SET deleted=1 WHERE `key`='m3_test_approval';
SELECT COUNT(*) AS active_m3_tickets FROM ticket WHERE deleted=0 AND title LIKE 'M3测试-%';
SELECT COUNT(*) AS active_wf FROM wf_definition WHERE deleted=0;