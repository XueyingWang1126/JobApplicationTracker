package com.xueying.robot.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置分页插件
 */
@Configuration
public class MybatisPlusConfig {

        @Bean
        public MybatisPlusInterceptor mybatisPlusInterceptor() {
            return new MybatisPlusInterceptor();
        }
    }


