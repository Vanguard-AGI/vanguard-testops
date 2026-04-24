package io.vanguard.testops.runner.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * spotter_runner.mock_scene_rule 表实体，用于 Mock 工厂指标总数统计
 */
@Data
@TableName(value = "mock_scene_rule", schema = "spotter_runner")
@Schema(description = "Mock 场景规则")
public class MockSceneRule implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Integer id;

    @TableField("scene_code")
    @Schema(description = "场景编码")
    private String sceneCode;

    @TableField("service_code")
    @Schema(description = "服务编码")
    private String serviceCode;

    @TableField("created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @TableField("deleted_at")
    @Schema(description = "删除时间，NULL 表示未删除")
    private LocalDateTime deletedAt;
}
