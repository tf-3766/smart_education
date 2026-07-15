package com.zhongruan.edu.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@MapperScan(basePackages = "com.zhongruan.edu.biz", annotationClass = Mapper.class)
@EnableScheduling
public class BizServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BizServiceApplication.class, args);
    }
}
