package com.ifonly.museagent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ChangelogVersionService {

  private static final String FALLBACK_VERSION = "0.0.0";

  private final String latestVersion;

  public ChangelogVersionService(BuildProperties buildProperties) {
    String resolved = buildProperties != null ? buildProperties.getVersion() : null;
    if (resolved == null || resolved.isBlank()) {
      log.warn("BuildProperties 가 비어 있어 fallback 버전 사용: {}", FALLBACK_VERSION);
      this.latestVersion = FALLBACK_VERSION;
    } else {
      this.latestVersion = resolved;
      log.info("앱 버전 로드: {} (source=META-INF/build-info.properties)", resolved);
    }
  }

  public String getLatestVersion() {
    return latestVersion;
  }
}
