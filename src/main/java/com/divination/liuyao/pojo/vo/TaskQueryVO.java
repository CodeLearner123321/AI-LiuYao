package com.divination.liuyao.pojo.vo;

import com.divination.liuyao.pojo.model.AiResult;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 任务查询结果VO
 */
@Data
public class TaskQueryVO {
    
    /**
     * 任务ID
     */
    private Long taskId;
    
    /**
     * 任务状态
     */
    private String status;
    
    /**
     * 任务是否完成
     */
    private boolean completed;
    
    /**
     * 任务结果数据
     */
    private JsonNode data;

    /**
     * 支付金额(新增的)
     */
    private BigDecimal price;

    /**
     * 支付类型 (新增的)
     * 0：免费额度支付
     * 1：余额支付
     * 2：用户自定义API支付
     * @see com.divination.liuyao.pojo.enums.PaymentType
     */
    private Integer paymentType;

    /**
     * 错误信息
     */
    private String error;
} 