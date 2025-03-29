package com.divination.liuyao.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 配置异步任务执行器
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：10
        executor.setCorePoolSize(10);
        // 最大线程数：50
        executor.setMaxPoolSize(50);
        // 队列大小：1000
        executor.setQueueCapacity(1000);
        // 线程前缀名
        executor.setThreadNamePrefix("async-task-");
        // 拒绝策略：直接在调用者线程中运行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 线程空闲超时：60秒
        executor.setKeepAliveSeconds(60);
        // 初始化
        executor.initialize();
        log.info("创建异步任务执行器，核心线程数: {}, 最大线程数: {}", executor.getCorePoolSize(), executor.getMaxPoolSize());
        return executor;
    }

    /**
     * 重写getAsyncExecutor方法，提供默认的异步执行器
     */
    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }
} 