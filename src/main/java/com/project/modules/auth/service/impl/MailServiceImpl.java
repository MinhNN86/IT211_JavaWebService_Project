package com.project.modules.auth.service.impl;

import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.project.config.MailProperties;
import com.project.modules.auth.service.MailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {
    private static final String FROM_PERSONAL_NAME = "Badminton Court Booking System";

  private final JavaMailSender mailSender;
  private final MailProperties mailProperties;

  @Async
  @Override
  public void sendOtp(String toEmail, String otp) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(mailProperties.from(), FROM_PERSONAL_NAME);
      helper.setTo(toEmail);
      helper.setSubject("Reset your password");
      helper.setText(buildPlainTextContent(otp), buildHtmlContent(otp));
      mailSender.send(message);
    } catch (Exception exception) {
      log.warn("Failed to send OTP email to {}", toEmail, exception);
    }
  }

  private String buildPlainTextContent(String otp) {
    return "Your password reset code is: " + otp
        + "\nIt expires in " + mailProperties.otpTtlMinutes() + " minutes."
        + "\nIf you did not request this, ignore this email.";
  }

  private String buildHtmlContent(String otp) {
    return """
        <!doctype html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Reset your password</title>
        </head>
        <body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,Helvetica,sans-serif;color:#172033;">
          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f7fb;margin:0;padding:32px 16px;">
            <tr>
              <td align="center">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #e6edf5;box-shadow:0 18px 48px rgba(20,35,60,0.12);">
                  <tr>
                    <td style="background:#0f766e;padding:28px 32px;color:#ffffff;">
                      <div style="font-size:13px;font-weight:700;letter-spacing:1.6px;text-transform:uppercase;opacity:0.86;">Badminton Booking</div>
                      <h1 style="margin:12px 0 0;font-size:26px;line-height:1.25;font-weight:700;">Password reset request</h1>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:32px;">
                      <p style="margin:0 0 18px;font-size:16px;line-height:1.6;color:#39465d;">Use the verification code below to reset your password.</p>
                      <div style="margin:24px 0;padding:22px 16px;background:#ecfdf5;border:1px solid #b7ead2;border-radius:8px;text-align:center;">
                        <div style="font-size:12px;font-weight:700;letter-spacing:1.4px;text-transform:uppercase;color:#0f766e;margin-bottom:10px;">Your OTP code</div>
                        <div style="font-family:'Courier New',Courier,monospace;font-size:36px;line-height:1;font-weight:700;letter-spacing:8px;color:#0b3b35;">%s</div>
                      </div>
                      <p style="margin:0 0 16px;font-size:15px;line-height:1.6;color:#39465d;">This code expires in <strong>%d minutes</strong>. For your security, do not share it with anyone.</p>
                      <div style="margin-top:24px;padding:16px 18px;background:#fff7ed;border-left:4px solid #f59e0b;border-radius:6px;color:#7c3f00;font-size:14px;line-height:1.55;">
                        If you did not request a password reset, you can safely ignore this email.
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:18px 32px;background:#f8fafc;color:#6b7485;font-size:12px;line-height:1.5;text-align:center;">
                      This is an automated email. Please do not reply.
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """
        .formatted(otp, mailProperties.otpTtlMinutes());
  }
}
