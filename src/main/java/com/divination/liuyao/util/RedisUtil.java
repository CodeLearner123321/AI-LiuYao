package com.divination.liuyao.util;

import cn.hutool.core.date.LocalDateTimeUtil;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.springframework.data.redis.connection.RedisZSetCommands;
import cn.hutool.core.io.resource.ClassPathResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类，封装Redis常用操作
 */
@Component
@Slf4j
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplateString;

    public static final String USER_REQUEST_CREDIT_LIMIT = "limit:user:request:credit";

    private DefaultRedisScript<Long> slidingWindowScript;
    private DefaultRedisScript<Long> checkSlidingWindowScript;


    @PostConstruct
    public void init() {
        try {
            // 1. 加载 Lua 脚本
            ClassPathResource luaFile = new ClassPathResource("lua/rateLimit.lua");
            slidingWindowScript = new DefaultRedisScript<>();
            slidingWindowScript.setScriptText(
                    new String(luaFile.getStream().readAllBytes(), StandardCharsets.UTF_8)
            );
            slidingWindowScript.setResultType(Long.class);

            ClassPathResource checkLuaFile = new ClassPathResource("lua/checkRateLimit.lua");
            checkSlidingWindowScript = new DefaultRedisScript<>();
            checkSlidingWindowScript.setScriptText(
                    new String(checkLuaFile.getStream().readAllBytes(), StandardCharsets.UTF_8)
            );
            checkSlidingWindowScript.setResultType(Long.class);

        } catch (Exception e) {
            throw new RuntimeException("无法加载 Lua 脚本: sliding_window.lua", e);
        }
    }


    /**
     * 执行时间窗口逻辑的lua脚本
     -- KEYS[1]  限流 key (zset)
     -- ARGV[1]  当前时间戳（毫秒）
     -- ARGV[2]  窗口大小（毫秒）
     -- ARGV[3]  最大请求数
     -- ARGV[4]  当前请求的唯一 member（例如 UUID）
     */
    public Long limitLuaExecute(String key, Object... args){
        return redisTemplateString.execute(
                slidingWindowScript,
                Collections.singletonList(key),
                args
        );
    }

    /**
     * 执行时间窗口逻辑的lua脚本
     -- KEYS[1]  限流 key (zset)
     -- ARGV[1]  当前时间戳（毫秒）
     -- ARGV[2]  窗口大小（毫秒）
     -- ARGV[3]  最大请求数
     -- ARGV[4]  当前请求的唯一 member（例如 UUID）
     */
    public Long checkLimitLuaExecute(String key, Object... args){
        return redisTemplateString.execute(
                checkSlidingWindowScript,
                Collections.singletonList(key),
                args
        );
    }

    /**
     * 指定缓存失效时间
     *
     * @param key  键
     * @param time 时间(秒)
     * @return 是否成功
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 重置指定用户的使用次数为0
     * @param userId 用户ID
     * @return true 表示重置成功，false 表示重置失败或key不存在
     */
    public boolean resetUserCreditLimit(Long userId) {
        try {
            String key = USER_REQUEST_CREDIT_LIMIT + userId;
            // 检查 key 是否存在
            if (!hasKey(key)) {
                log.warn("resetUserCreditLimit：用户 {} 的限额 key 不存在", userId);
                return false;
            }
            // 获取剩余过期时间
            Long expire = getExpire(key);
            if (expire == null || expire <= 0) {
                // 如果没过期时间，就重新设为当天 23:59
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime lastTime = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 23, 59);
                expire = LocalDateTimeUtil.between(now, lastTime).getSeconds();
            }
            // 重置为0
            set(key, "0", expire);
            log.info("已重置用户 {} 的请求额度为 0，剩余过期时间 {} 秒", userId, expire);
            return true;
        } catch (Exception e) {
            log.error("resetUserCreditLimit 操作异常", e);
            return false;
        }
    }

    /**
     * 根据key获取过期时间
     *
     * @param key 键
     * @return 时间(秒) 返回0代表为永久有效
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除缓存
     *
     * @param key 可以传一个或多个值
     */
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                for (String k : key) {
                    redisTemplate.delete(k);
                }
            }
        }
    }

    /**
     * 普通缓存获取
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存放入
     *
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 递增
     *
     * @param key   键
     * @param delta 要增加几(大于0)
     * @return 增加后的值
     */
    public long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    // ================================Map=================================

    /**
     * 递减
     *
     * @param key   键
     * @param delta 要减少几(小于0)
     * @return 减少后的值
     */
    public long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, -delta);
    }

    /**
     * HashGet
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return 值
     */
    public Object hget(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    /**
     * 获取hashKey对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    public Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * HashSet
     *
     * @param key 键
     * @param map 对应多个键值
     * @return true 成功 false 失败
     */
    public boolean hmset(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HashSet 并设置时间
     *
     * @param key  键
     * @param map  对应多个键值
     * @param time 时间(秒)
     * @return true成功 false失败
     */
    public boolean hmset(String key, Map<String, Object> map, long time) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @param time  时间(秒) 注意:如果已存在的hash表有时间,这里将会替换原有的时间
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value, long time) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除hash表中的值
     *
     * @param key  键 不能为null
     * @param item 项 可以使多个 不能为null
     */
    public void hdel(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    // ============================Set=============================

    /**
     * 判断hash表中是否有该项的值
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return true 存在 false不存在
     */
    public boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    /**
     * 根据key获取Set中的所有值
     *
     * @param key 键
     * @return Set集合
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将数据放入set缓存
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 将set数据放入缓存
     *
     * @param key    键
     * @param time   时间(秒)
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSetAndTime(String key, long time, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取set缓存的长度
     *
     * @param key 键
     * @return 长度
     */
    public long sGetSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // ===============================List=================================

    /**
     * 移除值为value的
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 移除的个数
     */
    public long setRemove(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().remove(key, values);
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取list缓存的内容
     *
     * @param key   键
     * @param start 开始
     * @param end   结束 0 到 -1代表所有值
     * @return List
     */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取list缓存的长度
     *
     * @param key 键
     * @return 长度
     */
    public long lGetListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 通过索引 获取list中的值
     *
     * @param key   键
     * @param index 索引 index>=0时， 0 表头，1 第二个元素，依次类推；index<0时，-1，表尾，-2倒数第二个元素，依次类推
     * @return 值
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return 是否成功
     */
    public boolean lSet(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public boolean lSet(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return 是否成功
     */
    public boolean lSet(String key, List<Object> value, long time) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据索引修改list中的某条数据
     *
     * @param key   键
     * @param index 索引
     * @param value 值
     * @return 是否成功
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 移除N个值为value
     *
     * @param key   键
     * @param count 移除多少个
     * @param value 值
     * @return 移除的个数
     */
    public long lRemove(String key, long count, Object value) {
        try {
            Long remove = redisTemplate.opsForList().remove(key, count, value);
            return remove;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 如果key不存在，则设置值并返回true，否则返回false
     *
     * @param key   键
     * @param value 值
     * @return 成功返回true，失败返回false
     */
    public boolean setIfAbsent(String key, Object value) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, value);
        } catch (Exception e) {
            log.error("Redis setIfAbsent 操作异常", e);
            return false;
        }
    }

    /**
     * 如果key不存在，则设置值和过期时间并返回true，否则返回false
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return 成功返回true，失败返回false
     */
    public boolean setIfAbsent(String key, Object value, long time) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, value, time, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis setIfAbsent with expire 操作异常", e);
            return false;
        }
    }

    /**
     * 检查并增加计数
     * 用于检查当前用户使用免费额度情况
     */
    public boolean checkAndIncrement(String key, Integer number) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastTime = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 23, 59);
        long seconds = LocalDateTimeUtil.between(now, lastTime).getSeconds();

        try {
            if (!hasKey(key)) {
                return set(key, String.valueOf(number), seconds);
            }
            Integer value = Integer.valueOf(get(key) + "");
            if (value == null || value >= 2) {
                return false;
            }
            incr(key, 1);
            return true;

        } catch (Exception e) {
            log.error("Redis checkAndIncrement 操作异常", e);
            return false;
        }
    }

    /**
     * 检查并增加计数
     * 用于检查当前用户使用免费额度情况
     */
    public boolean checkAndIncrement(String key) {
        if(Objects.equals(61L, UserContextHolder.getUserId())){
            return checkAndIncrement(key, -1);
        } else {
            return checkAndIncrement(key, 1);
        }
    }

    /**
     * 使用 Redis 原生命令 ZPOPMAX 删除 score 最大的一个元素
     */
    public Object popMax(String key) {
        return redisTemplate.execute((RedisConnection connection) -> {
            Set<RedisZSetCommands.Tuple> result = connection.zPopMax(key.getBytes(), 1);
            if (result == null || result.isEmpty()) {
                return null;
            }
            RedisZSetCommands.Tuple tuple = result.iterator().next();
            return new String(tuple.getValue());
        });
    }

} 