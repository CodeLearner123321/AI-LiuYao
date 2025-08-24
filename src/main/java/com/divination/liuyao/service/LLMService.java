package com.divination.liuyao.service;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.divination.liuyao.assemblies.enums.LLMServiceType;
import com.divination.liuyao.assemblies.enums.ModelType;

/**
 * 大语言模型服务接口
 * 用于处理与AI大模型的交互
 */
public interface LLMService {

    /**
     * 调用大语言模型生成文本
     *系统提示词, 用户提示词, 模型类型, apiKey
     */
    String generateText(String systemPrompt, String userPrompt, ModelType modelType, String apiKey) throws NoApiKeyException, InputRequiredException;

    /**
     * 图片处理
     */
    String generateTextByImage(String systemPrompt, String userPrompt, String imageUrl, String modelId) throws NoApiKeyException, UploadFileException;

    /**
     * 获取LLM服务类型
     * 
     * @return LLM服务类型枚举
     */
    LLMServiceType getLLMServiceType();
} 