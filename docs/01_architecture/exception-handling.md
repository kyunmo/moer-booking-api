# 예외 처리

moer 예약 시스템의 예외 처리 구조를 설명합니다.

## 개요

- **전역 예외 처리**: `@RestControllerAdvice` 사용
- **계층화된 예외**: `BaseException` 추상 클래스 상속
- **표준화된 에러 코드**: `ErrorCode` Enum
- **통합 에러 응답**: `ApiResponse<Void>` 포맷

## 예외 처리 구조

### 1. 전체 흐름도

```
┌──────────────┐
│  Controller  │
└──────┬───────┘
       │
       │ 비즈니스 로직 실행
       ↓
┌──────────────┐
│   Service    │
└──────┬───────┘
       │
       │ 예외 발생 (BusinessException, EntityNotFoundException 등)
       ↓
┌─────────────────────────┐
│ GlobalExceptionHandler  │  ← @RestControllerAdvice
│  (@ExceptionHandler)    │
└─────────┬───────────────┘
          │
          │ ErrorCode 기반 응답 생성
          ↓
┌─────────────────────────┐
│   ApiResponse<Void>     │  ← 통합 에러 응답
│  {                      │
│    "success": false,    │
│    "error": {           │
│      "code": "U001",    │
│      "message": "..."   │
│    }                    │
│  }                      │
└─────────────────────────┘
```

## ErrorCode (에러 코드 체계)

### 1. ErrorCode Enum

**위치**: `src/main/java/io/moer/booking/common/exception/ErrorCode.java`

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통 에러 (C001~C006)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "지원하지 않는 HTTP 메서드입니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류가 발생했습니다"),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C004", "잘못된 타입입니다"),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C005", "엔티티를 찾을 수 없습니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C006", "접근 권한이 없습니다"),

    // 인증/권한 관련 에러 (A001~A005)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "만료된 토큰입니다"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A004", "아이디 또는 비밀번호가 올바르지 않습니다"),
    TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "A005", "토큰을 찾을 수 없습니다"),

    // 사용자 관련 에러 (U001~U004)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 사용 중인 이메일입니다"),
    DUPLICATE_PHONE(HttpStatus.CONFLICT, "U003", "이미 사용 중인 전화번호입니다"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "U004", "비밀번호가 일치하지 않습니다"),

    // 비즈니스 관련 에러 (B001~B003)
    BUSINESS_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "매장을 찾을 수 없습니다"),
    BUSINESS_ALREADY_EXISTS(HttpStatus.CONFLICT, "B002", "이미 등록된 매장입니다"),
    BUSINESS_ACCESS_DENIED(HttpStatus.FORBIDDEN, "B003", "해당 매장에 접근 권한이 없습니다"),

    // 예약 관련 에러 (R001~R007)
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "예약을 찾을 수 없습니다"),
    RESERVATION_TIME_CONFLICT(HttpStatus.CONFLICT, "R002", "이미 예약된 시간입니다"),
    RESERVATION_ALREADY_CONFIRMED(HttpStatus.BAD_REQUEST, "R003", "이미 확정된 예약입니다"),
    RESERVATION_CANCELLED(HttpStatus.BAD_REQUEST, "R004", "취소된 예약입니다"),
    RESERVATION_PAST_DATE(HttpStatus.BAD_REQUEST, "R005", "과거 날짜는 예약할 수 없습니다"),
    RESERVATION_HOLIDAY(HttpStatus.BAD_REQUEST, "R006", "해당 날짜는 휴무일입니다"),
    RESERVATION_INVALID_STATUS(HttpStatus.BAD_REQUEST, "R007", "잘못된 예약 상태입니다"),

    // Staff 관련 에러 (S001~S002)
    STAFF_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "직원을 찾을 수 없습니다"),
    STAFF_ACCESS_DENIED(HttpStatus.FORBIDDEN, "S002", "해당 직원 정보에 접근 권한이 없습니다"),

    // Customer 관련 에러 (CU001~CU002)
    CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND, "CU001", "고객을 찾을 수 없습니다"),
    DUPLICATE_CUSTOMER_PHONE(HttpStatus.CONFLICT, "CU002", "이미 등록된 전화번호입니다"),

    // Service 관련 에러 (SV001~SV002)
    SERVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "SV001", "서비스를 찾을 수 없습니다"),
    SERVICE_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "SV002", "이용할 수 없는 서비스입니다"),

    // Portfolio 관련 에러 (P001)
    PORTFOLIO_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "포트폴리오를 찾을 수 없습니다"),

    // Holiday 관련 에러 (H001)
    HOLIDAY_NOT_FOUND(HttpStatus.NOT_FOUND, "H001", "휴무일을 찾을 수 없습니다"),

    // History 관련 에러 (HI001)
    HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "HI001", "이력을 찾을 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

