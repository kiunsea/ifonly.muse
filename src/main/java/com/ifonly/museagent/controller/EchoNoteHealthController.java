package com.ifonly.museagent.controller;

import com.ifonly.museagent.config.DynamicReactiveClientRegistrationRepository;
import com.ifonly.museagent.service.ChangelogVersionService;
import com.ifonly.museagent.service.SecureConfigService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Echo Note 모듈 health endpoint — Phase 4.
 *
 * <p>echo-server 의 echo-note popup s4_5 (보관 위치 선택) 단계가 brower 에서 호출해 muse-agent 의 *기설치 + 동작 중* 여부를
 * 감지한다. CORS 정책으로 echo origin 만 허용 (다른 사이트가 사용자 PC 의 muse-agent 존재 여부를 fingerprint 하지 못하도록).
 *
 * <p>응답 본문은 의도적으로 단순 — version + capabilities. 여기에 민감 정보 (사용자 이메일, OAuth credential 등) 를 절대 노출하지
 * 않음. echo-server 가 ping 결과를 신뢰해 redirect 만 결정하는 단순 계약.
 *
 * <p>CORS 화이트리스트는 {@link com.ifonly.museagent.config.EchoNoteCorsConfig} 가 관리.
 *
 * @author if-only
 * @version 0.1.0
 */
@RestController
@RequestMapping("/api/echo-note")
@Slf4j
public class EchoNoteHealthController {

  private final ChangelogVersionService changelogVersionService;
  private final SecureConfigService secureConfigService;

  @Value("${spring.application.name:muse-agent}")
  private String applicationName;

  @Autowired
  public EchoNoteHealthController(
      ChangelogVersionService changelogVersionService, SecureConfigService secureConfigService) {
    this.changelogVersionService = changelogVersionService;
    this.secureConfigService = secureConfigService;
  }

  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> health() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", "ok");
    body.put("service", applicationName);
    body.put("version", changelogVersionService.getLatestVersion());
    body.put("capabilities", List.of("echo-note"));
    body.put("composeUrl", "/echo-note");
    // v1.8.0: 자격증명 보유 여부만 노출 (사용자 이메일/clientId 등 민감 정보는 절대 미노출).
    //   echo popup/wrapper 가 이 값으로 *덮어쓰기 안전망* 분기 — 공유 PC 에서 다른 사용자 자격증명을 의도치 않게
    //   덮어쓰는 케이스 차단.
    body.put("hasCredentials", currentHasCredentials());
    return ResponseEntity.ok(body);
  }

  private boolean currentHasCredentials() {
    return hasNonBlank(DynamicReactiveClientRegistrationRepository.KEY_CLIENT_ID)
        && hasNonBlank(DynamicReactiveClientRegistrationRepository.KEY_SECRET);
  }

  private boolean hasNonBlank(String key) {
    if (!secureConfigService.hasKey(key)) return false;
    String v = secureConfigService.getSecureValue(key);
    return StringUtils.hasText(v);
  }
}
