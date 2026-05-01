package com.ifonly.museagent.service;

import com.ifonly.museagent.client.EchoErrorMapper;
import com.ifonly.museagent.config.EchoServerProperties;
import com.ifonly.museagent.config.EchoUrlWhitelist;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Echo Note Privacy Phase 5b — echo-server 의 continuation token 을 OAuth client credential 로 교환.
 *
 * <p>이 service 는 의도적으로 기존 {@link com.ifonly.museagent.client.EchoServerClient} 의 OAuth-aware
 * WebClient 를 사용하지 않는다. 이유:
 *
 * <ul>
 *   <li>exchange endpoint 는 echo 측 SecurityConfig 의 permitAll 에 포함됨 (인증 불요)
 *   <li>호출 시점에 muse-agent 가 *아직 진짜 자격증명을 모를 수 있음* (신규 사용자) — OAuth filter 가 token 발급 시도 시 실패 가능
 *   <li>echo URL 이 동적 (사용자 popup 진입 시점에만 알 수 있음, 운영/개발 환경별로 다름) — 부팅 시 고정된 EchoServerClient 와 부적합
 * </ul>
 *
 * <p>plain {@link RestTemplate} 으로 단순 POST. 결과 자격증명을 호출자가 받아 {@link SecureConfigService} 에 저장.
 *
 * @author if-only
 * @version 0.1.0
 */
@Service
@Slf4j
public class EchoContinuationExchangeService {

  private static final String DEFAULT_EXCHANGE_PATH =
      "/api/external/echo-note/exchange-continuation-token";

  private final RestTemplate restTemplate;
  private final EchoUrlWhitelist echoUrlWhitelist;
  private final EchoServerProperties echoServerProperties;
  private final EchoErrorMapper echoErrorMapper;

  public EchoContinuationExchangeService(
      EchoUrlWhitelist echoUrlWhitelist,
      EchoServerProperties echoServerProperties,
      EchoErrorMapper echoErrorMapper) {
    this.restTemplate = new RestTemplate();
    this.echoUrlWhitelist = echoUrlWhitelist;
    this.echoServerProperties = echoServerProperties;
    this.echoErrorMapper = echoErrorMapper;
  }

  /**
   * @param echoUrl echo-server base URL (popup 이 전달, 예: {@code
   *     https://echo-server.omnibuscode.com})
   * @param token raw continuation token
   * @return 교환 결과 ({@code clientId, clientSecret, userEmail}). echo 측이 거부한 경우 (만료/사용됨/무효) 예외
   * @throws IllegalStateException 응답이 비정상 (HTTP 4xx/5xx, 응답 본문 누락)
   */
  @SuppressWarnings("unchecked")
  public ContinuationCredential exchange(String echoUrl, String token) {
    if (echoUrl == null || echoUrl.isBlank()) {
      throw new IllegalArgumentException("echoUrl must not be blank");
    }
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("token must not be blank");
    }
    // v1.7.0: 화이트리스트 검증 — 가짜 popup 이 동봉한 가짜 echoUrl 로 자격증명을 끌어오는 phishing 차단.
    String trimmedUrl = echoUrlWhitelist.validateAndNormalize(echoUrl);
    String exchangePath =
        echoServerProperties.getApi() == null
                || echoServerProperties.getApi().getEchoNoteExchangeContinuation() == null
                || echoServerProperties.getApi().getEchoNoteExchangeContinuation().isBlank()
            ? DEFAULT_EXCHANGE_PATH
            : echoServerProperties.getApi().getEchoNoteExchangeContinuation();
    String endpoint = trimmedUrl + exchangePath;

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    Map<String, String> body = Map.of("token", token);

    try {
      ResponseEntity<Map> response =
          restTemplate.exchange(
              endpoint, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        throw new IllegalStateException("popup 링크 처리 중 echo 응답이 비정상이에요. 잠시 후 다시 시도해주세요.");
      }

      Map<String, Object> data = new HashMap<>(response.getBody());
      String clientId = String.valueOf(data.get("clientId"));
      String clientSecret = String.valueOf(data.get("clientSecret"));
      String userEmail = String.valueOf(data.get("userEmail"));

      if (clientId == null
          || clientId.isBlank()
          || "null".equals(clientId)
          || clientSecret == null
          || clientSecret.isBlank()
          || "null".equals(clientSecret)) {
        throw new IllegalStateException("echo 응답에 자격증명이 누락됐어요. echo-config 페이지에서 직접 설정해주세요.");
      }

      log.info(
          "Continuation token exchanged successfully: userEmail={}, clientId={}",
          userEmail,
          clientId);
      return new ContinuationCredential(clientId, clientSecret, userEmail);
    } catch (HttpStatusCodeException e) {
      String errBody = e.getResponseBodyAsString();
      String code = echoErrorMapper.extractErrorCode(errBody);
      String userMsg = echoErrorMapper.mapContinuationError(e.getStatusCode().value(), code);
      log.error(
          "Continuation token exchange rejected: endpoint={}, status={}, code={}",
          endpoint,
          e.getStatusCode().value(),
          code);
      throw new IllegalStateException(userMsg, e);
    } catch (RestClientException e) {
      log.error(
          "Continuation token exchange transport failure: endpoint={}, reason={}",
          endpoint,
          e.getMessage());
      throw new IllegalStateException("echo 와 통신할 수 없어요. 네트워크 또는 echo 상태를 확인해주세요.", e);
    } catch (RuntimeException e) {
      log.error(
          "Continuation token exchange failed: endpoint={}, reason={}", endpoint, e.getMessage());
      throw e;
    }
  }

  /** 교환 결과 페이로드. */
  public record ContinuationCredential(String clientId, String clientSecret, String userEmail) {}
}
