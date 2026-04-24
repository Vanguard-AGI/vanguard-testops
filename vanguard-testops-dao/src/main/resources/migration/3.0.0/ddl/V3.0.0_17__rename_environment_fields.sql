-- 重命名环境配置中的字段名称
-- dataEndpoint: host -> data_host, port -> data_port, user -> data_user, password -> data_password
-- xxljobInfo: user -> xxljobuser, password -> xxljobpassword

-- 更新 data_endpoint JSON 字段中的字段名
UPDATE workflow_engine_profile
SET data_endpoint = JSON_OBJECT(
    'data_host', JSON_EXTRACT(data_endpoint, '$.host'),
    'data_port', JSON_EXTRACT(data_endpoint, '$.port'),
    'data_user', JSON_EXTRACT(data_endpoint, '$.user'),
    'data_password', JSON_EXTRACT(data_endpoint, '$.password'),
    'database', JSON_EXTRACT(data_endpoint, '$.database'),
    'charset', JSON_EXTRACT(data_endpoint, '$.charset'),
    'connect_timeout', JSON_EXTRACT(data_endpoint, '$.connect_timeout'),
    'read_timeout', JSON_EXTRACT(data_endpoint, '$.read_timeout'),
    'write_timeout', JSON_EXTRACT(data_endpoint, '$.write_timeout')
)
WHERE data_endpoint IS NOT NULL 
  AND JSON_EXTRACT(data_endpoint, '$.host') IS NOT NULL;

-- 更新 xxljob_info JSON 字段中的字段名
UPDATE workflow_engine_profile
SET xxljob_info = JSON_OBJECT(
    'xxljobuser', JSON_EXTRACT(xxljob_info, '$.user'),
    'xxljobpassword', JSON_EXTRACT(xxljob_info, '$.password'),
    'xxjob_url', JSON_EXTRACT(xxljob_info, '$.xxjob_url')
)
WHERE xxljob_info IS NOT NULL 
  AND (JSON_EXTRACT(xxljob_info, '$.user') IS NOT NULL 
       OR JSON_EXTRACT(xxljob_info, '$.password') IS NOT NULL 
       OR JSON_EXTRACT(xxljob_info, '$.xxjob_url') IS NOT NULL);
