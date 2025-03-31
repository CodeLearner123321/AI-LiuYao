package com.divination.liuyao.service;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.divination.liuyao.assemblies.enums.CastType;
import com.divination.liuyao.mapper.AiLiuyaoHistoryMapper;
import com.divination.liuyao.mapper.TaskMapper;
import com.divination.liuyao.mapper.UserMapper;
import com.divination.liuyao.pojo.dto.CastDto;
import com.divination.liuyao.pojo.entity.AiLiuyaoHistory;
import com.divination.liuyao.pojo.entity.Task;
import com.divination.liuyao.pojo.model.AiResult;
import com.divination.liuyao.pojo.model.Hexagram;
import com.divination.liuyao.service.factory.LLMServiceFactory;
import com.divination.liuyao.util.BaZiUtil;
import com.divination.liuyao.util.ConstantUtil;
import com.divination.liuyao.util.RedisUtil;
import com.divination.liuyao.util.TaskConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.scheduling.annotation.Async;

import javax.annotation.PostConstruct;

@Slf4j
@Service
public class AiAnalysisService {

    private final TaskMapper taskMapper;
    private final HexagramService hexagramService;
    private final AiLiuyaoHistoryMapper historyMapper;
    private final UserMapper userMapper;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;
    private final LLMServiceFactory llmServiceFactory;

    public AiAnalysisService(
            TaskMapper taskMapper,
            HexagramService hexagramService,
            AiLiuyaoHistoryMapper historyMapper,
            UserMapper userMapper,
            RedisUtil redisUtil,
            ObjectMapper objectMapper,
            LLMServiceFactory llmServiceFactory) {
        this.taskMapper = taskMapper;
        this.hexagramService = hexagramService;
        this.historyMapper = historyMapper;
        this.userMapper = userMapper;
        this.redisUtil = redisUtil;
        this.objectMapper = objectMapper;
        this.llmServiceFactory = llmServiceFactory;
    }

