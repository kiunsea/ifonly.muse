# DEVLOG

`ifonly.muse` 의 개발 일지. 사용자-가시 변경 요약은 [CHANGELOG.md](../CHANGELOG.md), 다음 세션 인계 사항은 [doc/NEXT_SESSION.md](NEXT_SESSION.md) 참고.

본 로그는 v2.0.0 첫 공개 릴리즈 이후의 작업만 기록함. 이전 (`if-only/muse-agent` 시기) 의 내부 개발 이력은 공개 repo 에 포함되지 않음.

---

## 2026-05-07 — v2.2.0 release: 가이드 페이지 + 헤더 통합 + 다국어 보강

긴 누적 작업 (v2.1.1 직후 ~ 본 시점) 을 한 commit (`4693e7e`) 으로 정리하고 minor 릴리즈로 배포.

### 신규 / 통합

- 새 `/guide` 페이지 — `WebController.guide` 핸들러 + `templates/guide.html` (TOC + 6 섹션 카드 계층). 시스템 패널 도구 줄 + 비-index 페이지 헤더에 [가이드] 버튼 추가.
- 헤더 ⋯ 메뉴 패턴을 7 페이지에 통합. 신규 `static/js/more-menu.js` 로 `toggleMoreMenu` / `closeMoreMenu` 분리 (index 는 app.js, 그 외는 더 작은 standalone 로 옮겨 self-skip 가드 포함).

### i18n full coverage

- guide / echo-config 본문이 hardcoded 한국어로 굳어 있던 회귀 해소. ~84 신규 키 (`page.guide.*`, `page.echo_config.*`) × 4 locale = ~336 properties 라인 추가. echo-config 의 inline JS `showImportResult` 메시지 13개도 신규 `EC_I18N` 객체로 분리하고 `/*[[#{...}]]*/` 인라인 패턴으로 i18n.
- "Echo Note (잔잔한 물결)" Korean 서브타이틀 도입 — `page.dashboard.section.echo_note`, `page.dashboard.btn.manage_echo_note`, `page.echo_note.title` (default + ko 만, en/ja 는 plain). README + index 코멘트 + guide 본문의 "마음의 울림" 도 "잔잔한 물결" 로 통일.

### UX 폴리시

- `.help-tooltip` CSS 가 `--card-bg / --primary-color / --border-color` 변수 사용으로 테마 따라감. soft 테마에서 본문 글자 invisible 회귀 해소.
- `buildDashboardSteps` 의 candidate 필터에 `museSystemPanel.contains(el)` 검사 추가 — 시스템 패널 가이드 범위 confine.
- `context-help.js` 의 도움말 모드 click handler 에 navigation 우회 분기 추가 — `<a href>` / `<button onclick=location.href>` 클릭 시 `setHelpMode(false)` 후 navigation 진행. 모바일에서 도움말 버튼이 숨겨진 상태로 도움말 모드 ON 인 dead-end 자동 해소.
- 모바일 viewport (`≤768px`) 에서 `.btn-help-toggle { display: none }`.

### Tour 매핑 정리

- 시스템 패널 안 도움말/둘러보기 매핑 mismatch 점검 후 정정. `tour.muse.task_status`, `tour.muse.collapse_toggle`, `tour.muse.cleanup_nav`, `tour.muse.task_history_nav` 4개 신규 키로 popover 정확화. selector 중복 (`a[href="/echo-config"]` 와 `#btnManageEchoConfig`) 제거. section + 내부 button 의 동일 i18n 키 중복 매핑 정리.

### 인프라

- `.github/workflows/build.yml` 도입 — push / PR 마다 ubuntu-latest + Java 17 (temurin) + gradle cache 로 `bootJar -x test` 자동 실행.
- 첫 회 실패: `./gradlew: Permission denied` (Windows commit 의 default mode `100644`). `git update-index --chmod=+x gradlew` → mode `100755` 로 commit 후 정상.

### 결정·관찰

