package io.vanguard.testops.system.controller;

import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.system.dto.request.user.PersonalLocaleRequest;
import io.vanguard.testops.system.dto.request.user.PersonalUpdatePasswordRequest;
import io.vanguard.testops.system.dto.request.user.PersonalUpdateRequest;
import io.vanguard.testops.system.dto.user.PersonalDTO;
import io.vanguard.testops.system.log.annotation.Log;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.service.SimpleUserService;
import io.vanguard.testops.system.service.UserLogService;
import io.vanguard.testops.system.utils.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "个人中心")
@RequestMapping("/personal")
public class PersonalCenterController {

    @Resource
    private SimpleUserService simpleUserService;

    @GetMapping("/get/{id}")
    @Operation(summary = "个人中心-获取信息")
    public PersonalDTO getInformation(@PathVariable String id) {
        this.checkPermission(id);
        return simpleUserService.getPersonalById(id);
    }

    @PostMapping("/update-info")
    @Operation(summary = "个人中心-修改信息")
    @Log(type = OperationLogType.UPDATE, expression = "#msClass.updateAccountLog(#request)", msClass = UserLogService.class)
    public boolean updateUser(@Validated @RequestBody PersonalUpdateRequest request) {
        this.checkPermission(request.getId());
        return simpleUserService.updateAccount(request, SessionUtils.getUserId());
    }

    @PostMapping("/update-locale")
    @Operation(summary = "个人中心-修改信息")
    public void updateLocale(@Validated @RequestBody PersonalLocaleRequest request) {
        simpleUserService.updateLanguage(request, SessionUtils.getUserId());
    }

    @PostMapping("/update-password")
    @Operation(summary = "个人中心-修改密码")
    @Log(type = OperationLogType.UPDATE, expression = "#msClass.updatePasswordLog(#request)", msClass = UserLogService.class)
    public String updateUser(@Validated @RequestBody PersonalUpdatePasswordRequest request) {
        this.checkPermission(request.getId());
        if (simpleUserService.updatePassword(request)) {
            SessionUtils.kickOutUser(SessionUtils.getUser().getId());
        }
        return "OK";
    }

    private void checkPermission(String id) {
        if (!StringUtils.equals(id, SessionUtils.getUserId())) {
            throw new MSException("personal.no.permission");
        }
    }

}
