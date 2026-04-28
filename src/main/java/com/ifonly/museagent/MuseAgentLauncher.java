package com.ifonly.museagent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Muse Agent 애플리케이션 메인 클래스
 *
 * <p>로컬 스케줄 애플리케이션
 *
 * <p>Echo Server와 REST로 통신하는 로컬 스케줄 관리 애플리케이션
 *
 * @author if-only
 * @version 0.1.0
 */
@SpringBootApplication
public class MuseAgentLauncher {

  public static void main(String[] args) {
    ensureRuntimeDirectories();
    SpringApplication.run(MuseAgentLauncher.class, args);
  }

  private static void ensureRuntimeDirectories() {
    Path appBase = Path.of("").toAbsolutePath().normalize();

    try {
      Files.createDirectories(appBase.resolve("data"));
      Files.createDirectories(appBase.resolve("logs"));
      Files.createDirectories(appBase.resolve("schedules"));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to prepare runtime directories", e);
    }
  }
}
