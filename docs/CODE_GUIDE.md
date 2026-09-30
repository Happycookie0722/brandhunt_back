# BrandHunt Backend 코드 분석 가이드

> 목적: 신입 개발자가 BrandHunt 백엔드의 파일 역할과 요청 흐름을 빠르게 이해할 수 있도록 작성한 교육 문서다.
>
> 기본 구조는 **Controller → Service → Repository → Entity(DB)** 이며,
> 외부 브랜드 상품 수집은 **Controller → ProductService → SeleniumService → ProductService → Repository** 흐름으로 동작한다.

## 1. 전체 구조

### 일반적인 API 요청

```
브라우저
  ↓ HTTP 요청
Controller
  ↓ 입력 검증 / 인증 사용자 추출
Service
  ↓ 비즈니스 규칙
Repository
  ↓ JPA
MySQL
```

응답은 보통 Entity를 직접 반환하지 않고 DTO로 변환한다.

### 상품 크롤링

```
POST /products/crawling
        ↓
ProductController
        ↓
ProductService.crawlingItem()
        ↓
SeleniumService.getNikeProduct()
SeleniumService.getAdidasProduct()
        ↓
ProductCrawlDto 목록
        ↓
상품 식별
(externalProductId → name fallback)
        ↓
신규 저장 / 가격 갱신 / active 상태 갱신
        ↓
이번 크롤링에서 사라진 상품 active=false
        ↓
CrawlResultDto
```

### 상품 노출

```
GET /products/list
        ↓
ProductController
        ↓
ProductService.getProducts()
        ↓
ProductRepository.findByActiveTrue()
        ↓
ProductListDto
        ↓
프론트엔드
```

따라서 판매 종료 상품은 DB에 남아 있어도 사용자 목록에서는 나오지 않는다.

---

# 2. Controller

## AuthController.java

**역할:** 로그인/회원가입/이메일 인증/토큰 갱신/로그아웃 HTTP API를 제공한다.

핵심 흐름:
- `/auth/login` → AuthService 인증 → Access/Refresh Token 발급
- `/auth/refresh` → 저장된 Refresh Token과 비교 → Token Rotation
- `/auth/logout` → Access Token 폐기 + Refresh Token 삭제
- `/auth/signup` → UserService 회원가입
- `/auth/send-verification` → 인증 메일 발송
- `/auth/verify-code` → 인증코드 검증

교육 포인트:
- Controller는 비밀번호 검증 같은 업무 로직을 직접 처리하지 않는다.
- `@Valid`는 DTO validation을 실행한다.
- `@AuthenticationPrincipal`은 Spring Security가 인증한 사용자를 가져온다.

## ProductController.java

**역할:** 상품 목록/검색/상세/크롤링/가격알림 API를 제공한다.

핵심 흐름:
- `GET /products/list` → 현재 판매 중인 상품 목록
- `GET /products/{id}` → 현재 판매 중인 상품 상세
- `POST /products/search` → 현재 판매 중인 상품 검색
- `POST /products/crawling` → 관리자 크롤링
- `GET/POST /products/{id}/price-alert` → 사용자별 가격 알림 구독

특히 크롤링 API는 SecurityConfig에서 ADMIN 권한으로 제한한다.

## SearchController.java

**역할:** 인기 검색어 등의 검색 관련 HTTP 진입점이다.

Controller는 HTTP 요청을 받고 SearchService에 업무 처리를 위임한다.

## UserController.java

**역할:** 사용자 정보 및 닉네임 중복 확인 등 사용자 관련 API의 진입점이다.

인증이 필요한 API에서는 현재 로그인한 사용자의 Principal을 사용한다.

---

# 3. Service

## ProductService.java

**가장 중요한 상품 비즈니스 로직이다.**

### `crawlingItem()`

1. Nike/Adidas를 각각 크롤링한다.
2. 각 결과를 ProductCrawlDto로 받는다.
3. 브랜드를 찾거나 새로 만든다.
4. externalProductId로 기존 상품을 먼저 찾는다.
5. 과거 데이터 호환을 위해 이름으로 한 번 더 찾는다.
6. 신규 상품이면 저장한다.
7. 기존 상품이면 가격과 판매 상태를 비교한다.
8. 품절이면 `active=false`로 만든다.
9. 목록에서 완전히 사라진 상품도 `active=false`로 만든다.
10. 다음 크롤링에서 다시 발견되면 `active=true`로 복구한다.

