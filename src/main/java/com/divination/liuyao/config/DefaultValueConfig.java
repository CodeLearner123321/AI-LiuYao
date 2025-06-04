package com.divination.liuyao.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "default")
public class DefaultValueConfig {

    private String apiKey;
}
