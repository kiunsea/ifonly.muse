package com.ifonly.museagent.service;

import com.ifonly.museagent.service.TrashPurgeService.PurgeResult;
import com.ifonly.museagent.service.TrashService.TrashMoveResult;
import com.ifonly.museagent.service.TrashService.TrashRestoreResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Schedule executor service for managing and executing scheduled tasks
 *
 * <p>Coordinates file cleanup when alive status has been EXPIRED for the configured threshold.
 * Cleanup paths are loaded from database via CleanupPathService.
 *
 * @author if-only
 * @version 0.4.0
 */
@Service
@Slf4j
public class ScheduleExecutorService {

  private final FileCleanupService fileCleanupService;
  private final CleanupPathService cleanupPathService;
  private final TrashService trashService;
  private final TrashPurgeService trashPurgeService;
  private final TaskExecutionHistoryService taskExecutionHistoryService;

  private boolean tasksExecuted = false;

  @Autowired
  public ScheduleExecutorService(
      FileCleanupService fileCleanupService,
      CleanupPathService cleanupPathService,
      TrashService trashService,
      TrashPurgeService trashPurgeService,
      TaskExecutionHistoryService taskExecutionHistoryService) {
    this.fileCleanupService = fileCleanupService;
    this.cleanupPathService = cleanupPathService;
    this.trashService = trashService;
    this.trashPurgeService = trashPurgeService;
    this.taskExecutionHistoryService = taskExecutionHistoryService;
    log.info("ScheduleExecutorService initialized");
  }

  /**
   * Execute all scheduled tasks
   *
   * @return execution result
   */
  public ExecutionResult executeScheduledTasks() {
    String executionId = UUID.randomUUID().toString();
    LocalDateTime startTime = LocalDateTime.now();

    if (tasksExecuted) {
      log.warn("Scheduled tasks have already been executed");
      taskExecutionHistoryService.record(
          executionId,
          "SCHEDULED_TASKS",
          "FILE_CLEANUP",
          "File Cleanup",
          "SKIPPED",
          false,
          0,
          0,
          0,
          startTime,
          LocalDateTime.now(),
          "{\"reason\":\"already_executed\"}",
          "Tasks already executed");
      return ExecutionResult.builder()
          .success(false)
          .executionTime(startTime)
          .errorMessage("Tasks already executed")
          .build();
    }

    log.info("Executing scheduled tasks");

    ExecutionResult.ExecutionResultBuilder resultBuilder =
        ExecutionResult.builder().executionTime(startTime);

    // Execute file cleanup to trash (loaded from database)
    TrashMoveResult cleanupResult = null;
    List<String> cleanupPaths = cleanupPathService.getEnabledPathStrings();
    if (!cleanupPaths.isEmpty()) {
      log.info("Executing file cleanup move-to-trash for {} paths", cleanupPaths.size());
      cleanupResult = trashService.moveToTrash(cleanupPaths, executionId);
      resultBuilder.cleanupResult(cleanupResult);
    } else {
      taskExecutionHistoryService.record(
          executionId,
          "SCHEDULED_TASKS",
          "FILE_TRASH_MOVE",
          "File Cleanup Trash Move",
          "SKIPPED",
          true,
          0,
          0,
          0,
          startTime,
          LocalDateTime.now(),
          "{\"reason\":\"no_enabled_paths\"}",
          null);
    }

    // Determine overall success
    boolean cleanupSuccess = cleanupResult == null || cleanupResult.isAllSuccess();

    tasksExecuted = true;

    LocalDateTime completionTime = LocalDateTime.now();
    ExecutionResult result =
        resultBuilder.success(cleanupSuccess).completionTime(completionTime).build();

    if (cleanupResult != null) {
      taskExecutionHistoryService.record(
          executionId,
          "SCHEDULED_TASKS",
          "FILE_TRASH_MOVE",
          "File Cleanup Trash Move",
          cleanupSuccess ? "SUCCESS" : "FAILED",
          cleanupSuccess,
          cleanupResult.getTotalPaths(),
          cleanupResult.getSuccessCount(),
          cleanupResult.getFailureCount(),
          startTime,
          completionTime,
          "{\"totalBytesMoved\":" + cleanupResult.getTotalBytesMoved() + "}",
          cleanupSuccess ? null : "Some trash move items failed");
    }

    log.info(
        "Scheduled tasks execution completed: success={}, cleanup={}",
        cleanupSuccess,
        cleanupResult != null
            ? cleanupResult.getSuccessCount() + "/" + cleanupResult.getTotalPaths()
            : "N/A");

    return result;
  }

