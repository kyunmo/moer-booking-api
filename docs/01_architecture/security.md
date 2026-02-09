# 보안 구조

moer 예약 시스템의 보안 및 인증 구조를 설명합니다.

## 개요

- **인증 방식**: JWT (JSON Web Token) 기반 Stateless 인증
- **토큰 종류**: Access Token (1시간) + Refresh Token (7일)
- **비밀번호 암호화**: BCrypt
- **보안 프레임워크**: Spring Security 7

## 인증 흐름

### 1. 전체 흐름도

```
┌──────────┐                  ┌──────────────┐
│  Client  │                  │  API Server  │
└────┬─────┘                  └──────┬───────┘
     │                               │
     │  1. POST /api/auth/login     │
     │  { email, password }          │
     │─────────────────────────────→ │
     │                               │ 2. 사용자 검증
     │                               │ 3. JWT 생성
     │                               │
     │  4. Access Token +            │
     │     Refresh Token 반환        │
     │←───────────────────────────── │
     │                               │
     │  5. API 요청                  │
     │  Header: Authorization:       │
     │          Bearer {accessToken} │
     │─────────────────────────────→ │
     │                               │ 6. JwtAuthenticationFilter
     │                               │    - 토큰 추출
     │                               │    - 토큰 검증
     │                               │    - 사용자 로딩
     │                               │    - SecurityContext 저장
     │                               │
     │  7. API 응답                  │
     │←───────────────────────────── │
     │                               │
     │  8. Access Token 만료 시      │
     │  POST /api/auth/refresh       │
     │  { refreshToken }             │
     │─────────────────────────────→ │
     │                               │ 9. Refresh Token 검증
     │                               │ 10. 새 Access Token 발급
     │                               │
     │  11. 새 Access Token 반환     │
     │←───────────────────────────── │
     │                               │
```

### 2. 로그인 흐름

```java
// 1. Client Request
POST /api/auth/login
{
  "email": "user@example.com",
  "password": "password123"
}

// 2. AuthController
@PostMapping("/login")
public ResponseEntity<ApiResponse<LoginResponse>> login(
        @Valid @RequestBody LoginRequest request) {
    LoginResponse response = authService.login(request);
    return ResponseEntity.ok(ApiResponse.success(response));
}

// 3. AuthService
@Transactional
public LoginResponse login(LoginRequest request) {
    // 3-1. 사용자 조회
    User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new BusinessException(
                ErrorCode.INVALID_CREDENTIALS,
                "아이디 또는 비밀번호가 올바르지 않습니다"
            ));

    // 3-2. 비밀번호 검증
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        throw new BusinessException(
            ErrorCode.INVALID_CREDENTIALS,
            "아이디 또는 비밀번호가 올바르지 않습니다"
        );
    }

    // 3-3. JWT 토큰 생성
    String accessToken = jwtTokenProvider.generateAccessToken(user);
    String refreshToken = jwtTokenProvider.generateRefreshToken(user);

    // 3-4. Refresh Token 저장
    refreshTokenRepository.save(RefreshToken.builder()
            .userId(user.getId())
            .token(refreshToken)
            .expiresAt(LocalDateTime.now().plusDays(7))
            .build());

    // 3-5. 마지막 로그인 시간 업데이트
    user.updateLastLoginAt();
    userRepository.update(user);

    return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(3600)  // 1시간
            .user(UserResponse.from(user))
            .build();
}

// 4. Client Response
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": { ... }
  }
}
```

### 3. API 요청 인증 흐름

