package io.moer.booking.common.service;

import io.moer.booking.common.util.MaskingUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;

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

            // SECURITY (P1-7): PII 로그 마스킹
            log.info("Password reset email sent successfully: to={}, token={}",
                    MaskingUtils.maskEmail(to), MaskingUtils.maskToken(token));

        } catch (MessagingException e) {
            log.error("Failed to send password reset email: to={}, error={}",
                    MaskingUtils.maskEmail(to), e.getMessage(), e);
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
            log.info("Test email sent successfully: to={}", MaskingUtils.maskEmail(to));

        } catch (MessagingException e) {
            log.error("Failed to send test email: to={}, error={}", MaskingUtils.maskEmail(to), e.getMessage(), e);
            throw new RuntimeException("테스트 이메일 발송 실패", e);
        }
    }

    // ========================================
    // 배치 작업용 이메일 발송
    // ========================================

    /**
     * 체험판 종료 7일 전 알림
     */
    @Async
    public void sendTrialExpirationReminder(String email, String userName, String businessName, LocalDateTime expiresAt) {
        String subject = "[moer] 체험판 종료 7일 전 알림";
        String content = String.format(
            "안녕하세요, %s님!\n\n" +
            "매장 '%s'의 체험판이 %s에 종료됩니다.\n\n" +
            "체험판 종료 후에도 계속 사용하시려면 유료 플랜으로 업그레이드해주세요.\n\n" +
            "감사합니다.\n" +
            "moer 팀",
            userName, businessName, expiresAt
        );
        sendSimpleEmail(email, subject, content);
    }

    /**
     * 구독 만료 알림
     */
    @Async
    public void sendSubscriptionExpiredNotification(String email, String userName, String businessName) {
        String subject = "[moer] 구독이 만료되었습니다";
        String content = String.format(
            "안녕하세요, %s님!\n\n" +
            "매장 '%s'의 구독이 만료되었습니다.\n\n" +
            "결제 실패로 인해 서비스 이용이 제한될 수 있습니다.\n" +
            "결제 정보를 확인하시고 다시 시도해주세요.\n\n" +
            "감사합니다.\n" +
            "moer 팀",
            userName, businessName
        );
        sendSimpleEmail(email, subject, content);
    }

    /**
     * 간단한 텍스트 이메일 발송
     */
    private void sendSimpleEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            message.setFrom(fromEmail);

            mailSender.send(message);
            log.info("이메일 발송 완료: to={}, subject={}", MaskingUtils.maskEmail(to), subject);
        } catch (Exception e) {
            log.error("이메일 발송 실패: to={}, subject={}, error={}",
                    MaskingUtils.maskEmail(to), subject, e.getMessage(), e);
        }
    }
}
