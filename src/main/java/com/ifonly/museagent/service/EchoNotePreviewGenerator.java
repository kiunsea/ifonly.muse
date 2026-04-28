package com.ifonly.museagent.service;

import com.ifonly.museagent.client.EchoServerClient;
import com.ifonly.museagent.exception.EchoServerConnectionException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Echo Note AI 프리뷰 생성기 — Phase 2 echo-backed 구현.
 *
 * <p>Phase 1 에선 단순 stub 가공이었고, Phase 2 에서 echo-server 의 {@code POST
 * /api/external/echo-note/preview} 호출로 교체됨 (계획서 §7.2 결정).
 *
 * <p>echo 호출 실패 (네트워크 단절, 자격증명 미설정, echo 다운, AI LLM 장애 등) 시 자동으로 stub 폴백 → muse-agent 단독 동작 보장. 폴백
 * 결과에는 "(echo 미연결)" 표시를 명시해 사용자가 자격증명 설정/네트워크 점검을 인지하도록 함.
 *
 * @author if-only
 * @version 0.2.0
 */
@Component
@Slf4j
public class EchoNotePreviewGenerator {

  private final EchoServerClient echoServerClient;

  @Autowired
  public EchoNotePreviewGenerator(EchoServerClient echoServerClient) {
    this.echoServerClient = echoServerClient;
  }

  /**
   * 원본 메시지를 echo-server 의 LLM 가공본으로 교체. 실패 시 stub 폴백.
   *
   * @param originalMessage 사용자 원본 메시지
   * @param locale ko/en/ja
   * @return 가공된 프리뷰 텍스트 (echo 가공본 또는 stub 폴백)
   */
  public String generate(String originalMessage, String locale) {
    if (originalMessage == null || originalMessage.isBlank()) {
      return "";
    }
    try {
      Map<String, Object> response =
          echoServerClient.generateEchoNotePreview(null, originalMessage, locale);
      Object aiGenerated = response == null ? null : response.get("aiGeneratedMessage");
      if (aiGenerated instanceof String s && !s.isBlank()) {
        return s;
      }
      log.warn("Echo echo-note preview returned empty body — falling back to stub");
      return stubFallback(originalMessage, locale, "echo 응답 비어있음");
    } catch (EchoServerConnectionException e) {
      log.warn("Echo echo-note preview failed — falling back to stub: {}", e.getMessage());
      return stubFallback(originalMessage, locale, "echo 미연결");
    } catch (Exception e) {
      log.warn("Echo echo-note preview unexpected error — falling back to stub", e);
      return stubFallback(originalMessage, locale, "예기치 않은 오류");
    }
  }

  /**
   * echo 호출 실패 시 폴백. 사용자에게 "이건 echo 가공본이 아닌 stub 임" 을 명시해 발송 의사 결정에 신중을 기하도록 함.
   *
   * @param originalMessage 원본 그대로 노출
   * @param locale ko/en/ja
   * @param reason 폴백 사유 (한국어 짧은 라벨)
   */
  private String stubFallback(String originalMessage, String locale, String reason) {
    String trimmed = originalMessage.trim();
    String prefix;
    String suffix;
    if ("en".equalsIgnoreCase(locale)) {
      prefix = "[stub fallback — " + reason + "] A quiet note arrived through the system:\n\n";
      suffix =
          "\n\n— (echo-server not reached; this is a local stub. Configure credentials or retry.)";
    } else if ("ja".equalsIgnoreCase(locale)) {
      prefix = "[stub フォールバック — " + reason + "] システムから静かな便りが届きました。\n\n";
      suffix = "\n\n— (echo-server に接続できないため、これはローカルのスタブ表示です。資格情報の設定や再試行をお願いします。)";
    } else {
      prefix = "[stub 폴백 — " + reason + "] 시스템을 통해 조용한 마음이 도착했어요.\n\n";
      suffix = "\n\n— (echo-server 와 연결되지 않아 로컬 stub 으로 표시됐어요. 자격증명 설정 또는 재시도를 확인해주세요.)";
    }
    return prefix + trimmed + suffix;
  }
}
