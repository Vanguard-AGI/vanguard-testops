ALTER TABLE metadata_definition
ADD COLUMN is_case TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为案例：0-否，1-是';

