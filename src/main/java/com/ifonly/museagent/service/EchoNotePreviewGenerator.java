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

  /** Preview 결과 — text 와 함께 echo 가공본인지 stub 폴백인지 표시. UI 가 안내 배너 표시 여부 판단에 사용. */
  public record PreviewResult(String text, boolean stubFallback, String fallbackReason) {
    public static PreviewResult echo(String text) {
      return new PreviewResult(text, false, null);
    }

    public static PreviewResult stub(String text, String reason) {
      return new PreviewResult(text, true, reason);
    }
  }

  /**
   * 원본 메시지를 echo-server 의 LLM 가공본으로 교체. 실패 시 stub 폴백.
   *
   * <p>Backward-compat: 옛 호출 측 ({@link EchoNoteMessageService#generatePreview}) 은 그냥 String 반환을
   * 기대하므로 여기서 텍스트만 추출해 반환. stub 여부를 알아야 하는 새 호출 측은 {@link #generateDetailed}.
   *
   * @param originalMessage 사용자 원본 메시지
   * @param locale ko/en/ja
   * @return 가공된 프리뷰 텍스트 (echo 가공본 또는 stub 폴백)
   */
  public String generate(String originalMessage, String locale) {
    return generateDetailed(originalMessage, locale).text();
  }

  /**
   * {@link #generate} 의 상세 버전. UI 가 stub 여부를 알아야 할 때 사용.
   *
   * @return {@link PreviewResult} — text + stubFallback + fallbackReason
   */
  public PreviewResult generateDetailed(String originalMessage, String locale) {
    if (originalMessage == null || originalMessage.isBlank()) {
      return PreviewResult.echo("");
    }
    try {
      Map<String, Object> response =
          echoServerClient.generateEchoNotePreview(null, originalMessage, locale);
      Object aiGenerated = response == null ? null : response.get("aiGeneratedMessage");
      if (aiGenerated instanceof String s && !s.isBlank()) {
        return PreviewResult.echo(s);
      }
      log.warn("Echo echo-note preview returned empty body — falling back to stub");
      String reason = "echo 응답 비어있음";
      return PreviewResult.stub(stubFallback(originalMessage, locale, reason), reason);
    } catch (EchoServerConnectionException e) {
      log.warn("Echo echo-note preview failed — falling back to stub: {}", e.getMessage());
      String reason = "echo 미연결";
      return PreviewResult.stub(stubFallback(originalMessage, locale, reason), reason);
    } catch (Exception e) {
      log.warn("Echo echo-note preview unexpected error — falling back to stub", e);
      String reason = "예기치 않은 오류";
      return PreviewResult.stub(stubFallback(originalMessage, locale, reason), reason);
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
