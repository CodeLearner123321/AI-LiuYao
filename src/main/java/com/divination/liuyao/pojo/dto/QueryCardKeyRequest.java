package com.divination.liuyao.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 查询卡密请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryCardKeyRequest {
    
    /**
     * 卡密状态
     * 0-未使用，1-已使用
     * 可选参数，为null时查询所有状态
     */
    private Integer status;
    
    /**
     * 卡密金额
     * 可选参数，为null时查询所有金额
     */
    private BigDecimal amount;
}

