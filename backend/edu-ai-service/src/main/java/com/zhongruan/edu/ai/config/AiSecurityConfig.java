package com.zhongruan.edu.ai.config;

import com.zhongruan.edu.common.security.JwtTokenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiJwtProperties.class)
public class AiSecurityConfig {
    @Bean
    JwtTokenService aiJwtTokenService(AiJwtProperties properties) {
        return new JwtTokenService(properties.secret(), properties.ttl(), properties.issuer());
    }
}

