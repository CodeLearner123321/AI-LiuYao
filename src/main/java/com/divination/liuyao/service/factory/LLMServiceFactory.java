package com.divination.liuyao.service.factory;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.divination.liuyao.assemblies.enums.LLMServiceType;
import com.divination.liuyao.assemblies.enums.ModelType;
import com.divination.liuyao.pojo.dto.CastDto;
import com.divination.liuyao.service.LLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * LLM服务工厂类
 * 用于根据配置或需求动态选择合适的LLM服务实现
 */
@Slf4j
@Component
public class LLMServiceFactory {
    
    private final LLMService volcengineLLMService;
    private final LLMService dashScopeLLMService;
    
    @Value("${ai.default-llm-service:volcengine}")
    private String defaultLLMServiceCode;
    
    public LLMServiceFactory(
            @Qualifier("volcengineLLMService") LLMService volcengineLLMService,
            @Qualifier("dashScopeLLMService") LLMService dashScopeLLMService) {
        this.volcengineLLMService = volcengineLLMService;
        this.dashScopeLLMService = dashScopeLLMService;
        
        log.info("LLM服务工厂初始化完成，默认服务类型: {}", defaultLLMServiceCode);
    }
    
    /**
     * 获取默认的LLM服务实现
     */
    public LLMService getDefaultLLMService() {
        LLMServiceType defaultType = LLMServiceType.fromvalue(defaultLLMServiceCode);
        if (defaultType == null) {
            log.warn("配置的默认服务类型 '{}' 无效，使用VOLCENGINE作为默认值", defaultLLMServiceCode);
            defaultType = LLMServiceType.VOLCENGINE;
        }
        return getLLMService(defaultType);
    }
    
    /**
     * 根据服务类型枚举获取LLM服务实现
     * 
     * @param serviceType 服务类型枚举
     * @return 对应的LLM服务实现
     */
    public LLMService getLLMService(LLMServiceType serviceType) {
        if (serviceType == null) {
            log.warn("未指定服务类型，使用默认服务");
            return getDefaultLLMService();
        }
        
        switch (serviceType) {
            case VOLCENGINE:
                return volcengineLLMService;
            case DASHSCOPE:
                return dashScopeLLMService;
            default:
                log.warn("未能处理的服务类型 '{}', 使用默认服务", serviceType);
                return getDefaultLLMService();
        }
    }

    /**
     * 超越规则的实现方法
     * 主要是为了兼容用户可以自定义apiKey
     */
    public String generateText(String systemPrompt, String prompt, CastDto castDto) throws NoApiKeyException, InputRequiredException {
        //如果有一个为null，则优先用系统提供的大语言模型实现
        if(castDto.getLlmServiceType() == null || castDto.getModelId() == null || castDto.getApiKey() == null){
            return getLLMService(castDto.getLlmServiceType()).generateText(systemPrompt, prompt);
        }

        //都不为空则调用自定义的资源实现
        return getLLMService(castDto.getLlmServiceType()).generateText(systemPrompt, prompt, castDto.getModelId(), castDto.getApiKey());
    }


} 