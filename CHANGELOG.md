# Changelog

본 문서는 `ifonly.muse` 의 공개된 변경사항을 기록합니다.
이전 (v1.x) 의 내부 개발 이력은 본 공개 repo 에 포함되지 않습니다.

---

## v2.0.0 — Initial public release

`ifonly.muse` 의 첫 공개 버전입니다. if-only Echo 서비스의 PC 동반 에이전트로, Windows 데스크톱에 설치되어 다음 기능을 제공합니다.

> 일부 기능은 v2.0.0 시점에서 아직 검증 단계에 있습니다. 아래에서 "베타 — 시험 단계" 로 표기된 항목은 동작은 하지만 실 사용 흐름을 더 다듬는 중입니다. 자세한 후속 일정은 "향후 계획" 섹션 참고.

### echo 서비스 연동이 필요하거나 echo 동작에 종속된 기능

- **Echo Note 보관함** *(베타 — 시험 단계)* — 메시지 본문이 사용자 PC 에만 저장되는 강화 privacy 메시지 보관함
  - 작성·편집·삭제
  - AI 프리뷰 (echo 외부 API 호출)
  - 마침 처리 시 3~6 개월 사이 임의 일자로 자동 발송 예약
  - "echo 발송" 버튼으로 즉시 수동 발송 (테스트·긴급용)
- **Alive-Check 모니터링** — echo 서버의 alive 상태 정기 폴링, EXPIRED 감지
- **디바이스 등록·재등록** — echo 서비스에 PC 등록 관리
- **Echo Server 자격증명 관리** — OAuth2 client credentials, continuation token 자동 교환
- **파일 정리 자동 실행** — alive 상태가 EXPIRED 임계 일수에 도달하면 등록된 cleanup 경로의 파일을 휴지통으로 이동 (echo 가 EXPIRED 를 반환해야 트리거)
- **휴지통 자동 만료 purge** — Alive-Check 스케줄러의 매 폴링 cycle 마다 보관 기간이 지난 휴지통 항목을 영구 삭제 (echo 응답 자체는 무관, 폴링이 돌기만 하면 됨)

### 에이전트 단독으로 동작하는 기능

- **휴지통 수동 관리** — 보관 항목 조회·복원·즉시 만료 삭제
- **Cleanup 경로 등록** — 자동 정리 대상 경로 등록·편집·기록·쓰기 권한 검증
- **작업 수행 이력** — 모든 자동/수동 작업의 상세 로그 (필터·정렬·기간 조회)
- **자체 설정** — Alive-Check 폴링 주기, 휴지통 보관 기간, purge 배치 크기 등

### 운영 기능

- **i18n** — 한국어, 영어, 일본어
- **다크/소프트 테마** — 시스템 설정 또는 수동 전환
- **온보딩 가이드 + 컨텍스트 도움말** — 첫 진입 시 투어, 카드별 도움말 토글
- **카드 그룹화 대시보드** — 작업 상태 / 작업 관리 / 관리자 (시스템 정보·Echo 설정·디바이스) 그룹

### 기술 스택

- Spring Boot 3.5
- Java 17
- SQLite (로컬 저장)
- Thymeleaf (UI)
- Driver.js (온보딩)

### 호환성

| 영역                  | 값                                |
| :-------------------- | :-------------------------------- |
| 호출 대상 echo API    | `unversioned`                     |
| echo-note path prefix | `/api/external/echo-note/...`     |
| 선언 위치             | `app.echo-server.api.external-api-version` (application.yml) |

부팅 시 `Echo external API contract: muse expects version='unversioned'` 가 INFO 레벨로 로그됩니다.

echo 운영팀이 `/v1/` 도입 시 본 repo 의 새 릴리즈는 `application.yml` 갱신만으로 대응 가능 (코드 변경 불요). 본 매트릭스의 다음 행은 향후 릴리즈에 추가됩니다.

### 향후 계획 (Roadmap)

현재 시점에서 검증·다듬기 진행 중이거나 향후 도입 예정인 항목입니다:

- **Echo Note 보관함 — 시험 단계 → 정식**: AI 프리뷰·자동 발송 흐름이 동작하지만 실 사용 시나리오 검증 진행 중. 안정화되면 베타 표기 제거 예정.
- **자체 작업 스케줄링** — 파일 정리·휴지통 purge 등의 사용자 정의 cron 류 주기 설정. 현재는 Alive-Check 트리거에 종속.
- **파일 정리 수동 실행 트리거** — Alive-Check 와 무관하게 즉시 실행 가능한 수동 버튼/엔드포인트.
- **외부 기여 정책** — `CONTRIBUTING.md` 에 게시 예정.

### 알려진 제약

- Windows 우선 (Mac / Linux 는 미검증)
- Echo Note 자동 발송은 muse-agent 가 동작 중일 때만 가능 — PC 가 꺼져 있으면 다음 부팅 시 누락된 발송분이 처리됨
- echo 서비스의 외부 API 가 호환되지 않는 응답을 반환하면 Echo Note 기능이 stub fallback 으로 동작. 에이전트 단독 기능 (휴지통 수동 관리·cleanup 경로 등록·작업 이력 조회·자체 설정) 은 영향 없이 동작합니다.
