# 백엔드 확인 요청: SNS 로그인 플로우 검증

> **작성일:** 2026-02-19
> **상태:** 확인 요청
> **관련:** 관리자 SNS OAuth 로그인 신규 가입 플로우

---

## 1. 현재 구현된 프론트엔드 플로우

### 1-1. 기존 회원 (이메일 일치)
```
SNS 로그인 → 백엔드 자동 연동 → isNewUser=false → /shop-admin/dashboard
```

### 1-2. 신규 회원 (이메일 미존재) — 새로 구현
```
SNS 로그인 → 백엔드 자동 생성 (OWNER + 기본 매장 + 30일 체험판)
  → isNewUser=true → /shop-admin/setup (프로필 완성 페이지)
  → 매장명/업종/전화번호/약관동의 수집
  → PATCH /businesses/{id} (매장명, 업종)
  → PATCH /auth/profile (전화번호)
  → /shop-admin/dashboard
```

---

## 2. 확인 필요 사항

### 2-1. `isNewUser` 파라미터 확인
- **질문:** `loginType=admin`으로 신규 가입 시 리다이렉트 URL에 `isNewUser=true`가 포함되는지 확인
- **예상 URL:** `/oauth2-redirect?accessToken=...&refreshToken=...&loginType=admin&isNewUser=true`
- **중요:** `isNewUser` 파라미터가 없으면 프론트엔드에서 신규/기존 사용자 구분 불가

### 2-2. `PATCH /auth/profile` 전화번호 업데이트
- **질문:** `PATCH /auth/profile` API에서 `phone` 필드 업데이트가 가능한지 확인
- **요청 예시:**
```json
PATCH /auth/profile
{
  "phone": "010-1234-5678"
}
```
- **현재 알려진 필드:** name은 가능. phone은 미확인.

### 2-3. `PATCH /businesses/{id}` 매장명/업종 업데이트
- **질문:** 매장 생성 직후(초기 상태)에서 `name`, `businessType` 업데이트가 정상 동작하는지 확인
- **요청 예시:**
```json
PATCH /businesses/{businessId}
{
  "name": "준수헤어",
  "businessType": "BEAUTY_SHOP"
}
```

### 2-4. 고객(CUSTOMER) 계정이 관리자 SNS 로그인 시도할 경우
- **시나리오:** 고객으로 가입된 이메일(role=CUSTOMER)이 `loginType=admin`으로 SNS 로그인
- **현재 백엔드 동작:** 기존 역할(CUSTOMER) 유지, SNS 연동만 추가
- **질문:** 이 경우 프론트에서 `/auth/me`로 조회하면 `role: CUSTOMER`가 반환되는데, 관리자 페이지 접근이 차단됨. 에러 메시지를 제공해야 하는지? 아니면 자동으로 역할 업그레이드(CUSTOMER → OWNER)가 필요한지?

### 2-5. SNS 제공자에서 이메일 미제공 시
- **시나리오:** 카카오에서 이메일 동의를 거부하는 경우
- **현재 백엔드 동작:** `OA003` 에러 코드로 실패 처리
- **확인:** 에러 메시지가 한글로 제공되는지 ("SNS에서 이메일 정보를 제공하지 않았습니다")

---

## 3. 백엔드 변경이 필요하지 않은 사항

| 항목 | 설명 |
|------|------|
| 계정 자동 생성 | 기존 동작 유지 (OWNER + 기본 매장 + 30일 체험) |
| `isNewUser` 파라미터 | 이미 구현되어 있다면 변경 불필요 |
| SNS 연동 | 기존 동작 유지 |
| 토큰 발급 | 기존 동작 유지 |

---

## 4. (선택) 향후 개선 제안

### 4-1. 약관 동의 기록
- 현재: 프론트엔드에서만 약관 동의 체크
- 개선: 약관 동의 일시를 서버에 기록하는 API 추가
```json
POST /auth/agree-terms
{
  "termsAgreedAt": "2026-02-19T10:00:00",
  "privacyAgreedAt": "2026-02-19T10:00:00"
}
```

### 4-2. 프로필 완성 여부 플래그
- 현재: 프론트엔드에서 `isNewUser` URL 파라미터로만 판단 (일회성)
- 개선: 사용자 DB에 `profileCompleted` 플래그 추가
- 장점: 중도 이탈 후 재로그인 시에도 셋업 페이지로 유도 가능

### 4-3. SNS 연결 후 프로필 페이지 복귀
- 현재: SNS 연결 시 `/oauth2-redirect` → 대시보드로 이동 (프로필 페이지에서 벗어남)
- 개선: OAuth state 파라미터에 `returnUrl` 포함하여 원래 페이지로 복귀
