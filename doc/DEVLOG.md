# DEVLOG

`ifonly.muse` 의 개발 일지. 사용자-가시 변경 요약은 [CHANGELOG.md](../CHANGELOG.md), 다음 세션 인계 사항은 [doc/NEXT_SESSION.md](NEXT_SESSION.md) 참고.

본 로그는 v2.0.0 첫 공개 릴리즈 이후의 작업만 기록함. 이전 (`if-only/muse-agent` 시기) 의 내부 개발 이력은 공개 repo 에 포함되지 않음.

---

## 2026-05-01 — Track F: Echo Note ↔ echo-server 정합 강화

`if-only/echo-server` 의 `ExternalEchoNoteController` 와 muse 의 `EchoServerClient` / `EchoContinuationExchangeService` 양측 contract 를 cross-read 한 뒤 4개 보강 항목을 일괄 적용. 다음 세션에서 commit 전략 결정.

### F1. Recipient email 로그 해싱 (privacy)

신규 `com.ifonly.museagent.util.RecipientHashUtil` — salted SHA-256 의 앞 12 char hex 반환. salt 는 `app.echo-note.recipient-hash-salt` 프로퍼티 (기본 빈 문자열, `ECHO_NOTE_RECIPIENT_HASH_SALT` 환경변수로 override 권장).

- echo 의 `ContactHashUtil` 패턴 차용. salt 는 echo 와 별도 (echo 의 `app.faint-thread.hash-salt` 와 무관) — muse-local 관측 전용.
- 패치 site: `EchoNoteMessageService.sendViaEcho` 의 발송 성공 로그 (`recipient={email}` → `recipientHash={12char hex}`). 다른 로그 경로엔 recipient 평문 흔적 없음.

### F2. echo 응답 error 코드 parsing & 한국어 매핑

신규 `com.ifonly.museagent.client.EchoErrorMapper` — Spring `ObjectMapper` 로 응답 body JSON 파싱, 알려진 코드 (`missing_message`, `message_too_long`, `invalid_recipient`, `missing_token`, `invalid_token`, `not_authenticated`, `exchange_failed`) 를 사용자 친화 한국어로 환원. 미지 코드는 null 반환 → 호출자 fallback.

- `EchoServerClient.toFriendlyMessage` 가 `WebClientResponseException.getResponseBodyAsString()` 로 본문을 읽어 매퍼에 전달. 매핑 성공 시 그 메시지 사용, 실패 시 기존 generic 메시지로 떨어짐.
- 영향 범위: alive / device / preview / send 모든 echo API 호출. 401 의 raw "HTTP 401 상태코드" 가 "echo 인증에 실패했어요. echo-config 에서 자격증명을 다시 설정해주세요." 로.

### F3. Continuation token 410 GONE 세분화

`EchoContinuationExchangeService.exchange` 의 catch 블록을 `HttpStatusCodeException` (4xx/5xx) / `RestClientException` (transport) / 기타 `RuntimeException` 3분기로 재구성.

- 410 GONE → `mapContinuationError(410, code)` → "이전에 사용됐거나 만료된 popup 링크예요. echo 에서 다시 진입해주세요."
- 4xx 코드 (`missing_token`, `invalid_token`) → 각각 친숙한 표현
- 5xx → "echo 서버에 일시적 오류가 발생했어요" / transport 실패 → "echo 와 통신할 수 없어요"
- 기존 raw `"echo exchange returned non-2xx or empty body: <statusCode>"` 가 사용자 화면에 노출되지 않음.

### F4. Send response 의 raw `message` → 친숙한 표현

`EchoNoteMessageService.sendViaEcho` 의 비-sent 분기에서 `echoErrorMapper.mapSendFailure(rawMessage)` 호출. echo 의 `EmailService.SendResult.message()` 가 영문 raw ("Failed to send HTML email: ...", "Email service is not available", "Email notification disabled by policy") 였으나 이제 모두 한국어 표현으로 변환. sentinel `email_service_unavailable` 도 별도 매핑.

### 검증

- `gradlew compileJava` 통과 (DI 변경: `EchoNoteMessageService`, `EchoServerClient`, `EchoContinuationExchangeService` 생성자 시그니처 확장 — Spring 자동 주입).
- `bat/deploy.bat` 실행 → ZIP 재생성 + `bat/ifonly.muse-distribution-2.0.0/` 추출 갱신.
- 사용자 retest 대기 (실패 경로는 echo 측 협업 없이 단독으로 reproduce 어려움 — credential 회수된 자격증명으로 send 시도하면 401 매핑 검증 가능).

