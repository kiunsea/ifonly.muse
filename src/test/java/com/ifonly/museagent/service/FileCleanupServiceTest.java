package com.ifonly.museagent.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ifonly.museagent.service.FileCleanupService.CleanupResult;
import com.ifonly.museagent.service.FileCleanupService.VerificationResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for FileCleanupService
 *
 * @author if-only
 * @version 0.1.0
 */
@DisplayName("FileCleanupService Tests")
class FileCleanupServiceTest {

  private FileCleanupService fileCleanupService;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    fileCleanupService = new FileCleanupService();
  }

  @AfterEach
  void tearDown() {
    // TempDir cleanup is automatic
  }

  @Test
  @DisplayName("should delete a single file successfully")
  void testDeleteSingleFile() throws IOException {
    // Given
    Path testFile = tempDir.resolve("test-file.txt");
    Files.writeString(testFile, "Test content");

    // When
    CleanupResult result = fileCleanupService.executeCleanup(List.of(testFile.toString()));

    // Then
    assertThat(result.isAllSuccess()).isTrue();
    assertThat(result.getSuccessCount()).isEqualTo(1);
    assertThat(result.getFailureCount()).isEqualTo(0);
    assertThat(Files.exists(testFile)).isFalse();
  }

  @Test
  @DisplayName("should delete a directory recursively")
  void testDeleteDirectoryRecursively() throws IOException {
    // Given
    Path subDir = tempDir.resolve("subdir");
    Files.createDirectories(subDir);
    Files.writeString(subDir.resolve("file1.txt"), "Content 1");
    Files.writeString(subDir.resolve("file2.txt"), "Content 2");

    Path nestedDir = subDir.resolve("nested");
    Files.createDirectories(nestedDir);
    Files.writeString(nestedDir.resolve("nested-file.txt"), "Nested content");

    // When
    CleanupResult result = fileCleanupService.executeCleanup(List.of(subDir.toString()));

    // Then
    assertThat(result.isAllSuccess()).isTrue();
    assertThat(result.getSuccessCount()).isEqualTo(1);
    assertThat(Files.exists(subDir)).isFalse();

    var item = result.getItems().get(0);
    assertThat(item.isDirectory()).isTrue();
    assertThat(item.getFilesDeleted()).isEqualTo(3);
    assertThat(item.getDirectoriesDeleted()).isEqualTo(2);
  }

  @Test
  @DisplayName("should handle non-existent path gracefully")
  void testNonExistentPath() {
    // Given
    String nonExistentPath = tempDir.resolve("non-existent.txt").toString();

    // When
    CleanupResult result = fileCleanupService.executeCleanup(List.of(nonExistentPath));

    // Then
    assertThat(result.isAllSuccess()).isFalse();
    assertThat(result.getFailureCount()).isEqualTo(1);
    assertThat(result.getItems().get(0).getErrorMessage()).contains("does not exist");
  }

  @Test
  @DisplayName("should delete multiple paths")
  void testDeleteMultiplePaths() throws IOException {
    // Given
    Path file1 = tempDir.resolve("file1.txt");
    Path file2 = tempDir.resolve("file2.txt");
    Files.writeString(file1, "Content 1");
    Files.writeString(file2, "Content 2");

    // When
    CleanupResult result =
        fileCleanupService.executeCleanup(Arrays.asList(file1.toString(), file2.toString()));

    // Then
    assertThat(result.isAllSuccess()).isTrue();
    assertThat(result.getSuccessCount()).isEqualTo(2);
    assertThat(result.getTotalPaths()).isEqualTo(2);
    assertThat(Files.exists(file1)).isFalse();
    assertThat(Files.exists(file2)).isFalse();
  }

  @Test
  @DisplayName("should track total bytes deleted")
  void testBytesDeleted() throws IOException {
    // Given
    Path testFile = tempDir.resolve("sized-file.txt");
    String content = "X".repeat(1024); // 1KB of content
    Files.writeString(testFile, content);

    // When
    CleanupResult result = fileCleanupService.executeCleanup(List.of(testFile.toString()));

    // Then
    assertThat(result.isAllSuccess()).isTrue();
    assertThat(result.getTotalBytesDeleted()).isGreaterThanOrEqualTo(1024);
  }

  @Test
  @DisplayName("should verify paths correctly")
  void testVerifyPaths() throws IOException {
    // Given
    Path existingFile = tempDir.resolve("existing.txt");
    Files.writeString(existingFile, "Content");
    String nonExistentPath = tempDir.resolve("non-existent.txt").toString();

    // When
    VerificationResult result =
        fileCleanupService.verifyPaths(Arrays.asList(existingFile.toString(), nonExistentPath));

    // Then
    assertThat(result.getTotalPaths()).isEqualTo(2);
    assertThat(result.getExistingCount()).isEqualTo(1);
    assertThat(result.getWritableCount()).isEqualTo(1);

    var verifications = result.getVerifications();
    assertThat(verifications).hasSize(2);

    var existingVerification =
        verifications.stream().filter(v -> v.getPath().equals(existingFile.toString())).findFirst();
    assertThat(existingVerification).isPresent();
    assertThat(existingVerification.get().isExists()).isTrue();
    assertThat(existingVerification.get().isWritable()).isTrue();
  }

  @Test
  @DisplayName("should handle empty path list")
  void testEmptyPathList() {
    // When
    CleanupResult result = fileCleanupService.executeCleanup(List.of());

    // Then
    assertThat(result.getTotalPaths()).isEqualTo(0);
    assertThat(result.isAllSuccess()).isTrue();
  }

  @Test
  @DisplayName("should identify directory in verification")
  void testVerifyDirectory() throws IOException {
    // Given
    Path dir = tempDir.resolve("test-dir");
    Files.createDirectories(dir);

    // When
    VerificationResult result = fileCleanupService.verifyPaths(List.of(dir.toString()));

    // Then
    var verification = result.getVerifications().get(0);
    assertThat(verification.isDirectory()).isTrue();
    assertThat(verification.isExists()).isTrue();
  }
}
