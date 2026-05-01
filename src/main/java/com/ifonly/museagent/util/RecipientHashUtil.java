package com.ifonly.museagent.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 로깅용 수신자 식별자 해시 유틸.
 *
 * <p>echo Note 흐름의 수신자 이메일을 muse 로그에 *평문으로 남기지 않기* 위한 결정론적 해시. SHA-256 + 사용자별 salt → 12 char hex
 * 접두만 사용 (개인 PC 의 muse 로그 규모에선 충분히 식별 가능, 출력은 짧게).
 *
 * <p>salt 는 코드에 박지 않고 {@code app.echo-note.recipient-hash-salt} 프로퍼티로 주입. 미설정 시 빈 문자열 — 그래도 결정론은
 * 보장되지만 (동일 이메일 → 동일 해시) 역산 방어력은 약해진다. 사용자가 환경변수로 임의 salt 를 주입하기를 권장.
 *
 * <p>본 유틸은 muse-local 관측용이며 echo-server 의 {@code ContactHashUtil} 과는 salt 가 다르므로 cross-system 식별
 * 키로는 사용하지 않는다.
 *
 * @author if-only
 * @version 0.1.0
 */
@Component
public class RecipientHashUtil {

  /** 해시 출력 길이 (hex char 수). 64 char 풀 SHA-256 의 앞 N 자만 사용. */
  private static final int HASH_PREFIX_LENGTH = 12;

  private final String salt;

  public RecipientHashUtil(@Value("${app.echo-note.recipient-hash-salt:}") String salt) {
    this.salt = salt == null ? "" : salt;
  }

  /** 이메일 정규화 (소문자 + trim) → salted SHA-256 의 앞 12 char hex 반환. null 입력 시 sentinel "(none)" 반환. */
  public String hashEmail(String email) {
    if (email == null || email.isBlank()) {
      return "(none)";
    }
    String normalized = email.trim().toLowerCase();
    return sha256Hex(salt + ":EMAIL:" + normalized).substring(0, HASH_PREFIX_LENGTH);
  }

  private static String sha256Hex(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
