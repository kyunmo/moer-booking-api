# Backend API 요구사항 구현 계획

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 프론트엔드 요구사항 문서의 12개 API를 P0→P4 우선순위로 구현

**Architecture:** 기존 Spring Boot + MyBatis Layered Architecture 패턴을 따름. 각 API는 Controller → Service → Repository → Mapper XML 순서로 구현. 기존 코드 확장 우선, 필요 시 신규 테이블/도메인 추가.

**Tech Stack:** Spring Boot 4.0.1, Java 17, PostgreSQL 16, MyBatis 4.0.0, Lombok

---

## Task 1: 신규 에러코드 추가 (공통)

**Files:**
- Modify: `src/main/java/io/moer/booking/common/exception/ErrorCode.java`

**Step 1: ErrorCode enum에 신규 코드 추가**

기존 `BC003` 뒤에 추가:

```java
    // 서비스 이미지 (IMG001~IMG005)
    SERVICE_IMAGE_UNSUPPORTED_FORMAT(HttpStatus.BAD_REQUEST, "IMG001", "지원하지 않는 파일 형식입니다. (jpg, jpeg, png, webp만 허용)"),
    SERVICE_IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "IMG002", "파일 크기가 초과되었습니다. (최대 10MB)"),
    SERVICE_IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "IMG003", "서비스 이미지는 최대 3장까지 등록 가능합니다"),
    SERVICE_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "IMG004", "이미지를 찾을 수 없습니다"),
    SERVICE_IMAGE_ORDER_MISMATCH(HttpStatus.BAD_REQUEST, "IMG005", "이미지 ID 목록이 서비스의 이미지와 일치하지 않습니다"),

    // 알림 발송 (NTF001~NTF003)
    NOTIFICATION_TARGET_EMPTY(HttpStatus.BAD_REQUEST, "NTF001", "발송 대상이 없습니다"),
    NOTIFICATION_INVALID_CHANNEL(HttpStatus.BAD_REQUEST, "NTF002", "유효하지 않은 발송 채널입니다"),
    KAKAO_CHANNEL_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "NTF003", "카카오 채널 ID 인증에 실패했습니다"),

    // 고객 CRM (CRM001~CRM005)
    CUSTOMER_NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "CRM001", "메모를 찾을 수 없습니다"),
    CUSTOMER_TAG_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "CRM002", "태그는 최대 10개까지 등록 가능합니다"),
    CUSTOMER_TAG_LENGTH_EXCEEDED(HttpStatus.BAD_REQUEST, "CRM003", "태그 이름은 최대 20자까지 입력 가능합니다"),
    CUSTOMER_MERGE_PRIMARY_CONFLICT(HttpStatus.BAD_REQUEST, "CRM004", "주 고객 ID가 병합 목록에 포함될 수 없습니다"),
    CUSTOMER_MERGE_EMPTY(HttpStatus.BAD_REQUEST, "CRM005", "병합할 고객이 없습니다"),

    // 플랫폼 통계 (STAT001)
    PLATFORM_STATS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "STAT001", "통계 데이터를 일시적으로 사용할 수 없습니다"),

    // 도움말 (PUB001)
    HELP_ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "PUB001", "도움말 콘텐츠를 찾을 수 없습니다"),

    // 예약 일정 변경 (RS001~RS004)
    RESCHEDULE_TIME_CONFLICT(HttpStatus.CONFLICT, "RS001", "해당 시간대에 직원이 이미 예약되어 있습니다"),
    RESCHEDULE_OUTSIDE_HOURS(HttpStatus.BAD_REQUEST, "RS002", "직원의 근무 시간 외의 시간입니다"),
    RESCHEDULE_HOLIDAY(HttpStatus.BAD_REQUEST, "RS003", "휴무일에는 예약을 등록할 수 없습니다"),
    RESCHEDULE_INVALID_STATUS(HttpStatus.BAD_REQUEST, "RS004", "취소/완료된 예약은 일정을 변경할 수 없습니다"),

    // 직원 스케줄 조회 (SCH001~SCH002)
    SCHEDULE_DATE_RANGE_EXCEEDED(HttpStatus.BAD_REQUEST, "SCH001", "조회 기간이 최대 31일을 초과합니다"),
    SCHEDULE_INVALID_DATE_ORDER(HttpStatus.BAD_REQUEST, "SCH002", "시작일이 종료일보다 늦습니다"),

    // 알림 발송 한도 (NTF004)
    NOTIFICATION_SEND_LIMIT_EXCEEDED(HttpStatus.FORBIDDEN, "NTF004", "이번 달 알림 발송 한도를 초과했습니다"),

    // CSV 내보내기 (EX001~EX002)
    EXPORT_PLAN_RESTRICTED(HttpStatus.FORBIDDEN, "EX001", "해당 플랜에서는 CSV 내보내기를 사용할 수 없습니다"),
    EXPORT_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "EX002", "내보내기 요청이 너무 많습니다. 잠시 후 다시 시도해주세요");
```

