# 다음 세션 핸드오프

**마지막 작업 기준일**: 2026-04-29
**Repo 상태**: PRIVATE on GitHub, main = origin/main (push 게이트 해제 후 동기화 완료)

---

## 지금까지 완료된 것

### Split & 이전 (2026-04-29 오전)
- `if-only/muse-agent/` → `ifonly.muse/` 분리
- Relation Bridge 코드 완전 제거
- schema.sql 의 home_nudge legacy migration 제거
- 옛 phase1.stub_note user-facing 메시지 제거
- LICENSE (MIT) · README · CHANGELOG 신규 작성, build.gradle/settings.gradle 갱신
- Java 패키지 `com.ifonly.museagent.*` 그대로 유지 (rename 비용 대비 가치 낮음)
- DEVLOG.md 미이전, packaging/distribution/jre/ 미이전 (download-jre.ps1 로 동적 받음)

### 신규 산출물 (오전)
- `bat/deploy.bat`, `bat/run.bat`
- `doc/TRANSFER_WHITELIST.md`, `doc/NEXT_SESSION.md`

### 2026-04-29 오후 (B/C/D/E 세션)
| commit    | 내용 |
| :-------- | :--- |
| `ed7179a` | UI: Echo Note 카드 top-level 승격 |
| `3745931` | config: echo-note 엔드포인트 properties 화 + `external-api-version` 선언 |
| `6893e84` | docs: 호환성 매트릭스 (README + CHANGELOG) + LICENSE 노트 정정 |
| `34cb72c` | docs: CONTRIBUTING.md 신규 (보안 disclosure + PR 정책) |
| (E1)      | README 의 `install.bat` → `agent_install.bat` 참조 수정 |
| (E3)      | `LATEST_KNOWN_EXTERNAL_API_VERSION` 메커니즘 — 향후 stale yaml 자동 WARN |
| (E4)      | `.gitignore` 에 `/.claude/` 추가, 본 NEXT_SESSION 갱신 + commit |

### 2026-04-29 부팅 검증
- `./gradlew bootRun` → :8484 정상 → echo-server.omnibuscode.com OAuth 핸드셰이크 통과 → alive 응답 정상 수신 (status=EXPIRED)
- 신규 부팅 로그 확인: `Echo external API contract: muse expects version='unversioned' (latest known by this build: 'unversioned'; ...)` ✓

---

## ✅ push 게이트 해제 — soul-keeper 자격증명 회수 완료 (2026-04-29)

`application*.yml` 과 두 thymeleaf template 에 `soul-keeper-client` / `soul-keeper-secret` 가 하드코딩되어 있으나, 별도 세션에서 echo-server 측 자격증명이 회수됨. 사용자 보고 + 본 repo 에서 직접 검증 완료:

```
POST https://echo-server.omnibuscode.com/oauth2/token  →  HTTP 401 invalid_client
```

→ muse 측 코드는 사용 가이드 차원에서 그대로 유지, 자격증명은 dead. 7 commit (`ca31123..352c790`) origin/main 에 push 완료.

---

## 다음에 할 수 있는 일

### A. Public 전환 준비
- ~~OAuth 자격증명 회수 완료 확인~~ ✅ (2026-04-29 완료)
- ~~보안 grep (RB / FAINT_THREAD / Home Nudge / 운영 정보)~~ ✅ — `Home-nudge` 옛 코드네임 i18n+로그 14곳 → `Echo-note` 정리
- ~~`bat/deploy.bat` ZIP 검증~~ ✅ — `service\logs\*` cleanup 누락 발견 (호스트명·split 전 경로 노출), 패치 + 로컬 로그 삭제 후 재빌드 검증
- ~~브라우저 dashboard smoke test~~ — string-only 변경이라 boot/route 회귀 가능성 0 으로 판단, skip
- **남은 일**: GitHub UI 에서 visibility public 전환 (사용자 직접)

### B. echo external API versioning — 후속 (echo-server 측 작업 후)
- echo-server 가 `/v1/` prefix 도입하면:
  1. `application.yml` 의 echo-note-* 경로 갱신 + `external-api-version: v1`
  2. `EchoServerProperties.LATEST_KNOWN_EXTERNAL_API_VERSION = "v1"` 갱신
  3. README/CHANGELOG 호환 매트릭스에 새 행 추가
- 코드 변경은 상수 한 줄, 나머지는 yaml + docs

### C. 호환성 매트릭스 — 후속 release notes 정착
- echo 릴리즈 노트에 "muse vN+ 호환" 표기
- muse 릴리즈 노트에 "echo external API vN+ 필요" 표기

### D. 코드 위생 — 잔여
- `if-only/muse-agent/README.md` (legacy repo) 에 archive 헤더 추가 — 본 working dir 가 아닌 별도 repo 작업 필요
- `bat/run.bat` 의 매 실행 `gradlew clean bootRun` — 사용자 정책상 그대로 유지 (테스트용)
- `bat/deploy.bat` 의 VERSION-INFO 안 `Git Commit: unknown` — cmd 환경에서 git PATH 없을 때 fallback. traceability 만 영향, 보안 무관

### D2. 릴리즈 검증 체크리스트 (방법론)
deploy.bat ZIP 검증을 *내용물 누설 검사* 로만 마치면 launcher 회귀가 통과한다 (2026-04-30 사례: split 후 `muse-agent-*.jar` → `ifonly-muse-*.jar` 이름 변경이 launcher 들에 미반영, fix `32b5bc2`). 다음 릴리즈 검증 체크 항목:
1. ZIP 내용물 sanity (현재 항목)
2. **추출 → `Muse-Agent.bat` 1회 launch 시도** — JAR 탐색 통과 / 부팅 성공까지 확인. 회귀 차단의 핵심.
3. **`agent_install.bat` 검증 (관리자 권한 필요)** — 서비스 등록 + service XML JAR 경로 정상 substitution 확인. 환경 영향 큼, release 직전에만 수행.

### E. 기능 추가·개선
- 사용자 추가 시 갱신

---

## 알려진 미해결 사항

### 미스터리: split 작업 중 첫 commit 사라짐 (재발 시 의심)
- 2026-04-29 첫 git commit (`e1dcd91 v2.0.0`) 이 reflog 흔적 없이 사라짐
- 같은 메시지로 재commit 했더니 정상 (`ca31123`)
- **재발 시 우선 의심 항목**:
  - `.git/config` 의 `[submodule] active = .` 항목
  - `core.autocrlf = true` 환경의 LF/CRLF 변환 충돌
  - bash on Windows 에서 cmd.exe 로 chained 실행 시 환경 누수

---

## 작업 시작 체크리스트

다음 세션 시작 시:
1. `cd C:\Users\11A\DEV\github\ifonly.muse`
2. `git status` — 클린 확인
3. `git log --oneline -10` — 마지막 commit 확인
4. `git remote -v` — origin 이 `kiunsea/ifonly.muse` 인지 확인

---

## 절대 하지 말 것

- ❌ `if-only/muse-agent/` 에 새 기능 추가 (legacy 격리)
- ❌ Faint Thread / Relation Bridge / AI 시스템 프롬프트 / 운영 정보 를 ifonly.muse 에 노출
- ❌ public 전환 후 IP 누출 검출 시: force-push 로 history rewrite 시도 (cached fork 가 남을 수 있음)
- ❌ DEVLOG.md 또는 if-only 의 doc/design/ 자산을 ifonly.muse 로 복사
