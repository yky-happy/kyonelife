package com.yky.blog.api.service;

import com.yky.blog.common.exception.BizException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/** 发送邮箱验证码邮件 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCodeService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${blog.mail-from-name:Kyonelife}")
    private String fromName;

    public void sendCode(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(new InternetAddress(from, fromName, "UTF-8").toString());
            helper.setTo(to);
            helper.setSubject("【" + fromName + "】邮箱验证码");
            helper.setText("你的验证码是：" + code + "，5 分钟内有效。如非本人操作请忽略本邮件。");
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("验证码邮件发送失败 to={}: {}", to, e.getMessage());
            throw new BizException("验证码发送失败，请稍后重试");
        }
    }
}
