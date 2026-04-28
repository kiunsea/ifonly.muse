package com.ifonly.museagent.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Echo Note Message DTO — 사용자 PC 에 보관되는 echo-note 메시지.
 *
 * <p>echo-server 의 echo_note_drafts 와 동일 도메인이지만 *데이터 보유 주체* 가 다름. 이 DTO 는 사용자 PC 의 muse-agent 에만
 * 존재. echo-server 는 발송 시점에만 메시지를 받아 처리 (Phase 2 이후).
 *
 * <p>{@code scheduledAt} 은 Phase 3 스케줄러가 자동 설정 (3개월 후 임의 날짜) 하며, 사용자에게는 정확한 날짜를 노출하지 않는 것이 정책이다
 * (계획서 §7.3 결정 — 시스템이 행위자라는 원칙).
 *
 * @author if-only
 * @version 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EchoNoteMessageDto {

  private Long id;
  private String recipientEmail;
  private String originalMessage;
  private String aiGeneratedMessage;
  private String locale;

  /** {@code DRAFT} | {@code READY} | {@code SENT} */
  private String status;

  private LocalDateTime scheduledAt;
  private LocalDateTime sentAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
