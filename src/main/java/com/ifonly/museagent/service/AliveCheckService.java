package com.ifonly.museagent.service;

import com.ifonly.museagent.dto.AliveStatusResponse;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Alive 상태 확인 서비스
 *
 * <p>Echo Server의 alive_event 상태가 EXPIRED로 감지되면 first_trigger를 ON으로 전환합니다. first_trigger가 ON인 상태에서
 * EXPIRED가 다시 감지되고, first_trigger ON 이후 경과 시간이 실행 임계값(일)에 도달하면 스케줄을 실행합니다.
 *
 * <p>안전 장치: 첫 EXPIRED에서는 실행하지 않고, 두 번째 EXPIRED부터 실행 조건을 평가합니다.
 *
 * <p>임계값(execution-threshold-days)은 DB(app_setting) 설정으로 런타임에 변경할 수 있습니다.
 *
 * @author if-only
 * @version 0.19.0
 */
@Service
@Slf4j
public class AliveCheckService {

  private final ScheduleExecutorService scheduleExecutorService;
  private final AppSettingService appSettingService;

  // 첫 EXPIRED 감지 후 트리거 상태
  private boolean firstTriggerOn = false;

  // firstTriggerOn=true 로 전환된 시각
  private LocalDateTime firstTriggerOnAt;

  @Autowired
  public AliveCheckService(
      ScheduleExecutorService scheduleExecutorService, AppSettingService appSettingService) {
    this.scheduleExecutorService = scheduleExecutorService;
    this.appSettingService = appSettingService;
  }

  /**
   * Alive 상태 처리
   *
   * @param aliveStatus Echo Server로부터 조회한 alive 상태
   */
  public void processAliveStatus(AliveStatusResponse aliveStatus) {
    if (aliveStatus == null || aliveStatus.getStatus() == null) {
      log.warn("Alive 상태 응답이 null입니다");
      return;
    }

    LocalDateTime now = LocalDateTime.now();

    if (!"EXPIRED".equals(aliveStatus.getStatus())) {
      log.debug("Alive 상태 정상: status={}", aliveStatus.getStatus());
      if (firstTriggerOn) {
        firstTriggerOn = false;
        firstTriggerOnAt = null;
        log.info("✓ Alive 상태 정상화 - first_trigger OFF");
      }
      if (scheduleExecutorService.isTasksExecuted()) {
        scheduleExecutorService.resetExecutionStatus();
        log.info("✓ Alive 상태 정상화 - 스케줄 실행 상태 초기화");
      }
      return;
    }

    if (!firstTriggerOn) {
      firstTriggerOn = true;
      firstTriggerOnAt = now;
      log.warn("✗ EXPIRED 첫 감지 - first_trigger ON, onAt={}", firstTriggerOnAt);
      return;
    }

    if (firstTriggerOnAt == null) {
      // 방어 코드: 비정상 상태에서는 현재 시각으로 복구하고 실행은 다음 체크로 미룹니다.
      firstTriggerOnAt = now;
      log.warn("first_trigger ON 상태지만 기준 시각이 없어 현재 시각으로 복구: {}", firstTriggerOnAt);
      return;
    }

    int threshold = appSettingService.getAliveCheckThresholdDays();
    LocalDateTime thresholdReachedAt = firstTriggerOnAt.plusDays(threshold);
    boolean thresholdReached = !now.isBefore(thresholdReachedAt);

    log.info(
        "Alive EXPIRED 재감지: firstTriggerOnAt={}, threshold={}일, thresholdReachedAt={}, thresholdReached={}",
        firstTriggerOnAt,
        threshold,
        thresholdReachedAt,
        thresholdReached);

    if (thresholdReached) {
      log.warn("✗ EXPIRED 재감지 + 임계 도달 ({}일) - 스케줄 실행", threshold);
      scheduleExecutorService.executeScheduledTasks();
    } else {
      log.info("EXPIRED 재감지 되었지만 임계값({}일) 미도달 - 스케줄 대기", threshold);
    }
  }
}
