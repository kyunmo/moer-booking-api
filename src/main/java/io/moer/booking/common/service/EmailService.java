package io.moer.booking.common.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * 이메일 발송 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.reset-password-url}")
    private String resetPasswordUrl;

    /**
     * 비밀번호 재설정 이메일 발송 (비동기)
     */
    @Async
    public void sendPasswordResetEmail(String to, String name, String token) {
        try {
            String resetUrl = resetPasswordUrl + "?token=" + token;

            // Thymeleaf 컨텍스트 설정
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("resetUrl", resetUrl);
            context.setVariable("expirationMinutes", 30);

            // HTML 템플릿 렌더링
            String htmlContent = templateEngine.process("email/password-reset", context);

            // 이메일 메시지 생성
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("[moer] 비밀번호 재설정 안내");
            helper.setText(htmlContent, true);

            // 이메일 발송
            mailSender.send(message);

            log.info("Password reset email sent successfully: to={}, token={}", to, token.substring(0, 8) + "...");

        } catch (MessagingException e) {
            log.error("Failed to send password reset email: to={}, error={}", to, e.getMessage(), e);
            throw new RuntimeException("이메일 발송에 실패했습니다", e);
        }
    }

    /**
     * 테스트용 이메일 발송
     */
    @Async
    public void sendTestEmail(String to) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("[moer] 테스트 이메일");
            helper.setText("이메일 발송 테스트입니다.", false);

            mailSender.send(message);
            log.info("Test email sent successfully: to={}", to);

        } catch (MessagingException e) {
            log.error("Failed to send test email: to={}, error={}", to, e.getMessage(), e);
            throw new RuntimeException("테스트 이메일 발송 실패", e);
        }
    }
}