- **단일 vs 분할 commit**: 누적 변경 18 파일 / +1057 / -114 가 6+ 트랙 (header consolidation, /guide page, tour fixes, tooltip theme, full i18n, brand subtitle) 에 걸쳐 있어 분할이 자연스러우나 파일 overlap (특히 index.html, guide.html, echo-config.html) 이 많아 깔끔히 자르기 어려움. 단일 commit + 종합 메시지로 결정. 향후 동일 상황엔 작업 단위마다 즉시 commit 권장.
- **CI gate 의 가치**: 첫 push 가 즉시 fail (gradlew exec bit) — 로컬 windows 에선 동작하나 Linux runner 에서 처음 검증되는 종류의 회귀를 잡음. 릴리즈 워크플로우의 5.5단계 (push → CI green 대기) 가 의미있게 작동.

### 미해결 / 후속

- 아직 i18n 미적용인 페이지가 남아 있을 수 있음 — 사용자 보고 시점에 추가 패스.
- 헤더 통합 패턴이 7 페이지에 흩어져 있음 — Thymeleaf fragment 로 추출하면 향후 변경 시 한 곳만 수정. 별도 트랙.

---

## 2026-05-06 — v2.1.1 release: Brand 로고 갱신

`img/muse_logo.jpeg` (실제 1024x1024 PNG) 를 새 source 로 받아 muse 의 brand asset 4종 일괄 재생성.

### 절차

1. 기존 4종 (`muse-agent.png`, `muse-agent.svg`, `muse-brand.png`, `favicon.ico`) 을 `*.bak.{ext}` 로 백업 (echo-server 의 `favicon.bak.ico` naming 패턴 차용).
2. Pillow 12.2 로 source 로드 후:
   - `muse-agent.png` ← 1024x1024 (full source)
   - `muse-brand.png` ← 384x384 (LANCZOS 리샘플)
   - `favicon.ico` ← 256x256 (PNG-embedded ICO)
   - `muse-agent.svg` ← PNG-embedded SVG wrapper (raster → vector 변환 불가, 동일 파일명 유지를 위한 컨테이너)
3. `bat/deploy.bat` 실행으로 `static/img/`, `packaging/distribution/img/` 에 sync + ZIP 재생성.

### 결정·관찰

- **vector → raster 강등**: 기존 `muse-agent.svg` 는 진짜 vector path 였음. 새 source 가 PNG 라 vector 재현 불가능 — base64 PNG 를 SVG `<image>` 태그로 wrap. 템플릿의 `.svg` 참조 호환성 유지가 우선이라 강등 수용. 진짜 vector 가 필요하면 별도 vector source 받아 교체.
- **aspect 미세 변경**: 기존 PNG 들이 1008x1055 / 384x402 (살짝 portrait), 새 source 는 정사각 1024x1024. 템플릿 CSS 가 explicit width/height 로 표시 — 시각적 영향 미미.

---

## 2026-05-02 — v2.1.0 release: Echo Note preview UX 개선

### 배경

사용자 검증 사이클에서 다음 세 문제가 한 흐름으로 보고됨:

1. **첫 preview 가 stub 으로 굳음** — popup 의 PC 보관 분기 첫 진입 직후 작성한 메시지의 [프리뷰] 응답이 `[stub 폴백 — echo 미연결]` 로 떨어지고, 그 후엔 재호출이 없어 화면에 그대로 남음. echo 의 alive-check 는 정상 200 받는 상태라 자격증명 자체는 제대로 받았으나 첫 preview 호출만 401.
2. **프리뷰 결과 가시성 부재** — 정상 응답이어도 결과는 보관함 row 의 badge 색깔 변경 + [수정] 모달 안의 textarea 로만 표시. 사용자가 모달을 명시적으로 열어야만 본문 확인 가능.
3. **트리거 인터페이스 부재** — 작성 form 에 [메시지 보관] 만 있어 사용자가 "프리뷰 기능이 존재한다" 는 사실 자체를 인지 못 함. 보관함 row 의 [프리뷰] 버튼이라는 우회 동선을 알아내야 했음.

### 진단

