# Spring Cloud MSA Platform

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-green)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.3-blue)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.13-FF6600)

Spring Cloud 기반 **마이크로서비스 아키텍처 플랫폼**입니다.
서비스 디스커버리, API Gateway, 동기/비동기 통신, Circuit Breaker 등 MSA 핵심 패턴을 구현합니다.

## Why MSA?

| 모놀리식 문제 | MSA 해결 |
|-------------|---------|
| 하나의 서비스 장애 → 전체 다운 | **Circuit Breaker** — 장애 격리, fallback 응답 |
| 서비스 주소 하드코딩 | **Eureka** — 동적 서비스 디스커버리 |
| 인증 로직 분산 | **API Gateway** — 단일 인증 게이트 |
| 동기 호출 병목 | **RabbitMQ** — 비동기 이벤트 처리 |
| 배포 단위가 큼 | **모듈별 독립 배포** — 영향 범위 최소화 |

## Architecture

```mermaid
graph TB
    subgraph Client
        WEB["Web / Mobile Client"]
    end

    subgraph Gateway["API Gateway :8080"]
        GW["Spring Cloud Gateway"]
        JWT["JWT Filter"]
        CB["Circuit Breaker<br/>Resilience4j"]
    end

    subgraph Discovery["Service Discovery"]
        EUR["Eureka Server :8761"]
    end

    subgraph Services["Microservices"]
        AUTH["Auth Service :8081<br/>JWT 발급 · 검증<br/>BCrypt 암호화"]
        USER["User Service :8082<br/>프로필 관리<br/>OpenFeign 동기 호출"]
        NOTI["Notification Service :8083<br/>이벤트 소비<br/>알림 발송"]
    end

    subgraph Messaging["Async Messaging"]
        RMQ["RabbitMQ :5672<br/>Exchange: user.events<br/>Queue: notification.user.profile"]
    end

    subgraph Data["Data Layer"]
        DB1["H2 / MySQL<br/>Auth DB"]
        DB2["H2 / MySQL<br/>User DB"]
    end

    WEB --> GW
    GW --> JWT
    JWT --> CB
    GW -- "lb://auth-service" --> AUTH
    GW -- "lb://user-service" --> USER
    GW -- "lb://notification-service" --> NOTI

    AUTH -.-> EUR
    USER -.-> EUR
    NOTI -.-> EUR

    USER -- "OpenFeign<br/>동기 호출" --> AUTH
    USER -- "RabbitTemplate<br/>비동기 발행" --> RMQ
    RMQ -- "@RabbitListener<br/>비동기 소비" --> NOTI

    AUTH --> DB1
    USER --> DB2
```

## 핵심 패턴

### 1. Service Discovery (Eureka)

```mermaid
sequenceDiagram
    participant AUTH as Auth Service
    participant USER as User Service
    participant EUR as Eureka Server
    participant GW as API Gateway

    AUTH->>EUR: Register (auth-service, :8081)
    USER->>EUR: Register (user-service, :8082)
    EUR-->>GW: Service Registry 동기화

    GW->>EUR: Lookup "user-service"
    EUR-->>GW: [192.168.1.10:8082, 192.168.1.11:8082]
    GW->>USER: Load Balanced Request (lb://user-service)
```

- 서비스가 시작되면 Eureka에 자동 등록
- Gateway는 서비스 이름(논리명)으로 라우팅 → IP/포트 하드코딩 불필요
- 인스턴스를 추가하면 자동으로 로드밸런싱 대상에 포함

### 2. API Gateway + JWT 인증

```mermaid
sequenceDiagram
    participant C as Client
    participant GW as API Gateway
    participant JWT as JWT Filter
    participant AUTH as Auth Service
    participant USER as User Service

    C->>GW: POST /api/auth/login
    Note over JWT: /api/auth/** → 인증 SKIP
    GW->>AUTH: Forward (StripPrefix=1)
    AUTH-->>GW: {token: "eyJ..."}
    GW-->>C: {token: "eyJ..."}

    C->>GW: GET /api/users/1/profile<br/>Authorization: Bearer eyJ...
    GW->>JWT: 토큰 검증
    Note over JWT: 유효 → X-User-Id 헤더 추가<br/>무효 → 401 Unauthorized
    JWT->>GW: X-User-Id: 1
    GW->>USER: Forward + X-User-Id: 1
    USER-->>GW: {nickname: "..."}
    GW-->>C: {nickname: "..."}
```

**설계 포인트:**
- 인증은 Gateway에서 1회만 수행 → 각 서비스는 `X-User-Id` 헤더만 신뢰
- `/api/auth/**`, `/actuator/**` 경로는 인증 제외
- 토큰 검증 실패 시 Gateway에서 즉시 401 반환 (서비스까지 도달하지 않음)

### 3. 동기 통신 (OpenFeign) + Circuit Breaker

```mermaid
sequenceDiagram
    participant C as Client
    participant USER as User Service
    participant AUTH as Auth Service
    participant CB as Circuit Breaker

    C->>USER: GET /users/1/profile
    USER->>AUTH: OpenFeign: GET /auth/users/1
    AUTH-->>USER: {email, name, role}
    Note over USER: UserProfile (local) + AuthUser (Feign) 조합
    USER-->>C: {nickname, bio, email, name}

    Note over AUTH: Auth Service 장애 발생!
    C->>USER: GET /users/2/profile
    USER->>CB: OpenFeign 호출 시도
    Note over CB: 실패율 50% 초과<br/>→ Circuit OPEN
    CB-->>USER: FallbackFactory 응답
    Note over USER: 기본 사용자 정보로 응답<br/>(email: "unavailable")
    USER-->>C: {nickname, bio, email: "unavailable"}
```

