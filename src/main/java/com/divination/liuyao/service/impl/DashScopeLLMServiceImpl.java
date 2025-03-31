package com.divination.liuyao.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.divination.liuyao.assemblies.enums.LLMServiceType;
import com.divination.liuyao.assemblies.enums.ModelType;
import com.divination.liuyao.service.LLMService;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 阿里百练LLM服务实现
 * 通过REST API方式调用DashScope服务
 */
@Slf4j
@Service("dashScopeLLMService")
public class DashScopeLLMServiceImpl implements LLMService {

    @Value("${ai.dashscope.api.key:null}")
    private String apiKey;

    @Value("${ai.dashscope.model.id:null}")
    private String modelId;

    @Value("${ai.dashscope.model.name:null}")
    private String modelName;

    @Value("${ai.dashscope.max-tokens:2000}")
    private int maxTokens;

    @Value("${ai.dashscope.temperature:0.7}")
    private double temperature;

    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

    private final RestTemplate restTemplate;

    public DashScopeLLMServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    public void initialize() {
        log.info("阿里百练(DashScope)AI服务初始化完成，使用模型: {}", modelName);
    }
    
    /**
     * 返回模型名称
     */
    @Override
    public String getModelName() {
        return modelName;
    }
    
    /**
     * 返回服务类型
     */
    @Override
    public LLMServiceType getLLMServiceType() {
        return LLMServiceType.DASHSCOPE;
    }

    /**
     * 调用阿里百练API生成文本
     */
    @Override
    public String generateText(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("阿里百练API密钥未设置，请检查配置。");
        }

        try {
            // 创建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 构建消息
            Map<String, Object> message = new HashMap<>();
            
            // 构建系统消息和用户消息
            Map<String, Object> input = new HashMap<>();
            input.put("prompt", userPrompt);
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("max_tokens", maxTokens);
            parameters.put("temperature", temperature);

            // 如果有系统提示，添加到参数中
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                parameters.put("system", systemPrompt);
            }

            // 构建整个请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelId);
            requestBody.put("input", input);
            requestBody.put("parameters", parameters);

            log.debug("发送请求到阿里百练API，使用模型ID: {}", modelId);

            // 发送POST请求
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(API_URL, requestEntity, Map.class);

            // 处理响应
            Map<String, Object> responseBody = responseEntity.getBody();
            if (responseBody != null) {
                Map<String, Object> output = (Map<String, Object>) responseBody.get("output");
                if (output != null) {
                    String text = (String) output.get("text");
                    return text;
                }
            }

            throw new RuntimeException("阿里百练API返回格式不符合预期");

        } catch (Exception e) {
            log.error("调用阿里百练API时发生错误: ", e);
            throw new RuntimeException("调用阿里百练API时发生错误: " + e.getMessage(), e);
        }
    }

    /**
     * 调用大语言模型生成文本 系统提示词, 用户提示词, 模型类型, apiKey
     *
     * @param systemPrompt
     * @param userPrompt
     * @param modelType 目前只支持DeepSeek
     * @param apiKey
     */
    @Override
    public String generateText(String systemPrompt, String userPrompt, ModelType modelType, String apiKey)
        throws NoApiKeyException, InputRequiredException {
        Generation gen = new Generation();
        Message userMsg = Message.builder()
            .role(Role.USER.getValue())
            .content(systemPrompt + userPrompt)
            .build();
        GenerationParam param = GenerationParam.builder()
            // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
            .apiKey(apiKey)
            .model(modelType.getDashScopeValue())
            .messages(Arrays.asList(userMsg))
            .resultFormat(GenerationParam.ResultFormat.MESSAGE)
            .build();
        String content = gen.call(param).getOutput().getChoices().get(0).getMessage().getContent();

        return content;
    }
} 