### 2. 에러 코드 명명 규칙

| 접두사 | 도메인 | 범위 | 예시 |
|--------|--------|------|------|
| **C** | Common (공통) | C001~C099 | C001: 잘못된 입력값 |
| **A** | Auth (인증) | A001~A099 | A004: 잘못된 자격증명 |
| **U** | User (사용자) | U001~U099 | U001: 사용자 미존재 |
| **B** | Business (매장) | B001~B099 | B001: 매장 미존재 |
| **R** | Reservation (예약) | R001~R099 | R002: 시간 충돌 |
| **S** | Staff (직원) | S001~S099 | S001: 직원 미존재 |
| **CU** | Customer (고객) | CU001~CU099 | CU001: 고객 미존재 |
| **SV** | Service (서비스) | SV001~SV099 | SV001: 서비스 미존재 |
| **P** | Portfolio | P001~P099 | P001: 포트폴리오 미존재 |
| **H** | Holiday (휴무일) | H001~H099 | H001: 휴무일 미존재 |
| **HI** | History (이력) | HI001~HI099 | HI001: 이력 미존재 |

## 예외 클래스 계층 구조

### 1. BaseException (추상 클래스)

**위치**: `src/main/java/io/moer/booking/common/exception/BaseException.java`

```java
@Getter
public abstract class BaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object details;  // 추가 상세 정보

    protected BaseException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    protected BaseException(ErrorCode errorCode, Object details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = details;
    }
}
```

### 2. BusinessException

비즈니스 규칙 위반 시 사용합니다.

**위치**: `src/main/java/io/moer/booking/common/exception/BusinessException.java`

```java
public class BusinessException extends BaseException {

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(ErrorCode errorCode, Object details) {
        super(errorCode, details);
    }
}
```

**사용 예시**:
```java
// 이메일 중복
if (userRepository.findByEmail(email).isPresent()) {
    throw new BusinessException(
        ErrorCode.DUPLICATE_EMAIL,
        "이미 사용 중인 이메일입니다: " + email
    );
}

// 시간 충돌
if (hasTimeConflict(staffId, date, startTime, endTime)) {
    throw new BusinessException(
        ErrorCode.RESERVATION_TIME_CONFLICT,
        Map.of(
            "staffId", staffId,
            "date", date,
            "startTime", startTime,
            "endTime", endTime
        )
    );
}

// 접근 권한 없음
if (!currentUser.canAccessBusiness(businessId)) {
    throw new BusinessException(
        ErrorCode.BUSINESS_ACCESS_DENIED,
        "해당 매장에 접근 권한이 없습니다"
    );
}
```

### 3. EntityNotFoundException

엔티티를 찾을 수 없을 때 사용합니다.

**위치**: `src/main/java/io/moer/booking/common/exception/EntityNotFoundException.java`

```java
public class EntityNotFoundException extends BaseException {

    public EntityNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EntityNotFoundException(ErrorCode errorCode, Object details) {
        super(errorCode, details);
    }
}
```

**사용 예시**:
```java
// 사용자 조회
User user = userRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException(
            ErrorCode.USER_NOT_FOUND,
            "사용자를 찾을 수 없습니다: " + id
        ));

// 매장 조회
Business business = businessRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException(
            ErrorCode.BUSINESS_NOT_FOUND,
            "매장을 찾을 수 없습니다: " + id
        ));

// 예약 조회
Reservation reservation = reservationRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException(
            ErrorCode.RESERVATION_NOT_FOUND,
            "예약을 찾을 수 없습니다: " + id
        ));
```

## GlobalExceptionHandler (전역 예외 처리기)

