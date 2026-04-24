package io.vanguard.testops.requirementquality.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vanguard.testops.requirementquality.domain.RequirementChangeStats;
import io.vanguard.testops.requirementquality.mapper.RequirementChangeStatsMapper;
import io.vanguard.testops.requirementquality.request.PipelineReportRequest;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 云效流水线白名单上报 - 接收运维脚本上报的流水线数据，落库 requirement_change_stats
 */
@Service
public class PipelineReportService {

    @Resource
    private RequirementChangeStatsMapper requirementChangeStatsMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 流水线上报时发布结果待定，由界面负责人选择后落库 */
    private static final String DEPLOY_RESULT_PENDING = "PENDING";
    private static final String ENDPOINT_FRONTEND = "FRONTEND";
    private static final String ENDPOINT_BACKEND = "BACKEND";
    private static final String ENDPOINT_MIXED = "MIXED";

    /**
     * 流水线上报一条变更记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void report(PipelineReportRequest request) {
        RequirementChangeStats row = new RequirementChangeStats();
        row.setId(UUID.randomUUID().toString());
        row.setStoryId("");
        row.setProjectId("");
        row.setRepoName(request.getRepoName());
        row.setServiceName(request.getServiceName());
        String otherInfoStr = serializeOtherInfo(request.getOtherInfo());
        row.setOtherInfo(otherInfoStr);
        row.setEndpointType(normalizeEndpointType(request.getEndpointType()));
        row.setPipelineId(request.getPipelineId());
        row.setPipelineName(request.getPipelineName());
        row.setPipelineUrl(buildPipelineUrl(request.getPipelineId(), request.getOtherInfo(), otherInfoStr));
        row.setEnv(null);
        row.setDeployTime(parseDeployTime(request.getDeployTime()));
        row.setLocAdd(parseInt(request.getLocAdd(), 0));
        row.setLocDelete(parseInt(request.getLocDelete(), 0));
        row.setLocModify(0);
        int add = row.getLocAdd();
        int del = row.getLocDelete();
        row.setLocValid(add + del);
        row.setDeployResult(DEPLOY_RESULT_PENDING);
        row.setIsRollback(0);
        row.setIsHotfix(0);
        row.setDeployer(request.getDeployer());
        row.setFrontend(null);
        row.setBackend(null);
        row.setRemark(null);
        row.setDetails(serializeDetails(request.getDetails() != null ? request.getDetails() : null));
        row.setCreatedAt(System.currentTimeMillis());

        requirementChangeStatsMapper.insert(row);
    }

    private String normalizeEndpointType(String endpointType) {
        if (StringUtils.isBlank(endpointType)) {
            return ENDPOINT_MIXED;
        }
        String t = endpointType.trim().toUpperCase();
        if ("前端".equals(endpointType.trim()) || "FRONTEND".equals(t)) {
            return ENDPOINT_FRONTEND;
        }
        if ("后端".equals(endpointType.trim()) || "BACKEND".equals(t)) {
            return ENDPOINT_BACKEND;
        }
        if ("混合".equals(endpointType.trim()) || "MIXED".equals(t)) {
            return ENDPOINT_MIXED;
        }
        return t;
    }

    private long parseDeployTime(String deployTime) {
        if (StringUtils.isBlank(deployTime)) {
            return System.currentTimeMillis();
        }
        try {
            return Long.parseLong(deployTime.trim());
        } catch (NumberFormatException e) {
            return System.currentTimeMillis();
        }
    }

    private int parseInt(String value, int defaultValue) {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseDetails(Object details) {
        if (details == null) {
            return null;
        }
        if (details instanceof List) {
            return (List<Map<String, Object>>) details;
        }
        if (details instanceof String) {
            String s = ((String) details).trim();
            if (s.isEmpty() || "[]".equals(s)) {
                return null;
            }
            try {
                return OBJECT_MAPPER.readValue(s, new TypeReference<List<Map<String, Object>>>() {});
            } catch (JsonProcessingException e) {
                return null;
            }
        }
        return null;
    }

    private String serializeDetails(Object detailsObj) {
        List<Map<String, Object>> details = parseDetails(detailsObj);
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static final String FLOW_BASE = "https://flow.aliyun.com/pipelines/";

    /**
     * 根据 pipelineId 与 otherInfo 中的 BUILD_NUMBER 拼云效流水线详情链接，无 buildNumber 时用 /current
     */
    private String buildPipelineUrl(String pipelineId, Object otherInfoRaw, String otherInfoStr) {
        if (StringUtils.isBlank(pipelineId)) {
            return null;
        }
        String buildNumber = extractBuildNumber(otherInfoRaw, otherInfoStr);
        if (StringUtils.isNotBlank(buildNumber)) {
            return FLOW_BASE + pipelineId.trim() + "/builds/" + buildNumber.trim();
        }
        return FLOW_BASE + pipelineId.trim() + "/current";
    }

    @SuppressWarnings("unchecked")
    private String extractBuildNumber(Object otherInfoRaw, String otherInfoStr) {
        if (otherInfoRaw instanceof Map) {
            Object v = ((Map<String, Object>) otherInfoRaw).get("BUILD_NUMBER");
            if (v != null) {
                return v.toString().trim();
            }
        }
        if (StringUtils.isNotBlank(otherInfoStr)) {
            try {
                Map<String, Object> map = OBJECT_MAPPER.readValue(otherInfoStr, new TypeReference<Map<String, Object>>() {});
                Object v = map != null ? map.get("BUILD_NUMBER") : null;
                return v != null ? v.toString().trim() : null;
            } catch (JsonProcessingException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * otherInfo 支持字符串或 JSON 对象（如流水线 env 整份），落库时统一转为字符串
     */
    private String serializeOtherInfo(Object otherInfo) {
        if (otherInfo == null) {
            return null;
        }
        if (otherInfo instanceof String) {
            String s = ((String) otherInfo).trim();
            return s.isEmpty() ? null : s;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(otherInfo);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
