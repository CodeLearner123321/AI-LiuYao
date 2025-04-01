package com.divination.liuyao.pojo.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class RegisterRequest {

    /**
     * 手机号（非必填）
     */
    private String phoneNumber;

    /**
     * 邮箱（必填）
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    /**
     * 账号，只能包含大小写英文、数字和点(.)
     */
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9.]+$", message = "用户名只能包含大小写英文、数字和点")
    @Size(min = 4, max = 20, message = "用户名长度必须在4-20个字符之间")
    private String userName;


    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String passWord;

    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码长度必须为6位")
    private String authCode;
    
}