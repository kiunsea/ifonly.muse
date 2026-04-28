package com.ifonly.museagent.service;

import com.ifonly.museagent.client.EchoServerClient;
import com.ifonly.museagent.dao.EchoNoteMessageDao;
import com.ifonly.museagent.dto.EchoNoteMessageDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Echo Note Message Service — 사용자 PC 의 echo-note 보관함 비즈니스 로직.
 *
 * <p>Phase 1 책임:
 *
 * <ul>
 *   <li>메시지 CRUD (작성/조회/수정/삭제)
 *   <li>AI 프리뷰 생성 ({@link EchoNotePreviewGenerator} 의 stub 호출)
 *   <li>마침 처리 ({@code DRAFT} → {@code READY}). Phase 3 의 스케줄러가 {@code READY} 인 항목을 발송 대상으로 선정
 * </ul>
 *
 * <p>Phase 2 에서 prefix 생성을 echo-server 호출로 교체. Phase 3 에서 자동 발송 스케줄러 추가.
 *
 * @author if-only
 * @version 0.1.0
 */
@Service
@Slf4j
public class EchoNoteMessageService {

  private static final int MAX_MESSAGE_LENGTH = 2000;

  /**
   * 자동 발송 예약 최소 지연 (일). Phase 3 결정: 3개월 후부터 임의 일자에 발송 → 90일 ≤ delay < 180일.
   *
   * <p>사용자에게는 정확한 일자를 노출하지 않음 (시스템이 행위자라는 원칙). UI 에는 "예약됨 (3개월 이후)" 정도로만 표시.
   */
  private static final long SCHEDULE_MIN_DAYS = 90L;

  /** 자동 발송 예약 최대 지연 (일). 90~180일 사이 임의 일자 + 임의 시각으로 분산. */
  private static final long SCHEDULE_MAX_DAYS = 180L;

  private final EchoNoteMessageDao dao;
  private final EchoNotePreviewGenerator previewGenerator;
  private final EchoServerClient echoServerClient;

  @Autowired
  public EchoNoteMessageService(
      EchoNoteMessageDao dao,
      EchoNotePreviewGenerator previewGenerator,
      EchoServerClient echoServerClient) {
    this.dao = dao;
    this.previewGenerator = previewGenerator;
    this.echoServerClient = echoServerClient;
  }

  public List<EchoNoteMessageDto> getAll() {
    return dao.findAll();
  }

