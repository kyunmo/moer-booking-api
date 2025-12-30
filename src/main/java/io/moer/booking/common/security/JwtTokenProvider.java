package io.moer.booking.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.moer.booking.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Access Token 생성
     */
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        JwtBuilder builder = Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey());

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
     * 토큰에서 이메일 추출
     */
    public String getEmailFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject();
    }

    /**
     * 토큰에서 userId 추출
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 토큰에서 role 추출
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * 토큰에서 staffId 추출 (STAFF인 경우)
     */
    public Long getStaffIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("staffId", Long.class);
    }

    /**
     * 토큰에서 businessId 추출
     */
    public Long getBusinessIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("businessId", Long.class);
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT token compact of handler are invalid: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Claims 파싱
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}