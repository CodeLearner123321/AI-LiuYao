package com.divination.liuyao.service.impl;

import java.io.UnsupportedEncodingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.divination.liuyao.config.EmailConfig;
import com.divination.liuyao.service.EmailService;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * 邮件服务实现类
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private EmailConfig emailConfig;
    
    @Override
    public void sendSimpleEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailConfig.getFrom());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        
        try {
            mailSender.send(message);
            log.info("简单邮件已发送至: {}", to);
        } catch (Exception e) {
            log.error("发送简单邮件时发生错误: {}", e.getMessage());
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        MimeMessage message = mailSender.createMimeMessage();
        
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(emailConfig.getFrom(), emailConfig.getPersonal());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("HTML邮件已发送至: {}", to);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("发送HTML邮件时发生错误: {}", e.getMessage());
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    @Override
    public void sendVerificationCode(String to, String code) {
        String subject = "爻算云鉴 - 验证码";
        
        // 创建HTML格式的邮件内容
        String htmlContent = "<div style='background-color: #f7f7f7; padding: 20px; font-family: Arial, sans-serif;'>"
                + "<div style='background-color: white; padding: 20px; border-radius: 5px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);'>"
                + "<h2 style='color: #333; text-align: center;'>爻算云鉴</h2>"
                + "<div style='border-top: 1px solid #eee; border-bottom: 1px solid #eee; padding: 20px; margin: 20px 0;'>"
                + "<p>您好，</p>"
                + "<p>您的验证码是: <span style='font-size: 22px; font-weight: bold; color: #007bff;'>" + code + "</span></p>"
                + "<p>该验证码将在10分钟内有效。</p>"
                + "<p>如果您没有请求此验证码，请忽略此邮件。</p>"
                + "</div>"
                + "<p style='font-size: 12px; color: #999; text-align: center;'>© " + java.time.Year.now().getValue()
                + " 爻算云鉴. 保留所有权利。</p>"
                + "</div></div>";
        
        sendHtmlEmail(to, subject, htmlContent);
    }
} 