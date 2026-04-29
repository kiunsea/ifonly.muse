# ifonly.muse

**PC 동반 에이전트 for the if-only Echo 서비스**

`ifonly.muse` 는 데스크톱 PC 에 설치되어 if-only Echo 서비스의 일부 기능을 사용자의 로컬 환경에서 보조하는 도구입니다.

가장 큰 역할은 **Echo Note (마음의 울림)** 의 *강화 privacy 보관 경로* 를 제공하는 것입니다 — 메시지 본문이 echo 서버가 아닌 사용자 PC 에만 저장되고, 발송 시점에만 echo 가 잠깐 받아 대신 전송하는 방식입니다.

> **상태**: v2.0.0 첫 공개 버전. Windows 우선.

---

## 무엇을 하나

> 본 도구는 v2.0.0 첫 공개 버전이며 일부 기능은 아직 검증 단계에 있습니다. 아래에서 "베타 — 시험 단계" 로 표기된 항목은 동작은 하지만 실제 사용 흐름을 더 다듬는 중입니다.

### 에이전트 단독으로 동작하는 기능

- **휴지통 수동 관리** — 보관 중인 항목 조회·복원·즉시 만료 삭제
- **Cleanup 경로 등록** — 자동 정리 대상 경로 등록·편집·기록·쓰기 권한 검증
- **태스크 수행 이력** — 모든 자동/수동 작업의 상세 로그 (필터·정렬)
- **자체 설정** — Alive-Check 폴링 주기, 휴지통 보관 기간, purge 배치 크기 등

### echo 서비스 연동이 필요하거나 echo 동작에 종속된 기능

- **Echo Note 보관함** *(베타 — 시험 단계)* — 미래의 누군가에게 닿을 메시지를 PC 에 보관, 일정 시간 후 echo 를 통해 발송
  - AI 프리뷰 (echo 의 LLM 가공)
  - 자동 발송 스케줄러 (3개월 ~ 6개월 사이 임의 일자)
  - 발송 시 echo 는 가공본만 받아 즉시 전송, 본문은 저장하지 않음
- **Alive-Check 모니터링** — echo 의 alive 상태 폴링, EXPIRED 감지
- **디바이스 등록** — echo 서비스에 PC 등록
- **파일 정리 자동 실행** — alive 상태가 EXPIRED 임계 일수에 도달하면 등록된 cleanup 경로의 파일을 휴지통으로 이동 (echo 가 EXPIRED 를 반환해야 트리거)
- **휴지통 자동 만료 purge** — Alive-Check 스케줄러의 매 폴링 cycle 마다 보관 기간이 지난 휴지통 항목을 영구 삭제 (echo 응답 자체는 무관, 폴링이 돌기만 하면 됨)

> 자동 동작 항목은 Alive-Check 스케줄러에 의해 트리거됩니다. 현재 버전은 별도의 사용자 정의 cron 스케줄을 지원하지 않습니다 — 아래 "향후 계획" 참고.

---

## 설치

### 필수 환경

- Windows 10 / 11 (64-bit)
- Java 17 이상 — 별도 설치 불요 (배포 패키지의 `agent_install.bat` 가 `download-jre.ps1` 로 자동 다운받음)
- if-only Echo 서비스 계정 (Echo Note 기능을 사용하려면)

### 설치 단계

