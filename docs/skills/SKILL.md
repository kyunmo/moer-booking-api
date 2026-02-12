# moer Backend Development SKILL

## 🎯 개요
이 SKILL은 moer 예약 시스템 백엔드 개발을 위한 **4개의 전문 Agent**를 정의합니다.
각 Agent는 독립적으로 호출 가능하며, 복잡한 작업 시 자동으로 협업합니다.

**Agent 목록**:
1. 🔍 **Project Analyzer** - 현재 프로젝트 분석 및 문서화
2. 🎨 **Senior Planner** - 도메인 설계 및 기획 검토
3. 💻 **Backend Developer** - 코드 자동 생성
4. ✅ **QA Engineer** - 테스트 및 품질 검증

---

## 🤖 Agent 사용 가이드

### 기본 사용법
```
@Agent명 [요청사항]
```

### Agent 선택 기준
| 요청 유형 | 사용할 Agent |
|----------|-------------|
| "현재 진행 상황 정리해줘" | @Project Analyzer |
| "Customer 도메인 설계 검토해줘" | @Senior Planner |
| "Customer 도메인 코드 생성해줘" | @Backend Developer |
| "생성된 코드 검증해줘" | @QA Engineer |
| "Customer 도메인 전체 만들어줘" | @Backend Developer (자동으로 다른 Agent 호출) |

### Agent 협업 플로우
```
사용자 요청: "Customer 도메인 전체 만들어줘"
     ↓
@Backend Developer 시작
     ↓
1. @Project Analyzer 호출 (현재 상태 파악)
     ↓
2. @Senior Planner 호출 (설계 검토)
     ↓
3. 자체 코드 생성 (Entity → Controller)
     ↓
4. @QA Engineer 호출 (검증)
     ↓
완료 보고
```

---

## 📋 Agent 1: Project Analyzer

### Agent 정보
```yaml
Name: Backend Project Analyzer
Role: 프로젝트 상태 분석 및 문서화 전문가
Trigger: "현재 상태", "진행 상황", "분석", "문서화"
```

### 역할
**현재 프로젝트 상태를 분석하고 문서화**하는 전문 분석가입니다.

### 주요 기능

#### 1. 진행 상황 파악
- 완료된 도메인 목록
- 각 도메인별 구현 완성도 (Entity, DTO, Service, Controller)
- 미완성 부분 식별

#### 2. 코드베이스 분석
- 패키지 구조 분석
- 파일 개수 및 라인 수 통계
- 코딩 스타일 일관성 체크

#### 3. 문서 생성
- API 엔드포인트 목록 자동 생성
- ERD 관계도 텍스트 생성
- 개발 진행률 리포트

#### 4. 다음 단계 제안
- 우선순위 추천
- 의존성 분석 (A 도메인 완성 후 B 가능)
- 잠재적 이슈 경고

### 호출 예시

```bash
# 전체 상태 분석
@Project Analyzer
현재 백엔드 프로젝트 진행 상황을 분석해줘.
완료된 도메인, 미완성 부분, 다음 단계를 정리해줘.

# 특정 도메인 분석
@Project Analyzer
Reservation 도메인의 구현 완성도를 체크해줘.
Entity, Service, Controller 각각 확인하고
누락된 메서드나 개선점을 알려줘.

# API 문서 생성
@Project Analyzer
현재 구현된 모든 API 엔드포인트를 정리해서
Markdown 문서로 만들어줘. 도메인별로 그룹핑해줘.

# 코드 통계
@Project Analyzer
프로젝트 전체 통계를 내줘.
- 도메인별 파일 개수
- 총 라인 수
- 평균 메서드 길이
```

### 출력 예시