**위치**: `src/main/java/io/moer/booking/common/exception/GlobalExceptionHandler.java`

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * BaseException (우리가 정의한 예외) 처리
     */
    @ExceptionHandler(BaseException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
        log.error("BaseException: {}", e.getMessage(), e);

        ErrorCode errorCode = e.getErrorCode();
        ApiResponse<Void> response = ApiResponse.error(
                errorCode.getCode(),
                errorCode.getMessage(),
                e.getDetails()  // 추가 상세 정보
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }

    /**
     * Validation 실패 (@Valid, @Validated)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException: {}", e.getMessage());

        // 필드별 에러 메시지 추출
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.INVALID_INPUT_VALUE.getCode(),
                ErrorCode.INVALID_INPUT_VALUE.getMessage(),
                errors  // 필드별 에러 메시지
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 타입 불일치
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        log.error("MethodArgumentTypeMismatchException: {}", e.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.INVALID_TYPE_VALUE.getCode(),
                ErrorCode.INVALID_TYPE_VALUE.getMessage(),
                String.format("%s는 %s 타입이어야 합니다",
                        e.getName(),
                        e.getRequiredType().getSimpleName())
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 지원하지 않는 HTTP 메서드
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        log.error("HttpRequestMethodNotSupportedException: {}", e.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.METHOD_NOT_ALLOWED.getCode(),
                ErrorCode.METHOD_NOT_ALLOWED.getMessage(),
                String.format("지원하는 메서드: %s", String.join(", ", e.getSupportedMethods()))
        );

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(response);
    }

    /**
     * 그 외 모든 예외
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected Exception: {}", e.getMessage(), e);

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
```

## 에러 응답 포맷

### 1. 기본 에러 응답

```json
{
  "success": false,
  "error": {
    "code": "U001",
    "message": "사용자를 찾을 수 없습니다"
  },
  "timestamp": "2026-02-08T12:34:56"
}
```

### 2. 상세 정보가 있는 에러 응답

```json
{
  "success": false,
  "error": {
    "code": "R002",
    "message": "이미 예약된 시간입니다",
    "details": {
      "staffId": 123,
      "date": "2026-02-10",
      "startTime": "14:00",
      "endTime": "15:00"
    }
  },
  "timestamp": "2026-02-08T12:34:56"
}
```

### 3. Validation 에러 응답

```json
{
  "success": false,
  "error": {
    "code": "C001",
    "message": "잘못된 입력값입니다",
    "details": {
      "email": "이메일 형식이 올바르지 않습니다",
      "password": "비밀번호는 8자 이상이어야 합니다",
      "name": "이름은 필수입니다"
    }
  },
  "timestamp": "2026-02-08T12:34:56"
}
```

## Validation (입력값 검증)

### 1. DTO에서 Validation

```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다")
    private String password;

    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 50, message = "이름은 2~50자여야 합니다")
    private String name;

    @Pattern(regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$",
             message = "전화번호 형식이 올바르지 않습니다")
    private String phone;

    @NotNull(message = "역할은 필수입니다")
    private UserRole role;
}
```

### 2. Controller에서 @Valid 사용

```java
@PostMapping
public ResponseEntity<ApiResponse<UserResponse>> createUser(
        @Valid @RequestBody UserCreateRequest request) {  // ← @Valid

    UserResponse response = userService.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response));
}
```

### 3. Validation 에러 처리

Validation 실패 시 `MethodArgumentNotValidException`이 발생하고, `GlobalExceptionHandler`에서 자동으로 처리됩니다.

```
Client Request
    ↓
Controller (@Valid 검증)
    ↓ (Validation 실패)
MethodArgumentNotValidException
    ↓
GlobalExceptionHandler.handleMethodArgumentNotValidException()
    ↓
ApiResponse<Void> (C001 에러 응답)
    ↓
Client Response (400 Bad Request)
```

## 실제 예외 처리 예시

### 1. 예약 생성 시 시간 충돌 검증

```java
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;

    @Transactional
    public ReservationResponse createReservation(
            Long businessId, ReservationCreateRequest request, User currentUser) {

        // ... (다른 검증 로직)

        // 시간 충돌 체크
        boolean hasConflict = reservationRepository.existsByStaffIdAndDateAndTimeRange(
                request.getStaffId(),
                request.getReservationDate(),
                request.getStartTime(),
                endTime
        );

        if (hasConflict) {
            throw new BusinessException(
                ErrorCode.RESERVATION_TIME_CONFLICT,
                Map.of(
                    "staffId", request.getStaffId(),
                    "date", request.getReservationDate(),
                    "startTime", request.getStartTime(),
                    "endTime", endTime
                )
            );
        }

        // 예약 생성 로직
        // ...
    }
}
```

**클라이언트 응답**:
```json
{
  "success": false,
  "error": {
    "code": "R002",
    "message": "이미 예약된 시간입니다",
    "details": {
      "staffId": 123,
      "date": "2026-02-10",
      "startTime": "14:00",
      "endTime": "15:00"
    }
  },
  "timestamp": "2026-02-08T12:34:56"
}
```

### 2. 엔티티 조회 시 미존재 검증

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id, User currentUser) {
        // 1. 사용자 조회 (미존재 시 예외)
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                    ErrorCode.USER_NOT_FOUND,
                    "사용자를 찾을 수 없습니다: " + id
                ));

        // 2. 권한 체크 (권한 없으면 예외)
        if (!currentUser.canAccessUser(user)) {
            throw new BusinessException(
                ErrorCode.ACCESS_DENIED,
                "해당 사용자 정보에 접근 권한이 없습니다"
            );
        }

        return UserResponse.from(user);
    }
}
```

**클라이언트 응답 (미존재)**:
```json
{
  "success": false,
  "error": {
    "code": "U001",
    "message": "사용자를 찾을 수 없습니다",
    "details": "사용자를 찾을 수 없습니다: 999"
  },
  "timestamp": "2026-02-08T12:34:56"
}
```

**클라이언트 응답 (권한 없음)**:
```json
{
  "success": false,
  "error": {
    "code": "C006",
    "message": "접근 권한이 없습니다",
    "details": "해당 사용자 정보에 접근 권한이 없습니다"
  },
  "timestamp": "2026-02-08T12:34:56"
}
```

### 3. 복잡한 비즈니스 규칙 검증

```java
@Service
@RequiredArgsConstructor
public class ReservationService {

    @Transactional
    public void confirmReservation(Long id, User currentUser) {
        // 1. 예약 조회
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                    ErrorCode.RESERVATION_NOT_FOUND,
                    "예약을 찾을 수 없습니다"
                ));

        // 2. 권한 체크
        if (!currentUser.canAccessBusiness(reservation.getBusinessId())) {
            throw new BusinessException(
                ErrorCode.BUSINESS_ACCESS_DENIED,
                "해당 예약에 접근 권한이 없습니다"
            );
        }

        // 3. 상태 체크 (이미 확정된 예약)
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            throw new BusinessException(
                ErrorCode.RESERVATION_ALREADY_CONFIRMED,
                "이미 확정된 예약입니다"
            );
        }

        // 4. 상태 체크 (취소된 예약)
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException(
                ErrorCode.RESERVATION_CANCELLED,
                "취소된 예약은 확정할 수 없습니다"
            );
        }

        // 5. 예약 확정
        reservation.confirm();
        reservationRepository.update(reservation);
    }
}
```

## 예외 처리 Best Practices

### 1. 예외 발생 시점
- ✅ Service 계층에서 예외 발생
- ✅ 비즈니스 규칙 위반 즉시 예외 발생
- ❌ Controller에서 예외 발생 지양

### 2. 예외 메시지
- ✅ 사용자에게 의미 있는 메시지
- ✅ ErrorCode에 기본 메시지 정의
- ✅ details에 추가 정보 포함 (선택적)
- ❌ 기술적인 스택 트레이스 노출 금지

### 3. HTTP 상태 코드
- `400 Bad Request` - 잘못된 입력값, 비즈니스 규칙 위반
- `401 Unauthorized` - 인증 실패
- `403 Forbidden` - 권한 없음
- `404 Not Found` - 엔티티 미존재
- `409 Conflict` - 중복, 충돌
- `500 Internal Server Error` - 서버 오류

### 4. 로깅
- ✅ 모든 예외는 로깅 (`log.error()`)
- ✅ 예외 스택 트레이스 포함
- ✅ 비즈니스 예외는 INFO, 시스템 예외는 ERROR
- ❌ 민감한 정보 (비밀번호 등) 로깅 금지

### 5. 예외 전파
- ✅ Service → Controller → GlobalExceptionHandler
- ✅ RuntimeException 사용 (트랜잭션 롤백)
- ❌ Checked Exception 사용 지양
- ❌ 예외를 catch 후 무시 금지

## 다음 문서

- [패키지 구조](./package-structure.md)
- [레이어 아키텍처](./layered-architecture.md)
- [보안 구조](./security.md)
