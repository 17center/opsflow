SELECT id, hostname, ip_address FROM cmdb_host WHERE deleted=0;
SELECT id, name, service_type, host_id FROM cmdb_service WHERE deleted=0;