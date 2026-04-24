package io.vanguard.testops.api.service.definition;

import io.vanguard.testops.api.domain.ApiReport;
import io.vanguard.testops.api.mapper.ApiReportMapper;
import io.vanguard.testops.api.mapper.ExtApiReportMapper;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.sdk.util.SubListUtils;
import io.vanguard.testops.system.domain.User;
import io.vanguard.testops.system.dto.sdk.ApiReportMessageDTO;
import io.vanguard.testops.system.notice.constants.NoticeConstants;
import io.vanguard.testops.system.service.CommonNoticeSendService;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ApiReportNoticeService {

    @Resource
    private ApiReportMapper apiReportMapper;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private ExtApiReportMapper extApiReportMapper;

    public ApiReportMessageDTO getDto(String id) {
        ApiReport apiReport = apiReportMapper.selectByPrimaryKey(id);
        ApiReportMessageDTO reportMessageDTO = new ApiReportMessageDTO();
        reportMessageDTO.setId(apiReport.getId());
        reportMessageDTO.setName(apiReport.getName());
        return reportMessageDTO;
    }

    public void batchSendNotice(List<String> ids, User user, String projectId, String event) {
        if (CollectionUtils.isNotEmpty(ids)) {
            SubListUtils.dealForSubList(ids, 100, (subList) -> {
                List<ApiReportMessageDTO> noticeLists = extApiReportMapper.getNoticeList(subList);
                List<Map> resources = new ArrayList<>(JSON.parseArray(JSON.toJSONString(noticeLists), Map.class));
                commonNoticeSendService.sendNotice(NoticeConstants.TaskType.API_REPORT_TASK, event, resources, user, projectId);
            });
        }
    }
}
