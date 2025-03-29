package com.divination.liuyao.service;

import com.divination.liuyao.pojo.dto.LoginRequest;
import com.divination.liuyao.pojo.dto.LoginResponse;
import com.divination.liuyao.pojo.dto.RegisterRequest;
import com.divination.liuyao.pojo.dto.SmsCodeRequest;
import com.divination.liuyao.pojo.dto.UpdatePasswordRequest;
import com.divination.liuyao.pojo.entity.User;
import com.divination.liuyao.exception.AuthenticationException;
import com.divination.liuyao.result.RespEntity;
import com.divination.liuyao.util.ConstantUtil;
import com.divination.liuyao.util.PasswordUtil;
import com.divination.liuyao.util.RedisUtil;
import com.divination.liuyao.util.TokenUtil;
import com.divination.liuyao.util.UserContextHolder;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
public class AuthService {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordUtil passwordUtil;
    
    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private RedisUtil redisUtil;

    public RespEntity<String> register(RegisterRequest registerRequest) {
        try {
            String phoneNumber = registerRequest.getPhoneNumber();
            String code = registerRequest.getAuthCode();
            
            // 验证短信验证码
            String redisKey = ConstantUtil.SMS_CODE_KEY + ConstantUtil.SMS_CODE_TYPE_SIGN_IN + phoneNumber;
            Object storedCode = redisUtil.get(redisKey);
            
            if (storedCode == null || !code.equals(storedCode.toString())) {
                log.warn("注册验证码错误或已过期，手机号: {}", phoneNumber);
                return RespEntity.error("验证码错误或已过期（验证码有效期为1分钟）");
            }

            // 检查用户名是否已存在
            if (userService.findByPhoneNumber(phoneNumber).isPresent()) {
                return RespEntity.error("该手机号已注册");
            }
            
            // 检查账号是否已存在
            if (userService.findByUserName(registerRequest.getUserName()).isPresent()) {
                return RespEntity.error("该账号已被使用");
            }
            
            // 创建新用户
            User user = new User();
            user.setPhoneNumber(phoneNumber);
            user.setUserName(registerRequest.getUserName());

            // 生成盐值并哈希密码
            String salt = passwordUtil.generateSalt();
            String hashedPassword = passwordUtil.hashPassword(registerRequest.getPassWord(), salt);
            
            user.setSalt(salt);
            user.setPassWord(hashedPassword);
            user.setIsVip(0); // 默认非VIP用户
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            // 保存用户
            userService.save(user);
            
            // 使用后删除验证码
            redisUtil.del(redisKey);
            
            log.info("用户注册成功，用户名: {}", user.getUserName());
            return RespEntity.ok("注册成功");

        } catch (Exception e) {
            log.error("注册失败: {}", e.getMessage(), e);
            return RespEntity.error(e.getMessage());
        }
    }

