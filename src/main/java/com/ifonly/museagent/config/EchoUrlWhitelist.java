package com.ifonly.museagent.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Echo URL 화이트리스트 검증 — Phase 5b 의 알려진 한계 해소.
 *
 * <p>Phase 5b 의 continuation token 자동 교환 흐름은 popup 이 동봉한 {@code echoUrl} 를 *그대로* 신뢰해 호출했음. 정교한
 * phishing 시나리오에선 가짜 popup 이 가짜 echoUrl 을 동봉할 수 있고, 사용자는 *muse-agent 가 그 echoUrl 의 OAuth credential
 * 을 정상으로 인식하면* 가짜 echo 가 발급한 client_secret 을 secure-config 에 저장하게 됨.
 *
 * <p>본 화이트리스트는 muse-agent 가 *어떤 echo origin* 을 신뢰할지 명시적으로 정의. 환경변수 {@code ALLOWED_ECHO_ORIGINS} 또는
 * {@code app.echo-note.allowed-echo-origins} 로 설정 가능 (콤마 구분). 기본값엔 production echo + 개발용 localhost
 * 포함.
 *
 * <p>화이트리스트 외 origin 은 {@link #validateEchoUrl(String)} 에서 거부.
 *
 * @author if-only
 * @version 0.1.0
 */
@Component
@Slf4j
public class EchoUrlWhitelist {

  private final Set<String> allowedOrigins;

  public EchoUrlWhitelist(
      @Value(
              "${app.echo-note.allowed-echo-origins:${ALLOWED_ECHO_ORIGINS:https://echo-server.omnibuscode.com,https://echo-server-439906149104.asia-northeast3.run.app,http://localhost:8080,http://localhost:8081}}")
          String origins) {
    this.allowedOrigins = parseOrigins(origins);
    log.info(
        "EchoUrlWhitelist initialized with {} origins: {}",
        allowedOrigins.size(),
        String.join(", ", allowedOrigins));
  }

  /**
   * 입력 echoUrl 이 화이트리스트의 어느 origin 에 해당하는지 검증.
   *
   * @param echoUrl 검증 대상 URL (예: {@code https://echo-server.omnibuscode.com})
   * @throws IllegalArgumentException URL 형식 오류 또는 화이트리스트 외 origin
   */
  public void validateEchoUrl(String echoUrl) {
    if (!StringUtils.hasText(echoUrl)) {
      throw new IllegalArgumentException("echoUrl 이 비어있어요.");
    }
    String origin = extractOrigin(echoUrl);
    if (!allowedOrigins.contains(origin)) {
      log.warn("Echo URL whitelist rejected: origin={}, allowed={}", origin, allowedOrigins);
      throw new IllegalArgumentException(
          "이 echoUrl 은 신뢰 목록에 없어요: " + origin + ". 운영 환경 설정을 확인해주세요.");
    }
  }

  /**
   * 검증 + 정규화된 base URL 반환 (path 제거, trailing slash 정리). secure-config 저장 시 일관된 형태로 보관하도록.
   *
   * @return scheme://host[:port] 형태의 정규화 URL
   */
  public String validateAndNormalize(String echoUrl) {
    validateEchoUrl(echoUrl);
    return extractOrigin(echoUrl);
  }

  /** 테스트용 — 현재 화이트리스트 노출 (방어적 복사). */
  public Set<String> getAllowedOrigins() {
    return new LinkedHashSet<>(allowedOrigins);
  }

  private Set<String> parseOrigins(String raw) {
    if (!StringUtils.hasText(raw)) {
      return Set.of();
    }
    Set<String> set = new LinkedHashSet<>();
    Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(StringUtils::hasText)
        .map(this::extractOrigin)
        .forEach(set::add);
    return set;
  }

  /**
   * URL 에서 scheme://host[:port] 부분만 추출. URL parse 실패 시 IllegalArgumentException.
   *
   * <p>표준 포트 (http:80, https:443) 는 생략 형태로 정규화 (e.g. {@code https://example.com:443} → {@code
   * https://example.com}).
   */
  private String extractOrigin(String url) {
    try {
      URI uri = new URI(url.trim());
      String scheme = uri.getScheme();
      String host = uri.getHost();
      int port = uri.getPort();
      if (scheme == null || host == null) {
        throw new IllegalArgumentException("URL 형식이 올바르지 않아요: " + url);
      }
      boolean defaultPort =
          port == -1
              || ("http".equals(scheme) && port == 80)
              || ("https".equals(scheme) && port == 443);
      return defaultPort ? scheme + "://" + host : scheme + "://" + host + ":" + port;
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("URL 파싱 실패: " + url, e);
    }
  }
}
