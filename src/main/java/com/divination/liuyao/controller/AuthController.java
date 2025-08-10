package com.divination.liuyao.controller;

import com.divination.liuyao.annotation.RateLimit;
import com.divination.liuyao.pojo.dto.EmailCodeRequest;
import com.divination.liuyao.pojo.dto.LoginRequest;
import com.divination.liuyao.pojo.dto.LoginResponse;
import com.divination.liuyao.pojo.dto.RegisterRequest;
import com.divination.liuyao.pojo.dto.SmsCodeRequest;
import com.divination.liuyao.pojo.dto.UpdatePasswordRequest;
import com.divination.liuyao.result.RespEntity;
import com.divination.liuyao.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    

    /**
     * 注册
     */
    @PostMapping("/register")
    public RespEntity<String> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return authService.register(registerRequest);
    }
    

    /**
     * 登录
     */
    @PostMapping("/login")
    public RespEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public RespEntity<Void> logout(@RequestParam Long userId, @RequestParam String deviceFingerprint) {
        authService.logout(userId, deviceFingerprint);
        return RespEntity.ok();
    }
    
    /**
     * 发送短信验证码
     * 用于注册或其他操作
     * 废弃接口 前端已不访问
     */
    @PostMapping("/sms/code")
    public RespEntity<String> sendSmsCode(@Valid @RequestBody SmsCodeRequest request) {
        return authService.sendSmsCode(request);
    }
    
    /**
     * 发送邮箱验证码
     * 用于注册或其他操作
     * 限制：每5分钟内最多发送3次验证码
     */
    @PostMapping("/email/code")
    @RateLimit(period = 300, timeUnit = TimeUnit.SECONDS, maxRequests = 3, message = "验证码发送过于频繁，请5分钟后再试")
    public RespEntity<String> sendEmailCode(@Valid @RequestBody EmailCodeRequest request) {
        return authService.sendEmailCode(request);
    }

    /**
     * 返回用户余额
     * Redis缓存优化，过期时间1分钟
     */
    @GetMapping("/get/balance")
    public RespEntity<String> getBalance() {
        return authService.getBalance();
    }

    /**
     * 修改密码
     * 通过邮箱验证码验证后修改密码
     * 限制：每小时最多尝试5次修改密码
     */
    @PostMapping("/update/password")
    @RateLimit(period = 1, timeUnit = TimeUnit.HOURS, maxRequests = 5, message = "密码修改请求过于频繁，请稍后再试")
    public RespEntity<String> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        return authService.updatePassword(request);
    }



}