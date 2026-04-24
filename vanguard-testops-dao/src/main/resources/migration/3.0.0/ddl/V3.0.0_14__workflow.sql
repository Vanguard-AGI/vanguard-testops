-- 1. 模块/目录表 (统一管理元数据和工作流的目录)
create table metadata_module
(
    module_id    varchar(50)                 not null comment '模块ID'
        primary key,
    project_id   varchar(50)                 not null comment '项目/工作空间ID',
    parent_id    varchar(50) default 'ROOT'  not null comment '父节点ID',
    name         varchar(255)                not null comment '模块名称',
    module_type  varchar(20) default 'MIXED' not null comment '目录类型: API/WORKFLOW/SQL/MIXED',
    pos          bigint      default 0       not null comment '排序值',
    create_time  bigint                      not null comment '创建时间',
    update_time  bigint                      not null comment '更新时间',
    deleted_time datetime                    null,
    type_id      varchar(50)                 null comment '根据module_type来存对应业务下第一层ID（如WORKFLOW类型存储workspace_id）'
)
    comment '资源目录模块';

create index idx_project
    on metadata_module (project_id, parent_id);

create index idx_project_type
    on metadata_module (project_id, type_id);

create index idx_type_id
    on metadata_module (type_id);



-- 2. 元数据定义表
-- 适用于 API 定义、SQL 脚本、公共脚本等原子资源
CREATE TABLE metadata_definition (
                                     definition_id           VARCHAR(50)  NOT NULL COMMENT '主键ID',
                                     project_id   VARCHAR(50)  NOT NULL COMMENT '项目ID',
                                     module_id    VARCHAR(50)  NOT NULL COMMENT '模块ID',


                                     name         VARCHAR(255) NOT NULL COMMENT '名称',
    -- 维度一：技术协议（决定 request_config 的 JSON 结构）
                                     protocol     VARCHAR(20)  NOT NULL COMMENT '协议类型: HTTP/SQL/DUBBO/SCRIPT/FILE/MQ',


    -- 版本控制
                                     version      INT          NOT NULL DEFAULT 1 COMMENT '版本号',
                                     is_latest    TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否最新版本',
                                     description  VARCHAR(1000)         COMMENT '描述',


                                     request_config  JSON      COMMENT '请求配置(URL/Method/Headers/Body)',
                                     response_config JSON      COMMENT '响应配置(Schema/Extract)',
                                     script_content  LONGTEXT  COMMENT 'SQL语句 / Python脚本 / Shell脚本',
                                     tags         JSON         COMMENT '标签 ["冒烟", "交易"]',
                                     create_user  VARCHAR(50)  NOT NULL COMMENT '创建人',
                                     create_time  BIGINT       NOT NULL COMMENT '创建时间',
                                     update_time  BIGINT       NOT NULL COMMENT '最后修改时间',
                                     deleted_time  datetime DEFAULT NULL,

                                     PRIMARY KEY (definition_id),
                                     KEY idx_module (module_id),
                                     KEY idx_protocol (protocol)
) COMMENT='元数据定义(API/脚本)';


