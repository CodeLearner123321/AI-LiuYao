package com.divination.liuyao.service.impl;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.divination.liuyao.assemblies.enums.LLMServiceType;
import com.divination.liuyao.assemblies.enums.ModelType;
import com.divination.liuyao.pojo.model.AiResult;
import com.divination.liuyao.service.LLMService;
import com.divination.liuyao.util.AIUtil;
import com.volcengine.ark.runtime.model.completion.chat.*;
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

    private ArkService arkService;

    @PostConstruct
    public void initialize() {
        // 使用火山引擎SDK初始化服务
        arkService = ArkService.builder()
            .apiKey(apiKey)
            .build();
        log.info("火山引擎AI服务初始化完成");
    }
    
    /**
     * 返回服务类型
     */
    @Override
    public LLMServiceType getLLMServiceType() {
        return LLMServiceType.VOLCENGINE;
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
    public AiResult generateText(String systemPrompt, String userPrompt, ModelType modelType, String apiKey, Boolean processTheText) {
        String contents = "";
        AiResult aiResult = new AiResult();
        try {
            // 创建ArkService实例
            ArkService arkService = ArkService.builder().apiKey(apiKey == null ? this.apiKey : apiKey)
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

            ChatCompletionResult chatCompletion = arkService.createChatCompletion(chatCompletionRequest);
            List<ChatCompletionChoice> choices = chatCompletion.getChoices();
            contents = choices.stream()
                    .map(choice -> choice.getMessage().stringContent())
                    .collect(Collectors.joining("\n"));
            if(processTheText != null && processTheText) {
                aiResult = AIUtil.conductAIResults(contents);
            } else {
                AiResult result = new AiResult();
                result.setText(contents);
                aiResult = result;
            }
            aiResult.setInputToken(chatCompletion.getUsage().getPromptTokens());
            aiResult.setOutputToken(chatCompletion.getUsage().getCompletionTokens());
        } finally {
            arkService.shutdownExecutor();
        }

        return aiResult;
    }

    @Override
    public AiResult generateTextByImage(String systemPrompt, String userPrompt, String imageUrl, String modelId) throws NoApiKeyException, UploadFileException {
        return new AiResult();
    }
} 