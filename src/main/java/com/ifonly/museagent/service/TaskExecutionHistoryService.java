package com.ifonly.museagent.service;

import com.ifonly.museagent.dao.TaskExecutionHistoryDao;
import com.ifonly.museagent.dto.TaskExecutionHistoryDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service for persisting and querying task execution history. */
@Service
@Slf4j
public class TaskExecutionHistoryService {

  private final TaskExecutionHistoryDao taskExecutionHistoryDao;

  @Autowired
  public TaskExecutionHistoryService(TaskExecutionHistoryDao taskExecutionHistoryDao) {
    this.taskExecutionHistoryDao = taskExecutionHistoryDao;
  }

  public TaskExecutionHistoryDto record(
      String executionId,
      String taskGroup,
      String taskKey,
      String taskName,
      String status,
      boolean success,
      Integer targetCount,
      Integer successCount,
      Integer failureCount,
      LocalDateTime startedAt,
      LocalDateTime completedAt,
      String metadataJson,
      String errorMessage) {
    TaskExecutionHistoryDto dto =
        TaskExecutionHistoryDto.builder()
            .executionId(executionId)
            .taskGroup(taskGroup)
            .taskKey(taskKey)
            .taskName(taskName)
            .status(status)
            .success(success)
            .targetCount(targetCount)
            .successCount(successCount)
            .failureCount(failureCount)
            .startedAt(startedAt)
            .completedAt(completedAt)
            .metadataJson(metadataJson)
            .errorMessage(errorMessage)
            .build();

    taskExecutionHistoryDao.save(dto);
    log.debug(
        "Task execution history saved: executionId={}, taskKey={}, status={}",
        executionId,
        taskKey,
        status);
    return dto;
  }

  public List<TaskExecutionHistoryDto> getRecent(int limit, String taskGroup) {
    return getRecent(limit, taskGroup, null, null, null, null, null, "createdAt", "desc");
  }

  public List<TaskExecutionHistoryDto> getRecent(
      int limit,
      String taskGroup,
      String taskKey,
      String status,
      Boolean success,
      LocalDate startDate,
      LocalDate endDate,
      String sortBy,
      String sortDir) {
    int normalizedLimit = Math.min(Math.max(limit, 1), 100);
    LocalDateTime normalizedStartAt = startDate == null ? null : startDate.atStartOfDay();
    LocalDateTime normalizedEndExclusive =
        endDate == null ? null : endDate.plusDays(1).atStartOfDay();

    return taskExecutionHistoryDao.findRecentByFilters(
        normalizedLimit,
        normalizeBlank(taskGroup),
        normalizeBlank(taskKey),
        normalizeBlank(status),
        success,
        normalizedStartAt,
        normalizedEndExclusive,
        normalizeBlank(sortBy),
        normalizeBlank(sortDir));
  }

  private String normalizeBlank(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