CREATE TABLE metadata_file_resource (
                                        file_id           VARCHAR(64)  NOT NULL COMMENT '主键ID',
                                        project_id   VARCHAR(64)  NOT NULL COMMENT '归属项目ID',
                                        storage_name  VARCHAR(255) NOT NULL COMMENT '存储文件名/Key (e.g. uuid_timestamp.csv)',

    -- 文件存放位置
                                        storage_type  VARCHAR(20)  NOT NULL DEFAULT 'LOCAL' COMMENT '存储方式: LOCAL/MINIO/S3/OSS',
                                        path          VARCHAR(500) NOT NULL COMMENT '存放路径或URL',

    -- 文件属性
                                        file_size     BIGINT       NOT NULL COMMENT '文件大小(字节)',
                                        extension     VARCHAR(20)  COMMENT '文件后缀',
                                        content_type  VARCHAR(100) COMMENT 'MIME类型 (e.g. application/json)',
                                        checksum      VARCHAR(64)  COMMENT '文件MD5/SHA256, 用于防止重复上传',

    -- 业务分类 (可选，看你是否需要区分用途)
                                        category      VARCHAR(32)  DEFAULT 'ATTACHMENT' COMMENT '分类: DATA(测试数据)/CERT(证书)/ATTACHMENT(附件)',

                                        create_user   VARCHAR(64)  NOT NULL,
                                        create_time   BIGINT       NOT NULL,
                                        deleted_time  datetime DEFAULT NULL,

                                        PRIMARY KEY (file_id),
                                        KEY idx_project (project_id)
) COMMENT='文件资源表';
-- ==================== 工作流工作空间表 ====================
CREATE TABLE workflow_workspace (
                                    workspace_id          VARCHAR(50)  NOT NULL COMMENT '工作空间ID',
                                    workspace_name        VARCHAR(255) NOT NULL COMMENT '工作空间名称',
                                    project_id            VARCHAR(50)  NOT NULL COMMENT '归属项目ID',
                                    owner                 VARCHAR(50)  NOT NULL COMMENT '负责人',
                                    global_vars           JSON                  COMMENT '全局变量定义，按照不同环境设置变量组',
                                    visibility            VARCHAR(20)  NOT NULL DEFAULT 'PRIVATE' COMMENT '可见范围：PRIVATE/PROJECT/ORG',
                                    description           VARCHAR(1000)         COMMENT '描述',
                                    icon                  VARCHAR(20)  DEFAULT '📁' COMMENT '图标（emoji）',
                                    icon_color            VARCHAR(50)  DEFAULT 'bg-gray-100' COMMENT '图标背景颜色（CSS类名）',
                                    create_time           BIGINT       NOT NULL COMMENT '创建时间（毫秒）',
                                    create_user           VARCHAR(50)  NOT NULL COMMENT '创建人',
                                    update_time           BIGINT       NOT NULL COMMENT '最后修改时间（毫秒）',
                                    update_user           VARCHAR(50)  NOT NULL COMMENT '最后修改人',
                                    deleted_by            VARCHAR(50)  NULL COMMENT '删除人',
                                    deleted_time          DATETIME     NULL DEFAULT NULL COMMENT '删除时间（毫秒，软删除，NULL表示未删除）',

                                    PRIMARY KEY (workspace_id),
                                    KEY idx_workspace_project (project_id),
                                    KEY idx_workspace_deleted (deleted_time)
) COMMENT='工作流工作空间';
-- ==================== 执行引擎/环境配置定义表 ====================
CREATE TABLE workflow_engine_profile (
                                         environment_id             VARCHAR(50)  NOT NULL COMMENT '环境ID',
                                         project_id        VARCHAR(50)  NOT NULL COMMENT '项目ID',
                                         environment_name         VARCHAR(255) NOT NULL COMMENT '环境名称',
                                         engine_type  VARCHAR(50)  NOT NULL COMMENT '引擎类型: API/UI',
    -- 连接配置
                                         auth_config  JSON                  COMMENT '认证信息(Token/Key)',
    -- 环境标识
                                         env_code     VARCHAR(32)  NOT NULL COMMENT '环境: DEV/TEST/PROD',
                                         robots     json         null comment '机器人',
                                         data_endpoint  json         null comment '被测服务数据库及各种数据连接',
                                         variables  json         not null comment '公共参数',
                                         domain     varchar(200) null comment '服务域名/IP地址',
                                         xxljob_info json        null comment 'XXL-Job配置信息',
                                         mq_info     json        null comment 'MQ配置信息',
                                         dubbo_info  json        null comment 'Dubbo调用信息',
                                         create_user VARCHAR(50)  NOT NULL COMMENT '创建人',
                                         update_user VARCHAR(50)  NOT NULL COMMENT '最后修改人',
                                         create_time  BIGINT       NOT NULL,
                                         update_time  BIGINT       NOT NULL,
                                         deleted_time datetime DEFAULT NULL,
                                         PRIMARY KEY (environment_id)
) COMMENT='执行引擎/环境配置';
-- ==================== 工作流定义表 ====================
CREATE TABLE workflow_definition (
                                     workflow_id           VARCHAR(50)  NOT NULL COMMENT '工作流ID',
                                     project_id            VARCHAR(50)  NOT NULL COMMENT '项目ID',
                                     module_id             VARCHAR(50)  NOT NULL COMMENT '模块ID',
                                     name                  VARCHAR(255) NOT NULL COMMENT '工作流名称',
                                     category              VARCHAR(32)  DEFAULT 'API' COMMENT '工作流分类: API/UI/AGENT',
                                     type                  VARCHAR(20)  NOT NULL DEFAULT 'TEST_CASE' COMMENT '类型: TEST_CASE(测试用例) / PUBLIC_STEP(公共步骤)',
                                     version               INT          NOT NULL DEFAULT 1 COMMENT '版本号',

    -- 全局配置
                                     global_vars           JSON                  COMMENT '全局变量定义',
                                     schedule_config       JSON                  COMMENT '定时触发配置 (Cron等)',
                                     description           VARCHAR(1000)         COMMENT '描述',
                                     status                VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED',

                                     create_user           VARCHAR(50)  NOT NULL COMMENT '创建人',
                                     create_time           BIGINT       NOT NULL COMMENT '创建时间（毫秒）',
                                     update_time           BIGINT       NOT NULL COMMENT '最后修改时间（毫秒）',
                                     update_user           VARCHAR(50)            COMMENT '最后修改人',
                                     deleted_time          DATETIME     NULL DEFAULT NULL COMMENT '删除时间（软删除，NULL表示未删除）',

                                     PRIMARY KEY (workflow_id),
                                     KEY idx_module (module_id),
                                     KEY idx_project (project_id),
                                     KEY idx_definition_deleted (deleted_time)
) COMMENT='工作流定义';

