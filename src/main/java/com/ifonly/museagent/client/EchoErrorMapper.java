package com.ifonly.museagent.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * echo-server 의 외부 API 가 반환하는 에러 응답 본문을 사용자 친화 한국어로 매핑.
 *
 * <p>echo-server 의 {@code ExternalEchoNoteController} 는 두 형태의 에러 본문을 반환한다:
 *
 * <ul>
 *   <li>4xx — {@code {error: "<code>"}} 형태. 예: {@code missing_message}, {@code message_too_long},
 *       {@code invalid_recipient}, {@code missing_token}, {@code invalid_token}, {@code
 *       not_authenticated}.
 *   <li>503 (/send 만 해당) — {@code {status: "failed", message: "<reason>"}} 형태. {@code message} 는
 *       echo 의 {@code EmailService.SendResult.message()} 또는 sentinel 인 {@code
 *       email_service_unavailable}.
 *   <li>410 (/exchange-continuation-token) — {@code {error: "<reason>"}} 형태로 토큰 만료/사용됨/무효.
 * </ul>
 *
 * <p>본 매퍼는 응답 본문을 파싱해 알려진 코드/메시지 패턴을 사용자 친화 표현으로 환원하고, 미지의 입력은 일반적인 fallback 으로 떨어뜨려 raw 영문이 사용자에게
 * 그대로 노출되지 않도록 막는다.
 *
 * @author if-only
 * @version 0.1.0
 */
@Component
@Slf4j
public class EchoErrorMapper {

  private final ObjectMapper objectMapper;

  public EchoErrorMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** 응답 body JSON 에서 {@code error} 필드를 추출. 파싱 실패 시 null. */
  public String extractErrorCode(String body) {
    return extractField(body, "error");
  }

  /** 응답 body JSON 에서 {@code message} 필드를 추출. 파싱 실패 시 null. */
  public String extractMessage(String body) {
    return extractField(body, "message");
  }

  @SuppressWarnings("unchecked")
  private String extractField(String body, String key) {
    if (body == null || body.isBlank()) {
      return null;
    }
    try {
      Map<String, Object> map = objectMapper.readValue(body, Map.class);
      Object value = map.get(key);
      return value == null ? null : value.toString();
    } catch (Exception e) {
      log.debug("Echo error body parse failed (key={}, body length={})", key, body.length());
      return null;
    }
  }

  /**
   * echo 의 4xx/5xx 응답에서 추출한 코드와 작업명으로 사용자 친화 한국어 메시지 반환. 매칭되는 코드가 없으면 {@code null} — 호출자가 자체
   * fallback 처리.
   *
   * @param status HTTP status code
   * @param errorCode 응답 body 의 {@code error} 필드 (없으면 null)
   * @param action 작업명 (예: "Echo Note 프리뷰", "Echo Note 발송")
   * @return 사용자 친화 메시지 또는 매칭 실패 시 null
   */
  public String mapApiError(int status, String errorCode, String action) {
    if (status == 401 || "not_authenticated".equals(errorCode)) {
      return "echo 인증에 실패했어요. echo-config 에서 자격증명을 다시 설정해주세요.";
    }
    if (errorCode == null) {
      return null;
    }
    return switch (errorCode) {
      case "missing_message" -> "원본 메시지를 입력해주세요.";
      case "message_too_long" -> "메시지가 너무 길어요. 길이를 줄여 다시 시도해주세요.";
      case "invalid_recipient" -> "수신자 이메일 형식이 잘못됐어요.";
      case "missing_token" -> "popup 링크가 손상됐어요. echo 에서 다시 진입해주세요.";
      case "invalid_token" -> "유효하지 않은 popup 링크예요. echo 에서 다시 진입해주세요.";
      case "exchange_failed" -> "echo 와의 자격증명 교환 중 오류가 발생했어요. 잠시 후 다시 시도해주세요.";
      default -> null;
    };
  }

  /**
   * /exchange-continuation-token 의 4xx/5xx 응답을 사용자 친화 메시지로 매핑. 410 GONE 은 echo 가 토큰 사용/만료 시 반환.
   *
   * @param status HTTP status code
   * @param errorCode 응답 body 의 {@code error} 필드 (없으면 null)
   * @return 사용자 친화 메시지 (항상 non-null — 미지 케이스도 generic fallback 으로 처리)
   */
  public String mapContinuationError(int status, String errorCode) {
    if (status == 410) {
      return "이전에 사용됐거나 만료된 popup 링크예요. echo 에서 다시 진입해주세요.";
    }
    String mapped = mapApiError(status, errorCode, "자격증명 교환");
    if (mapped != null) {
      return mapped;
    }
    if (status >= 500) {
      return "echo 서버에 일시적 오류가 발생했어요. 잠시 후 다시 시도해주세요.";
    }
    return "popup 링크 처리 중 오류가 발생했어요. echo 에서 다시 진입해주세요.";
  }

  /**
   * /send 의 503 응답에서 echo 가 돌려준 raw 실패 메시지를 사용자 친화 한국어로 환원. echo 의 {@code
   * EmailService.SendResult.message()} 또는 sentinel ({@code email_service_unavailable}) 가 입력으로 들어옴.
   *
   * @param rawMessage echo 응답 body 의 {@code message} 필드
   * @return 사용자 친화 메시지 (항상 non-null)
   */
  public String mapSendFailure(String rawMessage) {
    if (rawMessage == null || rawMessage.isBlank()) {
      return "echo 가 메일 발송에 실패했어요. 잠시 후 다시 시도해주세요.";
    }
    if ("email_service_unavailable".equals(rawMessage)
        || rawMessage.startsWith("Email service is not available")) {
      return "echo 의 메일 서비스가 일시적으로 사용 불가예요. 잠시 후 다시 시도해주세요.";
    }
    if (rawMessage.startsWith("Email notification disabled")) {
      return "수신자가 echo 의 메일 발송을 차단해 두셨어요.";
    }
    if (rawMessage.startsWith("Failed to send")) {
      return "echo 의 메일 발송 도중 오류가 발생했어요. 잠시 후 다시 시도해주세요.";
    }
    // 미지 메시지: raw 노출 회피
    log.warn("Unmapped echo send failure message: {}", rawMessage);
    return "echo 가 메일 발송에 실패했어요. 잠시 후 다시 시도해주세요.";
  }
}
