# Changelog

본 문서는 `ifonly.muse` 의 공개된 변경사항을 기록합니다.
이전 (v1.x) 의 내부 개발 이력은 본 공개 repo 에 포함되지 않습니다.

---

## v2.2.0 (2026-05-07) — 가이드 페이지 + 헤더 통합 + 다국어 보강

### 새 기능

- **가이드 페이지 (`/guide`)** — Muse 가 제공하는 전체 기능을 카드 단위·계층 구조로 안내하는 정적 참조 페이지. 시스템 패널의 [가이드] 버튼이나 다른 페이지의 헤더 [가이드] 버튼에서 진입.
- **헤더 더보기 (⋯) 메뉴 통합** — 7개 페이지 (echo-config / cleanup / task-history / device-register / echo-note / task-settings / guide) 의 헤더 우측이 ⋯ 메뉴 한 곳으로 정리. 테마 토글 + 언어 전환 (ko/en/ja) 이 메뉴 안에 모임.

### 다국어 (i18n)

- **가이드 / Echo Server 설정 페이지 본문 전체 다국어** — 이전엔 한국어 본문이 hardcoded 라 EN/JA 사용자가 언어 토글해도 본문이 안 바뀌던 회귀. 이제 양 페이지 모두 4 locale 모두 전환.
- **"Echo Note (잔잔한 물결)" 한국어 브랜드 서브타이틀 도입** — 대시보드 카드 / 보관함 페이지 타이틀 / 관리 버튼 라벨 등 사용자-가시 모든 위치. 영어 / 일본어는 plain "Echo Note" 유지.
- **헤더 도구 라벨 단순화** — "가이드 시작" → "둘러보기" (Tour / ツアー), "도움말 보기/숨기기" → "도움말" (Help / ヘルプ) 양 상태 통합.

### 사용성·접근성 개선

- **도움말 툴팁이 현재 테마 (soft/dark) 를 따름** — 이전엔 어두운 배경이 하드코딩되어 soft 테마에서 본문 글자가 거의 안 보였음.
- **시스템 패널의 "둘러보기" 가이드가 패널 내부만 안내** — 이전엔 가이드가 헤더 / Echo Note Hero 로 새어나갔음. 다른 페이지의 페이지-레벨 가이드는 그대로 페이지 전체 안내 유지.
- **도움말 모드에서 nav 버튼 클릭 시 모드 자동 OFF 후 이동** — 이전엔 도움말 모드 ON 상태에서 페이지 이동 버튼이 동작 안 하는 것처럼 보임. nav 클릭 한 번이면 모드 풀고 정상 이동.
- **모바일에서 도움말 버튼 자동 숨김** — touch UI 에서 hover 어색해 비노출. 모드가 ON 인 채로 시야에서 사라지는 dead-end 는 위 자동 OFF 동작으로 해소.

### Tour 정확도

- `#btnToggleAll`, `#btnManageCleanup`, `#btnOpenTaskHistoryPage` 의 popover 가 generic "Task Management" 였던 것을 각각 collapse_toggle / cleanup_nav / task_history_nav 로 정확화.
- selector 모호함으로 같은 element 가 두 번 highlight 되던 step 제거.
- section + 안쪽 button 이 동일 i18n 키로 도움말 중복 발동하던 매핑 정리 (echo-config / device 의 button data-help 제거).

### 인프라

- **GitHub Actions build workflow 도입** — push / PR 마다 ubuntu-latest + Java 17 (temurin) + gradle cache 로 `bootJar -x test` 자동 실행. 릴리즈 워크플로우의 push → CI green → tag 게이트 정착.

---

## v2.1.1 (2026-05-06) — Brand 로고 갱신

### 시각

- **Muse 로고 이미지 일괄 교체** — `img/muse_logo.jpeg` 를 새 source 로 받아 brand asset 4종 (`muse-agent.png`, `muse-brand.png`, `favicon.ico`, `muse-agent.svg`) 을 모두 재생성. 헤더 로고 / 즐겨찾기 아이콘 / 트레이 / Releases ZIP 의 favicon 이 새 디자인으로 통일.
- 기존 자산은 `img/*.bak.{ext}` 로 보존 (echo-server 의 backup naming 패턴 차용).
- `muse-agent.svg` 는 진짜 vector path 였으나, 신규 source 가 raster (PNG) 라서 raster-embedded SVG wrapper 로 교체. 진짜 vector 가 필요해지면 별도 source 받아 교체 필요.

---

## v2.1.0 (2026-05-02) — Echo Note preview UX 개선 + ↔ echo-server 정합 강화

### Echo Note 작성 form 의 preview UX

