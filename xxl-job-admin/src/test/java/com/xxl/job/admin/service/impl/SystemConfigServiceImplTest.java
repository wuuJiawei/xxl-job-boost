package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.mapper.XxlJobSystemConfigMapper;
import com.xxl.job.admin.model.EmailSettings;
import com.xxl.job.admin.model.XxlJobSystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceImplTest {

    @Mock
    private XxlJobSystemConfigMapper mapper;

    private SystemConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SystemConfigServiceImpl();
        ReflectionTestUtils.setField(service, "xxlJobSystemConfigMapper", mapper);
    }

    @Test
    void missingValuesShouldUseSafeDefaults() {
        when(mapper.findByPrefix("mail.")).thenReturn(List.of());

        EmailSettings settings = service.getEmailSettings();

        assertFalse(settings.isEnabled());
        assertEquals("smtp.qq.com", settings.getHost());
        assertEquals(25, settings.getPort());
        assertTrue(settings.isSmtpAuth());
        assertTrue(settings.isStarttlsEnabled());
        assertFalse(settings.isSslEnabled());
        assertFalse(settings.isPasswordConfigured());
    }

    @Test
    void blankPasswordShouldPreserveStoredPassword() {
        when(mapper.findByPrefix("mail.")).thenReturn(List.of(config("mail.password", "secret")));
        EmailSettings settings = validSettings();
        settings.setPassword("");

        service.updateEmailSettings(settings);

        ArgumentCaptor<XxlJobSystemConfig> captor = ArgumentCaptor.forClass(XxlJobSystemConfig.class);
        verify(mapper, atLeastOnce()).upsert(captor.capture());
        XxlJobSystemConfig password = captor.getAllValues().stream()
                .filter(item -> "mail.password".equals(item.getConfigKey()))
                .findFirst()
                .orElseThrow();
        assertEquals("secret", password.getConfigValue());
    }

    @Test
    void sslAndStarttlsShouldBeRejectedTogether() {
        EmailSettings settings = validSettings();
        settings.setSslEnabled(true);
        settings.setStarttlsEnabled(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.updateEmailSettings(settings));

        assertEquals("SSL 与 STARTTLS 不能同时启用", exception.getMessage());
    }

    private EmailSettings validSettings() {
        EmailSettings settings = new EmailSettings();
        settings.setEnabled(false);
        settings.setHost("smtp.example.com");
        settings.setPort(587);
        settings.setUsername("user@example.com");
        settings.setFrom("user@example.com");
        settings.setSmtpAuth(true);
        settings.setStarttlsEnabled(true);
        settings.setStarttlsRequired(true);
        settings.setSslEnabled(false);
        return settings;
    }

    private XxlJobSystemConfig config(String key, String value) {
        XxlJobSystemConfig config = new XxlJobSystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        return config;
    }
}
