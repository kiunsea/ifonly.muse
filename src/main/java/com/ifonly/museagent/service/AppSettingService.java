package com.ifonly.museagent.service;

import com.ifonly.museagent.dao.AppSettingDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * App Setting Service
 *
 * <p>alive-check 스케줄러의 interval, threshold 등 런타임에 변경 가능한 설정을 DB에 저장하고 조회합니다.
 *
 * @author if-only
 * @version 0.18.0
 */
@Service
@Slf4j
public class AppSettingService {

  static final String KEY_ALIVE_CHECK_INTERVAL_MS = "alive.check.interval.ms";
  static final String KEY_ALIVE_CHECK_THRESHOLD_DAYS = "alive.check.threshold.days";
  static final String KEY_CLEANUP_TRASH_RETENTION_DAYS = "cleanup.trash.retention.days";
  static final String KEY_CLEANUP_TRASH_ROOT_PATH = "cleanup.trash.root.path";
  static final String KEY_CLEANUP_TRASH_PURGE_BATCH_SIZE = "cleanup.trash.purge.batch-size";

  private final AppSettingDao appSettingDao;

  @Value("${app.scheduler.alive-check.interval-ms:600000}")
  private long defaultIntervalMs;

  @Value("${app.scheduler.alive-check.execution-threshold-days:7}")
  private int defaultThresholdDays;

  @Value("${app.cleanup.trash.retention-days:30}")
  private int defaultTrashRetentionDays;

  @Value("${app.cleanup.trash.root-path:data/trash}")
  private String defaultTrashRootPath;

  @Value("${app.cleanup.trash.purge-batch-size:200}")
  private int defaultTrashPurgeBatchSize;

  @Autowired
  public AppSettingService(AppSettingDao appSettingDao) {
    this.appSettingDao = appSettingDao;
  }

  /** alive-check 폴링 주기 (ms) — DB 우선, 없으면 application.yml 기본값 */
  public long getAliveCheckIntervalMs() {
    return appSettingDao
        .getValue(KEY_ALIVE_CHECK_INTERVAL_MS)
        .map(v -> parseLong(v, defaultIntervalMs))
        .orElse(defaultIntervalMs);
  }

  /** alive-check 실행 임계값 (일) — DB 우선, 없으면 application.yml 기본값 */
  public int getAliveCheckThresholdDays() {
    return appSettingDao
        .getValue(KEY_ALIVE_CHECK_THRESHOLD_DAYS)
        .map(v -> parseInt(v, defaultThresholdDays))
        .orElse(defaultThresholdDays);
  }

  public void setAliveCheckIntervalMs(long intervalMs) {
    appSettingDao.setValue(
        KEY_ALIVE_CHECK_INTERVAL_MS, String.valueOf(intervalMs), "alive-check 폴링 주기 (ms)");
    log.info("alive-check interval 갱신: {}ms", intervalMs);
  }

  public void setAliveCheckThresholdDays(int thresholdDays) {
    appSettingDao.setValue(
        KEY_ALIVE_CHECK_THRESHOLD_DAYS, String.valueOf(thresholdDays), "EXPIRED 지속 임계값 (일)");
    log.info("alive-check threshold 갱신: {}일", thresholdDays);
  }

  /** 휴지통 보관 기간(일) */
  public int getCleanupTrashRetentionDays() {
    return appSettingDao
        .getValue(KEY_CLEANUP_TRASH_RETENTION_DAYS)
        .map(v -> Math.max(parseInt(v, defaultTrashRetentionDays), 1))
        .orElse(Math.max(defaultTrashRetentionDays, 1));
  }

  /** 휴지통 루트 경로 */
  public String getCleanupTrashRootPath() {
    return appSettingDao
        .getValue(KEY_CLEANUP_TRASH_ROOT_PATH)
        .map(String::trim)
        .filter(v -> !v.isBlank())
        .orElse(defaultTrashRootPath);
  }

  /** 만료 삭제 배치 크기 */
  public int getCleanupTrashPurgeBatchSize() {
    return appSettingDao
        .getValue(KEY_CLEANUP_TRASH_PURGE_BATCH_SIZE)
        .map(v -> Math.max(parseInt(v, defaultTrashPurgeBatchSize), 1))
        .orElse(Math.max(defaultTrashPurgeBatchSize, 1));
  }

  public void setCleanupTrashRetentionDays(int retentionDays) {
    int normalized = Math.max(retentionDays, 1);
    appSettingDao.setValue(
        KEY_CLEANUP_TRASH_RETENTION_DAYS, String.valueOf(normalized), "휴지통 보관 기간 (일)");
    log.info("cleanup trash retention 갱신: {}일", normalized);
  }

  public void setCleanupTrashRootPath(String rootPath) {
    if (rootPath == null || rootPath.isBlank()) {
      throw new IllegalArgumentException("trash root path must not be empty");
    }
    appSettingDao.setValue(KEY_CLEANUP_TRASH_ROOT_PATH, rootPath.trim(), "휴지통 루트 경로");
    log.info("cleanup trash root path 갱신: {}", rootPath.trim());
  }

  public void setCleanupTrashPurgeBatchSize(int batchSize) {
    int normalized = Math.max(batchSize, 1);
    appSettingDao.setValue(
        KEY_CLEANUP_TRASH_PURGE_BATCH_SIZE, String.valueOf(normalized), "휴지통 만료 삭제 배치 크기");
    log.info("cleanup trash purge batch size 갱신: {}", normalized);
  }

  private long parseLong(String value, long fallback) {
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException e) {
      log.warn("설정값 파싱 실패 (long): '{}' — 기본값 {}ms 사용", value, fallback);
      return fallback;
    }
  }

  private int parseInt(String value, int fallback) {
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      log.warn("설정값 파싱 실패 (int): '{}' — 기본값 {}일 사용", value, fallback);
      return fallback;
    }
  }
}
