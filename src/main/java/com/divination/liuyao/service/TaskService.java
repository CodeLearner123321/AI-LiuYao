package com.divination.liuyao.service;

import cn.hutool.Hutool;
import com.divination.liuyao.assemblies.enums.LLMServiceType;
import com.divination.liuyao.assemblies.enums.ModelType;
import com.divination.liuyao.config.DefaultValueConfig;
import com.divination.liuyao.exception.BusinessException;
import com.divination.liuyao.pojo.dto.CastDto;
import com.divination.liuyao.pojo.entity.Task;
import com.divination.liuyao.pojo.entity.User;
import com.divination.liuyao.exception.AuthenticationException;
import com.divination.liuyao.mapper.TaskMapper;
import com.divination.liuyao.mapper.UserMapper;
import com.divination.liuyao.pojo.enums.PaymentType;
import com.divination.liuyao.pojo.model.AiResult;
import com.divination.liuyao.result.RespEntity;
import com.divination.liuyao.util.ConstantUtil;
import com.divination.liuyao.util.RedisUtil;
import com.divination.liuyao.util.TaskConstants;
import com.divination.liuyao.util.UserContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.divination.liuyao.assemblies.enums.CastType;
import com.divination.liuyao.pojo.vo.TaskQueryVO;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    private final RedisUtil redisUtil;
    private final AiAnalysisService aiAnalysisService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DefaultValueConfig defaultValueConfig;

    @Autowired
    private PaymentService  paymentService;

    /**
     * 创建起卦任务
     * @param castDto 起卦参数
     * @return 任务ID
     */
    @Transactional
    public RespEntity<Map<String, Object>> createLiuyaoTask(CastDto castDto) throws Exception {
        // 获取当前用户
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return RespEntity.error("用户未登录");
        }

        // 检查Redis中是否已有任务锁
        String lockKey = TaskConstants.TASK_LOCK_PREFIX + userId + ":" + TaskConstants.TASK_TYPE_LIUYAO;
        Boolean lockSet = redisUtil.setIfAbsent(lockKey, "1", TaskConstants.TASK_LOCK_EXPIRE_TIME);

        if (Boolean.FALSE.equals(lockSet)) {
            log.warn("用户 {} 已有进行中的六爻任务", userId);
            return RespEntity.error("您有一个正在进行的六爻分析任务，请等待完成后再试");
        }

        // 在这里声明task变量，使其在try和catch块中都可见
        Task task = null;
        Integer paymentType = PaymentType.USER_DEFINED_API_PAYMENT.getCode();

        try {
            // 创建任务记录
            task = new Task();
            Boolean isTrue = paymentService.checkFreeQuota(userId);
            if (StringUtils.isBlank(defaultValueConfig.getApiKey())) {
                int affected = paymentService.advancePayment(userId);
                if (affected == 0 && !isTrue) {
                    return RespEntity.error(String.format("您的每日%s次免费额度已使用完，请联系管理员或明日在试吧~", ConstantUtil.USER_FREE_QUOTA));
                }
                if (affected == 0) {
                    //免费额度
                    paymentType = PaymentType.FREE_QUOTA_PAYMENT.getCode();
                    paymentService.payTheFreeQuota(userId);
                } else {
                    //付费额度
                    paymentType = PaymentType.BALANCE_PAYMENT.getCode();
                }
                castDto.setLlmServiceType(LLMServiceType.VOLCENGINE);
                castDto.setApiKey(defaultValueConfig.getApiKey());
                castDto.setModelId(ModelType.DeepSeek);
            } else {
                if (castDto.getLlmServiceType() == null || castDto.getModelId() == null) {
                    throw new BusinessException("请补充模型类型和LLM服务类型");
                }
            }
            task.setRequestParams(objectMapper.writeValueAsString(castDto));
            task.setUserId(userId);
            task.setTaskType(TaskConstants.TASK_TYPE_LIUYAO);
            task.setStatus(TaskConstants.TASK_STATUS_PENDING);
            task.setPreAmount(TaskConstants.LIUYAO_PRICE);
            task.setActualAmount(BigDecimal.ZERO);
            task.setIsCharged(TaskConstants.CHARGE_STATUS_NO);
            task.setPaymentType(paymentType);

            task.setResultData(null);
            task.setErrorMsg(null);
            task.setCreatedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());

            // 保存任务
            taskMapper.insert(task);

            // 异步执行AI分析 - 直接调用aiAnalysisService的方法
            aiAnalysisService.executeAiAnalysis(task);

            Long taskId = task.getId();
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            return RespEntity.ok(response);
        } catch (Exception e) {
            // 获取任务ID（如果已创建）
            if (task != null && task.getId() != null) {
                // 更新任务状态为失败
                taskMapper.updateTaskComplete(
                        task.getId(),
                        null,
                        TaskConstants.TASK_STATUS_FAILED,
                        "任务创建过程中发生异常: " + e.getMessage(),
                        BigDecimal.ZERO,
                        TaskConstants.CHARGE_STATUS_NO
                );
                // 如果已经预扣费，执行退款操作
                if (paymentType.equals(PaymentType.BALANCE_PAYMENT.getCode())) {
                    paymentService.rollbackQuota(userId);
                } else if (paymentType.equals(PaymentType.FREE_QUOTA_PAYMENT.getCode())) {
                    paymentService.rollbackFreeQuota(userId);
                }
            }
            throw e;
        } finally {
            redisUtil.del(lockKey);
        }
    }
    
    /**
     * 获取任务状态，按任务类型过滤
     */
    public Task getTaskStatus(Long taskId, String taskType) {
        Optional<Task> taskOpt = taskMapper.findById(taskId);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            if (task.getTaskType().equals(taskType)) {
                return task;
            }
        }
        return null;
    }
    
    /**
     * 查询任务结果
     */
    public TaskQueryVO getTaskResult(Long taskId, String taskType, Long userId) throws JsonProcessingException {
        // 创建返回结果对象
        TaskQueryVO result = new TaskQueryVO();
        result.setTaskId(taskId);
        
        // 检查Redis中是否有任务锁，有锁说明任务正在处理中
        String lockKey = TaskConstants.TASK_LOCK_PREFIX + userId + ":" + taskType;
        if (redisUtil.hasKey(lockKey)) {
            // 任务正在处理中
            result.setStatus(TaskConstants.TASK_STATUS_PROCESSING);
            result.setCompleted(false);
            return result;
        }
        
        // 从数据库查询任务
        Task task = getTaskStatus(taskId, taskType);
        if (task == null) {
            // 任务不存在
            result.setStatus("NOT_FOUND");
            result.setCompleted(false);
            result.setError("任务不存在");
            return result;
        }
        
        // 设置任务状态
        result.setStatus(task.getStatus());
        
        // 检查任务是否完成
        if (TaskConstants.TASK_STATUS_COMPLETED.equals(task.getStatus())) {
            result.setCompleted(true);
            result.setData(objectMapper.readTree(task.getResultData()));
        } else if (TaskConstants.TASK_STATUS_FAILED.equals(task.getStatus())) {
            // 任务失败
            result.setCompleted(true);
            result.setError(task.getErrorMsg());
        } else {
            // 其他状态（如PENDING）
            result.setCompleted(false);
        }
        
        return result;
    }
} 