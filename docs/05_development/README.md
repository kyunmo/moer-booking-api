# 05. 개발 가이드

개발 환경 설정 및 개발 가이드입니다.

## 목차

1. [개발 환경 설정](#개발-환경-설정)
2. [로컬 실행](#로컬-실행)
3. [코딩 컨벤션](#코딩-컨벤션)
4. [Git 워크플로우](#git-워크플로우)
5. [새 도메인 추가](#새-도메인-추가)

## 개발 환경 설정

### 필수 요구사항

- **Java**: 17 이상
- **Gradle**: 8.x (Wrapper 사용)
- **PostgreSQL**: 16
- **Docker**: 20.x 이상 (선택, PostgreSQL 컨테이너용)
- **IDE**: IntelliJ IDEA (권장) 또는 Eclipse

### IDE 설정 (IntelliJ IDEA)

#### 1. 프로젝트 열기

```
File > Open > 프로젝트 루트 디렉토리 선택
```

#### 2. Lombok 플러그인 설치

```
File > Settings > Plugins > "Lombok" 검색 및 설치
File > Settings > Build, Execution, Deployment > Compiler > Annotation Processors
  > Enable annotation processing 체크
```

#### 3. Code Style 설정

```
File > Settings > Editor > Code Style > Java
  - Tab size: 4
  - Indent: 4
  - Continuation indent: 8
```

#### 4. File Encoding 설정

```
File > Settings > Editor > File Encodings
  - Global Encoding: UTF-8
  - Project Encoding: UTF-8
  - Default encoding for properties files: UTF-8
```

## 로컬 실행

### 1. PostgreSQL 시작 (Docker)

```bash
# PostgreSQL 컨테이너 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f postgres

# 컨테이너 중지
docker-compose down
```

### 2. 데이터베이스 초기화

```bash
# PostgreSQL 접속
docker exec -it moer-postgres psql -U moer -d moer_dev

# 스키마 생성 (최초 1회)
\i /docker-entrypoint-initdb.d/schema.sql

# 초기 데이터 삽입 (선택)
\i /docker-entrypoint-initdb.d/initdata.sql
```

또는 애플리케이션 실행 시 자동으로 초기화됩니다 (`spring.sql.init.mode=always` 설정 시).

### 3. 애플리케이션 실행

#### Gradle 사용
```bash
# 빌드
./gradlew clean build

# 실행
./gradlew bootRun
```

#### IDE에서 실행
```
src/main/java/io/moer/booking/MoerBookingApplication.java
우클릭 > Run 'MoerBookingApplication'
```

### 4. 접속 확인

- API 서버: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health Check: http://localhost:8080/api/health

## 코딩 컨벤션

### 1. 네이밍 규칙

#### 클래스명
- **PascalCase** 사용
- 명사형
- 예: `UserService`, `ReservationController`, `CustomerRepository`

#### 메서드명
- **camelCase** 사용
- 동사형
- 예: `createUser()`, `findById()`, `updateReservation()`

#### 변수명
- **camelCase** 사용
- 명사형
- 예: `userId`, `reservationDate`, `totalPrice`

#### 상수명
- **UPPER_SNAKE_CASE** 사용
- 예: `MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE`

#### 패키지명
- **lowercase** 사용
- 단수형
- 예: `user`, `business`, `reservation`

### 2. 주석 규칙

#### 클래스 주석
```java
/**
 * 사용자 서비스
 * 사용자 생성, 조회, 수정, 삭제 기능을 제공합니다.
 */
@Service
public class UserService {
}
```

#### 메서드 주석
```java
/**
 * 사용자 생성
 *
 * @param request 사용자 생성 요청 DTO
 * @return 생성된 사용자 응답 DTO
 * @throws BusinessException 이메일 중복 시
 */
@Transactional
public UserResponse createUser(UserCreateRequest request) {
}
```

#### 복잡한 로직 주석
```java
// 1. 고객 조회 또는 자동 생성
Customer customer = resolveCustomer(businessId, request);

// 2. 시간 충돌 체크
validateReservation(businessId, staffId, date, startTime, endTime);

// 3. 예약 생성
Reservation reservation = Reservation.builder()...
```

### 3. 코드 스타일

#### 들여쓰기
- **4 스페이스** 사용 (탭 사용 금지)

#### 줄 길이
- **최대 120자** 권장
- 120자 초과 시 줄바꿈

#### 중괄호
- **K&R 스타일** 사용
```java
// Good
if (condition) {
    doSomething();
}

// Bad
if (condition)
{
    doSomething();
}
```

#### 빈 줄
- 메서드 사이: 1줄
- 로직 블록 사이: 1줄

### 4. Lombok 사용

#### 권장
- `@Getter`, `@Builder`, `@RequiredArgsConstructor`
- `@Slf4j` (로깅)

#### 지양
- `@Setter` (Entity는 불변성 유지)
- `@Data` (너무 많은 기능 포함)

### 5. 예외 처리

#### 비즈니스 예외
```java
// Good: 명확한 에러 코드와 메시지
throw new BusinessException(
    ErrorCode.RESERVATION_TIME_CONFLICT,
    "이미 예약된 시간입니다"
);

// Bad: 일반 예외
throw new RuntimeException("에러 발생");
```

#### 엔티티 미존재
```java
// Good: EntityNotFoundException 사용
User user = userRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException(
            ErrorCode.USER_NOT_FOUND,
            "사용자를 찾을 수 없습니다: " + id
        ));

// Bad: null 체크
User user = userRepository.findById(id).orElse(null);
if (user == null) {
    throw new RuntimeException("사용자 없음");
}
```

## Git 워크플로우

### 1. 브랜치 전략

```
master (main)
  ↓
feature/기능명
  ↓
Pull Request
  ↓
master (merge)
```

### 2. 브랜치 명명 규칙

- **feature/**: 새 기능 개발
  - 예: `feature/add-payment`
- **fix/**: 버그 수정
  - 예: `fix/reservation-time-conflict`
- **refactor/**: 리팩토링
  - 예: `refactor/user-service`
- **docs/**: 문서 수정
  - 예: `docs/update-readme`

### 3. 커밋 메시지 규칙

```
<type>: <subject>

<body> (선택)
```

**Type**:
- `feat`: 새 기능
- `fix`: 버그 수정
- `refactor`: 리팩토링
- `docs`: 문서 수정
- `style`: 코드 포맷팅
- `test`: 테스트 추가/수정
- `chore`: 빌드, 설정 수정

**예시**:
```
feat: 예약 생성 API 추가

- POST /api/businesses/{id}/reservations 엔드포인트 구현
- 고객 자동 생성 지원
- 시간 충돌 검증 로직 추가
```

### 4. Git 명령어

#### 새 기능 개발
```bash
# 최신 코드 받기
git checkout master
git pull origin master

# 기능 브랜치 생성
git checkout -b feature/add-payment

# 작업 후 커밋
git add .
git commit -m "feat: 결제 기능 추가"

# 원격 저장소에 푸시
git push origin feature/add-payment

# GitHub에서 Pull Request 생성
```

#### 코드 리뷰 후 머지
```bash
# master로 이동
git checkout master

# 최신 코드 받기
git pull origin master

# 기능 브랜치 삭제 (로컬)
git branch -d feature/add-payment

# 기능 브랜치 삭제 (원격)
git push origin --delete feature/add-payment
```

## 새 도메인 추가

새로운 도메인을 추가하는 방법은 [도메인 개발 패턴](../02_domain/development-pattern.md) 문서를 참고하세요.

### 간단 요약

1. **데이터베이스 테이블 생성** (`schema.sql`)
2. **Entity 작성** (`{Domain}.java`)
3. **DTO 작성** (`dto/`)
4. **Repository 작성** (`repository/`)
5. **MyBatis XML 작성** (`mapper/`)
6. **Service 작성** (`service/`)
7. **Controller 작성** (`controller/`)
8. **ErrorCode 추가** (`ErrorCode.java`)
9. **테스트 및 커밋**

## 디버깅

### 1. 로그 레벨 설정

`application.yml`:
```yaml
logging:
  level:
    io.moer.booking: DEBUG
    org.springframework.web: INFO
    org.mybatis: DEBUG
```

### 2. MyBatis SQL 로깅

`application.yml`:
```yaml
mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 3. 디버거 사용

IntelliJ IDEA:
- 중단점 설정: 줄 번호 옆 클릭
- 디버그 모드 실행: `Shift + F9`
- 단계별 실행: `F8` (Step Over), `F7` (Step Into)

## 문제 해결

### PostgreSQL 연결 실패

```
Error: could not connect to server
```

**해결**:
```bash
# Docker 컨테이너 상태 확인
docker ps

# 컨테이너 재시작
docker-compose restart postgres
```

### Lombok 미작동

```
Error: cannot find symbol @Getter
```

**해결**:
1. Lombok 플러그인 설치 확인
2. Annotation Processing 활성화 확인
3. Gradle 의존성 확인
4. IntelliJ IDEA 재시작

### 포트 충돌

```
Error: Port 8080 is already in use
```

**해결**:
```bash
# 8080 포트 사용 프로세스 확인 (Windows)
netstat -ano | findstr :8080

# 프로세스 종료
taskkill /PID {PID} /F
```

또는 `application.yml`에서 포트 변경:
```yaml
server:
  port: 8081
```

## 관련 문서

- [도메인 개발 패턴](../02_domain/development-pattern.md)
- [아키텍처](../01_architecture/README.md)
- [API 문서](../04_api/README.md)