-- ==================== 工作流步骤表 ====================
CREATE TABLE workflow_step (
                               step_id               VARCHAR(50)  NOT NULL COMMENT '步骤ID',
                               workflow_id           VARCHAR(50)  NOT NULL COMMENT '所属工作流ID',
                               name                  VARCHAR(255) NOT NULL COMMENT '步骤名称',
                               step_type             VARCHAR(20)  NOT NULL COMMENT '类型: API/SQL/WAIT/IF/LOOP/SCRIPT/WEBSOCKET/DUBBO/CONDITION',

    -- 排序与结构
                               order_num             BIGINT       NOT NULL COMMENT '列表模式下的顺序',

    -- 步骤核心配置
                               step_config           JSON         NOT NULL COMMENT '步骤核心配置（API请求/断言/提取变量等）',

    -- 画布坐标
                               position_x            DECIMAL(10, 2) DEFAULT 0 COMMENT '节点在画布上的X坐标（像素）',
                               position_y            DECIMAL(10, 2) DEFAULT 0 COMMENT '节点在画布上的Y坐标（像素）',

    -- 引用模式
                               ref_mode              VARCHAR(20)  NOT NULL DEFAULT 'NONE' COMMENT '引用模式: NONE/COPY/REF_METADATA/REF_WORKFLOW',
                               ref_metadata_id       VARCHAR(50)            COMMENT '关联的元数据ID(可选)',
                               ref_workflow_id       VARCHAR(50)            COMMENT '关联的workflow(可选)',

                               enable                TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
                               create_time           BIGINT       NOT NULL COMMENT '创建时间（毫秒）',
                               update_time           BIGINT       NOT NULL COMMENT '最后修改时间（毫秒）',
                               deleted_time          DATETIME     NULL DEFAULT NULL COMMENT '删除时间（软删除，NULL表示未删除）',

                               PRIMARY KEY (step_id),
                               KEY idx_workflow (workflow_id),
                               KEY idx_ref_metadata (ref_metadata_id),
                               KEY idx_ref_workflow (ref_workflow_id),
                               KEY idx_step_deleted (deleted_time)
) COMMENT='工作流步骤节点';

-- ==================== 工作流步骤连线表 ====================
CREATE TABLE workflow_step_link (
                                    link_id               VARCHAR(50)  NOT NULL COMMENT '连线ID',
                                    workflow_id           VARCHAR(50)  NOT NULL COMMENT '工作流ID',
                                    source_step_id        VARCHAR(50)  NOT NULL COMMENT '源步骤ID',
                                    target_step_id        VARCHAR(50)  NOT NULL COMMENT '目标步骤ID',

    -- 连线属性
                                    label                 VARCHAR(100)           COMMENT '连线标签（如：true/false/case1等，用于条件分支）',
                                    color                 VARCHAR(20)            COMMENT '连线颜色（如：#10B981/#EF4444，用于区分不同分支）',
                                    condition_expr        VARCHAR(500)           COMMENT '流转条件表达式（当label不足以表达时使用）',
                                    order_num             INT          DEFAULT 0 COMMENT '连线顺序（同一源步骤的多条连线排序）',

                                    create_time           BIGINT       NOT NULL COMMENT '创建时间（毫秒）',
                                    update_time           BIGINT       NOT NULL COMMENT '最后修改时间（毫秒）',
                                    deleted_time          DATETIME     NULL DEFAULT NULL COMMENT '删除时间（软删除，NULL表示未删除）',

                                    PRIMARY KEY (link_id),
                                    KEY idx_workflow (workflow_id),
                                    KEY idx_source_step (source_step_id),
                                    KEY idx_target_step (target_step_id),
                                    KEY idx_workflow_source (workflow_id, source_step_id),
                                    KEY idx_link_deleted (deleted_time)
) COMMENT='工作流步骤连线';


