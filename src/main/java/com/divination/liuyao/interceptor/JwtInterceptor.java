package com.divination.liuyao.interceptor;

import com.divination.liuyao.pojo.entity.User;
import com.divination.liuyao.exception.AuthenticationException;
import com.divination.liuyao.util.ConstantUtil;
import com.divination.liuyao.util.RedisUtil;
import com.divination.liuyao.util.TokenUtil;
import com.divination.liuyao.util.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {
    
    @Autowired
    private TokenUtil tokenUtil;
    
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthenticationException("未提供有效的认证令牌", 401);
        }
        
        String token = authHeader.substring(7);
        
        try {
            // 验证令牌
            if (tokenUtil.validateToken(token)) {
                Long userId = tokenUtil.extractUserId(token);
                String userName = tokenUtil.extractUsername(token);
                String deviceFingerprint = tokenUtil.extractDeviceFingerprint(token);
                
                // 从Redis检查令牌是否存在
                String tokenRedisKey = ConstantUtil.USER_REDIS_KEY + userId + deviceFingerprint;
                Object storedToken = redisUtil.get(tokenRedisKey);
                
                if (storedToken != null && token.equals(storedToken.toString())) {
                    // 令牌有效，设置用户ID到请求属性
                    request.setAttribute("userId", userId);
                    
                    // 直接从Token中创建用户对象，而不是从Redis获取
                    User user = new User();
                    user.setId(userId);
                    user.setUserName(userName);
                    user.setIsVip(tokenUtil.extractIsVip(token));

                    UserContextHolder.setUser(user);
                    return true;
                }
            }
            response.setStatus(401);
            throw new AuthenticationException("无效的认证令牌", 401);
        } catch (Exception e) {
            response.setStatus(401);
            log.error("令牌验证失败: {}", e.getMessage(), e);
            throw new AuthenticationException("登录验证失败: " + e.getMessage(), 401);
        }
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求完成后清除ThreadLocal，防止内存泄漏
        UserContextHolder.clear();
    }
} 