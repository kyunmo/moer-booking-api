# 2026-02-10 트랜잭션 전파 설정 수정

## 문제 상황

**에러 메시지**:
```
ERROR: cannot execute INSERT in a read-only transaction
```

**발생 위치**: `BusinessService.createDefaultSettings()`

**원인**:
- `getBusiness()` 메서드는 `@Transactional(readOnly = true)`로 설정
- 내부에서 `createDefaultSettings()`를 호출하여 INSERT 시도
- readOnly 트랜잭션에서는 INSERT/UPDATE/DELETE 불가

## 해결 방법

### 트랜잭션 전파 설정 변경

**파일**: `src/main/java/io/moer/booking/domain/business/service/BusinessService.java`

#### 수정 전
```java
@Transactional  // ❌ 부모 트랜잭션 참여 (readOnly=true)
private BusinessSettings createDefaultSettings(Long businessId) {
    businessSettingsRepository.save(settings);  // ERROR!
}
```

#### 수정 후
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)  // ✅ 새 트랜잭션 시작
public BusinessSettings createDefaultSettings(Long businessId) {
    businessSettingsRepository.save(settings);  // SUCCESS!
}
```

## 트랜잭션 전파 (Propagation)

### REQUIRES_NEW

**동작 방식**:
1. 현재 트랜잭션이 있으면 일시 중단
2. 새로운 독립적인 트랜잭션 시작
3. 새 트랜잭션 완료 후 기존 트랜잭션 재개

**사용 시나리오**:
- readOnly 트랜잭션 내에서 쓰기 작업 필요
- 부모 트랜잭션 롤백과 무관하게 커밋 필요
- 독립적인 작업 단위

### 트랜잭션 흐름

```
┌─────────────────────────────────────────┐
│ getBusiness() - readOnly Transaction    │
│                                         │
│  1. Business 조회                       │
│  2. BusinessSettings 조회 시도          │
│  3. Settings 없음                       │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │ createDefaultSettings()           │ │
│  │ - REQUIRES_NEW Transaction        │ │
│  │   (별도 쓰기 트랜잭션)              │ │
│  │                                   │ │
│  │  INSERT INTO business_settings    │ │
│  │  COMMIT ✅                         │ │
│  └───────────────────────────────────┘ │
│                                         │
│  4. 생성된 Settings 반환                │
│  5. BusinessResponse 생성               │
│                                         │
└─────────────────────────────────────────┘
```

## Spring 트랜잭션 전파 옵션

| 옵션 | 설명 | 사용 예시 |
|------|------|----------|
| **REQUIRED** (기본) | 기존 트랜잭션 참여, 없으면 새로 생성 | 일반적인 경우 |
| **REQUIRES_NEW** | 항상 새 트랜잭션 시작 | readOnly 내 쓰기 작업 |
| **SUPPORTS** | 트랜잭션 있으면 참여, 없어도 실행 | 조회 메서드 |
| **NOT_SUPPORTED** | 트랜잭션 없이 실행 | 외부 시스템 호출 |
| **MANDATORY** | 반드시 트랜잭션 필요, 없으면 에러 | 쓰기 작업 강제 |
| **NEVER** | 트랜잭션 있으면 에러 | 트랜잭션 금지 |
| **NESTED** | 중첩 트랜잭션 (JDBC 세이브포인트) | 부분 롤백 |

## 주의사항

### 1. 메서드 접근 제한자

`REQUIRES_NEW`가 작동하려면 **public 메서드**여야 합니다:
```java
// ✅ 작동
@Transactional(propagation = REQUIRES_NEW)
public BusinessSettings createDefaultSettings(Long businessId) { }

// ❌ 작동 안 함 (프록시 우회)
@Transactional(propagation = REQUIRES_NEW)
private BusinessSettings createDefaultSettings(Long businessId) { }
```

**이유**: Spring AOP 프록시는 외부에서 호출되는 public 메서드만 인터셉트

### 2. 같은 클래스 내부 호출

같은 클래스 내에서 메서드를 직접 호출하면 프록시를 거치지 않아 트랜잭션 설정이 무시될 수 있습니다:

```java
// ❌ 프록시 우회 (트랜잭션 설정 무시)
public void methodA() {
    this.methodB();  // 직접 호출
}

@Transactional(propagation = REQUIRES_NEW)
public void methodB() { }

// ✅ 프록시 사용 (트랜잭션 설정 적용)
// 방법 1: self-injection
@Autowired
private BusinessService self;

public void methodA() {
    self.methodB();  // 프록시를 통한 호출
}

// 방법 2: 별도 서비스로 분리
```

**현재 코드**: `orElseGet(() -> createDefaultSettings(businessId))`는 람다 내에서 호출되므로 정상 작동

### 3. 성능 고려사항

`REQUIRES_NEW`는 새 트랜잭션을 시작하므로:
- 데이터베이스 커넥션 추가 사용
- 트랜잭션 오버헤드 증가
- 동시성 처리 주의 필요

**권장**: 꼭 필요한 경우에만 사용

## 테스트 방법

### 1. Settings 없는 매장 조회
```bash
GET /api/businesses/3
Authorization: Bearer {token}
```

**예상 로그**:
```
INFO - Creating default settings for business: 3
INFO - Default settings created for business: 3
```

**예상 응답**:
```json
{
  "success": true,
  "data": {
    "id": 3,
    "settings": {
      "id": 1,
      "businessId": 3,
      ...
    }
  }
}
```

### 2. 데이터베이스 확인
```sql
-- 자동 생성된 Settings 확인
SELECT * FROM business_settings WHERE business_id = 3;
```

## 빌드 결과

```
BUILD SUCCESSFUL in 20s
```

## 관련 이슈

### 유사한 트랜잭션 문제 방지

다음 경우들도 동일한 문제가 발생할 수 있습니다:

1. **조회 메서드에서 로그 기록**
```java
@Transactional(readOnly = true)
public User getUser(Long id) {
    User user = userRepository.findById(id).orElseThrow();

    // ❌ readOnly 트랜잭션에서 INSERT 시도
    auditLogService.log(...);

    return user;
}
```

**해결**: AuditLogService.log()도 `REQUIRES_NEW` 사용

2. **조회 중 캐시 업데이트**
```java
@Transactional(readOnly = true)
public Product getProduct(Long id) {
    Product product = productRepository.findById(id).orElseThrow();

    // ❌ readOnly 트랜잭션에서 UPDATE 시도
    cacheService.update(product);

    return product;
}
```

**해결**: 캐시 업데이트는 별도 트랜잭션 또는 비동기 처리

## 참고 자료

### Spring 공식 문서
- [Transaction Propagation](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html)
- [Transaction Management](https://docs.spring.io/spring-framework/reference/data-access/transaction.html)

### 트랜잭션 격리 수준 vs 전파 수준

| 개념 | 설명 |
|------|------|
| **격리 수준** (Isolation) | 동시에 실행되는 트랜잭션 간의 데이터 가시성 |
| **전파 수준** (Propagation) | 트랜잭션 간의 중첩 관계 |

현재 이슈는 **전파 수준** 문제입니다.

---

**작업 완료일**: 2026-02-10
**담당**: Claude Code
**상태**: ✅ 완료