1. [Releases 페이지](https://github.com/kiunsea/ifonly.muse/releases/latest) 에서 최신 zip 파일 다운로드
2. 압축 해제
3. `agent_install.bat` 우클릭 → "관리자 권한으로 실행"
4. 자동으로 JRE 다운로드 → Windows 서비스 등록 → 트레이 앱 실행 → 기본 브라우저로 대시보드 열림

### 첫 실행

- 대시보드: `http://localhost:8484`
- echo 서비스 연동을 사용하려면 "Echo Server 설정" 카드에서 자격증명 입력
- echo 의 popup 흐름에서 "PC 보관" 을 선택해 진입한 경우, 자격증명은 자동으로 저장됨

---

## echo 서비스와의 관계

`ifonly.muse` 는 if-only Echo 서비스의 *외부 클라이언트* 입니다. echo 서비스와 다음 관점에서 상호작용합니다:

- echo 의 **외부 API** (`/api/external/echo-note/*`) 호출 — Echo Note 의 AI 프리뷰 생성 및 발송 위임
- echo 의 **alive-check API** 호출 — 사용자 alive 상태 폴링
- echo 의 **continuation token** 흐름 — popup 에서 PC 보관 선택 시 자격증명 자동 교환

echo 서비스 자체는 본 repo 의 일부가 아니며, if-only 운영팀에서 별도 운영합니다. 본 도구는 그 외부 API 와의 통신만 담당합니다.

### echo 호환성

`ifonly.muse` 는 부팅 시 *어떤 echo external API 버전을 호출하는지* 를 명시적으로 선언합니다. 현재 매트릭스:

| ifonly.muse | echo external API | echo-note 경로 prefix              | 비고                           |
| :---------- | :---------------- | :--------------------------------- | :----------------------------- |
| 2.0.x       | `unversioned`     | `/api/external/echo-note/...`      | 첫 공개 버전. 현재 운영 상태  |

선언된 버전은 `application.yml` 의 `app.echo-server.api.external-api-version` 에서 확인할 수 있고, muse-agent 부팅 직후 `INFO` 레벨 로그로도 노출됩니다 (`Echo external API contract: muse expects version='...'`).

향후 echo 운영팀이 `/v1/` prefix 를 도입하면, 새 muse 릴리즈가 `application.yml` 의 path / version 만 갱신하여 대응합니다 — 코드 변경 없이 호환 매트릭스가 한 줄 추가되는 형태로 진화합니다.

호환되지 않는 응답을 받을 경우 `ifonly.muse` 는 해당 echo 의존 기능 (Echo Note, Alive-Check) 을 stub fallback 으로 처리하고 사용자에게 안내를 표시합니다. 에이전트 단독 기능 (휴지통 수동 관리·cleanup 경로 등록·태스크 이력 조회·자체 설정) 은 영향 없이 동작합니다.

---

## 데이터 보관

- **로컬 SQLite**: `%APPDATA%\ifonly-muse\muse-agent.db`
  - Echo Note 메시지 본문 (원본 + AI 가공본)
  - 작업 실행 이력, 휴지통 항목, 정리 경로 설정
- **Secure config** (Windows DPAPI 암호화): echo OAuth2 자격증명
- **echo 서버**: 본 도구는 echo 에 메시지 본문을 영구 저장하지 않습니다. 발송 시점에만 가공본이 echo 를 잠깐 통과하며, echo 의 송신 로그에는 메타 정보 (수신자·제목·발송 결과) 만 남습니다

---

## 향후 계획

현재 시점에서 검증·다듬기 진행 중이거나 향후 도입 예정인 항목입니다:

- **Echo Note 보관함 — 시험 단계 → 정식**: AI 프리뷰·자동 발송 흐름이 동작하지만 실 사용 시나리오 검증을 더 진행 중입니다. 안정화되면 베타 표기를 제거합니다.
- **자체 작업 스케줄링**: 파일 정리·휴지통 purge 등 작업의 사용자 정의 주기 설정 (cron 류). 현재는 Alive-Check 트리거에 종속되어 있습니다.
- **파일 정리 수동 실행 트리거**: Alive-Check 와 무관하게 즉시 실행 가능한 수동 버튼/엔드포인트.
- **외부 기여 정책**: 정해지면 `CONTRIBUTING.md` 에 게시 예정.

---

## 빌드 (개발자용)

```
./gradlew clean build
```

산출물: `build/libs/ifonly-muse-2.0.0.jar`

### 로컬 실행

```
./gradlew bootRun
```

기본 포트: 8484. 변경하려면 `application.yml` 또는 환경변수 `SERVER_PORT`.

---

## 기여

본 도구는 if-only Echo 서비스의 개인 사용자용 보조 도구로 운영됩니다. 외부 PR 및 이슈는 현재 정기적으로 응대하지 않습니다 (서비스 운영 정책상). 보안 취약점 발견 시 별도 채널로 제보를 부탁드립니다.

향후 기여 정책이 정해지면 `CONTRIBUTING.md` 에 게시할 예정입니다.

---

## 라이선스

[MIT License](LICENSE).
