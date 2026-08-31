package com.musekit.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Sends an OTP verification email with modern HTML formatting.
     *
     * @param toEmail recipient email address
     * @param otp 6-digit OTP code
     */
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your Verification Code - MuseKit");

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f7; color: #333333; margin: 0; padding: 20px; }
                        .container { max-width: 500px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; padding: 30px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
                        .header { text-align: center; border-bottom: 1px solid #eeeeee; padding-bottom: 20px; margin-bottom: 20px; }
                        .brand { font-size: 24px; font-weight: bold; color: #6366f1; letter-spacing: 1px; }
                        .content { text-align: center; line-height: 1.6; }
                        .otp-box { background: linear-gradient(135deg, #6366f1, #8b5cf6); color: #ffffff; font-size: 32px; font-weight: bold; letter-spacing: 8px; padding: 15px 30px; border-radius: 8px; display: inline-block; margin: 25px 0; }
                        .expiry { color: #6b7280; font-size: 13px; }
                        .footer { margin-top: 30px; border-top: 1px solid #eeeeee; padding-top: 15px; font-size: 12px; color: #9ca3af; text-align: center; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <div class="brand">MuseKit</div>
                        </div>
                        <div class="content">
                            <h2>Email Verification</h2>
                            <p>Use the 6-digit verification code below to complete your authentication request:</p>
                            <div class="otp-box">%s</div>
                            <p class="expiry">This code will expire in <strong>5 minutes</strong>. If you did not request this code, please ignore this email.</p>
                        </div>
                        <div class="footer">
                            &copy; %s MuseKit. All rights reserved.
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(otp, java.time.Year.now().getValue());

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Successfully sent OTP email to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send verification email. Please try again later.", e);
        }
    }
}
