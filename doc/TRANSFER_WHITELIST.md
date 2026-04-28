# 이전 시 변경 화이트리스트 — `if-only/muse-agent` → `ifonly.muse` v2.0.0

**목적**: 첫 공개 이전 시 의도적으로 변경할 항목을 사전 고정. 이 외엔 1:1 이전. "이왕 손대는 김에" 함정 차단.

---

## 1. 변경 (의도적 차이)

### 1.1 메타·식별자
- `build.gradle`: `version = '1.9.1'` → `version = '2.0.0'`
- `build.gradle`: `archivesBaseName` 또는 `archiveFileName` → `ifonly-muse-${version}.jar` (현재는 `muse-agent-${version}.jar`)
- `settings.gradle`: `rootProject.name = 'muse-agent'` → `rootProject.name = 'ifonly-muse'`
- Java 패키지: **유지** (`com.ifonly.museagent.*`) — 패키지 rename 은 비용 대비 가치 낮음, v2.x 동안 유지하고 추후 결정

### 1.2 문서
- `README.md`: 현재 GitHub 자동생성본 → 공개 사용자 관점 README 새로 작성
- `CHANGELOG.md`: 기존 v1.x 이력 미이전. fresh `## v2.0.0 — Initial public release` 한 entry 만
- `DEVLOG.md`: **이전 안 함** (개발 저널, public 부적합)
- `HELP.md`: Spring Boot 자동 생성본 — 그대로 이전 OK
- `CHANGELOG_HEADER.md`: 검토 후 결정 (있다면)

### 1.3 라이선스·기여 정책
- `LICENSE`: 기존 muse-agent 의 MIT 라이선스 그대로 이전 (이미 정착된 결정)
- `CONTRIBUTING.md` 또는 README 의 Contributing 섹션: "personal-use companion tool, external contributions not accepted at this time" 명시

### 1.4 Git 메타
- `.gitignore`: ifonly.muse 의 GitHub 자동 생성본 (Java 템플릿) + muse-agent 의 기존 `.gitignore` 병합. 중복 제거
- `.github/`: 디렉토리 전체 미이전 (workflow 가 if-only 컨텍스트에 묶여 있음). 필요 시 ifonly.muse 용으로 별도 작성
- `.vscode/`, `.idea/`: 이전 안 함

### 1.5 if-only 컨텍스트 제거
- `build.gradle` 주석에서 if-only 모노레포 참조 제거
- `application.yml` / `application.properties` 의 if-only 운영 정보 (있다면) 검토 — echo URL 디폴트는 제거 또는 placeholder
- 코드 주석의 if-only 내부 paths (`doc/design/...`) 제거

---

## 2. 유지 (1:1 이전)

- `src/main/java/**` — RB 제거 작업 후 상태 그대로
- `src/main/resources/templates/**` — 동일
- `src/main/resources/static/**` — 동일
- `src/main/resources/messages*.properties` — 동일 (RB 키 제거 후 상태)
- `src/main/resources/schema.sql` — 동일 (home_nudge 제거 후 상태)
- `src/test/**` — RB 테스트 제거 후 상태
- Spring Boot 버전, Java 21 — 그대로
- 의존성 — 그대로
- Gradle wrapper — 그대로

---

## 3. 이전 후 별도 의사결정 항목 (이번엔 안 함)

- 패키지 rename `com.ifonly.museagent` → `com.ifonly.muse`
- README 의 echo-server URL placeholder 처리
- echo external API versioning (`/v1/` prefix) — echo 측 작업 선행 필요
- CI/CD 파이프라인 (GitHub Actions)
- 이슈 템플릿 / PR 템플릿
- 코드 sign / Windows installer signing

---

## 4. 작업 순서

1. README.md 작성 (ifonly.muse 에 commit)
2. CHANGELOG.md fresh 작성
3. 본 화이트리스트 commit
4. `if-only/muse-agent/` 의 src + 핵심 설정을 `ifonly.muse/` 로 복사 (rsync 또는 cp)
5. `build.gradle`, `settings.gradle` 메타 변경분 적용
6. .gitignore 병합
7. 빌드 검증 (`./gradlew compileJava` from ifonly.muse 루트)
8. 첫 commit + push

---

**Why this whitelist:**

자유로운 시간 = 스코프 크리프 위험. 이전 작업을 4-7일 안에 끝낼 수 있도록 변경 범위를 사전에 못 박음. 이 외 욕구는 v2.0.0 release 이후 별도 작업으로.
