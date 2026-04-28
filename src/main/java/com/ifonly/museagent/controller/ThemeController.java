package com.ifonly.museagent.controller;

import com.ifonly.museagent.dao.AppSettingDao;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 테마 설정 API (Muse Agent).
 *
 * <p>AppSettingDao 를 통해 SQLite 에 영속화합니다. 인증 불필요 (로컬 에이전트).
 *
 * @author if-only
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/settings/theme")
public class ThemeController {

  private static final String KEY = "ui.theme.preference";
  private static final Set<String> VALID = Set.of("soft", "dark");

  private final AppSettingDao appSettingDao;

  public ThemeController(AppSettingDao appSettingDao) {
    this.appSettingDao = appSettingDao;
  }

  @GetMapping
  public ResponseEntity<Map<String, String>> getTheme() {
    String theme = appSettingDao.getValue(KEY).orElse("soft");
    Map<String, String> body = new HashMap<>();
    body.put("theme", theme);
    return ResponseEntity.ok(body);
  }

  @PutMapping
  public ResponseEntity<Map<String, String>> setTheme(@RequestBody Map<String, String> req) {
    String theme = req.getOrDefault("theme", "soft");
    if (!VALID.contains(theme)) {
      theme = "soft";
    }
    appSettingDao.setValue(KEY, theme, "UI 테마 (dark / soft)");
    Map<String, String> body = new HashMap<>();
    body.put("theme", theme);
    return ResponseEntity.ok(body);
  }
}
