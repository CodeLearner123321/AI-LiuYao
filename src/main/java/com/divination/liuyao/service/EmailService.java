package com.divination.liuyao.service;

/**
 * 邮件服务接口
 */
public interface EmailService {
    
    /**
     * 发送简单文本邮件
     * 
     * @param to 收件人
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    void sendSimpleEmail(String to, String subject, String content);
    
    /**
     * 发送HTML格式邮件
     * 
     * @param to 收件人
     * @param subject 邮件主题
     * @param htmlContent HTML格式的邮件内容
     */
    void sendHtmlEmail(String to, String subject, String htmlContent);
    
    /**
     * 发送验证码邮件
     * 
     * @param to 收件人
     * @param code 验证码
     */
    void sendVerificationCode(String to, String code);
} 