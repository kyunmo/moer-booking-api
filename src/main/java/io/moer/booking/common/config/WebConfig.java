package io.moer.booking.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * SECURITY (P3-3): CORS 정의는 {@link SecurityConfig#corsConfigurationSource()} 한 곳에서만 관리.
 * 이전에는 본 클래스의 {@code addCorsMappings} 와 중복 정의되어 있어 변경 시 추적이 어려웠음.
 * 이제 본 클래스는 정적 리소스 핸들러(/uploads/**) 만 담당.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.storage.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absolutePath);
    }
}