```markdown
# moer Backend 프로젝트 분석 리포트
**생성일**: 2026-02-11
**분석 대상**: moer-booking-backend

## 📊 전체 현황
- 총 도메인 수: 8개
- 완료: 6개 (User, Business, Staff, Portfolio, Service, SpecialHoliday)
- 진행 중: 1개 (Reservation - Service 구현 중)
- 미착수: 1개 (CustomerHistory)
- 전체 진행률: 75%

## 📁 도메인별 상세 현황

### ✅ User 도메인 (100% 완료)
**파일 목록**:
- Entity: User.java, UserRole.java, UserStatus.java
- DTO: UserCreateRequest.java, UserUpdateRequest.java, UserResponse.java, UserSearchCondition.java
- Repository: UserRepository.java
- Service: UserService.java (12개 메서드)
- Controller: UserController.java (6개 엔드포인트)
- XML: UserMapper.xml

**API 엔드포인트**:
- POST   /api/users (회원가입)
- GET    /api/users/{id} (조회)
- GET    /api/users (목록, 페이징)
- PUT    /api/users/{id} (수정)
- DELETE /api/users/{id} (삭제)
- PATCH  /api/users/{id}/status (상태 변경)

**코드 품질**:
- ✅ Lombok Builder 패턴 사용
- ✅ @Transactional 적절히 적용
- ✅ 로깅 일관성 유지
- ✅ 예외 처리 완료

---

### 🚧 Reservation 도메인 (70% 진행 중)
**파일 목록**:
- Entity: ✅ Reservation.java, ReservationStatus.java
- DTO: ✅ 4개 완료
- Repository: ✅ ReservationRepository.java
- Service: ⚠️ ReservationService.java (미완성)
- Controller: ❌ 미작성
- XML: ✅ ReservationMapper.xml

**누락 사항**:
1. Service 미구현 메서드:
   - validateTimeConflict() - 시간 겹침 검증
   - calculateTotalPrice() - 가격 자동 계산
   
2. Controller 미작성:
   - 전체 CRUD 엔드포인트 필요

**잠재적 이슈**:
- Service와 Staff의 순환 참조 가능성
- JSONB 필드(services) TypeHandler 일관성 체크 필요

---

### ❌ CustomerHistory 도메인 (미착수)
**의존성**: Reservation 완료 필요
**예상 소요**: 2시간

## 📈 다음 단계 제안

### 🔥 우선순위 1: Reservation Service 완성
**이유**: 70% 완료되어 즉시 마무리 가능
**작업 내역**:
1. validateTimeConflict() 구현 (30분)
2. calculateTotalPrice() 구현 (20분)
3. 단위 테스트 작성 (20분)

**예상 소요**: 1.5시간

### 🔥 우선순위 2: Reservation Controller 작성
**이유**: Service 완성 직후 연결
**작업 내역**:
1. CRUD 엔드포인트 6개 작성
2. Swagger 문서화
3. API 테스트

**예상 소요**: 1시간

### ⏸️ 우선순위 3: CustomerHistory 도메인 시작
**이유**: Reservation 100% 완료 후 착수 가능
**예상 소요**: 2시간

## ⚠️ 발견된 이슈

### 1. 트랜잭션 전파 설정 불명확
**위치**: ReservationService
**문제**: createReservation()에서 CustomerService 호출 시 트랜잭션 전파 미정의
**제안**: @Transactional(propagation = Propagation.REQUIRED) 명시

### 2. JSONB TypeHandler 중복 코드
**위치**: Business, Reservation
**문제**: JsonbTypeHandler를 여러 곳에서 재정의
**제안**: 공통 모듈로 추출

## 📊 코드 통계
- 총 Java 파일: 48개
- 총 라인 수: 3,247줄
- 평균 파일 라인: 67줄
- Service 평균 메서드: 8개/클래스
- DTO 타입: Request(8), Response(8), Condition(6)

## 🎯 이번 주 목표
1. ✅ Reservation 도메인 완성 (100%)
2. 🎯 CustomerHistory 도메인 시작 (50%)
3. 🎯 전체 API 통합 테스트
```

---

## 🎨 Agent 2: Senior Planner

### Agent 정보
```yaml
Name: Backend Senior Planner
Role: 도메인 설계 및 아키텍처 검토 전문가
Trigger: "설계", "검토", "개선", "아키텍처"
```

### 역할
**도메인 설계 및 아키텍처를 검토**하는 시니어 기획자입니다.

### 주요 책임

#### 1. 도메인 모델링 검증
- 엔티티 관계 (1:N, N:M)
- FK 설정 및 CASCADE 정책
- 테이블명/컬럼명 규칙

#### 2. 비즈니스 규칙 명세
- 중복 검증 규칙
- 상태 전이 다이어그램
- 권한 체크 로직

#### 3. API 설계 검토
- RESTful 원칙 준수
- 엔드포인트 구조
- HTTP 메서드 선택

#### 4. 데이터베이스 최적화
- 인덱스 전략
- 쿼리 성능 예측
- JSONB 사용 적절성

### 설계 검토 체크리스트

```markdown
**도메인 모델링**
- [ ] 도메인 경계가 명확한가? (SRP 준수)
- [ ] FK 관계가 올바른가? (business_id, staff_id 등)
- [ ] Enum 타입이 확장 가능한 구조인가?
- [ ] JSONB 필드 사용이 적절한가?

**비즈니스 규칙**
- [ ] 중복 검증 규칙이 명확한가?
- [ ] 상태 전이가 정의되었는가? (PENDING → CONFIRMED)
- [ ] 권한 체크 로직이 있는가?
- [ ] 데이터 무결성 보장 방법은?

**데이터베이스**
- [ ] 인덱스 전략이 검색 패턴과 일치하는가?
- [ ] 복합 인덱스가 필요한가? (business_id + date)
- [ ] CASCADE 정책이 적절한가?
- [ ] NULL 허용 필드가 명확한가?

**API 설계**
- [ ] RESTful 원칙을 준수하는가?
- [ ] 엔드포인트 경로가 일관적인가?
- [ ] HTTP 메서드가 적절한가?
- [ ] 페이지네이션이 필요한가?
```

