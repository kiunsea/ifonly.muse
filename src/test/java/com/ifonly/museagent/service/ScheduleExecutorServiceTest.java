package com.ifonly.museagent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ifonly.museagent.service.FileCleanupService.VerificationResult;
import com.ifonly.museagent.service.ScheduleExecutorService.ExecutionResult;
import com.ifonly.museagent.service.TrashService.TrashMoveResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for ScheduleExecutorService
 *
 * @author if-only
 * @version 0.4.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleExecutorService Tests")
class ScheduleExecutorServiceTest {

  @Mock private FileCleanupService fileCleanupService;

  @Mock private CleanupPathService cleanupPathService;

  @Mock private TrashService trashService;

  @Mock private TrashPurgeService trashPurgeService;

  @Mock private TaskExecutionHistoryService taskExecutionHistoryService;

  private ScheduleExecutorService scheduleExecutorService;

  @BeforeEach
  void setUp() {
    scheduleExecutorService =
        new ScheduleExecutorService(
            fileCleanupService,
            cleanupPathService,
            trashService,
            trashPurgeService,
            taskExecutionHistoryService);
  }

  @Test
  @DisplayName("should execute cleanup tasks")
  void testExecuteCleanup() {
    // Given
    when(cleanupPathService.getEnabledPathStrings()).thenReturn(List.of("/test/path"));

    TrashMoveResult cleanupResult =
        TrashMoveResult.builder()
            .totalPaths(1)
            .successCount(1)
            .failureCount(0)
            .totalBytesMoved(1024)
            .items(List.of())
            .build();

    when(trashService.moveToTrash(anyList(), org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(cleanupResult);

    // When
    ExecutionResult result = scheduleExecutorService.executeScheduledTasks();

    // Then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getCleanupResult()).isNotNull();
    assertThat(result.getCleanupResult().getSuccessCount()).isEqualTo(1);
    verify(trashService).moveToTrash(anyList(), org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  @DisplayName("should not execute twice")
  void testExecuteOnlyOnce() {
    // Given
    when(cleanupPathService.getEnabledPathStrings()).thenReturn(List.of());

    // First execution
    ExecutionResult firstResult = scheduleExecutorService.executeScheduledTasks();

    // When - Second execution attempt
    ExecutionResult secondResult = scheduleExecutorService.executeScheduledTasks();

    // Then
    assertThat(firstResult.isSuccess()).isTrue();
    assertThat(secondResult.isSuccess()).isFalse();
    assertThat(secondResult.getErrorMessage()).contains("already executed");
  }

  @Test
  @DisplayName("should reset execution status")
  void testResetExecutionStatus() {
    // Given
    when(cleanupPathService.getEnabledPathStrings()).thenReturn(List.of());

    // First execution
    scheduleExecutorService.executeScheduledTasks();
    assertThat(scheduleExecutorService.isTasksExecuted()).isTrue();

    // When
    scheduleExecutorService.resetExecutionStatus();

    // Then
    assertThat(scheduleExecutorService.isTasksExecuted()).isFalse();
  }

  @Test
  @DisplayName("should clear scheduled tasks")
  void testClearScheduledTasks() {
    // Given
    when(cleanupPathService.getEnabledPathStrings()).thenReturn(List.of());
    scheduleExecutorService.executeScheduledTasks();

    // When
    scheduleExecutorService.clearScheduledTasks();

    // Then
    assertThat(scheduleExecutorService.isTasksExecuted()).isFalse();
  }

  @Test
  @DisplayName("should verify scheduled tasks")
  void testVerifyScheduledTasks() {
    // Given
    when(cleanupPathService.getEnabledPathStrings()).thenReturn(List.of("/path1"));

    VerificationResult pathVerification =
        VerificationResult.builder()
            .totalPaths(1)
            .existingCount(1)
            .writableCount(1)
            .verifications(List.of())
            .build();

    when(fileCleanupService.verifyPaths(anyList())).thenReturn(pathVerification);

    // When
    var result = scheduleExecutorService.verifyScheduledTasks();

    // Then
    assertThat(result.getCleanupPathsCount()).isEqualTo(1);
    assertThat(result.isAllPathsWritable()).isTrue();
    assertThat(result.isReady()).isTrue();
  }

  @Test
  @DisplayName("should handle cleanup failure")
  void testHandleCleanupFailure() {
    // Given
    when(cleanupPathService.getEnabledPathStrings()).thenReturn(List.of("/path1", "/path2"));

    TrashMoveResult cleanupResult =
        TrashMoveResult.builder()
            .totalPaths(2)
            .successCount(1)
            .failureCount(1)
            .totalBytesMoved(512)
            .items(List.of())
            .build();

    when(trashService.moveToTrash(anyList(), org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(cleanupResult);

    // When
    ExecutionResult result = scheduleExecutorService.executeScheduledTasks();

    // Then
    assertThat(result.isSuccess()).isFalse(); // Partial failure means not all success
    assertThat(result.getCleanupResult().getFailureCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("should execute empty schedules successfully")
  void testExecuteEmptySchedules() {
    // Given
    when(cleanupPathService.getEnabledPathStrings()).thenReturn(List.of());

    // When - no paths added
    ExecutionResult result = scheduleExecutorService.executeScheduledTasks();

    // Then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getCleanupResult()).isNull();
  }

  @Test
  @DisplayName("should not execute cleanup when no paths configured")
  void testNoCleanupWhenNoPathsConfigured() {
    // Given
    when(cleanupPathService.getEnabledPathStrings()).thenReturn(List.of());

    // When
    ExecutionResult result = scheduleExecutorService.executeScheduledTasks();

    // Then
    assertThat(result.isSuccess()).isTrue();
    verify(trashService, never()).moveToTrash(anyList(), org.mockito.ArgumentMatchers.anyString());
  }
}
