DELETE FROM wf_definition WHERE `key`='m3_test_approval';
UPDATE ticket SET deleted=1 WHERE title LIKE 'M3测试-%';
SELECT COUNT(*) AS leftover_wf FROM wf_definition WHERE `key`='m3_test_approval';