# OmniCharge — Mobile Recharge Platform

## Project Report

**Project Name:** OmniCharge  
**Version:** 1.0  
**Team:** Sprint Team  
**Date:** May 2026  

---

## 1. Introduction

### 1.1 Purpose
OmniCharge is a full-stack mobile recharge platform that enables users to browse mobile operators, select recharge plans, make secure payments, and receive confirmation notifications. The platform is built using a Microservices Architecture, ensuring scalability, fault tolerance, and independent deployment of each service.

### 1.2 Scope
The application covers:
- User registration and authentication with role-based access (User/Admin).
- Browsing mobile operators and their recharge plans.
- Initiating mobile recharges with real-time validation.
- Secure payment processing through multiple payment methods (Card, UPI, Net Banking, Wallet).
- Asynchronous notification delivery via email.
- Admin dashboard for managing users, operators, plans, recharges, and transactions.

### 1.3 Target Audience
- End Users: Individuals seeking to recharge mobile numbers.
- Administrators: Platform managers who oversee operators, plans, and system health.

---

## 2. Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Frontend | Angular | 21 |
| Backend Language | Java | 21 |
| Backend Framework | Spring Boot | 3.5.12 |
| Microservices Framework | Spring Cloud | 2025.0.1 |
| API Gateway | Spring Cloud Gateway | — |
| Service Discovery | Netflix Eureka | — |
| Central Configuration | Spring Cloud Config Server | — |
| Database | PostgreSQL | — |
| Caching | Redis | — |
| Message Queue | RabbitMQ | — |
| Security | Spring Security + JWT | — |
| Service-to-Service Communication | OpenFeign | — |
| API Documentation | SpringDoc Swagger | — |
| Distributed Tracing | Zipkin + Micrometer | — |
| Unit Testing | JUnit 5 + Mockito | — |
| Code Coverage | JaCoCo | — |
| Code Quality | SonarQube | — |
| Containerization | Docker + Docker Compose | — |

---

## 3. System Architecture

### 3.1 High-Level Architecture

The system follows a Microservices Architecture pattern where the application is decomposed into loosely coupled, independently deployable services.

```
Browser (Angular Frontend, Port 4200)
    |
    v
API Gateway (Port 8080) — JWT Validation, Routing, CORS
    |
    +——> User Service (Port 8081) ——> PostgreSQL (user_db)
    |
    +——> Operator Service (Port 8082) ——> PostgreSQL (operator_db) + Redis Cache
    |
    +——> Recharge Service (Port 8083) ——> PostgreSQL (recharge_db)
    |         |
    |         +——> Feign Call to Operator Service
    |         +——> RabbitMQ (payment.queue)
    |
    +——> Payment Service (Port 8084) ——> PostgreSQL (payment_db)
    |         |
    |         +——> Feign Call to Recharge Service
    |         +——> Feign Call to User Service (Wallet)
    |         +——> RabbitMQ (notification.queue)
    |
    +——> Notification Service (Port 8085) ——> Email (Gmail SMTP)
```

### 3.2 Infrastructure Services

| Service | Port | Purpose |
|---|---|---|
| Eureka Server | 8761 | Service Discovery — All services register here |
| Config Server | 8888 | Centralized configuration management |
| API Gateway | 8080 | Single entry point, JWT validation, request routing |

### 3.3 Communication Patterns

1. **Synchronous (OpenFeign):** Used when an immediate response is needed.
   - Recharge Service → Operator Service (to validate plans)
   - Payment Service → User Service (to check/update wallet)
   - Payment Service → Recharge Service (to update recharge status)

2. **Asynchronous (RabbitMQ):** Used for background/decoupled tasks.
   - Recharge Service → Payment Service (via `payment.queue`)
   - Payment Service → Notification Service (via `notification.queue`)

---

## 4. Microservices Description

### 4.1 User Service (Port 8081)
**Database:** user_db

**Responsibilities:**
- User and Admin registration with BCrypt password encryption.
- User and Admin login with JWT token generation.
- User profile management.
- Wallet balance management (read, update, top-up).
- Admin functions: view all users, promote user to admin.

**Key Design Patterns:**
- DTO Pattern for data transfer.
- Repository Pattern for database access.
- Stateless JWT-based authentication.

