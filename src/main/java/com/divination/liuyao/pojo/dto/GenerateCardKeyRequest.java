package com.divination.liuyao.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 生成卡密请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenerateCardKeyRequest {
    
    /**
     * 卡密金额
     */
    @NotNull(message = "卡密金额不能为空")
    @DecimalMin(value = "0.01", message = "卡密金额必须大于0")
    private BigDecimal amount;
    
    /**
     * 生成数量
     */
    @NotNull(message = "生成数量不能为空")
    @Min(value = 1, message = "生成数量至少为1")
    private Integer count;
}

