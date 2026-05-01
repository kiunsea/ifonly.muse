package com.ifonly.museagent.controller;

import com.ifonly.museagent.client.EchoServerClient;
import com.ifonly.museagent.config.EchoServerProperties;
import com.ifonly.museagent.dto.DeviceStatusInfo;
import com.ifonly.museagent.service.ChangelogVersionService;
import com.ifonly.museagent.service.CleanupPathService;
import com.ifonly.museagent.service.DeviceRegistrationService;
import com.ifonly.museagent.service.EchoNoteMessageService;
import com.ifonly.museagent.service.SecureConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web UI Controller
 *
 * @author if-only
 * @version 0.2.0
 */
@Controller
@Slf4j
public class WebController {

  private static final String KEY_URL = "echo.server.url";
  private static final String KEY_CLIENT_ID = "echo.server.client.id";

  private static final int MAIN_RECENT_LIMIT = 5;

  private final EchoServerProperties echoServerProperties;
  private final EchoServerClient echoServerClient;
  private final DeviceRegistrationService deviceRegistrationService;
  private final CleanupPathService cleanupPathService;
  private final ChangelogVersionService changelogVersionService;
  private final SecureConfigService secureConfigService;
  private final EchoNoteMessageService echoNoteMessageService;

  @Value("${spring.application.name:muse-agent}")
  private String applicationName;

  @Value("${server.port:8484}")
  private int serverPort;

  @Autowired
  public WebController(
      EchoServerProperties echoServerProperties,
      EchoServerClient echoServerClient,
      DeviceRegistrationService deviceRegistrationService,
      CleanupPathService cleanupPathService,
      ChangelogVersionService changelogVersionService,
      SecureConfigService secureConfigService,
      EchoNoteMessageService echoNoteMessageService) {
    this.echoServerProperties = echoServerProperties;
    this.echoServerClient = echoServerClient;
    this.deviceRegistrationService = deviceRegistrationService;
    this.cleanupPathService = cleanupPathService;
    this.changelogVersionService = changelogVersionService;
    this.secureConfigService = secureConfigService;
    this.echoNoteMessageService = echoNoteMessageService;
  }

  /**
   * Main dashboard page
   *
   * @param model model
   * @return view name
   */
  @GetMapping("/")
  public String index(Model model) {
    // Application info
    model.addAttribute("appName", applicationName);
    model.addAttribute("appVersion", changelogVersionService.getLatestVersion());
    model.addAttribute("serverPort", serverPort);
    model.addAttribute("javaVersion", System.getProperty("java.version"));
    model.addAttribute("osName", System.getProperty("os.name"));

    // Echo Server configuration — prefer SecureConfigService values over static properties
    String echoUrl =
        secureConfigService.hasKey(KEY_URL)
            ? secureConfigService.getSecureValue(KEY_URL)
            : echoServerProperties.getUrl();
    String echoClientId =
        secureConfigService.hasKey(KEY_CLIENT_ID)
            ? secureConfigService.getSecureValue(KEY_CLIENT_ID)
            : echoServerProperties.getClientId();
    model.addAttribute("echoServerUrl", echoUrl);
    model.addAttribute("echoServerClientId", echoClientId);

    // Device registration status
    DeviceStatusInfo deviceStatus = deviceRegistrationService.getDeviceStatus();
    model.addAttribute("deviceStatus", deviceStatus);

    // Cleanup path summary
    model.addAttribute("cleanupPathCount", cleanupPathService.getCount());
    model.addAttribute("enabledPathCount", cleanupPathService.getEnabledCount());
    model.addAttribute("cleanupPaths", cleanupPathService.getAllPaths());

    // Echo Note 보관함 요약 — 메인 화면이 보관함 hero 영역으로 변경됨 (Phase 3).
    // 보관 중 = DRAFT + READY (전체 - SENT). 닿은 = SENT.
    int totalCount = echoNoteMessageService.getTotalCount();
    int sentCount = echoNoteMessageService.getSentCount();
    model.addAttribute("echoNoteHoldingCount", totalCount - sentCount);
    model.addAttribute("echoNoteSentCount", sentCount);
    model.addAttribute("recentEchoNotes", echoNoteMessageService.getRecent(MAIN_RECENT_LIMIT));

    return "index";
  }

  /**
   * Device registration page
   *
   * @param model model
   * @return view name
   */
  @GetMapping("/device/register")
  public String deviceRegister(Model model) {
    // Application info
    model.addAttribute("appName", applicationName);
    model.addAttribute("appVersion", changelogVersionService.getLatestVersion());
    model.addAttribute("osName", System.getProperty("os.name"));
    model.addAttribute("osVersion", System.getProperty("os.version"));

    // Get current device registration status
    DeviceStatusInfo status = deviceRegistrationService.getDeviceStatus();
    model.addAttribute("deviceStatus", status);

    return "device-register";
  }

  /**
   * Cleanup path management page
   *
   * @param model model
   * @return view name
   */
  @GetMapping("/cleanup")
  public String cleanupPaths(Model model) {
    model.addAttribute("appName", applicationName);
    model.addAttribute("appVersion", changelogVersionService.getLatestVersion());
    model.addAttribute("cleanupPaths", cleanupPathService.getAllPaths());
    model.addAttribute("enabledCount", cleanupPathService.getEnabledCount());
    return "cleanup";
  }

  /**
   * Task execution history page
   *
   * @param model model
   * @return view name
   */
  @GetMapping("/task-history")
  public String taskHistory(Model model) {
    model.addAttribute("appName", applicationName);
    model.addAttribute("appVersion", changelogVersionService.getLatestVersion());
    return "task-history";
  }

  /**
   * Task settings page
   *
   * @param model model
   * @return view name
   */
  @GetMapping("/task-settings")
  public String taskSettings(Model model) {
    model.addAttribute("appName", applicationName);
    model.addAttribute("appVersion", changelogVersionService.getLatestVersion());
    return "task-settings";
  }

  /**
   * Echo Server configuration management page
   *
   * @param model model
   * @return view name
   */
  @GetMapping("/echo-config")
  public String echoConfig(Model model) {
    model.addAttribute("appName", applicationName);
    model.addAttribute("appVersion", changelogVersionService.getLatestVersion());

    // Echo Server configuration
    String url =
        secureConfigService.hasKey(KEY_URL)
            ? secureConfigService.getSecureValue(KEY_URL)
            : echoServerProperties.getUrl();
    String clientId =
        secureConfigService.hasKey(KEY_CLIENT_ID)
            ? secureConfigService.getSecureValue(KEY_CLIENT_ID)
            : echoServerProperties.getClientId();
    model.addAttribute("echoServerUrl", url);
    model.addAttribute("echoServerClientId", clientId);
    model.addAttribute(
        "secureCredentialSaved", secureConfigService.hasKey("echo.server.client.secret"));
    model.addAttribute(
        "isWindows", System.getProperty("os.name", "").toLowerCase().contains("windows"));

    return "echo-config";
  }
}
