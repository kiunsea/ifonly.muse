package com.ifonly.museagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 생존 상태 응답 DTO
 *
 * @author if-only
 * @version 0.3.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AliveStatusResponse {

  /** 생존 상태 (OK/WARN/EXPIRED) */
  @JsonProperty("status")
  private String status;

  /** 마지막 확인 시각 */
  @JsonProperty("lastConfirmAt")
  private LocalDateTime lastConfirmAt;

  /** 마지막 확인 출처 */
  @JsonProperty("lastSource")
  private String lastSource;

  /** 생존 확인 유효 기간 값 */
  @JsonProperty("ttlValue")
  private Integer ttlValue;

  /** 생존 확인 유효 기간 단위 (DAY/MONTH/QUARTER/HALF_YEAR) */
  @JsonProperty("ttlUnit")
  private String ttlUnit;

  /** 경고 유예 기간 (일) */
  @JsonProperty("warnDays")
  private Integer warnDays;

  /** 다음 상태 변경 예상 시각 */
  @JsonProperty("nextDeadline")
  private LocalDateTime nextDeadline;

  /** 상태 메시지 */
  @JsonProperty("message")
  private String message;
}
