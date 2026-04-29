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

  /**
   * 본 muse 빌드가 인지하고 있는 echo external API 의 최신 버전.
   *
   * <p>{@link ApiProperties#externalApiVersion} 가 본 값과 다르면 부팅 시 WARN — yaml 이 stale 한 상태로 운영 중일
   * 가능성을 알린다 (예: muse 새 버전을 배포해 latest 가 {@code v1} 인데 application.yml 은 여전히 {@code unversioned}
   * 인 경우).
   *
   * <p>echo-server 가 새 버전을 도입할 때마다 본 상수를 함께 갱신.
   */
  public static final String LATEST_KNOWN_EXTERNAL_API_VERSION = "unversioned";

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

    /** Echo Note AI 프리뷰 생성 엔드포인트 */
    private String echoNotePreview;

    /** Echo Note 발송 엔드포인트 */
    private String echoNoteSend;

    /** Echo Note continuation token 교환 엔드포인트 (popup 진입 시 자격증명 부트스트랩) */
    private String echoNoteExchangeContinuation;

    /**
     * muse 가 호출 대상으로 기대하는 echo external API 버전.
     *
     * <p>현재 echo-server external API 는 미버전 ({@code /api/external/echo-note/...}). echo-server 가 {@code
     * /v1/} prefix 를 도입하면 본 값을 {@code v1} 로, 위 echo-note 경로들을 {@code
     * /api/external/v1/echo-note/...} 로 갱신 후 배포. 부팅 시 INFO 로그로 노출되어 운영자가 호환 상태를 확인할 수 있게 한다.
     */
    private String externalApiVersion = "unversioned";
  }
}
