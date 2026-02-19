# Trial(30일 체험) → 유료 전환 시스템 — 백엔드 요청사항

## 1. GET /api/subscription 응답 필드 확인

프론트엔드에서 아래 필드들을 사용하고 있습니다. 누락된 필드가 있다면 추가 부탁드립니다.

```json
{
  "plan": "FREE | PAID",
  "status": "ACTIVE | TRIAL | EXPIRED | CANCELED | SUSPENDED",
  "isTrialActive": true,
  "daysUntilTrialEnd": 15,
  "maxMonthlyReservations": 30,
  "currentMonthReservationCount": 22,
  "maxStaff": 1,
  "currentStaffCount": 1,
  "canUseService": true,
  "canAddStaff": true,
  "canCreateReservation": true,
  "monthlyPrice": 0,
  "planDescription": "무료",
  "nextBillingDate": null
}
```

### 특히 확인 필요한 항목
- `isTrialActive`: 30일 체험 기간 진행 중이면 `true`, 종료되었으면 `false`
- `daysUntilTrialEnd`: 체험 종료까지 남은 일수 (체험 비활성이면 `null` 또는 `0`)

---

## 2. Trial 관련 에러 코드 (403 응답)

프론트엔드에서 아래 에러 코드를 처리합니다. 해당 시나리오에서 에러가 올바르게 발생하는지 확인 부탁드립니다.

| 에러 코드 | 의미 | 발생 시나리오 | 프론트 처리 |
|-----------|------|--------------|-------------|
| `TR001` | 체험판 만료 | FREE 플랜 + 체험 종료 후 유료 기능 API 호출 시 | 체험 만료 모달 표시 |
| `TR002` | 체험판 기능 제한 | 체험 중이지만 특정 기능 제한에 걸렸을 때 | 기능 잠금 모달 표시 |
| `TR003` | 업그레이드 필요 | FREE 플랜에서 유료 전용 기능 접근 시 | 기능 잠금 모달 표시 |

### 에러 응답 형식 (기존 패턴과 동일)
```json
{
  "error": {
    "code": "TR001",
    "message": "체험 기간이 종료되었습니다. 유료 플랜으로 업그레이드해 주세요."
  }
}
```

---

## 3. 체험 만료 후 자동 FREE 전환 스케줄러

- 가입 시 30일 체험이 자동 활성화되는 것으로 알고 있습니다.
- 체험 30일 종료 후 자동으로 `isTrialActive = false`로 전환되는 스케줄러가 존재하는지 확인 부탁드립니다.
- 전환 시 `plan`은 `FREE` 유지, `status`는 어떻게 변경되는지?

---

## 4. 월간 예약 카운트 리셋

- `currentMonthReservationCount`가 매월 1일에 0으로 리셋되는지 확인 부탁드립니다.
- 리셋 기준 시간대 (KST 00:00 기준?)

---

## 5. 유료 기능 목록 (TR001/TR003 발생 대상)

FREE 플랜에서 접근 시 `TR001` 또는 `TR003`이 발생해야 하는 API 목록:

- 카카오톡 알림 관련 API
- 고객 태그 관리 API
- 매출 통계 API
- 재방문 알림 API
- 데이터 추출(CSV/Excel) API