### 4.2 Operator Service (Port 8082)
**Database:** operator_db

**Responsibilities:**
- CRUD operations for mobile operators (Jio, Airtel, etc.).
- CRUD operations for recharge plans.
- Redis caching for frequently accessed data.

**Key Design Patterns:**
- Cache-Aside Pattern using Redis (`@Cacheable`, `@CacheEvict`).
- One-to-Many relationship between Operator and RechargePlan entities.

### 4.3 Recharge Service (Port 8083)
**Database:** recharge_db

**Responsibilities:**
- Validate mobile numbers and plan availability.
- Create recharge requests with PENDING status.
- Publish recharge events to RabbitMQ for asynchronous payment processing.
- Recharge cancellation.
- Recharge history for users and admin.

**Key Design Patterns:**
- Event-Driven Architecture using RabbitMQ.
- Feign Client for inter-service communication.

### 4.4 Payment Service (Port 8084)
**Database:** payment_db

**Responsibilities:**
- Consume recharge events from RabbitMQ and create pending transactions.
- Process payments using a simulated payment gateway.
- Support CARD, UPI, NETBANKING, and WALLET payment methods.
- Wallet top-up functionality.
- Retry logic with circuit breaker for recharge status updates.
- Automatic refund for failed recharges (wallet payments).
- Publish payment results to the notification queue.

**Key Design Patterns:**
- Circuit Breaker Pattern (retry with fallback).
- Compensation/Saga Pattern (automatic refund on failure).
- Consumer/Producer Pattern for RabbitMQ.

### 4.5 Notification Service (Port 8085)

**Responsibilities:**
- Consume payment result messages from RabbitMQ.
- Build notification content (success/failure/refund).
- Send formatted email notifications using Gmail SMTP.

**Key Design Patterns:**
- Event Consumer Pattern.
- Fire-and-Forget messaging.

---

## 5. Security Architecture

### 5.1 Authentication Flow
1. User submits credentials (username + password).
2. User Service validates credentials using BCrypt.
3. On success, a JWT token is generated containing username, role, and email.
4. Token is sent to the Angular frontend and stored in a browser cookie.
5. Every subsequent request includes the token in the Authorization header.

### 5.2 Authorization Flow
1. API Gateway intercepts every request and validates the JWT token.
2. If valid, it extracts the username and role and forwards them as headers.
3. Each microservice has its own SecurityConfig that enforces role-based access:
   - Public endpoints: `/api/auth/**`
   - Authenticated endpoints: `/api/recharge/**`, `/api/users/profile`
   - Admin-only endpoints: `/api/users/all`, `/api/operators` (POST/PUT/DELETE)

### 5.3 Security Measures
- BCrypt password hashing (one-way encryption).
- JWT with HMAC-SHA256 signature.
- Stateless session management (no server-side session storage).
- CORS configuration to allow only trusted frontend origins.
- CSRF disabled (not needed with JWT-based authentication).

---

## 6. Database Design

### 6.1 Database Per Service Pattern
Each microservice owns its database, ensuring data isolation and loose coupling.

| Service | Database Name | Key Tables |
|---|---|---|
| User Service | user_db | users |
| Operator Service | operator_db | operators, recharge_plans |
| Recharge Service | recharge_db | recharge_requests |
| Payment Service | payment_db | transactions |

### 6.2 Entity Relationship

**Users Table:**
| Column | Type | Description |
|---|---|---|
| id | BIGINT (PK) | Auto-generated primary key |
| username | VARCHAR (Unique) | Login username |
| email | VARCHAR (Unique) | User email |
| password | VARCHAR | BCrypt encrypted password |
| full_name | VARCHAR | Display name |
| phone | VARCHAR | Phone number |
| role | ENUM | ROLE_USER or ROLE_ADMIN |
| active | BOOLEAN | Account status |
| wallet_balance | DECIMAL | OmniCharge wallet balance |

**Operators Table:**
| Column | Type | Description |
|---|---|---|
| id | BIGINT (PK) | Auto-generated primary key |
| name | VARCHAR | Operator name (e.g., Jio) |
| type | VARCHAR | MOBILE, DTH, BROADBAND |
| status | VARCHAR | ACTIVE or INACTIVE |
| logo_url | VARCHAR | Logo image URL |
| description | TEXT | Operator description |