### 결정·관찰

- **echo "Faint Thread" 용어 격리**: echo 의 `ContactHashUtil` 이 `app.faint-thread.hash-salt` 를 사용하나, muse 는 그 프로퍼티 이름을 그대로 차용하지 않음 — `app.echo-note.recipient-hash-salt` 로 교체. "Faint Thread" 가 echo-server 의 내부 기능명이므로 muse 공개 코드에 노출 회피.
- **muse hash 출력 길이**: echo 는 64char 풀 hex, muse 는 12char 접두만. muse 로그는 PC-local 인 데다 일반 사용자 발송량 (수십~수백 건) 에선 12char 도 collision 없는 식별 가능. 가독성 우선.
- **build 일시 실패**: `gradle clean` 이 `build/test-results/binary/output.bin` 잠금 때문에 실패. `./gradlew --stop` 후에도 잔존 — `rm -rf build/test-results` 수동 정리 후 통과. Gradle 8.14 + Windows + 어제부터 살아있던 daemon 의 file handle 누수 추정.

### 미커밋 보유 (기존 + 본 트랙 누적)

- 다른 세션 UI/UX 리팩토링 (16 파일) + 본 세션 tooltip/tour 패치 (2 파일) + 본 트랙 F (5 파일 신규/수정)
- 합계 ~22 파일 / ~2000 라인 변경
- 다음 세션 commit 시 분할 전략 권장: ① UI/UX 리팩토링 본체 / ② tooltip 테마 / ③ tour scope confine / ④ Track F (privacy + error mapping)
- README/CHANGELOG/Release asset 정합 처리는 별도 단계.

---

## 2026-04-30 — UI/UX 리팩토링 (다른 세션) + 후속 패치 (본 세션)

본 시점 17 파일 / ~1700 라인 uncommitted. 두 출처 합본 — 다음 세션에서 commit 전략 (단일 vs 분할) 결정 예정.

### 다른 세션 — UI/UX 리팩토링

**메인 대시보드 재구성**
- `/` 의 메인 영역 = "Echo Note 보관함" hero 뷰 (이전엔 시스템 상태 카드 모음).
  - 상단: "보관 중인 메시지 N개 · 닿은 메시지 N개" 상태 라인
  - "+새로 보관하기" / "🔍 검색" 액션 버튼
  - 빈 상태 placeholder + 최근 5건 메시지 카드 (DRAFT/READY/SENT 상태 표시)
- `WebController.index` 가 hero 데이터 model attribute 주입: `echoNoteHoldingCount`, `echoNoteSentCount`, `recentEchoNotes`.
- `EchoNoteMessageService` 에 보조 메서드: `getSentCount()`, `getRecent(int limit)`.

**헤더 재구성**
- 인라인 버튼 그룹 (가이드/도움말/모두접기/테마/언어) → 더보기 (⋯) 드롭다운 메뉴 + ⚙️ 시스템 패널 토글 단독 노출.
- ⋯ 안에 테마 토글 + 언어 (ko/en/ja) 만 — 가이드/도움말/모두접기 는 시스템 패널 안 도구 줄로 이주.

**시스템 패널**
- `#museSystemPanel` 신설 — `aside role="dialog"` 로 우측 슬라이드 패널.
- 패널 내부 5개 카드: 현재 상태 / 안부 신호 / 작업 상태 (수행 이력 포함) / Echo Server 설정 / 디바이스 / 파일 정리 / 시스템 정보 (실측 기준).
- 패널 헤더: 제목 + ✕ 닫기. 본문 위 sticky 도구 줄 (가이드 시작 / 도움말 보기 / 모두 접기).

**용어 부드럽게**
- "생존 신호 상태" → "안부 신호 상태" (i18n + tour copy + echo-config.html label).
- "Echo Server 통신 관리자" 등의 운영적 톤 → 사용자 친화 표현으로 점진 교체.

**부수**
- cosmetic import 제거 (`EchoServerClient`, `ApiController`, `AliveCheckScheduler`).
- `EchoServerProperties` javadoc 줄바꿈 정리.

