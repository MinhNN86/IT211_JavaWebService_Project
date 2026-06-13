package com.project.modules.auth.service;

public interface MailService {
    void sendOtp(String toEmail, String otp);
}
