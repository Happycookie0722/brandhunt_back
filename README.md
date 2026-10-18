# BrandHunt Backend

## 필수 설치 및 준비

| 항목 | 준비할 내용 | 필수 여부 |
|---|---|---|
| JDK | **Java 17 이상** 설치 | 필수 |
| Git | 저장소 clone 및 버전 관리 | 필수 |
| MySQL | 실행 후 `brandhunt` 데이터베이스 생성 | 필수 |
| Redis | 로컬 Redis 서버 실행 | Redis 기능 사용 시 필수 |
| Google 계정 앱 비밀번호 | Gmail SMTP 메일 발송 설정 | 메일 기능 사용 시 |
| Chrome | Selenium 상품 크롤링 실행용 브라우저 | 크롤링 기능 사용 시 |
| IntelliJ IDEA | 프로젝트 실행용 IDE | 권장 |
| IntelliJ Lombok 플러그인 | Lombok 코드 인식 지원 | IntelliJ 사용 시 권장 |

Gradle은 프로젝트에 포함된 Gradle Wrapper를 사용하므로 별도 설치할 필요가 없습니다. Selenium WebDriver는 WebDriverManager가 관리하므로 ChromeDriver를 직접 설치할 필요는 없습니다.

## IntelliJ 설정

1. 프로젝트 SDK를 Java 17 이상으로 설정합니다.
2. `Settings → Build, Execution, Deployment → Build Tools → Gradle`에서 **Gradle JVM도 Java 17 이상**으로 설정합니다.
3. Lombok 플러그인을 설치했다면 `Settings → Build, Execution, Deployment → Compiler → Annotation Processors`에서 annotation processing을 활성화합니다.

## 로컬 환경변수 설정

프로젝트 루트에 `.env` 파일을 직접 생성하고 아래 항목을 로컬 환경에 맞게 설정합니다. 이 파일에는 비밀값이 포함되므로 Git에 커밋하지 마세요.

```properties
DB_URL=jdbc:mysql://localhost:3306/brandhunt?serverTimezone=Asia/Seoul
DB_USERNAME=본인의_MySQL_사용자명
DB_PASSWORD=본인의_MySQL_비밀번호
JWT_SECRET=32바이트_이상의_충분히_긴_임의_비밀키
REDIS_HOST=localhost
REDIS_PORT=6379
MAIL_USERNAME=본인의_Gmail_주소
MAIL_PASSWORD=Google_앱_비밀번호
```

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`은 로컬 실행 전에 설정해야 합니다.
- `JWT_SECRET`에는 짧은 문자열이나 위 예시 문구를 사용하지 말고, 충분히 긴 임의의 비밀값을 생성해 사용합니다.
- Redis 및 Gmail 설정은 해당 기능을 사용할 때 실제 서버 정보와 인증값을 입력합니다.
- `.env` 파일은 프로젝트 루트에 두고, IntelliJ 실행 설정의 작업 디렉터리도 프로젝트 루트로 유지합니다.

MySQL에서 데이터베이스를 생성합니다.

```sql
CREATE DATABASE brandhunt CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 실행

Windows PowerShell:

```powershell
.\gradlew.bat clean build
.\gradlew.bat bootRun
```

macOS/Linux:

```bash
./gradlew clean build
./gradlew bootRun
```

서버 기본 포트는 `8000`입니다.
