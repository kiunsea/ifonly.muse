package com.ifonly.museagent.service;

import com.ifonly.museagent.config.DynamicReactiveClientRegistrationRepository;
import com.ifonly.museagent.config.EchoServerProperties;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Echo OAuth 자격증명 동적 갱신 — muse-agent 재시작 없이 새 자격증명 적용.
 *
 * <p>기존엔 secure-config 가 갱신돼도 {@link
 * org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository} 가
 * 부팅 시 1회만 만들어진 캐시를 들고 있어서 muse-agent 재시작이 필요했음. 본 service 가 두 가지 캐시를 명시적으로 무효화:
 *
 * <ul>
 *   <li>{@link DynamicReactiveClientRegistrationRepository} 의 ClientRegistration 캐시 — 다음 요청 시 새
 *       자격증명으로 재빌드
 *   <li>{@link ReactiveOAuth2AuthorizedClientService} 의 access token 캐시 — 옛 자격증명으로 발급된 토큰을 즉시 폐기
 * </ul>
 *
 * <p>호출 시점: {@link com.ifonly.museagent.service.SecureConfigService#setSecureValue(String, String)}
 * 으로 echo 자격증명 (`echo.server.url` / `echo.server.client.id` / `echo.server.client.secret`) 이 갱신된
 * 직후. 호출자는:
 *
 * <ul>
 *   <li>{@link com.ifonly.museagent.controller.EchoNoteMessageController} 의 continuation token 자동
 *       교환 흐름 (Phase 5b)
 *   <li>echo-config 페이지의 자격증명 수동 입력 흐름
 * </ul>
 *
 * @author if-only
 * @version 0.1.0
 */
@Service
@Slf4j
public class EchoCredentialUpdater {

  private final DynamicReactiveClientRegistrationRepository registrationRepository;
  private final ReactiveOAuth2AuthorizedClientService authorizedClientService;
  private final EchoServerProperties echoServerProperties;

  @Autowired
  public EchoCredentialUpdater(
      DynamicReactiveClientRegistrationRepository registrationRepository,
      ReactiveOAuth2AuthorizedClientService authorizedClientService,
      EchoServerProperties echoServerProperties) {
    this.registrationRepository = registrationRepository;
    this.authorizedClientService = authorizedClientService;
    this.echoServerProperties = echoServerProperties;
  }

  /**
   * 자격증명 변경을 알림 — 두 캐시 모두 무효화.
   *
   * <p>호출 후 다음 echo API 호출 시 자동으로:
   *
   * <ol>
   *   <li>{@code DynamicReactiveClientRegistrationRepository.findByRegistrationId} 가 새 자격증명으로
   *       ClientRegistration 빌드
   *   <li>{@code ReactiveOAuth2AuthorizedClientManager} 가 옛 token 이 cache 에 없으니 새 token 발급
   * </ol>
   *
   * <p>v1.x — principalName 후보 다중 시도. {@link
   * org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction}
   * 의 client_credentials grant 는 호출 컨텍스트에 따라 principalName 이 *anonymousUser* (default), 또는
   * registrationId, 또는 clientId 가 될 수 있음. 어느 하나만 invalidate 하면 다른 키로 저장된 옛 token (TTL 1시간) 이 그대로
   * 사용되어 첫 호출이 401. 따라서 **모든 후보 키를 동기적으로** 비워야 race 없이 다음 호출이 새 token 으로 진행.
   */
  public void onCredentialsChanged() {
    registrationRepository.invalidateCache();

    String registrationId = DynamicReactiveClientRegistrationRepository.REGISTRATION_ID;
    Set<String> principalCandidates = new LinkedHashSet<>();
    principalCandidates.add("anonymousUser"); // Spring Security default for unauthenticated context
    principalCandidates.add(registrationId);
    if (StringUtils.hasText(echoServerProperties.getClientId())) {
      principalCandidates.add(echoServerProperties.getClientId());
    }

    for (String principal : principalCandidates) {
      try {
        // block() 으로 동기 처리 — invalidate 가 끝나기 전에 후속 호출이 옛 cache 를 못 보도록.
        // in-memory Map.remove 라 ms 단위 — block 비용 미미.
        authorizedClientService.removeAuthorizedClient(registrationId, principal).block();
      } catch (Exception e) {
        log.warn(
            "Failed to remove authorized client for principal={}: {}", principal, e.getMessage());
      }
    }
    log.info(
        "Echo credential caches invalidated for principals={}. Next call will use new credentials.",
        principalCandidates);
  }
}
