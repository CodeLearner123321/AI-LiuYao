package com.divination.liuyao.controller;

import com.divination.liuyao.annotation.RateLimit;
import com.divination.liuyao.exception.AuthenticationException;
import com.divination.liuyao.pojo.dto.FeedbackRequest;
import com.divination.liuyao.pojo.vo.AiLiuyaoHistoryVO;
import com.divination.liuyao.result.RespEntity;
import com.divination.liuyao.service.AiLiuyaoHistoryService;
import com.divination.liuyao.util.UserContextHolder;
import java.util.concurrent.TimeUnit;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI六爻历史记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/liuyao/history")
@RequiredArgsConstructor
public class AiLiuyaoHistoryController {

    private final AiLiuyaoHistoryService historyService;

    /**
     * 获取当前用户的历史记录
     */
    @GetMapping
    @RateLimit(period = 60, timeUnit = TimeUnit.SECONDS, maxRequests = 10, message = "获取历史记录过于频繁，请稍后再试！")
    public RespEntity<List<AiLiuyaoHistoryVO>> getHistory() {
        // 验证用户是否登录
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new AuthenticationException("用户未登录", 401);
        }

        List<AiLiuyaoHistoryVO> histories = historyService.getCurrentUserHistory();
        return RespEntity.ok(histories);
    }

    /**
     * 根据历史记录ID获取历史记录
     * @param historyId 历史记录ID
     * @return 历史记录详情
     */
    @GetMapping("/{historyId}")
    @RateLimit(period = 60, timeUnit = TimeUnit.SECONDS, maxRequests = 24, message = "操作过于频繁，请稍后再试！")
    public RespEntity<AiLiuyaoHistoryVO> getHistoryById(@PathVariable Long historyId) {
        // 验证用户是否登录
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new AuthenticationException("用户未登录", 401);
        }

        // 获取历史记录
        AiLiuyaoHistoryVO history = historyService.getHistoryById(historyId);

        // 验证记录是否存在
        if (history == null) {
            return RespEntity.error(404, "历史记录不存在");
        }

        // 验证记录是否属于当前用户
        if (!history.getUserId().equals(userId)) {
            throw new AuthenticationException("无权访问该历史记录", 403);
        }

        return RespEntity.ok(history);
    }

    /**
     * 根据历史记录ID删除历史记录
     * @param historyId 历史记录ID
     * @return 是否删除成功
     */
    @GetMapping("delete/{historyId}")
    @RateLimit(period = 60, timeUnit = TimeUnit.SECONDS, maxRequests = 60, message = "操作过于频繁，请稍后再试！")
    public RespEntity<Boolean> deleteById(@PathVariable Long historyId) {
        return RespEntity.ok(historyService.deleteById(historyId));
    }

    /**
     * 用户反馈历史记录的准确性
     * @param request 反馈请求，包含历史记录ID和准确性（0-不准确，1-准确）
     * @return 是否反馈成功
     */
    @PostMapping("/feedback")
    @RateLimit(period = 60, timeUnit = TimeUnit.SECONDS, maxRequests = 30, message = "操作过于频繁，请稍后再试！")
    public RespEntity<Boolean> feedback(@Valid @RequestBody FeedbackRequest request) {
        // 更新反馈
        Boolean result = historyService.updateFeedback(request.getId(), request.getIsAccurate());
        
        if (result) {
            return RespEntity.ok(true);
        } else {
            return RespEntity.error(500, "反馈失败");
        }
    }

} 