  public EchoNoteMessageDto getById(Long id) {
    return dao.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("echo_note_message not found: id=" + id));
  }

  public EchoNoteMessageDto create(String recipientEmail, String originalMessage, String locale) {
    String email = normalizeEmail(recipientEmail);
    String body = validateMessage(originalMessage);

    EchoNoteMessageDto dto =
        EchoNoteMessageDto.builder()
            .recipientEmail(email)
            .originalMessage(body)
            .locale(locale != null ? locale : "ko")
            .status("DRAFT")
            .build();

    EchoNoteMessageDto saved = dao.save(dto);
    log.info("Home-nudge message created: id={}, status=DRAFT", saved.getId());
    return saved;
  }

  public EchoNoteMessageDto update(
      Long id, String recipientEmail, String originalMessage, String aiGeneratedMessage) {
    EchoNoteMessageDto existing = getById(id);
    if ("SENT".equals(existing.getStatus())) {
      throw new IllegalStateException("이미 발송된 메시지는 수정할 수 없어요.");
    }

    if (recipientEmail != null && !recipientEmail.isBlank()) {
      existing.setRecipientEmail(normalizeEmail(recipientEmail));
    }
    if (originalMessage != null) {
      existing.setOriginalMessage(validateMessage(originalMessage));
    }
    if (aiGeneratedMessage != null) {
      // 사용자가 프리뷰 텍스트를 직접 편집한 경우. null/빈문자열 명시 의도가 있을 수 있어
      // 빈 문자열 검증은 하지 않음 — 단지 길이 제한만 적용.
      if (aiGeneratedMessage.length() > MAX_MESSAGE_LENGTH * 2) {
        throw new IllegalArgumentException(
            "프리뷰 메시지가 너무 길어요. 최대 " + (MAX_MESSAGE_LENGTH * 2) + "자까지 가능해요.");
      }
      existing.setAiGeneratedMessage(aiGeneratedMessage);
    }

    EchoNoteMessageDto updated = dao.update(existing);
    log.info("Home-nudge message updated: id={}", updated.getId());
    return updated;
  }

  /**
   * AI 프리뷰 생성/재생성 (Phase 1 stub).
   *
   * <p>현재 보관된 {@code originalMessage} 와 {@code locale} 을 stub 가공기에 넣어 결과를 {@code
   * aiGeneratedMessage} 에 저장. status 는 변경하지 않음 (편집 단계는 PREVIEW 라는 별도 상태 없이 ai_generated_message 의
   * 존재 여부로 표현).
   */
  public EchoNoteMessageDto generatePreview(Long id) {
    EchoNoteMessageDto existing = getById(id);
    if ("SENT".equals(existing.getStatus())) {
      throw new IllegalStateException("이미 발송된 메시지의 프리뷰는 다시 만들 수 없어요.");
    }
    String preview = previewGenerator.generate(existing.getOriginalMessage(), existing.getLocale());
    existing.setAiGeneratedMessage(preview);
    EchoNoteMessageDto updated = dao.update(existing);
    log.info("Home-nudge preview generated (stub): id={}", updated.getId());
    return updated;
  }

  /**
   * 마침 처리: DRAFT → READY + scheduled_at 자동 설정 (3개월 후~6개월 후 임의 일자/시각).
   *
   * <p>Phase 3: scheduled_at 을 본 메서드가 직접 설정. {@link
   * com.ifonly.museagent.scheduler.EchoNoteAutoSendScheduler} 가 매시간 돌면서 {@code status=READY +
   * scheduled_at <= now} 인 항목을 발송 대상으로 선정.
   *
   * <p>정확한 발송 일자는 사용자에게 노출하지 않음 — UI 에는 "예약됨 (3개월 이후)" 정도로만 표시 (계획서 §7.3 b 결정 — 시스템이 행위자라는 원칙). AI
   * 프리뷰가 없으면 마침 불가 (사용자가 발송될 본문을 한 번이라도 확인해야 함).
   */
  public EchoNoteMessageDto finalizeMessage(Long id) {
    EchoNoteMessageDto existing = getById(id);
    if ("SENT".equals(existing.getStatus())) {
      throw new IllegalStateException("이미 발송된 메시지예요.");
    }
    if (existing.getAiGeneratedMessage() == null || existing.getAiGeneratedMessage().isBlank()) {
      throw new IllegalStateException("프리뷰를 먼저 만들어주세요.");
    }
    existing.setStatus("READY");
    existing.setScheduledAt(generateRandomScheduledAt());
    EchoNoteMessageDto updated = dao.update(existing);
    log.info(
        "Home-nudge message finalized to READY: id={} (scheduled_at hidden from user)",
        updated.getId());
    return updated;
  }

  /**
   * 자동 발송 예약 시각 생성 — 90~180일 후 사이 임의 분 단위 시각.
   *
   * <p>일 + 시각 모두 임의화 → 동일 시점에 마침된 여러 메시지가 같은 시각에 몰려 발송되지 않도록 분산.
   */
  private LocalDateTime generateRandomScheduledAt() {
    long minMinutes = SCHEDULE_MIN_DAYS * 24L * 60L;
    long maxMinutes = SCHEDULE_MAX_DAYS * 24L * 60L;
    long randomMinutes = ThreadLocalRandom.current().nextLong(minMinutes, maxMinutes);
    return LocalDateTime.now().plusMinutes(randomMinutes);
  }

  /**
   * READY → DRAFT 되돌리기. 사용자가 마음이 바뀌어 다시 편집하고 싶을 때.
   *
   * <p>Phase 3 스케줄러 도입 후엔 이미 scheduled_at 이 도래해 SENT 가 된 메시지엔 적용되지 않음.
   */
  public EchoNoteMessageDto revertToDraft(Long id) {
    EchoNoteMessageDto existing = getById(id);
    if ("SENT".equals(existing.getStatus())) {
      throw new IllegalStateException("이미 발송된 메시지는 되돌릴 수 없어요.");
    }
    existing.setStatus("DRAFT");
    existing.setScheduledAt(null);
    EchoNoteMessageDto updated = dao.update(existing);
    log.info("Home-nudge message reverted to DRAFT: id={}", updated.getId());
    return updated;
  }

  /**
   * READY 상태의 메시지를 echo-server 외부 API 로 즉시 발송.
   *
   * <p>Phase 2 의 end-to-end 발송 경로. Phase 3 의 자동 스케줄러도 동일 메서드를 호출 예정. echo-server 는 본문을 보관하지 않고 발송만
   * 수행하므로, 발송 후 muse-agent 가 자체적으로 status=SENT 와 sentAt 을 기록한다.
   *
   * <p>발송 실패 (네트워크/auth/SMTP 오류) 시 IllegalStateException 을 던지며 상태는 READY 그대로 유지 → 사용자가 재시도 가능.
   *
   * @param id 발송할 메시지 ID
   * @return 갱신된 메시지 (status=SENT)
   */
  public EchoNoteMessageDto sendViaEcho(Long id) {
    EchoNoteMessageDto existing = getById(id);
    if ("SENT".equals(existing.getStatus())) {
      throw new IllegalStateException("이미 발송된 메시지예요.");
    }
    if (!"READY".equals(existing.getStatus())) {
      throw new IllegalStateException("마침 처리된 메시지만 발송할 수 있어요. 먼저 마침 버튼을 눌러주세요.");
    }
    if (existing.getAiGeneratedMessage() == null || existing.getAiGeneratedMessage().isBlank()) {
      throw new IllegalStateException("프리뷰가 없어요. 먼저 프리뷰를 만들어주세요.");
    }

    Map<String, Object> result;
    try {
      result =
          echoServerClient.sendEchoNote(
              existing.getRecipientEmail(), existing.getAiGeneratedMessage(), existing.getLocale());
    } catch (Exception e) {
      log.error("Home-nudge send via echo failed: id={}", id, e);
      throw new IllegalStateException("echo-server 발송에 실패했어요. 자격증명/네트워크/echo 상태를 확인해주세요.", e);
    }

    String status = result == null ? null : String.valueOf(result.get("status"));
    if (!"sent".equals(status)) {
      String message = result == null ? "응답 없음" : String.valueOf(result.get("message"));
      log.warn(
          "Home-nudge send via echo returned non-sent status: id={}, status={}, message={}",
          id,
          status,
          message);
      throw new IllegalStateException("echo-server 가 발송에 실패했어요: " + message);
    }

    existing.setStatus("SENT");
    existing.setSentAt(LocalDateTime.now());
    EchoNoteMessageDto updated = dao.update(existing);
    log.info(
        "Home-nudge message sent via echo: id={}, recipient={}", id, existing.getRecipientEmail());
    return updated;
  }

  public boolean delete(Long id) {
    boolean deleted = dao.deleteById(id);
    if (deleted) {
      log.info("Home-nudge message deleted: id={}", id);
    }
    return deleted;
  }

  public int getTotalCount() {
    return dao.countAll();
  }

  public int getReadyCount() {
    return dao.countByStatus("READY");
  }

  // ---------------------------------------------------------------------------
  // 검증 헬퍼
  // ---------------------------------------------------------------------------

  private String normalizeEmail(String email) {
    if (email == null) {
      throw new IllegalArgumentException("수신자 이메일을 입력해주세요.");
    }
    String e = email.trim().toLowerCase();
    if (e.isEmpty()) {
      throw new IllegalArgumentException("수신자 이메일을 입력해주세요.");
    }
    if (!e.contains("@") || !e.contains(".")) {
      throw new IllegalArgumentException("이메일 형식이 올바르지 않아요.");
    }
    return e;
  }

  private String validateMessage(String message) {
    if (message == null) {
      throw new IllegalArgumentException("메시지 내용을 입력해주세요.");
    }
    String trimmed = message.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("메시지 내용을 입력해주세요.");
    }
    if (trimmed.length() > MAX_MESSAGE_LENGTH) {
      throw new IllegalArgumentException("메시지가 너무 길어요. 최대 " + MAX_MESSAGE_LENGTH + "자까지 가능해요.");
    }
    return trimmed;
  }
}