### 왜 externalProductId가 중요한가?

상품명은 변경될 수 있다.

예:
- 기존: `Air Max 1`
- 변경: `Nike Air Max 1`

이때 이름만 기준으로 사용하면 새 상품으로 잘못 저장될 수 있다.
브랜드가 제공하는 외부 ID는 같은 상품을 식별하기 위한 안정적인 키이므로 우선 사용한다.

### `active`의 의미

`active`는 DB에 존재하는지 여부가 아니라 **사용자에게 현재 판매 상품으로 보여줄지 여부**다.

- true → 현재 판매/조회 가능
- false → 과거 상품, 품절, 판매 종료

물리 삭제하지 않는 이유는 가격 알림/이력 등의 데이터를 보존하기 위해서다.

## SeleniumService.java

**역할:** Nike와 Adidas 웹페이지를 Selenium으로 열고 상품 데이터를 수집한다.

주의:
- 이 클래스는 DB를 수정하지 않는다.
- `ProductCrawlDto`만 생성한다.
- 품절 상태도 같이 수집한다.

Nike:
- 상품 카드의 텍스트에 `품절`이 있으면 `soldOut=true`.

Adidas:
- Adidas가 제공하는 `data-testid='sold-out'` 요소를 확인한다.

## AuthService.java

**역할:** 인증의 실제 업무 규칙을 담당한다.

핵심:
- 로그인 비밀번호 검증
- 로그인 시도 제한
- 이메일 인증코드 생성/발송
- 인증코드 시도 제한
- 사용자 상태 검사

비밀번호 자체는 로그에 출력하면 안 된다.

## UserService.java

**역할:** 회원가입과 사용자 관련 업무를 담당한다.

회원가입 흐름:
1. 이메일 중복 확인
2. 닉네임 중복 확인
3. 이메일 인증 여부 확인
4. PasswordEncoder로 비밀번호 해시
5. User Entity 생성
6. DB 저장

## SearchService.java

**역할:** 검색 실행과 검색 로그/인기 검색어 처리를 담당한다.

상품 검색 자체는 ProductRepository가 수행하고,
검색어 기록과 Redis 인기 검색어 증가는 Service에서 관리한다.

## PriceAlertService.java

**역할:** 사용자별 상품 가격 알림 구독을 관리한다.

핵심 관계:

```
User 1 ─── N PriceAlert N ─── 1 Product
```

즉 한 사용자는 여러 상품을 구독할 수 있고,
한 상품도 여러 사용자가 구독할 수 있다.

SALEPRICE가 변경되면 해당 상품의 구독자에게 Notification을 생성한다.

## CategoryService.java

**역할:** 크롤링한 카테고리 문자열을 DB Category와 매칭한다.

## RedisService.java

**역할:** Redis에 저장하는 인증/검색/로그인 제한 데이터를 한 곳에서 관리한다.

주요 키:
- `verifyCode:` → 이메일 인증
- `refresh:` → Refresh Token
- `popularKeyword:` → 인기 검색어
- `login:email:` → 이메일별 로그인 시도 제한
- `login:ip:` → IP별 로그인 시도 제한

## MailService.java

**역할:** JavaMailSender를 이용해 이메일 인증 메일을 발송한다.

메일 계정과 비밀번호는 환경변수로 관리한다.

---

# 4. Repository

## ProductRepository.java

Product DB 접근 계층이다.

중요 메서드:

- `findByActiveTrue()` → 판매 중 상품만 조회
- `findByNameContainingIgnoreCaseAndActiveTrue()` → 판매 중 상품 검색
- `findByIdAndActiveTrue()` → 판매 중 상세 조회
- `findByBrandIdAndExternalProductId()` → 크롤링 상품 식별
- `findByBrandId()` → 크롤링에서 사라진 상품 탐지

Repository 메서드 이름 자체가 SQL 조건이 된다.

## BrandRepository.java

