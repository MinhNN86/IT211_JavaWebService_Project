package com.project.modules.auth.service;

public interface OtpService {
    void issueOtp(String email);

    void verifyAndReset(String email, String otp, String newPassword);
}
