package com.divination.liuyao.service;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.divination.liuyao.assemblies.enums.LLMServiceType;
import com.divination.liuyao.assemblies.enums.ModelType;

/**
 * 大语言模型服务接口
 * 用于处理与AI大模型的交互
 */
public interface LLMService {
    
    /**
     * 调用大语言模型生成文本
     * 
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @return 生成的文本内容
     */
    String generateText(String systemPrompt, String userPrompt);

    /**
     * 调用大语言模型生成文本
     *系统提示词, 用户提示词, 模型类型, apiKey
     */
    String generateText(String systemPrompt, String userPrompt, ModelType modelType, String apiKey) throws NoApiKeyException, InputRequiredException;


    /**
     * 获取模型名称
     * 
     * @return 模型名称
     */
    String getModelName();

    /**
     * 获取LLM服务类型
     * 
     * @return LLM服务类型枚举
     */
    LLMServiceType getLLMServiceType();
} 