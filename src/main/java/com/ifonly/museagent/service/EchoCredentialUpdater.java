package com.ifonly.museagent.service;

import com.ifonly.museagent.config.DynamicReactiveClientRegistrationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;

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

  @Autowired
  public EchoCredentialUpdater(
      DynamicReactiveClientRegistrationRepository registrationRepository,
      ReactiveOAuth2AuthorizedClientService authorizedClientService) {
    this.registrationRepository = registrationRepository;
    this.authorizedClientService = authorizedClientService;
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
   * <p>principalName 은 client_credentials grant 의 경우 클라이언트 자신 (registrationId) 사용. 주 PrincipalName
   * 은 Spring Security OAuth2 의 표준 동작.
   */
  public void onCredentialsChanged() {
    registrationRepository.invalidateCache();
    // client_credentials grant 에서 principalName 은 보통 clientId 또는 registrationId. 양쪽 모두 시도.
    authorizedClientService
        .removeAuthorizedClient(
            DynamicReactiveClientRegistrationRepository.REGISTRATION_ID,
            DynamicReactiveClientRegistrationRepository.REGISTRATION_ID)
        .subscribe();
    log.info("Echo credential caches invalidated. Next call will use new credentials.");
  }
}
