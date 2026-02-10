# 2026-02-10 매장 설정 및 업종 변경 기능 수정

## 문제 점검 결과

### 1. 업종 변경 불가
- **원인**: `BusinessUpdateRequest`에 `businessType` 필드 누락
- **영향**: 매장 정보 수정 시 업종 변경 불가능

### 2. 매장 설정 저장 시 일부 데이터 누락
- **원인**:
  - `BusinessMapper.xml`의 UPDATE 쿼리에 일부 필드 누락
  - DTO가 없어서 엔티티를 직접 사용
- **영향**: 목표 설정(dailyRevenueGoal 등) 저장 안 됨

### 3. 매장 설정 API 개선 필요
- **원인**: 엔티티를 직접 받아서 타입 안정성 부족
- **영향**: 프론트엔드에서 일부 필드만 업데이트하기 어려움

### 4. BusinessType 업종 분류 문제 ⭐ 추가 수정
- **원인**: 업종명 혼용 (`PILATES("필라테스/요가")`, `CAFE("스터디카페/공방")`)
- **영향**: 업종 구분이 명확하지 않음, 저장 실패 가능성
- **해결**: 업종 분리 (PILATES, YOGA, CAFE, WORKSHOP 등 9개로 확장)

## 수정 내역

### 1. BusinessUpdateRequest 확장

**파일**: `src/main/java/io/moer/booking/domain/business/dto/BusinessUpdateRequest.java`

**추가된 필드**:
```java
// 업종 변경 지원
private BusinessType businessType;

// 목표 설정
private Integer dailyRevenueGoal;
private Integer monthlyRevenueGoal;
private Integer monthlyNewCustomerGoal;
```

### 2. BusinessMapper.xml UPDATE 쿼리 수정

**파일**: `src/main/resources/mapper/business/BusinessMapper.xml`

**수정 사항**:
```xml
<update id="update">
    UPDATE businesses
    SET name = #{name},
        business_type = #{businessType},        -- 추가
        phone = #{phone},
        address = #{address},
        description = #{description},
        business_hours = #{businessHours, ...}::jsonb,
        status = #{status},
        daily_revenue_goal = #{dailyRevenueGoal},              -- 추가
        monthly_revenue_goal = #{monthlyRevenueGoal},          -- 추가
        monthly_new_customer_goal = #{monthlyNewCustomerGoal}, -- 추가
        updated_at = CURRENT_TIMESTAMP
    WHERE id = #{id}
</update>
```

### 3. BusinessService 업데이트 로직 개선

**파일**: `src/main/java/io/moer/booking/domain/business/service/BusinessService.java`

**개선 내용**:
- `businessType` 필드 업데이트 지원
- 목표 설정 필드 업데이트 지원
- 업종 변경 시 감사로그에 이전/이후 값 기록

```java
// 업종 변경 지원
.businessType(request.getBusinessType() != null ?
    request.getBusinessType() : business.getBusinessType())

// 목표 설정 필드
.dailyRevenueGoal(request.getDailyRevenueGoal() != null ?
    request.getDailyRevenueGoal() : business.getDailyRevenueGoal())
// ...
```

### 4. BusinessSettingsUpdateRequest DTO 신규 생성

**파일**: `src/main/java/io/moer/booking/domain/business/dto/BusinessSettingsUpdateRequest.java`

**목적**:
- 타입 안정성 향상
- 프론트엔드에서 일부 필드만 전송 가능
- 엔티티와 API 레이어 분리

**포함된 설정**:
- 예약 설정 (5개 필드)
- 알림 설정 (4개 필드)
- 카카오톡 설정 (3개 필드)
- 결제 설정 (3개 필드)
- 취소 정책 (3개 필드)
- 기타 (2개 필드)

### 5. BusinessController 수정

**파일**: `src/main/java/io/moer/booking/domain/business/controller/BusinessController.java`

**변경 사항**:
```java
// 이전: BusinessSettings 엔티티를 직접 받음
@RequestBody BusinessSettings settings

// 이후: DTO 사용
@RequestBody BusinessSettingsUpdateRequest request
```

### 6. BusinessService 설정 업데이트 메서드 개선

**파일**: `src/main/java/io/moer/booking/domain/business/service/BusinessService.java`

**개선 내용**:
- DTO 기반으로 변경
- 모든 필드에 대해 null 체크 적용
- null인 필드는 기존 값 유지

## 수정 전/후 비교

### 매장 정보 수정 API

**엔드포인트**: `PATCH /api/businesses/{id}`

