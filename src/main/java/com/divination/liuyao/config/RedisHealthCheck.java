package com.divination.liuyao.config;

import com.divination.liuyao.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
public class RedisHealthCheck {
    
    @Autowired
    private RedisUtil redisUtil;
    
    private static final String HEALTH_CHECK_KEY = "health:check";
}