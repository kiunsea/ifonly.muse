package com.ifonly.museagent.service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * File cleanup service for deleting specified files and directories
 *
 * <p>Executes file cleanup tasks when user enters locked state.
 *
 * @author if-only
 * @version 0.1.0
 */
@Service
@Slf4j
public class FileCleanupService {

  /**
   * Execute cleanup for specified paths
   *
   * @param paths list of file/directory paths to delete
   * @return cleanup result
   */
  public CleanupResult executeCleanup(List<String> paths) {
    log.info("Starting file cleanup for {} path(s)", paths.size());
    LocalDateTime startTime = LocalDateTime.now();

    List<CleanupItem> results = new ArrayList<>();
    int successCount = 0;
    int failureCount = 0;
    long totalBytesDeleted = 0;

    for (String pathStr : paths) {
      CleanupItem item = cleanupPath(pathStr);
      results.add(item);

      if (item.isSuccess()) {
        successCount++;
        totalBytesDeleted += item.getBytesDeleted();
      } else {
        failureCount++;
      }
    }

    LocalDateTime endTime = LocalDateTime.now();

    CleanupResult result =
        CleanupResult.builder()
            .startTime(startTime)
            .endTime(endTime)
            .totalPaths(paths.size())
            .successCount(successCount)
            .failureCount(failureCount)
            .totalBytesDeleted(totalBytesDeleted)
            .items(results)
            .build();

    log.info(
        "File cleanup completed: {} success, {} failure, {} bytes deleted",
        successCount,
        failureCount,
        totalBytesDeleted);

    return result;
  }

  /**
   * Cleanup a single path (file or directory)
   *
   * @param pathStr path to clean up
   * @return cleanup item result
   */
  private CleanupItem cleanupPath(String pathStr) {
    Path path = Paths.get(pathStr);
    CleanupItem.CleanupItemBuilder itemBuilder =
        CleanupItem.builder().path(pathStr).timestamp(LocalDateTime.now());

    if (!Files.exists(path)) {
      log.warn("Path does not exist: {}", pathStr);
      return itemBuilder.success(false).errorMessage("Path does not exist").build();
    }

    try {
      if (Files.isDirectory(path)) {
        return deleteDirectory(path, itemBuilder);
      } else {
        return deleteFile(path, itemBuilder);
      }
    } catch (IOException e) {
      log.error("Failed to delete path: {}", pathStr, e);
      return itemBuilder.success(false).errorMessage(e.getMessage()).build();
    } catch (SecurityException e) {
      log.error("Permission denied for path: {}", pathStr, e);
      return itemBuilder
          .success(false)
          .errorMessage("Permission denied: " + e.getMessage())
          .build();
    }
  }

  /**
   * Delete a single file
   *
   * @param path file path
   * @param itemBuilder cleanup item builder
   * @return cleanup item result
   * @throws IOException on deletion failure
   */
  private CleanupItem deleteFile(Path path, CleanupItem.CleanupItemBuilder itemBuilder)
      throws IOException {
    long fileSize = Files.size(path);
    Files.delete(path);

    log.debug("Deleted file: {} ({} bytes)", path, fileSize);

    return itemBuilder
        .success(true)
        .isDirectory(false)
        .filesDeleted(1)
        .bytesDeleted(fileSize)
        .build();
  }

  /**
   * Delete a directory and all its contents recursively
   *
   * @param path directory path
   * @param itemBuilder cleanup item builder
   * @return cleanup item result
   * @throws IOException on deletion failure
   */
  private CleanupItem deleteDirectory(Path path, CleanupItem.CleanupItemBuilder itemBuilder)
      throws IOException {
    AtomicInteger fileCount = new AtomicInteger(0);
    AtomicInteger dirCount = new AtomicInteger(0);
    final long[] totalSize = {0};

    Files.walkFileTree(
        path,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            totalSize[0] += attrs.size();
            Files.delete(file);
            fileCount.incrementAndGet();
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFileFailed(Path file, IOException exc) {
            log.warn("Failed to access file: {}", file, exc);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (exc == null) {
              Files.delete(dir);
              dirCount.incrementAndGet();
              return FileVisitResult.CONTINUE;
            } else {
              throw exc;
            }
          }
        });

    log.debug(
        "Deleted directory: {} ({} files, {} dirs, {} bytes)",
        path,
        fileCount.get(),
        dirCount.get(),
        totalSize[0]);

    return itemBuilder
        .success(true)
        .isDirectory(true)
        .filesDeleted(fileCount.get())
        .directoriesDeleted(dirCount.get())
        .bytesDeleted(totalSize[0])
        .build();
  }

  /**
   * Verify if cleanup can be executed for given paths
   *
   * @param paths list of paths to verify
   * @return verification result
   */
  public VerificationResult verifyPaths(List<String> paths) {
    List<PathVerification> verifications = new ArrayList<>();
    int existingCount = 0;
    int readableCount = 0;
    int writableCount = 0;

    for (String pathStr : paths) {
      Path path = Paths.get(pathStr);
      boolean exists = Files.exists(path);
      boolean readable = Files.isReadable(path);
      boolean writable = Files.isWritable(path);

      if (exists) existingCount++;
      if (readable) readableCount++;
      if (writable) writableCount++;

      verifications.add(
          PathVerification.builder()
              .path(pathStr)
              .exists(exists)
              .readable(readable)
              .writable(writable)
              .isDirectory(exists && Files.isDirectory(path))
              .build());
    }

    return VerificationResult.builder()
        .totalPaths(paths.size())
        .existingCount(existingCount)
        .readableCount(readableCount)
        .writableCount(writableCount)
        .verifications(verifications)
        .build();
  }

  /** Cleanup result containing summary and individual item results */
  @Data
  @Builder
  public static class CleanupResult {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int totalPaths;
    private int successCount;
    private int failureCount;
    private long totalBytesDeleted;
    private List<CleanupItem> items;

    public boolean isAllSuccess() {
      return failureCount == 0;
    }
  }

  /** Individual cleanup item result */
  @Data
  @Builder
  public static class CleanupItem {
    private String path;
    private LocalDateTime timestamp;
    private boolean success;
    private boolean isDirectory;
    private int filesDeleted;
    private int directoriesDeleted;
    private long bytesDeleted;
    private String errorMessage;
  }

  /** Path verification result */
  @Data
  @Builder
  public static class VerificationResult {
    private int totalPaths;
    private int existingCount;
    private int readableCount;
    private int writableCount;
    private List<PathVerification> verifications;

    public boolean isAllWritable() {
      return writableCount == totalPaths;
    }
  }

  /** Individual path verification */
  @Data
  @Builder
  public static class PathVerification {
    private String path;
    private boolean exists;
    private boolean readable;
    private boolean writable;
    private boolean isDirectory;
  }
}
