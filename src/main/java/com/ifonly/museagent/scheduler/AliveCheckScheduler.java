package com.ifonly.museagent.scheduler;

import com.ifonly.museagent.client.EchoServerClient;
import com.ifonly.museagent.dto.AliveStatusResponse;
import com.ifonly.museagent.service.AliveCheckService;
import com.ifonly.museagent.service.AppSettingService;
import com.ifonly.museagent.service.ScheduleExecutorService;
import com.ifonly.museagent.service.TaskExecutionHistoryService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

/**
 * Alive 상태 확인 스케줄러
 *
 * <p>정기적으로 Echo Server의 alive_event 상태를 조회하여 EXPIRED 지속 시간을 모니터링합니다.
 *
 * <p>폴링 주기는 DB(app_setting) 설정으로 런타임에 변경할 수 있습니다. 변경 사항은 다음 실행 주기부터 반영됩니다.
 *
 * @author if-only
 * @version 0.18.0
 */
@Component
@EnableScheduling
@ConditionalOnProperty(
    name = "app.scheduler.alive-check.enabled",
    havingValue = "true",
    matchIfMissing = true)
@Slf4j
public class AliveCheckScheduler implements SchedulingConfigurer {

  private final EchoServerClient echoServerClient;
  private final AliveCheckService aliveCheckService;
  private final AppSettingService appSettingService;
  private final ScheduleExecutorService scheduleExecutorService;
  private final TaskExecutionHistoryService taskExecutionHistoryService;

  @Autowired
  public AliveCheckScheduler(
      EchoServerClient echoServerClient,
      AliveCheckService aliveCheckService,
      AppSettingService appSettingService,
      ScheduleExecutorService scheduleExecutorService,
      TaskExecutionHistoryService taskExecutionHistoryService) {
    this.echoServerClient = echoServerClient;
    this.aliveCheckService = aliveCheckService;
    this.appSettingService = appSettingService;
    this.scheduleExecutorService = scheduleExecutorService;
    this.taskExecutionHistoryService = taskExecutionHistoryService;
    log.info("✓ AliveCheckScheduler 초기화 완료");
  }

  @Override
  public void configureTasks(ScheduledTaskRegistrar registrar) {
    registrar.addTriggerTask(
        this::checkAliveStatus,
        context -> {
          long intervalMs = appSettingService.getAliveCheckIntervalMs();
          Instant lastExecution = context.lastScheduledExecution();
          if (lastExecution == null) {
            // 최초 실행: 즉시 실행
            return Instant.now();
          }
          return lastExecution.plusMillis(intervalMs);
        });
  }

  /**
   * Alive 상태 확인 (SchedulingConfigurer Trigger에 의해 동적 주기로 실행)
   *
   * <p>실행 주기는 DB의 alive.check.interval.ms 설정으로 제어됩니다.
   */
  public void checkAliveStatus() {
    log.debug("Alive 상태 확인 시작 (interval={}ms)", appSettingService.getAliveCheckIntervalMs());
    String executionId = UUID.randomUUID().toString();
    LocalDateTime startedAt = LocalDateTime.now();
    try {
      AliveStatusResponse status = echoServerClient.getAliveStatus();
      aliveCheckService.processAliveStatus(status);
      taskExecutionHistoryService.record(
          executionId,
          "SCHEDULED_TASKS",
          "ALIVE_CHECK_POLL",
          "Alive Check Poll",
          "SUCCESS",
          true,
          1,
          1,
          0,
          startedAt,
          LocalDateTime.now(),
          "{\"aliveStatus\":\"" + status.getStatus() + "\"}",
          null);
    } catch (Exception e) {
      log.error("✗ Alive 상태 확인 실패: {}", e.getMessage());
      taskExecutionHistoryService.record(
          executionId,
          "SCHEDULED_TASKS",
          "ALIVE_CHECK_POLL",
          "Alive Check Poll",
          "FAILED",
          false,
          1,
          0,
          1,
          startedAt,
          LocalDateTime.now(),
          null,
          e.getMessage());
    } finally {
      scheduleExecutorService.purgeExpiredTrash();
    }
  }
}
