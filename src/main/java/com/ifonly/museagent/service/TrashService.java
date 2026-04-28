package com.ifonly.museagent.service;

import com.ifonly.museagent.dao.TrashItemDao;
import com.ifonly.museagent.dto.TrashItemDto;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Moves cleanup targets to trash storage instead of immediate deletion. */
@Service
@Slf4j
public class TrashService {

  private final AppSettingService appSettingService;
  private final TrashItemDao trashItemDao;

  @Autowired
  public TrashService(AppSettingService appSettingService, TrashItemDao trashItemDao) {
    this.appSettingService = appSettingService;
    this.trashItemDao = trashItemDao;
  }

  public TrashMoveResult moveToTrash(List<String> paths, String executionId) {
    LocalDateTime startTime = LocalDateTime.now();
    LocalDateTime endTime;

    List<TrashMoveItem> items = new ArrayList<>();
    int successCount = 0;
    int failureCount = 0;
    long totalBytesMoved = 0L;

    for (String pathStr : paths) {
      TrashMoveItem item = moveSingle(pathStr, executionId);
      items.add(item);
      if (item.isSuccess()) {
        successCount++;
        totalBytesMoved += item.getBytesMoved();
      } else {
        failureCount++;
      }
    }

    endTime = LocalDateTime.now();

    return TrashMoveResult.builder()
        .startTime(startTime)
        .endTime(endTime)
        .totalPaths(paths.size())
        .successCount(successCount)
        .failureCount(failureCount)
        .totalBytesMoved(totalBytesMoved)
        .items(items)
        .build();
  }

  public List<TrashItemDto> getRecentItems(int limit, String status) {
    return trashItemDao.findRecent(limit, status);
  }

  public TrashStats getStats() {
    LocalDateTime now = LocalDateTime.now();
    return TrashStats.builder()
        .movedCount(trashItemDao.countByStatus("MOVED"))
        .deletedCount(trashItemDao.countByStatus("DELETED"))
        .restoredCount(trashItemDao.countByStatus("RESTORED"))
        .deleteFailedCount(trashItemDao.countByStatus("DELETE_FAILED"))
        .purgeCandidateCount(trashItemDao.countPurgeCandidates(now))
        .retentionDays(appSettingService.getCleanupTrashRetentionDays())
        .trashRootPath(resolveTrashRoot().toString())
        .build();
  }

