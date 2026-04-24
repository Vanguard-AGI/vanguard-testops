package io.vanguard.testops.system.service;

import io.vanguard.testops.system.domain.User;
import io.vanguard.testops.system.domain.UserExample;
import io.vanguard.testops.system.dto.sdk.OptionDTO;
import io.vanguard.testops.system.dto.table.TableBatchProcessDTO;
import io.vanguard.testops.system.mapper.BaseUserMapper;
import io.vanguard.testops.system.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户工具服务
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class UserToolService {
    @Resource
    private BaseUserMapper baseUserMapper;
    @Resource
    private UserMapper userMapper;

    public List<User> selectByIdList(List<String> userIdList) {
        UserExample example = new UserExample();
        example.createCriteria().andIdIn(userIdList);
        return userMapper.selectByExample(example);
    }

    public List<String> getBatchUserIds(TableBatchProcessDTO request) {
        if (request.isSelectAll()) {
            List<User> userList = baseUserMapper.selectByKeyword(request.getCondition().getKeyword(), true);
            List<String> userIdList = userList.stream().map(User::getId).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(request.getExcludeIds())) {
                userIdList.removeAll(request.getExcludeIds());
            }
            return userIdList;
        } else {
            return request.getSelectIds();
        }
    }

    /**
     * 获取用户Map集合 (复用)
     * @param userIds 用户ID集合
     * @return 用户 <ID, NAME> 映射集合
     */
    public Map<String, String> getUserMapByIds(List<String> userIds) {
        List<OptionDTO> userOptions = baseUserMapper.selectUserOptionByIds(userIds);
        return userOptions.stream().collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
    }
}