```java
// 1. Client Request
GET /api/users/123
Header: Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

// 2. JwtAuthenticationFilter (자동 실행)
@Override
protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

    // 2-1. Authorization 헤더에서 JWT 추출
    String jwt = getJwtFromRequest(request);  // "Bearer {token}" → "{token}"

    // 2-2. JWT 검증
    if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
        // 2-3. JWT에서 userId 추출
        Long userId = tokenProvider.getUserIdFromToken(jwt);

        // 2-4. 사용자 정보 로딩
        UserDetails userDetails = userDetailsService.loadUserById(userId);

        // 2-5. 인증 객체 생성
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,  // Principal
                        null,         // Credentials
                        userDetails.getAuthorities()  // Authorities
                );

        // 2-6. SecurityContext에 인증 정보 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // 2-7. 다음 필터로 진행
    filterChain.doFilter(request, response);
}

// 3. Controller (인증된 사용자 정보 접근)
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<UserResponse>> getUser(
        @PathVariable Long id,
        @AuthenticationPrincipal CustomUserDetails currentUser) {  // ← 인증된 사용자

    UserResponse response = userService.getUser(id, currentUser.getUser());
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

## JWT 구조

### 1. JwtTokenProvider

JWT 생성 및 파싱을 담당합니다.

**위치**: `src/main/java/io/moer/booking/common/security/JwtTokenProvider.java`

```java
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    /**
     * Access Token 생성
     * - 유효기간: 1시간
     * - Payload: userId, email, role, businessId, staffId
     */
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        JwtBuilder builder = Jwts.builder()
                .subject(user.getEmail())                    // Subject: 이메일
                .claim("userId", user.getId())               // Claim: 사용자 ID
                .claim("role", user.getRole().name())        // Claim: 역할
                .issuedAt(now)                               // 발급 시간
                .expiration(expiryDate)                      // 만료 시간
                .signWith(getSigningKey());                  // 서명

        // STAFF인 경우 staffId 추가
        if (user.getStaffId() != null) {
            builder.claim("staffId", user.getStaffId());
        }

        // businessId 추가
        if (user.getBusinessId() != null) {
            builder.claim("businessId", user.getBusinessId());
        }

        return builder.compact();
    }

    /**
     * Refresh Token 생성
     * - 유효기간: 7일
     * - Payload: userId, email만 포함 (최소 정보)
     */
    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 토큰 검증
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature");
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException e) {
            log.error("JWT token compact of handler are invalid");
        }
        return false;
    }

    /**
     * 토큰에서 Claims 추출
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

### 2. JWT Payload 구조

#### Access Token
```json
{
  "sub": "user@example.com",      // Subject: 이메일
  "userId": 123,                   // 사용자 ID
  "role": "OWNER",                 // 역할 (ADMIN/OWNER/STAFF)
  "businessId": 456,               // 매장 ID (OWNER/STAFF만)
  "staffId": 789,                  // 직원 ID (STAFF만)
  "iat": 1609459200,               // 발급 시간
  "exp": 1609462800                // 만료 시간 (1시간 후)
}
```

#### Refresh Token
```json
{
  "sub": "user@example.com",      // Subject: 이메일
  "userId": 123,                   // 사용자 ID
  "iat": 1609459200,               // 발급 시간
  "exp": 1610064000                // 만료 시간 (7일 후)
}
```

### 3. JWT 설정 (application.yml)

```yaml
jwt:
  secret: your-secret-key-min-32-chars-for-hs256
  access-token-expiration: 3600000    # 1시간 (milliseconds)
  refresh-token-expiration: 604800000 # 7일 (milliseconds)
```

## Spring Security 설정

### 1. SecurityConfig

**위치**: `src/main/java/io/moer/booking/common/security/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenProvider tokenProvider;

    /**
     * 비밀번호 암호화 (BCrypt)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * JWT 필터
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenProvider, userDetailsService);
    }

    /**
     * CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",   // React 개발 서버
            "http://localhost:5173"    // Vite 개발 서버
        ));
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Security FilterChain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (JWT 사용)
            .csrf(AbstractHttpConfigurer::disable)

            // CORS 설정
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Stateless 세션 관리
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // URL별 인증 설정
            .authorizeHttpRequests(auth -> auth
                // Public 엔드포인트 (인증 불필요)
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/error").permitAll()

                // Swagger/API 문서 (개발 환경)
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()

                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )

            // JWT 필터 추가 (UsernamePasswordAuthenticationFilter 앞에)
            .addFilterBefore(jwtAuthenticationFilter(),
                             UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

### 2. 엔드포인트별 접근 권한

| 엔드포인트 | 권한 | 설명 |
|-----------|------|------|
| `POST /api/auth/login` | Public | 로그인 |
| `POST /api/auth/register` | Public | 회원가입 |
| `POST /api/auth/refresh` | Public | 토큰 갱신 |
| `POST /api/users` | Public | 사용자 생성 (회원가입) |
| `GET /api/health` | Public | 헬스 체크 |
| `/swagger-ui/**` | Public | Swagger UI |
| **나머지 모든 API** | Authenticated | JWT 인증 필요 |

## 권한 제어

### 1. 역할 기반 제어 (Role-based)

```java
public enum UserRole {
    ADMIN,   // 시스템 관리자
    OWNER,   // 매장 소유주
    STAFF    // 직원
}
```

### 2. 비즈니스 레벨 권한 체크

Entity에서 권한 검증 메서드를 제공합니다.

```java
@Entity
@Table(name = "users")
public class User {
    private Long id;
    private UserRole role;
    private Long businessId;
    private Long staffId;

    /**
     * 매장 접근 권한 체크
     */
    public boolean canAccessBusiness(Long targetBusinessId) {
        if (this.role == UserRole.ADMIN) {
            return true;  // ADMIN은 모든 매장 접근 가능
        }
        return Objects.equals(this.businessId, targetBusinessId);
    }

    /**
     * 직원 접근 권한 체크
     */
    public boolean canAccessStaff(Long targetStaffId) {
        if (this.role == UserRole.ADMIN) {
            return true;  // ADMIN은 모든 직원 접근 가능
        }
        if (this.role == UserRole.STAFF) {
            return Objects.equals(this.staffId, targetStaffId);  // 본인만 접근
        }
        return false;
    }

    /**
     * 사용자 접근 권한 체크
     */
    public boolean canAccessUser(User targetUser) {
        if (this.role == UserRole.ADMIN) {
            return true;  // ADMIN은 모든 사용자 접근 가능
        }
        if (Objects.equals(this.id, targetUser.getId())) {
            return true;  // 본인 접근 가능
        }
        // 같은 매장의 사용자 접근 가능
        return Objects.equals(this.businessId, targetUser.getBusinessId());
    }
}
```

### 3. Service에서 권한 체크 예시

```java
@Service
@RequiredArgsConstructor
public class BusinessService {

