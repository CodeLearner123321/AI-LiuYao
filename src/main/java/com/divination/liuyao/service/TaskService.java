package com.divination.liuyao.service;

import cn.hutool.Hutool;
import com.divination.liuyao.assemblies.enums.LLMServiceType;
import com.divination.liuyao.assemblies.enums.ModelType;
import com.divination.liuyao.config.DefaultValueConfig;
import com.divination.liuyao.pojo.dto.CastDto;
import com.divination.liuyao.pojo.entity.Task;
import com.divination.liuyao.pojo.entity.User;
import com.divination.liuyao.exception.AuthenticationException;
import com.divination.liuyao.mapper.TaskMapper;
import com.divination.liuyao.mapper.UserMapper;
import com.divination.liuyao.pojo.model.AiResult;
import com.divination.liuyao.result.RespEntity;
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
        
        try {
            // 预扣费操作（使用乐观锁）
            int affected = userMapper.preDeduct(userId, TaskConstants.LIUYAO_PRICE);
            if (affected == 0) {
                return RespEntity.error("余额不足或账户状态异常，请稍后再试");
            }
            
            // 创建任务记录
            task = new Task();
            task.setUserId(userId);
            task.setTaskType(TaskConstants.TASK_TYPE_LIUYAO);
            task.setStatus(TaskConstants.TASK_STATUS_PENDING);
            task.setPreAmount(TaskConstants.LIUYAO_PRICE);
            task.setActualAmount(BigDecimal.ZERO);
            task.setIsCharged(TaskConstants.CHARGE_STATUS_NO);
            
            // 将请求参数转为JSON
            try {
                //使用Redis计算当前可用额度
                //注：用户每天可以免费使用两次，如果超出
                boolean isTrue = redisUtil.checkAndIncrement(RedisUtil.USER_REQUEST_CREDIT_LIMIT + userId);
                if(isTrue){
                    castDto.setLlmServiceType(LLMServiceType.VOLCENGINE);
                    castDto.setApiKey(defaultValueConfig.getApiKey());
                    castDto.setModelId(ModelType.DeepSeek);
                }
                task.setRequestParams(objectMapper.writeValueAsString(castDto));
            } catch (Exception e) {
                log.error("序列化请求参数失败", e);
                task.setRequestParams("{}");
            }
            
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
            log.error("创建六爻任务失败: {}", e.getMessage(), e);
            
            // 获取任务ID（如果已创建）
            if (task != null && task.getId() != null) {
                try {
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
                    if (task.getPreAmount() != null && task.getPreAmount().compareTo(BigDecimal.ZERO) > 0) {
                        aiAnalysisService.refundPreAmount(task.getId());
                    }
                    
                    log.info("已将任务 {} 标记为失败状态", task.getId());
                } catch (Exception ex) {
                    // 发生异常，释放Redis锁
                    redisUtil.del(lockKey);
                    log.error("更新任务状态失败: {}", ex.getMessage(), ex);
                }
            }

            // 发生异常，释放Redis锁
            redisUtil.del(lockKey);
            throw e;
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
     * 实际扣费操作
     */
    @Transactional
    public void performActualCharge(Task task) {
        // 确认扣费
        int affected = userMapper.confirmDeduct(task.getUserId());
        if (affected > 0) {
            // 更新任务实际扣费金额和扣费状态
            task.setActualAmount(task.getPreAmount());
            task.setIsCharged(TaskConstants.CHARGE_STATUS_YES);
            
            // 使用新的updateTaskComplete方法更新任务
            taskMapper.updateTaskComplete(
                task.getId(),
                task.getResultData(),
                task.getStatus(),
                task.getErrorMsg(),
                task.getPreAmount(),
                TaskConstants.CHARGE_STATUS_YES
            );
            
            log.info("用户 {} 任务 {} 实际扣费 {} 成功", 
                    task.getUserId(), task.getId(), task.getActualAmount());
        } else {
            log.warn("用户 {} 任务 {} 确认扣费失败", task.getUserId(), task.getId());
        }
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
            
            // 如果任务已完成但尚未扣费，执行实际扣费操作
            if (task.getIsCharged() == TaskConstants.CHARGE_STATUS_NO) {
                performActualCharge(task);
            }
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