### 호출 예시

```bash
# 새 도메인 설계 검토
@Senior Planner
Customer 도메인을 설계하려고 해.

필드:
- name, phone, email, birthDate, gender
- tags (JSONB 배열)
- visitCount, totalSpent (자동 계산)

비즈니스 규칙:
- 전화번호 중복 방지 (같은 매장 내)
- 이메일 선택적 (NULL 허용)

설계 검토하고 개선점 제안해줘.

# 기존 도메인 리팩토링 검토
@Senior Planner
Reservation 도메인의 상태 관리를 개선하고 싶어.

현재:
- PENDING, CONFIRMED, COMPLETED, CANCELLED

문제점:
- 노쇼(No-show) 상태 없음
- 부분 취소 불가
- 취소 사유 저장 안됨

개선 방안 제안해줘.

# 도메인 간 관계 검토
@Senior Planner
Reservation과 CustomerHistory의 관계를 설계하려고 해.

요구사항:
- 예약 완료 시 자동으로 History 생성
- History는 삭제 불가 (로그 용도)
- History에는 시술 상세 정보 저장

이 설계가 적절한지 검토하고
트랜잭션 처리 방안도 제안해줘.
```

### 출력 예시

```markdown
# Customer 도메인 설계 검토 결과

## ✅ 잘 설계된 부분

1. **전화번호 중복 방지**
   - UNIQUE(business_id, phone) 제약조건 적절
   - 같은 매장 내에서만 중복 방지로 정확함

2. **JSONB 활용**
   - tags 필드는 동적 태그 관리에 적합
   - 검색 성능 위해 GIN 인덱스 필요

3. **NULL 처리**
   - birthDate, gender, email 선택적 → 적절
   - 고객 등록 시 부담 감소

## ⚠️ 개선 필요 사항

### 1. 이메일 중복 체크 추가 고려
**현재**: email 필드 UNIQUE 제약 없음
**문제**: 같은 이메일로 여러 고객 등록 가능
**제안 옵션**:

**옵션 A**: 전역 UNIQUE (추천)
```sql
ALTER TABLE customers ADD CONSTRAINT uk_customers_email 
UNIQUE (email);
```
장점: 이메일로 고객 통합 관리 가능
단점: 여러 매장 이용 시 이메일 재사용 불가

**옵션 B**: 매장별 UNIQUE
```sql
ALTER TABLE customers ADD CONSTRAINT uk_customers_business_email 
UNIQUE (business_id, email);
```
장점: 매장별 독립성 유지
단점: 같은 사람이 여러 이메일 필요

**권장**: 옵션 A (전역 UNIQUE)

### 2. 전화번호 형식 표준화
**현재**: VARCHAR 타입만 정의
**문제**: "010-1234-5678", "01012345678" 혼재 가능
**제안**:

#### DTO 검증
```java
@Pattern(regexp = "^010-\\d{4}-\\d{4}$", 
         message = "전화번호는 010-1234-5678 형식입니다")
private String phone;
```

#### Service 정규화
```java
// 저장 전 자동 포맷 변환
private String normalizePhone(String phone) {
    return phone.replaceAll("[^0-9]", "")  // 숫자만 추출
                .replaceAll("(\\d{3})(\\d{4})(\\d{4})", "$1-$2-$3");
}
```

### 3. tags JSONB 스키마 명확화
**현재**: JSONB 구조 미정의
**문제**: String[] vs Object[] 불명확
**제안**:

```sql
-- tags 구조 표준화
tags JSONB DEFAULT '[]'::jsonb

-- String 배열 (간단)
-- 예: ["VIP", "단골", "주의"]

-- 또는 객체 배열 (상세)
-- 예: [{"id": 1, "name": "VIP", "color": "#FF0000"}]
```

**권장**: String 배열 (간단하고 검색 쉬움)
```java
@TypeHandler(JsonbTypeHandler.class)
private List<String> tags;
```

### 4. visitCount, totalSpent 업데이트 전략
**현재**: 자동 계산 명시만 됨
**문제**: 업데이트 시점/방법 불명확
**제안**:

#### 옵션 A: Application 레벨 (추천)
```java
// ReservationService
@Transactional
public void completeReservation(Long id) {
    Reservation reservation = getReservation(id);
    reservation.setStatus(COMPLETED);
    reservationRepository.update(reservation);
    
    // 고객 정보 업데이트
    customerService.incrementVisitCount(reservation.getCustomerId());
    customerService.addTotalSpent(
        reservation.getCustomerId(), 
        reservation.getTotalPrice()
    );
}
```

#### 옵션 B: Database Trigger
```sql
CREATE OR REPLACE FUNCTION update_customer_stats()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE customers
    SET visit_count = visit_count + 1,
        total_spent = total_spent + NEW.total_price,
        last_visit = NEW.reservation_date
    WHERE id = NEW.customer_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reservation_completed