- Cloud Run access log 에서 첫 issue 의 시간 순서: `10:20:20 401 /preview` (옛 token) → `10:20:21 200 /exchange-continuation-token` (새 credential 발급). 즉 **사용자가 popup 의 PC 보관 분기를 시작하는 시점에 muse 가 옛/캐시된 credential 로 preview 를 한 번 시도** → 401 → stub fallback 으로 화면에 굳음. 이후 새 credential 받아도 preview 재호출이 없어 stub 그대로.
- `EchoCredentialUpdater.onCredentialsChanged()` 가 호출되긴 하나 `removeAuthorizedClient(REGISTRATION_ID, REGISTRATION_ID)` 의 두 번째 인자 (principalName) 가 실제 저장된 cache 키와 불일치. Spring Security 의 `ServerOAuth2AuthorizedClientExchangeFilterFunction` 이 client_credentials grant 에서 사용하는 default principalName 은 **`"anonymousUser"`** — 코드는 `"echo-server"` 만 시도해 실제 cache 가 비워지지 않음. 옛 token 이 TTL 1시간 동안 cache 에 살아남아 후속 호출이 모두 옛 credential 로 진행 → 401.

### 구현

#### A. EchoCredentialUpdater — token cache invalidation race 해결

- principalName 후보 다중 시도: `anonymousUser` / registrationId / clientId
- `block()` 동기 처리 — 비동기 `subscribe()` 에서는 다음 호출이 cache invalidation 끝나기 전에 들어가는 race 가능
- 후보 모두 비우므로 어떤 키로 저장됐든 invalidate 보장

#### B. 작성 form 인라인 미리보기

- `EchoNotePreviewGenerator.PreviewResult` record + `generateDetailed()` — text 와 함께 `stubFallback` boolean + `fallbackReason` 노출
- `EchoNoteMessageService.previewOnly(originalMessage, locale)` — DB 저장 없이 가공본만 반환 (사용자가 보관 결정 전에 미리 확인)
- `EchoNoteMessageController` 에 `POST /api/echo-note-messages/preview-only` endpoint
- `templates/echo-note.html` 작성 form 에 [미리보기 ✨] 버튼 + form 아래 인라인 결과 카드 (textarea + stub 경고 배너 + [다시 시도] / [이대로 보관] / [닫기])
- `static/js/echo-note.js` 의 `hnPreviewOnly()` / `hnAcceptPreview()` / `hnDiscardPreview()` 핸들러
- i18n 4 locale (default + ko + en + ja) 새 키 13개

#### C. 자동 preview (보관 직후)

- `hnCreate()` 가 save 성공 후 자동으로 `/preview` 호출 → 인라인 카드 *정보 모드* 로 표시. [이대로 보관] / [다시 시도] hide, [닫기] 만 노출 — [닫기] 가 reload trigger
- stub heuristic — saved preview 응답엔 stubFallback 플래그가 없어서 텍스트의 `[stub` prefix 로 추정 (한·영·일 모두 동일 prefix)
- card 의 `data-mode` attribute 로 수동/자동 모드 분기

### 검증

- 로컬 빌드 + 테스트 통과 (단, 첫 시도에 ScheduleExecutorService 테스트가 Mockito inline mock maker 의 ClassLoader race 로 일시 fail — 두 번째 시도엔 통과. JVM 21 + Mockito 의 pre-existing flaky 패턴, 본 변경분 무관)
- echo-server 측은 변경 없음 — `/api/external/echo-note/preview` 그대로 사용

### 알려진 한계 / 후속

- `static/js/echo-note.js` 의 stub heuristic 이 텍스트 prefix 검사라 fragile. 견고하게 하려면 `service.generatePreview(id)` 도 `PreviewResult` 형태로 응답 + controller 응답에 stubFallback 키 추가 필요. 다음 사이클로 미룸.
- 보관함 row 의 [프리뷰] 재생성도 인라인 카드와 통합하면 일관된 UX. 현재는 별도 모달 흐름 — 다음 사이클로.

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
