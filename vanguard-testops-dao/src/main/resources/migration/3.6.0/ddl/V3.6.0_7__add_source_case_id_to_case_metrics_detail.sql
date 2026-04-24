-- 与 functional_case.source_case_id 一致：复制/导入时落库源用例 id，执行归父、复用统计用；写入时从 functional_case 同步
ALTER TABLE spotter_efficiency.case_metrics_detail
    ADD COLUMN source_case_id VARCHAR(50) NULL COMMENT '来源用例ID(与functional_case.source_case_id一致，从业务表同步)' AFTER case_id;

CREATE INDEX idx_source_case_id ON spotter_efficiency.case_metrics_detail (source_case_id);
