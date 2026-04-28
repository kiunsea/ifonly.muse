package com.ifonly.museagent.dao;

import com.ifonly.museagent.dto.TaskExecutionHistoryDto;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** DAO for task execution history records. */
@Repository
@Slf4j
public class TaskExecutionHistoryDao {

  private static final DateTimeFormatter SQLITE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final JdbcTemplate jdbcTemplate;

  private final RowMapper<TaskExecutionHistoryDto> rowMapper =
      (rs, rowNum) ->
          TaskExecutionHistoryDto.builder()
              .id(rs.getLong("id"))
              .executionId(rs.getString("execution_id"))
              .taskGroup(rs.getString("task_group"))
              .taskKey(rs.getString("task_key"))
              .taskName(rs.getString("task_name"))
              .status(rs.getString("status"))
              .success(rs.getInt("success") == 1)
              .targetCount(getInteger(rs.getObject("target_count")))
              .successCount(getInteger(rs.getObject("success_count")))
              .failureCount(getInteger(rs.getObject("failure_count")))
              .startedAt(parseDateTime(rs.getString("started_at")))
              .completedAt(parseDateTime(rs.getString("completed_at")))
              .metadataJson(rs.getString("metadata_json"))
              .errorMessage(rs.getString("error_message"))
              .createdAt(parseDateTime(rs.getString("created_at")))
              .build();

  @Autowired
  public TaskExecutionHistoryDao(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public TaskExecutionHistoryDto save(TaskExecutionHistoryDto dto) {
    String sql =
        "INSERT INTO task_execution_history (execution_id, task_group, task_key, task_name, status, success, "
            + "target_count, success_count, failure_count, started_at, completed_at, metadata_json, error_message) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    jdbcTemplate.update(
        connection -> {
          PreparedStatement ps = connection.prepareStatement(sql);
          ps.setString(1, dto.getExecutionId());
          ps.setString(2, dto.getTaskGroup());
          ps.setString(3, dto.getTaskKey());
          ps.setString(4, dto.getTaskName());
          ps.setString(5, dto.getStatus());
          ps.setInt(6, dto.isSuccess() ? 1 : 0);
          ps.setObject(7, dto.getTargetCount());
          ps.setObject(8, dto.getSuccessCount());
          ps.setObject(9, dto.getFailureCount());
          ps.setString(10, formatDateTime(dto.getStartedAt()));
          ps.setString(11, formatDateTime(dto.getCompletedAt()));
          ps.setString(12, dto.getMetadataJson());
          ps.setString(13, dto.getErrorMessage());
          return ps;
        });

    return dto;
  }

  public List<TaskExecutionHistoryDto> findRecent(int limit) {
    return jdbcTemplate.query(
        "SELECT * FROM task_execution_history ORDER BY created_at DESC LIMIT ?", rowMapper, limit);
  }

  public List<TaskExecutionHistoryDto> findRecentByTaskGroup(String taskGroup, int limit) {
    return jdbcTemplate.query(
        "SELECT * FROM task_execution_history WHERE task_group = ? ORDER BY created_at DESC LIMIT ?",
        rowMapper,
        taskGroup,
        limit);
  }

  public List<TaskExecutionHistoryDto> findRecentByFilters(
      int limit,
      String taskGroup,
      String taskKey,
      String status,
      Boolean success,
      LocalDateTime startAt,
      LocalDateTime endExclusive,
      String sortBy,
      String sortDir) {
    StringBuilder sql = new StringBuilder("SELECT * FROM task_execution_history WHERE 1=1");
    List<Object> params = new ArrayList<>();

    if (taskGroup != null && !taskGroup.isBlank()) {
      sql.append(" AND task_group = ?");
      params.add(taskGroup);
    }
    if (taskKey != null && !taskKey.isBlank()) {
      sql.append(" AND task_key LIKE ?");
      params.add("%" + taskKey + "%");
    }
    if (status != null && !status.isBlank()) {
      sql.append(" AND UPPER(status) = UPPER(?)");
      params.add(status);
    }
    if (success != null) {
      sql.append(" AND success = ?");
      params.add(success ? 1 : 0);
    }
    if (startAt != null) {
      sql.append(" AND created_at >= ?");
      params.add(formatDateTime(startAt));
    }
    if (endExclusive != null) {
      sql.append(" AND created_at < ?");
      params.add(formatDateTime(endExclusive));
    }

    sql.append(" ORDER BY ")
        .append(resolveSortColumn(sortBy))
        .append(" ")
        .append(resolveSortDirection(sortDir))
        .append(", id DESC LIMIT ?");
    params.add(limit);

    return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
  }

  private String resolveSortColumn(String sortBy) {
    if (sortBy == null || sortBy.isBlank()) {
      return "created_at";
    }
    return switch (sortBy.toLowerCase()) {
      case "completedat" -> "completed_at";
      case "startedat" -> "started_at";
      case "status" -> "status";
      case "taskgroup" -> "task_group";
      case "successcount" -> "success_count";
      case "failurecount" -> "failure_count";
      case "targetcount" -> "target_count";
      case "createdat" -> "created_at";
      default -> "created_at";
    };
  }

  private String resolveSortDirection(String sortDir) {
    return "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";
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

  private String formatDateTime(LocalDateTime dateTime) {
    return dateTime == null ? null : dateTime.format(SQLITE_FORMATTER);
  }

  private LocalDateTime parseDateTime(String dateTimeStr) {
    if (dateTimeStr == null || dateTimeStr.isBlank()) {
      return null;
    }
    return LocalDateTime.parse(dateTimeStr, SQLITE_FORMATTER);
  }
}
