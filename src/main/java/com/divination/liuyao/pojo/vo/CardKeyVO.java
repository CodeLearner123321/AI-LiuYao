package com.divination.liuyao.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 卡密视图对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardKeyVO {
    
    /**
     * 卡密码
     */
    private String cardCode;
    
    /**
     * 卡密金额
     */
    private BigDecimal amount;
    
    /**
     * 卡密状态
     * 0-未使用，1-已使用
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 使用时间
     */
    private LocalDateTime useTime;
}

