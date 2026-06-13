package com.project.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
        String from,
        int otpLength,
        int otpTtlMinutes,
        int otpMaxAttempts,
        int resendCooldownSeconds) {
}
