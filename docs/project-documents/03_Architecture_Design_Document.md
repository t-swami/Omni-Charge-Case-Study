# OmniCharge — Architecture & Design Document

**Document Version:** 1.0  
**Project Name:** OmniCharge — Mobile Recharge Platform  
**Prepared By:** Sprint Team  
**Date:** May 2026  

---

## 1. Architecture Overview

### 1.1 Architecture Style
The OmniCharge platform follows a **Microservices Architecture** where the application is decomposed into 5 business services and 3 infrastructure services. Each service is independently developed, deployed, and scaled.

### 1.2 Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                        BROWSER (Angular 21)                        │
│                         Port: 4200                                 │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ HTTP (REST)
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     API GATEWAY (Port 8080)                        │
│  ┌──────────────────────┐  ┌──────────────────────────────────┐    │
│  │ JwtAuthFilter        │  │ Route Mapping                    │    │
│  │ - Validate JWT       │  │ /api/auth/**   → user-service    │    │
│  │ - Extract username   │  │ /api/users/**  → user-service    │    │
│  │ - Extract role       │  │ /api/operators → operator-service│    │
│  │ - Set X-Auth headers │  │ /api/plans/**  → operator-service│    │
│  └──────────────────────┘  │ /api/recharge  → recharge-service│    │
│                            │ /api/transactions → payment-svc  │    │
│                            └──────────────────────────────────┘    │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
           ┌───────────────────┼───────────────────────┐
           ▼                   ▼                       ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────────────┐
│  USER SERVICE    │ │ OPERATOR SERVICE │ │   RECHARGE SERVICE       │
│  Port: 8081      │ │ Port: 8082       │ │   Port: 8083             │
│                  │ │                  │ │                          │
│ - Registration   │ │ - CRUD Operators │ │ - Initiate Recharge      │
│ - Login (JWT)    │ │ - CRUD Plans     │ │ - Cancel Recharge        │
│ - Profile        │ │ - Redis Cache    │ │ - Recharge History       │
│ - Wallet         │ │                  │ │                          │
│ - Admin Mgmt     │ │ ┌──────────┐    │ │ ┌───────────────────┐    │
│                  │ │ │  Redis   │    │ │ │ OperatorFeignClient│    │
│ ┌────────────┐   │ │ │  Cache   │    │ │ │ (calls operator-  │    │
│ │ PostgreSQL │   │ │ └──────────┘    │ │ │  service via Feign)│    │
│ │  user_db   │   │ │ ┌──────────┐    │ │ └───────────────────┘    │
│ └────────────┘   │ │ │PostgreSQL│    │ │                          │
│                  │ │ │operator_db│   │ │ ┌────────────────────┐   │
└──────────────────┘ │ └──────────┘    │ │ │ RechargeEvent      │   │
                     └──────────────────┘ │ │ Publisher           │   │
                                          │ │ (sends to RabbitMQ) │   │
                                          │ └─────────┬──────────┘   │
                                          │ ┌─────────┘              │
                                          │ │ ┌──────────┐           │
                                          │ │ │PostgreSQL│           │
                                          │ │ │recharge_db│          │
                                          │ │ └──────────┘           │
                                          └─┼────────────────────────┘
                                            │
                                            ▼ (RabbitMQ: payment.queue)
                               ┌──────────────────────────────┐
                               │    PAYMENT SERVICE           │
                               │    Port: 8084                │
                               │                              │
                               │ - Consume Recharge Events    │
                               │ - Process Payments           │
                               │ - Wallet Top-up              │
                               │ - Retry & Circuit Breaker    │
                               │ - Auto Refund                │
                               │                              │
                               │ ┌──────────────────────────┐ │
                               │ │ DummyPaymentGateway      │ │
                               │ │ (Card, UPI, NetBanking,  │ │
                               │ │  Wallet validation)      │ │
                               │ └──────────────────────────┘ │
                               │ ┌──────────────────────────┐ │
                               │ │ RechargeServiceFeign     │ │
                               │ │ (updates recharge status)│ │
                               │ └──────────────────────────┘ │
                               │ ┌──────────────────────────┐ │
                               │ │ UserServiceFeign         │ │
                               │ │ (wallet balance mgmt)    │ │
                               │ └──────────────────────────┘ │
                               │ ┌──────────────┐             │
                               │ │ PostgreSQL   │             │
                               │ │ payment_db   │             │
                               │ └──────────────┘             │
                               └──────────────┬───────────────┘
                                              │
                                              ▼ (RabbitMQ: notification.queue)
                               ┌──────────────────────────────┐
                               │  NOTIFICATION SERVICE        │
                               │  Port: 8085                  │
                               │                              │
                               │ - Consume Payment Results    │
                               │ - Build Email Content        │
                               │ - Send via Gmail SMTP        │
                               └──────────────────────────────┘
```

---

## 2. Design Patterns Used

### 2.1 Database Per Service Pattern
Each microservice owns its own database. No service can access another service's database directly. Communication happens only through REST APIs or messaging.

**Benefit:** Data isolation, independent schema evolution, no tight coupling.

### 2.2 API Gateway Pattern
All client requests pass through a single entry point (API Gateway). The gateway handles routing, authentication, and cross-cutting concerns like CORS.

**Benefit:** Simplified client communication, centralized security.

### 2.3 Service Discovery Pattern (Eureka)
Services register themselves with a central registry (Eureka). The API Gateway and Feign clients look up service instances dynamically.

**Benefit:** No hardcoded IP addresses; supports dynamic scaling.

### 2.4 Event-Driven Architecture (RabbitMQ)
The Recharge Service publishes events to RabbitMQ. The Payment Service consumes these events asynchronously.

**Benefit:** Loose coupling between services; resilience to temporary failures.

### 2.5 Circuit Breaker Pattern
The Payment Service implements a retry mechanism with a fallback strategy when the Recharge Service is unavailable.

**Configuration:**
- Max Retry Attempts: 6
- Retry Interval: 30 seconds
- Fallback: Mark as REFUND_PENDING, auto-refund wallet

**Benefit:** Prevents cascading failures; protects user funds.

### 2.6 Cache-Aside Pattern (Redis)
The Operator Service checks Redis before querying the database. On cache miss, data is fetched from PostgreSQL and stored in Redis.

**Cache Strategy:**
- Read: `@Cacheable` — returns cached data if available.
- Write: `@CacheEvict` — invalidates cache when data changes.
- Cache Names: `plans`, `plan-by-id`, `plans-by-operator`, `operators`

**Benefit:** Reduces database load; sub-millisecond response times.

### 2.7 DTO (Data Transfer Object) Pattern
Entities are never exposed directly to the API layer. DTOs are used for request/response objects.

**Benefit:** Security (hides internal structure), flexibility (API can differ from DB schema).

### 2.8 Saga Pattern (Compensation)
When a payment succeeds but the recharge status update fails:
1. Transaction is marked REFUND_PENDING.
2. If payment was via Wallet, money is automatically returned.
3. User is notified via email.

**Benefit:** Maintains data consistency across distributed services.

---

## 3. Security Architecture

### 3.1 JWT Token Lifecycle

```
[User Login] → [User Service validates credentials]
    → [JwtUtil generates token with username, role, email]
    → [Token sent to Angular Frontend]
    → [Frontend stores token in Cookie]
    → [AuthInterceptor adds "Bearer <token>" to every request]
    → [API Gateway validates token using JwtAuthenticationFilter]
    → [If valid: request forwarded with X-Auth-Username header]
    → [If invalid: 401 Unauthorized response]
```

### 3.2 Role-Based Access Control

| Endpoint Pattern | Required Role | Description |
|---|---|---|
| `/api/auth/**` | None (Public) | Login and registration |
| `/api/users/profile` | ROLE_USER or ROLE_ADMIN | User profile |
| `/api/recharge/**` | ROLE_USER or ROLE_ADMIN | Recharge operations |
| `/api/operators` (POST) | ROLE_ADMIN only | Create operator |
| `/api/users/all` | ROLE_ADMIN only | View all users |

### 3.3 Password Security
- Algorithm: BCrypt (one-way hash with salt).
- Even database administrators cannot see original passwords.
- Each hash includes a random salt, so identical passwords produce different hashes.

---

## 4. Data Flow Diagrams

### 4.1 Recharge & Payment Flow

```
Step 1: User selects operator and plan on Angular UI
    → Step 2: POST /api/recharge/initiate (via API Gateway)
    → Step 3: Recharge Service validates mobile number (regex)
    → Step 4: Recharge Service calls Operator Service (Feign) to verify plan
    → Step 5: Recharge saved in DB with status PENDING
    → Step 6: Recharge event published to RabbitMQ (payment.queue)
    → Step 7: Payment Service consumes event, creates pending Transaction
    → Step 8: User navigates to Payment page in Angular
    → Step 9: POST /api/transactions/pay (with payment details)
    → Step 10: DummyPaymentGateway validates payment data
    → Step 11: If WALLET: check balance via User Service (Feign)
    → Step 12: Transaction status updated to SUCCESS or FAILED
    → Step 13: Recharge status updated via Feign (with retry)
    → Step 14: Payment result published to RabbitMQ (notification.queue)
    → Step 15: Notification Service sends email via Gmail SMTP
```

### 4.2 Wallet Top-Up Flow

```
Step 1: User clicks "Add Money" on Payment page
    → Step 2: POST /api/transactions/wallet/topup
    → Step 3: Payment Gateway validates UPI details
    → Step 4: User Service wallet balance updated (Feign)
    → Step 5: Transaction recorded as SUCCESS
```

### 4.3 Circuit Breaker Flow

```
Payment SUCCESS
    → Attempt 1: Call Recharge Service → FAILED (service down)
    → Wait 30 seconds
    → Attempt 2: Call Recharge Service → FAILED
    → Wait 30 seconds
    → ... (up to 6 attempts)
    → All retries exhausted
    → Mark transaction as REFUND_PENDING
    → If WALLET payment: auto-refund to user's wallet
    → Send refund notification email
```

---

## 5. Infrastructure & Deployment

### 5.1 Docker Compose Services

| Service | Image | Port | Dependencies |
|---|---|---|---|
| PostgreSQL | postgres:latest | 5432 | — |
| Redis | redis:latest | 6379 | — |
| RabbitMQ | rabbitmq:management | 5672, 15672 | — |
| Zipkin | openzipkin/zipkin | 9411 | — |
| Eureka Server | Custom build | 8761 | — |
| Config Server | Custom build | 8888 | Eureka |
| User Service | Custom build | 8081 | PostgreSQL, Eureka, Config |
| Operator Service | Custom build | 8082 | PostgreSQL, Redis, Eureka, Config |
| Recharge Service | Custom build | 8083 | PostgreSQL, RabbitMQ, Eureka, Config |
| Payment Service | Custom build | 8084 | PostgreSQL, RabbitMQ, Eureka, Config |
| Notification Service | Custom build | 8085 | RabbitMQ, Eureka, Config |
| Angular Frontend | Custom build | 4200 | API Gateway |

### 5.2 Environment Variables
All sensitive data (database passwords, JWT secrets, SMTP credentials, RabbitMQ passwords) are stored in a `.env` file and injected via Docker Compose or `application.properties`. They are never hardcoded in source code.
