# 06. 배포 가이드

프로덕션 환경 배포 가이드입니다.

## 목차

1. [빌드](#빌드)
2. [환경 변수](#환경-변수)
3. [Docker 배포](#docker-배포)
4. [프로덕션 설정](#프로덕션-설정)

## 빌드

### Gradle 빌드

```bash
# 테스트 포함 빌드
./gradlew clean build

# 테스트 제외 빌드
./gradlew clean build -x test

# 빌드 결과물 위치
# build/libs/moer-booking-0.0.1-SNAPSHOT.jar
```

### JAR 파일 실행

```bash
java -jar build/libs/moer-booking-0.0.1-SNAPSHOT.jar
```

## 환경 변수

### 필수 환경 변수

프로덕션 환경에서는 다음 환경 변수를 설정해야 합니다:

```bash
# 데이터베이스
export DB_URL=jdbc:postgresql://localhost:5432/moer_prod
export DB_USERNAME=moer
export DB_PASSWORD=your-secure-password

# JWT
export JWT_SECRET=your-secret-key-min-32-chars-for-hs256-production
export JWT_ACCESS_TOKEN_EXPIRATION=3600000    # 1시간
export JWT_REFRESH_TOKEN_EXPIRATION=604800000 # 7일

# 서버
export SERVER_PORT=8080

# 로그
export LOGGING_LEVEL_ROOT=INFO
export LOGGING_LEVEL_APP=INFO
```

### application-prod.yml

프로덕션 환경 설정:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

  jpa:
    show-sql: false

  sql:
    init:
      mode: never  # 프로덕션에서는 스키마 자동 생성 비활성화

jwt:
  secret: ${JWT_SECRET}
  access-token-expiration: ${JWT_ACCESS_TOKEN_EXPIRATION}
  refresh-token-expiration: ${JWT_REFRESH_TOKEN_EXPIRATION}

logging:
  level:
    root: ${LOGGING_LEVEL_ROOT:INFO}
    io.moer.booking: ${LOGGING_LEVEL_APP:INFO}
  file:
    name: /var/log/moer-booking/application.log
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

server:
  port: ${SERVER_PORT:8080}
```

### 프로파일 활성화

```bash
# 프로덕션 프로파일로 실행
java -jar -Dspring.profiles.active=prod moer-booking.jar
```

## Docker 배포

### 1. Dockerfile 작성

**파일**: `Dockerfile`

```dockerfile
# 빌드 스테이지
FROM gradle:8-jdk17 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src
RUN gradle clean build -x test --no-daemon

# 실행 스테이지
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 타임존 설정
RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul

# JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1

# 실행
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
```

### 2. docker-compose.yml (프로덕션)

**파일**: `docker-compose.prod.yml`

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: moer-postgres-prod
    environment:
      POSTGRES_DB: moer_prod
      POSTGRES_USER: moer
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      TZ: Asia/Seoul
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./src/main/resources/db/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql
    ports:
      - "5432:5432"
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U moer"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: moer-app-prod
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/moer_prod
      DB_USERNAME: moer
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      SPRING_PROFILES_ACTIVE: prod
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/api/health"]
      interval: 30s
      timeout: 3s
      retries: 3

volumes:
  postgres-data:
```

### 3. Docker 빌드 및 실행

```bash
# 환경 변수 설정
export DB_PASSWORD=your-secure-password
export JWT_SECRET=your-secret-key-min-32-chars

# 빌드
docker-compose -f docker-compose.prod.yml build

# 실행
docker-compose -f docker-compose.prod.yml up -d

# 로그 확인
docker-compose -f docker-compose.prod.yml logs -f app

# 중지
docker-compose -f docker-compose.prod.yml down
```

### 4. Docker 이미지 최적화

#### 멀티 스테이지 빌드
- 빌드 스테이지: Gradle + JDK 사용
- 실행 스테이지: JRE만 사용 (이미지 크기 축소)

#### 레이어 캐싱
```dockerfile
# 의존성 먼저 복사 (캐시 활용)
COPY build.gradle.kts settings.gradle.kts ./
RUN gradle dependencies --no-daemon

# 소스 코드 나중에 복사
COPY src ./src
RUN gradle build -x test --no-daemon
```

## 프로덕션 설정

### 1. 보안 설정

#### HTTPS 활성화

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: moer
```

#### CORS 설정

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList(
        "https://yourdomain.com"  // 프로덕션 도메인만 허용
    ));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

### 2. 데이터베이스 설정

#### 커넥션 풀 최적화

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20      # 최대 연결 수
      minimum-idle: 5            # 최소 유휴 연결 수
      connection-timeout: 30000  # 연결 타임아웃 (30초)
      idle-timeout: 600000       # 유휴 타임아웃 (10분)
      max-lifetime: 1800000      # 최대 수명 (30분)
```

#### 백업 전략

```bash
# PostgreSQL 백업
docker exec moer-postgres-prod pg_dump -U moer moer_prod > backup-$(date +%Y%m%d).sql

# 복원
docker exec -i moer-postgres-prod psql -U moer moer_prod < backup-20260208.sql
```

### 3. 로깅 설정

#### 파일 로깅

```yaml
logging:
  file:
    name: /var/log/moer-booking/application.log
    max-size: 10MB
    max-history: 30  # 30일 보관
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

#### 로그 수집 (선택)

ELK Stack, CloudWatch 등 로그 수집 도구 연동.

### 4. 모니터링

#### Spring Boot Actuator

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

#### 헬스 체크 엔드포인트

```
GET /api/health
```

**Response**:
```json
{
  "status": "UP",
  "database": "UP",
  "diskSpace": "UP"
}
```

### 5. 성능 최적화

#### JVM 옵션

```bash
java -jar \
  -Xms512m \           # 초기 힙 크기
  -Xmx2g \             # 최대 힙 크기
  -XX:+UseG1GC \       # G1 가비지 컬렉터
  -XX:MaxGCPauseMillis=200 \
  -Dspring.profiles.active=prod \
  moer-booking.jar
```

## CI/CD (선택)

### GitHub Actions 예시

**파일**: `.github/workflows/deploy.yml`

```yaml
name: Deploy to Production

on:
  push:
    branches:
      - master

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build with Gradle
        run: ./gradlew clean build -x test

      - name: Build Docker image
        run: docker build -t moer-booking:latest .

      - name: Deploy to server
        # SSH, Docker Hub push 등 배포 스크립트
        run: |
          echo "Deploy to production server"
```

## 체크리스트

배포 전 확인사항:

- [ ] 환경 변수 설정 (DB, JWT 등)
- [ ] HTTPS 설정
- [ ] CORS 설정 (프로덕션 도메인만)
- [ ] 로그 레벨 설정 (INFO)
- [ ] 데이터베이스 백업
- [ ] 헬스 체크 엔드포인트 확인
- [ ] 성능 테스트
- [ ] 보안 검토

## 관련 문서

- [개발 가이드](../05_development/README.md)
- [API 문서](../04_api/README.md)
- [아키텍처](../01_architecture/README.md)
