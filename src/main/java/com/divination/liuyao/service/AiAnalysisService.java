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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import static com.divination.liuyao.util.ConstantUtil.IMAGE_PROCESSING_PROMPT_WORDS2;

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
//    @Async
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeAiAnalysis(Task task) {
        String lockKey = TaskConstants.TASK_LOCK_PREFIX + task.getUserId() + ":" + TaskConstants.TASK_TYPE_LIUYAO;

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
                task.setActualAmount(BigDecimal.ONE);
            } else {
                task.setActualAmount(BigDecimal.ZERO);
            }
            task.setActualAmount(paymentService.amountCalculation(AITaskType.fromCode(AITaskType.TEXT.getCode()),
                    analysis));
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
                redisUtil.del(lockKey);
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
            if(hexagram.getCustomTime() != null && !hexagram.getCustomTime().isEmpty()){
                prompt.append("时间： " + hexagram.getCustomTime() + "\n");
            } else if(hexagram.getLocalDateTime() == null && hexagram.getBaZi() != null){
                prompt.append("时间： " + hexagram.getBaZi().toString() + "\n");
            } else if(hexagram.getLocalDateTime() != null){
                prompt.append("时间： " + BaZiUtil.getAllByLocalDateTime(hexagram.getLocalDateTime()) + "\n");
            }
            //卦名
            prompt.append(hexagram.getGuaStringByPosition(hexagram.isExistChanged()) + "\n");
            //神煞
            if(hexagram.getShenSha() != null && !hexagram.getShenSha().isEmpty()){
                prompt.append(hexagram.getShenShaString() + "\n");
            }
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
            AiResult response = llmServiceFactory.generateText(systemPrompt, prompt.toString(), castDto);

            // 计算响应时间（毫秒）转换为秒，保留2位小数
            log.debug("AI响应成功，响应时间{}", String.format("%.2f", (System.currentTimeMillis() - startTime) / 1000.0));
            return response;
        } catch (Exception e) {
            log.error("AI分析过程中发生错误: ", e);
            throw e; // 重新抛出异常，让全局异常处理器处理
        }
    }
}