- **첫 preview 401 race 해결** — 사용자가 popup 의 PC 보관 분기를 처음 진입한 직후 첫 preview 호출이 옛/캐시된 자격증명으로 시도되어 401 → stub fallback 으로 굳던 문제. `EchoCredentialUpdater` 가 token cache invalidation 시 principalName 후보 다중 시도 (`anonymousUser` / registrationId / clientId) + `block()` 동기 처리로 race 제거. continuation exchange 직후 첫 echo API 호출이 항상 새 credential 사용.
- **작성 form 인라인 미리보기 ([미리보기 ✨] 버튼)** — 보관 결정 *전* 에 echo 가공본을 미리 확인할 수 있는 흐름. DB 저장 없이 stateless preview 호출 (`POST /api/echo-note-messages/preview-only`). 결과는 form 아래 인라인 카드로 표시 — stub fallback 시 노란 배너 + 사유 + [다시 시도] / [이대로 보관] / [닫기]. 이전엔 보관함 row 의 [수정] 모달을 명시적으로 열어야만 결과 텍스트를 볼 수 있어 "프리뷰 기능의 존재를 인지하기 어렵다" 는 회귀 해소.
- **자동 preview (보관 직후)** — [메시지 보관] 클릭 시 save 후 자동으로 가공본 받아 인라인 카드 *정보 모드* 로 표시 ([닫기] 만 노출, reload trigger). 사용자가 명시적으로 [미리보기 ✨] 누르지 않아도 "이렇게 가공돼요" 가 첫 보관 흐름에서 즉시 보이도록.

### UI/UX 리팩토링

- **메인 대시보드 = Echo Note 보관함 hero 뷰**. 진입 즉시 "보관 중인 메시지 N개 · 닿은 메시지 N개" 상태 라인 + "+새로 보관하기" / "🔍 검색" 액션 + 최근 5건 메시지 카드 (DRAFT/READY/SENT 상태) 가 보이도록 변경. 이전엔 시스템 상태 카드 모음이 첫 화면이었음.
- **헤더 재구성** — 인라인 버튼 그룹 (가이드/도움말/모두접기/테마/언어) 을 더보기 (⋯) 드롭다운 + ⚙️ 시스템 패널 토글 단독 노출로 단순화.
- **⚙️ 시스템 · 설정 패널 신설** — 옛 대시보드 카드들 (현재 상태 / 안부 신호 / 작업 상태 / Echo Server 설정 / 디바이스 / 시스템 정보) 을 우측 슬라이드 패널 안으로 격리. 평상시엔 메시지 보관함에 집중, 설정·진단 작업이 필요할 때만 패널을 연다.
- **용어 부드럽게** — "생존 신호" → "안부 신호" (i18n 4개 locale + tour copy 일괄 갱신).

### 사용성

- **도움말 툴팁이 테마를 따른다** — 이전엔 어두운 배경이 하드코딩되어 soft 테마에서 본문 글자가 거의 안 보이던 회귀가 있었음. `--card-bg` / `--primary-color` / `--border-color` 변수로 교체해 테마 토글 시 즉시 반영.
- **시스템 패널의 "가이드 시작" 은 패널 내부만 안내** — 이전엔 가이드가 패널 외부 (헤더 / Echo Note Hero) 로 새어나갔음. 다른 페이지 (`/echo-config`, `/task-history`, `/cleanup`, `/device/register`) 의 페이지-레벨 가이드는 그대로 페이지 전체 안내 유지.

### Echo Note ↔ echo-server 정합

- **수신자 이메일 로그 해싱** — muse 가 echo 발송 성공 로그에 평문으로 남기던 수신자 주소를 salted SHA-256 (앞 12 char) 으로 변환. salt 는 환경변수 `ECHO_NOTE_RECIPIENT_HASH_SALT` 로 사용자가 임의 강화 가능. 공개 repo 의 사용자 로그 공유 상황에서 PII 누출 방지.
- **echo 응답 에러를 사용자 친화 한국어로** — 이전엔 echo 의 4xx/5xx 응답이 "Echo Server 접속 실패: HTTP 401 상태코드를 확인하세요" 같은 generic 메시지로 환원됐음. 이제 echo 가 본문에 실어주는 `error` / `message` 필드를 읽어 "echo 인증에 실패했어요. echo-config 에서 자격증명을 다시 설정해주세요" 같은 구체 메시지로 노출. 영향 범위: preview / send / continuation token 교환 / alive / device 등록 — 모든 echo API 호출.
- **메일 발송 실패 사유 환원** — echo 가 돌려준 영문 raw 메시지 (예: "Failed to send HTML email: ...", "Email service is not available", "Email notification disabled by policy") 를 한국어로 매핑. sentinel `email_service_unavailable` 별도 처리.
- **popup 링크 410 GONE 세분화** — 이전에 사용됐거나 만료된 popup 링크는 "이전에 사용됐거나 만료된 popup 링크예요. echo 에서 다시 진입해주세요" 로 안내.

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
