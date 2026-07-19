package com.xxl.job.admin.core.alarm;

import com.xxl.job.admin.model.EmailSettings;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemEmailServiceTest {

    @Test
    void shouldBuildSenderFromDatabaseSettings() {
        EmailSettings settings = new EmailSettings();
        settings.setHost("smtp.example.com");
        settings.setPort(465);
        settings.setUsername("alerts@example.com");
        settings.setPassword("secret");
        settings.setSmtpAuth(true);
        settings.setStarttlsEnabled(false);
        settings.setStarttlsRequired(false);
        settings.setSslEnabled(true);

        JavaMailSenderImpl sender = new SystemEmailService().createMailSender(settings);

        assertEquals("smtp.example.com", sender.getHost());
        assertEquals(465, sender.getPort());
        assertEquals("alerts@example.com", sender.getUsername());
        assertEquals("secret", sender.getPassword());
        assertEquals("true", sender.getJavaMailProperties().getProperty("mail.smtp.auth"));
        assertEquals("false", sender.getJavaMailProperties().getProperty("mail.smtp.starttls.enable"));
        assertEquals("true", sender.getJavaMailProperties().getProperty("mail.smtp.ssl.enable"));
    }
}
