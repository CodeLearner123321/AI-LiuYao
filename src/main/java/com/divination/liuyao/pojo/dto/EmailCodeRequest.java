package com.divination.liuyao.pojo.dto;

import com.divination.liuyao.util.ConstantUtil;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.util.Objects;

/**
 * 邮箱验证码请求DTO
 */
@Data
public class EmailCodeRequest {
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 请求类型，目前请求验证码有注册和修改密码的请求
     * 分别是：SIGN_IN 和 UPDATE
     */
    private String requestType;

    /**
     * 验证请求参数是否合法
     * @return 是否合法
     */
    public Boolean parameterCheck(){
        return Objects.equals(ConstantUtil.SMS_CODE_TYPE_SIGN_IN, requestType)
            || Objects.equals(ConstantUtil.SMS_CODE_TYPE_UPDATE, requestType);
    }
} 