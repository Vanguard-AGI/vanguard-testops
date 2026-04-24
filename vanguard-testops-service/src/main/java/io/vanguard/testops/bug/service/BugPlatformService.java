package io.vanguard.testops.bug.service;

import io.vanguard.testops.plugin.platform.dto.request.SyncAttachmentToPlatformRequest;
import io.vanguard.testops.plugin.platform.spi.Platform;
import io.vanguard.testops.project.service.ProjectApplicationService;
import io.vanguard.testops.sdk.util.LogUtils;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Service
public class BugPlatformService {

	@Resource
	private ProjectApplicationService projectApplicationService;

	/**
	 * 同步附件到平台
	 * @param platformAttachments 平台附件参数
	 * @param projectId 项目ID
	 */
	@Async
	public void syncAttachmentToPlatform(List<SyncAttachmentToPlatformRequest> platformAttachments, String projectId) {
		// 平台缺陷需同步附件
		Platform platform = projectApplicationService.getPlatform(projectId, true);
		if (!platform.isSupportAttachment()) {
			return;
		}
		platformAttachments.forEach(attachment -> {
			platform.syncAttachmentToPlatform(attachment);
			try {
				Files.deleteIfExists(attachment.getFile().toPath());
			} catch (IOException e) {
				LogUtils.error(e.getMessage());
			}
		});
	}
}
