package com.ifonly.museagent.dao;

import com.ifonly.museagent.dto.CleanupPathDto;
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
 * Cleanup Path DAO
 *
 * <p>Data access layer for cleanup_path table using JdbcTemplate.
 *
 * @author if-only
 * @version 0.1.0
 */
@Repository
@Slf4j
public class CleanupPathDao {

  private final JdbcTemplate jdbcTemplate;

  private static final DateTimeFormatter SQLITE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final RowMapper<CleanupPathDto> rowMapper =
      (rs, rowNum) ->
          CleanupPathDto.builder()
              .id(rs.getLong("id"))
              .path(rs.getString("path"))
              .description(rs.getString("description"))
              .pathType(rs.getString("path_type"))
              .enabled(rs.getInt("enabled") == 1)
              .createdAt(parseDateTime(rs.getString("created_at")))
              .updatedAt(parseDateTime(rs.getString("updated_at")))
              .build();

  @Autowired
  public CleanupPathDao(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<CleanupPathDto> findAll() {
    return jdbcTemplate.query("SELECT * FROM cleanup_path ORDER BY created_at DESC", rowMapper);
  }

  public List<CleanupPathDto> findAllEnabled() {
    return jdbcTemplate.query(
        "SELECT * FROM cleanup_path WHERE enabled = 1 ORDER BY created_at DESC", rowMapper);
  }

  public Optional<CleanupPathDto> findById(Long id) {
    try {
      CleanupPathDto dto =
          jdbcTemplate.queryForObject("SELECT * FROM cleanup_path WHERE id = ?", rowMapper, id);
      return Optional.ofNullable(dto);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  public boolean existsByPath(String path) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM cleanup_path WHERE path = ?", Integer.class, path);
    return count != null && count > 0;
  }

  public CleanupPathDto save(CleanupPathDto dto) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    String now = LocalDateTime.now().format(SQLITE_FORMATTER);

    jdbcTemplate.update(
        connection -> {
          var ps =
              connection.prepareStatement(
                  "INSERT INTO cleanup_path (path, description, path_type, enabled, created_at, updated_at) "
                      + "VALUES (?, ?, ?, ?, ?, ?)");
          ps.setString(1, dto.getPath());
          ps.setString(2, dto.getDescription());
          ps.setString(3, dto.getPathType() != null ? dto.getPathType() : "UNKNOWN");
          ps.setInt(4, dto.isEnabled() ? 1 : 0);
          ps.setString(5, now);
          ps.setString(6, now);
          return ps;
        },
        keyHolder);

    dto.setId(keyHolder.getKey().longValue());
    dto.setCreatedAt(LocalDateTime.parse(now, SQLITE_FORMATTER));
    dto.setUpdatedAt(LocalDateTime.parse(now, SQLITE_FORMATTER));
    return dto;
  }

  public CleanupPathDto update(CleanupPathDto dto) {
    String now = LocalDateTime.now().format(SQLITE_FORMATTER);
    jdbcTemplate.update(
        "UPDATE cleanup_path SET path = ?, description = ?, path_type = ?, enabled = ?, updated_at = ? WHERE id = ?",
        dto.getPath(),
        dto.getDescription(),
        dto.getPathType(),
        dto.isEnabled() ? 1 : 0,
        now,
        dto.getId());
    dto.setUpdatedAt(LocalDateTime.parse(now, SQLITE_FORMATTER));
    return dto;
  }

  public boolean deleteById(Long id) {
    int rows = jdbcTemplate.update("DELETE FROM cleanup_path WHERE id = ?", id);
    return rows > 0;
  }

  public int countAll() {
    Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cleanup_path", Integer.class);
    return count != null ? count : 0;
  }

  public int countEnabled() {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM cleanup_path WHERE enabled = 1", Integer.class);
    return count != null ? count : 0;
  }

  private LocalDateTime parseDateTime(String dateTimeStr) {
    if (dateTimeStr == null) {
      return null;
    }
    // SQLite stores as "yyyy-MM-dd HH:mm:ss"
    return LocalDateTime.parse(dateTimeStr, SQLITE_FORMATTER);
  }
}