AFTER UPDATE OF status ON reservations
FOR EACH ROW
WHEN (NEW.status = 'COMPLETED' AND OLD.status != 'COMPLETED')
EXECUTE FUNCTION update_customer_stats();
```

**권장**: 옵션 A (Application)
- 이유: 비즈니스 로직 명확, 테스트 쉬움, 로깅 가능

### 5. 인덱스 전략
**필수 인덱스**:
```sql
-- 1. 기본 검색용
CREATE INDEX idx_customers_business_id ON customers(business_id);

-- 2. 전화번호 검색 (UNIQUE 포함)
CREATE UNIQUE INDEX idx_customers_phone 
ON customers(business_id, phone);

-- 3. 이메일 검색
CREATE UNIQUE INDEX idx_customers_email ON customers(email)
WHERE email IS NOT NULL; -- Partial index

-- 4. JSONB 태그 검색용
CREATE INDEX idx_customers_tags ON customers 
USING gin(tags jsonb_path_ops);

-- 5. 최근 방문일 정렬용
CREATE INDEX idx_customers_last_visit 
ON customers(business_id, last_visit DESC NULLS LAST);
```

## 📋 최종 설계 명세

### Database Schema
```sql
CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(13) NOT NULL, -- 010-1234-5678
    email VARCHAR(255),
    birth_date DATE,
    gender VARCHAR(10), -- MALE, FEMALE, OTHER
    tags JSONB DEFAULT '[]'::jsonb,
    visit_count INTEGER NOT NULL DEFAULT 0,
    total_spent INTEGER NOT NULL DEFAULT 0,
    last_visit DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uk_customers_phone UNIQUE (business_id, phone),
    CONSTRAINT uk_customers_email UNIQUE (email),
    CONSTRAINT chk_customers_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'))
);
```

### Entity
```java
package io.moer.booking.domain.customer;

import io.moer.booking.common.config.JsonbTypeHandler;
import lombok.*;
import org.apache.ibatis.type.TypeHandler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Customer {
    
    private Long id;
    private Long businessId;
    private String name;
    private String phone; // 010-1234-5678 형식
    private String email;
    private LocalDate birthDate;
    private String gender; // MALE, FEMALE, OTHER
    
    @TypeHandler(JsonbTypeHandler.class)
    private List<String> tags; // ["VIP", "단골"]
    
    private Integer visitCount;
    private Integer totalSpent;
    private LocalDate lastVisit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 고객 등급 계산 (비즈니스 로직)
     */
    public String getCustomerGrade() {
        if (visitCount >= 10) return "VIP";
        if (visitCount >= 5) return "단골";
        return "일반";
    }
    
    /**
     * 다음 생일까지 남은 일수
     */
    public long getDaysUntilBirthday() {
        if (birthDate == null) return -1;
        
        LocalDate today = LocalDate.now();
        LocalDate nextBirthday = birthDate.withYear(today.getYear());
        
        if (nextBirthday.isBefore(today)) {
            nextBirthday = nextBirthday.plusYears(1);
        }
        
        return ChronoUnit.DAYS.between(today, nextBirthday);
    }
}
```

### 비즈니스 규칙
```java
// CustomerService
public class CustomerService {
    
    /**
     * 규칙 1: 전화번호 중복 검증
     */
    private void validatePhoneDuplication(Long businessId, String phone) {
        customerRepository.findByPhone(businessId, phone).ifPresent(customer -> {
            throw new BusinessException(
                ErrorCode.CUSTOMER_PHONE_DUPLICATED,
                "이미 등록된 전화번호입니다: " + phone
            );
        });
    }
    
    /**
     * 규칙 2: 방문 횟수 증가
     */
    @Transactional
    public void incrementVisitCount(Long customerId) {
        Customer customer = getCustomer(customerId);
        customerRepository.incrementVisitCount(customerId);
        log.info("Customer visit count incremented: id={}, count={}", 
                 customerId, customer.getVisitCount() + 1);
    }
    
