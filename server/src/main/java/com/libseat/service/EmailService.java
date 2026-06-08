package com.libseat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String from;

    @Value("${app.email.base-url}")
    private String baseUrl;

    @Value("${app.email.token-expiry-hours:24}")
    private int tokenExpiryHours;

    public void sendActivation(String toEmail, String realName, String token) {
        String link = baseUrl + "/api/v1/auth/activate?token=" + token;
        send(toEmail, "激活您的图书馆账号",
             "您好 " + realName + "，\n\n请点击以下链接激活账号（有效期 " + tokenExpiryHours + " 小时）：\n" + link);
    }

    public void sendPasswordReset(String toEmail, String realName, String token) {
        String link = baseUrl + "/api/v1/auth/password/reset?token=" + token;
        send(toEmail, "重置图书馆账号密码",
             "您好 " + realName + "，\n\n请点击以下链接重置密码（有效期 " + tokenExpiryHours + " 小时）：\n" + link);
    }

    public void sendEmailChange(String toEmail, String realName, String token) {
        String link = baseUrl + "/api/v1/auth/email/confirm?token=" + token;
        send(toEmail, "确认更改图书馆账号邮箱",
             "您好 " + realName + "，\n\n请点击以下链接确认新邮箱（有效期 " + tokenExpiryHours + " 小时）：\n" + link);
    }

    private void send(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }
}
