package com.divination.liuyao.controller;

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
     * 废弃接口
     */
    @PostMapping("/sms/code")
    public RespEntity<String> sendSmsCode(@Valid @RequestBody SmsCodeRequest request) {
        return authService.sendSmsCode(request);
    }
    
    /**
     * 发送邮箱验证码
     * 用于注册或其他操作
     */
    @PostMapping("/email/code")
    public RespEntity<String> sendEmailCode(@Valid @RequestBody EmailCodeRequest request) {
        return authService.sendEmailCode(request);
    }

    /**
     * 返回用户余额
     */
    @GetMapping("/get/balance")
    public RespEntity<String> getBalance() {
        return authService.getBalance();
    }

    /**
     * 修改密码
     * 通过验证码验证后修改密码
     */
    @PostMapping("/update/password")
    public RespEntity<String> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        return authService.updatePassword(request);
    }
} 