    /**
     * 규칙 3: 총 결제 금액 누적
     */
    @Transactional
    public void addTotalSpent(Long customerId, Integer amount) {
        customerRepository.addTotalSpent(customerId, amount);
        log.info("Customer total spent updated: id={}, amount=+{}", 
                 customerId, amount);
    }
}
```

### API 엔드포인트
```
POST   /api/businesses/{businessId}/customers      # 고객 생성
GET    /api/businesses/{businessId}/customers      # 목록 조회 (페이징, 검색)
GET    /api/businesses/{businessId}/customers/{id} # 상세 조회
PUT    /api/businesses/{businessId}/customers/{id} # 수정
DELETE /api/businesses/{businessId}/customers/{id} # 삭제 (soft delete 권장)
GET    /api/businesses/{businessId}/customers/search?q={keyword} # 검색
```

### ErrorCode
```java
// ErrorCode.java에 추가
CUSTOMER_NOT_FOUND(404, "C001", "고객을 찾을 수 없습니다"),
CUSTOMER_PHONE_DUPLICATED(409, "C002", "이미 등록된 전화번호입니다"),
CUSTOMER_EMAIL_DUPLICATED(409, "C003", "이미 등록된 이메일입니다"),
CUSTOMER_INVALID_PHONE_FORMAT(400, "C004", "전화번호 형식이 올바르지 않습니다"),
```

## 🎯 설계 승인 및 다음 단계

### ✅ 설계 승인
위 명세대로 진행하시면 됩니다.

### 다음 단계
1. **@Backend Developer 호출**
   - 위 명세 기반으로 코드 생성 요청
   
2. **Schema SQL 먼저 작성**
   - `schema.sql`에 customers 테이블 추가
   - 인덱스 생성 쿼리 포함
   
3. **Domain 코드 생성 순서**
   - Entity → DTO → Repository → Service → Controller

4. **테스트 데이터 준비**
   - `initdata.sql`에 샘플 고객 10명 추가
```

---

## 💻 Agent 3: Backend Developer

### Agent 정보
```yaml
Name: Backend Code Generator
Role: Spring Boot + MyBatis 코드 자동 생성 전문가
Trigger: "코드 생성", "만들어줘", "구현", "개발"
```

### 역할
**Spring Boot + MyBatis 코드를 자동 생성**하는 백엔드 개발자입니다.

### 코드 생성 패턴

#### 1. Entity (Lombok Builder)
```java
package io.moer.booking.domain.{domain};

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class {Domain} {
    private Long id;
    private Long businessId;
    private String name;
    // ... 필드들
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 비즈니스 로직 메서드
    public boolean isValid() {
        return name != null && !name.isBlank();
    }
}
```

#### 2. DTO (Request/Response)
```java
// CreateRequest
@Getter
@NoArgsConstructor
@Schema(description = "{Domain} 생성 요청")
public class {Domain}CreateRequest {
    
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 100)
    @Schema(description = "이름", example = "홍길동")
    private String name;
    
    // ... 필드들
}

// Response
@Getter
@Builder
public class {Domain}Response {
    private Long id;
    private String name;
    // ... 필드들
    
    public static {Domain}Response from({Domain} entity) {
        return {Domain}Response.builder()
            .id(entity.getId())
            .name(entity.getName())
            .build();
    }
}
```

#### 3. Repository + XML
```java
// Repository Interface
@Mapper
public interface {Domain}Repository {
    void save({Domain} entity);
    Optional<{Domain}> findById(Long id);
    List<{Domain}> findByBusinessId(Long businessId);
    void update({Domain} entity);
    void deleteById(Long id);
}
```

```xml
<!-- MyBatis XML -->
<mapper namespace="io.moer.booking.domain.{domain}.repository.{Domain}Repository">
    
    <resultMap id="{domain}ResultMap" type="io.moer.booking.domain.{domain}.{Domain}">
        <id property="id" column="id"/>
        <result property="businessId" column="business_id"/>
        <result property="name" column="name"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>
    
    <insert id="save" parameterType="{Domain}" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO {domains} (business_id, name)
        VALUES (#{businessId}, #{name})
    </insert>
    
    <select id="findById" resultMap="{domain}ResultMap">
        SELECT * FROM {domains} WHERE id = #{id}
    </select>
</mapper>
```

#### 4. Service (트랜잭션)
```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class {Domain}Service {
    
    private final {Domain}Repository repository;
    
    @Transactional
    public {Domain}Response create(Long businessId, {Domain}CreateRequest request) {
        log.info("Creating {domain}: businessId={}, name={}", businessId, request.getName());
        
        // 1. 비즈니스 규칙 검증
        validateDuplication(businessId, request.getName());
        
        // 2. Entity 생성
        {Domain} entity = {Domain}.builder()
            .businessId(businessId)
            .name(request.getName())
            .build();
        
        // 3. 저장
        repository.save(entity);
        
        log.info("{Domain} created: id={}", entity.getId());
        
        // 4. DTO 변환
        return {Domain}Response.from(entity);
    }
    
    public {Domain}Response get(Long id) {
        return repository.findById(id)
            .map({Domain}Response::from)
            .orElseThrow(() -> new EntityNotFoundException(
                ErrorCode.{DOMAIN}_NOT_FOUND,
                "{Domain}을 찾을 수 없습니다"
            ));
    }
    
    private void validateDuplication(Long businessId, String name) {
        // 중복 검증 로직
    }
}
```

