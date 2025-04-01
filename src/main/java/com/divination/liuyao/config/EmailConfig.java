package com.divination.liuyao.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * 邮件服务配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "email")
public class EmailConfig {
    
    /**
     * 发件人邮箱地址
     */
    private String from;
    
    /**
     * 发件人显示名称
     */
    private String personal;
    
} 