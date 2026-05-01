package com.ifonly.museagent.client;

import com.ifonly.museagent.config.EchoServerProperties;
import com.ifonly.museagent.dto.AliveHistoryResponse;
import com.ifonly.museagent.dto.AliveStatusResponse;
import com.ifonly.museagent.dto.DeviceRegistrationRequest;
import com.ifonly.museagent.dto.DeviceResponse;
import com.ifonly.museagent.exception.DeviceRegistrationException;
import com.ifonly.museagent.exception.EchoServerConnectionException;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Echo Server REST Client
 *
 * <p>REST client for communication with Echo Server.
 *
 * @author if-only
 * @version 0.1.1
 */
@Component
@Slf4j
public class EchoServerClient {

  private final WebClient webClient;
  private final EchoServerProperties properties;
  private final EchoErrorMapper echoErrorMapper;

  private static final int MAX_RETRIES = 3;
  private static final long RETRY_DELAY_MS = 1000;
  private static final long TIMEOUT_SECONDS = 30;

  @Autowired
  public EchoServerClient(
      WebClient webClient, EchoServerProperties properties, EchoErrorMapper echoErrorMapper) {
    this.webClient = webClient;
    this.properties = properties;
    this.echoErrorMapper = echoErrorMapper;
  }

  @PostConstruct
  void logExpectedExternalApiVersion() {
    String configured =
        properties.getApi() == null ? null : properties.getApi().getExternalApiVersion();
    String latestKnown = EchoServerProperties.LATEST_KNOWN_EXTERNAL_API_VERSION;
    log.info(
        "Echo external API contract: muse expects version='{}' (latest known by this build: '{}'; echo-note paths preview={}, send={})",
        configured,
        latestKnown,
        properties.getApi() == null ? null : properties.getApi().getEchoNotePreview(),
        properties.getApi() == null ? null : properties.getApi().getEchoNoteSend());
    if (configured != null && !configured.equals(latestKnown)) {
      log.warn(
          "Echo external API version mismatch: application.yml declares '{}' but this muse build's latest known version is '{}'. "
              + "Update app.echo-server.api.external-api-version (and the echo-note-* paths) — or upgrade muse if you intend to track a newer echo.",
          configured,
          latestKnown);
    }
  }