#### 5. Controller (REST API)
```java
@RestController
@RequestMapping("/api/businesses/{businessId}/{domains}")
@RequiredArgsConstructor
@Tag(name = "{Domain}", description = "{Domain} 관리 API")
public class {Domain}Controller {
    
    private final {Domain}Service service;
    
    @PostMapping
    @Operation(summary = "{Domain} 생성")
    public ResponseEntity<ApiResponse<{Domain}Response>> create(
            @PathVariable Long businessId,
            @Valid @RequestBody {Domain}CreateRequest request) {
        
        {Domain}Response response = service.create(businessId, request);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "{Domain} 조회")
    public ResponseEntity<ApiResponse<{Domain}Response>> get(
            @PathVariable Long businessId,
            @PathVariable Long id) {
        
        {Domain}Response response = service.get(id);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

### 호출 예시

```bash
# 전체 도메인 생성 (자동으로 Senior Planner 호출)
@Backend Developer
Customer 도메인을 만들어줘.

필드:
- name, phone, email, birthDate, gender
- tags (JSONB)
- visitCount, totalSpent

비즈니스 규칙:
- 전화번호 중복 방지
- tags는 최대 10개

전체 레이어(Entity, DTO, Repository, Service, Controller) 생성해줘.

# 특정 레이어만 생성
@Backend Developer
Customer 도메인의 Service 메서드를 추가해줘.

새 메서드:
- incrementVisitCount(customerId)
- addTotalSpent(customerId, amount)
- searchByKeyword(businessId, keyword) - 이름/전화번호 검색

# 누락된 부분 보완
@Backend Developer
Reservation 도메인의 Controller가 없어.
CRUD 엔드포인트 6개 만들어줘:
- POST, GET(목록), GET(상세), PUT, DELETE, PATCH(상태변경)
```

### 작업 순서

```markdown
1. @Project Analyzer 호출 (현재 상태 파악)
   ↓
2. @Senior Planner 호출 (설계 검토)
   ↓
3. Schema SQL 생성
   ↓
4. Entity 생성 (+ Enum)
   ↓
5. DTO 생성 (Request, Response, SearchCondition)
   ↓
6. Repository Interface 생성
   ↓
7. MyBatis XML 생성
   ↓
8. Service 생성 (비즈니스 로직)
   ↓
9. Controller 생성 (REST API)
   ↓
10. ErrorCode 추가
   ↓
11. @QA Engineer 호출 (검증)
```

---

## ✅ Agent 4: QA Engineer

### Agent 정보
```yaml
Name: Backend QA Engineer  
Role: 코드 품질 검증 및 테스트 전문가
Trigger: "검증", "테스트", "확인", "체크"
```

### 역할
**생성된 코드를 검증하고 테스트**하는 QA 엔지니어입니다.

### 검증 항목

#### 1. 코드 규칙 준수
```markdown
- [ ] 패키지 구조: io.moer.booking.domain.{domain}
- [ ] Entity는 루트에, 나머지는 하위 패키지
- [ ] Lombok @Builder 패턴 사용
- [ ] DTO에 from() 정적 팩토리 메서드 포함
- [ ] Service는 @Transactional(readOnly=true) 클래스 레벨
- [ ] Controller는 /api/businesses/{businessId} 경로
```

#### 2. MyBatis 매핑 검증
```markdown
- [ ] XML namespace가 Repository 인터페이스와 일치
- [ ] resultMap과 Entity 필드 일치
- [ ] parameterType 정확히 지정
- [ ] JSONB 필드에 TypeHandler 설정
- [ ] Enum 필드에 EnumTypeHandler 설정
```

#### 3. 트랜잭션 설정 확인
```markdown
- [ ] 읽기 메서드: @Transactional(readOnly=true) 또는 클래스 레벨
- [ ] 쓰기 메서드: @Transactional (readOnly 없음)
- [ ] 여러 Repository 호출 시 트랜잭션 전파 확인
- [ ] 예외 발생 시 롤백 보장
```

#### 4. API 테스트 생성

자동으로 curl 테스트 케이스 생성:

```bash
#!/bin/bash
API_BASE="http://localhost:8080/api/businesses/1"

echo "=== Customer Domain API Test ==="

# 1. 생성
echo "\n1. POST /customers (생성)"
curl -X POST $API_BASE/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "홍길동",
    "phone": "010-1234-5678",
    "email": "hong@example.com"
  }' | jq .

# 2. 목록 조회
echo "\n2. GET /customers (목록)"
curl "$API_BASE/customers?page=1&size=10" | jq .

# 3. 상세 조회
echo "\n3. GET /customers/1 (상세)"
curl $API_BASE/customers/1 | jq .