**Step 2: 빌드 확인**

Run: `cd C:/Project/moer-booking && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/io/moer/booking/common/exception/ErrorCode.java
git commit -m "feat: 신규 API용 에러코드 추가 (IMG, NTF, CRM, STAT, PUB, RS, SCH, EX)"
```

---

## Task 2: P0 - FREE 플랜 기본 통계 API

**Files:**
- Create: `src/main/java/io/moer/booking/domain/dashboard/dto/BasicStatsResponse.java`
- Modify: `src/main/java/io/moer/booking/domain/dashboard/service/DashboardService.java`
- Modify: `src/main/java/io/moer/booking/domain/dashboard/controller/DashboardController.java`

**Step 1: BasicStatsResponse DTO 생성**

```java
package io.moer.booking.domain.dashboard.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class BasicStatsResponse {
    private PeriodStats today;
    private PeriodStats thisWeek;
    private int pendingReservations;
    private int unreadReviews;
    private LocalDateTime generatedAt;

    @Getter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PeriodStats {
        private int reservationCount;
        private int completedCount;
        private int cancelledCount;
        private long revenue;
    }
}
```

**Step 2: DashboardService에 getBasicStats() 추가**

기존 `getDashboardStats()` 메서드의 today/week 로직을 재사용. 플랜 체크 없음.

```java
public BasicStatsResponse getBasicStats(Long businessId, LocalDate date) {
    // 기존 todayStats, weekStats 쿼리 재사용
    // pendingReservations: reservationRepository count by status=PENDING
    // unreadReviews: reviewRepository count by isRead=false (or 신규 리뷰 카운트)
    // generatedAt: LocalDateTime.now()
}
```

**Step 3: DashboardController에 엔드포인트 추가**

```java
@GetMapping("/dashboard/basic-stats")
public ApiResponse<BasicStatsResponse> getBasicStats(
        @PathVariable Long businessId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
    User user = userDetails.getUser();
    user.canAccessBusiness(businessId); // 권한 체크
    BasicStatsResponse response = dashboardService.getBasicStats(businessId, LocalDate.now());
    return ApiResponse.success(response);
}
```

**Step 4: 빌드 확인**

Run: `./gradlew compileJava`

**Step 5: Commit**

```bash
git add -A
git commit -m "feat: FREE 플랜 기본 통계 API (GET /dashboard/basic-stats)"
```

---

## Task 3: P0 - 회원가입 플로우 변경

**Files:**
- Modify: `src/main/java/io/moer/booking/domain/auth/dto/RegisterRequest.java`
- Modify: `src/main/java/io/moer/booking/domain/auth/service/AuthService.java`

**Step 1: RegisterRequest 수정**

`selectedPlan` 필드의 `@NotNull` 제거 (이미 optional일 수 있음 확인), `getSubscriptionPlan()` 메서드가 항상 BASIC을 반환하도록 변경하지 않고, AuthService에서 무시 처리.

**Step 2: AuthService.register() 수정**

`request.getSubscriptionPlan()` 대신 항상 `SubscriptionPlan.FREE` (또는 TRIAL에 해당하는 값)를 사용. plan/billingCycle 파라미터를 무시하도록 변경:

```java
// 변경 전:
.subscriptionPlan(request.getSubscriptionPlan())
// 변경 후:
.subscriptionPlan(SubscriptionPlan.FREE)  // 가입 시 항상 FREE, 30일 TRIAL 자동 적용
```

**Step 3: RegisterResponse에 subscription 정보 추가**

기존 `TrialInfo` 필드가 이미 있으므로 프론트엔드가 요청한 `subscription` 구조와 매핑 확인.

**Step 4: 빌드 확인 및 Commit**

```bash
./gradlew compileJava
git add -A
git commit -m "feat: 회원가입 시 plan 필드 무시, 자동 TRIAL 30일 적용"
```

---

## Task 4: P1 - 서비스 이미지 DB 테이블 및 엔티티

**Files:**
- Modify: `src/main/resources/db/schema.sql`
- Create: `src/main/java/io/moer/booking/domain/service/ServiceImage.java`
- Create: `src/main/java/io/moer/booking/domain/service/dto/ServiceImageResponse.java`

**Step 1: schema.sql에 service_images 테이블 추가**

```sql
-- 서비스 이미지
CREATE TABLE IF NOT EXISTS service_images (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    business_id BIGINT NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    original_filename VARCHAR(255),
    file_size BIGINT DEFAULT 0,
    sort_order INTEGER DEFAULT 0,
    caption VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_service_images_service_id ON service_images(service_id);
```

**Step 2: ServiceImage 엔티티**

