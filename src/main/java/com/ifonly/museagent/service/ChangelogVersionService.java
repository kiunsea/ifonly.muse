package com.ifonly.museagent.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ChangelogVersionService {

  private static final Pattern VERSION_HEADER =
      Pattern.compile("^##\\s+([0-9]+\\.[0-9]+\\.[0-9]+)\\b.*$");
  private static final String FALLBACK_VERSION = "0.0.0";

  private final String latestVersion;

  public ChangelogVersionService() {
    this.latestVersion = resolveLatestVersion();
  }

  public String getLatestVersion() {
    return latestVersion;
  }

  private String resolveLatestVersion() {
    List<Path> candidates =
        List.of(
            Paths.get("CHANGELOG.md"),
            Paths.get(".", "CHANGELOG.md"),
            Paths.get("..", "CHANGELOG.md"));

    for (Path candidate : candidates) {
      if (!Files.exists(candidate)) {
        continue;
      }

      try {
        for (String line : Files.readAllLines(candidate, StandardCharsets.UTF_8)) {
          Matcher matcher = VERSION_HEADER.matcher(line.trim());
          if (matcher.matches()) {
            String version = matcher.group(1);
            log.info("CHANGELOG 최신 버전 감지: {} (source={})", version, candidate.toAbsolutePath());
            return version;
          }
        }
      } catch (IOException e) {
        log.warn("CHANGELOG 읽기 실패: {}", candidate.toAbsolutePath(), e);
      }
    }

    log.warn("CHANGELOG에서 버전을 찾지 못해 fallback 버전 사용: {}", FALLBACK_VERSION);
    return FALLBACK_VERSION;
  }
}
