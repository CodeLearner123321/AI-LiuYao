package com.divination.liuyao.service;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.divination.liuyao.exception.BusinessException;
import com.divination.liuyao.mapper.AiLiuyaoHistoryMapper;
import com.divination.liuyao.mapper.TaskMapper;
import com.divination.liuyao.mapper.UserMapper;
import com.divination.liuyao.pojo.entity.Task;
import com.divination.liuyao.pojo.entity.User;
import com.divination.liuyao.pojo.enums.AITaskType;
import com.divination.liuyao.pojo.enums.PaymentType;
import com.divination.liuyao.pojo.model.AiResult;
import com.divination.liuyao.result.RespEntity;
import com.divination.liuyao.util.ConstantUtil;
import com.divination.liuyao.util.RedisUtil;
import com.divination.liuyao.util.TaskConstants;
import com.divination.liuyao.util.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * 与金额相关的service
 */
@Slf4j
@Service
public class PaymentService {
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private UserService userService;
    @Autowired
    private TaskMapper taskMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 支付免费额度（每次调用消耗1次）
     * @param userId 用户ID
     * @return 剩余额度（消费后）
     */
    public Integer payTheFreeQuota(Long userId) {
        String redisKey = RedisUtil.USER_REQUEST_CREDIT_LIMIT + userId;

        // 如果 Redis 中没有该 key
        if (!redisUtil.hasKey(redisKey)) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endOfDay = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 23, 59);
            long seconds = LocalDateTimeUtil.between(now, endOfDay).getSeconds();

