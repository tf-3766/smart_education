package com.zhongruan.edu.biz.storage.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileStorageConfiguration {}
