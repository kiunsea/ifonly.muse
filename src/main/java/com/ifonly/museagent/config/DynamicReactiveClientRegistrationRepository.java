package com.ifonly.museagent.config;

import com.ifonly.museagent.service.SecureConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * 자격증명 동적 갱신을 지원하는 OAuth2 ClientRegistrationRepository.
 *
 * <p>기존 {@link
 * org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository}
 * 는 부팅 시 1회만 ClientRegistration 을 받아 보관 → secure-config 가 갱신돼도 반영되지 않음 → muse-agent 재시작 필요. 본 구현체는
 * {@link #findByRegistrationId(String)} 호출 시점마다 {@link SecureConfigService} 에서 최신 자격증명을 읽어
 * ClientRegistration 을 빌드하므로, 사용자가 echo-config 에서 자격증명을 갱신하거나 Phase 5b 의 continuation token 자동 교환으로
 * secure-config 가 변경되면 *다음 호출부터 즉시 반영*.
 *
 * <p>성능: 매 호출이 아니라 자격증명 hash 가 바뀌었을 때만 새 객체를 빌드 (캐시). 자격증명이 그대로면 캐시된 instance 반환.
 *
 * <p>자격증명 부재 시: {@link Mono#error(Throwable)} 로 친화적 예외. 부팅은 막지 않음 (사용자가 echo-config 진입 흐름을 거치도록).
 *
 * @author if-only
 * @version 0.1.0
 */
@Component
@Slf4j
public class DynamicReactiveClientRegistrationRepository
    implements ReactiveClientRegistrationRepository {

  public static final String REGISTRATION_ID = "echo-server";

  public static final String KEY_URL = "echo.server.url";
  public static final String KEY_CLIENT_ID = "echo.server.client.id";
  public static final String KEY_SECRET = "echo.server.client.secret";

  private final SecureConfigService secureConfigService;
  private final EchoServerProperties echoServerProperties;

  private volatile ClientRegistration cachedRegistration;
  private volatile String cachedKey;

  public DynamicReactiveClientRegistrationRepository(
      SecureConfigService secureConfigService, EchoServerProperties echoServerProperties) {
    this.secureConfigService = secureConfigService;
    this.echoServerProperties = echoServerProperties;
  }

  @Override
  public Mono<ClientRegistration> findByRegistrationId(String registrationId) {
    if (!REGISTRATION_ID.equals(registrationId)) {
      return Mono.empty();
    }
    return Mono.fromCallable(this::resolveCurrent);
  }

  /** 외부에서 호출해 캐시 무효화 (자격증명 갱신 직후 호출하면 다음 요청부터 새 자격증명 적용). */
  public synchronized void invalidateCache() {
    cachedRegistration = null;
    cachedKey = null;
    log.info("DynamicReactiveClientRegistrationRepository cache invalidated");
  }

  private synchronized ClientRegistration resolveCurrent() {
    String url = readEffective(KEY_URL, echoServerProperties.getUrl());
    String clientId = readEffective(KEY_CLIENT_ID, echoServerProperties.getClientId());
    String clientSecret = readEffective(KEY_SECRET, echoServerProperties.getClientSecret());

    if (url != null) {
      url = url.trim().replaceAll("/+$", "");
    }

    if (!StringUtils.hasText(url)
        || !StringUtils.hasText(clientId)
        || !StringUtils.hasText(clientSecret)) {
      throw new IllegalStateException(
          "Echo Server 자격증명이 설정되어 있지 않아요. echo-config 페이지에서 URL/Client ID/Client Secret 을 설정하거나, "
              + "echo home page popup 의 PC 보관 옵션을 통한 자동 발급을 이용해주세요.");
    }

    String key = url + "|" + clientId + "|" + clientSecret;
    if (key.equals(cachedKey) && cachedRegistration != null) {
      return cachedRegistration;
    }

    // 자격증명이 바뀌었거나 첫 빌드 — 새 instance 생성 + EchoServerProperties 도 동기화
    ClientRegistration registration =
        ClientRegistration.withRegistrationId(REGISTRATION_ID)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri(url + "/oauth2/token")
            .scope("read", "write")
            .build();

    echoServerProperties.setUrl(url);
    echoServerProperties.setClientId(clientId);
    echoServerProperties.setClientSecret(clientSecret);

    cachedRegistration = registration;
    cachedKey = key;
    log.info(
        "OAuth2 ClientRegistration rebuilt: clientId={}, tokenUri={}",
        clientId,
        url + "/oauth2/token");
    return registration;
  }

  private String readEffective(String key, String fallback) {
    if (secureConfigService.hasKey(key)) {
      String v = secureConfigService.getSecureValue(key);
      if (StringUtils.hasText(v)) return v;
    }
    return fallback;
  }
}
