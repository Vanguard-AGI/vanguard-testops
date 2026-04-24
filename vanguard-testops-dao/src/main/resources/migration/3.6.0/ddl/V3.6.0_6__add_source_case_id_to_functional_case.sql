-- 功能用例表增加「来源用例ID」：复制/导入时记录由哪条用例复制而来，用于执行归父、复用统计
ALTER TABLE functional_case
    ADD COLUMN source_case_id VARCHAR(50) NULL COMMENT '来源用例ID（复制或从两库导入时记录被复制用例的id）' AFTER ref_id;

CREATE INDEX idx_source_case_id ON functional_case (source_case_id);
