package com.dinogo.member.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetMailService {

    private final JavaMailSender mailSender;
    private final String from;
    private final String frontendBaseUrl;

    public PasswordResetMailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String from,
            @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/$", "");
    }

    public void sendPasswordReset(String recipient, String token) {
        String resetUrl = frontendBaseUrl + "/reset-password?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("DinoGo 密碼重設");
        message.setText("我們收到重設 DinoGo 密碼的請求。\n\n"
                + "請在 15 分鐘內開啟以下連結設定新密碼：\n"
                + resetUrl
                + "\n\n若非您本人操作，請忽略此封信件。此連結在密碼重設後會立即失效。");
        mailSender.send(message);
    }
}
