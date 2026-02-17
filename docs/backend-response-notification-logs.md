# [백엔드 응답] 알림 이력 API - 데이터 미반환 이슈 수정

## 요청 문서
- `docs/backend-request-notification-logs.md`

## 수정 결과: 완료

### 원인 분석

프로젝트에 **두 개의 독립된 알림 시스템**이 존재:

| 시스템 | 테이블 | 대상 | 용도 |
|--------|--------|------|------|
| **notifications** (알림 벨) | `notifications` | 매장 OWNER | 앱 내부 알림 벨 표시 |
| **notification_logs** (알림 이력) | `notification_logs` | 고객 | 외부 발송(카카오/SMS) 이력 |

**문제**: `ReservationService`가 `NotificationService`(알림 벨)만 호출하고, `NotificationSender`(알림 이력 로그)는 **구현만 되어 있고 어디에서도 호출하지 않음**.

따라서 `notification_logs` 테이블에 데이터가 한 건도 쌓이지 않아 API가 빈 배열을 반환.

### 수정 내용

**수정 파일**: `src/main/java/io/moer/booking/domain/reservation/service/ReservationService.java`

#### 1. NotificationSender 의존성 주입 추가
```java
private final NotificationSender notificationSender;
```

#### 2. 외부 알림 로그 헬퍼 메서드 추가
```java
private void sendExternalNotificationLog(Business business, Reservation reservation,
                                          String recipientPhone, String recipientName,
                                          String serviceName, String type) {
    // created, confirmed, cancelled, review_request 타입별 분기
    // try-catch로 감싸 알림 로그 실패가 비즈니스 로직에 영향 주지 않도록 처리
}
```

#### 3. 예약 이벤트별 알림 로그 호출 추가

| 이벤트 | 메서드 | 알림 타입 | 설명 |
|--------|--------|-----------|------|
| 예약 생성 | `createReservation()` | `RESERVATION_CREATED` | 고객에게 예약 생성 알림 |
| 예약 확정 | `confirmReservation()` | `RESERVATION_CONFIRMED` | 고객에게 예약 확정 알림 |
| 예약 완료 | `completeReservation()` | `REVIEW_REQUEST` | 고객에게 리뷰 요청 알림 |
| 예약 취소 | `cancelReservation()` | `RESERVATION_CANCELLED` | 고객에게 예약 취소 알림 |

### 수정하지 않은 부분 (정상 동작)

- **NotificationLogController**: `GET /api/businesses/{businessId}/notification-logs` 엔드포인트 정상 구현
- **NotificationLogService**: 조회 로직 정상
- **NotificationLogMapper.xml**: CRUD 쿼리 정상
- **LogNotificationSender**: DB 로그 저장 구현체 정상
- **notification_logs 테이블 스키마**: 정상

### 데이터 흐름 (수정 후)

```
예약 생성/확정/취소/완료
  ├─ NotificationService.createNotification()     → notifications 테이블 (알림 벨, 기존)
  └─ NotificationSender.sendReservationXxx()       → notification_logs 테이블 (알림 이력, 신규 연결)
        └─ LogNotificationSender.saveLog()
              └─ NotificationLogRepository.save()
```

### 빌드 검증
- `./gradlew compileJava` : **BUILD SUCCESSFUL**

### API 응답 예시 (수정 후)

```json
// GET /api/businesses/{businessId}/notification-logs
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "businessId": 1,
        "reservationId": 10,
        "channel": "KAKAO",
        "templateType": "RESERVATION_CREATED",
        "recipientPhone": "010-1234-5678",
        "recipientName": "홍길동",
        "title": "예약이 생성되었습니다",
        "content": "홍길동님, 모어 네일에 예약이 생성되었습니다.\n일시: 2026-02-20 14:00\n서비스: 젤네일, 손톱케어",
        "status": "SENT",
        "sentAt": "2026-02-18 10:00:00",
        "createdAt": "2026-02-18 10:00:00"
      }
    ],
    "pageInfo": {
      "page": 1,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1
    }
  }
}
```

### 참고사항
- `PublicBookingService`와 `CustomerBookingService`는 내부적으로 `ReservationService`를 호출하므로, 온라인/로그인 예약에서도 알림 로그가 자동으로 기록됩니다.
- 알림 로그 실패 시 `try-catch`로 감싸 예약 비즈니스 로직에는 영향을 주지 않습니다.