# 4. 수정
echo "\n4. PUT /customers/1 (수정)"
curl -X PUT $API_BASE/customers/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "홍길동(수정)",
    "phone": "010-1234-5678"
  }' | jq .

# 5. 삭제
echo "\n5. DELETE /customers/1 (삭제)"
curl -X DELETE $API_BASE/customers/1 | jq .

# 6. 에러 시나리오 - 중복 전화번호
echo "\n6. POST /customers (중복 전화번호 에러)"
curl -X POST $API_BASE/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "김철수",
    "phone": "010-1234-5678"
  }' | jq .
# 예상: 409 Conflict, "이미 등록된 전화번호입니다"
```

### 호출 예시

```bash
# 전체 도메인 검증
@QA Engineer
Customer 도메인 전체를 검증해줘.
- 코드 규칙 준수 확인
- MyBatis XML 매핑 확인
- API 테스트 케이스 생성

# 특정 부분 검증
@QA Engineer
ReservationService의 트랜잭션 설정을 확인해줘.
createReservation() 메서드에서
CustomerService를 호출하는데
트랜잭션 전파가 제대로 되는지 검증해줘.

# 에러 시나리오 테스트
@QA Engineer
Customer 도메인의 에러 시나리오를 테스트해줘:
1. 중복 전화번호
2. 잘못된 이메일 형식
3. 존재하지 않는 고객 조회
4. 필수 필드 누락
```

### 검증 리포트 예시

```markdown
# Customer 도메인 검증 리포트

## ✅ 통과 항목

### 코드 규칙 (10/10)
- ✅ 패키지 구조 정확
- ✅ Lombok Builder 사용
- ✅ DTO from() 메서드 포함
- ✅ Service @Transactional 적절
- ✅ Controller 경로 일관성

### MyBatis 매핑 (5/5)
- ✅ namespace 일치
- ✅ resultMap 정확
- ✅ JSONB TypeHandler 설정
- ✅ 동적 쿼리 작동
- ✅ 페이징 쿼리 정확

### API 테스트 (6/6)
- ✅ POST /customers (201 Created)
- ✅ GET /customers (200 OK, 페이징)
- ✅ GET /customers/1 (200 OK)
- ✅ PUT /customers/1 (200 OK)
- ✅ DELETE /customers/1 (204 No Content)
- ✅ 중복 전화번호 (409 Conflict)

## ⚠️ 개선 필요 사항

### 1. Service 로깅 불충분
**위치**: CustomerService.create()
**문제**: 생성 시작 로그만 있고 완료 로그 없음
**제안**:
```java
@Transactional
public CustomerResponse create(Long businessId, CustomerCreateRequest request) {
    log.info("Creating customer: businessId={}, name={}", businessId, request.getName());
    
    // ... 비즈니스 로직
    
    repository.save(customer);
    
    log.info("Customer created: id={}, businessId={}", customer.getId(), businessId); // 추가
    
    return CustomerResponse.from(customer);
}
```

### 2. 예외 메시지 개선
**위치**: CustomerService.validatePhoneDuplication()
**현재**:
```java
throw new BusinessException(
    ErrorCode.CUSTOMER_PHONE_DUPLICATED,
    "이미 등록된 전화번호입니다"
);
```
**제안**: 전화번호 포함으로 명확화
```java
throw new BusinessException(
    ErrorCode.CUSTOMER_PHONE_DUPLICATED,
    String.format("이미 등록된 전화번호입니다: %s", phone)
);
```

### 3. 페이지네이션 테스트 부족
**현재**: 기본 페이징(page=1, size=10)만 테스트
**추가 필요**:
- 빈 결과 (page=999)
- 경계값 (size=1, size=100)
- 정렬 테스트

## 📊 테스트 커버리지
- Entity: 100% (모든 필드 검증됨)
- Service: 85% (validateDuplication 예외 케이스 추가 필요)
- Controller: 90% (에러 응답 포맷 확인 필요)

## 🎯 최종 판정
**전체 품질**: ✅ 우수 (90/100점)
**배포 가능**: ✅ 예 (minor 이슈만 존재)

**권장 사항**:
1. 로깅 보완 (5분)
2. 예외 메시지 개선 (3분)
3. 추가 테스트 작성 (선택)
```

---

## 🔄 Agent 협업 시나리오

### 시나리오 1: 새 도메인 전체 생성

```bash
사용자: "Tag 도메인을 만들어줘. businessId + name + color 필드"

@Backend Developer 시작
  ↓
[Step 1] @Project Analyzer 호출
  → 현재 6개 도메인 완료, Tag는 미착수 확인
  ↓
[Step 2] @Senior Planner 호출
  → 설계 검토: UNIQUE(business_id, name), color HEX 검증
  ↓
[Step 3] 코드 생성
  → Entity, DTO, Repository, Service, Controller 생성
  ↓
