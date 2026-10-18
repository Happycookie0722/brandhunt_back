# BrandHunt Backend

## 로컬 개발 환경 설정

IntelliJ IDEA에 환경변수를 하나씩 등록하지 않고, 프로젝트 루트의 `.env` 파일로 로컬 설정을 관리합니다.

### 1. 사전 준비

- Java 17
- MySQL 실행 및 `brandhunt` 데이터베이스 생성
- Redis 실행 (Redis 기능을 사용하는 경우)

MySQL에서 데이터베이스를 생성합니다.

```sql
CREATE DATABASE brandhunt CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 환경변수 파일 만들기

프로젝트 루트에서 `.env.example`을 복사해 `.env` 파일을 만듭니다.

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

macOS/Linux:

```bash
cp .env.example .env
```

`.env`의 `DB_USERNAME`과 `DB_PASSWORD`를 본인의 로컬 MySQL 계정에 맞게 수정합니다. 이메일 발송 기능을 사용할 경우 `MAIL_USERNAME`과 `MAIL_PASSWORD`도 설정합니다. 개발용 `JWT_SECRET`은 로컬 테스트에만 사용하고 실제 서비스에는 별도의 강력한 비밀값을 사용하세요.

### 3. 실행

IntelliJ에서 프로젝트를 열고 Spring Boot 애플리케이션을 평소처럼 실행하면 됩니다. `application.yml`이 프로젝트 루트의 `.env`를 자동으로 읽으므로 Run Configuration의 Environment variables에 값을 입력할 필요가 없습니다.

- `.env`: 개인 로컬 설정이며 Git에 커밋하지 않습니다.
- `.env.example`: 다른 개발자가 복사해 사용할 수 있도록 키 목록과 예시를 공유합니다.

`.env`는 프로젝트 루트에 있어야 합니다. IntelliJ의 실행 설정에서 Working directory를 프로젝트 루트로 유지하세요.
