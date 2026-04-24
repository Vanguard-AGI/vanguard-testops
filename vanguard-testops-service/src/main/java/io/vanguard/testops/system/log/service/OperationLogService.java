package io.vanguard.testops.system.log.service;

import io.vanguard.testops.project.domain.Project;
import io.vanguard.testops.sdk.domain.OperationLogBlob;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.sdk.mapper.OperationLogBlobMapper;
import io.vanguard.testops.sdk.mapper.OperationLogMapper;
import io.vanguard.testops.sdk.util.BeanUtils;
import io.vanguard.testops.sdk.util.Translator;
import io.vanguard.testops.system.domain.OperationHistory;
import io.vanguard.testops.system.domain.OperationHistoryExample;
import io.vanguard.testops.system.domain.Organization;
import io.vanguard.testops.system.dto.sdk.OptionDTO;
import io.vanguard.testops.system.log.dto.LogDTO;
import io.vanguard.testops.system.log.vo.OperationLogResponse;
import io.vanguard.testops.system.log.vo.SystemOperationLogRequest;
import io.vanguard.testops.system.mapper.*;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class OperationLogService {
    @Resource
    private OperationLogMapper operationLogMapper;
    @Resource
    private OperationHistoryMapper operationHistoryMapper;
    @Resource
    private OperationLogBlobMapper operationLogBlobMapper;
    @Resource
    private SqlSessionFactory sqlSessionFactory;

    @Resource
    private BaseOperationLogMapper baseOperationLogMapper;

    @Resource
    private BaseUserMapper baseUserMapper;

    @Resource
    private BaseProjectMapper baseProjectMapper;

    @Resource
    private BaseOrganizationMapper baseOrganizationMapper;

    private static OperationHistory getHistory(LogDTO log) {
        OperationHistory history = new OperationHistory();
        BeanUtils.copyBean(history, log);
        return history;
    }

    private OperationLogBlob getBlob(LogDTO log) {
        OperationLogBlob blob = new OperationLogBlob();
        blob.setId(log.getId());
        blob.setOriginalValue(log.getOriginalValue());
        blob.setModifiedValue(log.getModifiedValue());
        return blob;
    }

    private String subStrContent(String content) {
        if (StringUtils.isNotBlank(content) && content.length() > 500) {
            return content.substring(0, 499);
        }
        return content;
    }

    public void add(LogDTO log) {
        if (StringUtils.isBlank(log.getProjectId())) {
            log.setProjectId("none");
        }
        if (StringUtils.isBlank(log.getCreateUser())) {
            log.setCreateUser("admin");
        }
        log.setContent(subStrContent(log.getContent()));
        operationLogMapper.insert(log);
        if (log.getHistory()) {
            operationHistoryMapper.insert(getHistory(log));
        }
        operationLogBlobMapper.insert(getBlob(log));
    }

    public List<OperationLogResponse> list(SystemOperationLogRequest request) {
        int compare = Long.compare(request.getStartTime(), request.getEndTime());
        if (compare > 0) {
            throw new MSException(Translator.get("startTime_must_be_less_than_endTime"));
        }
        List<OperationLogResponse> list = baseOperationLogMapper.list(request);

        if (CollectionUtils.isNotEmpty(list)) {
            List<String> userIds = list.stream().map(OperationLogResponse::getCreateUser).collect(Collectors.toList());
            List<String> projectIds = list.stream().map(OperationLogResponse::getProjectId).collect(Collectors.toList());
            List<String> organizationIds = list.stream().map(OperationLogResponse::getOrganizationId).collect(Collectors.toList());
            List<OptionDTO> userList = baseUserMapper.selectUserOptionByIds(userIds);
            Map<String, String> userMap = userList.stream().collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
            List<Project> projects = baseProjectMapper.selectProjectByIdList(projectIds);
            Map<String, String> projectMap = projects.stream().collect(Collectors.toMap(Project::getId, Project::getName));
            List<Organization> organizations = baseOrganizationMapper.selectOrganizationByIdList(organizationIds);
            Map<String, String> organizationMap = organizations.stream().collect(Collectors.toMap(Organization::getId, Organization::getName));
            list.forEach(item -> {
                item.setUserName(userMap.getOrDefault(item.getCreateUser(), StringUtils.EMPTY));
                item.setProjectName(projectMap.getOrDefault(item.getProjectId(), StringUtils.EMPTY));
                item.setOrganizationName(organizationMap.getOrDefault(item.getOrganizationId(), StringUtils.EMPTY));
            });
        }
        return list;
    }

    @Async
    public void batchAdd(List<LogDTO> logs) {
        if (CollectionUtils.isEmpty(logs)) {
            return;
        }
        
        // 分批处理，每批最多 100 条，避免一次性加载所有数据到内存导致内存溢出
        int batchSize = 100;
        int totalSize = logs.size();
        long currentTimeMillis = System.currentTimeMillis();
        
        for (int i = 0; i < totalSize; i += batchSize) {
            int end = Math.min(i + batchSize, totalSize);
            List<LogDTO> batch = logs.subList(i, end);
            
            SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
            try {
                OperationLogBlobMapper logBlobMapper = sqlSession.getMapper(OperationLogBlobMapper.class);
                OperationLogMapper logMapper = sqlSession.getMapper(OperationLogMapper.class);
                OperationHistoryMapper historyMapper = sqlSession.getMapper(OperationHistoryMapper.class);
                
                for (LogDTO item : batch) {
                    item.setContent(subStrContent(item.getContent()));
                    item.setCreateTime(currentTimeMillis);
                    
                    logMapper.insert(item);
                    // 在 BATCH 模式下，需要立即 flush 才能获取生成的主键 id
                    sqlSession.flushStatements();
                    
                    if (item.getHistory()) {
                        historyMapper.insert(getHistory(item));
                    }
                    logBlobMapper.insert(getBlob(item));
                }
                
                // 最后再 flush 一次，确保所有操作都提交
                sqlSession.flushStatements();
            } finally {
                SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
            }
            
            // 给 GC 一些时间回收内存，避免内存累积
            if (i + batchSize < totalSize) {
                System.gc();
            }
        }
    }

    @Async
    public void deleteBySourceIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        OperationHistoryExample example = new OperationHistoryExample();
        example.createCriteria().andSourceIdIn(ids);
        operationHistoryMapper.deleteByExample(example);
    }
}