    public RespEntity<LoginResponse> login(LoginRequest loginRequest) {
        try {
            // 根据用户名查询用户
            Optional<User> userOptional = userService.findByUserName(loginRequest.getUsername());
            if (!userOptional.isPresent()) {
                return RespEntity.error(HttpStatus.UNAUTHORIZED.value(), "用户名或密码错误");
            }

            User user = userOptional.get();
            
            // 验证密码
            boolean isPasswordValid = passwordUtil.verifyPassword(
                    loginRequest.getPassword(), 
                    user.getSalt(), 
                    user.getPassWord()
            );
            
            if (!isPasswordValid) {
                return RespEntity.error(HttpStatus.UNAUTHORIZED.value(), "用户名或密码错误");
            }
            
            // 生成令牌
            String deviceFingerprint = loginRequest.getDeviceFingerprint() != null ? 
                    loginRequest.getDeviceFingerprint() : ConstantUtil.DEFAULT_DEVICE_FINGERPRINT;
            String token = tokenUtil.generateToken(
                    user.getUserName(),
                    user.getId(),
                    deviceFingerprint,
                    user.getIsVip()
            );
            
            // 将令牌信息存储到Redis
            String redisKey = ConstantUtil.USER_REDIS_KEY + user.getId() + ConstantUtil.DEFAULT_DEVICE_FINGERPRINT;
            redisUtil.set(redisKey, token, 7 * 24 * 60 * 60); // 设置7天过期

            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setUsername(loginRequest.getUsername());
            loginResponse.setPhone(user.getEncryptedPhoneNumber());
            loginResponse.setToken(token);

            log.debug("用户登录UserId: {}", user.getId());
            return RespEntity.ok(loginResponse);
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage(), e);
            return RespEntity.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }
    }
    
    public void logout(Long userId, String deviceFingerprint) {
        String redisKey = ConstantUtil.USER_REDIS_KEY + userId + ConstantUtil.DEFAULT_DEVICE_FINGERPRINT;
        redisUtil.del(redisKey);
    }
    
    // 验证令牌并获取用户ID
    public Long validateTokenAndGetUserId(String token) {
        if (!tokenUtil.validateToken(token)) {
            throw new AuthenticationException("令牌已过期或无效", 401);
        }
        
        return tokenUtil.extractUserId(token);
    }
    
    /**
     * 发送短信验证码
     * 1. 验证码过期时间为1分钟
     * 2. 对于同一个手机号，不同的请求类型，一天最多支持发送3次
     */
    public RespEntity<String> sendSmsCode(SmsCodeRequest smsCodeRequest) {
        // 参数校验
        if(!smsCodeRequest.parameterCheck()){
            return RespEntity.error("参数有误!");
        }

        try {
            String phoneNumber = smsCodeRequest.getPhoneNumber();
            String requestType = smsCodeRequest.getRequestType();
            
            // 验证码的Redis键
            String redisKey = ConstantUtil.SMS_CODE_KEY + requestType + phoneNumber;
            
            // 检查是否已经发送过验证码且尚未过期
            if(redisUtil.hasKey(redisKey)){
                return RespEntity.error("验证码已发送，请勿重复操作");
            }
            
            // 检查当天发送次数限制
            // 计数器键格式：SMS_COUNTER:请求类型:手机号:日期(yyyyMMdd)
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String counterKey = ConstantUtil.SMS_COUNTER_KEY + requestType + ":" + phoneNumber + ":" + today;
            
            // 获取当前计数
            Object countObj = redisUtil.get(counterKey);
            int count = 0;
            if (countObj != null) {
                count = Integer.parseInt(countObj.toString());
            }
            
            // 检查是否超过每日最大发送次数
            if (count >= ConstantUtil.SMS_MAX_DAILY_COUNT) {
                return RespEntity.error("今日验证码发送次数已达上限，请明天再试");
            }
            
            // 生成6位随机验证码
            String code = String.format("%06d", (int)(Math.random() * 1000000));
            log.info("向手机号 {} 发送验证码: {} 业务类型：{}", phoneNumber, code, requestType);
            
            // TODO: 调用短信API发送验证码，这里省略实现
            
            // 设置验证码，过期时间为1分钟
            redisUtil.set(redisKey, code, ConstantUtil.SMS_CODE_EXPIRE_TIME);
            
            // 增加计数器并设置过期时间（当天剩余时间）
            long secondsLeftToday = calculateSecondsUntilEndOfDay();
            // TODO: 这里好像有一个报错，有空看看
            redisUtil.set(counterKey, count + 1, secondsLeftToday);
            
            return RespEntity.ok("验证码发送成功");
        } catch (Exception e) {
            log.error("发送短信验证码失败: {}", e.getMessage(), e);
            throw new AuthenticationException("验证码发送失败: " + e.getMessage(), 500);
        }
    }
    
    /**
     * 计算当天剩余秒数
     * @return 当天剩余秒数
     */
    private long calculateSecondsUntilEndOfDay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfDay = now.toLocalDate().atTime(23, 59, 59);
        return java.time.Duration.between(now, endOfDay).getSeconds() + 1; // 加1秒确保包含23:59:59
    }

    public RespEntity<String> getBalance() {
        Long userId = UserContextHolder.getUserId();
        Optional<User> byId = userService.findById(userId);
        return byId.map(user -> RespEntity.ok(user.getBalance().toString())).orElseGet(() -> RespEntity.error("该用户不存在"));
    }

    /**
     * 修改密码
     * 通过验证码验证后修改密码
     * @param request 修改密码请求，包含手机号、新密码和验证码
     * @return 结果，成功时返回用户账号
     */
    public RespEntity<String> updatePassword(UpdatePasswordRequest request) {
        try {
            String phoneNumber = request.getPhoneNumber();
            String authCode = request.getAuthCode();
            String newPassword = request.getNewPassword();
            
            // 验证短信验证码
            String redisKey = ConstantUtil.SMS_CODE_KEY + ConstantUtil.SMS_CODE_TYPE_UPDATE + phoneNumber;
            Object storedCode = redisUtil.get(redisKey);
            
            if (storedCode == null || !authCode.equals(storedCode.toString())) {
                log.warn("修改密码验证码错误或已过期，手机号: {}", phoneNumber);
                return RespEntity.error("验证码错误或已过期（验证码有效期为1分钟）");
            }
            
            // 根据手机号查找用户
            Optional<User> userOptional = userService.findByPhoneNumber(phoneNumber);
            if (!userOptional.isPresent()) {
                return RespEntity.error("该手机号未注册");
            }
            
            User user = userOptional.get();
            
            // 生成新的盐值和密码哈希
            String salt = passwordUtil.generateSalt();
            String hashedPassword = passwordUtil.hashPassword(newPassword, salt);
            
            // 更新用户密码
            user.setSalt(salt);
            user.setPassWord(hashedPassword);
            user.setUpdatedAt(LocalDateTime.now());
            userService.save(user);
            
            // 使用后删除验证码
            redisUtil.del(redisKey);
            
            log.info("用户密码修改成功，用户名: {}", user.getUserName());
            
            // 返回用户账号
            return RespEntity.ok(user.getUserName());
        } catch (Exception e) {
            log.error("修改密码失败: {}", e.getMessage(), e);
            return RespEntity.error("修改密码失败: " + e.getMessage());
        }
    }
}