            int initQuota = Objects.equals(61L, userId) ? 4 : ConstantUtil.USER_FREE_QUOTA - 1;
            redisUtil.set(redisKey, initQuota, seconds);
            return initQuota;
        } else {
            // 执行自减操作
            Long remaining = redisUtil.decr(redisKey, 1);
            return remaining != null ? remaining.intValue() : null;
        }
    }

    /**
     * 回滚免费额度（恢复1次使用机会）
     * @param userId 用户ID
     * @return 回滚后的剩余额度
     */
    public Integer rollbackFreeQuota(Long userId) {
        String redisKey = RedisUtil.USER_REQUEST_CREDIT_LIMIT + userId;

        // 如果 Redis 中没有该 key，说明不应该回滚
        if (!redisUtil.hasKey(redisKey)) {
            throw new BusinessException("无法回滚：该用户没有初始化免费额度");
        }

        // 执行自增操作
        Long remaining = redisUtil.incr(redisKey, 1);
        return remaining != null ? remaining.intValue() : null;
    }

    /**
     * 回滚真实额度
     */
    public Boolean rollbackQuota(Long userId) {
        return userMapper.refund(userId, TaskConstants.LIUYAO_PRICE) == 1;
    }


    /**
     * 校验是否有免费额度 true：有
     */
    public Boolean checkFreeQuota(Long userId) {
        String redisKey = RedisUtil.USER_REQUEST_CREDIT_LIMIT + userId;
        boolean isTrue = redisUtil.hasKey(redisKey);
        if(isTrue){
            return Integer.parseInt(redisUtil.get(redisKey) + "") <= 0;
        } else {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastTime = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 23, 59);
            long seconds = LocalDateTimeUtil.between(now, lastTime).getSeconds();
            redisUtil.set(redisKey, Objects.equals(61L, UserContextHolder.getUserId()) ? 4 : ConstantUtil.USER_FREE_QUOTA, seconds);
            return true;
        }
    }


    /**
     * 刷新用户余额缓存
     * 直接更新Redis缓存并设置过期时间，用于余额变更时保持缓存一致性
     *
     * @param userId 用户ID
     * @param newBalance 新的余额值
     * @return 操作结果
     */
    public Boolean refreshUserBalanceCache(Long userId, String newBalance) {
        if (userId == null || newBalance == null) {
            throw new BusinessException(400, "参数不能为空");
        }
        try {
            // 构建Redis缓存key
            String cacheKey = ConstantUtil.USER_BALANCE_KEY + userId;

            // 直接更新Redis缓存，设置相同的过期时间
            redisUtil.set(cacheKey, newBalance, ConstantUtil.USER_BALANCE_EXPIRE_TIME);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取用户余额
     */
    public String getBalance() {
        // 获取当前用户ID
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(400, "用户未登录");
        }

        // 构建Redis缓存key
        String cacheKey = ConstantUtil.USER_BALANCE_KEY + userId;
        // 构建分布式锁key
        String lockKey = ConstantUtil.USER_BALANCE_LOCK_KEY + userId;

        try {
            // 1. 先从Redis查询
            Object cachedBalance = redisUtil.get(cacheKey);
            if (cachedBalance != null) {
                // 缓存命中，直接返回
                return cachedBalance.toString();
            }

            // 2. 缓存未命中，使用SETNX尝试获取分布式锁，防止缓存穿透
            boolean lockAcquired = redisUtil.setIfAbsent(lockKey, "1", 10); // 锁有效期10秒

            if (!lockAcquired) {
                // 未获取到锁，说明有其他线程正在查询数据库并更新缓存
                // 短暂等待后重新查询缓存
                Thread.sleep(2000);
                Object retryCache = redisUtil.get(cacheKey);
                if (retryCache != null) {
                    return retryCache.toString();
                }
                throw new BusinessException(400, "系统繁忙，请稍后再试");
            }

            try {
                // 3. 获取到锁，查询数据库
                Optional<User> userOpt = userService.findById(userId);
                if (!userOpt.isPresent()) {
                    throw new BusinessException(400, "该用户不存在");
                }

                // 4. 更新Redis缓存，设置1分钟过期时间
                String balance = userOpt.get().getBalance().toString();
                redisUtil.set(cacheKey, balance, ConstantUtil.USER_BALANCE_EXPIRE_TIME);
                return balance;

            } finally {
                // 释放锁
                redisUtil.del(lockKey);
            }
        } catch (Exception e) {
            log.error("获取用户余额失败", e);
            throw new BusinessException(400, "获取余额失败" + e.getMessage());
        }
    }


    /**
     * 预支付额度
     */
    public Integer advancePayment(Long userId) {
        return userMapper.preDeduct(userId, TaskConstants.LIUYAO_PRICE);
    }

    /**
     * 最终支付额度
     */
    public Integer finalPay(Long userId, Integer payNumber) {
        return 1;
    }


    /**
     * 最终支付
     * @param paymentType   支付类型
     * @param taskType      任务类型
     * @param userId        用户id
     * @param aiResult      AI使用token
     */
    public void confirmPay(PaymentType paymentType, AITaskType taskType, Long userId, AiResult aiResult) {
        BigDecimal price = amountCalculation(taskType, aiResult);
        if(PaymentType.BALANCE_PAYMENT == paymentType){
            userMapper.confirmDeduct(userId, TaskConstants.LIUYAO_PRICE, price);
        } else if (PaymentType.FREE_QUOTA_PAYMENT == paymentType){
            //免费额度没有预支付
        }
    }

    public BigDecimal amountCalculation(AITaskType taskType, AiResult aiResult){
        //todo 后续支持配置
        BigDecimal inputPrice = new BigDecimal("0.003").divide(new BigDecimal(1000));
        BigDecimal outputPrice = new BigDecimal("0.009").divide(new BigDecimal(1000));
        BigDecimal imagePrice = new BigDecimal("0.009").divide(new BigDecimal(1000));
        BigDecimal price = inputPrice.multiply(BigDecimal.valueOf(aiResult.getInputToken()))
                .add(outputPrice.multiply(BigDecimal.valueOf(aiResult.getOutputToken())));

        if(AITaskType.IMAGE == taskType) {
            price = price.add(imagePrice.multiply(BigDecimal.valueOf(aiResult.getImageToken())));
        }
        //收一毛钱用于维护项目
        return price.add(new  BigDecimal("0.1"));
    }
}
