package com.divination.liuyao.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.divination.liuyao.pojo.model.AiResult;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 异步任务实体类
 */
@Data
@TableName("task")
public class Task {
    
    /**
     * 任务ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 任务类型
     * LIUYAO - 六爻起卦分析
     */
    private String taskType;
    
    /**
     * 任务状态
     * PENDING - 等待处理
     * PROCESSING - 处理中
     * COMPLETED - 完成
     * FAILED - 失败
     */
    private String status;
    
    /**
     * 预扣费金额(废弃了)
     */
    private BigDecimal preAmount;
    
    /**
     * 实际扣费金额，任务完成后填写
     */
    private BigDecimal actualAmount;
    
    /**
     * 是否已扣费
     * 0 - 未扣费
     * 1 - 已扣费
     */
    private Integer isCharged;

    /**
     * 支付类型
     * 0：免费额度支付
     * 1：余额支付
     * 2：用户自定义API支付
     * @see com.divination.liuyao.pojo.enums.PaymentType
     */
    private Integer paymentType;
    
    /**
     * 请求参数JSON
     */
    private String requestParams;
    
    /**
     * 结果数据JSON
     */
    private String resultData;
    
    /**
     * 错误信息
     */
    private String errorMsg;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 完成时间
     */
    private LocalDateTime completedAt;
} 