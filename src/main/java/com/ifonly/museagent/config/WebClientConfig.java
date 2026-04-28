package com.ifonly.museagent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient OAuth2 Configuration.
 *
 * <p>v1.7.0 변경: ClientRegistrationRepository 를 부팅 시 1회 캐시
 * (`InMemoryReactiveClientRegistrationRepository`) 에서 동적 갱신 가능 ({@link
 * DynamicReactiveClientRegistrationRepository}) 로 전환. 자격증명 변경 시 muse-agent 재시작 없이 즉시 반영됨 (캐시 무효화는
 * {@link com.ifonly.museagent.service.EchoCredentialUpdater} 가 담당).
 *
 * <p>또한 부팅 시점의 자격증명 부재 검증 (IllegalStateException) 을 제거 — 신규 사용자가 자격증명 입력 전에도 muse-agent 가 정상 부팅하고,
 * echo-config 페이지 또는 popup 의 PC 보관 흐름으로 자격증명을 받게끔 함. 자격증명 부재 시점에 echo API 호출하면 친화적 에러로 응답 (Dynamic
 * Repository 가 IllegalStateException 발생).
 *
 * @author if-only
 * @version 0.3.0
 */
@Configuration
@Slf4j
public class WebClientConfig {

  /**
   * OAuth2 Authorized Client Service — token cache 역할. {@link
   * com.ifonly.museagent.service.EchoCredentialUpdater} 가 자격증명 변경 시 캐시 무효화에 사용.
   */
  @Bean
  public ReactiveOAuth2AuthorizedClientService authorizedClientService(
      ReactiveClientRegistrationRepository clientRegistrationRepository) {
    return new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository);
  }

  /** OAuth2 Authorized Client Manager — client_credentials grant 동작. */
  @Bean
  public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
      ReactiveClientRegistrationRepository clientRegistrationRepository,
      ReactiveOAuth2AuthorizedClientService authorizedClientService) {

    ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider =
        ReactiveOAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build();

    AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager manager =
        new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
            clientRegistrationRepository, authorizedClientService);

    manager.setAuthorizedClientProvider(authorizedClientProvider);
    log.info("OAuth2 Authorized Client Manager configured (dynamic registration repository)");
    return manager;
  }

  /** OAuth2 WebClient — 매 요청마다 Dynamic Repository 가 최신 자격증명 반환. */
  @Bean
  public WebClient webClient(ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
    ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
        new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

    oauth2Client.setDefaultClientRegistrationId(
        DynamicReactiveClientRegistrationRepository.REGISTRATION_ID);

    WebClient client =
        WebClient.builder()
            .filter(oauth2Client)
            .defaultHeader("Content-Type", "application/json")
            .build();

    log.info("WebClient configured with dynamic OAuth2 Client Credentials flow");
    return client;
  }
}