```java
package io.moer.booking.domain.service;

import lombok.*;
import java.time.LocalDateTime;

@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceImage {
    private Long id;
    private Long serviceId;
    private Long businessId;
    private String imageUrl;
    private String thumbnailUrl;
    private String originalFilename;
    private Long fileSize;
    private Integer sortOrder;
    private String caption;
    private LocalDateTime createdAt;
}
```

**Step 3: ServiceImageResponse DTO**

```java
package io.moer.booking.domain.service.dto;

import io.moer.booking.domain.service.ServiceImage;
import lombok.*;
import java.time.LocalDateTime;

@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceImageResponse {
    private Long id;
    private Long serviceId;
    private String imageUrl;
    private String thumbnailUrl;
    private Integer sortOrder;
    private String caption;
    private LocalDateTime createdAt;

    public static ServiceImageResponse from(ServiceImage image) {
        return ServiceImageResponse.builder()
                .id(image.getId())
                .serviceId(image.getServiceId())
                .imageUrl(image.getImageUrl())
                .thumbnailUrl(image.getThumbnailUrl())
                .sortOrder(image.getSortOrder())
                .caption(image.getCaption())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
```

**Step 4: Commit**

```bash
git add -A
git commit -m "feat: 서비스 이미지 테이블, 엔티티, DTO 생성"
```

---

## Task 5: P1 - 서비스 이미지 Repository + Mapper XML

**Files:**
- Create: `src/main/java/io/moer/booking/domain/service/repository/ServiceImageRepository.java`
- Create: `src/main/resources/mapper/service/ServiceImageMapper.xml`

**Step 1: ServiceImageRepository 인터페이스**

```java
package io.moer.booking.domain.service.repository;

import io.moer.booking.domain.service.ServiceImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ServiceImageRepository {
    void save(ServiceImage image);
    Optional<ServiceImage> findById(@Param("id") Long id);
    List<ServiceImage> findByServiceId(@Param("serviceId") Long serviceId);
    int countByServiceId(@Param("serviceId") Long serviceId);
    void deleteById(@Param("id") Long id);
    void updateSortOrder(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);
    void deleteByServiceId(@Param("serviceId") Long serviceId);
}
```

