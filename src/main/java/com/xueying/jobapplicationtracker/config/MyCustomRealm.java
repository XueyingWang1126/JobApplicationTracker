package com.xueying.jobapplicationtracker.config;

import com.xueying.jobapplicationtracker.entity.User;
import com.xueying.jobapplicationtracker.service.UserService;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.AuthenticatingRealm;

/**
 * Authenticates users against app_users table by email and password hash.
 */
public class MyCustomRealm extends AuthenticatingRealm {
    private final UserService userService;

    public MyCustomRealm(UserService userService) {
        this.userService = userService;
    }

    @Override
    /**
     * Validates submitted credentials and returns principal on success.
     */
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken userToken = (UsernamePasswordToken) token;
        String email = userToken.getUsername();
        User user = userService.findByEmail(email);
        if (user == null) {
            throw new AuthenticationException("Unknown account");
        }
        String submitted = userToken.getPassword() == null ? "" : new String(userToken.getPassword());
        if (!userService.verifyPassword(user, submitted)) {
            throw new AuthenticationException("Invalid credentials");
        }
        return new SimpleAuthenticationInfo(user.getEmail(), submitted.toCharArray(), getName());
    }
}

