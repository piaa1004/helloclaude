package com.example.board.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// BoardApplication에 두면 @WebMvcTest에서 "JPA metamodel must not be empty"로 실패하므로 분리한다.
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing
public class JpaAuditingConfig {
}
