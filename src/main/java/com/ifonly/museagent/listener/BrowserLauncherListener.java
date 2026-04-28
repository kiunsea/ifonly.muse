package com.ifonly.museagent.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Application startup listener that automatically opens the web browser
 *
 * @author if-only
 * @version 0.1.0
 */
@Component
@Slf4j
public class BrowserLauncherListener {

  @Value("${server.port:8484}")
  private int serverPort;

  @EventListener(ApplicationStartedEvent.class)
  public void launchBrowser() {
    String url = "http://localhost:" + serverPort;
    String osName = System.getProperty("os.name").toLowerCase();

    try {
      if (osName.contains("win")) {
        // Windows
        Runtime.getRuntime().exec(new String[] {"cmd", "/c", "start", url});
        log.info("Browser opened: {}", url);
        System.out.println("\n========================================");
        System.out.println("  Muse Agent Dashboard");
        System.out.println("  URL: " + url);
        System.out.println("========================================\n");
      } else if (osName.contains("mac")) {
        // macOS
        Runtime.getRuntime().exec(new String[] {"open", url});
        log.info("Browser opened: {}", url);
      } else if (osName.contains("linux")) {
        // Linux
        Runtime.getRuntime().exec(new String[] {"xdg-open", url});
        log.info("Browser opened: {}", url);
      }
    } catch (Exception e) {
      log.warn("Failed to open browser: {}", e.getMessage());
      System.out.println("\nPlease open this URL manually: " + url);
    }
  }
}
