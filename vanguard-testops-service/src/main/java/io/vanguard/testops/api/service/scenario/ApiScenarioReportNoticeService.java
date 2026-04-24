package io.vanguard.testops.api.service.scenario;

import io.vanguard.testops.api.domain.ApiScenarioReport;
import io.vanguard.testops.api.mapper.ApiScenarioReportMapper;
import io.vanguard.testops.api.mapper.ExtApiScenarioReportMapper;
import io.vanguard.testops.sdk.util.JSON;
import io.vanguard.testops.sdk.util.SubListUtils;
import io.vanguard.testops.system.domain.User;
import io.vanguard.testops.system.dto.sdk.ApiReportMessageDTO;
import io.vanguard.testops.system.mapper.UserMapper;
import io.vanguard.testops.system.notice.constants.NoticeConstants;
import io.vanguard.testops.system.service.CommonNoticeSendService;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ApiScenarioReportNoticeService {

    @Resource
    private ApiScenarioReportMapper apiScenarioReportMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private ExtApiScenarioReportMapper extApiScenarioReportMapper;

    public ApiReportMessageDTO getDto(String id) {
        ApiScenarioReport scenarioReport = apiScenarioReportMapper.selectByPrimaryKey(id);
        ApiReportMessageDTO reportMessageDTO = new ApiReportMessageDTO();
        reportMessageDTO.setId(scenarioReport.getId());
        reportMessageDTO.setName(scenarioReport.getName());
        return reportMessageDTO;
    }

    public void batchSendNotice(List<String> ids, User user, String projectId, String event) {
        if (CollectionUtils.isNotEmpty(ids)) {
            SubListUtils.dealForSubList(ids, 100, (subList) -> {
                List<ApiReportMessageDTO> noticeLists = extApiScenarioReportMapper.getNoticeList(subList);
                List<Map> resources = new ArrayList<>(JSON.parseArray(JSON.toJSONString(noticeLists), Map.class));
                commonNoticeSendService.sendNotice(NoticeConstants.TaskType.API_REPORT_TASK, event, resources, user, projectId);
            });
        }
    }
}