```java
// OpenFeign 클라이언트 + Fallback
@FeignClient(name = "auth-service", fallbackFactory = AuthServiceClientFallback.class)
public interface AuthServiceClient {
    @GetMapping("/auth/users/{userId}")
    AuthUserResponse getUserById(@PathVariable("userId") Long userId);
}

// 장애 시 기본값 반환 (서비스 전체 다운 방지)
@Component
public class AuthServiceClientFallback implements FallbackFactory<AuthServiceClient> {
    @Override
    public AuthServiceClient create(Throwable cause) {
        return userId -> new AuthUserResponse(userId, "unavailable", "Unknown", "USER");
    }
}
```

### 4. 비동기 통신 (RabbitMQ)

```mermaid
sequenceDiagram
    participant USER as User Service
    participant RMQ as RabbitMQ
    participant NOTI as Notification Service

    USER->>USER: updateProfile(userId, nickname)
    USER->>RMQ: Publish to "user.events"<br/>routing key: "user.profile.updated"<br/>{userId, nickname, eventType, timestamp}

    Note over USER: 즉시 응답 (비동기)
    USER-->>USER: return updated profile

    RMQ->>NOTI: @RabbitListener<br/>Queue: "notification.user.profile"
    Note over NOTI: 알림 기록 저장<br/>이메일/푸시 발송 (확장)
```

**동기 vs 비동기 선택 기준:**

| 상황 | 방식 | 이유 |
|------|------|------|
| 프로필 조회 시 Auth 정보 필요 | **OpenFeign (동기)** | 응답에 Auth 데이터가 필수 |
| 프로필 변경 후 알림 발송 | **RabbitMQ (비동기)** | 알림 실패가 프로필 변경을 롤백하면 안 됨 |

### 5. Circuit Breaker (Resilience4j)

Gateway 레벨에서도 Circuit Breaker를 적용합니다:

```yaml
# api-gateway application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          filters:
            - name: CircuitBreaker
              args:
                name: userServiceCB
                fallbackUri: forward:/fallback/users

resilience4j:
  circuitbreaker:
    instances:
      userServiceCB:
        slidingWindowSize: 10          # 최근 10개 요청 기준
        failureRateThreshold: 50       # 50% 실패 시 OPEN
        waitDurationInOpenState: 10000  # 10초 후 HALF_OPEN
        permittedNumberOfCallsInHalfOpenState: 3  # 3개 요청으로 재시도
```

```
CLOSED ──(실패율 50% 초과)──→ OPEN ──(10초 대기)──→ HALF_OPEN
  ↑                                                    │
  └──────────(3개 요청 성공)────────────────────────────┘
```

## Module Structure

```
spring-cloud-msa-platform/
├── discovery-server/          # Eureka Server (:8761)
│   └── @EnableEurekaServer
├── api-gateway/               # Spring Cloud Gateway (:8080)
│   ├── JwtAuthenticationFilter   — GlobalFilter, 토큰 검증
│   ├── FallbackController        — Circuit Breaker fallback
│   └── application.yml           — 라우팅 + Resilience4j 설정
├── auth-service/              # 인증 서비스 (:8081)
│   ├── JwtTokenProvider          — HS256 토큰 생성/검증
│   ├── AuthService               — register/login + BCrypt
│   └── SecurityConfig            — stateless, permitAll
├── user-service/              # 사용자 서비스 (:8082)
│   ├── AuthServiceClient         — @FeignClient + FallbackFactory
│   ├── UserService               — API Composition (Feign + Local DB)
│   ├── UserEventPublisher        — RabbitTemplate 이벤트 발행
│   └── RabbitMQConfig            — Exchange 선언
├── notification-service/      # 알림 서비스 (:8083)
│   ├── UserEventConsumer         — @RabbitListener 이벤트 소비
│   ├── RabbitMQConfig            — Queue + Binding 선언
│   └── NotificationRecord        — 알림 이력
├── docker-compose.yml         # RabbitMQ
└── build.gradle               # 멀티 모듈 루트
```

## Quick Start

```bash
# 1. RabbitMQ 실행
docker-compose up -d

# 2. 서비스 순서대로 실행
./gradlew :discovery-server:bootRun &    # Eureka (:8761)
sleep 5
./gradlew :auth-service:bootRun &        # Auth (:8081)
./gradlew :user-service:bootRun &        # User (:8082)
./gradlew :notification-service:bootRun & # Notification (:8083)
./gradlew :api-gateway:bootRun &         # Gateway (:8080)

# 3. Eureka Dashboard 확인
open http://localhost:8761

# 4. API 테스트
# 회원가입
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"1234","name":"테스트"}'

# 로그인 → 토큰 획득
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"1234"}' | jq -r '.token')

# 프로필 조회 (Gateway → JWT 검증 → User Service → Feign → Auth Service)
curl http://localhost:8080/api/users/1/profile \
  -H "Authorization: Bearer $TOKEN"
```

## Tech Stack

| Category | Technology |
|----------|-----------|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.3.4, Spring Cloud 2023.0.3 |
| **Discovery** | Netflix Eureka Server / Client |
| **Gateway** | Spring Cloud Gateway (Reactive) |
| **Sync Communication** | OpenFeign + Ribbon Load Balancer |
| **Async Messaging** | RabbitMQ 3.13 + Spring AMQP |
| **Resilience** | Resilience4j Circuit Breaker, Fallback |
| **Auth** | JWT (JJWT 0.11.5), BCrypt, Spring Security |
| **Database** | H2 (dev) / MySQL (prod), Spring Data JPA |
| **Build** | Gradle 8.10 Multi-module |
| **Infra** | Docker Compose |

## License

This project is for portfolio purposes.
