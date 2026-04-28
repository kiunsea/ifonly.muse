package com.ifonly.museagent.dao;

import com.ifonly.museagent.dto.TrashItemDto;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** DAO for cleanup trash item records. */
@Repository
@Slf4j
public class TrashItemDao {

  private static final DateTimeFormatter SQLITE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final JdbcTemplate jdbcTemplate;

  private final RowMapper<TrashItemDto> rowMapper =
      (rs, rowNum) ->
          TrashItemDto.builder()
              .id(rs.getLong("id"))
              .executionId(rs.getString("execution_id"))
              .originalPath(rs.getString("original_path"))
              .trashPath(rs.getString("trash_path"))
              .itemType(rs.getString("item_type"))
              .sizeBytes(getLong(rs.getObject("size_bytes")))
              .movedAt(parseDateTime(rs.getString("moved_at")))
              .expireAt(parseDateTime(rs.getString("expire_at")))
              .status(rs.getString("status"))
              .deleteAttempts(getInteger(rs.getObject("delete_attempts")))
              .deletedAt(parseDateTime(rs.getString("deleted_at")))
              .lastError(rs.getString("last_error"))
              .createdAt(parseDateTime(rs.getString("created_at")))
              .updatedAt(parseDateTime(rs.getString("updated_at")))
              .build();

  @Autowired
  public TrashItemDao(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public TrashItemDto saveMoved(TrashItemDto dto) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    String movedAt = formatDateTime(dto.getMovedAt());
    String expireAt = formatDateTime(dto.getExpireAt());
    String now = LocalDateTime.now().format(SQLITE_FORMATTER);

    jdbcTemplate.update(
        connection -> {
          var ps =
              connection.prepareStatement(
                  "INSERT INTO cleanup_trash_item "
                      + "(execution_id, original_path, trash_path, item_type, size_bytes, moved_at, expire_at, status, delete_attempts, deleted_at, last_error, created_at, updated_at) "
                      + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
          ps.setString(1, dto.getExecutionId());
          ps.setString(2, dto.getOriginalPath());
          ps.setString(3, dto.getTrashPath());
          ps.setString(4, dto.getItemType());
          ps.setObject(5, dto.getSizeBytes());
          ps.setString(6, movedAt);
          ps.setString(7, expireAt);
          ps.setString(8, dto.getStatus());
          ps.setInt(9, dto.getDeleteAttempts() == null ? 0 : dto.getDeleteAttempts());
          ps.setString(10, formatDateTime(dto.getDeletedAt()));
          ps.setString(11, dto.getLastError());
          ps.setString(12, now);
          ps.setString(13, now);
          return ps;
        },
        keyHolder);

    dto.setId(keyHolder.getKey().longValue());
    dto.setCreatedAt(LocalDateTime.parse(now, SQLITE_FORMATTER));
    dto.setUpdatedAt(LocalDateTime.parse(now, SQLITE_FORMATTER));
    return dto;
  }

  public List<TrashItemDto> findRecent(int limit, String status) {
    int normalizedLimit = Math.min(Math.max(limit, 1), 500);
    if (status == null || status.isBlank()) {
      return jdbcTemplate.query(
          "SELECT * FROM cleanup_trash_item ORDER BY moved_at DESC, id DESC LIMIT ?",
          rowMapper,
          normalizedLimit);
    }

    return jdbcTemplate.query(
        "SELECT * FROM cleanup_trash_item WHERE UPPER(status) = UPPER(?) ORDER BY moved_at DESC, id DESC LIMIT ?",
        rowMapper,
        status,
        normalizedLimit);
  }

  public Optional<TrashItemDto> findById(long id) {
    try {
      TrashItemDto dto =
          jdbcTemplate.queryForObject(
              "SELECT * FROM cleanup_trash_item WHERE id = ?", rowMapper, id);
      return Optional.ofNullable(dto);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  public List<TrashItemDto> findExpiredForPurge(LocalDateTime now, int batchSize) {
    int normalizedBatchSize = Math.min(Math.max(batchSize, 1), 1000);
    return jdbcTemplate.query(
        "SELECT * FROM cleanup_trash_item "
            + "WHERE (status = 'MOVED' OR status = 'DELETE_FAILED') AND expire_at <= ? "
            + "ORDER BY expire_at ASC, id ASC LIMIT ?",
        rowMapper,
        formatDateTime(now),
        normalizedBatchSize);
  }

  public void markDeleted(long id, LocalDateTime deletedAt) {
    String now = LocalDateTime.now().format(SQLITE_FORMATTER);
    jdbcTemplate.update(
        "UPDATE cleanup_trash_item SET status = 'DELETED', deleted_at = ?, last_error = NULL, updated_at = ? WHERE id = ?",
        formatDateTime(deletedAt),
        now,
        id);
  }

  public void markDeleteFailed(long id, String errorMessage) {
    String now = LocalDateTime.now().format(SQLITE_FORMATTER);
    jdbcTemplate.update(
        "UPDATE cleanup_trash_item SET status = 'DELETE_FAILED', delete_attempts = delete_attempts + 1, last_error = ?, updated_at = ? WHERE id = ?",
        errorMessage,
        now,
        id);
  }

  public void markRestored(long id) {
    String now = LocalDateTime.now().format(SQLITE_FORMATTER);
    jdbcTemplate.update(
        "UPDATE cleanup_trash_item SET status = 'RESTORED', last_error = NULL, updated_at = ? WHERE id = ?",
        now,
        id);
  }

  public int countByStatus(String status) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM cleanup_trash_item WHERE UPPER(status) = UPPER(?)",
            Integer.class,
            status);
    return count == null ? 0 : count;
  }

  public int countPurgeCandidates(LocalDateTime now) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM cleanup_trash_item WHERE (status = 'MOVED' OR status = 'DELETE_FAILED') AND expire_at <= ?",
            Integer.class,
            formatDateTime(now));
    return count == null ? 0 : count;
  }

  public Set<String> findActiveOriginalPaths() {
    List<String> paths =
        jdbcTemplate.query(
            "SELECT DISTINCT original_path FROM cleanup_trash_item WHERE status IN ('MOVED', 'DELETE_FAILED')",
            (rs, rowNum) -> rs.getString("original_path"));
    return new HashSet<>(paths);
  }

  private String formatDateTime(LocalDateTime dateTime) {
    return dateTime == null ? null : dateTime.format(SQLITE_FORMATTER);
  }

  private LocalDateTime parseDateTime(String dateTimeStr) {
    if (dateTimeStr == null || dateTimeStr.isBlank()) {
      return null;
    }
    return LocalDateTime.parse(dateTimeStr, SQLITE_FORMATTER);
  }

  private Integer getInteger(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    return Integer.parseInt(String.valueOf(value));
  }

  private Long getLong(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    return Long.parseLong(String.valueOf(value));
  }
}