[Step 4] @QA Engineer 호출
  → 검증 통과, curl 테스트 케이스 제공
  ↓
완료 보고: "Tag 도메인 100% 완성!"
```

### 시나리오 2: 기존 도메인 개선

```bash
사용자: "Reservation 도메인에 노쇼 상태 추가해줘"

@Senior Planner 시작
  ↓
[Step 1] 현재 상태 분석
  → PENDING, CONFIRMED, COMPLETED, CANCELLED
  ↓
[Step 2] 개선 설계
  → NO_SHOW 상태 추가
  → 상태 전이: CONFIRMED → NO_SHOW
  → 페널티 규칙 추가
  ↓
설계 승인 → @Backend Developer 전달
  ↓
@Backend Developer 시작
  ↓
[Step 3] 코드 수정
  → ReservationStatus Enum 수정
  → Service에 markAsNoShow() 메서드 추가
  → Controller에 PATCH 엔드포인트 추가
  ↓
@QA Engineer 검증
  ↓
완료!
```

### 시나리오 3: 진행 상황 파악

```bash
사용자: "현재까지 뭐가 완료됐고, 다음에 뭘 해야 해?"

@Project Analyzer 시작
  ↓
[Step 1] 프로젝트 스캔
  → 6개 도메인 완료 (User, Business, ...)
  → Reservation 70% (Controller 미작성)
  → CustomerHistory 미착수
  ↓
[Step 2] 리포트 생성
  → 진행률 75%
  → 우선순위: Reservation Controller 완성
  ↓
[Step 3] 제안
  → "Reservation Controller를 먼저 완성하세요 (1시간)"
  → "그 다음 CustomerHistory 시작 (2시간)"
```

---

## 📚 참고 자료

### 프로젝트 문서
- `/mnt/project/layered-architecture.md` - 레이어 아키텍처
- `/mnt/project/development-pattern.md` - 개발 패턴
- `/mnt/project/schema.sql` - 데이터베이스 스키마
- `/mnt/project/exception-handling.md` - 예외 처리
- `/mnt/project/security.md` - 보안 설정

### 기존 도메인 참고
1. **Service 도메인** (가장 단순, CRUD 표준)
2. **Staff 도메인** (JSONB portfolio 활용)
3. **Reservation 도메인** (복잡한 비즈니스 로직)

---

## 🎯 핵심 원칙

### 1. 레이어 분리
```
Controller → Service → Repository → Database
     ↓          ↓          ↓
    DTO      Entity     Entity
```

### 2. 네이밍 규칙
- Entity: `{Domain}.java`
- Request DTO: `{Domain}{Action}Request.java`
- Response DTO: `{Domain}Response.java`
- Repository: `{Domain}Repository.java`
- Service: `{Domain}Service.java`
- Controller: `{Domain}Controller.java`

### 3. 패키지 구조
```
io.moer.booking.domain.{domain}/
├── {Domain}.java (Entity, 루트)
├── {DomainEnum}.java (Enum, 루트)
├── controller/
│   └── {Domain}Controller.java
├── dto/
│   ├── {Domain}CreateRequest.java
│   ├── {Domain}UpdateRequest.java
│   ├── {Domain}Response.java
│   └── {Domain}SearchCondition.java
├── repository/
│   └── {Domain}Repository.java
└── service/
    └── {Domain}Service.java
```

### 4. 트랜잭션 원칙
- 읽기: `@Transactional(readOnly = true)`
- 쓰기: `@Transactional`
- Service 레벨에서 관리
- Repository는 트랜잭션 무관

### 5. API 경로 규칙
```
POST   /api/businesses/{businessId}/{domains}
GET    /api/businesses/{businessId}/{domains}
GET    /api/businesses/{businessId}/{domains}/{id}
PUT    /api/businesses/{businessId}/{domains}/{id}
DELETE /api/businesses/{businessId}/{domains}/{id}
```

---

## ✅ 체크리스트

### 새 도메인 생성 시
- [ ] @Senior Planner로 설계 검토
- [ ] Schema SQL 작성
- [ ] Entity 생성 (+ Enum)
- [ ] DTO 3종 생성 (Request, Response, Condition)
- [ ] Repository Interface 생성
- [ ] MyBatis XML 생성
- [ ] Service 생성 (비즈니스 로직)
- [ ] Controller 생성 (REST API)
- [ ] ErrorCode 추가
- [ ] @QA Engineer로 검증
- [ ] Git 커밋

---

**이 SKILL을 사용하면 도메인 개발이 자동화됩니다!**
- Project Analyzer: 진행 상황 파악
- Senior Planner: 설계 검토
- Backend Developer: 코드 자동 생성
- QA Engineer: 품질 검증

각 Agent를 독립적으로 호출하거나,
Backend Developer가 자동으로 다른 Agent를 호출하여 협업합니다.
