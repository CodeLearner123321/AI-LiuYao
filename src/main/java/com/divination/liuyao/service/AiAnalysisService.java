package com.divination.liuyao.service;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.divination.liuyao.assemblies.enums.CastType;
import com.divination.liuyao.mapper.AiLiuyaoHistoryMapper;
import com.divination.liuyao.mapper.TaskMapper;
import com.divination.liuyao.mapper.UserMapper;
import com.divination.liuyao.pojo.dto.CastDto;
import com.divination.liuyao.pojo.entity.AiLiuyaoHistory;
import com.divination.liuyao.pojo.entity.Prediction;
import com.divination.liuyao.pojo.entity.Task;
import com.divination.liuyao.pojo.enums.AITaskType;
import com.divination.liuyao.pojo.enums.PaymentType;
import com.divination.liuyao.pojo.model.AiResult;
import com.divination.liuyao.pojo.model.Hexagram;
import com.divination.liuyao.service.factory.LLMServiceFactory;
import com.divination.liuyao.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class AiAnalysisService {

    @Autowired
    private TaskMapper taskMapper;
    @Autowired
    private AiLiuyaoHistoryMapper historyMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private LLMServiceFactory llmServiceFactory;
    @Autowired
    private HexagramService hexagramService;
    @Autowired
    private PaymentService paymentService;
    // 允许的图片 MIME 类型
    private final List<String> IMAGE_TYPES = Arrays.asList(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/gif",
            "image/bmp",
            "image/webp"
    );

    /**
     * 异步执行AI分析任务
     * @param task 任务对象
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeAiAnalysis(Task task) {
        String aiLockKey = TaskConstants.TASK_LOCK_PREFIX_AI_PREDICTION + ":" + task.getUserId() + ":" + TaskConstants.TASK_TYPE_LIUYAO;

        try {
            long start = System.currentTimeMillis();
            // 将requestParams转换为CastDto
            CastDto castDto = objectMapper.readValue(task.getRequestParams(), CastDto.class);
            // 计算卦象
            Hexagram hexagram = hexagramService.castHexagram(castDto);
            // 调用AI进行分析
            AiResult analysis = this.analyzeHexagram(hexagram, castDto);
            // 使用ObjectMapper将分析结果转换为JSON
            String resultJson = objectMapper.writeValueAsString(analysis);
            //给用户实际扣款
            if(Objects.equals(PaymentType.BALANCE_PAYMENT.getCode(),task.getPaymentType())){
                paymentService.confirmPay(PaymentType.fromCode(task.getPaymentType()),
                        AITaskType.fromCode(AITaskType.TEXT.getCode()),
                        task.getUserId(),
                        analysis);
                task.setActualAmount(paymentService.amountCalculation(AITaskType.fromCode(AITaskType.TEXT.getCode()),
                        analysis));
            } else if(Objects.equals(PaymentType.FREE_QUOTA_PAYMENT.getCode(),task.getPaymentType())){
                //免费消费一次
                task.setActualAmount(BigDecimal.ONE);
            } else {
                task.setActualAmount(BigDecimal.ZERO);
            }
            task.setIsCharged(analysis.isFalse() ? TaskConstants.CHARGE_STATUS_NO : TaskConstants.CHARGE_STATUS_YES);


            // 使用新的updateTaskComplete方法一次性更新所有字段
            taskMapper.updateTaskComplete(
                task.getId(),
                resultJson,
                analysis.getIsTrue() ? TaskConstants.TASK_STATUS_COMPLETED : TaskConstants.TASK_STATUS_FAILED,
                analysis.getIsTrue() ? null : analysis.getText(),
                task.getActualAmount(),
                task.getIsCharged(),
                    task.getPaymentType()
            );

            // 保存历史记录
            saveHistoryRecord(task, castDto, analysis, (int) ((System.currentTimeMillis() - start) / 1000));

            log.info("六爻分析任务 {} 已完成", task.getId());
        } catch (Exception e) {
            log.error("六爻分析任务 {} 执行失败: {}", task.getId(), e.getMessage(), e);
            String msg = e.getMessage();
            if(e.getMessage().contains("The API key in the request is missing or invali")){
                msg = "两次免费接口调用余额已用完，请改日再来或在右上角头像中选择设置KEY";
            }
            try {
                // 更新任务状态为失败（包含错误信息）
                taskMapper.updateTaskComplete(
                    task.getId(),
                    null,
                    TaskConstants.TASK_STATUS_FAILED,
                    msg,
                    BigDecimal.ZERO,
                    TaskConstants.CHARGE_STATUS_NO, task.getPaymentType()
                );

                // 如果失败，恢复用户余额
                paymentService.rollbackQuota(task.getUserId());
            } catch (Exception ex) {
                log.error("更新任务状态失败: {}", ex.getMessage(), ex);
            }
        } finally {
            // 无论成功还是失败，都删除Redis锁
            try {
                redisUtil.del(aiLockKey);
            } catch (Exception e) {
                log.error("删除Redis锁失败: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 保存历史记录
     */
    private void saveHistoryRecord(Task task, CastDto castDto, AiResult aiResult, Integer durationSeconds) {
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
            history.setCustomTime(castDto.getCustomTime());

            // 设置结果
            history.setKeyOutcome(aiResult.getKeyOutcome());
            history.setResultData(objectMapper.writeValueAsString(aiResult.getText()));

            // 设置创建时间和消费金额
            history.setCreateTime(LocalDateTime.now());
            history.setAmount(task.getActualAmount().toString());
            history.setDurationSeconds(durationSeconds);

            // 保存历史记录
            historyMapper.insert(history);

            log.info("六爻历史记录已保存: historyId={}, taskId={}", history.getHistoryId(), task.getId());
        } catch (Exception e) {
            log.error("保存六爻历史记录失败: {}", e.getMessage(), e);
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
            // 使用模板生成系统提示
            String systemPrompt = FreemarkerUtil.render("liuyao_system_prompt.ftl", new HashMap<>());
            
            // 准备模板参数
            Map<String, Object> templateParams = new HashMap<>();
            
            // 设置问题和背景
            templateParams.put("question", question);
            templateParams.put("background", background);
            
            // 处理时间信息
            String timeString = null;
            if(hexagram.getCustomTime() != null && !hexagram.getCustomTime().isEmpty()){
                timeString = hexagram.getCustomTime();
            } else if(hexagram.getLocalDateTime() == null && hexagram.getBaZi() != null){
                timeString = hexagram.getBaZi().toString();
            } else if(hexagram.getLocalDateTime() != null){
                timeString = BaZiUtil.getAllByLocalDateTime(hexagram.getLocalDateTime());
            }
            templateParams.put("timeString", timeString);
            
            // 设置卦名
            templateParams.put("guaString", hexagram.getGuaStringByPosition(hexagram.isExistChanged()));
            
            // 设置神煞
            if(hexagram.getShenSha() != null && !hexagram.getShenSha().isEmpty()){
                templateParams.put("shenShaString", hexagram.getShenShaString());
            }
            
            // 设置六爻信息（从上爻到初爻）
            List<String> yaoStrings = new ArrayList<>();
            for (int i = 5; i >= 0; i--) {
                yaoStrings.add(hexagram.getYaoStringByPosition(i, hexagram.isExistChanged()));
            }
            templateParams.put("yaoStrings", yaoStrings);
            
            // 设置错误代码
            templateParams.put("errorCode", ConstantUtil.AI_ERROR_RESULT_CODE);
            
            // 使用模板生成用户提示
            String prompt = FreemarkerUtil.render("liuyao_analysis_prompt.ftl", templateParams);

            log.debug("准备发送AI请求，提示词长度: {}", prompt.length());
            log.debug("提示词为：\n{}", prompt);

            // 记录开始时间
            long startTime = System.currentTimeMillis();
//             调用LLMService获取回复
            AiResult response = llmServiceFactory.generateText(systemPrompt, prompt, castDto);

            // 计算响应时间（毫秒）转换为秒，保留2位小数
            log.debug("AI响应成功，响应时间{}", String.format("%.2f", (System.currentTimeMillis() - startTime) / 1000.0));
//            return new AiResult();
            return response;
        } catch (Exception e) {
            log.error("AI分析过程中发生错误: ", e);
            throw e; // 重新抛出异常，让全局异常处理器处理
        }
    }
}