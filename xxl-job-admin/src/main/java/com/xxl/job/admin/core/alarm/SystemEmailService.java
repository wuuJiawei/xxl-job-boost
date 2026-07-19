package com.xxl.job.admin.core.alarm;

import com.xxl.job.admin.model.EmailSettings;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Service
public class SystemEmailService {

    public void send(EmailSettings settings, String target, String personal, String title, String content) throws Exception {
        JavaMailSenderImpl mailSender = createMailSender(settings);
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
        helper.setFrom(settings.getFrom(), personal);
        helper.setTo(target);
        helper.setSubject(title);
        helper.setText(content, true);
        mailSender.send(mimeMessage);
    }

    JavaMailSenderImpl createMailSender(EmailSettings settings) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        mailSender.setHost(settings.getHost());
        mailSender.setPort(settings.getPort());
        mailSender.setUsername(settings.getUsername());
        mailSender.setPassword(settings.getPassword());

        Properties properties = mailSender.getJavaMailProperties();
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.auth", String.valueOf(settings.isSmtpAuth()));
        properties.put("mail.smtp.starttls.enable", String.valueOf(settings.isStarttlsEnabled()));
        properties.put("mail.smtp.starttls.required", String.valueOf(settings.isStarttlsRequired()));
        properties.put("mail.smtp.ssl.enable", String.valueOf(settings.isSslEnabled()));
        return mailSender;
    }
}