  /**
   * Get alive status from Echo Server
   *
   * @return alive status
   * @throws EchoServerConnectionException on connection failure
   */
  public AliveStatusResponse getAliveStatus() {
    log.info("Querying alive status");
    validateConnectionConfig();

    try {
      return webClient
          .get()
          .uri(properties.getUrl() + properties.getApi().getAliveStatus())
          .retrieve()
          .bodyToMono(AliveStatusResponse.class)
          .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
          .onErrorMap(
              e -> new EchoServerConnectionException(toFriendlyMessage(e, "Alive 상태 조회"), e))
          .block();
    } catch (EchoServerConnectionException e) {
      log.error("Echo Server 접속 실패 (Alive 상태 조회): {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      String message = toFriendlyMessage(e, "Alive 상태 조회");
      log.error("Echo Server 접속 실패 (Alive 상태 조회): {}", message);
      throw new EchoServerConnectionException(message, e);
    }
  }

  /**
   * Get alive history from Echo Server
   *
   * @param limit query limit
   * @param offset offset
   * @return alive history
   * @throws EchoServerConnectionException on connection failure
   */
  public AliveHistoryResponse getAliveHistory(int limit, int offset) {
    log.info("Querying alive history: limit={}, offset={}", limit, offset);
    validateConnectionConfig();

    try {
      return webClient
          .get()
          .uri(
              properties.getUrl()
                  + properties.getApi().getAliveHistory()
                  + "?limit="
                  + limit
                  + "&offset="
                  + offset)
          .retrieve()
          .bodyToMono(AliveHistoryResponse.class)
          .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
          .onErrorMap(
              e -> new EchoServerConnectionException(toFriendlyMessage(e, "Alive 이력 조회"), e))
          .block();
    } catch (EchoServerConnectionException e) {
      log.error("Echo Server 접속 실패 (Alive 이력 조회): {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      String message = toFriendlyMessage(e, "Alive 이력 조회");
      log.error("Echo Server 접속 실패 (Alive 이력 조회): {}", message);
      throw new EchoServerConnectionException(message, e);
    }
  }

  /**
   * Register device with Echo Server
   *
   * @param request device registration request
   * @return device registration response
   * @throws DeviceRegistrationException on registration failure
   */
  public DeviceResponse registerDevice(DeviceRegistrationRequest request) {
    log.info(
        "Registering device with Echo Server: name={}, identifier={}",
        request.getDeviceName(),
        request.getDeviceIdentifier());
    validateConnectionConfig();

    try {
      return webClient
          .post()
          .uri(properties.getUrl() + properties.getApi().getDeviceRegistration())
          .bodyValue(request)
          .retrieve()
          .bodyToMono(DeviceResponse.class)
          .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
          .retryWhen(
              Retry.backoff(MAX_RETRIES, Duration.ofMillis(RETRY_DELAY_MS))
                  .maxBackoff(Duration.ofSeconds(10)))
          .onErrorMap(e -> new DeviceRegistrationException(toFriendlyMessage(e, "디바이스 등록"), e))
          .block();
    } catch (DeviceRegistrationException e) {
      log.error("Echo Server 접속 실패 (디바이스 등록): {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      String message = toFriendlyMessage(e, "디바이스 등록");
      log.error("Echo Server 접속 실패 (디바이스 등록): {}", message);
      throw new DeviceRegistrationException(message, e);
    }
  }

  /**
   * Unregister device from Echo Server
   *
   * @param deviceId device ID to unregister
   * @throws DeviceRegistrationException on unregistration failure
   */
  public void unregisterDevice(Long deviceId) {
    log.info("Unregistering device from Echo Server: deviceId={}", deviceId);
    validateConnectionConfig();

    try {
      webClient
          .delete()
          .uri(properties.getUrl() + properties.getApi().getDeviceRegistration() + "/" + deviceId)
          .exchangeToMono(
              response -> {
                if (response.statusCode().is2xxSuccessful()
                    || response.statusCode().value() == 404) {
                  return Mono.empty();
                }
                return response.createException().flatMap(Mono::error);
              })
          .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
          .retryWhen(
              Retry.backoff(MAX_RETRIES, Duration.ofMillis(RETRY_DELAY_MS))
                  .maxBackoff(Duration.ofSeconds(10)))
          .onErrorMap(e -> new DeviceRegistrationException(toFriendlyMessage(e, "디바이스 해제"), e))
          .block();
    } catch (DeviceRegistrationException e) {
      log.error("Echo Server 접속 실패 (디바이스 해제): {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      String message = toFriendlyMessage(e, "디바이스 해제");
      log.error("Echo Server 접속 실패 (디바이스 해제): {}", message);
      throw new DeviceRegistrationException(message, e);
    }
  }

  /**
   * Echo 의 외부 Echo Note API 로 AI 프리뷰 생성 요청.
   *
   * <p>echo-server 는 응답을 즉시 반환하고 어떤 DB 에도 저장하지 않음 (Phase 2a 의 보증).
   *
   * @param recipientEmail 수신자 이메일 (preview 자체엔 미사용, 로깅 메타용)
   * @param originalMessage 사용자 원본 메시지
   * @param locale ko/en/ja
   * @return {@code {aiGeneratedMessage: ...}}
   */
  @SuppressWarnings("unchecked")
  public Map<String, Object> generateEchoNotePreview(
      String recipientEmail, String originalMessage, String locale) {
    log.info(
        "Requesting echo-note preview from Echo Server (locale={}, msgLen={})",
        locale,
        originalMessage == null ? 0 : originalMessage.length());
    validateConnectionConfig();

    Map<String, Object> body =
        Map.of(
            "recipientEmail", recipientEmail == null ? "" : recipientEmail,
            "originalMessage", originalMessage == null ? "" : originalMessage,
            "locale", locale == null ? "ko" : locale);

    try {
      return webClient
          .post()
          .uri(properties.getUrl() + properties.getApi().getEchoNotePreview())
          .bodyValue(body)
          .retrieve()
          .bodyToMono(Map.class)
          .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
          .onErrorMap(
              e -> new EchoServerConnectionException(toFriendlyMessage(e, "Echo Note 프리뷰"), e))
          .block();
    } catch (EchoServerConnectionException e) {
      log.error("Echo Server 접속 실패 (Echo Note 프리뷰): {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      String message = toFriendlyMessage(e, "Echo Note 프리뷰");
      log.error("Echo Server 접속 실패 (Echo Note 프리뷰): {}", message);
      throw new EchoServerConnectionException(message, e);
    }
  }

  /**
   * Echo 의 외부 Echo Note API 로 가공본을 즉시 발송 요청.
   *
   * <p>echo-server 는 메일 발송 후 본문을 폐기 (EmailSendLog 메타만 기록). 응답 성공 시 발송 완료 상태.
   *
   * @param recipientEmail 수신자 이메일
   * @param aiGeneratedMessage 사용자가 확인/편집한 가공본
   * @param locale ko/en/ja
   * @return {@code {status: "sent"|"failed", deliveredAt?: ..., message?: ...}}
   */
  @SuppressWarnings("unchecked")
  public Map<String, Object> sendEchoNote(
      String recipientEmail, String aiGeneratedMessage, String locale) {
    log.info(
        "Requesting echo-note send from Echo Server (locale={}, msgLen={})",
        locale,
        aiGeneratedMessage == null ? 0 : aiGeneratedMessage.length());
    validateConnectionConfig();

    Map<String, Object> body =
        Map.of(
            "recipientEmail", recipientEmail == null ? "" : recipientEmail,
            "aiGeneratedMessage", aiGeneratedMessage == null ? "" : aiGeneratedMessage,
            "locale", locale == null ? "ko" : locale);

    try {
      return webClient
          .post()
          .uri(properties.getUrl() + properties.getApi().getEchoNoteSend())
          .bodyValue(body)
          .retrieve()
          .bodyToMono(Map.class)
          .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
          .onErrorMap(
              e -> new EchoServerConnectionException(toFriendlyMessage(e, "Echo Note 발송"), e))
          .block();
    } catch (EchoServerConnectionException e) {
      log.error("Echo Server 접속 실패 (Echo Note 발송): {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      String message = toFriendlyMessage(e, "Echo Note 발송");
      log.error("Echo Server 접속 실패 (Echo Note 발송): {}", message);
      throw new EchoServerConnectionException(message, e);
    }
  }

  private void validateConnectionConfig() {
    if (!StringUtils.hasText(properties.getUrl())
        || !StringUtils.hasText(properties.getClientId())
        || !StringUtils.hasText(properties.getClientSecret())) {
      throw new EchoServerConnectionException(
          "Echo Server 접속 설정이 누락되었습니다. URL/Client ID/Client Secret을 확인하세요.");
    }
  }

  private String toFriendlyMessage(Throwable error, String action) {
    Throwable cause = error;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }

    if (error instanceof WebClientResponseException responseException) {
      // echo 가 구조화 error code 를 본문에 담아 보냈으면 그쪽을 먼저 반영.
      String body = responseException.getResponseBodyAsString();
      String code = echoErrorMapper.extractErrorCode(body);
      String mapped =
          echoErrorMapper.mapApiError(responseException.getStatusCode().value(), code, action);
      if (mapped != null) {
        return mapped;
      }
      if (error instanceof WebClientResponseException.Unauthorized) {
        return "Echo Server 접속 실패(" + action + "): 인증에 실패했습니다. Client ID/Client Secret을 확인하세요.";
      }
      return "Echo Server 접속 실패("
          + action
          + "): 서버 응답 오류입니다. HTTP "
          + responseException.getStatusCode().value()
          + " 상태코드를 확인하세요.";
    }
    if (error instanceof TimeoutException || cause instanceof TimeoutException) {
      return "Echo Server 접속 실패(" + action + "): 요청 시간이 초과되었습니다.";
    }
    if (error instanceof WebClientRequestException) {
      return "Echo Server 접속 실패(" + action + "): 서버 연결에 실패했습니다. URL/네트워크/서버 상태를 확인하세요.";
    }
    return "Echo Server 접속 실패(" + action + "): 연결 중 오류가 발생했습니다.";
  }
}
