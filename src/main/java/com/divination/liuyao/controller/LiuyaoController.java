package com.divination.liuyao.controller;

import com.divination.liuyao.annotation.RateLimit;
import com.divination.liuyao.assemblies.enums.LLMServiceType;
import com.divination.liuyao.pojo.dto.BaGuaDto;
import com.divination.liuyao.pojo.dto.BaZi;
import com.divination.liuyao.pojo.dto.CastDto;
import com.divination.liuyao.pojo.dto.UserPermissionDTO;
import com.divination.liuyao.pojo.entity.User;
import com.divination.liuyao.pojo.enums.UserRoleType;
import com.divination.liuyao.pojo.enums.ViewPermission;
import com.divination.liuyao.pojo.model.Hexagram;
import com.divination.liuyao.pojo.vo.BaGuaVo;
import com.divination.liuyao.pojo.vo.TaskQueryVO;
import com.divination.liuyao.result.RespEntity;
import com.divination.liuyao.service.HexagramService;
import com.divination.liuyao.service.TaskService;
import com.divination.liuyao.util.BaZiUtil;
import com.divination.liuyao.util.UserContextHolder;
import com.divination.liuyao.exception.AuthenticationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/liuyao")
@RequiredArgsConstructor
public class LiuyaoController {

    @Autowired
    private HexagramService hexagramService;

    @Autowired
    private TaskService taskService;
    
    @Value("${ai.default-llm-service:volcengine}")
    private String defaultLLMService;

    /**
     * 时间转换
     * @param dateTime
     * @return
     */
    @GetMapping("/calculate")
    @RateLimit(period = 60, timeUnit = TimeUnit.SECONDS, maxRequests = 300, message = "操作过于频繁，请稍后再试！")
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
     * 
     * @param castDto 起卦请求数据，可以通过llmServiceType字段指定使用的LLM服务
     */
    @PostMapping("/cast")
    @RateLimit(period = 30, timeUnit = TimeUnit.SECONDS, maxRequests = 2, message = "操作过于频繁，请稍后再试！")
    public RespEntity<Map<String, Object>> castByTimestamp(@Valid @RequestBody CastDto castDto) throws Exception {
        // 设置用户ID
        castDto.setUserId(UserContextHolder.getUserId());
        
        // 如果没有指定LLM服务类型，使用默认值
        if (castDto.getLlmServiceType() == null) {
            castDto.setLlmServiceType(LLMServiceType.fromvalue(defaultLLMService));
            log.debug("未指定LLM服务类型，使用默认值: {}", defaultLLMService);
        } else {
            log.debug("使用指定的LLM服务类型: {}", castDto.getLlmServiceType());
        }
        
        return taskService.createLiuyaoTask(castDto);
    }
    
    /**
     * 查询任务结果
     * @param taskId 任务ID
     * @param taskType 任务类型，默认为LIUYAO
     * @return 任务状态和结果
     */
    @GetMapping("/task/{taskId}")
    @RateLimit(period = 60, timeUnit = TimeUnit.SECONDS, maxRequests = 25, message = "操作过于频繁，请稍后再试！")
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
    @RateLimit(period = 60, timeUnit = TimeUnit.SECONDS, maxRequests = 24, message = "操作过于频繁，请稍后再试！")
    public RespEntity<BaGuaVo> calculateLiuYao(@Valid @RequestBody BaGuaDto baGuaDto) {
        BaGuaVo baGuaVo = hexagramService.calculateLiuYao(baGuaDto);
        return RespEntity.ok(baGuaVo);
    }

    /**
     * 上传图片，识别文字
     */
    @PostMapping("/recognize")
    public RespEntity<Hexagram> recognizeText(@RequestParam("file") MultipartFile file) {
        try {
            return RespEntity.ok(hexagramService.recognizeTextByImage(file));
        } catch (Exception e) {
            e.printStackTrace();
            return RespEntity.error("图片识别失败");
        }
    }

    /**
     * 获取用户权限信息
     */
    @GetMapping("/permissions")
    public RespEntity<UserPermissionDTO> getUserPermissions() {
        if(UserContextHolder.isRoot()){
            UserPermissionDTO userPermissionDTO = new UserPermissionDTO();
            userPermissionDTO.setViewPermissions(Arrays.asList(ViewPermission.UPLOAD_VIEW.getCode()));
            userPermissionDTO.setRole(UserRoleType.ROOT.getCode());
            return RespEntity.ok(userPermissionDTO);
        }
        return RespEntity.ok(new UserPermissionDTO());
    }
} 