package io.vanguard.testops.system.security.realm;

import io.vanguard.testops.sdk.constants.UserSource;
import io.vanguard.testops.sdk.exception.MSException;
import io.vanguard.testops.system.domain.User;
import io.vanguard.testops.system.dto.sdk.SessionUser;
import io.vanguard.testops.system.mapper.UserMapper;
import io.vanguard.testops.system.service.UserLoginService;
import io.vanguard.testops.system.utils.SessionUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;

/**
 * 飞书认证Realm
 */
@Component
public class LarkRealm extends AuthorizingRealm {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserLoginService userLoginService;

    @Override
    public boolean supports(AuthenticationToken token) {
        // 支持飞书认证
        return token instanceof UsernamePasswordToken;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        try {
            // 获取当前Session中的认证来源
            Session session = SecurityUtils.getSubject().getSession();
            String login = (String) session.getAttribute("authenticate");

            // 只有飞书认证才走这个Realm
            if (!StringUtils.equals(login, UserSource.LARK.name())) {
                return null;
            }

            UsernamePasswordToken token = (UsernamePasswordToken) authenticationToken;
            String username = StringUtils.trim(token.getUsername());

            if (StringUtils.isBlank(username)) {
                throw new AuthenticationException("用户名不能为空");
            }

            // 查找用户
            User user = userMapper.selectByPrimaryKey(username);
            if (user == null) {
                throw new UnknownAccountException("用户不存在");
            }

            // 检查用户是否启用
            if (!user.getEnable()) {
                throw new DisabledAccountException("用户已被禁用");
            }

            // 检查用户是否删除
            if (user.getDeleted()) {
                throw new UnknownAccountException("用户已被删除");
            }

            // 检查用户来源
            if (!StringUtils.equals(user.getSource(), UserSource.LARK.name())) {
                throw new AuthenticationException("用户来源不匹配");
            }

            // 创建认证信息
            return new SimpleAuthenticationInfo(
                    username,
                    user.getPassword(),
                    getName()
            );

        } catch (Exception e) {
            throw new AuthenticationException("飞书认证失败: " + e.getMessage());
        }
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        try {
            String username = (String) principals.getPrimaryPrincipal();

            if (StringUtils.isBlank(username)) {
                return null;
            }

            // 获取用户信息
            User user = userMapper.selectByPrimaryKey(username);
            if (user == null || !user.getEnable() || user.getDeleted()) {
                return null;
            }

            // 创建授权信息
            SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();

            // 设置角色和权限（这里可以根据实际需求扩展）
            Set<String> roles = new HashSet<>();
            Set<String> permissions = new HashSet<>();

            // 添加默认角色
            roles.add("user");

            // 如果是系统管理员
            if ("admin".equals(username)) {
                roles.add("admin");
                permissions.add("*:*:*");
            }

            authorizationInfo.setRoles(roles);
            authorizationInfo.setStringPermissions(permissions);

            return authorizationInfo;

        } catch (Exception e) {
            throw new MSException("获取用户权限失败: " + e.getMessage());
        }
    }
}
