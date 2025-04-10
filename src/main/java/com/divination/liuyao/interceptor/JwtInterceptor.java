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
import java.io.IOException;

@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {
    
    @Autowired
    private TokenUtil tokenUtil;
    
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS请求直接放行（解决CORS预检问题）
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        
        try {
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return handleAuthError(response, 302, "未提供有效的认证令牌");
            }
            
            String token = authHeader.substring(7);
            
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
            throw new AuthenticationException("无效的认证令牌", 302);
        } catch (Exception e) {
            log.error("令牌验证失败: {}", e.getMessage(), e);
            return handleAuthError(response, 302, "登录验证失败: " + e.getMessage());
        }
    }

    /**
     * 处理认证错误，返回统一格式的JSON响应
     *
     * @param response HTTP响应对象
     * @param status 状态码
     * @param message 错误信息
     * @return false 表示中断请求处理
     */
    private boolean handleAuthError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        // 构建与RespEntity格式一致的JSON响应
        String jsonResponse = "{\"code\":" + status + ",\"message\":\"" + message + "\",\"data\":null}";
        response.getWriter().write(jsonResponse);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求完成后清除ThreadLocal，防止内存泄漏
        UserContextHolder.clear();
    }
} 