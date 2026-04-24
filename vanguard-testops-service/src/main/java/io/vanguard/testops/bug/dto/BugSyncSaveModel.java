package io.vanguard.testops.bug.dto;

import io.vanguard.testops.bug.domain.Bug;
import io.vanguard.testops.plugin.platform.dto.response.PlatformBugDTO;
import io.vanguard.testops.plugin.platform.spi.Platform;
import io.vanguard.testops.project.domain.Project;
import io.vanguard.testops.system.domain.Template;
import io.vanguard.testops.system.domain.TemplateCustomField;
import io.vanguard.testops.system.dto.sdk.TemplateDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class BugSyncSaveModel {

	/**
	 * 平台缺陷
	 */
	private PlatformBugDTO platformBug;

	/**
	 * MS缺陷
	 */
	private Bug msBug;

	/**
	 * MS默认模板
	 */
	private TemplateDTO msDefaultTemplate;

	/**
	 * 插件默认模板
	 */
	private Template pluginDefaultTemplate;

	/**
	 * 模板字段映射
	 */
	private Map<String, List<TemplateCustomField>> templateFieldMap;

	/**
	 * 所属平台
	 */
	private Platform platform;

	/**
	 * 平台名称
	 */
	private String platformName;

	/**
	 * 所属项目
	 */
	private Project project;
}
