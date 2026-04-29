package com.ifonly.museagent.scheduler;

import com.ifonly.museagent.dao.EchoNoteMessageDao;
import com.ifonly.museagent.dto.EchoNoteMessageDto;
import com.ifonly.museagent.service.EchoNoteMessageService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Echo Note 자동 발송 스케줄러 — Phase 3.
 *
 * <p>매시간 정각에 실행하며, {@code status=READY} + {@code scheduled_at <= now} 인 메시지를 echo-server 외부 API 로
 * 발송한다.
 *
 * <p>설계:
 *
 * <ul>
 *   <li>주기 — 매시간 (KST 기준 매 정각). 메시지의 {@code scheduled_at} 자체가 90~180일 후 임의 분 단위 시각이므로, 매시간 처리해도 발송
 *       시점이 자연스럽게 분산
 *   <li>실패 처리 — {@link EchoNoteMessageService#sendViaEcho} 가 IllegalStateException 을 던지면 잡아서 로그만
 *       남기고 status 유지 → 다음 시간에 재시도. 영구 실패 가능성 있는 메시지는 운영 관찰 후 수동 정리
 *   <li>중복 방지 — {@link EchoNoteMessageService#sendViaEcho} 내부에서 status==READY 검증. 동시에 두 스케줄러 인스턴스가
 *       실행되더라도 첫 번째가 status=SENT 로 바꾸면 두 번째는 IllegalStateException 발생
 *   <li>스케줄 정확도 — 사용자에게는 정확한 일자를 노출하지 않음. 매시간 ± 처리 지연 정도의 오차는 정책상 허용 (실제로 어느 시각에 도착했는지가 사용자 통제
 *       외이므로)
 * </ul>
 *
 * <p>비활성화: {@code app.scheduler.echo-note-auto-send.enabled=false} 로 OFF 가능 (테스트/유지보수용).
 *
 * @author if-only
 * @version 0.1.0
 */
@Component
@ConditionalOnProperty(
    name = "app.scheduler.echo-note-auto-send.enabled",
    havingValue = "true",
    matchIfMissing = true)
@Slf4j
public class EchoNoteAutoSendScheduler {

  private final EchoNoteMessageDao dao;
  private final EchoNoteMessageService messageService;

  @Autowired
  public EchoNoteAutoSendScheduler(EchoNoteMessageDao dao, EchoNoteMessageService messageService) {
    this.dao = dao;
    this.messageService = messageService;
    log.info("✓ EchoNoteAutoSendScheduler 초기화 완료 (매시간 정각 실행)");
  }

  /**
   * 매시간 정각 실행 (KST). 도래한 READY 메시지를 모두 발송 처리.
   *
   * <p>발송 실패는 개별 메시지 단위로 로그를 남기고 다음 메시지 처리를 계속한다 — 한 메시지 실패가 전체 배치를 막지 않도록.
   */
  @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
  public void runHourly() {
    LocalDateTime now = LocalDateTime.now();
    List<EchoNoteMessageDto> due = dao.findReadyDueForSending(now);
    if (due.isEmpty()) {
      log.debug("Echo-note auto-send: 도래한 메시지 없음 (now={})", now);
      return;
    }

    log.info("Echo-note auto-send 시작: {}건 처리 예정", due.size());
    int sent = 0;
    int failed = 0;
    for (EchoNoteMessageDto m : due) {
      try {
        messageService.sendViaEcho(m.getId());
        sent++;
        log.info("Echo-note auto-send 성공: id={}", m.getId());
      } catch (Exception e) {
        failed++;
        log.warn(
            "Echo-note auto-send 실패 (status 유지, 다음 시간에 재시도): id={}, reason={}",
            m.getId(),
            e.getMessage());
      }
    }
    log.info("Echo-note auto-send 완료: sent={}, failed={}, total={}", sent, failed, due.size());
  }
}
