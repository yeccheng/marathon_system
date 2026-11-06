package com.ape.marathon.shiro;

import com.ape.marathon.entity.Admin;
import com.ape.marathon.mapper.AdminMapper;
import com.ape.marathon.util.JwtUtil;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 自定义 Realm：专门校验 JWT token
 * 认证流程：JwtFilter 取出 Header 中的 Bearer token → Subject.login(JwtToken)
 *          → 本 Realm.doGetAuthenticationInfo 解析并校验 token / 用户是否存在
 */
@Component
public class JwtRealm extends AuthorizingRealm {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AdminMapper adminMapper;

    /**
     * 只处理 JwtToken 类型的凭证
     */
    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    /**
     * 授权（本项目暂不做角色/权限细分，返回空授权信息即可）
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        return new SimpleAuthorizationInfo();
    }

    /**
     * 认证：解析 JWT，校验是否过期，并确认用户仍存在于 admin 表
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken)
            throws AuthenticationException {
        String token = (String) authenticationToken.getCredentials();
        if (token == null || token.trim().isEmpty()) {
            throw new AuthenticationException("缺少 token");
        }

        // 校验签名与过期
        if (!jwtUtil.validateToken(token)) {
            throw new AuthenticationException("token 无效或已过期");
        }

        String username = jwtUtil.getUsername(token);
        if (username == null || username.trim().isEmpty()) {
            throw new AuthenticationException("token 中无用户信息");
        }

        // 确认用户仍存在（账号被删后旧 token 也应失效）
        Admin admin = adminMapper.selectByUsername(username);
        if (admin == null) {
            throw new AuthenticationException("用户不存在");
        }

        // principal / credentials 均使用 token，realmName 用本类名
        return new SimpleAuthenticationInfo(token, token, getName());
    }
}