브랜드 이름으로 Brand를 조회한다.

## CategoryRepository.java

카테고리 DB 접근을 담당한다.

## UserRepository.java

이메일/닉네임 등을 기준으로 사용자를 조회한다.

## PriceAlertRepository.java

사용자와 상품의 가격 알림 구독 관계를 조회한다.

## NotificationRepository.java

사용자별 가격 변경 알림을 조회/저장한다.

## SearchRepository.java

검색 기록을 저장한다.

---

# 5. Entity

## Product.java

상품의 DB 모델이다.

중요 변수:
- `id` → BrandHunt 내부 상품 ID
- `brand` → 브랜드
- `category` → 카테고리
- `name` → 상품명
- `price` → 정가
- `salePrice` → 할인가
- `productUrl` → 공식 상품 페이지
- `externalProductId` → 브랜드 사이트 상품 ID
- `gender` → 성별
- `active` → 현재 판매 여부

## Brand.java

브랜드 정보를 저장한다.

Brand → Product는 1:N 관계다.

## Category.java

카테고리를 저장한다.

Brand → Category가 1:N이고 Product → Category가 N:1이다.

## User.java

회원 계정 정보를 저장한다.

중요 변수:
- email
- nickName
- password
- role
- status

비밀번호는 평문으로 저장하지 않고 PasswordEncoder 결과를 저장한다.

## PriceAlert.java

특정 사용자가 특정 상품의 가격 알림을 구독했다는 관계 데이터다.

## Notification.java

가격 변경 등 사용자에게 보여줄 알림을 저장한다.

Push 알림과 DB 알림은 별개의 개념이다.
현재 Notification은 DB 알림 기록 역할을 한다.

## Favorite.java

사용자가 상품을 즐겨찾기한 관계를 저장한다.

## SearchLog.java

검색어와 검색 활동을 기록한다.

## UserSearchHistory.java

사용자 개인 검색 이력을 저장한다.

## ChatRoom.java / ChatMember.java / ChatMessage.java / MessageHistory.java

채팅 기능을 위한 데이터 모델이다.

- ChatRoom → 채팅방
- ChatMember → 사용자와 채팅방 관계
- ChatMessage → 채팅 메시지
- MessageHistory → 메시지 이력

---

# 6. DTO

DTO는 **API나 계층 사이에서 데이터를 전달하기 위한 객체**다.

## SignUpDto.java

회원가입 입력값.

- email → 이메일
- password → 비밀번호
- nickName → 닉네임

Validation을 통해 잘못된 입력을 Service에 전달하기 전에 차단한다.

## LoginRequestDto.java

로그인 요청의 이메일과 비밀번호를 전달한다.

## TokenResponseDto.java

Access Token과 Refresh Token을 프론트엔드에 전달한다.

## EmailVerifyDto.java

이메일과 6자리 인증코드를 전달한다.

## ProductDto.java

상품 검색어 등의 입력값을 전달한다.

## ProductCrawlDto.java

Selenium → ProductService로 크롤링 결과를 전달한다.

특히 `soldOut`은 브랜드 사이트에서 현재 판매 불가능한 상품인지 나타낸다.

## ProductListDto.java

상품 목록 화면에 필요한 최소 데이터만 반환한다.

## ProductDetailDto.java

상품 상세 화면에 필요한 데이터를 반환한다.

## CrawlResultDto.java

크롤링 결과 통계다.

- total
- inserted
- updated

---

# 7. Common

## JwtUtil.java

JWT 생성/검증을 담당한다.

Access Token과 Refresh Token은 `type` claim으로 구분한다.

또한 Access Token에는 JTI를 부여하여 로그아웃 시 Redis에 폐기 목록을 저장할 수 있다.

## CustomException.java

프로젝트에서 정의한 비즈니스 예외를 표현한다.

## GlobalExceptionHandler.java

Controller에서 발생한 예외를 공통 HTTP 응답으로 변환한다.

장점:
- Controller마다 try/catch를 반복하지 않는다.
- 내부 예외 메시지나 stack trace를 사용자에게 노출하지 않는다.

## RedisUtil.java

Redis 관련 공통 기능을 제공하는 보조 클래스다.

---

