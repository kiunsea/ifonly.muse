package com.ifonly.museagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 생존 확인 이력 응답 DTO
 *
 * @author if-only
 * @version 0.3.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AliveHistoryResponse {

  /** 이벤트 목록 */
  @JsonProperty("events")
  private List<AliveEventItem> events;

  /** 전체 건수 */
  @JsonProperty("total")
  private Long total;

  /** 페이지 크기 */
  @JsonProperty("limit")
  private Integer limit;

  /** 오프셋 */
  @JsonProperty("offset")
  private Integer offset;

  /** 생존 확인 이벤트 항목 */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AliveEventItem {

    /** 이벤트 ID */
    @JsonProperty("id")
    private Long id;

    /** 확인 출처 */
    @JsonProperty("source")
    private String source;

    /** 확인 시각 */
    @JsonProperty("confirmedAt")
    private LocalDateTime confirmedAt;

    /** 요청 ID */
    @JsonProperty("requestId")
    private String requestId;
  }
}
