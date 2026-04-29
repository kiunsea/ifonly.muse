# Muse-Agent 배포 안내

이 폴더는 Muse-Agent 실행과 설치에 필요한 파일을 포함합니다.

## 먼저 볼 문서

- `설치가이드.md`: 설치와 제거 방법
- `사용설명서.md`: 서비스 화면 메뉴별 사용자 가이드
- `service/README.md`: Windows 서비스 동작 설명

## 빠른 설치

1. 이 폴더 전체를 원하는 위치에 둡니다.
2. `agent_install.bat`를 관리자 권한으로 실행합니다.
3. 브라우저에서 `http://127.0.0.1:8484` 화면이 열리면 초기 설정을 진행합니다.

## 설치 후 권장 순서

1. `설치가이드.md`를 읽고 설치 상태를 점검합니다.
2. `사용설명서.md`를 보면서 화면 메뉴를 익힙니다.
3. 대시보드에서 Echo Server 설정, 디바이스 등록, 정리 경로 설정, 작업 관리 영역을 순서대로 확인합니다.

## 포함 파일 설명

- `README.md`
	- 배포 폴더의 사용 순서와 핵심 파일을 빠르게 안내하는 문서입니다.
- `설치가이드.md`
	- 관리자 권한 설치, 제거, 설치 후 점검 절차를 설명합니다.
- `사용설명서.md`
	- 대시보드 메뉴별 기능과 운영 순서를 설명합니다.
- `agent_install.bat`
	- 통합 설치 스크립트입니다.
	- JRE 준비, Windows 서비스 설치/시작, 바로가기 생성, 트레이 실행, 대시보드 오픈을 수행합니다.
	- 생성 바로가기:
	  - 바탕화면 `Muse-Agent Tray.lnk`: 트레이 앱 실행
	  - 바탕화면 `Muse-Agent Dashboard.url`: 기본 브라우저로 대시보드 열기
	  - 시작 메뉴 `Muse-Agent Tray.lnk`: 트레이 앱 실행
- `agent_uninstall.bat`
	- 통합 제거 스크립트입니다.
	- 서비스/트레이/바로가기 정리를 수행합니다.
	- 제거 대상 바로가기: `Muse-Agent Tray.lnk`, `Muse-Agent Dashboard.url`
- `Muse-Agent.bat`
	- 서비스 설치 없이 콘솔에서 직접 실행하는 스크립트입니다.
	- 배포 폴더의 최신 `ifonly-muse-*.jar`를 자동 탐색해 실행합니다.
	- 기본 포트 `8484`이 사용 중이면 다른 포트를 입력받아 실행할 수 있습니다.
	- 실행 전 초기화 확인 프롬프트가 표시되며 기본 운영 정책은 `N`(초기화 안 함)입니다.
- `Muse-Agent-Tray.ps1`
	- 트레이 아이콘 앱 실행 스크립트입니다.
	- 상태 확인, 대시보드 열기, 종료 동작을 제공합니다.
- `download-jre.ps1`
	- 설치 시 필요한 JRE를 내려받아 `jre/` 폴더를 구성합니다.
- `create-shortcuts.ps1`
	- 바탕화면/시작 메뉴 바로가기를 생성합니다.
	- 생성 파일:
	  - 바탕화면 `Muse-Agent Tray.lnk`
	  - 바탕화면 `Muse-Agent Dashboard.url`
	  - 시작 메뉴 `Muse-Agent Tray.lnk`
- `muse-agent-<version>.jar`
	- Muse-Agent 실제 애플리케이션 본체입니다.
- `service/`
	- Windows 서비스(WinSW) 실행 파일, 설정, 로그가 위치합니다.
	- 상세 내용은 `service/README.md`를 참고합니다.
- `jre/`
	- 번들 Java 런타임 폴더입니다.
	- 없으면 `agent_install.bat` 실행 시 자동 구성됩니다.
- `logs/`
	- 애플리케이션/설치 실행 로그가 저장되는 폴더입니다.
- `data/`
	- 런타임 데이터 폴더입니다.
	- 기본 SQLite DB(`data/muse-agent.db`)와 민감 설정 파일(`data/secure-config.properties`)이 생성됩니다.

## 참고

- 브라우저가 자동으로 열리지 않으면 `http://127.0.0.1:8484`로 직접 접속합니다.
- 포트를 바꿔 실행했다면 콘솔에 표시된 주소로 접속합니다.
- 사용자 문서는 패키지 루트에서 바로 확인할 수 있습니다.