# 8. Security

## JwtAuthenticationFilter.java

모든 HTTP 요청에서 Authorization 헤더를 확인한다.

흐름:

```
Authorization: Bearer <JWT>
        ↓
JWT 형식 확인
        ↓
Access Token인지 확인
        ↓
폐기된 토큰인지 확인
        ↓
email 추출
        ↓
UserDetails 조회
        ↓
SecurityContext에 인증 정보 저장
```

중요:
Refresh Token은 API 인증용으로 사용할 수 없다.

## UserPrincipal.java

Spring Security가 이해할 수 있는 UserDetails 구현체다.

User Entity의 role을:
- USER → ROLE_USER
- ADMIN → ROLE_ADMIN

형태로 변환한다.

## CustomUserDetailService.java

email을 기준으로 User를 찾아 Spring Security의 UserDetails로 변환한다.

---

# 9. Config

## SecurityConfig.java

URL별 접근 권한을 정의한다.

예:
- 로그인/회원가입 → 공개
- 상품 목록 → 공개
- 상품 상세 → 공개
- 가격 알림 → 로그인 필요
- 크롤링 → ADMIN 필요

비밀번호 암호화에는 BCryptPasswordEncoder를 사용한다.

## WebConfig.java

CORS 정책을 설정한다.

허용 Origin은 환경변수로 관리하여 운영 서버에서 개발 서버 Origin을 그대로 사용하는 문제를 방지한다.

## RedisConfig.java

Redis 연결과 관련된 Spring Bean을 설정한다.

## SeleniumConfig.java

Selenium WebDriver 생성 옵션을 관리한다.

---

# 10. Constant

## Gender.java

상품 성별을 enum으로 관리한다.

## UserStatus.java

사용자의 계정 상태를 표현한다.

## UserRole.java

권한을 표현한다.

- USER
- ADMIN

## SiteType.java

크롤링 대상 사이트를 표현한다.

## ErrorCode.java

애플리케이션에서 사용하는 오류 코드와 HTTP 상태를 한 곳에서 관리한다.

## ChatRoomType.java / MessageType.java

채팅방 및 메시지 종류를 enum으로 관리한다.

---

# 11. Application

## BrandHuntApplication.java

Spring Boot 애플리케이션의 시작점이다.

`main()`을 실행하면 Spring Application Context가 생성되고
Controller/Service/Repository 등의 Bean이 등록된다.

---

# 12. 크롤링 문제를 해결한 이유

기존 구조에서는:

```
브랜드 사이트
   ↓
상품 발견
   ↓
DB 저장
   ↓
다음 크롤링
   ↓
가격만 변경
```

상품이 브랜드 사이트에서 판매 종료되어도 DB에서 삭제하거나 상태를 변경하지 않았기 때문에:

```
브랜드 사이트: 상품 없음
BrandHunt DB: 상품 있음
BrandHunt 검색: 상품 노출
```

문제가 발생했다.

현재 구조는:

```
브랜드 사이트
   ↓
크롤링
   ↓
soldOut 여부 확인
   ↓
Product.active
   ├─ 판매 가능 → true
   └─ 품절 → false

그리고

이번 크롤링에 존재하지 않는 기존 상품
   ↓
active=false
```

사용자 조회는 항상:

```
active=true
```

조건을 사용한다.

따라서 DB에 과거 데이터를 보존하면서도 사용자에게는 현재 판매 상품만 보여준다.

---

# 13. 신입 개발자가 코드를 읽는 순서

상품 기능을 공부한다면 다음 순서로 읽는 것을 추천한다.

1. ProductController
2. ProductService
3. ProductRepository
4. Product Entity
5. ProductListDto / ProductDetailDto
6. SeleniumService
7. ProductCrawlDto
8. Brand / Category Entity
9. PriceAlertService
10. PriceAlert / Notification Entity

인증 기능은:

1. AuthController
2. AuthService
3. RedisService
4. JwtUtil
5. JwtAuthenticationFilter
6. UserPrincipal
7. SecurityConfig
8. User Entity
9. UserRepository

순서로 읽으면 요청이 어디에서 들어와서 어디까지 내려가는지 이해하기 쉽다.
