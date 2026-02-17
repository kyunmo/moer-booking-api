# [백엔드 확인요청] 알림 이력 API - 데이터 미반환 이슈

## 현상
- 관리자 페이지 > 알림 이력 목록에 데이터가 표시되지 않음
- 반면, 상단 네비게이션 바의 알림 벨 아이콘에는 알림이 정상적으로 표시됨

## 원인 분석 (두 시스템 비교)

| 구분 | 알림 이력 (미동작) | 알림 벨 (정상) |
|------|-------------------|---------------|
| API 엔드포인트 | `GET /businesses/{businessId}/notification-logs` | `GET /notifications` |
| businessId 사용 | O (URL 경로에 포함) | X (인증 토큰으로 자동 식별) |
| 폴링 | X (1회 호출) | O (30초 간격) |
| 데이터 구조 | `{ content: [], pageInfo: {} }` | `{ items: [], unreadCount, totalCount }` |

## 프론트엔드 코드 참조
```javascript
// 알림 이력 - src/api/notification-logs.js
getNotificationLogs(businessId, params = {}) {
  return apiClient.get(`/businesses/${businessId}/notification-logs`, { params })
}

// 알림 벨 - src/api/notifications.js
getNotifications(params = {}) {
  return apiClient.get('/notifications', { params })
}
```

## 확인/요청사항
1. **`GET /businesses/{businessId}/notification-logs` 엔드포인트가 구현되어 있는지 확인**
    - 404/500 에러 발생 여부
    - 구현되어 있다면 빈 배열을 반환하는 이유 확인
2. **기대하는 응답 형식**:
   ```json
   {
     "content": [
       {
         "id": 1,
         "type": "RESERVATION_CREATED",
         "channel": "KAKAO",
         "recipient": "010-1234-5678",
         "title": "예약 확인",
         "content": "...",
         "status": "SENT",
         "sentAt": "2026-02-18T10:00:00",
         "createdAt": "2026-02-18T10:00:00"
       }
     ],
     "pageInfo": {
       "totalElements": 50,
       "totalPages": 5,
       "page": 1,
       "size": 10
     }
   }
   ```
3. **알림 발송 시 notification-logs에도 기록이 저장되는지 확인**
    - 알림 벨(`/notifications`)에는 데이터가 있으므로, 알림 자체는 발생하고 있음
    - notification-logs 테이블에 INSERT가 되고 있는지 확인 필요

## 관련 API
- `GET /businesses/{businessId}/notification-logs` - 데이터 미반환
- `GET /notifications` - 정상 동작

## 우선순위
높음 - 알림 발송 이력 확인 불가
