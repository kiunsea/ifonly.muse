package com.ifonly.museagent.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/** Trash item metadata for deferred deletion workflow. */
@Data
@Builder
public class TrashItemDto {

  private Long id;
  private String executionId;
  private String originalPath;
  private String trashPath;
  private String itemType;
  private Long sizeBytes;
  private LocalDateTime movedAt;
  private LocalDateTime expireAt;
  private String status;
  private Integer deleteAttempts;
  private LocalDateTime deletedAt;
  private String lastError;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
