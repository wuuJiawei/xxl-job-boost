package com.xxl.job.admin.service;

import com.xxl.job.admin.model.EmailSettings;

public interface SystemConfigService {

    EmailSettings getEmailSettings();

    EmailSettings updateEmailSettings(EmailSettings settings);
}
