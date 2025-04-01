package com.divination.liuyao.aspect;

import com.divination.liuyao.annotation.RateLimit;
import com.divination.liuyao.exception.RateLimitException;
import com.divination.liuyao.util.RedisUtil;
import com.divination.liuyao.util.UserContextHolder;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 接口访问限流AOP实现
 */
@Slf4j
@Aspect
@Order(1)
@Component
public class RateLimitAspect {

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    
    @Autowired
    private RedisUtil redisUtil;

    @Pointcut("@annotation(com.divination.liuyao.annotation.RateLimit)")
    public void rateLimit() {
    }

    @Around("rateLimit()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        // 获取请求方法
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        // 获取注解
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return point.proceed();
        }

        // 获取当前用户ID
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            // 如果没有登录，则尝试获取请求IP地址
            userId = getUserIpAsLong();
        }

        // 构建限流的key
        String limitKey = buildLimitKey(method, rateLimit, userId);

        // 获取限流参数
        int period = rateLimit.period();
        TimeUnit timeUnit = rateLimit.timeUnit();
        int maxRequests = rateLimit.maxRequests();

        // 转换为秒
        long seconds = timeUnit.toSeconds(period);

        // 获取当前计数
        Object countObj = redisUtil.get(limitKey);
        int count = 0;
        if (countObj != null) {
            count = Integer.parseInt(countObj.toString());
        }

        // 判断是否超出限制
        if (count >= maxRequests) {
            log.warn("用户[{}]访问[{}]超出限制，限制为{}次/{}秒", userId, method.getName(), maxRequests, seconds);
            throw new RateLimitException(rateLimit.message());
        }

        // 正常访问，计数器+1
        if (count == 0) {
            // 第一次访问，设置过期时间
            redisUtil.set(limitKey, "1", (int) seconds);
        } else {
            // 非第一次访问，计数器+1
            redisUtil.incr(limitKey, 1);
        }

        // 执行原方法
        return point.proceed();
    }
    
    /**
     * 构建限流key
     */
    private String buildLimitKey(Method method, RateLimit rateLimit, Long userId) {
        StringBuilder keyBuilder = new StringBuilder(RATE_LIMIT_PREFIX);
        
        // 如果注解中指定了key，则使用指定的key
        if (!StringUtils.isEmpty(rateLimit.key())) {
            keyBuilder.append(rateLimit.key());
        } else {
            // 默认使用类名+方法名作为key
            keyBuilder.append(method.getDeclaringClass().getSimpleName())
                    .append(":")
                    .append(method.getName());
        }
        
        // 追加用户ID
        keyBuilder.append(":").append(userId);
        
        return keyBuilder.toString();
    }
    
    /**
     * 获取用户IP地址转换为Long类型
     * 用于未登录用户的限流
     */
    private Long getUserIpAsLong() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ip = getIpAddr(request);
        // 简单实现：将IP地址转换为数字
        String[] ipParts = ip.split("\\.");
        long ipNum = 0;
        for (int i = 0; i < ipParts.length; i++) {
            ipNum = ipNum * 256 + Long.parseLong(ipParts[i]);
        }
        return ipNum;
    }
    
    /**
     * 获取用户IP地址
     */
    private String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        if (ip != null && ip.indexOf(",") > 0) {
            ip = ip.substring(0, ip.indexOf(","));
        }
        return ip;
    }
} 