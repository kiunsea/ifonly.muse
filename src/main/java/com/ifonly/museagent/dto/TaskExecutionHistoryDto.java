package com.ifonly.museagent.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * Task execution history DTO.
 *
 * <p>Stores per-task execution results so task types can be extended without schema changes.
 */
@Data
@Builder
public class TaskExecutionHistoryDto {
  private Long id;
  private String executionId;
  private String taskGroup;
  private String taskKey;
  private String taskName;
  private String status;
  private boolean success;
  private Integer targetCount;
  private Integer successCount;
  private Integer failureCount;
  private LocalDateTime startedAt;
  private LocalDateTime completedAt;
  private String metadataJson;
  private String errorMessage;
  private LocalDateTime createdAt;
}
