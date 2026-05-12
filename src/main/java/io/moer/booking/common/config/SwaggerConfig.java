package io.moer.booking.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // Security Scheme 정의 (JWT)
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        // Security Requirement 정의
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme))
                .addSecurityItem(securityRequirement);
    }

    private Info apiInfo() {
        // SECURITY (P0-6): 운영 노출 시 정보 유출 방지를 위해 테스트 계정/내부 가이드 문구 제거.
        return new Info()
                .title("moer 예약 시스템 API")
                .description("""
                        ## moer 예약 관리 시스템 REST API 문서

                        ### 인증 방법
                        1. POST /api/auth/login 으로 로그인
                        2. 응답에서 accessToken 복사
                        3. 우측 상단 [Authorize] 버튼 클릭
                        4. "Bearer {accessToken}" 형식으로 입력
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("moer")
                        .email("support@moer.io")
                        .url("https://moer.io"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0.html"));
    }

    private List<Server> serverList() {
        return List.of(
                new Server()
                        .url("http://localhost:8080")
                        .description("로컬 개발 서버"),
                new Server()
                        .url("https://api.moer.io")
                        .description("운영 서버 (예정)")
        );
    }
}