  /** Clear scheduled tasks and reset execution status */
  public void clearScheduledTasks() {
    tasksExecuted = false;
    log.info("Scheduled tasks cleared");
  }

  /**
   * Check if tasks have been executed
   *
   * @return true if executed
   */
  public boolean isTasksExecuted() {
    return tasksExecuted;
  }

  /** Reset execution status (for testing purposes) */
  public void resetExecutionStatus() {
    tasksExecuted = false;
    log.debug("Execution status reset");
  }

  /**
   * Verify all scheduled tasks can be executed
   *
   * @return verification result
   */
  public VerificationResult verifyScheduledTasks() {
    List<String> cleanupPaths = cleanupPathService.getEnabledPathStrings();
    var pathVerification = fileCleanupService.verifyPaths(cleanupPaths);

    return VerificationResult.builder()
        .cleanupPathsCount(cleanupPaths.size())
        .allPathsWritable(pathVerification.isAllWritable())
        .pathVerification(pathVerification)
        .build();
  }

  /** Purge expired trash items and write task history for scheduler visibility. */
  public PurgeResult purgeExpiredTrash() {
    String executionId = UUID.randomUUID().toString();
    LocalDateTime startedAt = LocalDateTime.now();
    PurgeResult result = trashPurgeService.purgeExpired();
    LocalDateTime completedAt = LocalDateTime.now();

    String status;
    boolean success;
    String errorMessage = null;
    if (result.getTargetCount() == 0) {
      status = "SKIPPED";
      success = true;
    } else if (result.isAllSuccess()) {
      status = "SUCCESS";
      success = true;
    } else {
      status = "FAILED";
      success = false;
      errorMessage = "Some trash purge items failed";
    }

    taskExecutionHistoryService.record(
        executionId,
        "SCHEDULED_TASKS",
        "TRASH_PURGE",
        "Cleanup Trash Purge",
        status,
        success,
        result.getTargetCount(),
        result.getSuccessCount(),
        result.getFailureCount(),
        startedAt,
        completedAt,
        null,
        errorMessage);

    return result;
  }

  /** Restore a trash item and record history for auditability. */
  public TrashRestoreResult restoreTrashItem(long trashItemId) {
    String executionId = UUID.randomUUID().toString();
    LocalDateTime startedAt = LocalDateTime.now();
    TrashRestoreResult result = trashService.restoreItem(trashItemId);
    LocalDateTime completedAt = LocalDateTime.now();

    taskExecutionHistoryService.record(
        executionId,
        "MANUAL_TASKS",
        "TRASH_RESTORE",
        "Cleanup Trash Restore",
        result.isSuccess() ? "SUCCESS" : "FAILED",
        result.isSuccess(),
        1,
        result.isSuccess() ? 1 : 0,
        result.isSuccess() ? 0 : 1,
        startedAt,
        completedAt,
        "{\"trashItemId\":" + trashItemId + "}",
        result.getErrorMessage());

    return result;
  }

  /** Execution result containing cleanup results */
  @Data
  @Builder
  public static class ExecutionResult {
    private boolean success;
    private LocalDateTime executionTime;
    private LocalDateTime completionTime;
    private TrashMoveResult cleanupResult;
    private String errorMessage;
  }

  /** Verification result for scheduled tasks */
  @Data
  @Builder
  public static class VerificationResult {
    private int cleanupPathsCount;
    private boolean allPathsWritable;
    private FileCleanupService.VerificationResult pathVerification;

    public boolean isReady() {
      return allPathsWritable;
    }
  }
}