**Recharge Plans Table:**
| Column | Type | Description |
|---|---|---|
| id | BIGINT (PK) | Auto-generated primary key |
| plan_name | VARCHAR | Plan title |
| price | DECIMAL | Recharge amount |
| validity | VARCHAR | Plan validity (e.g., 28 Days) |
| data | VARCHAR | Data benefits |
| calls | VARCHAR | Calling benefits |
| sms | VARCHAR | SMS benefits |
| category | VARCHAR | Plan category |
| status | VARCHAR | ACTIVE or INACTIVE |
| operator_id | BIGINT (FK) | Foreign key to Operators |

**Recharge Requests Table:**
| Column | Type | Description |
|---|---|---|
| id | BIGINT (PK) | Auto-generated primary key |
| username | VARCHAR | Requesting user |
| mobile_number | VARCHAR | Number to recharge |
| operator_id | BIGINT | Selected operator |
| operator_name | VARCHAR | Operator name |
| plan_id | BIGINT | Selected plan |
| plan_name | VARCHAR | Plan name |
| amount | DECIMAL | Recharge amount |
| status | ENUM | PENDING, SUCCESS, FAILED, CANCELLED |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Last update time |
| failure_reason | TEXT | Failure or cancellation reason |

**Transactions Table:**
| Column | Type | Description |
|---|---|---|
| id | BIGINT (PK) | Auto-generated primary key |
| transaction_id | VARCHAR (Unique) | Public transaction ID |
| recharge_id | BIGINT | Related recharge ID |
| username | VARCHAR | Paying user |
| user_email | VARCHAR | User email for notifications |
| mobile_number | VARCHAR | Recharged number |
| amount | DECIMAL | Payment amount |
| status | ENUM | PENDING, SUCCESS, FAILED, REFUND_PENDING, CANCELLED |
| payment_method | ENUM | CARD, UPI, NETBANKING, WALLET |
| payment_reference | VARCHAR | Gateway reference |
| failure_reason | TEXT | Failure or refund reason |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Last update time |

---

## 7. API Documentation

### 7.1 Authentication APIs (User Service)

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | /api/auth/register | Register new user | Public |
| POST | /api/auth/register-admin | Register admin (requires secret) | Public |
| POST | /api/auth/user/login | User login | Public |
| POST | /api/auth/admin/login | Admin login | Public |

### 7.2 User Management APIs (User Service)

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | /api/users/profile | Get user profile | Authenticated |
| PUT | /api/users/change-password | Change password | Authenticated |
| GET | /api/users/profile/wallet | Get wallet balance | Authenticated |
| POST | /api/users/profile/wallet/update | Update wallet balance | Internal (Feign) |
| GET | /api/users/all | List all users | Admin |
| PUT | /api/users/promote/{userId} | Promote user to admin | Admin |

### 7.3 Operator APIs (Operator Service)

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | /api/operators | List all operators | Authenticated |
| GET | /api/operators/{id} | Get operator by ID | Authenticated |
| GET | /api/operators/status/{status} | Filter by status | Authenticated |
| GET | /api/operators/type/{type} | Filter by type | Authenticated |
| POST | /api/operators | Create operator | Admin |
| PUT | /api/operators/{id} | Update operator | Admin |
| PATCH | /api/operators/{id} | Partial update | Admin |
| DELETE | /api/operators/{id} | Delete operator | Admin |

### 7.4 Recharge Plan APIs (Operator Service)

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | /api/plans | List all plans | Authenticated |
| GET | /api/plans/{id} | Get plan by ID | Authenticated |
| GET | /api/plans/operator/{operatorId} | Plans by operator | Authenticated |
| GET | /api/plans/operator/{operatorId}/active | Active plans by operator | Authenticated |
| GET | /api/plans/category/{category} | Plans by category | Authenticated |
| POST | /api/plans | Create plan | Admin |
| PUT | /api/plans/{id} | Update plan | Admin |
| PATCH | /api/plans/{id} | Partial update | Admin |
| DELETE | /api/plans/{id} | Delete plan | Admin |

