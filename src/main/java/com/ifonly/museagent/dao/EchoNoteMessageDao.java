package com.ifonly.museagent.dao;

import com.ifonly.museagent.dto.EchoNoteMessageDto;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Echo Note Message DAO — 로컬 SQLite의 echo_note_message 테이블 접근.
 *
 * @author if-only
 * @version 0.1.0
 */
@Repository
@Slf4j
public class EchoNoteMessageDao {

  private static final DateTimeFormatter SQLITE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final JdbcTemplate jdbcTemplate;

  private final RowMapper<EchoNoteMessageDto> rowMapper =
      (rs, rowNum) ->
          EchoNoteMessageDto.builder()
              .id(rs.getLong("id"))
              .recipientEmail(rs.getString("recipient_email"))
              .originalMessage(rs.getString("original_message"))
              .aiGeneratedMessage(rs.getString("ai_generated_message"))
              .locale(rs.getString("locale"))
              .status(rs.getString("status"))
              .scheduledAt(parseDateTime(rs.getString("scheduled_at")))
              .sentAt(parseDateTime(rs.getString("sent_at")))
              .createdAt(parseDateTime(rs.getString("created_at")))
              .updatedAt(parseDateTime(rs.getString("updated_at")))
              .build();

  @Autowired
  public EchoNoteMessageDao(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<EchoNoteMessageDto> findAll() {
    return jdbcTemplate.query(
        "SELECT * FROM echo_note_message ORDER BY created_at DESC", rowMapper);
  }

  public Optional<EchoNoteMessageDto> findById(Long id) {
    try {
      EchoNoteMessageDto dto =
          jdbcTemplate.queryForObject(
              "SELECT * FROM echo_note_message WHERE id = ?", rowMapper, id);
      return Optional.ofNullable(dto);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  public EchoNoteMessageDto save(EchoNoteMessageDto dto) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    String now = LocalDateTime.now().format(SQLITE_FORMATTER);

    jdbcTemplate.update(
        connection -> {
          var ps =
              connection.prepareStatement(
                  "INSERT INTO echo_note_message "
                      + "(recipient_email, original_message, ai_generated_message, locale, status, "
                      + " scheduled_at, sent_at, created_at, updated_at) "
                      + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
          ps.setString(1, dto.getRecipientEmail());
          ps.setString(2, dto.getOriginalMessage());
          ps.setString(3, dto.getAiGeneratedMessage());
          ps.setString(4, dto.getLocale() != null ? dto.getLocale() : "ko");
          ps.setString(5, dto.getStatus() != null ? dto.getStatus() : "DRAFT");
          ps.setString(6, formatNullable(dto.getScheduledAt()));
          ps.setString(7, formatNullable(dto.getSentAt()));
          ps.setString(8, now);
          ps.setString(9, now);
          return ps;
        },
        keyHolder);

    dto.setId(keyHolder.getKey().longValue());
    dto.setCreatedAt(LocalDateTime.parse(now, SQLITE_FORMATTER));
    dto.setUpdatedAt(LocalDateTime.parse(now, SQLITE_FORMATTER));
    return dto;
  }

  public EchoNoteMessageDto update(EchoNoteMessageDto dto) {
    String now = LocalDateTime.now().format(SQLITE_FORMATTER);
    jdbcTemplate.update(
        "UPDATE echo_note_message SET "
            + "recipient_email = ?, original_message = ?, ai_generated_message = ?, "
            + "locale = ?, status = ?, scheduled_at = ?, sent_at = ?, updated_at = ? "
            + "WHERE id = ?",
        dto.getRecipientEmail(),
        dto.getOriginalMessage(),
        dto.getAiGeneratedMessage(),
        dto.getLocale(),
        dto.getStatus(),
        formatNullable(dto.getScheduledAt()),
        formatNullable(dto.getSentAt()),
        now,
        dto.getId());
    dto.setUpdatedAt(LocalDateTime.parse(now, SQLITE_FORMATTER));
    return dto;
  }

  public boolean deleteById(Long id) {
    return jdbcTemplate.update("DELETE FROM echo_note_message WHERE id = ?", id) > 0;
  }

  public int countAll() {
    Integer c =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM echo_note_message", Integer.class);
    return c != null ? c : 0;
  }

  public int countByStatus(String status) {
    Integer c =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM echo_note_message WHERE status = ?", Integer.class, status);
    return c != null ? c : 0;
  }

  /**
   * Phase 3 자동 스케줄러 조회 — 발송 대상 (READY + scheduled_at 도래) 메시지.
   *
   * <p>{@code scheduled_at IS NULL} 인 레거시 데이터 (Phase 1/2 에 마침된 항목) 는 제외 — 사용자가 명시적으로 revert + 재마침을
   * 통해 scheduled_at 을 부여해야 자동 발송 대상이 됨.
   *
   * @param now 현재 시각 (스케줄러가 주입)
   * @return 발송 후보 목록. 오래된 scheduled_at 부터 처리 (먼저 예약된 것이 먼저)
   */
  public List<EchoNoteMessageDto> findReadyDueForSending(LocalDateTime now) {
    return jdbcTemplate.query(
        "SELECT * FROM echo_note_message "
            + "WHERE status = 'READY' AND scheduled_at IS NOT NULL AND scheduled_at <= ? "
            + "ORDER BY scheduled_at ASC",
        rowMapper,
        now.format(SQLITE_FORMATTER));
  }

  private LocalDateTime parseDateTime(String s) {
    return s == null ? null : LocalDateTime.parse(s, SQLITE_FORMATTER);
  }

  private String formatNullable(LocalDateTime dt) {
    return dt == null ? null : dt.format(SQLITE_FORMATTER);
  }
}
