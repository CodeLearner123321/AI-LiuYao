package com.divination.liuyao.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.divination.liuyao.assemblies.enums.LLMServiceType;
import com.divination.liuyao.assemblies.enums.ModelType;
import com.divination.liuyao.pojo.model.AiResult;
import com.divination.liuyao.service.LLMService;
import java.util.Arrays;

import com.divination.liuyao.util.AIUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Collections;
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


    public DashScopeLLMServiceImpl() {
    }

    @PostConstruct
    public void initialize() {
        log.info("阿里百练(DashScope)AI服务初始化完成，使用模型");
    }
    
    /**
     * 返回服务类型
     */
    @Override
    public LLMServiceType getLLMServiceType() {
        return LLMServiceType.DASHSCOPE;
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
    public AiResult generateText(String systemPrompt, String userPrompt, ModelType modelType, String apiKey)
        throws NoApiKeyException, InputRequiredException {
        Generation gen = new Generation();
        Message userMsg = Message.builder()
            .role(Role.USER.getValue())
            .content(systemPrompt + userPrompt)
            .build();
        GenerationParam param = GenerationParam.builder()
            .apiKey(apiKey == null ? this.apiKey : apiKey)
            .model(modelType.getDashScopeValue())
            .messages(Arrays.asList(userMsg))
            .resultFormat(GenerationParam.ResultFormat.MESSAGE)
            .build();
        GenerationResult call = gen.call(param);
        String content = call.getOutput().getChoices().get(0).getMessage().getContent();
        AiResult aiResult = AIUtil.conductAIResults(content);
        aiResult.setInputToken(Long.valueOf(call.getUsage().getInputTokens()));
        aiResult.setOutputToken(Long.valueOf(call.getUsage().getOutputTokens()));

        return aiResult;
    }

    @Override
    public AiResult generateTextByImage(String systemPrompt, String userPrompt, String imageUrl, String modelId) throws NoApiKeyException, UploadFileException {
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessage systemMessage = MultiModalMessage.builder().role(Role.SYSTEM.getValue())
                .content(Arrays.asList(
                        Collections.singletonMap("text", systemPrompt))).build();
        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(
                        Collections.singletonMap("image", imageUrl),
                        Collections.singletonMap("text", userPrompt))).build();
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(apiKey)
                .model(modelId)  // 此处以qwen-vl-max-latest为例，可按需更换模型名称。模型列表：https://help.aliyun.com/zh/model-studio/models
                .messages(Arrays.asList(systemMessage, userMessage))
                .build();
        MultiModalConversationResult result = conv.call(param);
        AiResult aiResult = new AiResult();
        aiResult.setText(result.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text").toString());
        aiResult.setInputToken(Long.valueOf(result.getUsage().getInputTokens()));
        aiResult.setOutputToken(Long.valueOf(result.getUsage().getOutputTokens()));
        aiResult.setImageToken(Long.valueOf(result.getUsage().getImageTokens()));

        return aiResult;


    }
} 