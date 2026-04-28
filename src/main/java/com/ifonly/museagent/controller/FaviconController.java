package com.ifonly.museagent.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Browser favicon controller.
 *
 * <p>Serves favicon assets from img/muse.png and img/favicon.ico.
 */
@Controller
public class FaviconController {

  private static final Path FAVICON_PNG_PATH =
      Paths.get("img", "muse.png").toAbsolutePath().normalize();
  private static final Path FAVICON_ICO_PATH =
      Paths.get("img", "favicon.ico").toAbsolutePath().normalize();
  private static final String CLASSPATH_FAVICON_ICO = "static/favicon.ico";

  @GetMapping("/favicon.png")
  public ResponseEntity<Resource> faviconPng() {
    if (!Files.exists(FAVICON_PNG_PATH) || !Files.isRegularFile(FAVICON_PNG_PATH)) {
      return ResponseEntity.notFound().build();
    }

    Resource icon = new FileSystemResource(FAVICON_PNG_PATH.toFile());
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noCache())
        .contentType(MediaType.IMAGE_PNG)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=muse.png")
        .body(icon);
  }

  @GetMapping("/favicon.ico")
  public ResponseEntity<Resource> faviconIco() {
    Resource icon;
    if (Files.exists(FAVICON_ICO_PATH) && Files.isRegularFile(FAVICON_ICO_PATH)) {
      icon = new FileSystemResource(FAVICON_ICO_PATH.toFile());
    } else {
      ClassPathResource classPathIcon = new ClassPathResource(CLASSPATH_FAVICON_ICO);
      if (!classPathIcon.exists()) {
        return ResponseEntity.notFound().build();
      }
      icon = classPathIcon;
    }

    return ResponseEntity.ok()
        .cacheControl(CacheControl.noCache())
        .contentType(MediaType.parseMediaType("image/x-icon"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=favicon.ico")
        .body(icon);
  }
}
