package com.divination.liuyao.util;

import com.divination.liuyao.pojo.entity.User;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户上下文持有者，用于在线程中存储和获取当前用户信息
 */
@Slf4j
public class UserContextHolder {
    
    private static final ThreadLocal<User> userThreadLocal = new ThreadLocal<>();
    
    /**
     * 设置当前线程的用户信息
     * 
     * @param user 用户信息
     */
    public static void setUser(User user) {
        userThreadLocal.set(user);
    }
    
    /**
     * 获取当前线程的用户信息
     * 
     * @return 用户信息
     */
    public static User getUser() {
        return userThreadLocal.get();
    }
    
    /**
     * 获取当前用户ID
     * 
     * @return 用户ID，如果未设置用户则返回null
     */
    public static Long getUserId() {
        User user = getUser();
        return user != null ? user.getId() : null;
    }
    
    /**
     * 获取当前用户名
     * 
     * @return 用户名，如果未设置用户则返回null
     */
    public static String getUsername() {
        User user = getUser();
        return user != null ? user.getUserName() : null;
    }
    
    /**
     * 判断当前用户是否为VIP
     * 
     * @return 如果用户是VIP返回true，否则返回false
     */
    public static boolean isVip() {
        User user = getUser();
        return user != null && user.getIsVip() != null && user.getIsVip() == 1;
    }
    
    /**
     * 清除当前线程的用户信息
     * 应在请求结束时调用，防止内存泄漏
     */
    public static void clear() {
        userThreadLocal.remove();
    }
} 