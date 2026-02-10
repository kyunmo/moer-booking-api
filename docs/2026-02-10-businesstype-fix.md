# 2026-02-10 BusinessType 업종 분류 개선

## 문제점

1. **업종명 혼용 문제**
   - `PILATES("필라테스/요가")` - 두 업종을 하나로 묶음
   - `CAFE("스터디카페/공방")` - 두 업종을 하나로 묶음

2. **일관성 부족**
   - 문서와 실제 코드의 BusinessType이 다름
   - 병원 관련 업종이 문서에만 존재

## 수정 내역

### BusinessType Enum 수정

**파일**: `src/main/java/io/moer/booking/domain/business/BusinessType.java`

#### 수정 전
```java
public enum BusinessType {
    BEAUTY_SHOP("미용실"),
    PILATES("필라테스/요가"),           // 혼용
    CAFE("스터디카페/공방"),            // 혼용
    STUDY_CAFE("스터디카페"),          // 중복
    ACADEMY("학원"),
    PET_SALON("애견미용"),
    OTHER("기타");
}
```

#### 수정 후
```java
public enum BusinessType {
    BEAUTY_SHOP("미용실"),
    PILATES("필라테스"),               // 분리
    YOGA("요가"),                      // 신규 추가
    CAFE("카페"),                      // 명확화
    STUDY_CAFE("스터디카페"),
    WORKSHOP("공방"),                  // 신규 추가
    ACADEMY("학원"),
    PET_SALON("애견미용"),
    OTHER("기타");
}
```

### 변경 사항 요약

| 항목 | 이전 | 이후 | 설명 |
|------|------|------|------|
| PILATES | "필라테스/요가" | "필라테스" | 단일 업종으로 명확화 |
| YOGA | (없음) | "요가" | 신규 추가 |
| CAFE | "스터디카페/공방" | "카페" | 단일 업종으로 명확화 |
| WORKSHOP | (없음) | "공방" | 신규 추가 |
| STUDY_CAFE | "스터디카페" | "스터디카페" | 유지 |

### 총 업종 개수

- **이전**: 7개
- **이후**: 9개

## 프론트엔드 영향

### TypeScript 타입 정의

```typescript
export type BusinessType =
  | 'BEAUTY_SHOP'      // 미용실
  | 'PILATES'          // 필라테스
  | 'YOGA'             // 요가 (신규)
  | 'CAFE'             // 카페
  | 'STUDY_CAFE'       // 스터디카페
  | 'WORKSHOP'         // 공방 (신규)
  | 'ACADEMY'          // 학원
  | 'PET_SALON'        // 애견미용
  | 'OTHER';           // 기타

export const BUSINESS_TYPE_LABELS: Record<BusinessType, string> = {
  BEAUTY_SHOP: '미용실',
  PILATES: '필라테스',
  YOGA: '요가',
  CAFE: '카페',
  STUDY_CAFE: '스터디카페',
  WORKSHOP: '공방',
  ACADEMY: '학원',
  PET_SALON: '애견미용',
  OTHER: '기타'
};
```

### UI 업데이트 필요

**업종 선택 드롭다운**
```jsx
<Select value={businessType} onChange={...}>
  <MenuItem value="BEAUTY_SHOP">미용실</MenuItem>
  <MenuItem value="PILATES">필라테스</MenuItem>
  <MenuItem value="YOGA">요가</MenuItem>              {/* 신규 */}
  <MenuItem value="CAFE">카페</MenuItem>
  <MenuItem value="STUDY_CAFE">스터디카페</MenuItem>
  <MenuItem value="WORKSHOP">공방</MenuItem>          {/* 신규 */}
  <MenuItem value="ACADEMY">학원</MenuItem>
  <MenuItem value="PET_SALON">애견미용</MenuItem>
  <MenuItem value="OTHER">기타</MenuItem>
</Select>
```

## 데이터 마이그레이션 (필요 시)

기존 데이터베이스에 `PILATES`로 저장된 데이터가 있을 경우:
- "필라테스"인지 "요가"인지 확인 필요
- 수동 데이터 정리 필요할 수 있음

```sql
-- 기존 PILATES 데이터 확인
SELECT id, name, business_type FROM businesses
WHERE business_type = 'PILATES';

-- 필요시 YOGA로 변경 (수동 확인 후)
-- UPDATE businesses SET business_type = 'YOGA'
-- WHERE id = ? AND /* 조건 */;
```

## 테스트 확인 사항

### 1. 회원가입 테스트
```bash
POST /api/auth/register
{
  "email": "yoga@example.com",
  "password": "Test123!",
  "name": "요가원",
  "phone": "010-1234-5678",
  "businessName": "힐링 요가",
  "businessType": "YOGA"  // 신규 업종
}
```

### 2. 매장 생성 테스트
```bash
POST /api/businesses
{
  "ownerId": 1,
  "businessType": "WORKSHOP",  // 신규 업종
  "name": "손수공예 공방",
  ...
}
```

### 3. 업종 변경 테스트
```bash
PATCH /api/businesses/1
{
  "businessType": "YOGA"  // PILATES -> YOGA 변경
}
```

## 빌드 결과

```
BUILD SUCCESSFUL in 22s
```

모든 컴파일 에러 없이 정상 빌드 완료.

## 업종별 특성 (참고)

| 업종 | 주요 기능 | 예약 특성 |
|------|----------|----------|
| **미용실** | 컷, 펌, 염색 등 | 1:1 예약, 시간대별 |
| **필라테스** | 기구 필라테스 | 소그룹, 시간대별 |
| **요가** | 요가 수업 | 그룹 수업, 시간대별 |
| **카페** | 좌석 예약 | 좌석별, 시간대별 |
| **스터디카페** | 좌석/룸 예약 | 좌석/공간별, 시간대별 |
| **공방** | 원데이 클래스 | 클래스별, 날짜별 |
| **학원** | 수업 예약 | 과목별, 시간대별 |
| **애견미용** | 반려동물 미용 | 1:1 예약, 시간대별 |

## 향후 확장 가능성

### 업종별 맞춤 설정
각 업종에 맞는 기본 설정값을 제공할 수 있습니다:

```java
public BusinessSettings getDefaultSettings(BusinessType type) {
    return switch (type) {
        case PILATES, YOGA -> BusinessSettings.builder()
            .bookingInterval(60)  // 1시간 단위
            .maxAdvanceBookingDays(7)
            .build();
        case CAFE, STUDY_CAFE -> BusinessSettings.builder()
            .bookingInterval(30)  // 30분 단위
            .maxAdvanceBookingDays(1)
            .build();
        case WORKSHOP -> BusinessSettings.builder()
            .bookingInterval(180)  // 3시간 단위
            .maxAdvanceBookingDays(30)
            .build();
        default -> BusinessSettings.builder()
            .bookingInterval(30)
            .build();
    };
}
```

## 관련 문서

- [매장 설정 수정 내역](./2026-02-10-business-settings-fix.md)
- [프론트엔드 가이드](./2026-02-10-business-settings-frontend-guide.md)

---

**작업 완료일**: 2026-02-10
**담당**: Claude Code
**상태**: ✅ 완료
