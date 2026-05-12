package io.moer.booking.common.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 인메모리 토큰 버킷 기반 Rate Limiter.
 *
 * SECURITY (P1-3): IP / 계정 기반 요청 제한 (Brute-force, DoS, 스팸 방어).
 *
 * - 단일 인스턴스 배포 가정. 멀티 인스턴스 시 Redis/Hazelcast 백엔드로 확장 필요.
 * - 키 형식: "{policy}:{identifier}" 예: "login:ip:1.2.3.4", "login:email:user@example.com"
 */
@Service
public class RateLimiterService {

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * 정책별 버킷을 가져오거나 새로 생성.
     */
    public Bucket resolveBucket(String key, RateLimitPolicy policy) {
        return buckets.computeIfAbsent(key, k -> createBucket(policy));
    }

    /**
     * 키에 대해 1 토큰 소비 시도. 성공/실패 + 남은 시간 정보 반환.
     */
    public ConsumptionProbe tryConsume(String key, RateLimitPolicy policy) {
        return resolveBucket(key, policy).tryConsumeAndReturnRemaining(1);
    }

    /**
     * 특정 키의 카운트 리셋 (로그인 성공 시 호출).
     */
    public void reset(String key) {
        buckets.remove(key);
    }

    private Bucket createBucket(RateLimitPolicy policy) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(policy.capacity)
                .refillIntervally(policy.capacity, Duration.ofSeconds(policy.windowSeconds))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Rate Limit 정책 정의.
     */
    public enum RateLimitPolicy {
        /** 로그인: IP 또는 계정당 15분에 5회 */
        LOGIN(5, 900),
        /** 비밀번호 재설정 요청: IP당 시간당 3회 */
        FORGOT_PASSWORD(3, 3600),
        /** 이메일 중복 확인: IP당 분당 10회 */
        CHECK_EMAIL(10, 60),
        /** 회원가입: IP당 시간당 5회 */
        REGISTER(5, 3600),
        /** Public 검색 API: IP당 분당 60회 */
        PUBLIC_SEARCH(60, 60);

        public final long capacity;
        public final long windowSeconds;

        RateLimitPolicy(long capacity, long windowSeconds) {
            this.capacity = capacity;
            this.windowSeconds = windowSeconds;
        }
    }
}
