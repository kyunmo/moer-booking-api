package io.moer.booking.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄러 설정
 * @Scheduled 어노테이션을 사용한 배치 작업 활성화
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // @Scheduled 어노테이션을 사용한 스케줄러 활성화
}
