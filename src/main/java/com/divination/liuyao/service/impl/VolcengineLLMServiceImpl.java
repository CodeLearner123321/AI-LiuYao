package com.divination.liuyao.service.impl;

import com.divination.liuyao.assemblies.enums.LLMServiceType;
import com.divination.liuyao.assemblies.enums.ModelType;
import com.divination.liuyao.service.LLMService;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChoice;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import java.time.Duration;
import java.util.Collections;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * 火山引擎LLM服务实现
 * 负责与火山引擎API交互，生成AI文本
 */
@Slf4j
@Service("volcengineLLMService")
public class VolcengineLLMServiceImpl implements LLMService {

    @Value("${ai.volcengine.api.key:null}")
    private String apiKey;

    @Value("${ai.volcengine.model.id:null}")
    private String modelId;

    @Value("${ai.volcengine.model.name:null}")
    private String modelName;

    @Value("${ai.volcengine.max-tokens:2000}")
    private int maxTokens;

    @Value("${ai.volcengine.temperature:0.7}")
    private double temperature;

    private ArkService arkService;

    @PostConstruct
    public void initialize() {
        // 使用火山引擎SDK初始化服务
        arkService = ArkService.builder()
            .apiKey(apiKey)
            .build();
        log.info("火山引擎AI服务初始化完成，使用模型: {}", modelName);
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
        return LLMServiceType.VOLCENGINE;
    }

    /**
     * 调用火山引擎API生成文本
     */
    @Override
    public String generateText(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("火山引擎API密钥未设置，请检查配置。");
        }

        try {
            // 创建消息列表
            final List<ChatMessage> messages = new ArrayList<>();

            // 添加系统消息
            final ChatMessage systemMessage = ChatMessage.builder()
                .role(ChatMessageRole.SYSTEM)
                .content(systemPrompt)
                .build();

            // 添加用户消息
            final ChatMessage userMessage = ChatMessage.builder()
                .role(ChatMessageRole.USER)
                .content(userPrompt)
                .build();

            messages.add(systemMessage);
            messages.add(userMessage);

            // 创建请求
            ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(modelId)
                .messages(messages)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();

            log.debug("使用SDK发送请求到火山引擎API，使用模型ID: {}", modelId);

            // 发送请求并获取响应
            String response = (String) arkService.createChatCompletion(chatCompletionRequest)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();

            return response;

        } catch (Exception e) {
            log.error("调用火山引擎API时发生错误: ", e);
            throw new RuntimeException("调用火山引擎API时发生错误: " + e.getMessage(), e);
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
    public String generateText(String systemPrompt, String userPrompt, ModelType modelType, String apiKey) {
        String contents = "";
        try {
            // 创建ArkService实例
            ArkService arkService = ArkService.builder().apiKey(apiKey)
                .timeout(Duration.ofMinutes(3))// 深度推理模型耗费时间会较长，请您设置较大的超时时间，避免超时导致任务失败。推荐30分钟以上 ？？？
                .build();
            // 创建用户消息
            ChatMessage userMessage = ChatMessage.builder()
                .role(ChatMessageRole.USER) // 设置消息角色为用户
                .content(systemPrompt + userPrompt) // 设置消息内容
                .build();
            // 创建聊天完成请求
            ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(modelType.getVolcengineValue())
                .messages(Collections.singletonList(userMessage)) // 设置消息列表
                .build();

            contents = arkService.createChatCompletion(chatCompletionRequest).getChoices().stream()
                .map(choice -> choice.getMessage().stringContent())
                .collect(Collectors.joining("\n"));
        } finally {
            arkService.shutdownExecutor();
        }

        return contents;
    }
} 