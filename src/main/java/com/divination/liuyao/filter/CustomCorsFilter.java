package com.divination.liuyao.filter;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义CORS过滤器，同时实现流量控制
 * 使用Guava Cache实现内存级别的请求计数和限流
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CustomCorsFilter implements Filter {


    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    // 全局限流器，限制系统总体QPS
    private final RateLimiter globalRateLimiter = RateLimiter.create(100.0); // 每秒最多100个请求
    
    // IP访问计数缓存，记录短时间内IP的访问次数
    // 设置过期时间为1分钟，最大容量为10000个IP
    private final Cache<String, AtomicInteger> ipCountCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();
    
    // IP黑名单缓存，记录被封禁的IP
    // 设置过期时间为1小时，最大容量为1000个IP
    private final Cache<String, Boolean> blacklistCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(1000)
            .build();
    
    // IP限流器映射，为每个IP分配独立的限流器
    private final Map<String, RateLimiter> ipRateLimiters = new ConcurrentHashMap<>();
    
    // 配置参数
    private static final int IP_RATE_LIMIT = 100; // 每个IP每秒最多请求数
    private static final int MAX_REQUESTS_PER_MINUTE = 60; // 每分钟最大请求数
    private static final String[] WHITELIST_IPS = {""}; // IP白名单

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;
        
        // 设置CORS响应头
        if (!configureCorsHeaders(request, response)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        // 如果是预检请求，直接返回200状态码
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        
        // 获取客户端IP
        String clientIp = getClientIp(request);
        
        // 检查IP是否在白名单中，如果是则跳过流量控制
        if (isWhitelisted(clientIp)) {
            chain.doFilter(req, res);
            return;
        }
        
        // 检查IP是否在黑名单中
        if (isBlacklisted(clientIp)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("请求频率过高，请稍后再试");
            return;
        }
        
        // 全局限流检查
        if (!globalRateLimiter.tryAcquire()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("系统繁忙，请稍后再试");
            return;
        }
        
        // 针对IP的限流检查
        RateLimiter ipRateLimiter = ipRateLimiters.computeIfAbsent(clientIp, 
                k -> RateLimiter.create(IP_RATE_LIMIT));
        if (!ipRateLimiter.tryAcquire()) {
            // 增加IP访问计数
            incrementIpCount(clientIp);
            
            // 检查是否需要加入黑名单
            if (shouldBlacklist(clientIp)) {
                // 加入黑名单
                addToBlacklist(clientIp);
                log.warn("IP {} 已被加入黑名单，访问频率过高", clientIp);
            }

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("请求频率过高，请稍后再试");
            return;
        }
        
        // 增加IP访问计数
        incrementIpCount(clientIp);
        
        // 继续处理请求
        chain.doFilter(req, res);
    }



    /**
     * 配置CORS响应头
     */
    private boolean configureCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            return true;
        }

        Set<String> configuredOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
        if (!configuredOrigins.contains(origin)) {
            log.warn("Rejected CORS request from origin={}", origin);
            return false;
        }

        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Vary", "Origin");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization");
        return true;
    }
    
    /**
     * 获取客户端真实IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 如果是多级代理，取第一个IP地址
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
    
    /**
     * 检查IP是否在白名单中
     */
    private boolean isWhitelisted(String ip) {
        for (String whitelistedIp : WHITELIST_IPS) {
            if (whitelistedIp.equals(ip)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查IP是否在黑名单中
     */
    private boolean isBlacklisted(String ip) {
        Boolean isBlacklisted = blacklistCache.getIfPresent(ip);
        return isBlacklisted != null && isBlacklisted;
    }
    
    /**
     * 增加IP访问计数
     */
    private void incrementIpCount(String ip) {
        try {
            AtomicInteger counter = ipCountCache.get(ip, () -> new AtomicInteger(0));
            counter.incrementAndGet();
        } catch (Exception e) {
            log.error("增加IP访问计数失败", e);
        }
    }
    
    /**
     * 判断是否应该将IP加入黑名单
     * 当IP在1分钟内的请求次数超过阈值时，将其加入黑名单
     */
    private boolean shouldBlacklist(String ip) {
        AtomicInteger counter = ipCountCache.getIfPresent(ip);
        return counter != null && counter.get() > MAX_REQUESTS_PER_MINUTE;
    }
    
    /**
     * 将IP加入黑名单
     */
    private void addToBlacklist(String ip) {
        blacklistCache.put(ip, true);
        // 同时移除该IP的限流器，释放资源
        ipRateLimiters.remove(ip);
    }
    
    /**
     * 定期清理过期的限流器，防止内存泄漏
     * 此方法可以通过定时任务调用
     */
    public void cleanupExpiredRateLimiters() {
        // 移除超过1小时未使用的限流器
        ipRateLimiters.entrySet().removeIf(entry -> 
            blacklistCache.getIfPresent(entry.getKey()) == null && 
            ipCountCache.getIfPresent(entry.getKey()) == null);
    }
}
