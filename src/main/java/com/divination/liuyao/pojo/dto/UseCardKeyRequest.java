package com.divination.liuyao.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 使用卡密请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UseCardKeyRequest {
    
    /**
     * 卡密码
     */
    @NotBlank(message = "卡密码不能为空")
    private String cardCode;
}

