package com.divination.liuyao.controller;

import com.divination.liuyao.pojo.dto.BaGuaDto;
import com.divination.liuyao.pojo.dto.BaZi;
import com.divination.liuyao.pojo.dto.CastDto;
import com.divination.liuyao.pojo.entity.Task;
import com.divination.liuyao.pojo.model.Hexagram;
import com.divination.liuyao.pojo.vo.BaGuaVo;
import com.divination.liuyao.pojo.vo.TaskQueryVO;
import com.divination.liuyao.result.RespEntity;
import com.divination.liuyao.service.AiAnalysisService;
import com.divination.liuyao.service.HexagramService;
import com.divination.liuyao.service.TaskService;
import com.divination.liuyao.util.BaZiUtil;
import com.divination.liuyao.util.RedisUtil;
import com.divination.liuyao.util.TaskConstants;
import com.divination.liuyao.util.UserContextHolder;
import com.divination.liuyao.exception.AuthenticationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/liuyao")
@RequiredArgsConstructor
public class LiuyaoController {

    private final HexagramService hexagramService;
    private final TaskService taskService;

    /**
     * 时间转换
     * @param dateTime
     * @return
     */
    @GetMapping("/calculate")
    public RespEntity<BaZi> calculateBazi(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime) {
        BaZi response = BaZiUtil.baziConvertByTime(dateTime);
        return RespEntity.ok(response);
    }

    /**
     * 根据传入的时间戳、问题和背景异步起卦并分析
     * 业务流程：
     * 1. 创建任务记录并返回任务ID
     * 2. 异步执行AI分析
     * 3. 前端通过任务ID查询结果
     */
    @PostMapping("/cast")
    public RespEntity<Map<String, Object>> castByTimestamp(@Valid @RequestBody CastDto castDto) throws Exception {
        // 设置用户ID
        castDto.setUserId(UserContextHolder.getUserId());
        
        return taskService.createLiuyaoTask(castDto);
    }
    
    /**
     * 查询任务结果
     * @param taskId 任务ID
     * @param taskType 任务类型，默认为LIUYAO
     * @return 任务状态和结果
     */
    @GetMapping("/task/{taskId}")
    public RespEntity<TaskQueryVO> getTaskResult(
            @PathVariable Long taskId,
            @RequestParam(value = "taskType", defaultValue = "LIUYAO") String taskType) throws JsonProcessingException {
        
        // 获取当前用户ID
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new AuthenticationException("用户未登录", 401);
        }
        
        // 调用Service层方法获取任务结果
        TaskQueryVO result = taskService.getTaskResult(taskId, taskType, userId);
        return RespEntity.ok(result);
    }

    /**
     * 根据前端传过来的数据组成卦象
     * 前端可能传递：值 或者 时间戳
     */
    @PostMapping("/generate/liuyao")
    public RespEntity<BaGuaVo> calculateLiuYao(@Valid @RequestBody BaGuaDto baGuaDto) {
        BaGuaVo baGuaVo = hexagramService.calculateLiuYao(baGuaDto);
        return RespEntity.ok(baGuaVo);
    }

} 