-- ==================== 工作流运行记录表 ====================
CREATE TABLE workflow_run (
                              run_id                VARCHAR(50)  NOT NULL COMMENT '运行ID',
                              workflow_id           VARCHAR(50)  NOT NULL COMMENT '关联工作流ID',
                              project_id            VARCHAR(50)            COMMENT '项目ID（冗余字段，便于查询）',
                              module_id             VARCHAR(50)            COMMENT '关联模块ID（冗余字段，便于查询）',
                              workflow_name         VARCHAR(255) NOT NULL COMMENT '工作流名称快照（保存运行时的名称）',

    -- 触发信息
                              trigger_type          VARCHAR(20)  NOT NULL COMMENT '触发类型: MANUAL(手动)/SCHEDULE(定时)/API(接口触发)',
                              trigger_user          VARCHAR(50)            COMMENT '触发人',

    -- 运行状态
                              status                VARCHAR(20)  NOT NULL COMMENT '状态: PENDING(待执行)/RUNNING(执行中)/SUCCESS(成功)/FAILED(失败)/CANCELLED(已取消)',

    -- 时间信息
                              start_time            BIGINT                COMMENT '开始时间（毫秒）',
                              end_time              BIGINT                COMMENT '结束时间（毫秒）',
                              duration_ms           BIGINT                COMMENT '总耗时（毫秒）',

    -- 统计结果
                              total_steps           INT          DEFAULT 0 COMMENT '总步骤数',
                              passed_count          INT          DEFAULT 0 COMMENT '通过步骤数',
                              failed_count          INT          DEFAULT 0 COMMENT '失败步骤数',
                              skipped_count         INT          DEFAULT 0 COMMENT '跳过步骤数',
                              pending_count         INT          DEFAULT 0 COMMENT '待执行步骤数',

    -- 运行结果摘要
                              result_summary        JSON                  COMMENT '运行上下文/输出摘要/错误信息',

    -- 环境信息
                              environment_id        VARCHAR(50)            COMMENT '执行环境ID',
                              environment_name      VARCHAR(255)           COMMENT '执行环境名称（快照）',

                              create_time           BIGINT       NOT NULL COMMENT '创建时间（毫秒）',
                              update_time           BIGINT       NOT NULL COMMENT '最后更新时间（毫秒）',
                              deleted_time          DATETIME     NULL DEFAULT NULL COMMENT '删除时间（软删除，NULL表示未删除）',

                              PRIMARY KEY (run_id),
                              KEY idx_workflow (workflow_id),
                              KEY idx_project (project_id),
                              KEY idx_time (start_time),
                              KEY idx_status (status),
                              KEY idx_trigger_user (trigger_user),
                              KEY idx_deleted_time (deleted_time)
) COMMENT='工作流运行实例';

