SELECT id, hostname, ip_address, auth_type, credential, LEFT(credential, 20) AS cred_head FROM cmdb_host WHERE deleted=0;
SELECT id, name, LEFT(content, 30) AS content_head FROM auto_script WHERE deleted=0;
SELECT id, script_id, version FROM auto_script_version WHERE deleted=0;
SELECT id, status, error_message FROM auto_exec_record WHERE deleted=0;