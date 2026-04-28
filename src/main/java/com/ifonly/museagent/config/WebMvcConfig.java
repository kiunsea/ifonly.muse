package com.ifonly.museagent.config;

import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

/**
 * Web MVC 설정 클래스 - i18n 지원
 *
 * @author if-only
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  /**
   * 세션 기반 Locale Resolver - 기본 언어: 한국어
   *
   * @return LocaleResolver
   */
  @Bean
  public LocaleResolver localeResolver() {
    SessionLocaleResolver resolver = new SessionLocaleResolver();
    resolver.setDefaultLocale(Locale.KOREAN);
    return resolver;
  }

  /**
   * URL 파라미터(lang=ko/en/ja)로 언어 전환을 처리하는 인터셉터
   *
   * @return LocaleChangeInterceptor
   */
  @Bean
  public LocaleChangeInterceptor localeChangeInterceptor() {
    LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
    interceptor.setParamName("lang");
    return interceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(localeChangeInterceptor());
  }
}
