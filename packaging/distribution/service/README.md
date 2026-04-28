# Windows 서비스 등록 가이드

Muse-Agent를 Windows 서비스로 등록하면 OS 시작 시 자동으로 실행됩니다.

---

## 서비스 등록 방법

### 0. WinSW 실행파일 준비

이 폴더에는 설정 파일(`muse-agent-service.xml`)만 포함되어 있습니다.
서비스 등록 전에 WinSW 실행파일을 내려받아 아래 이름으로 배치해야 합니다.

```cmd
cd C:\Muse-Agent\service
powershell -ExecutionPolicy Bypass -File .\download-winsw.ps1
```

준비 완료 후 `muse-agent-service.exe` 파일이 생성되어야 합니다.

### 1. 관리자 권한으로 명령 프롬프트 실행

**Windows 검색** → "cmd" 입력 → **우클릭** → **"관리자 권한으로 실행"**

### 2. 서비스 폴더로 이동

```cmd
cd C:\Muse-Agent\service
```

(경로는 실제 설치 위치에 맞게 변경)

### 3. 서비스 설치 및 시작

```cmd
muse-agent-service.exe install
muse-agent-service.exe start
```

### 4. 설치 확인

```cmd
muse-agent-service.exe status
```

또는 **Windows 서비스 관리자**에서 확인:
```
Windows + R → services.msc 입력 → 확인
```

서비스 이름: **"Muse-Agent Service"**

---

## 서비스 관리 명령어

### 서비스 시작
```cmd
muse-agent-service.exe start
```

### 서비스 중지
```cmd
muse-agent-service.exe stop
```

### 서비스 재시작
```cmd
muse-agent-service.exe restart
```

### 서비스 상태 확인
```cmd
muse-agent-service.exe status
```

### 서비스 제거
```cmd
muse-agent-service.exe stop
muse-agent-service.exe uninstall
```

---

## 서비스 설정

### 설정 파일

`muse-agent-service.xml` 파일에서 서비스 설정을 변경할 수 있습니다.

**주요 설정 항목:**

```xml
<service>
  <id>muse-agent</id>                      <!-- 서비스 ID -->
  <name>Muse-Agent Service</name>         <!-- 서비스 이름 -->
  <description>...</description>             <!-- 설명 -->
  
  <startmode>Automatic</startmode>           <!-- 시작 모드 -->
  
  <!-- JVM 메모리 설정 변경 가능 -->
  <executable>%BASE%\..\jre\bin\java.exe</executable>
  <arguments>-Xms256m -Xmx1024m -jar "%BASE%\..\muse-agent-<version>.jar"</arguments>
</service>
```

### 메모리 설정 변경

더 많은 메모리가 필요한 경우:

```xml
  <arguments>-Xms512m -Xmx2048m -jar "%BASE%\..\muse-agent-<version>.jar"</arguments>
```

---

## 서비스 로그

### 로그 파일 위치

- **애플리케이션 로그**: `..\logs\muse-agent.log`
- **서비스 래퍼 로그**: `logs\muse-agent-service.log`

### 로그 확인

```cmd
# 애플리케이션 로그
type ..\logs\muse-agent.log

# 서비스 로그
type logs\muse-agent-service.log
```

---

## 문제 해결

### 서비스 설치 실패

**오류**: "액세스가 거부되었습니다"

**해결**: 관리자 권한으로 명령 프롬프트 실행

### 서비스가 시작되지 않음

**확인 사항:**
1. Java 런타임 확인: `..\jre\bin\java.exe` 파일이 있는지
2. JAR 파일 확인: `..\muse-agent-<version>.jar` 파일이 있는지
3. WinSW 실행파일 확인: `muse-agent-service.exe` 파일이 있는지
4. 서비스 로그 확인: `logs\muse-agent-service.log`

### 포트 충돌

다른 프로그램이 8484 포트를 사용 중:

```cmd
# 포트 사용 확인
netstat -ano | findstr :8484

# 프로세스 종료
taskkill /F /PID [PID번호]
```

---

## WinSW 정보

WinSW는 MIT 라이선스의 오픈소스 프로젝트입니다.

- **공식 저장소**: https://github.com/winsw/winsw
- **버전**: v3.0.0-alpha.11
- **라이선스**: MIT

---

**서비스 등록을 원하지 않는 경우**, 그냥 `Muse-Agent.bat`을 실행하여 사용하세요.