**Step 2: ServiceImageMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="io.moer.booking.domain.service.repository.ServiceImageRepository">

    <resultMap id="serviceImageResultMap" type="io.moer.booking.domain.service.ServiceImage">
        <id property="id" column="id"/>
        <result property="serviceId" column="service_id"/>
        <result property="businessId" column="business_id"/>
        <result property="imageUrl" column="image_url"/>
        <result property="thumbnailUrl" column="thumbnail_url"/>
        <result property="originalFilename" column="original_filename"/>
        <result property="fileSize" column="file_size"/>
        <result property="sortOrder" column="sort_order"/>
        <result property="caption" column="caption"/>
        <result property="createdAt" column="created_at"/>
    </resultMap>

    <insert id="save" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO service_images (service_id, business_id, image_url, thumbnail_url,
                                     original_filename, file_size, sort_order, caption)
        VALUES (#{serviceId}, #{businessId}, #{imageUrl}, #{thumbnailUrl},
                #{originalFilename}, #{fileSize}, #{sortOrder}, #{caption})
    </insert>

    <select id="findById" resultMap="serviceImageResultMap">
        SELECT * FROM service_images WHERE id = #{id}
    </select>

    <select id="findByServiceId" resultMap="serviceImageResultMap">
        SELECT * FROM service_images WHERE service_id = #{serviceId} ORDER BY sort_order ASC
    </select>

    <select id="countByServiceId" resultType="int">
        SELECT COUNT(*) FROM service_images WHERE service_id = #{serviceId}
    </select>

    <delete id="deleteById">
        DELETE FROM service_images WHERE id = #{id}
    </delete>

    <update id="updateSortOrder">
        UPDATE service_images SET sort_order = #{sortOrder} WHERE id = #{id}
    </update>

    <delete id="deleteByServiceId">
        DELETE FROM service_images WHERE service_id = #{serviceId}
    </delete>

</mapper>
```

**Step 3: Commit**

```bash
git add -A
git commit -m "feat: 서비스 이미지 Repository + MyBatis Mapper XML"
```

---

## Task 6: P1 - 서비스 이미지 Service + Controller (4종 API)

**Files:**
- Create: `src/main/java/io/moer/booking/domain/service/service/ServiceImageService.java`
- Create: `src/main/java/io/moer/booking/domain/service/controller/ServiceImageController.java`
- Create: `src/main/java/io/moer/booking/domain/service/dto/ImageSortRequest.java`

**Step 1: ImageSortRequest DTO**

```java
package io.moer.booking.domain.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Getter @NoArgsConstructor @AllArgsConstructor
public class ImageSortRequest {
    @NotEmpty
    @Valid
    private List<ImageOrder> imageOrders;

    @Getter @NoArgsConstructor @AllArgsConstructor
    public static class ImageOrder {
        @NotNull private Long imageId;
        @NotNull private Integer sortOrder;
    }
}
```

**Step 2: ServiceImageService**

```java
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ServiceImageService {
    private final ServiceImageRepository serviceImageRepository;
    private final ServiceRepository serviceRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public ServiceImageResponse uploadImage(Long businessId, Long serviceId,
                                             MultipartFile file, Integer sortOrder, String caption) {
        // 1. 서비스 존재 확인
        // 2. 이미지 수 확인 (max 3)
        // 3. FileStorageService.store(file, "services/" + serviceId)
        // 4. ServiceImage 엔티티 생성 및 저장
        // 5. ServiceImageResponse.from() 반환
    }

    public List<ServiceImageResponse> getImages(Long serviceId) { ... }

    @Transactional
    public void deleteImage(Long businessId, Long serviceId, Long imageId) {
        // 1. 이미지 존재 확인
        // 2. FileStorageService.delete(image.getImageUrl())
        // 3. serviceImageRepository.deleteById()
    }

    @Transactional
    public void updateSortOrder(Long businessId, Long serviceId, ImageSortRequest request) {
        // 1. 서비스의 이미지 목록 조회
        // 2. 요청 imageId 목록과 실제 이미지 ID 비교 (IMG005)
        // 3. 각 이미지 sortOrder 업데이트
    }
}
```

**Step 3: ServiceImageController**

```java
@RestController
@RequestMapping("/api/businesses/{businessId}/services/{serviceId}/images")
@RequiredArgsConstructor
public class ServiceImageController {
    private final ServiceImageService serviceImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ServiceImageResponse>> uploadImage(
            @PathVariable Long businessId, @PathVariable Long serviceId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sortOrder", required = false) Integer sortOrder,
            @RequestParam(value = "caption", required = false) String caption,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        userDetails.getUser().canAccessBusiness(businessId);
        ServiceImageResponse response = serviceImageService.uploadImage(businessId, serviceId, file, sortOrder, caption);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ApiResponse<List<ServiceImageResponse>> getImages(
            @PathVariable Long businessId, @PathVariable Long serviceId,
            @AuthenticationPrincipal CustomUserDetails userDetails) { ... }

    @DeleteMapping("/{imageId}")
    public ApiResponse<Void> deleteImage(
            @PathVariable Long businessId, @PathVariable Long serviceId,
            @PathVariable Long imageId,
            @AuthenticationPrincipal CustomUserDetails userDetails) { ... }

    @PatchMapping("/sort")
    public ApiResponse<Void> updateSortOrder(
            @PathVariable Long businessId, @PathVariable Long serviceId,
            @Valid @RequestBody ImageSortRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) { ... }
}
```

**Step 4: Public 엔드포인트 추가**

`PublicBookingController` 또는 `PublicBusinessController`에:
```java
@GetMapping("/public/businesses/{slug}/services/{serviceId}/images")
```

**Step 5: 빌드 확인 및 Commit**

```bash
./gradlew compileJava
git add -A
git commit -m "feat: 서비스 이미지 CRUD API 4종 (업로드/조회/삭제/순서변경)"
```

---

## Task 7: P1 - 리뷰 이미지 업로드 개선 (multipart 동시 업로드)

**Files:**
- Modify: `src/main/java/io/moer/booking/domain/review/controller/CustomerReviewController.java`
- Modify: `src/main/java/io/moer/booking/domain/review/service/ReviewService.java`
- Create: `src/main/java/io/moer/booking/domain/review/dto/ReviewCreateMultipartRequest.java`

**Step 1: CustomerReviewController 리뷰 생성 메서드를 multipart 지원으로 변경**

기존 `@RequestBody ReviewCreateRequest` → multipart 파라미터로 변경:

```java
@PostMapping(value = "/businesses/{slug}/reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
        @PathVariable String slug,
        @RequestParam("rating") Integer rating,
        @RequestParam("content") String content,
        @RequestParam("reservationNumber") String reservationNumber,
        @RequestParam(value = "images", required = false) List<MultipartFile> images,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
    // ReviewService.createReviewWithImages() 호출
}
```

**하위 호환**: 기존 JSON body 엔드포인트도 유지 (Content-Type으로 분기 또는 별도 매핑)

**Step 2: ReviewService.createReviewWithImages() 추가**

기존 `createReviewByCustomer()` 로직 + 이미지 저장 로직 결합.

**Step 3: 기존 이미지 업로드 stub 수정**

`CustomerReviewController.uploadImage()`에서 `FileStorageService.store()` 호출하도록 수정.

**Step 4: 빌드 확인 및 Commit**

```bash
./gradlew compileJava
git add -A
git commit -m "feat: 리뷰 생성 시 multipart 이미지 동시 업로드 지원"
```

---

## Task 8: P1 - SSE 이벤트 타입 확장

**Files:**
- Modify: `src/main/java/io/moer/booking/domain/notification/service/SseEmitterService.java`
- Create: `src/main/java/io/moer/booking/domain/notification/dto/SseEventData.java`
- Modify: 예약/리뷰 서비스에서 SSE 이벤트 발송 코드 추가

**Step 1: SseEventData DTO 생성**

```java
package io.moer.booking.domain.notification.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class SseEventData {
    private String type;
    private Long referenceId;
    private String reservationNumber;
    private String customerName;
    private String serviceName;
    private String staffName;
    private String startTime;
    private String reason;
    private Integer rating;
    private String contentPreview;
    private String message;
    private LocalDateTime createdAt;
}
```

**Step 2: SseEmitterService에 HEARTBEAT 스케줄러 추가**

```java
@Scheduled(fixedRate = 30000)  // 30초
public void sendHeartbeat() {
    Map<String, Object> heartbeat = Map.of("type", "HEARTBEAT", "timestamp", LocalDateTime.now());
    emitters.forEach((userId, emitter) -> {
        try { emitter.send(SseEmitter.event().name("HEARTBEAT").data(heartbeat)); }
        catch (Exception e) { emitters.remove(userId); }
    });
}
```

**Step 3: sendEventToBusinessOwner() 구현**

UserRepository에서 businessId로 ownerId 조회 → sendEvent(ownerId, ...) 호출.

**Step 4: 예약/리뷰 서비스에서 이벤트 발송**

- `ReservationService`: 예약 생성 시 `RESERVATION_CREATED`, 취소 시 `RESERVATION_CANCELLED` 발송
- `ReviewService`: 리뷰 생성 시 `REVIEW_CREATED` 발송

**Step 5: 빌드 확인 및 Commit**

```bash
./gradlew compileJava
git add -A
git commit -m "feat: SSE 이벤트 타입 확장 (RESERVATION_CREATED/CANCELLED, REVIEW_CREATED, HEARTBEAT)"
```

---

## Task 9: P2 - 예약 시간/날짜 변경 API (reschedule)

**Files:**
- Create: `src/main/java/io/moer/booking/domain/reservation/dto/RescheduleRequest.java`
- Create: `src/main/java/io/moer/booking/domain/reservation/dto/RescheduleResponse.java`
- Modify: `src/main/java/io/moer/booking/domain/reservation/service/ReservationService.java`
- Modify: `src/main/java/io/moer/booking/domain/reservation/controller/ReservationController.java`

**Step 1: RescheduleRequest DTO**

```java
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class RescheduleRequest {
    @NotNull private LocalDate newDate;
    @NotNull private LocalTime newStartTime;
    private LocalTime newEndTime;       // null이면 기존 소요시간 유지
    private Long staffId;               // null이면 기존 직원 유지
    private Boolean notifyCustomer;     // 기본값 true
}
```

**Step 2: RescheduleResponse DTO**

```java
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class RescheduleResponse {
    private Long id;
    private String reservationNumber;
    private String customerName;
    private String serviceName;
    private String staffName;
    private LocalDate previousDate;
    private LocalTime previousStartTime;
    private LocalDate newDate;
    private LocalTime newStartTime;
    private LocalTime newEndTime;
    private String status;
    private LocalDateTime updatedAt;
}
```

**Step 3: ReservationService.reschedule() 구현**

기존 시간 충돌 검증 (`checkTimeConflict`), 근무시간 검증, 휴무일 검증 로직 재사용.

**Step 4: ReservationController에 엔드포인트 추가**

```java
@PatchMapping("/{reservationId}/reschedule")
```

**Step 5: 빌드 확인 및 Commit**

```bash
./gradlew compileJava
git add -A
git commit -m "feat: 예약 시간/날짜 변경 API (PATCH /reservations/{id}/reschedule)"
```

---

## Task 10: P2 - 직원 주간 스케줄 조회 API

**Files:**
- Create: `src/main/java/io/moer/booking/domain/staff/dto/StaffScheduleViewResponse.java`
- Modify: `src/main/java/io/moer/booking/domain/staff/service/StaffService.java`
- Modify: `src/main/java/io/moer/booking/domain/staff/controller/StaffController.java`
- Modify: `src/main/resources/mapper/reservation/ReservationMapper.xml` (날짜 범위 조회 쿼리)

**Step 1: StaffScheduleViewResponse DTO**

프론트엔드 요구사항 문서의 출력값 구조 그대로 구현:
- staffId, staffName, startDate, endDate
- reservations[] (id, reservationNumber, customerName, customerPhone, serviceName, startTime, endTime, status, color)
- workSchedule[] (date, dayOfWeek, isWorkDay, workStartTime, workEndTime, breakStartTime, breakEndTime)
- blockedSlots[] (date, startTime, endTime, reason) — 향후 확장용, 현재 빈 배열

**Step 2: ReservationMapper에 날짜 범위 조회 쿼리 추가**

```xml
<select id="findByStaffIdAndDateRange" resultMap="reservationResultMap">
    SELECT * FROM reservations
    WHERE business_id = #{businessId}
      AND staff_id = #{staffId}
      AND reservation_date BETWEEN #{startDate} AND #{endDate}
      AND status != 'CANCELLED'
    ORDER BY reservation_date, start_time
</select>
```

**Step 3: StaffService.getStaffSchedule() 구현**

**Step 4: StaffController에 엔드포인트 추가**

```java
@GetMapping("/{staffId}/schedule")
```

**Step 5: 빌드 확인 및 Commit**

```bash
./gradlew compileJava
git add -A
git commit -m "feat: 직원 주간 스케줄 조회 API (GET /staffs/{staffId}/schedule)"
```

---

## Task 11: P2 - 관리자 고객 알림 발송 API

**Files:**
- Create: `src/main/java/io/moer/booking/domain/notification/dto/NotificationSendRequest.java`
- Create: `src/main/java/io/moer/booking/domain/notification/dto/NotificationSendResponse.java`
- Modify: `src/main/java/io/moer/booking/domain/notification/service/NotificationService.java`
- Modify: `src/main/java/io/moer/booking/domain/notification/controller/NotificationController.java`

**Step 1: NotificationSendRequest/Response DTO**

요구사항 문서의 입력/출력 구조 그대로 구현.

**Step 2: NotificationService.sendToCustomers() 구현**

1. targetType에 따라 대상 고객 조회 (기존 CustomerRepository 세그먼트 쿼리 활용)
2. 대상이 0명이면 NTF001 예외
3. 각 고객에 대해 Notification 생성 + 저장
4. SSE로 실시간 전달 (연결 중인 고객)
5. notification_logs에 발송 이력 기록
6. 실제 외부 채널(카카오, SMS)은 인터페이스만 로깅

**Step 3: NotificationController에 엔드포인트 추가**

```java
@PostMapping("/businesses/{businessId}/notifications/send")
```

기존 `/api/notifications` 경로와 별개로 비즈니스 하위 경로.

**Step 4: 빌드 확인 및 Commit**

```bash
./gradlew compileJava
git add -A
git commit -m "feat: 관리자 고객 알림 발송 API (POST /notifications/send)"
```

---

## Task 12: P3 - 카카오 알림톡 설정 API

**Files:**
- Create: `src/main/java/io/moer/booking/domain/business/dto/KakaoAlimtalkSettingsRequest.java`
- Create: `src/main/java/io/moer/booking/domain/business/dto/KakaoAlimtalkSettingsResponse.java`
- Modify: 기존 `BusinessSettingsController` (또는 신규 컨트롤러)
- Modify: `BusinessSettingsMapper.xml` (settings JSONB 활용)

**Step 1: Request/Response DTO**

요구사항 문서의 구조 그대로:
- isEnabled, channelId, senderId, triggers (onReservationCreated, onReservationConfirmed, ...)

**Step 2: 기존 business_settings 테이블의 JSONB 컬럼 활용**

`business_settings.kakao_alimtalk_settings` 컬럼을 JSONB로 추가하거나, 기존 설정 컬럼에 포함.

**Step 3: GET/PUT 엔드포인트 구현**

```java
@GetMapping("/businesses/{businessId}/settings/kakao-alimtalk")
@PutMapping("/businesses/{businessId}/settings/kakao-alimtalk")
```

**Step 4: 빌드 확인 및 Commit**

```bash
./gradlew compileJava
git add -A
git commit -m "feat: 카카오 알림톡 설정 API (GET/PUT /settings/kakao-alimtalk)"
```

---

## Task 13: P3 - 고객 CRM API - 메모 (CRUD)

**Files:**
- Modify: `src/main/resources/db/schema.sql` (customer_notes 테이블)
- Create: `src/main/java/io/moer/booking/domain/customer/CustomerNote.java`
- Create: `src/main/java/io/moer/booking/domain/customer/dto/CustomerNoteRequest.java`
- Create: `src/main/java/io/moer/booking/domain/customer/dto/CustomerNoteResponse.java`
- Create: `src/main/java/io/moer/booking/domain/customer/repository/CustomerNoteRepository.java`
- Create: `src/main/resources/mapper/customer/CustomerNoteMapper.xml`
- Modify: `src/main/java/io/moer/booking/domain/customer/service/CustomerService.java`
- Modify: `src/main/java/io/moer/booking/domain/customer/controller/CustomerController.java`

**Step 1: DB 테이블 + 엔티티 + DTO + Repository + Mapper**

표준 도메인 패턴을 따름.

**Step 2: CustomerService에 메모 CRUD 메서드 추가**

**Step 3: CustomerController에 4개 엔드포인트 추가**

```
POST   /businesses/{businessId}/customers/{customerId}/notes
GET    /businesses/{businessId}/customers/{customerId}/notes
PUT    /businesses/{businessId}/customers/{customerId}/notes/{noteId}
DELETE /businesses/{businessId}/customers/{customerId}/notes/{noteId}
```

**Step 4: 빌드 확인 및 Commit**

```bash
./gradlew compileJava
git add -A
git commit -m "feat: 고객 메모 CRUD API (POST/GET/PUT/DELETE /customers/{id}/notes)"
```

---

## Task 14: P3 - 고객 CRM API - 태그 관리

**Files:**
- Create: `src/main/java/io/moer/booking/domain/customer/dto/CustomerTagRequest.java`
- Create: `src/main/java/io/moer/booking/domain/customer/dto/CustomerTagResponse.java`
- Modify: `src/main/java/io/moer/booking/domain/customer/repository/CustomerRepository.java`
- Modify: `src/main/resources/mapper/customer/CustomerMapper.xml`
- Modify: `src/main/java/io/moer/booking/domain/customer/service/CustomerService.java`
- Modify: `src/main/java/io/moer/booking/domain/customer/controller/CustomerController.java`

**Step 1: 태그 업데이트 쿼리**

기존 `customers.tags` 컬럼(VARCHAR)에 콤마 구분 문자열로 저장:

```xml
<update id="updateTags">
    UPDATE customers SET tags = #{tags}, updated_at = CURRENT_TIMESTAMP
    WHERE id = #{customerId} AND business_id = #{businessId}
</update>
```

**Step 2: 매장 전체 태그 목록 조회**

```xml
<select id="findAllTagsByBusinessId" resultType="string">
    SELECT DISTINCT unnest(string_to_array(tags, ',')) AS tag
    FROM customers WHERE business_id = #{businessId} AND tags IS NOT NULL AND tags != ''
    ORDER BY tag
</select>
```

**Step 3: 엔드포인트**

```
PUT /businesses/{businessId}/customers/{customerId}/tags
GET /businesses/{businessId}/customer-tags
```

**Step 4: 빌드 확인 및 Commit**

```bash
./gradlew compileJava
git add -A
git commit -m "feat: 고객 태그 관리 API (PUT /customers/{id}/tags, GET /customer-tags)"
```

---

## Task 15: P3 - 고객 CSV 내보내기

**Files:**
- Create: `src/main/java/io/moer/booking/domain/customer/dto/CustomerExportCondition.java`
- Modify: `src/main/java/io/moer/booking/domain/customer/service/CustomerService.java`
- Modify: `src/main/java/io/moer/booking/domain/customer/controller/CustomerController.java`

**Step 1: CustomerController에 CSV 엔드포인트 추가**

```java
@GetMapping("/customers/export")
public void exportCustomersCsv(
        @PathVariable Long businessId,
        @RequestParam(required = false) String segment,
        @RequestParam(required = false) List<String> tags,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate,
        @RequestParam(defaultValue = "UTF-8") String encoding,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        HttpServletResponse response) {
    // 플랜 체크 (BASIC 이상)
    // Content-Type, Content-Disposition 헤더 설정
    // UTF-8 BOM 추가 (엑셀 한글 깨짐 방지)
    // CSV 작성 (PrintWriter)
}
```

**Step 2: 빌드 확인 및 Commit**

```bash
./gradlew compileJava
git add -A
git commit -m "feat: 고객 목록 CSV 내보내기 API (GET /customers/export)"
```

---

## Task 16: P3 - 고객 중복 감지 및 병합

**Files:**
- Create: `src/main/java/io/moer/booking/domain/customer/dto/DuplicateCustomerResponse.java`
- Create: `src/main/java/io/moer/booking/domain/customer/dto/CustomerMergeRequest.java`
- Create: `src/main/java/io/moer/booking/domain/customer/dto/CustomerMergeResponse.java`
- Modify: `src/main/java/io/moer/booking/domain/customer/repository/CustomerRepository.java`
- Modify: `src/main/resources/mapper/customer/CustomerMapper.xml`
- Modify: `src/main/java/io/moer/booking/domain/customer/service/CustomerService.java`
- Modify: `src/main/java/io/moer/booking/domain/customer/controller/CustomerController.java`

**Step 1: 중복 감지 쿼리**

```xml
<select id="findDuplicatesByPhone" resultMap="customerResultMap">
    SELECT c.* FROM customers c
    INNER JOIN (
        SELECT phone, COUNT(*) as cnt FROM customers
        WHERE business_id = #{businessId} AND phone IS NOT NULL AND phone != ''
        GROUP BY phone HAVING COUNT(*) > 1
    ) dup ON c.phone = dup.phone
    WHERE c.business_id = #{businessId}
    ORDER BY c.phone, c.created_at
</select>
```

**Step 2: 병합 로직**

트랜잭션 내에서:
1. 병합 대상 예약의 customer_id를 primaryCustomerId로 업데이트
2. 메모, 태그 통합
3. visit_count, total_spent 합산
4. 병합된 고객 soft delete

**Step 3: 엔드포인트**

```
GET  /businesses/{businessId}/customers/duplicates
POST /businesses/{businessId}/customers/merge
```

**Step 4: 빌드 확인 및 Commit**

```bash
./gradlew compileJava
git add -A
git commit -m "feat: 고객 중복 감지 및 병합 API"
```

---

## Task 17: P3 - 랜딩 페이지 동적 통계 API

**Files:**
- Create: `src/main/java/io/moer/booking/domain/booking/dto/PlatformStatsResponse.java`
- Modify: `src/main/java/io/moer/booking/domain/booking/controller/PublicBusinessController.java`
- Modify: `src/main/java/io/moer/booking/domain/booking/service/PublicBusinessService.java`

**Step 1: PlatformStatsResponse DTO**

```java
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlatformStatsResponse {
    private long totalBusinesses;
    private long totalReservations;
    private long totalReviews;
    private double avgRating;
    private long activePlansCount;
    private LocalDateTime updatedAt;
}
```

**Step 2: PublicBusinessService에 getPlatformStats() 추가**

각 Repository의 count 쿼리 호출.

**Step 3: PublicBusinessController에 엔드포인트 추가**

```java
@GetMapping("/public/platform-stats")
```

SecurityConfig에서 Public 경로 허용 확인.

**Step 4: 빌드 확인 및 Commit**

```bash
./gradlew compileJava
git add -A
git commit -m "feat: 랜딩 페이지 동적 통계 API (GET /public/platform-stats)"
```

---

## Task 18: P4 - 인앱 도움말 API

**Files:**
- Modify: `src/main/resources/db/schema.sql` (help_articles 테이블)
- Create: `src/main/java/io/moer/booking/domain/help/HelpArticle.java`
- Create: `src/main/java/io/moer/booking/domain/help/dto/HelpArticleResponse.java`
- Create: `src/main/java/io/moer/booking/domain/help/dto/HelpArticleCreateRequest.java`
- Create: `src/main/java/io/moer/booking/domain/help/repository/HelpArticleRepository.java`
- Create: `src/main/resources/mapper/help/HelpArticleMapper.xml`
- Create: `src/main/java/io/moer/booking/domain/help/service/HelpArticleService.java`
- Create: `src/main/java/io/moer/booking/domain/help/controller/PublicHelpController.java`
- Create: `src/main/java/io/moer/booking/domain/help/controller/SuperAdminHelpController.java`

**Step 1: DB 테이블 + 엔티티 + DTO + Repository + Mapper**

표준 도메인 패턴.

**Step 2: Public 조회 API**

```java
@GetMapping("/public/help")
// category, keyword, lang 파라미터
```

**Step 3: SuperAdmin CRUD API**

```
POST   /api/superadmin/help
GET    /api/superadmin/help
PUT    /api/superadmin/help/{id}
DELETE /api/superadmin/help/{id}
```

**Step 4: 빌드 확인 및 Commit**

```bash
./gradlew compileJava
git add -A
git commit -m "feat: 인앱 도움말 API (Public 조회 + SuperAdmin CRUD)"
```

---

## Task 19: 전체 빌드 확인 및 최종 커밋

**Step 1: 전체 빌드**

Run: `./gradlew clean build -x test`
Expected: BUILD SUCCESSFUL

**Step 2: DB 마이그레이션 확인**

PostgreSQL에 신규 테이블(service_images, customer_notes, help_articles) 생성 확인.

**Step 3: 결과 문서 작성**

`docs/백엔드-작업완료-2026-03-04.md`에 구현 결과 정리:
- 완료된 API 목록
- 신규 테이블
- 신규 에러코드
- 프론트엔드에 전달할 사항

**Step 4: 프론트엔드 요청 문서 작성**

`docs/프론트엔드-요청사항-2026-03-04.md`에:
- 이미지 URL 형식 변경 안내
- 회원가입 Response 구조 변경
- SSE 이벤트 타입 및 데이터 구조
- CSV BOM 처리 안내

---

## 구현 순서 요약

| Task | 내용 | 우선순위 | 의존성 |
|------|------|----------|--------|
| 1 | 에러코드 추가 | 공통 | 없음 |
| 2 | FREE 기본 통계 | P0 | Task 1 |
| 3 | 회원가입 변경 | P0 | 없음 |
| 4-6 | 서비스 이미지 | P1 | Task 1 |
| 7 | 리뷰 이미지 개선 | P1 | 없음 |
| 8 | SSE 확장 | P1 | 없음 |
| 9 | 예약 reschedule | P2 | Task 1 |
| 10 | 직원 스케줄 | P2 | 없음 |
| 11 | 알림 발송 | P2 | Task 1 |
| 12 | 카카오 설정 | P3 | 없음 |
| 13-16 | CRM 4종 | P3 | Task 1 |
| 17 | 랜딩 통계 | P3 | 없음 |
| 18 | 도움말 | P4 | Task 1 |
| 19 | 최종 확인 | - | 전체 |
