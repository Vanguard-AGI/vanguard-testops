package io.vanguard.testops.system.controller;

import io.vanguard.testops.sdk.constants.SessionConstants;
import io.vanguard.testops.sdk.util.CommonBeanFactory;
import io.vanguard.testops.sdk.util.LogUtils;
import io.vanguard.testops.system.domain.UserKey;
import io.vanguard.testops.system.dto.UserKeyDTO;
import io.vanguard.testops.system.dto.sdk.SessionUser;
import io.vanguard.testops.system.dto.table.TableBatchProcessDTO;
import io.vanguard.testops.system.dto.table.TableBatchProcessResponse;
import io.vanguard.testops.system.log.annotation.Log;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.service.UserKeyService;
import io.vanguard.testops.system.utils.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "系统设置-个人信息-API密钥")
@RestController
@RequestMapping("/user/api/key")
public class UserApiKeysController {

    @Resource
    private UserKeyService userKeyService;

    @GetMapping("/list")
    @Operation(summary = "个人信息-API密钥-获取用户API密钥列表")
    public List<UserKey> getUserKeys() {
        String userId = SessionUtils.getUserId();
        return userKeyService.getUserKeysInfo(userId);
    }

    @PostMapping("/add")
    @Operation(summary = "个人信息-API密钥-创建新的API密钥")
    @Log(type = OperationLogType.ADD, expression = "#msClass.addLog()", msClass = UserApiKeysController.class)
    public void addUserKey() {
        String userId = SessionUtils.getUserId();
        userKeyService.add(userId);
    }

    @PostMapping("/update")
    @Operation(summary = "个人信息-API密钥-更新API密钥")
    @Log(type = OperationLogType.UPDATE, expression = "#msClass.updateLog(#request)", msClass = UserApiKeysController.class)
    public void updateUserKey(@RequestBody UserKeyDTO request) {
        String userId = SessionUtils.getUserId();
        userKeyService.checkUserKeyOwner(request.getId(), userId);
        userKeyService.updateUserKey(request);
    }

    @PostMapping("/delete")
    @Operation(summary = "个人信息-API密钥-删除API密钥")
    @Log(type = OperationLogType.DELETE, expression = "#msClass.deleteLog(#request)", msClass = UserApiKeysController.class)
    public void deleteUserKey(@RequestBody TableBatchProcessDTO request) {
        String userId = SessionUtils.getUserId();
        for (String id : request.getSelectIds()) {
            userKeyService.checkUserKeyOwner(id, userId);
            userKeyService.deleteUserKey(id);
            
            // 记录操作日志
            LogUtils.info("用户 {} 删除了API密钥 {}", userId, id);
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "个人信息-API密钥-获取API密钥统计信息")
    public TableBatchProcessResponse getApiKeyStats() {
        String userId = SessionUtils.getUserId();
        List<UserKey> userKeys = userKeyService.getUserKeysInfo(userId);
        int count = userKeys.size();
        return new TableBatchProcessResponse(count, count);
    }

    public static String addLog() {
        return "创建API密钥";
    }

    public static String updateLog(UserKeyDTO request) {
        return "更新API密钥: " + request.getId();
    }

    public static String deleteLog(TableBatchProcessDTO request) {
        return "删除API密钥，数量: " + request.getSelectIds().size();
    }
}
