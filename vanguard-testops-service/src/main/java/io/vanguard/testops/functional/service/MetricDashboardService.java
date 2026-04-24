package io.vanguard.testops.functional.service;

import io.vanguard.testops.functional.dto.dashboard.PersonalStatsDTO;
import io.vanguard.testops.functional.dto.dashboard.ProjectOverviewDTO;
import io.vanguard.testops.functional.mapper.MetricDashboardMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MetricDashboardService {

    @Resource
    private MetricDashboardMapper metricDashboardMapper;

    public List<ProjectOverviewDTO> getProjectOverview(String projectId, String userId, Long startTime, Long endTime) {
        List<ProjectOverviewDTO> list = metricDashboardMapper.selectProjectOverview(projectId, userId, startTime, endTime);
        // Parse topFrequentCases JSON string into List<TopFrequentCase>
        if (list != null && !list.isEmpty()) {
            ProjectOverviewDTO dto = list.get(0);
            String json = dto.getTopFrequentCasesRaw(); // Get the raw JSON string
            if (json != null && !json.isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.List<ProjectOverviewDTO.TopFrequentCase> cases = mapper.readValue("[" + json + "]",
                            mapper.getTypeFactory().constructCollectionType(java.util.List.class, ProjectOverviewDTO.TopFrequentCase.class));
                    dto.setTopFrequentCases(cases);
                } catch (Exception e) {
                    // log and ignore parsing errors
                }
            }
        }
        return list;
    }

    public List<PersonalStatsDTO> getPersonalStats(String projectId, Long startTime, Long endTime) {
        return metricDashboardMapper.selectPersonalStats(projectId, startTime, endTime);
    }
}