    private final BusinessRepository businessRepository;

    /**
     * 매장 조회 (권한 체크 포함)
     */
    @Transactional(readOnly = true)
    public BusinessResponse getBusiness(Long id, User currentUser) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                    ErrorCode.BUSINESS_NOT_FOUND,
                    "매장을 찾을 수 없습니다"
                ));

        // 권한 체크
        if (!currentUser.canAccessBusiness(business.getId())) {
            throw new BusinessException(
                ErrorCode.BUSINESS_ACCESS_DENIED,
                "해당 매장에 접근 권한이 없습니다"
            );
        }

        return BusinessResponse.from(business);
    }
}
```

## Refresh Token 관리

### 1. RefreshToken Entity

```java
@Entity
@Table(name = "refresh_tokens")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
```

### 2. 토큰 갱신 흐름

```java
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Access Token 갱신
     */
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        // 1. Refresh Token 검증
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new BusinessException(
                ErrorCode.INVALID_TOKEN,
                "유효하지 않은 Refresh Token입니다"
            );
        }

        // 2. DB에서 Refresh Token 조회
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new EntityNotFoundException(
                    ErrorCode.TOKEN_NOT_FOUND,
                    "Refresh Token을 찾을 수 없습니다"
                ));

        // 3. 만료 여부 확인
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.deleteById(refreshToken.getId());
            throw new BusinessException(
                ErrorCode.EXPIRED_TOKEN,
                "만료된 Refresh Token입니다"
            );
        }

        // 4. 사용자 조회
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                    ErrorCode.USER_NOT_FOUND,
                    "사용자를 찾을 수 없습니다"
                ));

        // 5. 새 Access Token 발급
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())  // 기존 Refresh Token 재사용
                .tokenType("Bearer")
                .expiresIn(3600)
                .user(UserResponse.from(user))
                .build();
    }

    /**
     * 로그아웃 (Refresh Token 삭제)
     */
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> refreshTokenRepository.deleteById(token.getId()));
    }
}
```

## 비밀번호 암호화

### 1. BCrypt 사용

```java
@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 2. 암호화/검증 예시

```java
@Service
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 시 비밀번호 암호화
     */
    public void register(RegisterRequest request) {
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)  // 암호화된 비밀번호 저장
                .build();

        userRepository.save(user);
    }

    /**
     * 로그인 시 비밀번호 검증
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "아이디 또는 비밀번호가 올바르지 않습니다"
                ));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(
                ErrorCode.INVALID_CREDENTIALS,
                "아이디 또는 비밀번호가 올바르지 않습니다"
            );
        }

        // JWT 생성 및 반환
        // ...
    }
}
```

## 보안 Best Practices

### 1. 토큰 관리
- ✅ Access Token은 짧은 유효기간 (1시간)
- ✅ Refresh Token은 DB에 저장하여 관리
- ✅ 로그아웃 시 Refresh Token 삭제
- ✅ Refresh Token은 한 번만 사용 (선택적)

### 2. 비밀번호 관리
- ✅ BCrypt로 암호화 저장
- ✅ 평문 비밀번호는 절대 저장하지 않음
- ✅ 비밀번호 검증 실패 시 구체적인 이유 노출 금지

### 3. API 보안
- ✅ HTTPS 사용 (프로덕션)
- ✅ CORS 설정으로 허용된 Origin만 접근
- ✅ Rate Limiting (추가 고려)
- ✅ SQL Injection 방지 (MyBatis PreparedStatement 사용)

### 4. 예외 처리
- ✅ 인증 실패 시 구체적인 실패 이유 노출 금지
- ✅ 일반화된 에러 메시지 반환
- ❌ "이메일이 존재하지 않습니다" (계정 존재 여부 노출)
- ✅ "아이디 또는 비밀번호가 올바르지 않습니다" (일반화)

## 다음 문서

- [예외 처리](./exception-handling.md)
