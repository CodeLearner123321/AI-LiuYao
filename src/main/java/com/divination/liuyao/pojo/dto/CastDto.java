package com.divination.liuyao.pojo.dto;

import com.divination.liuyao.assemblies.enums.CastType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 起卦请求
 */
@Data
public class CastDto extends BaGuaDto{

    /**
     * 用户ID（由系统自动设置，不需要前端传入）
     */
    private Long userId;

    /**
     * 问题（必填）
     */
    @NotBlank(message = "问题不能为空")
    private String question;

    /**
     * 背景
     */
    @NotBlank(message = "问题不能为空")
    private String background;
} 