  public TrashRestoreResult restoreItem(long trashItemId) {
    TrashItemDto item =
        trashItemDao
            .findById(trashItemId)
            .orElseThrow(
                () -> new IllegalArgumentException("Trash item not found: id=" + trashItemId));

    if (!"MOVED".equalsIgnoreCase(item.getStatus())
        && !"DELETE_FAILED".equalsIgnoreCase(item.getStatus())) {
      throw new IllegalArgumentException(
          "Trash item is not restorable: status=" + item.getStatus());
    }

    Path trashPath = Paths.get(item.getTrashPath());
    Path originalPath = Paths.get(item.getOriginalPath());

    if (!Files.exists(trashPath)) {
      return TrashRestoreResult.builder()
          .trashItemId(trashItemId)
          .originalPath(item.getOriginalPath())
          .trashPath(item.getTrashPath())
          .success(false)
          .errorMessage("Trash path does not exist")
          .build();
    }

    if (Files.exists(originalPath)) {
      return TrashRestoreResult.builder()
          .trashItemId(trashItemId)
          .originalPath(item.getOriginalPath())
          .trashPath(item.getTrashPath())
          .success(false)
          .errorMessage("Original path already exists")
          .build();
    }

    try {
      Path parent = originalPath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }

      Files.move(trashPath, originalPath);
      trashItemDao.markRestored(trashItemId);

      return TrashRestoreResult.builder()
          .trashItemId(trashItemId)
          .originalPath(item.getOriginalPath())
          .trashPath(item.getTrashPath())
          .success(true)
          .build();
    } catch (IOException e) {
      return TrashRestoreResult.builder()
          .trashItemId(trashItemId)
          .originalPath(item.getOriginalPath())
          .trashPath(item.getTrashPath())
          .success(false)
          .errorMessage(e.getMessage())
          .build();
    }
  }

  private TrashMoveItem moveSingle(String pathStr, String executionId) {
    LocalDateTime movedAt = LocalDateTime.now();
    TrashMoveItem.TrashMoveItemBuilder itemBuilder =
        TrashMoveItem.builder().originalPath(pathStr).movedAt(movedAt);

    Path sourcePath = Paths.get(pathStr);
    if (!Files.exists(sourcePath)) {
      return itemBuilder.success(false).errorMessage("Path does not exist").build();
    }

    try {
      boolean directory = Files.isDirectory(sourcePath);
      String itemType = directory ? "DIRECTORY" : "FILE";
      long sizeBytes = calculateSize(sourcePath);
      Path trashPath = buildTrashPath(sourcePath);

      Files.createDirectories(trashPath.getParent());
      try {
        Files.move(sourcePath, trashPath, StandardCopyOption.REPLACE_EXISTING);
      } catch (DirectoryNotEmptyException e) {
        // Same name collision in rare cases; create a fallback unique destination.
        trashPath = buildTrashPath(sourcePath);
        Files.move(sourcePath, trashPath, StandardCopyOption.REPLACE_EXISTING);
      }

      LocalDateTime expireAt = movedAt.plusDays(appSettingService.getCleanupTrashRetentionDays());
      trashItemDao.saveMoved(
          TrashItemDto.builder()
              .executionId(executionId)
              .originalPath(sourcePath.toString())
              .trashPath(trashPath.toString())
              .itemType(itemType)
              .sizeBytes(sizeBytes)
              .movedAt(movedAt)
              .expireAt(expireAt)
              .status("MOVED")
              .deleteAttempts(0)
              .build());

      return itemBuilder
          .success(true)
          .trashPath(trashPath.toString())
          .isDirectory(directory)
          .bytesMoved(sizeBytes)
          .expireAt(expireAt)
          .build();
    } catch (IOException e) {
      log.warn("Trash move failed for {}: {}", pathStr, e.getMessage());
      return itemBuilder.success(false).errorMessage(e.getMessage()).build();
    } catch (SecurityException e) {
      log.warn("Trash move permission denied for {}: {}", pathStr, e.getMessage());
      return itemBuilder
          .success(false)
          .errorMessage("Permission denied: " + e.getMessage())
          .build();
    }
  }

  private Path resolveTrashRoot() {
    return Paths.get(appSettingService.getCleanupTrashRootPath()).toAbsolutePath().normalize();
  }

  private Path buildTrashPath(Path sourcePath) {
    Path trashRoot = resolveTrashRoot();
    LocalDate now = LocalDate.now();
    String baseName =
        sourcePath.getFileName() == null ? "unknown" : sourcePath.getFileName().toString();
    String uniqueName = UUID.randomUUID() + "_" + baseName;
    return trashRoot
        .resolve(String.valueOf(now.getYear()))
        .resolve(String.format("%02d", now.getMonthValue()))
        .resolve(String.format("%02d", now.getDayOfMonth()))
        .resolve(uniqueName);
  }

  private long calculateSize(Path path) throws IOException {
    if (!Files.exists(path)) {
      return 0L;
    }
    if (Files.isRegularFile(path)) {
      return Files.size(path);
    }

    final long[] size = {0L};
    Files.walkFileTree(
        path,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            size[0] += attrs.size();
            return FileVisitResult.CONTINUE;
          }
        });
    return size[0];
  }

  @Data
  @Builder
  public static class TrashMoveResult {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int totalPaths;
    private int successCount;
    private int failureCount;
    private long totalBytesMoved;
    private List<TrashMoveItem> items;

    public boolean isAllSuccess() {
      return failureCount == 0;
    }
  }

  @Data
  @Builder
  public static class TrashMoveItem {
    private String originalPath;
    private String trashPath;
    private LocalDateTime movedAt;
    private LocalDateTime expireAt;
    private boolean success;
    private boolean isDirectory;
    private long bytesMoved;
    private String errorMessage;
  }

  @Data
  @Builder
  public static class TrashStats {
    private int movedCount;
    private int deletedCount;
    private int restoredCount;
    private int deleteFailedCount;
    private int purgeCandidateCount;
    private int retentionDays;
    private String trashRootPath;
  }

  @Data
  @Builder
  public static class TrashRestoreResult {
    private long trashItemId;
    private String originalPath;
    private String trashPath;
    private boolean success;
    private String errorMessage;
  }
}