### 본 세션 — 후속 패치 (2건)

**도움말 툴팁 테마 적용** (`style.css`)
- `.help-tooltip` 의 하드코딩된 `rgba(17,24,39,0.96)` 어두운 배경 / `#9ed0ff` 푸른 제목 / 화살표 색을 `var(--card-bg)` / `var(--primary-color)` / `var(--border-color)` 로 교체.
- soft 테마에서 어두운 배경 + 어두운 본문 글자가 안 읽히던 contrast 회귀 해소. 테마 토글 시 툴팁 색이 즉시 따라 바뀜.

**시스템 패널 가이드 범위 confine** (`onboarding-tour.js`)
- `buildDashboardSteps` 의 candidate 필터에 `museSystemPanel.contains(el)` 검사 추가.
- 결과: 시스템 패널의 "가이드 시작" 이 패널 외부 (헤더 로고 / Echo Note Hero / footer) 로 새지 않고 패널 내부 카드만 순회. 다른 페이지 (echo-config / task-history / cleanup / device-register) 의 가이드는 그대로 페이지 전체 안내 유지.

### 검증

- `gradlew compileJava` 통과.
- `bat/deploy.bat` 재실행 → ZIP 재생성 + `bat/ifonly.muse-distribution-2.0.0/` 추출 갱신.
- 사용자 retest 결과 confirmation 받음 (다른 세션 UI/UX 동작 + 본 세션 두 패치).

### 결정·관찰

- **README/CHANGELOG 정확성 정합** (2026-04-30 오전, 본 변경 직전): `자체 작업 스케줄러` 가 README/CHANGELOG 에 미구현 상태로 적혀 있던 것을 정정. "에이전트 단독 / echo 의존" 두 버킷으로 재분류, 자체 스케줄링은 Roadmap 섹션으로 이동. fix `de37416` (README), `570380e` (CHANGELOG).
- **Edit tool trailing-whitespace 회귀**: `Edit` 가 trailing-whitespace-only diff 를 "old_string and new_string are exactly the same" 로 판정하는 케이스 발견. trailing token 교체 시 anchor 명시 필요.
- **PowerShell mojibake 회귀**: `deploy.bat` 의 service XML JAR 이름 substitution 시 `Get-Content` 가 default ANSI (cp949) 로 읽어 한국어 주석을 깨뜨림. `-Encoding UTF8` 명시 + `[System.IO.File]::WriteAllText(..., new UTF8Encoding($false))` 로 BOM 없는 UTF-8 보존.
- **Public release 이후의 안정화 흐름**: v2.0.0 Release ZIP 은 *공개 시점* 의 코드 스냅샷. 본 UI/UX 리팩토링은 그 *이후* 의 누적 변경 → 다음 commit 시 v2.0.1 또는 v2.0.0 amend 결정 필요.

### 미커밋 보유 (다음 세션에 결정 위임)

- **commit 전략**: 단일 ("ui/ux refactor + tooltip theme + tour scope") vs 분할 (다른 세션 본체 ↔ 본 세션 두 패치). 분할이 history 추적성 좋음.
- **README 갱신**: 메인 화면이 Echo Note Hero 로 바뀐 사실 + ⚙️ 시스템 패널의 새 IA. 사용자 첫 인상이 바뀌어 update 강하게 권장.
- **CHANGELOG 갱신**: 본 변경분을 어떤 버전 헤더에 묶을지 (v2.0.1 신규 vs v2.0.0 amend).
- **GitHub Release asset 교체**: 현재 `v2.0.0` release 에 붙은 ZIP 은 리팩토링 *전* 빌드. 신규 ZIP 으로 재업로드 또는 `v2.0.1` 신규 release 결정.

---

## 2026-04-29 — v2.0.0 Initial public release (마일스톤)

`if-only/muse-agent` 에서 split → `ifonly.muse` 공개 repo 로 이전. 첫 GitHub Release 생성 (`v2.0.0`, asset: `ifonly.muse-distribution-2.0.0.zip`). 상세 작업 흐름 (자격증명 회수 게이트, Track A 보안 grep, deploy.bat ZIP 검증, BuildProperties 마이그레이션 등) 은 [doc/NEXT_SESSION.md](NEXT_SESSION.md) 의 "지금까지 완료된 것" 섹션과 commit history 참고.
