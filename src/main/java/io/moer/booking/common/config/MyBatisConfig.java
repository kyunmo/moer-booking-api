package io.moer.booking.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "io.moer.booking.domain.**.repository")
public class MyBatisConfig {
}
