package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.mapper.XxlJobSystemConfigMapper;
import com.xxl.job.admin.model.EmailSettings;
import com.xxl.job.admin.model.XxlJobSystemConfig;
import com.xxl.job.admin.service.SystemConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    static final String MAIL_PREFIX = "mail.";

    @Resource
    private XxlJobSystemConfigMapper xxlJobSystemConfigMapper;

    @Override
    public EmailSettings getEmailSettings() {
        Map<String, String> values = xxlJobSystemConfigMapper.findByPrefix(MAIL_PREFIX).stream()
                .collect(Collectors.toMap(XxlJobSystemConfig::getConfigKey, XxlJobSystemConfig::getConfigValue));

        EmailSettings settings = new EmailSettings();
        settings.setEnabled(booleanValue(values, "mail.enabled", false));
        settings.setHost(value(values, "mail.host", "smtp.qq.com"));
        settings.setPort(intValue(values, "mail.port", 25));
        settings.setUsername(value(values, "mail.username", ""));
        settings.setFrom(value(values, "mail.from", ""));
        settings.setPassword(value(values, "mail.password", ""));
        settings.setPasswordConfigured(!settings.getPassword().isBlank());
        settings.setSmtpAuth(booleanValue(values, "mail.smtp.auth", true));
        settings.setStarttlsEnabled(booleanValue(values, "mail.smtp.starttls.enable", true));
        settings.setStarttlsRequired(booleanValue(values, "mail.smtp.starttls.required", true));
        settings.setSslEnabled(booleanValue(values, "mail.smtp.ssl.enable", false));
        return settings;
    }

    @Override
    @Transactional
    public EmailSettings updateEmailSettings(EmailSettings settings) {
        validate(settings);
        EmailSettings current = getEmailSettings();
        String password = settings.getPassword();
        if (password == null || password.isBlank()) {
            password = current.getPassword();
        }

        Date updateTime = new Date();
        upsert("mail.enabled", String.valueOf(settings.isEnabled()), updateTime);
        upsert("mail.host", trim(settings.getHost()), updateTime);
        upsert("mail.port", String.valueOf(settings.getPort()), updateTime);
        upsert("mail.username", trim(settings.getUsername()), updateTime);
        upsert("mail.from", trim(settings.getFrom()), updateTime);
        upsert("mail.password", password == null ? "" : password, updateTime);
        upsert("mail.smtp.auth", String.valueOf(settings.isSmtpAuth()), updateTime);
        upsert("mail.smtp.starttls.enable", String.valueOf(settings.isStarttlsEnabled()), updateTime);
        upsert("mail.smtp.starttls.required", String.valueOf(settings.isStarttlsRequired()), updateTime);
        upsert("mail.smtp.ssl.enable", String.valueOf(settings.isSslEnabled()), updateTime);
        return getEmailSettings();
    }

    private void validate(EmailSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("邮箱设置不能为空");
        }
        if (settings.getPort() < 1 || settings.getPort() > 65535) {
            throw new IllegalArgumentException("SMTP 端口必须在 1 到 65535 之间");
        }
        if (settings.isEnabled()) {
            if (isBlank(settings.getHost())) {
                throw new IllegalArgumentException("启用邮件后必须填写 SMTP 服务器");
            }
            if (isBlank(settings.getFrom())) {
                throw new IllegalArgumentException("启用邮件后必须填写发件人地址");
            }
            EmailSettings current = getEmailSettings();
            if (settings.isSmtpAuth() && isBlank(settings.getUsername())) {
                throw new IllegalArgumentException("启用 SMTP 认证后必须填写用户名");
            }
            if (settings.isSmtpAuth() && isBlank(settings.getPassword()) && isBlank(current.getPassword())) {
                throw new IllegalArgumentException("启用 SMTP 认证后必须填写密码或授权码");
            }
        }
        if (settings.isStarttlsRequired() && !settings.isStarttlsEnabled()) {
            throw new IllegalArgumentException("强制 STARTTLS 时必须同时启用 STARTTLS");
        }
        if (settings.isSslEnabled() && settings.isStarttlsEnabled()) {
            throw new IllegalArgumentException("SSL 与 STARTTLS 不能同时启用");
        }
    }

    private void upsert(String key, String value, Date updateTime) {
        XxlJobSystemConfig config = new XxlJobSystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setUpdateTime(updateTime);
        xxlJobSystemConfigMapper.upsert(config);
    }

    private String value(Map<String, String> values, String key, String defaultValue) {
        String value = values.get(key);
        return value == null ? defaultValue : value;
    }

    private boolean booleanValue(Map<String, String> values, String key, boolean defaultValue) {
        String value = values.get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    private int intValue(Map<String, String> values, String key, int defaultValue) {
        try {
            return Integer.parseInt(value(values, key, String.valueOf(defaultValue)));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
