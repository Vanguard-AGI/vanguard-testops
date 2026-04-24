-- 复用类型：从两库 copy 出的用例，仅改标题=直接复用，还改了其它=适配复用；非复用用例为 NULL
-- 1）业务表 functional_case：唯一数据源，更新/复制/导入时写入
ALTER TABLE functional_case
    ADD COLUMN reuse_type VARCHAR(32) NULL COMMENT '复用类型：DIRECT_REUSE(直接复用-仅改标题)/ADAPT_REUSE(适配复用-改了其它)' AFTER source_case_id;

CREATE INDEX idx_reuse_type ON functional_case (reuse_type);

-- 2）效能指标表 case_metrics_detail：与 source_case_id 一致，从 functional_case 同步，统计时按 reuse_type 分组/筛选无需 join 业务表
ALTER TABLE spotter_efficiency.case_metrics_detail
    ADD COLUMN reuse_type VARCHAR(32) NULL COMMENT '复用类型(与functional_case.reuse_type一致，从业务表同步)' AFTER source_case_id;

CREATE INDEX idx_reuse_type ON spotter_efficiency.case_metrics_detail (reuse_type);