    /**
     * 异步执行AI分析任务
     * @param task 任务对象
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeAiAnalysis(Task task) {
        String lockKey = TaskConstants.TASK_LOCK_PREFIX + task.getUserId() + ":" + TaskConstants.TASK_TYPE_LIUYAO;

        try {
            // 将requestParams转换为CastDto
            CastDto castDto = objectMapper.readValue(task.getRequestParams(), CastDto.class);

            // 计算卦象
            var hexagram = hexagramService.castHexagram(castDto);

            // 调用AI进行分析
            AiResult analysis = this.analyzeHexagram(hexagram, castDto);

            // 使用ObjectMapper将分析结果转换为JSON
            String resultJson = objectMapper.writeValueAsString(analysis);

            //给用户实际扣款
            if(analysis.isFalse()){
                log.debug("UserId：{},分析结果有误,退还余额。\ntask:{}",task.getUserId(),task);
                refundPreAmount(task.getId());
            }else {
                userMapper.confirmDeduct(task.getUserId());
            }

            task.setActualAmount(task.getPreAmount());
            task.setIsCharged(TaskConstants.CHARGE_STATUS_YES);

            // 使用新的updateTaskComplete方法一次性更新所有字段
            taskMapper.updateTaskComplete(
                task.getId(),
                resultJson,
                TaskConstants.TASK_STATUS_COMPLETED,
                null,
                task.getPreAmount(),
                TaskConstants.CHARGE_STATUS_YES
            );

            // 保存历史记录
            saveHistoryRecord(task, castDto, analysis);

            log.info("六爻分析任务 {} 已完成", task.getId());
        } catch (Exception e) {
            log.error("六爻分析任务 {} 执行失败: {}", task.getId(), e.getMessage(), e);

            try {
                // 更新任务状态为失败（包含错误信息）
                taskMapper.updateTaskComplete(
                    task.getId(),
                    null,
                    TaskConstants.TASK_STATUS_FAILED,
                    e.getMessage(),
                    BigDecimal.ZERO,
                    TaskConstants.CHARGE_STATUS_NO
                );

                // 如果失败，恢复用户余额
                refundPreAmount(task.getId());
            } catch (Exception ex) {
                log.error("更新任务状态失败: {}", ex.getMessage(), ex);
            }
        } finally {
            // 无论成功还是失败，都删除Redis锁
            try {
                redisUtil.del(lockKey);
            } catch (Exception e) {
                log.error("删除Redis锁失败: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 保存历史记录
     */
    private void saveHistoryRecord(Task task, CastDto castDto, AiResult aiResult) {
        try {
            AiLiuyaoHistory history = new AiLiuyaoHistory();

            // 设置历史记录ID (使用任务ID作为历史ID)
            history.setHistoryId(task.getId());

            // 设置用户ID和任务ID
            history.setUserId(task.getUserId());
            history.setTaskId(task.getId());

            // 设置问题和背景
            history.setQuestion(castDto.getQuestion());
            history.setBackground(castDto.getBackground());

            // 设置起卦类型
            history.setCastType(CastType.valueOf(castDto.getCastType().name()));

            // 设置起卦数据
            history.setTimestamp(castDto.getTimestamp());
            history.setNumber(castDto.getNumber());
            history.setCastTime(castDto.getCastTime());
            
            // 设置结果
            history.setKeyOutcome(aiResult.getKeyOutcome());
            history.setResultData(objectMapper.writeValueAsString(aiResult.getText()));

            // 设置创建时间和消费金额
            history.setCreateTime(LocalDateTime.now());
            history.setAmount(task.getPreAmount().toString());

            // 保存历史记录
            historyMapper.insert(history);

            log.info("六爻历史记录已保存: historyId={}, taskId={}", history.getHistoryId(), task.getId());
        } catch (Exception e) {
            log.error("保存六爻历史记录失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 退款操作
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refundPreAmount(Long taskId) {
        try {
            // 查询任务信息
            Optional<Task> taskOpt = taskMapper.findById(taskId);
            if (taskOpt.isEmpty()) {
                log.error("退款失败: 任务 {} 不存在", taskId);
                return;
            }

            Task task = taskOpt.get();

            // 判断是否已经扣费，如果已扣费则不进行退款
            if (task.getIsCharged() == TaskConstants.CHARGE_STATUS_YES) {
                log.info("任务 {} 已扣费，不进行退款", taskId);
                return;
            }

            // 退款
            userMapper.refund(task.getUserId(), task.getPreAmount());
            log.info("用户 {} 退款 {} 成功", task.getUserId(), task.getPreAmount());
        } catch (Exception e) {
            log.error("退款处理异常: {}", e.getMessage(), e);
        }
    }


    /**
     * 分析卦象（包含问题和背景）
     */
    public AiResult analyzeHexagram(Hexagram hexagram, CastDto castDto)
        throws InterruptedException, NoApiKeyException, InputRequiredException {
        String question = castDto.getQuestion();
        String background = castDto.getBackground();

        try {
            // 系统提示
            String systemPrompt = "你是一位精通易经六爻预测的大师，熟读《增删卜易》、《古筮真诠》、《卜筮正宗》、《卜筮全书》、《黄金策》、《易冒》、《断易天机》、《火株林》、《京氏易传》《洞林秘诀》、《易林补遗》、《易隐》、《易冒》、《十翼》\n";

            // 构建用户提示
            StringBuilder prompt = new StringBuilder();
            prompt.append("请分析以下六爻卦：\n");
            if (question != null && !question.isEmpty()) {
                prompt.append("问题：" + question + "\n");
            }
            if (background != null && !background.isEmpty()) {
                prompt.append("背景：" + background + "\n");
            }

            //时间
            prompt.append("时间： " + BaZiUtil.getAllByLocalDateTime(hexagram.getLocalDateTime()) + "\n");
            //卦名
            prompt.append(hexagram.getGuaStringByPosition(hexagram.isExistChanged()) + "\n");
            //神煞
            prompt.append(hexagram.getShenShaString() + "\n");
            //从上爻到初爻
            for (int i = 5; i >= 0; i--) {
                prompt.append(hexagram.getYaoStringByPosition(i, hexagram.isExistChanged()) + "\n");
            }
            prompt.append("请你按照如下的分析思路去分析：\n");
            prompt.append("1.取用神（可能有一个或多个）\n");
            prompt.append("2.根据六亲旺衰、动爻与用神的关系、动变关系、以理法的角度分析事情的吉凶\n");
            prompt.append("3.根据六神、神煞、爻位、神煞结合已经分析的理法用象法的角度分析事情的具体过程\n");
            prompt.append("4.根据理法和象法两个角度，给出事情的定论。\n");
            prompt.append("请你依次按照：1、用神 2、理法  3、象法  4、吉凶定论 5、判辞 这五个标题的格式回复我（判辞就是总结吉凶判断的一句小诗，这句小诗要求通俗易懂，字数不超过12个字）\n");
            prompt.append("请你严格参照上述要求分析，要求分析时以专业的角度分析，给出答案要通俗易懂\n");
            prompt.append("如果我提交的问题和背景，有误，请直接返回" + ConstantUtil.AI_ERROR_RESULT_CODE + "\n");

            log.debug("准备发送AI请求，提示词长度: {}", prompt.length());
            log.debug("提示词为：\n" + prompt.toString());

            // 记录开始时间
            long startTime = System.currentTimeMillis();
            // 调用LLMService获取回复
            String response = llmServiceFactory.generateText(systemPrompt, prompt.toString(), castDto);

            // 计算响应时间（毫秒）转换为秒，保留2位小数
            log.debug("AI响应成功，响应长度: {}, 响应时间{}", response.length(),
                String.format("%.2f", (System.currentTimeMillis() - startTime) / 1000.0));
            log.debug("AI响应为：" + response);

            return conductAIResults(response);
        } catch (Exception e) {
            log.error("AI分析过程中发生错误: ", e);
            throw e; // 重新抛出异常，让全局异常处理器处理
        }
    }

    /**
     * 用于处理AI返回的结果
     * @param aiResponse AI返回的原始文本
     * @return 处理后的AiResult对象，包含判辞和完整文本
     */
    private AiResult conductAIResults(String aiResponse){
        // 如果AI返回了错误代码，直接返回错误信息
        if (aiResponse != null && aiResponse.contains(ConstantUtil.AI_ERROR_RESULT_CODE)) {
            log.info("AI返回了错误代码，问题或背景可能有误");
            AiResult errorResult = new AiResult();
            errorResult.setText(ConstantUtil.AI_ERROR_RESULT);
            errorResult.setKeyOutcome(ConstantUtil.AI_ERROR_RESULT_KEY);
            return errorResult;
        }

        AiResult result = new AiResult();

        try {
            // 尝试使用多种可能的判辞标记格式
            String[] possiblePrefixes = {
                "5、判辞", "5、 判辞", "5.判辞", "5. 判辞",
                "5、判词", "5、 判词", "5.判词", "5. 判词",
                "5.判断", "5、判断", "5. 判断", "5、 判断",
                "判辞：", "判辞:", "判词：", "判词:"
            };

            int keyOutcomeStartIndex = -1;
            String foundPrefix = null;

            // 查找判辞部分的开始位置
            for (String prefix : possiblePrefixes) {
                int index = aiResponse.indexOf(prefix);
                if (index != -1) {
                    keyOutcomeStartIndex = index;
                    foundPrefix = prefix;
                    break;
                }
            }

            // 处理找不到判辞的情况
            if (keyOutcomeStartIndex == -1) {
                log.warn("无法通过标准格式在AI响应中找到判辞部分，尝试识别最后一段...");

                // 尝试查找最后一个分隔符后的内容作为判辞
                String[] possibleSeparators = {"\\---", "---", "***", "\\*\\*\\*", "##", "\\n\\n"};

                for (String separator : possibleSeparators) {
                    int lastSepIndex = aiResponse.lastIndexOf(separator);
                    if (lastSepIndex != -1 && lastSepIndex < aiResponse.length() - 10) { // 确保分隔符后还有足够的内容
                        keyOutcomeStartIndex = lastSepIndex + separator.length();
                        foundPrefix = "";
                        break;
                    }
                }

                // 如果仍然找不到，将整个文本作为text返回，提取最后一段作为keyOutcome
                if (keyOutcomeStartIndex == -1) {
                    log.warn("无法在AI响应中找到判辞部分，使用最后一段作为判辞");
                    // 查找最后一个换行符
                    int lastNewline = aiResponse.lastIndexOf("\n\n");
                    if (lastNewline != -1 && lastNewline < aiResponse.length() - 10) {
                        String lastParagraph = aiResponse.substring(lastNewline).trim();
                        result.setKeyOutcome(cleanKeyOutcome(lastParagraph));
                        result.setText(aiResponse); // 设置全文
                    } else {
                        // 如果没有找到适合的最后一段，则设置默认判辞
                        result.setKeyOutcome("请参考完整分析结果");
                        result.setText(aiResponse); // 设置全文
                    }
                    return result;
                }
            }

            log.debug("找到判辞标记: {}, 位置: {}", foundPrefix, keyOutcomeStartIndex);

            // 提取判辞部分
            String keyOutcomePart = aiResponse.substring(keyOutcomeStartIndex);

            // 查找判辞部分的结束位置（如果有下一个段落的话）
            int nextSectionIndex = keyOutcomePart.indexOf("\n\n");
            if (nextSectionIndex != -1) {
                keyOutcomePart = keyOutcomePart.substring(0, nextSectionIndex);
            }

            // 清理判辞文本
            String keyOutcome = cleanKeyOutcome(keyOutcomePart);

            // 如果清理后的判辞是空的，或者太短，可能是处理有误
            if (keyOutcome.isEmpty() || keyOutcome.length() < 5) {
                log.warn("清理后的判辞内容异常短: [{}]，尝试使用原始文本", keyOutcome);
                keyOutcome = keyOutcomePart.replace(foundPrefix, "").trim();
            }

            log.debug("提取的判辞: {}", keyOutcome);

            // 设置keyOutcome
            result.setKeyOutcome(keyOutcome);

            // 设置text（不包含判辞部分）
            if (keyOutcomeStartIndex > 0) {
                result.setText(aiResponse.substring(0, keyOutcomeStartIndex).trim());
            } else {
                result.setText(""); // 如果判辞在开头，text为空
            }

        } catch (Exception e) {
            log.error("处理AI响应时出错: ", e);
            // 出错时将整个响应设为text，keyOutcome留空
            result.setText(aiResponse);
            result.setKeyOutcome("处理出错，请参考完整分析");
        }

        return result;
    }

    /**
     * 清理判辞文本，移除标记和格式
     */
    private String cleanKeyOutcome(String rawText) {
        if (rawText == null) return "";

        // 移除判辞标记
        String cleaned = rawText
            .replace("5、判辞", "")
            .replace("5、 判辞", "")
            .replace("5.判辞", "")
            .replace("5. 判辞", "")
            .replace("5、判词", "")
            .replace("5、 判词", "")
            .replace("5.判词", "")
            .replace("5. 判词", "")
            .replace("5.判断", "")
            .replace("5、判断", "")
            .replace("5. 判断", "")
            .replace("5、 判断", "")
            .replace("判辞：", "")
            .replace("判辞:", "")
            .replace("判词：", "")
            .replace("判词:", "");

        // 移除Markdown标记
        cleaned = cleaned
            .replaceAll("\\*\\*", "")
            .replaceAll("\\\\-", "")
            .replaceAll("\\\\n", " ")
            .replaceAll("#+", "");

        // 移除多余空白
        cleaned = cleaned.trim();

        // 如果判辞过长，可能不是真正的判辞，截取适当长度
        if (cleaned.length() > 200) {
            log.warn("判辞过长 ({} 字符)，可能包含了额外内容，进行截断", cleaned.length());
            // 尝试在一个合理的位置截断
            int cutPoint = cleaned.indexOf("。", 50);
            if (cutPoint == -1) {
                cutPoint = cleaned.indexOf("，", 50);
            }
            if (cutPoint == -1) {
                cutPoint = cleaned.indexOf("\n", 50);
            }
            if (cutPoint == -1 && cleaned.length() > 100) {
                cutPoint = 100;
            }

            if (cutPoint != -1) {
                cleaned = cleaned.substring(0, cutPoint + 1);
            }
        }

        return cleaned;
    }
}