package com.ifonly.museagent.service;

import com.ifonly.museagent.dao.TrashItemDao;
import com.ifonly.museagent.dto.TrashItemDto;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Permanently deletes expired trash items. */
@Service
@Slf4j
public class TrashPurgeService {

  private final TrashItemDao trashItemDao;
  private final AppSettingService appSettingService;

  @Autowired
  public TrashPurgeService(TrashItemDao trashItemDao, AppSettingService appSettingService) {
    this.trashItemDao = trashItemDao;
    this.appSettingService = appSettingService;
  }

  public PurgeResult purgeExpired() {
    LocalDateTime startedAt = LocalDateTime.now();
    int batchSize = appSettingService.getCleanupTrashPurgeBatchSize();
    List<TrashItemDto> candidates = trashItemDao.findExpiredForPurge(startedAt, batchSize);

    int successCount = 0;
    int failureCount = 0;

    for (TrashItemDto item : candidates) {
      try {
        deletePath(Paths.get(item.getTrashPath()));
        trashItemDao.markDeleted(item.getId(), LocalDateTime.now());
        successCount++;
      } catch (Exception e) {
        trashItemDao.markDeleteFailed(item.getId(), e.getMessage());
        failureCount++;
      }
    }

    return PurgeResult.builder()
        .startedAt(startedAt)
        .completedAt(LocalDateTime.now())
        .targetCount(candidates.size())
        .successCount(successCount)
        .failureCount(failureCount)
        .build();
  }

  private void deletePath(Path path) throws IOException {
    if (!Files.exists(path)) {
      return;
    }

    if (Files.isRegularFile(path)) {
      Files.delete(path);
      return;
    }

    Files.walkFileTree(
        path,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (exc != null) {
              throw exc;
            }
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  @Data
  @Builder
  public static class PurgeResult {
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private int targetCount;
    private int successCount;
    private int failureCount;

    public boolean isAllSuccess() {
      return failureCount == 0;
    }
  }
}
