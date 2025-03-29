package com.divination.liuyao.util;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class TokenUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.token-expiration}")
    private Long tokenExpiration; // 令牌过期时间，单位秒

    /**
     * 生成JWT令牌
     * @param username 用户名
     * @param userId 用户ID
     * @param deviceFingerprint 设备指纹
     * @param isVip 是否为VIP用户
     * @return JWT令牌
     */
    public String generateToken(String username, Long userId, String deviceFingerprint, Integer isVip) {
        DateTime now = DateTime.now();
        DateTime expireTime = now.offsetNew(DateField.SECOND, tokenExpiration.intValue());
        
        Map<String, Object> payload = new HashMap<>();
        // 签发时间
        payload.put(JWTPayload.ISSUED_AT, now);
        // 过期时间
        payload.put(JWTPayload.EXPIRES_AT, expireTime);
        // 生效时间
        payload.put(JWTPayload.NOT_BEFORE, now);
        // 主题
        payload.put(JWTPayload.SUBJECT, username);
        // 自定义数据
        payload.put("userId", userId);
        payload.put("deviceFingerprint", deviceFingerprint);
        payload.put("isVip", isVip);
        
        return JWTUtil.createToken(payload, secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 验证令牌
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            // 验证签名
            boolean verify = jwt.setKey(secret.getBytes(StandardCharsets.UTF_8)).verify();
            // 验证是否过期
            boolean notExpired = jwt.validate(0);
            return verify && notExpired;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从令牌中提取用户名
     * @param token JWT令牌
     * @return 用户名
     */
    public String extractUsername(String token) {
        JWT jwt = JWTUtil.parseToken(token);
        return (String) jwt.getPayload(JWTPayload.SUBJECT);
    }

    /**
     * 从令牌中提取用户ID
     * @param token JWT令牌
     * @return 用户ID
     */
    public Long extractUserId(String token) {
        JWT jwt = JWTUtil.parseToken(token);
        return Long.valueOf(jwt.getPayload("userId").toString());
    }

    /**
     * 从令牌中提取设备指纹
     * @param token JWT令牌
     * @return 设备指纹
     */
    public String extractDeviceFingerprint(String token) {
        JWT jwt = JWTUtil.parseToken(token);
        return (String) jwt.getPayload("deviceFingerprint");
    }

    /**
     * 从令牌中提取VIP状态
     * @param token JWT令牌
     * @return VIP状态，1表示是VIP，0表示不是VIP
     */
    public Integer extractIsVip(String token) {
        JWT jwt = JWTUtil.parseToken(token);
        Object isVipObj = jwt.getPayload("isVip");
        if (isVipObj == null) {
            return 0; // 默认不是VIP
        }
        return Integer.valueOf(isVipObj.toString());
    }
}
