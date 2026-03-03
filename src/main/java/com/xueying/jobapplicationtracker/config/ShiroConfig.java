package com.xueying.jobapplicationtracker.config;

import com.xueying.jobapplicationtracker.service.UserService;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central Shiro web security configuration.
 */
@Configuration
public class ShiroConfig {

    @Bean
    public MyCustomRealm myCustomRealm(UserService userService) {
        return new MyCustomRealm(userService);
    }

    @Bean
    public DefaultWebSecurityManager securityManager(MyCustomRealm myCustomRealm) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(myCustomRealm);
        return securityManager;
    }

    @Bean(name = "jobTrackerShiroFilter")
    public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager) {
        ShiroFilterFactoryBean bean = new ShiroFilterFactoryBean();
        bean.setSecurityManager(securityManager);

        Map<String, String> chain = new LinkedHashMap<>();
        chain.put("/login", "anon");
        chain.put("/register", "anon");
        chain.put("/css/**", "anon");
        chain.put("/js/**", "anon");
        chain.put("/images/**", "anon");
        chain.put("/error", "anon");
        chain.put("/logout", "logout");
        chain.put("/**", "authc");

        bean.setLoginUrl("/login");
        bean.setSuccessUrl("/dashboard");
        bean.setFilterChainDefinitionMap(chain);
        return bean;
    }

    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }
}