### 7.5 Recharge APIs (Recharge Service)

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | /api/recharge/initiate | Initiate recharge | Authenticated |
| GET | /api/recharge/my-history | User's recharge history | Authenticated |
| GET | /api/recharge/{id} | Get recharge by ID | Authenticated |
| PUT | /api/recharge/{id}/cancel | Cancel pending recharge | Authenticated |
| GET | /api/recharge/all | All recharges | Admin |
| GET | /api/recharge/status/{status} | Filter by status | Admin |
| GET | /api/recharge/mobile/{mobile} | Filter by mobile | Admin |

### 7.6 Transaction APIs (Payment Service)

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | /api/transactions/pay | Make payment | Authenticated |
| POST | /api/transactions/wallet/topup | Top up wallet | Authenticated |
| GET | /api/transactions/my-transactions | User's transactions | Authenticated |
| GET | /api/transactions/txn/{transactionId} | Get by transaction ID | Authenticated |
| GET | /api/transactions/recharge/{rechargeId} | Get by recharge ID | Authenticated |
| GET | /api/transactions/all | All transactions | Admin |
| GET | /api/transactions/status/{status} | Filter by status | Admin |
| GET | /api/transactions/mobile/{mobile} | Filter by mobile | Admin |

---

## 8. Key Features & Design Patterns

### 8.1 Caching (Redis)
- Implemented in the Operator Service using Spring Cache annotations.
- `@Cacheable`: Caches read operations (plans, operators).
- `@CacheEvict`: Invalidates cache on write operations (add, update, delete).
- Reduces database load and improves response times.

### 8.2 Circuit Breaker & Retry
- Implemented in the Payment Service for recharge status updates.
- Retries up to 6 times with 30-second intervals.
- On exhaustion, marks transaction as REFUND_PENDING.
- Automatic wallet refund for wallet-based payments.

### 8.3 Asynchronous Messaging (RabbitMQ)
- Decouples the Recharge and Payment services.
- Ensures message durability (messages survive server restarts).
- Provides reliable delivery even during temporary service outages.

### 8.4 Distributed Tracing (Zipkin)
- Every request is assigned a unique Trace ID.
- Allows tracking a request across all microservices.
- Helps identify performance bottlenecks.

### 8.5 Health Monitoring (Actuator)
- Every service exposes `/actuator/health` for liveness checks.
- Used by Docker and orchestration tools for automatic restart.

---

## 9. Testing Strategy

### 9.1 Unit Testing
- Framework: JUnit 5 + Mockito.
- Pattern: Given-When-Then (Arrange-Act-Assert).
- Mocking: All external dependencies (repositories, Feign clients) are mocked.

### 9.2 Code Coverage
- Tool: JaCoCo (Java Code Coverage).
- Target: 80% code coverage across all services.
- Dashboard: SonarQube for real-time quality monitoring.

### 9.3 API Testing
- Tool: Swagger UI available at `/swagger-ui/index.html` for each service.
- Gateway aggregation provides a unified view of all service APIs.

---

## 10. Deployment

### 10.1 Docker Compose
The entire platform is containerized using Docker Compose. A single `docker-compose up` command starts:
- PostgreSQL (4 databases: user_db, operator_db, recharge_db, payment_db)
- Redis (caching)
- RabbitMQ (messaging)
- Zipkin (tracing)
- All 8 Spring Boot services
- Angular frontend

### 10.2 Port Allocation

| Component | Port |
|---|---|
| Angular Frontend | 4200 |
| API Gateway | 8080 |
| User Service | 8081 |
| Operator Service | 8082 |
| Recharge Service | 8083 |
| Payment Service | 8084 |
| Notification Service | 8085 |
| Eureka Server | 8761 |
| Config Server | 8888 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| RabbitMQ | 5672 / 15672 |
| Zipkin | 9411 |
| SonarQube | 9000 |

---

## 11. Conclusion

OmniCharge demonstrates a production-grade Microservices Architecture with robust security (JWT), fault tolerance (Circuit Breaker, Retry), performance optimization (Redis Cache), asynchronous processing (RabbitMQ), and comprehensive observability (Zipkin, Actuator, SonarQube). The platform follows industry best practices including the Database-per-Service pattern, Event-Driven Architecture, and the DTO design pattern.
