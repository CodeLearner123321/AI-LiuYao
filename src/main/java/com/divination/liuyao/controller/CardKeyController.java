package com.divination.liuyao.controller;

import com.divination.liuyao.annotation.RateLimit;
import com.divination.liuyao.pojo.dto.GenerateCardKeyRequest;
import com.divination.liuyao.pojo.dto.QueryCardKeyRequest;
import com.divination.liuyao.pojo.dto.UseCardKeyRequest;
import com.divination.liuyao.pojo.vo.CardKeyVO;
import com.divination.liuyao.result.RespEntity;
import com.divination.liuyao.service.CardKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 卡密控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/cardkey")
@RequiredArgsConstructor
public class CardKeyController {
    
    private final CardKeyService cardKeyService;
    
    /**
     * 生成卡密
     * 只有userId=2的用户才能访问
     * 
     * @param request 生成卡密请求，包含金额和数量
     * @return 生成的卡密列表
     */
    @PostMapping("/generate")
    @RateLimit(period = 60, timeUnit = TimeUnit.SECONDS, maxRequests = 10, message = "生成卡密过于频繁，请稍后再试！")
    public RespEntity<List<CardKeyVO>> generateCardKeys(@Valid @RequestBody GenerateCardKeyRequest request) {
        List<CardKeyVO> cardKeys = cardKeyService.generateCardKeys(request.getAmount(), request.getCount());
        return RespEntity.ok(cardKeys);
    }
    
    /**
     * 使用卡密
     * 任何登录用户都可以使用卡密
     * 
     * @param request 使用卡密请求，包含卡密码
     * @return 充值结果，包含充值金额
     */
    @PostMapping("/use")
    @RateLimit(period = 60, timeUnit = TimeUnit.SECONDS, maxRequests = 10, message = "使用卡密过于频繁，请稍后再试！")
    public RespEntity<Map<String, Object>> useCardKey(@Valid @RequestBody UseCardKeyRequest request) {
        BigDecimal amount = cardKeyService.useCardKey(request.getCardCode());
        
        Map<String, Object> result = new HashMap<>();
        result.put("amount", amount);
        result.put("message", "卡密使用成功，余额已增加");
        
        return RespEntity.ok(result);
    }
    
    /**
     * 查询卡密列表
     * 只有userId=2或userId=1的用户才能访问
     * 
     * @param request 查询卡密请求，包含状态和金额（可选）
     * @return 卡密列表
     */
    @PostMapping("/query")
    @RateLimit(period = 60, timeUnit = TimeUnit.SECONDS, maxRequests = 30, message = "查询卡密过于频繁，请稍后再试！")
    public RespEntity<List<CardKeyVO>> queryCardKeys(@RequestBody QueryCardKeyRequest request) {
        List<CardKeyVO> cardKeys = cardKeyService.queryCardKeys(request.getStatus(), request.getAmount());
        return RespEntity.ok(cardKeys);
    }
}

