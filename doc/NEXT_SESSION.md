# 다음 세션 핸드오프

**마지막 작업 기준일**: 2026-04-29
**Repo 상태**: PRIVATE on GitHub, main 이 origin/main 보다 6 commit 앞섬 (push 보류 — 아래 "push 전 결정 사항" 참조)

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

## ⚠️ push 전 결정 사항 — 하드코딩된 OAuth 자격증명 (P0)

`application*.yml` line 33-34 / 18-19 + `index.html:283` / `echo-config.html:85` 에 다음 값이 하드코딩됨:

```
client-id: soul-keeper-client
client-secret: soul-keeper-secret
```

부팅 검증 시 이 값들로 echo-server.omnibuscode.com OAuth2 토큰이 **실제로 발급됨** — 즉 유효한 공유 자격증명. 사용자 정책 (2026-04-29 오후 결정):

> "기존 레거시 코드이지만 muse2 에서는 사용 가이드로 코드를 남기고, 운영중인 echo 에서 해당 인증 정보를 지우는 것만으로 해소. 작업은 다른 세션에서 진행 중."

→ **다른 세션에서 echo-server 의 `soul-keeper-client` 자격증명이 회수된 후** muse 측 push 진행. 그 전까지 push 금지.

---

## 다음에 할 수 있는 일

### A. Public 전환 준비 (보류 중)
- 위 OAuth 자격증명 회수 완료 확인
- 한 번 더 보안 grep — RB / FAINT_THREAD / Home Nudge / 운영 정보
- `bat/deploy.bat` 실행해 ZIP 검증
- 브라우저로 dashboard smoke test
- 준비 완료되면 GitHub UI 에서 visibility public 전환

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
3. `git log --oneline -10` — 마지막 commit 이 본 문서 commit 인지 확인
4. `git remote -v` — origin 이 `kiunsea/ifonly.muse` 인지 확인
5. **OAuth 자격증명 회수 별도 세션 상태 확인** — 회수 완료 시에만 push 진행

---

## 절대 하지 말 것

- ❌ `if-only/muse-agent/` 에 새 기능 추가 (legacy 격리)
- ❌ Faint Thread / Relation Bridge / AI 시스템 프롬프트 / 운영 정보 를 ifonly.muse 에 노출
- ❌ public 전환 후 IP 누출 검출 시: force-push 로 history rewrite 시도 (cached fork 가 남을 수 있음)
- ❌ DEVLOG.md 또는 if-only 의 doc/design/ 자산을 ifonly.muse 로 복사
- ❌ **`soul-keeper-client` 자격증명이 echo 측에서 회수되기 전 main 을 origin 에 push**
