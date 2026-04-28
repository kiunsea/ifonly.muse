package com.ifonly.museagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Echo Server 설정 정보 클래스
 *
 * @author if-only
 * @version 0.4.0
 */
@Component
@ConfigurationProperties(prefix = "app.echo-server")
@Data
public class EchoServerProperties {

  /** Echo Server URL */
  private String url;

  /** OAuth2 클라이언트 ID */
  private String clientId;

  /** OAuth2 클라이언트 시크릿 */
  private String clientSecret;

  /** API 엔드포인트 정보 */
  private ApiProperties api;

  /** API 엔드포인트 설정 */
  @Data
  public static class ApiProperties {

    /** 장비 등록 엔드포인트 */
    private String deviceRegistration;

    /** Alive 상태 조회 엔드포인트 */
    private String aliveStatus;

    /** Alive 이력 조회 엔드포인트 */
    private String aliveHistory;
  }
}
