package com.ifonly.museagent.controller;

import com.ifonly.museagent.dto.EchoNoteMessageDto;
import com.ifonly.museagent.service.ChangelogVersionService;
import com.ifonly.museagent.service.EchoContinuationExchangeService;
import com.ifonly.museagent.service.EchoCredentialUpdater;
import com.ifonly.museagent.service.EchoNoteMessageService;
import com.ifonly.museagent.service.SecureConfigService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Echo Note 보관함 컨트롤러 — 페이지 + REST 엔드포인트 통합.
 *
 * <p>muse-agent 의 일반 패턴 (단일 ApiController 에 REST 모음 + WebController 에 페이지 모음) 과 다르게, Echo Note 는
 * 자기완결성이 높고 Phase 2 에서 echo-server 통신이 추가될 예정이라 별도 컨트롤러로 분리. 한 클래스 안에 페이지 라우트 ({@code GET
 * /echo-note}) 와 REST ({@code /api/echo-note-messages/*}) 를 함께 둠으로써 슬라이스 단일화.
 *
 * @author if-only
 * @version 0.1.0
 */
@Controller
@Slf4j
public class EchoNoteMessageController {

  private static final String VIEW_ECHO_NOTE = "echo-note";

  private final EchoNoteMessageService service;
  private final ChangelogVersionService changelogVersionService;
  private final EchoContinuationExchangeService continuationExchangeService;
  private final SecureConfigService secureConfigService;
  private final EchoCredentialUpdater credentialUpdater;
  private final com.ifonly.museagent.config.EchoUrlWhitelist echoUrlWhitelist;

  @Value("${spring.application.name:muse-agent}")
  private String applicationName;

  @Autowired
  public EchoNoteMessageController(
      EchoNoteMessageService service,
      ChangelogVersionService changelogVersionService,
      EchoContinuationExchangeService continuationExchangeService,
      SecureConfigService secureConfigService,
      EchoCredentialUpdater credentialUpdater,
      com.ifonly.museagent.config.EchoUrlWhitelist echoUrlWhitelist) {
    this.service = service;
    this.changelogVersionService = changelogVersionService;
    this.continuationExchangeService = continuationExchangeService;
    this.secureConfigService = secureConfigService;
    this.credentialUpdater = credentialUpdater;
    this.echoUrlWhitelist = echoUrlWhitelist;
  }

  // ---------------------------------------------------------------------------
  // 페이지
  // ---------------------------------------------------------------------------

  @GetMapping("/echo-note")
  public String echoNotePage(
      @RequestParam(value = "from", required = false) String from,
      @RequestParam(value = "token", required = false) String token,
      @RequestParam(value = "echoUrl", required = false) String echoUrl,
      Model model) {
    model.addAttribute("appName", applicationName);
    model.addAttribute("appVersion", changelogVersionService.getLatestVersion());

    // Phase 5b: echo popup 의 PC 보관 경로에서 continuation token 을 동봉해 진입한 경우
    //   token 을 echo 의 exchange 엔드포인트로 보내 OAuth client credential 자동 발급받아
    //   secure-config 에 저장. 새 자격증명 적용을 위해선 muse-agent 재시작 필요.
    if ("echo".equals(from) && token != null && !token.isBlank()) {
      handleContinuationFromEcho(token, echoUrl, model);
    }

    List<EchoNoteMessageDto> messages = service.getAll();
    model.addAttribute("messages", messages);
    model.addAttribute("totalCount", messages.size());
    model.addAttribute("readyCount", service.getReadyCount());
    return VIEW_ECHO_NOTE;
  }

  /**
   * Phase 5b — echo continuation token 처리 흐름.
   *
   * <p>실패해도 echo-note 페이지 자체는 정상 렌더링 (graceful degrade). 사용자에게 결과를 model attribute 로 알려 banner 로
   * 표시.
   */
  private void handleContinuationFromEcho(String token, String echoUrl, Model model) {
    if (echoUrl == null || echoUrl.isBlank()) {
      log.warn("Continuation token received without echoUrl — cannot exchange");
      model.addAttribute("welcomeError", "echoUrl 누락");
      return;
    }
    // v1.7.0: 화이트리스트 검증 + 정규화 — 가짜 popup 이 가짜 echoUrl 동봉으로 자격증명 끌어오는 phishing 차단.
    String normalizedEchoUrl;
    try {
      normalizedEchoUrl = echoUrlWhitelist.validateAndNormalize(echoUrl);
    } catch (IllegalArgumentException e) {
      log.warn("Continuation token rejected: echoUrl not in whitelist: {}", echoUrl);
      model.addAttribute("welcomeError", e.getMessage());
      return;
    }
    try {
      EchoContinuationExchangeService.ContinuationCredential cred =
          continuationExchangeService.exchange(normalizedEchoUrl, token);
      // secure-config 에 정규화된 echo URL + clientId + clientSecret 저장. 다음 echo 호출부터 적용 (Dynamic
      // Repository).
      secureConfigService.setSecureValue("echo.server.url", normalizedEchoUrl);
      secureConfigService.setSecureValue("echo.server.client.id", cred.clientId());
      secureConfigService.setSecureValue("echo.server.client.secret", cred.clientSecret());

      // v1.7.0: 자격증명 캐시 즉시 무효화 → 재시작 없이 다음 echo 호출부터 새 자격증명 적용.
      credentialUpdater.onCredentialsChanged();

      model.addAttribute("welcomeFromEcho", true);
      model.addAttribute("welcomeUserEmail", cred.userEmail());
      log.info(
          "Continuation token applied: userEmail={}, credentials hot-reloaded.", cred.userEmail());
    } catch (Exception e) {
      log.error("Continuation token processing failed", e);
      String reason = e.getMessage();
      model.addAttribute("welcomeError", reason == null ? "교환 실패" : reason);
    }
  }

  // ---------------------------------------------------------------------------
  // REST
  // ---------------------------------------------------------------------------

  @GetMapping("/api/echo-note-messages")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> list() {
    Map<String, Object> result = new HashMap<>();
    try {
      List<EchoNoteMessageDto> data = service.getAll();
      result.put("success", true);
      result.put("data", data);
      result.put("count", data.size());
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("echo-note list failed", e);
      return errorResponse(e);
    }
  }

  @GetMapping("/api/echo-note-messages/{id}")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> get(@PathVariable Long id) {
    Map<String, Object> result = new HashMap<>();
    try {
      EchoNoteMessageDto dto = service.getById(id);
      result.put("success", true);
      result.put("data", dto);
      return ResponseEntity.ok(result);
    } catch (IllegalArgumentException e) {
      return badRequestResponse(e);
    } catch (Exception e) {
      log.error("echo-note get failed: id={}", id, e);
      return errorResponse(e);
    }
  }

  @PostMapping("/api/echo-note-messages")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, String> body) {
    Map<String, Object> result = new HashMap<>();
    try {
      EchoNoteMessageDto saved =
          service.create(
              body.get("recipientEmail"),
              body.get("originalMessage"),
              body.getOrDefault("locale", "ko"));
      result.put("success", true);
      result.put("data", saved);
      return ResponseEntity.ok(result);
    } catch (IllegalArgumentException e) {
      return badRequestResponse(e);
    } catch (Exception e) {
      log.error("echo-note create failed", e);
      return errorResponse(e);
    }
  }

  @PutMapping("/api/echo-note-messages/{id}")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> update(
      @PathVariable Long id, @RequestBody Map<String, String> body) {
    Map<String, Object> result = new HashMap<>();
    try {
      EchoNoteMessageDto updated =
          service.update(
              id,
              body.get("recipientEmail"),
              body.get("originalMessage"),
              body.get("aiGeneratedMessage"));
      result.put("success", true);
      result.put("data", updated);
      return ResponseEntity.ok(result);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return badRequestResponse(e);
    } catch (Exception e) {
      log.error("echo-note update failed: id={}", id, e);
      return errorResponse(e);
    }
  }

  @PostMapping("/api/echo-note-messages/{id}/preview")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> generatePreview(@PathVariable Long id) {
    Map<String, Object> result = new HashMap<>();
    try {
      EchoNoteMessageDto updated = service.generatePreview(id);
      result.put("success", true);
      result.put("data", updated);
      return ResponseEntity.ok(result);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return badRequestResponse(e);
    } catch (Exception e) {
      log.error("echo-note preview failed: id={}", id, e);
      return errorResponse(e);
    }
  }

  @PostMapping("/api/echo-note-messages/{id}/finalize")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> finalizeMessage(@PathVariable Long id) {
    Map<String, Object> result = new HashMap<>();
    try {
      EchoNoteMessageDto updated = service.finalizeMessage(id);
      result.put("success", true);
      result.put("data", updated);
      return ResponseEntity.ok(result);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return badRequestResponse(e);
    } catch (Exception e) {
      log.error("echo-note finalize failed: id={}", id, e);
      return errorResponse(e);
    }
  }

  @PostMapping("/api/echo-note-messages/{id}/send-via-echo")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> sendViaEcho(@PathVariable Long id) {
    Map<String, Object> result = new HashMap<>();
    try {
      EchoNoteMessageDto updated = service.sendViaEcho(id);
      result.put("success", true);
      result.put("data", updated);
      return ResponseEntity.ok(result);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return badRequestResponse(e);
    } catch (Exception e) {
      log.error("echo-note send-via-echo failed: id={}", id, e);
      return errorResponse(e);
    }
  }

  @PostMapping("/api/echo-note-messages/{id}/revert")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> revert(@PathVariable Long id) {
    Map<String, Object> result = new HashMap<>();
    try {
      EchoNoteMessageDto updated = service.revertToDraft(id);
      result.put("success", true);
      result.put("data", updated);
      return ResponseEntity.ok(result);
    } catch (IllegalArgumentException | IllegalStateException e) {
      return badRequestResponse(e);
    } catch (Exception e) {
      log.error("echo-note revert failed: id={}", id, e);
      return errorResponse(e);
    }
  }

  @DeleteMapping("/api/echo-note-messages/{id}")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
    Map<String, Object> result = new HashMap<>();
    try {
      boolean deleted = service.delete(id);
      result.put("success", deleted);
      result.put("message", deleted ? "deleted" : "not_found");
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("echo-note delete failed: id={}", id, e);
      return errorResponse(e);
    }
  }

  // ---------------------------------------------------------------------------
  // 응답 헬퍼
  // ---------------------------------------------------------------------------

  private ResponseEntity<Map<String, Object>> badRequestResponse(Exception e) {
    Map<String, Object> r = new HashMap<>();
    r.put("success", false);
    r.put("message", e.getMessage());
    return ResponseEntity.ok(r);
  }

  private ResponseEntity<Map<String, Object>> errorResponse(Exception e) {
    Map<String, Object> r = new HashMap<>();
    r.put("success", false);
    r.put("message", e.getMessage());
    return ResponseEntity.ok(r);
  }
}
