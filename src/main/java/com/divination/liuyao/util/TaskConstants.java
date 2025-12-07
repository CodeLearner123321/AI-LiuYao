package com.divination.liuyao.util;

import java.math.BigDecimal;

/**
 * 任务相关常量
 */
public class TaskConstants {
    
    // 任务类型
    public static final String TASK_TYPE_LIUYAO = "LIUYAO";
    
    // 任务状态
    public static final String TASK_STATUS_PENDING = "PENDING";
    //数据库中暂时没有PROCESSING状态，应为要减少系统开销
    public static final String TASK_STATUS_PROCESSING = "PROCESSING";
    public static final String TASK_STATUS_COMPLETED = "COMPLETED";
    public static final String TASK_STATUS_FAILED = "FAILED";
    
    /**
     *  0 - 未扣费
     *  1 - 已扣费
     */
    public static final int CHARGE_STATUS_NO = 0;
    public static final int CHARGE_STATUS_YES = 1;

    
    // Redis中任务锁的前缀
    public static final String TASK_LOCK_PREFIX = "TASK:LOCK:";

    // Redis中任务锁的前缀： ai预测
    public static final String TASK_LOCK_PREFIX_AI_PREDICTION = "TASK:LOCK:AI:PREDICTION";
    
    // Redis中任务锁的过期时间（100秒）
    public static final int TASK_LOCK_EXPIRE_TIME = 100;
    
    // 任务每个单位的价格（如每次起卦）
    public static final BigDecimal LIUYAO_PRICE = new BigDecimal("0.01");
} 