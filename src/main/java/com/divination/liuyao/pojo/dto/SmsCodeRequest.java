package com.divination.liuyao.pojo.dto;

import com.divination.liuyao.util.ConstantUtil;
import java.util.Objects;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 短信验证码请求DTO
 */
@Data
public class SmsCodeRequest {
    
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phoneNumber;

    /**
     * 请求类型，目前请求验证码有注册和修改密码的请求
     * 分别是：SIGN_IN 和 UPDATE
     */
    private String requestType;


    public Boolean parameterCheck(){
        return Objects.equals(ConstantUtil.SMS_CODE_TYPE_SIGN_IN, requestType)
            || Objects.equals(ConstantUtil.SMS_CODE_TYPE_UPDATE, requestType);
    }
} 