#### 이전
```json
{
  "name": "변경된 매장명",
  "phone": "010-1234-5678"
}
```
- ❌ 업종 변경 불가
- ❌ 목표 설정 변경 불가

#### 이후
```json
{
  "name": "변경된 매장명",
  "businessType": "PILATES",
  "phone": "010-1234-5678",
  "dailyRevenueGoal": 500000,
  "monthlyRevenueGoal": 15000000
}
```
- ✅ 업종 변경 가능
- ✅ 목표 설정 변경 가능
- ✅ 감사로그에 업종 변경 기록

### 매장 설정 수정 API

**엔드포인트**: `PATCH /api/businesses/{id}/settings`

#### 이전
```java
// Controller에서 엔티티 직접 받음
@RequestBody BusinessSettings settings
```
- ❌ 타입 안정성 부족
- ❌ 모든 필드를 보내야 함

#### 이후
```json
{
  "bookingInterval": 60,
  "autoConfirm": "Y",
  "sendReminderSms": "N"
}
```
- ✅ DTO 사용으로 타입 안정성 확보
- ✅ 변경할 필드만 전송 가능
- ✅ null 필드는 기존 값 유지

## 테스트 방법

### 1. 업종 변경 테스트
```bash
PATCH /api/businesses/1
Authorization: Bearer {token}
Content-Type: application/json

{
  "businessType": "PILATES"
}
```

**예상 결과**:
- 업종이 변경됨
- 감사로그에 이전/이후 업종 기록됨

### 2. 매장 설정 일부 변경 테스트
```bash
PATCH /api/businesses/1/settings
Authorization: Bearer {token}
Content-Type: application/json

{
  "bookingInterval": 60,
  "autoConfirm": "Y"
}
```

**예상 결과**:
- 지정한 2개 필드만 변경됨
- 나머지 필드는 기존 값 유지

### 3. 목표 설정 테스트
```bash
PATCH /api/businesses/1
Authorization: Bearer {token}
Content-Type: application/json

{
  "dailyRevenueGoal": 500000,
  "monthlyRevenueGoal": 15000000,
  "monthlyNewCustomerGoal": 50
}
```

**예상 결과**:
- 목표 설정이 정상 저장됨
- 조회 시 저장된 값이 반환됨

## 빌드 결과

```
BUILD SUCCESSFUL in 21s
```

모든 컴파일 에러 없이 정상 빌드 완료.

## 영향받는 파일

### 수정된 파일 (6개)
1. `dto/BusinessUpdateRequest.java` - 업종 및 목표 필드 추가
2. `service/BusinessService.java` - 업데이트 로직 개선
3. `controller/BusinessController.java` - DTO 변경
4. `mapper/business/BusinessMapper.xml` - UPDATE 쿼리 수정

### 신규 파일 (1개)
1. `dto/BusinessSettingsUpdateRequest.java` - 설정 업데이트 전용 DTO

## 주의사항

### 프론트엔드 영향

#### 매장 정보 수정 API
- **기존 동작 유지**: 기존 필드(name, phone 등)는 그대로 사용 가능
- **신규 필드 추가**: businessType, dailyRevenueGoal 등 선택적 사용

#### 매장 설정 수정 API
- **요청 형식 동일**: JSON 구조 변경 없음
- **부분 업데이트 지원**: null인 필드는 기존 값 유지

### 데이터베이스
- 스키마 변경 없음
- 기존 데이터에 영향 없음

## 추가 수정 사항 (2026-02-10)

### BusinessType 업종 분류 개선

**변경 사항**:
- `PILATES("필라테스/요가")` → `PILATES("필라테스")` + `YOGA("요가")` 추가
- `CAFE("스터디카페/공방")` → `CAFE("카페")` + `WORKSHOP("공방")` 추가

**전체 업종 목록** (9개):
1. BEAUTY_SHOP - 미용실
2. PILATES - 필라테스
3. YOGA - 요가 (신규)
4. CAFE - 카페
5. STUDY_CAFE - 스터디카페
6. WORKSHOP - 공방 (신규)
7. ACADEMY - 학원
8. PET_SALON - 애견미용
9. OTHER - 기타

**상세 문서**: [BusinessType 수정 내역](./2026-02-10-businesstype-fix.md)

## 관련 문서

- [Business 도메인 문서](../docs/02_domain/business.md)
- [감사로그 기능](./2026-02-10-audit-log-implementation.md)
- [BusinessType 업종 분류 개선](./2026-02-10-businesstype-fix.md)

---

**작업 완료일**: 2026-02-10
**담당**: Claude Code
**상태**: ✅ 완료 (BusinessType 추가 수정 포함)
