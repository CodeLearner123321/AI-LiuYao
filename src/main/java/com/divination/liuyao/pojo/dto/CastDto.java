package com.divination.liuyao.pojo.dto;

import com.divination.liuyao.assemblies.enums.LLMServiceType;
import com.divination.liuyao.assemblies.enums.ModelType;
import lombok.Data;

import javax.validation.constraints.NotBlank;

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

    /**
     * LLM服务类型：volcengine, dashscope
     */
    private LLMServiceType llmServiceType;

    /**
     * 模型类型
     */
    private ModelType modelId;

    /**
     * Key：用于消耗token的凭证
     */
    private String apiKey;
} 