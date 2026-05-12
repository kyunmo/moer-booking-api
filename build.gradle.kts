plugins {
    java
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"

    // SECURITY (P2-7): 의존성 취약점 스캔 (OWASP Dependency-Check).
    // 사용법: ./gradlew dependencyCheckAnalyze
    // CI/CD 또는 주기적 수동 실행. 빌드 시 자동 실행은 NVD 다운로드 시간 때문에 비활성.
    id("org.owasp.dependencycheck") version "11.1.1"
}

group = "io.moer"
version = "0.0.1-SNAPSHOT"
description = "moer-booking"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {

    // spring boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // OAuth2 Client (SNS 로그인)
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // Email & Template
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // MyBatis
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:4.0.0")

    // jackson — Spring Boot BOM 관리 버전 사용 (4.0.1 -> jackson 2.18.x).
    // 주의: jackson-databind 는 과거 CVE 가 잦았던 라이브러리. dependencyCheck 로 주기 점검.
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // PostgreSQL — Spring Boot BOM 관리 버전 사용 (4.0.1 -> 42.7.x).
    implementation("org.postgresql:postgresql")

    // Swagger/OpenAPI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    // SECURITY (P1-5): OWASP Java HTML Sanitizer — XSS 방어용 텍스트 정화
    implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20240325.1")

    // SECURITY (P1-3): Bucket4j — Rate Limiting (인메모리 토큰 버킷)
    implementation("com.bucket4j:bucket4j_jdk17-core:8.14.0")

    // lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mybatis.spring.boot:mybatis-spring-boot-starter-test:4.0.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// SECURITY (P2-7): OWASP Dependency-Check 설정.
// - High 이상 취약점 발견 시 빌드 실패하도록 임계값 설정.
// - HTML 리포트는 build/reports/dependency-check-report.html 생성.
// - NVD API 키 사용 시: -Dnvd.api.key=... 로 전달 (rate limit 완화).
dependencyCheck {
    failBuildOnCVSS = 7.0f // CVSS 7.0 이상(High/Critical) 시 실패
    suppressionFile = "config/dependency-check-suppressions.xml" // 향후 false-positive 억제용
    formats = listOf("HTML", "JSON")
    skipConfigurations = listOf("testImplementation", "testRuntimeOnly")
}
