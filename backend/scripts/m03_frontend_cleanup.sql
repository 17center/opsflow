-- 清理 M3 前端浏览器测试产生的流程定义（无关联实例）
DELETE FROM wf_definition WHERE `key`='m3_frontend_test';
SELECT COUNT(*) AS remaining FROM wf_definition WHERE `key`='m3_frontend_test';