package com.divination.liuyao.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 卡密实体类
 * 用于管理充值卡密的生成和使用
 */
@Data
@TableName("card_key")
public class CardKey {
    
    /**
     * 卡密ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 卡密码
     * 唯一标识，用于充值
     */
    private String cardCode;
    
    /**
     * 卡密金额
     */
    private BigDecimal amount;
    
    /**
     * 生成该卡密的用户ID
     */
    private Long creatorId;
    
    /**
     * 使用该卡密的用户ID
     * 未使用时为null
     */
    private Long userId;
    
    /**
     * 卡密状态
     * 0-未使用，1-已使用
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 使用时间
     */
    private LocalDateTime useTime;
}

