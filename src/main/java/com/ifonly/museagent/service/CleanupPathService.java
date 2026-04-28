package com.ifonly.museagent.service;

import com.ifonly.museagent.dao.CleanupPathDao;
import com.ifonly.museagent.dao.TrashItemDao;
import com.ifonly.museagent.dto.CleanupPathDto;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Cleanup Path Service
 *
 * <p>Business logic and validation for cleanup path management.
 *
 * @author if-only
 * @version 0.1.0
 */
@Service
@Slf4j
public class CleanupPathService {

  private final CleanupPathDao cleanupPathDao;
  private final TrashItemDao trashItemDao;

  private static final Set<String> BLOCKED_PATHS =
      Set.of(
          "C:\\Windows",
          "C:\\Program Files",
          "C:\\Program Files (x86)",
          "/",
          "/etc",
          "/usr",
          "/bin",
          "/sbin",
          "/var",
          "/boot",
          "/sys",
          "/proc");

  @Autowired
  public CleanupPathService(CleanupPathDao cleanupPathDao, TrashItemDao trashItemDao) {
    this.cleanupPathDao = cleanupPathDao;
    this.trashItemDao = trashItemDao;
  }

  public List<CleanupPathDto> getAllPaths() {
    return enrichCleanupStatus(cleanupPathDao.findAll());
  }

  public List<CleanupPathDto> getEnabledPaths() {
    return enrichCleanupStatus(cleanupPathDao.findAllEnabled());
  }

  /**
   * Get enabled path strings for execution
   *
   * @return list of enabled path strings
   */
  public List<String> getEnabledPathStrings() {
    return cleanupPathDao.findAllEnabled().stream().map(CleanupPathDto::getPath).toList();
  }

  /**
   * Add a new cleanup path
   *
   * @param path file or directory path
   * @param description optional description
   * @return saved cleanup path
   * @throws IllegalArgumentException on validation failure
   */
  public CleanupPathDto addPath(String path, String description) {
    validatePath(path);

    String normalizedPath = normalizePath(path);

    if (cleanupPathDao.existsByPath(normalizedPath)) {
      throw new IllegalArgumentException("Path already registered: " + normalizedPath);
    }

    String pathType = detectPathType(normalizedPath);

    CleanupPathDto dto =
        CleanupPathDto.builder()
            .path(normalizedPath)
            .description(description)
            .pathType(pathType)
            .enabled(true)
            .build();

    CleanupPathDto saved = enrichCleanupStatus(cleanupPathDao.save(dto));
    log.info(
        "Cleanup path added: id={}, path={}, type={}", saved.getId(), saved.getPath(), pathType);
    return saved;
  }

  /**
   * Update an existing cleanup path
   *
   * @param id path id
   * @param path new path
   * @param description new description
   * @param enabled enabled flag
   * @return updated cleanup path
   * @throws IllegalArgumentException on validation failure
   */
  public CleanupPathDto updatePath(Long id, String path, String description, boolean enabled) {
    CleanupPathDto existing =
        cleanupPathDao
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cleanup path not found: id=" + id));

    if (path != null && !path.isBlank()) {
      validatePath(path);
      String normalizedPath = normalizePath(path);
      existing.setPath(normalizedPath);
      existing.setPathType(detectPathType(normalizedPath));
    }

    if (description != null) {
      existing.setDescription(description);
    }
    existing.setEnabled(enabled);

    CleanupPathDto updated = enrichCleanupStatus(cleanupPathDao.update(existing));
    log.info("Cleanup path updated: id={}, path={}", updated.getId(), updated.getPath());
    return updated;
  }

  /**
   * Remove a cleanup path
   *
   * @param id path id
   * @return true if deleted
   */
  public boolean removePath(Long id) {
    boolean deleted = cleanupPathDao.deleteById(id);
    if (deleted) {
      log.info("Cleanup path removed: id={}", id);
    }
    return deleted;
  }

  /**
   * Toggle enabled/disabled state
   *
   * @param id path id
   * @return updated cleanup path
   */
  public CleanupPathDto toggleEnabled(Long id) {
    CleanupPathDto dto =
        cleanupPathDao
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cleanup path not found: id=" + id));

    dto.setEnabled(!dto.isEnabled());
    CleanupPathDto updated = enrichCleanupStatus(cleanupPathDao.update(dto));
    log.info("Cleanup path toggled: id={}, enabled={}", id, updated.isEnabled());
    return updated;
  }

  public int getCount() {
    return cleanupPathDao.countAll();
  }

  public int getEnabledCount() {
    return cleanupPathDao.countEnabled();
  }

  private void validatePath(String path) {
    if (path == null || path.isBlank()) {
      throw new IllegalArgumentException("Path must not be empty");
    }

    String normalized = normalizePath(path);

    for (String blocked : BLOCKED_PATHS) {
      if (normalized.equalsIgnoreCase(blocked)) {
        throw new IllegalArgumentException("System-critical path is not allowed: " + normalized);
      }
    }
  }

  private String normalizePath(String path) {
    return Paths.get(path.trim()).toAbsolutePath().normalize().toString();
  }

  private String detectPathType(String path) {
    Path p = Paths.get(path);
    if (Files.isDirectory(p)) {
      return "DIRECTORY";
    } else if (Files.isRegularFile(p)) {
      return "FILE";
    }
    return "UNKNOWN";
  }

  private List<CleanupPathDto> enrichCleanupStatus(List<CleanupPathDto> paths) {
    Set<String> activeTrashPaths = resolveComparablePaths(trashItemDao.findActiveOriginalPaths());
    paths.forEach(path -> path.setCleanupStatus(resolveCleanupStatus(path, activeTrashPaths)));
    return paths;
  }

  private CleanupPathDto enrichCleanupStatus(CleanupPathDto path) {
    path.setCleanupStatus(
        resolveCleanupStatus(path, resolveComparablePaths(trashItemDao.findActiveOriginalPaths())));
    return path;
  }

  private String resolveCleanupStatus(CleanupPathDto path, Set<String> activeTrashPaths) {
    if (isInTrashRetention(path.getPath(), activeTrashPaths)) {
      return "TRASH";
    }
    return path.isEnabled() ? "ENABLED" : "DISABLED";
  }

  private boolean isInTrashRetention(String cleanupPath, Set<String> activeTrashPaths) {
    if (activeTrashPaths == null || activeTrashPaths.isEmpty()) {
      return false;
    }

    String candidate = normalizeComparablePath(cleanupPath);
    if (candidate == null) {
      return false;
    }

    return activeTrashPaths.contains(candidate);
  }

  private Set<String> resolveComparablePaths(Set<String> paths) {
    Set<String> normalized = new HashSet<>();
    for (String path : paths) {
      String comparablePath = normalizeComparablePath(path);
      if (comparablePath != null) {
        normalized.add(comparablePath);
      }
    }
    return normalized;
  }

  private String normalizeComparablePath(String path) {
    try {
      return Paths.get(path.trim()).toAbsolutePath().normalize().toString();
    } catch (Exception e) {
      log.warn("Failed to normalize cleanup path for status comparison: {}", path);
      return null;
    }
  }
}
