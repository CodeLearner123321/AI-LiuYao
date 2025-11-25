package com.divination.liuyao.service;

import com.divination.liuyao.exception.AuthenticationException;
import com.divination.liuyao.exception.BusinessException;
import com.divination.liuyao.mapper.CardKeyMapper;
import com.divination.liuyao.mapper.UserMapper;
import com.divination.liuyao.pojo.entity.CardKey;
import com.divination.liuyao.pojo.vo.CardKeyVO;
import com.divination.liuyao.util.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 卡密服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardKeyService {
    
    private final CardKeyMapper cardKeyMapper;
    private final UserMapper userMapper;
    
    /**
     * 生成卡密（批量）
     * 只有userId=2或userId=1的用户才能生成卡密
     * 
     * @param amount 卡密金额
     * @param count 生成数量
     * @return 生成的卡密列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<CardKeyVO> generateCardKeys(BigDecimal amount, Integer count) {
        // 获取当前用户ID
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new AuthenticationException("用户未登录", 401);
        }
        
        // 验证权限：只有userId=2或userId=1的用户才能生成卡密
        if (!userId.equals(2L) && !userId.equals(1L)) {
            throw new AuthenticationException("无权限生成卡密", 403);
        }
        
        // 批量生成卡密实体
        LocalDateTime now = LocalDateTime.now();
        List<CardKey> cardKeyList = new ArrayList<>(count);
        List<CardKeyVO> cardKeyVOList = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            CardKey cardKey = new CardKey();
            cardKey.setCardCode(generateCardCode());
            cardKey.setAmount(amount);
            cardKey.setCreatorId(userId);
            cardKey.setStatus(0); // 0-未使用
            cardKey.setCreateTime(now);
            cardKeyList.add(cardKey);
            
            // 同时构建VO对象
            CardKeyVO vo = new CardKeyVO();
            vo.setCardCode(cardKey.getCardCode());
            vo.setAmount(cardKey.getAmount());
            vo.setStatus(cardKey.getStatus());
            vo.setCreateTime(cardKey.getCreateTime());
            cardKeyVOList.add(vo);
        }
        
        // 批量插入数据库（一次性插入所有卡密）
        int rows = cardKeyMapper.batchInsert(cardKeyList);
        if (rows != count) {
            log.error("批量插入卡密失败，预期插入{}条，实际插入{}条", count, rows);
            throw new BusinessException(500, "批量生成卡密失败");
        }
        
        return cardKeyVOList;
    }
    
    /**
     * 使用卡密
     * 任何用户都可以使用卡密
     * 
     * @param cardCode 卡密码
     * @return 充值金额
     */
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal useCardKey(String cardCode) {
        // 获取当前用户ID
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new AuthenticationException("用户未登录", 401);
        }
        
        // 查询卡密
        CardKey cardKey = cardKeyMapper.findByCardCode(cardCode)
                .orElseThrow(() -> new BusinessException(400, "卡密不存在"));
        // 检查卡密状态
        if (cardKey.getStatus() == 1) {
            throw new BusinessException(400, "卡密已被使用");
        }
        
        // 使用卡密（使用乐观锁）
        int rows = cardKeyMapper.useCardKey(cardCode, userId);
        if (rows == 0) {
            throw new BusinessException(400, "卡密使用失败，可能已被其他人使用");
        }
        
        // 给用户充值
        int rechargeRows = userMapper.recharge(userId, cardKey.getAmount());
        if (rechargeRows == 0) {
            throw new BusinessException(500, "充值失败");
        }
        
        return cardKey.getAmount();
    }
    
    /**
     * 根据状态和金额查询卡密列表
     * 只有userId=2或userId=1的用户才能查询
     * 
     * @param status 卡密状态（可选）
     * @param amount 卡密金额（可选）
     * @return 卡密列表
     */
    public List<CardKeyVO> queryCardKeys(Integer status, BigDecimal amount) {
        // 获取当前用户ID
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new AuthenticationException("用户未登录", 401);
        }
        
        // 验证权限：只有userId=2或userId=1的用户才能查询卡密
        if (!userId.equals(2L) && !userId.equals(1L)) {
            throw new AuthenticationException("无权限查询卡密", 403);
        }
        
        // 查询卡密列表
        List<CardKey> cardKeyList = cardKeyMapper.queryCardKeys(status, amount);
        
        // 转换为VO对象
        List<CardKeyVO> cardKeyVOList = new ArrayList<>();
        for (CardKey cardKey : cardKeyList) {
            CardKeyVO vo = new CardKeyVO();
            vo.setCardCode(cardKey.getCardCode());
            vo.setAmount(cardKey.getAmount());
            vo.setStatus(cardKey.getStatus());
            vo.setCreateTime(cardKey.getCreateTime());
            vo.setUseTime(cardKey.getUseTime());
            cardKeyVOList.add(vo);
        }
        
        return cardKeyVOList;
    }
    
    /**
     * 生成卡密码
     * 使用UUID生成唯一的卡密码，去除横杠并转大写
     * 
     * @return 卡密码
     */
    private String generateCardCode() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}

