package io.moer.booking.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 처리 설정
 * - 이메일 발송을 비동기로 처리
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring Boot의 기본 TaskExecutor 사용
    // 필요시 커스텀 Executor 설정 가능
}
