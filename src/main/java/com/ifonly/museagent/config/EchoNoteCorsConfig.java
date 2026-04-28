package com.ifonly.museagent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Echo Note health endpoint 전용 CORS 설정 — Phase 4.
 *
 * <p>echo-server 의 echo-note popup 이 사용자 브라우저에서 muse-agent 의 {@code /api/echo-note/health} 를 호출하려면
 * CORS 허용이 필요. 단, 모든 origin 에 허용하면 *임의 사이트가 사용자 PC 의 muse-agent 존재 여부를 fingerprint* 할 수 있어, echo
 * origin 으로만 화이트리스트 한정.
 *
 * <p>설정: {@code app.cors.echo-origins} (기본값 production echo + 개발 localhost). 콤마 구분 다중 origin 지원.
 * 환경변수 {@code ECHO_CORS_ORIGINS} 로도 오버라이드 가능.
 *
 * @author if-only
 * @version 0.1.0
 */
@Configuration
@Slf4j
public class EchoNoteCorsConfig implements WebMvcConfigurer {

  @Value(
      "${app.cors.echo-origins:https://echo-server.omnibuscode.com,http://localhost:8080,http://localhost:8081}")
  private String echoOrigins;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    String[] origins = echoOrigins.split(",");
    for (int i = 0; i < origins.length; i++) {
      origins[i] = origins[i].trim();
    }
    registry
        .addMapping("/api/echo-note/health")
        .allowedOrigins(origins)
        .allowedMethods("GET", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false)
        .maxAge(3600);
    log.info("Echo Note CORS configured: origins={}", String.join(", ", origins));
  }
}