-- ==================== 步骤运行明细表 ====================
CREATE TABLE workflow_run_step (
                                   run_step_id           VARCHAR(50)  NOT NULL COMMENT '运行步骤ID',
                                   run_id                VARCHAR(50)  NOT NULL COMMENT '运行ID',
                                   step_id               VARCHAR(50)  NOT NULL COMMENT '步骤ID（关联 workflow_step）',
                                   step_name             VARCHAR(255) NOT NULL COMMENT '步骤名称快照（保存运行时的名称）',
                                   step_type             VARCHAR(20)            COMMENT '步骤类型快照（API/SQL/WAIT/IF/LOOP等）',
                                   order_num             INT                     COMMENT '步骤顺序（快照）',

    -- 运行状态
                                   status                VARCHAR(20)  NOT NULL COMMENT '状态: PENDING(待执行)/RUNNING(执行中)/SUCCESS(成功)/FAILED(失败)/SKIPPED(跳过)',

    -- 时间信息
                                   start_time            BIGINT                COMMENT '开始时间（毫秒）',
                                   end_time              BIGINT                COMMENT '结束时间（毫秒）',
                                   duration_ms           BIGINT                COMMENT '耗时（毫秒）',

    -- 核心运行数据
                                   request_data          JSON                  COMMENT '实际请求内容（HTTP请求参数、SQL语句等）',
                                   response_data         JSON                  COMMENT '实际响应内容（HTTP响应、SQL查询结果等）',
                                   assertion_logs        JSON                  COMMENT '断言验证日志（断言结果列表）',
                                   extract_vars          JSON                  COMMENT '提取的变量（变量名和值的映射）',

    -- 错误信息
                                   error_msg             TEXT                  COMMENT '错误消息',
                                   error_stack           TEXT                  COMMENT '错误堆栈（详细错误信息）',

    -- 执行描述
                                   description           TEXT                  COMMENT '执行描述/摘要',

                                   create_time           BIGINT       NOT NULL COMMENT '创建时间（毫秒）',
                                   update_time           BIGINT       NOT NULL COMMENT '最后更新时间（毫秒）',

                                   PRIMARY KEY (run_step_id),
                                   KEY idx_run (run_id),
                                   KEY idx_step (step_id),
                                   KEY idx_run_step (run_id, step_id),
                                   KEY idx_status (status)
) COMMENT='步骤运行日志';

-- ==================== 运行文本日志表 ====================
CREATE TABLE workflow_run_log (
                                  log_id                BIGINT       AUTO_INCREMENT COMMENT '主键',
                                  run_id                VARCHAR(50)  NOT NULL COMMENT '运行ID',
                                  run_step_id           VARCHAR(50)            COMMENT '运行步骤ID（可选，如果日志属于某个步骤）',
                                  step_id               VARCHAR(50)            COMMENT '步骤ID（可选，冗余字段）',

    -- 日志级别和内容
                                  level                 VARCHAR(10)  NOT NULL COMMENT '日志级别: DEBUG/INFO/WARN/ERROR',
                                  content               TEXT         NOT NULL COMMENT '日志内容',

    -- 时间信息
                                  log_time              BIGINT       NOT NULL COMMENT '日志时间（毫秒）',
                                  create_time           BIGINT       NOT NULL COMMENT '创建时间（毫秒）',

                                  PRIMARY KEY (log_id),
                                  KEY idx_run (run_id),
                                  KEY idx_run_step (run_step_id),
                                  KEY idx_time (log_time),
                                  KEY idx_level (level),
                                  KEY idx_run_time (run_id, log_time)
) COMMENT='运行控制台日志';



-- 公共节点数据表（按项目隔离）
CREATE TABLE workflow_public_node (
    id VARCHAR(50) NOT NULL COMMENT '节点ID' PRIMARY KEY,
    project_id VARCHAR(50) NOT NULL COMMENT '项目ID',
    name VARCHAR(255) NOT NULL COMMENT '节点名称',
    description VARCHAR(1000) COMMENT '节点描述',
    type VARCHAR(50) NOT NULL COMMENT '节点类型: http_request/mysql/dubbo/script/condition/loop等',
    category VARCHAR(20) NOT NULL COMMENT '节点分类: api/data/logic/script/other',
    config JSON NOT NULL COMMENT '节点配置（JSON格式，包含节点的完整配置信息）',
    create_time BIGINT NOT NULL COMMENT '创建时间（毫秒）',
    update_time BIGINT NOT NULL COMMENT '更新时间（毫秒）',
    create_user VARCHAR(50) NOT NULL COMMENT '创建人',
    update_user VARCHAR(50) COMMENT '更新人',
    deleted_time DATETIME NULL COMMENT '删除时间（软删除，NULL表示未删除）',
    deleted_by VARCHAR(50) COMMENT '删除人'
) COMMENT='工作流公共节点表（按项目隔离）';

-- 创建索引
CREATE INDEX idx_project_id ON workflow_public_node (project_id);
CREATE INDEX idx_type ON workflow_public_node (type);
CREATE INDEX idx_category ON workflow_public_node (category);
CREATE INDEX idx_deleted_time ON workflow_public_node (deleted_time);
CREATE INDEX idx_create_time ON workflow_public_node (create_time);

