# [백엔드 응답] 매장 정보 API - slug 필드 누락 이슈 수정

## 요청 문서
- `docs/backend-request-business-slug.md`

## 수정 결과: 완료

### 원인
`BusinessResponse` DTO에 `slug` 필드가 선언되지 않았고, `from()` 메서드에서도 매핑하지 않아 API 응답에서 누락됨.

| 계층 | slug 존재 여부 | 비고 |
|------|:---:|------|
| DB 테이블 (businesses) | O | 정상 |
| MyBatis ResultMap | O | `BusinessMapper.xml` line 15 |
| Business Entity | O | `Business.java` line 29 |
| **BusinessResponse DTO** | **X (누락)** | **문제 지점** |
| BusinessController | - | DTO 의존 |

### 수정 파일

**`src/main/java/io/moer/booking/domain/business/dto/BusinessResponse.java`**

#### 1. 필드 추가 (line 28)
```java
private String slug;
```

#### 2. `from(Business)` 메서드에 매핑 추가 (line 54)
```java
.slug(business.getSlug())
```

#### 3. `from(Business, BusinessSettings)` 메서드에 매핑 추가 (line 75)
```java
.slug(business.getSlug())
```

### 수정 후 API 응답 예시

```json
// GET /api/businesses/{businessId}
{
  "id": 1,
  "ownerId": 1,
  "ownerName": "홍길동",
  "businessType": "NAIL",
  "name": "모어 네일",
  "slug": "moer-nail",
  "phone": "02-1234-5678",
  "address": "서울시 강남구...",
  "description": "...",
  "businessHours": { ... },
  "status": "ACTIVE",
  "createdAt": "2025-01-01 00:00:00",
  "updatedAt": "2025-01-01 00:00:00",
  "settings": { ... }
}
```

### 빌드 검증
- `./gradlew compileJava` : **BUILD SUCCESSFUL**

### 프론트엔드 영향
- `business.slug` 값이 정상 반환되므로 `currentSlug` 에 값이 설정됨
- `v-if="currentSlug"` 조건 통과 -> "예약 페이지 주소" UI 정상 표시
- 추가 프론트엔드 수정 불필요
