package com.project.modules.auth.service;

import com.project.modules.auth.dto.request.*;
import com.project.modules.auth.dto.response.AuthResponse;
import com.project.modules.auth.dto.response.RefreshResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    RefreshResponse refresh(RefreshTokenRequest request);

    void logout(LogoutRequest request);

    void changePassword(ChangePasswordRequest request);

    void forgotPassword(ForgotPasswordRequest request);
}
