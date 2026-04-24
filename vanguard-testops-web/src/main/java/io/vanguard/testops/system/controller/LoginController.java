package io.vanguard.testops.system.controller;


import io.vanguard.testops.project.domain.Project;
import io.vanguard.testops.sdk.constants.HttpMethodConstants;
import io.vanguard.testops.sdk.constants.UserSource;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.sdk.util.RsaKey;
import io.vanguard.testops.sdk.util.RsaUtils;
import io.vanguard.testops.sdk.util.Translator;
import io.vanguard.testops.system.controller.handler.ResultHolder;
import io.vanguard.testops.system.controller.handler.result.MsHttpResultCode;
import io.vanguard.testops.system.dto.sdk.LoginRequest;
import io.vanguard.testops.system.dto.sdk.SessionUser;
import io.vanguard.testops.system.dto.user.UserDTO;
import io.vanguard.testops.system.log.constants.OperationLogType;
import io.vanguard.testops.system.service.UserLoginService;
import io.vanguard.testops.system.utils.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@Tag(name = "登录")
public class LoginController {

    @Resource
    private UserLoginService userLoginService;
    @Resource
    private RedisIndexedSessionRepository redisIndexedSessionRepository;
    @Value("${spring.messages.default-locale}")
    private String defaultLocale;


    @GetMapping(value = "/is-login")
    @Operation(summary = "是否登录")
    public ResultHolder isLogin(HttpServletResponse response) throws Exception {
        SessionUser user = SessionUtils.getUser();
        if (user != null) {
            UserDTO userDTO = userLoginService.getUserDTO(user.getId());
            if (StringUtils.isBlank(userDTO.getLanguage())) {
                userDTO.setLanguage(defaultLocale.replace("_", "-"));
            }

            userLoginService.autoSwitch(userDTO);
            SessionUser sessionUser = SessionUser.fromUser(userDTO, SessionUtils.getSessionId());
            SessionUtils.putUser(sessionUser);
            // 用户只有工作空间权限, 或者项目已被禁用
            Project lastProject = userLoginService.getProjectById(sessionUser.getLastProjectId());
            if (StringUtils.isBlank(sessionUser.getLastProjectId()) || lastProject == null || !lastProject.getEnable()) {
                sessionUser.setLastProjectId("no_such_project");
            }
            return ResultHolder.success(sessionUser);
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        return ResultHolder.error(MsHttpResultCode.UNAUTHORIZED.getCode(), null);
    }

    @GetMapping(value = "/get-key")
    @Operation(summary = "获取公钥")
    public ResultHolder getKey() throws Exception {
        RsaKey rsaKey = RsaUtils.getRsaKey();
        return ResultHolder.success(rsaKey.getPublicKey());
    }

    @PostMapping(value = "/login")
    @Operation(summary = "登录")
    public ResultHolder login(@Validated @RequestBody LoginRequest request) {
        SessionUser sessionUser = SessionUtils.getUser();
        if (sessionUser != null) {
            if (!StringUtils.equals(sessionUser.getId(), request.getUsername())) {
                throw new MSException(Translator.get("please_logout_current_user"));
            }
        }
        SecurityUtils.getSubject().getSession().setAttribute("authenticate", UserSource.LOCAL.name());
        ResultHolder result = userLoginService.login(request);
        // 检查管理员是否需要改密码
        boolean changePassword = userLoginService.checkWhetherChangePasswordOrNot(request);
        result.setMessage(BooleanUtils.toStringTrueFalse(changePassword));
        return result;
    }

    @GetMapping(value = "/signout")
    @Operation(summary = "退出登录")
    public ResultHolder logout() throws Exception {
        if (SessionUtils.getUser() == null) {
            return ResultHolder.success("logout success");
        }
        userLoginService.saveLog(SessionUtils.getUserId(), HttpMethodConstants.GET.name(), "/signout", "登出成功", OperationLogType.LOGOUT.name());
        String sessionId = SessionUtils.getSessionId();
        if (sessionId != null && redisIndexedSessionRepository != null) {
            redisIndexedSessionRepository.deleteById(sessionId);
        }
        SecurityUtils.getSubject().logout();
        return ResultHolder.success("logout success");
    }

}
