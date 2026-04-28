package com.ifonly.museagent.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cleanup Path DTO
 *
 * <p>Represents a file or directory path scheduled for cleanup.
 *
 * @author if-only
 * @version 0.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleanupPathDto {

  private Long id;
  private String path;
  private String description;
  private String pathType;
  private String cleanupStatus;
  private boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
