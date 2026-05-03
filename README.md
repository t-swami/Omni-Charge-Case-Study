# OmniCharge Zero To Hero Guide

OmniCharge is a full mobile recharge application built with Angular, Spring Boot microservices, PostgreSQL, Redis, RabbitMQ, Eureka, Config Server, Docker, Zipkin, Swagger, JaCoCo, and SonarQube.

This README explains the project from zero. It tells you:

- What the project does.
- What each technical word means.
- How the complete backend architecture works.
- Which file implements each feature.
- How frontend pages connect to backend APIs.
- How data moves from browser to database and queues.
- How to run, test, debug, and understand the project.

If someone does not know the basics, they should still be able to follow this document slowly and understand the project.

## 1. Project In One Line

OmniCharge lets users register, log in, select a mobile operator, select a recharge plan, pay for it, track recharge status, view transaction history, and receive notifications.

Admins can manage users, operators, plans, recharges, and transactions.

## 2. First Learn These Basic Words

| Word | Meaning in simple language | Example in this project |
|---|---|---|
| Frontend | The part the user sees in the browser | `omnicharge-frontend` Angular app |
| Backend | Server-side code that stores data and runs business logic | Spring Boot services |
| API | A URL used to ask backend to do something | `POST /api/auth/user/login` |
| Database | Place where permanent data is stored | PostgreSQL |
| Table | A group of related records in database | `users`, `operators`, `recharge_request`, `transactions` |
| Microservice | Small backend app with one main responsibility | User Service, Payment Service |
| Controller | Java class that receives API requests | `AuthController.java` |
| Service | Java class that contains main business logic | `UserServiceImpl.java` |
| Repository | Java interface that talks to database | `UserRepository.java` |
| Entity | Java class mapped to a database table | `User.java` |
| DTO | Simple object used for request or response data | `UserDto.java` |
| JWT | Login token sent with protected requests | Created by `JwtUtil.java` |
| API Gateway | Main backend entry point | `api-gateway` on port `8080` |
| Eureka | Service registry where services register themselves | `eureka-server` on port `8761` |
| Config Server | Central configuration service | `config-server` on port `8888` |
| RabbitMQ | Message queue for async processing | `payment.queue`, `notification.queue` |
| Redis | Fast cache storage | Used by Operator Service |
| Feign Client | Java interface used by one service to call another service | `OperatorFeignClient.java` |
| Docker | Runs apps in containers | `docker-compose.yml` |
| Swagger | Browser page to test APIs | `/swagger-ui.html` |
| JaCoCo | Test coverage tool | Maven plugin in service `pom.xml` |
| SonarQube | Code quality dashboard | `http://localhost:9000` |

## 3. Main User Flow

This is the normal user journey:

```text
Open website
  -> Register user
  -> Login user
  -> Browse operators
  -> Browse plans
  -> Start recharge
  -> Pay
  -> Recharge status becomes SUCCESS or FAILED
  -> Notification is generated
  -> User checks history
```

Admin journey:

```text
Register admin with admin secret
  -> Login admin
  -> View all users
  -> Promote user to admin
  -> Add/update/delete operators
  -> Add/update/delete plans
  -> View/cancel/filter recharges
  -> View/filter transactions
```

## 4. Complete Project Structure

```text
sprint/
  api-gateway/              Main backend entry point. Routes requests to services.
  config-server/            Central configuration server.
  eureka-server/            Service discovery server.
  user-service/             Users, admins, login, JWT, profile, wallet.
  operator-service/         Mobile operators and recharge plans.
  recharge-service/         Recharge creation, cancellation, history, status.
  payment-service/          Payment, wallet top-up, transactions, refund handling.
  notification-service/     RabbitMQ notification consumer and email sending.
  omnicharge-frontend/      Angular browser application.
  docs/                     Extra documentation.
  docker-compose.yml        Starts infrastructure and backend containers.
  init-db.sql               Creates PostgreSQL databases.
  sonar-scan.ps1            Runs SonarQube scan.
  .env.example              Safe example environment file.
  README.md                 This guide.
```

## 5. Technology Stack

| Area | Technology |
|---|---|
| Frontend | Angular 21 |
| Backend language | Java 21 |
| Backend framework | Spring Boot 3.5.12 |
| Microservices | Spring Cloud 2025.0.1 |
| API Gateway | Spring Cloud Gateway |
| Service discovery | Netflix Eureka |
| Central config | Spring Cloud Config Server |
| Database | PostgreSQL |
| Cache | Redis |
| Queue | RabbitMQ |
| Security | Spring Security, JWT |
| Service-to-service REST | OpenFeign |
| API docs | SpringDoc Swagger |
| Tracing | Zipkin, Micrometer |
| Testing | JUnit, Mockito, Spring Boot Test |
| Coverage | JaCoCo |
| Code quality | SonarQube |
| Containers | Docker, Docker Compose |

## 6. Ports

| Component | Port | URL |
|---|---:|---|
| Frontend | 4200 | `http://localhost:4200` |
| API Gateway | 8080 | `http://localhost:8080` |
| User Service | 8081 | `http://localhost:8081` |
| Operator Service | 8082 | `http://localhost:8082` |
| Recharge Service | 8083 | `http://localhost:8083` |
| Payment Service | 8084 | `http://localhost:8084` |
| Notification Service | 8085 | `http://localhost:8085` |
| Eureka Server | 8761 | `http://localhost:8761` |
| Config Server | 8888 | `http://localhost:8888` |
| PostgreSQL | 5432 | Database port |
| RabbitMQ | 5672 | Queue port |
| RabbitMQ dashboard | 15672 | `http://localhost:15672` |
| Redis | 6379 | Cache port |
| Zipkin | 9411 | `http://localhost:9411` |
| SonarQube | 9000 | `http://localhost:9000` |

## 7. Full Backend Architecture

The backend is not one large application. It is split into services.

```text
Browser
  |
  v
Angular Frontend
  |
  | /api/... requests
  v
API Gateway
  |
  +--> User Service --------> PostgreSQL user_db
  |
  +--> Operator Service ----> PostgreSQL operator_db
  |                           Redis cache
  |
  +--> Recharge Service ----> PostgreSQL recharge_db
  |          |
  |          +--> Feign call to Operator Service
  |          |
  |          +--> RabbitMQ payment.queue
  |
  +--> Payment Service -----> PostgreSQL payment_db
  |          |
  |          +--> Feign call to Recharge Service
  |          +--> Feign call to User Service for wallet
  |          +--> RabbitMQ notification.queue
  |
  +--> Notification Service -> consumes notification.queue
```

### 7.1 Backend Request Pattern

Most backend features follow this path:

```text
Controller -> Service -> Repository -> Entity -> Database
```

Example from User Service:

```text
AuthController.java
  -> UserService.java
  -> UserServiceImpl.java
  -> UserRepository.java
  -> User.java
  -> user_db
```

Meaning:

- Controller receives the API request.
- Service checks rules and runs logic.
- Repository reads or writes database data.
- Entity represents the database table.
- DTO classes carry request and response data.

### 7.2 API Gateway

Folder:

```text
api-gateway/
```

Purpose:

- It is the single backend entry point.
- Frontend sends `/api/...` requests to gateway.
- Gateway routes each request to the correct service.
- Gateway validates JWT token for protected routes.
- Gateway adds `X-Auth-Username` and `X-Auth-Role` headers after JWT validation.

Important files:

| File | Purpose |
|---|---|
| `api-gateway/src/main/resources/application.properties` | Gateway routes, Swagger aggregation, Eureka URL, Zipkin |
| `api-gateway/src/main/java/com/omnicharge/api_gateway/filter/JwtAuthenticationFilter.java` | Global JWT check before request reaches services |
| `api-gateway/src/main/java/com/omnicharge/api_gateway/filter/JwtUtil.java` | Reads username and role from token |
| `api-gateway/src/main/java/com/omnicharge/api_gateway/config/CorsConfig.java` | Allows frontend browser calls during development |
| `api-gateway/src/main/java/com/omnicharge/api_gateway/ApiGatewayApplication.java` | Main class |

Gateway route examples:

| API path | Sent to |
|---|---|
| `/api/auth/**` | User Service |
| `/api/users/**` | User Service |
| `/api/operators/**` | Operator Service |
| `/api/plans/**` | Operator Service |
| `/api/recharge/**` | Recharge Service |
| `/api/transactions/**` | Payment Service |

### 7.3 Eureka Server

Folder:

```text
eureka-server/
```

Purpose:

- Services register here when they start.
- Gateway can call services by name, such as `lb://user-service`.
- Dashboard shows which services are alive.

Important files:

| File | Purpose |
|---|---|
| `eureka-server/src/main/java/com/omnicharge/eureka_server/EurekaServerApplication.java` | Starts Eureka server |
| `eureka-server/src/main/resources/application.properties` | Port `8761` and Eureka settings |

### 7.4 Config Server

Folder:

```text
config-server/
```

Purpose:

- Central place for configuration.
- Services import config using `spring.config.import`.

Important files:

| File | Purpose |
|---|---|
| `config-server/src/main/java/com/omnicharge/config_server/ConfigServerApplication.java` | Starts Config Server |
| `config-server/src/main/resources/application.properties` | Port `8888`, Eureka, config repository details |

### 7.5 User Service

Folder:

```text
user-service/
```

Database:

```text
user_db
```

Main responsibility:

- Normal user registration.
- Admin registration using secret key.
- User login.
- Admin login.
- JWT token generation.
- User profile.
- View all users.
- Promote user to admin.
- Change password.
- Wallet balance read/update.

Important files:

| Layer | File | Purpose |
|---|---|---|
| App start | `user-service/src/main/java/com/omnicharge/user_service/UserServiceApplication.java` | Starts service |
| Controller | `user-service/src/main/java/com/omnicharge/user_service/controller/AuthController.java` | Register and login APIs |
| Controller | `user-service/src/main/java/com/omnicharge/user_service/controller/UserController.java` | Profile, users, promote, password, wallet APIs |
| Service interface | `user-service/src/main/java/com/omnicharge/user_service/service/UserService.java` | Declares user methods |
| Service logic | `user-service/src/main/java/com/omnicharge/user_service/service/UserServiceImpl.java` | Main user business logic |
| Repository | `user-service/src/main/java/com/omnicharge/user_service/repository/UserRepository.java` | Database access |
| Entity | `user-service/src/main/java/com/omnicharge/user_service/entity/User.java` | User database model |
| Enum | `user-service/src/main/java/com/omnicharge/user_service/entity/Role.java` | `ROLE_USER`, `ROLE_ADMIN` |
| Security | `user-service/src/main/java/com/omnicharge/user_service/config/SecurityConfig.java` | Role rules and stateless security |
| Filter | `user-service/src/main/java/com/omnicharge/user_service/filter/JwtAuthFilter.java` | Reads JWT and sets Spring authentication |
| JWT | `user-service/src/main/java/com/omnicharge/user_service/security/JwtUtil.java` | Creates and validates tokens |
| User lookup | `user-service/src/main/java/com/omnicharge/user_service/security/CustomUserDetailsService.java` | Loads user for Spring Security |
| DTO | `user-service/src/main/java/com/omnicharge/user_service/dto/RegisterRequest.java` | User register request |
| DTO | `user-service/src/main/java/com/omnicharge/user_service/dto/AdminRegisterRequest.java` | Admin register request |
| DTO | `user-service/src/main/java/com/omnicharge/user_service/dto/LoginRequest.java` | Login request |
| DTO | `user-service/src/main/java/com/omnicharge/user_service/dto/AuthResponse.java` | Login/register response |
| DTO | `user-service/src/main/java/com/omnicharge/user_service/dto/UserDto.java` | User response |
| DTO | `user-service/src/main/java/com/omnicharge/user_service/dto/ChangePasswordRequest.java` | Change password request |
| Error handling | `user-service/src/main/java/com/omnicharge/user_service/exception/GlobalExceptionHandler.java` | Converts exceptions to API responses |
| Config | `user-service/src/main/resources/application.properties` | Port, database, Eureka, JWT, admin secret |

User entity fields:

| Field | Meaning |
|---|---|
| `id` | Database primary key |
| `username` | Unique login name |
| `email` | Unique email |
| `password` | BCrypt encrypted password |
| `fullName` | User full name |
| `phone` | User phone |
| `role` | `ROLE_USER` or `ROLE_ADMIN` |
| `active` | Whether account is active |
| `walletBalance` | OmniCharge wallet balance |

### 7.6 Operator Service

Folder:

```text
operator-service/
```

Database:

```text
operator_db
```

Main responsibility:

- Create, update, patch, delete operators.
- Read operators by id, status, and type.
- Create, update, patch, delete recharge plans.
- Read plans by id, operator, active operator, and category.
- Cache frequently-read operators and plans in Redis.

Important files:

| Layer | File | Purpose |
|---|---|---|
| App start | `operator-service/src/main/java/com/omnicharge/operator_service/OperatorServiceApplication.java` | Starts service |
| Controller | `operator-service/src/main/java/com/omnicharge/operator_service/controller/OperatorController.java` | Operator APIs |
| Controller | `operator-service/src/main/java/com/omnicharge/operator_service/controller/RechargePlanController.java` | Plan APIs |
| Service interface | `operator-service/src/main/java/com/omnicharge/operator_service/service/OperatorService.java` | Operator method contract |
| Service logic | `operator-service/src/main/java/com/omnicharge/operator_service/service/OperatorServiceImpl.java` | Operator CRUD and cache eviction |
| Service interface | `operator-service/src/main/java/com/omnicharge/operator_service/service/RechargePlanService.java` | Plan method contract |
| Service logic | `operator-service/src/main/java/com/omnicharge/operator_service/service/RechargePlanServiceImpl.java` | Plan CRUD, filters, cache |
| Repository | `operator-service/src/main/java/com/omnicharge/operator_service/repository/OperatorRepository.java` | Operator DB access |
| Repository | `operator-service/src/main/java/com/omnicharge/operator_service/repository/RechargePlanRepository.java` | Plan DB access |
| Entity | `operator-service/src/main/java/com/omnicharge/operator_service/entity/Operator.java` | Operator table model |
| Entity | `operator-service/src/main/java/com/omnicharge/operator_service/entity/RechargePlan.java` | Plan table model |
| Cache config | `operator-service/src/main/java/com/omnicharge/operator_service/config/CacheConfig.java` | Redis cache configuration |
| Security | `operator-service/src/main/java/com/omnicharge/operator_service/config/SecurityConfig.java` | GET for authenticated users, write APIs admin only |
| Filter | `operator-service/src/main/java/com/omnicharge/operator_service/filter/JwtAuthFilter.java` | JWT to Spring authentication |
| JWT | `operator-service/src/main/java/com/omnicharge/operator_service/security/JwtUtil.java` | Token validation |
| DTO | `operator-service/src/main/java/com/omnicharge/operator_service/dto/OperatorRequest.java` | Operator create/update request |
| DTO | `operator-service/src/main/java/com/omnicharge/operator_service/dto/OperatorDto.java` | Operator response |
| DTO | `operator-service/src/main/java/com/omnicharge/operator_service/dto/RechargePlanRequest.java` | Plan create/update request |
| DTO | `operator-service/src/main/java/com/omnicharge/operator_service/dto/RechargePlanDto.java` | Plan response |
| Config | `operator-service/src/main/resources/application.properties` | Port, DB, Redis, Eureka |

Operator entity fields:

| Field | Meaning |
|---|---|
| `id` | Operator id |
| `name` | Operator name |
| `type` | Example: `MOBILE`, `DTH`, `BROADBAND` |
| `status` | `ACTIVE` or `INACTIVE` |
| `logoUrl` | Logo image URL |
| `description` | Description |
| `rechargePlans` | Plans linked to this operator |

Recharge plan entity fields:

| Field | Meaning |
|---|---|
| `id` | Plan id |
| `planName` | Plan title |
| `price` | Recharge amount |
| `validity` | Validity such as `28 Days` |
| `data` | Data benefits |
| `calls` | Calling benefits |
| `sms` | SMS benefits |
| `description` | Description |
| `category` | Plan category |
| `status` | `ACTIVE` or `INACTIVE` |
| `operator` | Parent operator |

### 7.7 Recharge Service

Folder:

```text
recharge-service/
```

Database:

```text
recharge_db
```

Main responsibility:

- Validate mobile number.
- Validate operator and plan by calling Operator Service.
- Create recharge record with `PENDING` status.
- Publish recharge event to RabbitMQ `payment.queue`.
- Cancel pending recharge.
- Show user's own recharge history.
- Admin can view all recharges and filter by status or mobile.
- Payment Service can update recharge status.

Important files:

| Layer | File | Purpose |
|---|---|---|
| App start | `recharge-service/src/main/java/com/omnicharge/recharge_service/RechargeServiceApplication.java` | Starts service |
| Controller | `recharge-service/src/main/java/com/omnicharge/recharge_service/controller/RechargeController.java` | Recharge APIs |
| Service interface | `recharge-service/src/main/java/com/omnicharge/recharge_service/service/RechargeService.java` | Recharge method contract |
| Service logic | `recharge-service/src/main/java/com/omnicharge/recharge_service/service/RechargeServiceImpl.java` | Main recharge business logic |
| Repository | `recharge-service/src/main/java/com/omnicharge/recharge_service/repository/RechargeRepository.java` | Recharge DB access |
| Entity | `recharge-service/src/main/java/com/omnicharge/recharge_service/entity/RechargeRequest.java` | Recharge table model |
| Enum | `recharge-service/src/main/java/com/omnicharge/recharge_service/entity/RechargeStatus.java` | `PENDING`, `SUCCESS`, `FAILED`, `CANCELLED` |
| Feign | `recharge-service/src/main/java/com/omnicharge/recharge_service/feign/OperatorFeignClient.java` | Calls Operator Service |
| RabbitMQ config | `recharge-service/src/main/java/com/omnicharge/recharge_service/config/RabbitMQConfig.java` | Payment queue setup |
| Publisher | `recharge-service/src/main/java/com/omnicharge/recharge_service/messaging/RechargeEventPublisher.java` | Sends event to payment queue |
| Security | `recharge-service/src/main/java/com/omnicharge/recharge_service/config/SecurityConfig.java` | Admin-only and authenticated routes |
| Filter | `recharge-service/src/main/java/com/omnicharge/recharge_service/filter/JwtAuthFilter.java` | JWT authentication |
| DTO | `recharge-service/src/main/java/com/omnicharge/recharge_service/dto/InitiateRechargeRequest.java` | Start recharge request |
| DTO | `recharge-service/src/main/java/com/omnicharge/recharge_service/dto/RechargeRequestDto.java` | Recharge response |
| DTO | `recharge-service/src/main/java/com/omnicharge/recharge_service/dto/RechargeEventMessage.java` | Message sent to payment queue |
| DTO | `recharge-service/src/main/java/com/omnicharge/recharge_service/dto/RechargeStatusUpdateRequest.java` | Status update from Payment Service |
| DTO | `recharge-service/src/main/java/com/omnicharge/recharge_service/dto/OperatorResponse.java` | Operator response from Feign |
| DTO | `recharge-service/src/main/java/com/omnicharge/recharge_service/dto/PlanResponse.java` | Plan response from Feign |
| Config | `recharge-service/src/main/resources/application.properties` | Port, DB, RabbitMQ, Eureka |

Recharge entity fields:

| Field | Meaning |
|---|---|
| `id` | Recharge id |
| `username` | User who requested recharge |
| `mobileNumber` | Mobile number to recharge |
| `operatorId` | Selected operator id |
| `operatorName` | Selected operator name |
| `planId` | Selected plan id |
| `planName` | Selected plan name |
| `amount` | Recharge amount |
| `validity` | Plan validity |
| `dataInfo` | Plan data info |
| `status` | Recharge status |
| `createdAt` | Created timestamp |
| `updatedAt` | Last updated timestamp |
| `failureReason` | Failure or cancellation reason |

### 7.8 Payment Service

Folder:

```text
payment-service/
```

Database:

```text
payment_db
```

Main responsibility:

- Create pending transaction when recharge event arrives.
- Process manual payment using payment method.
- Support `CARD`, `UPI`, `NETBANKING`, and `WALLET`.
- Check and deduct wallet balance by calling User Service.
- Top up wallet.
- Update recharge status by calling Recharge Service.
- Publish notification message to RabbitMQ.
- Retry recharge status update if Recharge Service is unavailable.
- Mark payment as `REFUND_PENDING` if payment succeeded but recharge status update fails after retries.
- Refund wallet instantly for wallet payments during retry failure scenario.
- Provide user and admin transaction history APIs.

Important files:

| Layer | File | Purpose |
|---|---|---|
| App start | `payment-service/src/main/java/com/omnicharge/payment_service/PaymentServiceApplication.java` | Starts service |
| Controller | `payment-service/src/main/java/com/omnicharge/payment_service/controller/TransactionController.java` | Payment and transaction APIs |
| Service interface | `payment-service/src/main/java/com/omnicharge/payment_service/service/PaymentService.java` | Payment method contract |
| Service logic | `payment-service/src/main/java/com/omnicharge/payment_service/service/PaymentServiceImpl.java` | Payment, wallet, retry, refund, notification logic |
| Gateway simulation | `payment-service/src/main/java/com/omnicharge/payment_service/service/DummyPaymentGatewayService.java` | Simulates external payment gateway |
| Repository | `payment-service/src/main/java/com/omnicharge/payment_service/repository/TransactionRepository.java` | Transaction DB access |
| Entity | `payment-service/src/main/java/com/omnicharge/payment_service/entity/Transaction.java` | Transaction table model |
| Enum | `payment-service/src/main/java/com/omnicharge/payment_service/entity/TransactionStatus.java` | `PENDING`, `SUCCESS`, `FAILED`, `REFUND_PENDING`, `CANCELLED` |
| Enum | `payment-service/src/main/java/com/omnicharge/payment_service/entity/PaymentMethod.java` | `CARD`, `UPI`, `NETBANKING`, `WALLET` |
| Consumer | `payment-service/src/main/java/com/omnicharge/payment_service/messaging/RechargeEventConsumer.java` | Reads `payment.queue` |
| Publisher | `payment-service/src/main/java/com/omnicharge/payment_service/messaging/PaymentResultPublisher.java` | Sends message to `notification.queue` |
| RabbitMQ config | `payment-service/src/main/java/com/omnicharge/payment_service/config/RabbitMQConfig.java` | Payment and notification queue config |
| Feign | `payment-service/src/main/java/com/omnicharge/payment_service/feign/RechargeServiceFeignClient.java` | Calls Recharge Service status update |
| Feign | `payment-service/src/main/java/com/omnicharge/payment_service/feign/UserServiceFeignClient.java` | Calls User Service wallet APIs |
| Security | `payment-service/src/main/java/com/omnicharge/payment_service/config/SecurityConfig.java` | Transaction access rules |
| Filter | `payment-service/src/main/java/com/omnicharge/payment_service/filter/JwtAuthFilter.java` | JWT authentication |
| DTO | `payment-service/src/main/java/com/omnicharge/payment_service/dto/PaymentGatewayRequest.java` | Payment request |
| DTO | `payment-service/src/main/java/com/omnicharge/payment_service/dto/PaymentGatewayResponse.java` | Gateway result |
| DTO | `payment-service/src/main/java/com/omnicharge/payment_service/dto/TransactionDto.java` | Transaction response |
| DTO | `payment-service/src/main/java/com/omnicharge/payment_service/dto/RechargeEventMessage.java` | Message from recharge queue |
| DTO | `payment-service/src/main/java/com/omnicharge/payment_service/dto/PaymentResultMessage.java` | Message sent to notification queue |
| DTO | `payment-service/src/main/java/com/omnicharge/payment_service/dto/RechargeStatusUpdateRequest.java` | Request sent to Recharge Service |
| DB migration | `payment-service/src/main/java/com/omnicharge/payment_service/config/DatabaseMigrationConfig.java` | Ensures enum values exist |
| Config | `payment-service/src/main/resources/application.properties` | Port, DB, RabbitMQ, retry, refund settings |

Transaction entity fields:

| Field | Meaning |
|---|---|
| `id` | Database id |
| `transactionId` | Public transaction id |
| `rechargeId` | Related recharge id |
| `username` | Paying user |
| `userEmail` | User email |
| `mobileNumber` | Recharged number |
| `operatorName` | Operator name |
| `planName` | Plan name |
| `amount` | Payment amount |
| `validity` | Plan validity |
| `dataInfo` | Plan data |
| `status` | Transaction status |
| `paymentMethod` | Payment method |
| `paymentReference` | Gateway reference |
| `failureReason` | Failure or refund reason |
| `createdAt` | Created timestamp |
| `updatedAt` | Last updated timestamp |

### 7.9 Notification Service

Folder:

```text
notification-service/
```

Main responsibility:

- Read payment result messages from RabbitMQ `notification.queue`.
- Build notification content.
- Send email using configured mail credentials.

Important files:

| Layer | File | Purpose |
|---|---|---|
| App start | `notification-service/src/main/java/com/omnicharge/notification_service/NotificationServiceApplication.java` | Starts service |
| Consumer | `notification-service/src/main/java/com/omnicharge/notification_service/messaging/NotificationConsumer.java` | Reads notification queue |
| Email | `notification-service/src/main/java/com/omnicharge/notification_service/service/EmailService.java` | Sends email |
| RabbitMQ config | `notification-service/src/main/java/com/omnicharge/notification_service/config/RabbitMQConfig.java` | Queue and JSON converter |
| Security | `notification-service/src/main/java/com/omnicharge/notification_service/config/SecurityConfig.java` | JWT rules |
| Filter | `notification-service/src/main/java/com/omnicharge/notification_service/filter/JwtAuthFilter.java` | JWT authentication |
| DTO | `notification-service/src/main/java/com/omnicharge/notification_service/dto/PaymentResultMessage.java` | Payment result message |
| DTO | `notification-service/src/main/java/com/omnicharge/notification_service/dto/RechargeEventMessage.java` | Recharge event message |
| Config | `notification-service/src/main/resources/application.properties` | Port, RabbitMQ, mail |

## 8. Complete Feature Location Map

This is the most important section if you want to find where a feature is implemented.

| Feature | Frontend files | Backend API | Backend implementation files |
|---|---|---|---|
| Home page | `omnicharge-frontend/src/app/pages/home/*` | No main backend API | Clears session through `AuthService.clearAndLogout()` |
| User registration | `pages/user-register/*`, `services/auth.service.ts` | `POST /api/auth/register` | `AuthController.java`, `RegisterRequest.java`, `UserServiceImpl.register()`, `User.java`, `UserRepository.java` |
| Admin registration | `pages/admin-register/*`, `services/auth.service.ts` | `POST /api/auth/register-admin` | `AuthController.java`, `AdminRegisterRequest.java`, `UserServiceImpl.registerAdmin()`, `application.properties` admin secret |
| User login | `pages/user-login/*`, `services/auth.service.ts` | `POST /api/auth/user/login` | `AuthController.java`, `LoginRequest.java`, `UserServiceImpl.loginUser()`, `JwtUtil.java` |
| Admin login | `pages/admin-login/*`, `services/auth.service.ts` | `POST /api/auth/admin/login` | `AuthController.java`, `LoginRequest.java`, `UserServiceImpl.loginAdmin()`, `JwtUtil.java` |
| Store login token | `services/auth.service.ts` | No separate API | Cookies `currentUser` and `token`; idle timeout in `AuthService` |
| Add token to API calls | `interceptors/auth.interceptor.ts` | All protected APIs | Adds `Authorization: Bearer <token>` |
| Protect user pages | `guards/auth.guard.ts`, `app.routes.ts` | No direct API | Redirects to `/user/login` if not logged in |
| Protect admin pages | `guards/admin.guard.ts`, `app.routes.ts` | No direct API | Redirects to `/admin/login` if not admin |
| Get profile | `pages/profile/*`, `services/user.service.ts` | `GET /api/users/profile` | `UserController.java`, `UserServiceImpl.getUserProfile()` |
| Change password | `pages/profile/*`, `services/user.service.ts` | `PUT /api/users/change-password` | `UserController.java`, `ChangePasswordRequest.java`, `UserServiceImpl.changePassword()` |
| Get wallet balance | `pages/payment/*`, `services/user.service.ts` | `GET /api/users/profile/wallet` | `UserController.java`, `UserServiceImpl.getWalletBalance()` |
| Update wallet balance | Called by Payment Service | `POST /api/users/profile/wallet/update` | `UserController.java`, `UserServiceImpl.updateWalletBalance()`, `UserServiceFeignClient.java` |
| View all users | `pages/admin-dashboard/*`, `services/user.service.ts` | `GET /api/users/all` | `UserController.java`, `UserServiceImpl.getAllUsers()`, admin rule in `SecurityConfig.java` |
| Promote user to admin | `pages/admin-dashboard/*`, `services/user.service.ts` | `PUT /api/users/promote/{userId}` | `UserController.java`, `UserServiceImpl.promoteToAdmin()` |
| Create operator | `pages/admin-dashboard/*`, `services/operator.service.ts` | `POST /api/operators` | `OperatorController.java`, `OperatorServiceImpl.addOperator()`, `OperatorRepository.java`, `Operator.java` |
| Update operator | `pages/admin-dashboard/*`, `services/operator.service.ts` | `PUT /api/operators/{id}` | `OperatorController.java`, `OperatorServiceImpl.updateOperator()` |
| Patch operator | API supported | `PATCH /api/operators/{id}` | `OperatorController.java`, `OperatorServiceImpl.patchOperator()` |
| Delete operator | `pages/admin-dashboard/*`, `services/operator.service.ts` | `DELETE /api/operators/{id}` | `OperatorController.java`, `OperatorServiceImpl.deleteOperator()` |
| List operators | `pages/recharge/*`, `pages/admin-dashboard/*`, `services/operator.service.ts` | `GET /api/operators` | `OperatorController.java`, `OperatorServiceImpl.getAllOperators()`, Redis cache |
| Get operator by id | API and Feign | `GET /api/operators/{id}` | `OperatorController.java`, `OperatorServiceImpl.getOperatorById()`, `OperatorFeignClient.java` |
| Filter operators by status | `pages/admin-dashboard/*`, `services/operator.service.ts` | `GET /api/operators/status/{status}` | `OperatorController.java`, `OperatorServiceImpl.getOperatorsByStatus()` |
| Filter operators by type | `pages/admin-dashboard/*`, `services/operator.service.ts` | `GET /api/operators/type/{type}` | `OperatorController.java`, `OperatorServiceImpl.getOperatorsByType()` |
| Create recharge plan | `pages/admin-dashboard/*`, `services/operator.service.ts` | `POST /api/plans` | `RechargePlanController.java`, `RechargePlanServiceImpl.addPlan()`, `RechargePlan.java` |
| Update recharge plan | `pages/admin-dashboard/*`, `services/operator.service.ts` | `PUT /api/plans/{id}` | `RechargePlanController.java`, `RechargePlanServiceImpl.updatePlan()` |
| Patch recharge plan | API supported | `PATCH /api/plans/{id}` | `RechargePlanController.java`, `RechargePlanServiceImpl.patchPlan()` |
| Delete recharge plan | `pages/admin-dashboard/*`, `services/operator.service.ts` | `DELETE /api/plans/{id}` | `RechargePlanController.java`, `RechargePlanServiceImpl.deletePlan()` |
| List all plans | `pages/admin-dashboard/*`, `services/operator.service.ts` | `GET /api/plans` | `RechargePlanController.java`, `RechargePlanServiceImpl.getAllPlans()` |
| Plans by operator | `pages/recharge/*`, `services/operator.service.ts` | `GET /api/plans/operator/{operatorId}` | `RechargePlanController.java`, `RechargePlanServiceImpl.getPlansByOperator()` |
| Active plans by operator | `services/operator.service.ts` | `GET /api/plans/operator/{operatorId}/active` | `RechargePlanController.java`, `RechargePlanServiceImpl.getActivePlansByOperator()` |
| Plans by category | `pages/admin-dashboard/*`, `services/operator.service.ts` | `GET /api/plans/category/{category}` | `RechargePlanController.java`, `RechargePlanServiceImpl.getPlansByCategory()` |
| Start recharge | `pages/recharge/*`, `services/recharge.service.ts` | `POST /api/recharge/initiate` | `RechargeController.java`, `RechargeServiceImpl.initiateRecharge()`, `OperatorFeignClient.java`, `RechargeEventPublisher.java` |
| Validate mobile number | `pages/recharge/*` and backend | Same as start recharge | Regex in `RechargeServiceImpl.initiateRecharge()` |
| Validate operator and plan | Backend Feign call | Same as start recharge | `OperatorFeignClient.java`, `RechargeServiceImpl.initiateRecharge()` |
| Publish recharge event | Backend only | No direct frontend API | `RechargeEventPublisher.java`, `RabbitMQConfig.java`, `payment.queue` |
| Create pending transaction from queue | Backend only | RabbitMQ consumer | `RechargeEventConsumer.java`, `PaymentServiceImpl.processPayment()` |
| Pay for recharge | `pages/payment/*`, `services/payment.service.ts` | `POST /api/transactions/pay` | `TransactionController.java`, `PaymentServiceImpl.makePayment()`, `DummyPaymentGatewayService.java` |
| Wallet payment | `pages/payment/*` | `POST /api/transactions/pay` | `PaymentServiceImpl.makePayment()`, `UserServiceFeignClient.java`, `UserServiceImpl.updateWalletBalance()` |
| Wallet top-up | `pages/payment/*`, `services/payment.service.ts` | `POST /api/transactions/wallet/topup` | `TransactionController.java`, `PaymentServiceImpl.topUpWallet()`, `UserServiceFeignClient.java` |
| Update recharge status after payment | Backend only | `PUT /api/recharge/update-status/{rechargeId}` | `PaymentServiceImpl.updateRechargeStatusWithRetry()`, `RechargeServiceFeignClient.java`, `RechargeServiceImpl.updateRechargeStatus()` |
| Retry if recharge update fails | Backend only | No frontend API | `PaymentServiceImpl.updateRechargeStatusWithRetry()` |
| Refund pending if retry exhausted | Backend only | No frontend API | `PaymentServiceImpl.handleRetryExhaustion()`, `publishRefundNotification()` |
| Publish payment notification | Backend only | RabbitMQ message | `PaymentResultPublisher.java`, `PaymentServiceImpl.publishNotification()` |
| Consume notification | Backend only | RabbitMQ consumer | `NotificationConsumer.java`, `EmailService.java` |
| User recharge history | `pages/user-dashboard/*`, `pages/history/*`, `services/recharge.service.ts` | `GET /api/recharge/my-history` | `RechargeController.java`, `RechargeServiceImpl.getMyRechargeHistory()` |
| Cancel recharge | `pages/user-dashboard/*`, `pages/history/*`, `pages/admin-dashboard/*` | `PUT /api/recharge/{id}/cancel` | `RechargeController.java`, `RechargeServiceImpl.cancelRecharge()` |
| Admin all recharges | `pages/admin-dashboard/*`, `services/recharge.service.ts` | `GET /api/recharge/all` | `RechargeController.java`, `RechargeServiceImpl.getAllRecharges()` |
| Filter recharges by status | `pages/admin-dashboard/*`, `services/recharge.service.ts` | `GET /api/recharge/status/{status}` | `RechargeController.java`, `RechargeServiceImpl.getRechargesByStatus()` |
| Filter recharges by mobile | `pages/admin-dashboard/*`, `services/recharge.service.ts` | `GET /api/recharge/mobile/{mobileNumber}` | `RechargeController.java`, `RechargeServiceImpl.getRechargesByMobile()` |
| User transaction history | `pages/history/*`, `services/payment.service.ts` | `GET /api/transactions/my-transactions` | `TransactionController.java`, `PaymentServiceImpl.getMyTransactions()` |
| Transaction by id | `services/payment.service.ts` | `GET /api/transactions/txn/{transactionId}` | `TransactionController.java`, `PaymentServiceImpl.getByTransactionId()` |
| Transaction by recharge id | `services/payment.service.ts` | `GET /api/transactions/recharge/{rechargeId}` | `TransactionController.java`, `PaymentServiceImpl.getByRechargeId()` |
| Admin all transactions | `pages/admin-dashboard/*`, `services/payment.service.ts` | `GET /api/transactions/all` | `TransactionController.java`, `PaymentServiceImpl.getAllTransactions()` |
| Filter transactions by status | `pages/admin-dashboard/*`, `services/payment.service.ts` | `GET /api/transactions/status/{status}` | `TransactionController.java`, `PaymentServiceImpl.getTransactionsByStatus()` |
| Filter transactions by mobile | `pages/admin-dashboard/*`, `services/payment.service.ts` | `GET /api/transactions/mobile/{mobileNumber}` | `TransactionController.java`, `PaymentServiceImpl.getTransactionsByMobile()` |
| Swagger docs | Browser only | `/swagger-ui.html` | `SwaggerConfig.java` in services, gateway Swagger config in properties |
| Health checks | Browser or Docker | `/actuator/health` | Spring Actuator dependency and application properties |
| Logs | Runtime only | No API | `logback-spring.xml` files |
| Tests | Maven/JUnit | No runtime API | `src/test/java/...` folders in each service |

For a bigger version of this table, see [docs/FEATURE_LOCATION_MAP.md](docs/FEATURE_LOCATION_MAP.md).

## 9. Backend Flow Details

### 9.1 User Registration Flow

```text
Angular user register page
  -> AuthService.registerUser()
  -> POST /api/auth/register
  -> API Gateway
  -> User Service AuthController.register()
  -> UserServiceImpl.register()
  -> checks duplicate username/email
  -> encrypts password using BCrypt
  -> saves User with ROLE_USER
  -> creates JWT token
  -> returns AuthResponse
```

Files:

- `omnicharge-frontend/src/app/pages/user-register/user-register.component.ts`
- `omnicharge-frontend/src/app/services/auth.service.ts`
- `user-service/src/main/java/com/omnicharge/user_service/controller/AuthController.java`
- `user-service/src/main/java/com/omnicharge/user_service/service/UserServiceImpl.java`
- `user-service/src/main/java/com/omnicharge/user_service/entity/User.java`

### 9.2 Login And JWT Flow

```text
Login page
  -> AuthService.loginUser() or loginAdmin()
  -> User Service validates username/password
  -> JwtUtil generates token
  -> Frontend stores token in cookie
  -> auth.interceptor.ts adds token to future API calls
  -> Gateway validates token
  -> Downstream service validates token again
```

Important files:

- `omnicharge-frontend/src/app/services/auth.service.ts`
- `omnicharge-frontend/src/app/interceptors/auth.interceptor.ts`
- `api-gateway/src/main/java/com/omnicharge/api_gateway/filter/JwtAuthenticationFilter.java`
- `user-service/src/main/java/com/omnicharge/user_service/security/JwtUtil.java`
- `*/filter/JwtAuthFilter.java` inside each backend service

### 9.3 Admin Operator And Plan Management Flow

```text
Admin Dashboard
  -> OperatorService Angular client
  -> /api/operators or /api/plans
  -> API Gateway
  -> Operator Service controller
  -> OperatorServiceImpl or RechargePlanServiceImpl
  -> Repository
  -> PostgreSQL operator_db
  -> Redis cache evicted or refreshed
```

Write APIs require `ROLE_ADMIN`.

Important files:

- `omnicharge-frontend/src/app/pages/admin-dashboard/admin-dashboard.component.ts`
- `omnicharge-frontend/src/app/services/operator.service.ts`
- `operator-service/src/main/java/com/omnicharge/operator_service/config/SecurityConfig.java`
- `operator-service/src/main/java/com/omnicharge/operator_service/service/OperatorServiceImpl.java`
- `operator-service/src/main/java/com/omnicharge/operator_service/service/RechargePlanServiceImpl.java`

### 9.4 Recharge Flow

```text
Recharge page
  -> User selects operator
  -> Frontend loads plans
  -> User enters mobile number
  -> POST /api/recharge/initiate
  -> Recharge Service validates mobile number
  -> Recharge Service calls Operator Service through Feign
  -> Operator and plan must be ACTIVE
  -> Recharge record saved as PENDING
  -> Recharge event sent to RabbitMQ payment.queue
  -> Frontend navigates to payment page
```

Important files:

- `omnicharge-frontend/src/app/pages/recharge/recharge.component.ts`
- `omnicharge-frontend/src/app/services/recharge.service.ts`
- `recharge-service/src/main/java/com/omnicharge/recharge_service/service/RechargeServiceImpl.java`
- `recharge-service/src/main/java/com/omnicharge/recharge_service/feign/OperatorFeignClient.java`
- `recharge-service/src/main/java/com/omnicharge/recharge_service/messaging/RechargeEventPublisher.java`

### 9.5 Payment Flow

```text
Payment page
  -> User chooses CARD, UPI, NETBANKING, or WALLET
  -> POST /api/transactions/pay
  -> Payment Service finds pending transaction by rechargeId
  -> DummyPaymentGatewayService simulates payment
  -> Transaction becomes SUCCESS or FAILED
  -> Payment Service calls Recharge Service to update recharge status
  -> Payment Service publishes notification message
```

Important files:

- `omnicharge-frontend/src/app/pages/payment/payment.component.ts`
- `payment-service/src/main/java/com/omnicharge/payment_service/controller/TransactionController.java`
- `payment-service/src/main/java/com/omnicharge/payment_service/service/PaymentServiceImpl.java`
- `payment-service/src/main/java/com/omnicharge/payment_service/service/DummyPaymentGatewayService.java`
- `payment-service/src/main/java/com/omnicharge/payment_service/feign/RechargeServiceFeignClient.java`
- `payment-service/src/main/java/com/omnicharge/payment_service/messaging/PaymentResultPublisher.java`

### 9.6 Wallet Flow

Wallet balance lives in User Service, but payment logic lives in Payment Service.

Top-up:

```text
Payment page add money
  -> POST /api/transactions/wallet/topup
  -> Payment Service simulates top-up payment
  -> Payment Service calls User Service
  -> UserServiceImpl.updateWalletBalance(..., isTopUp=true)
```

Wallet payment:

```text
Payment page chooses WALLET
  -> Payment Service checks wallet balance through UserServiceFeignClient
  -> If enough balance, payment gateway succeeds
  -> Payment Service deducts wallet through UserServiceFeignClient
```

Files:

- `payment-service/src/main/java/com/omnicharge/payment_service/service/PaymentServiceImpl.java`
- `payment-service/src/main/java/com/omnicharge/payment_service/feign/UserServiceFeignClient.java`
- `user-service/src/main/java/com/omnicharge/user_service/service/UserServiceImpl.java`
- `user-service/src/main/java/com/omnicharge/user_service/controller/UserController.java`

### 9.7 Retry And Refund Flow

This is one of the advanced backend features.

```text
Payment succeeds
  -> Payment Service tries to update Recharge Service
  -> If Recharge Service is down, retry happens
  -> If all retries fail:
       transaction becomes REFUND_PENDING
       refund reason is saved
       wallet payment is refunded to wallet
       refund notification is published
```

Files:

- `payment-service/src/main/java/com/omnicharge/payment_service/service/PaymentServiceImpl.java`
- Method area: `updateRechargeStatusWithRetry()`
- Method area: `handleRetryExhaustion()`
- Method area: `publishRefundNotification()`

### 9.8 Notification Flow

```text
Payment Service
  -> PaymentResultPublisher
  -> RabbitMQ notification.queue
  -> NotificationConsumer
  -> EmailService
```

Files:

- `payment-service/src/main/java/com/omnicharge/payment_service/messaging/PaymentResultPublisher.java`
- `notification-service/src/main/java/com/omnicharge/notification_service/messaging/NotificationConsumer.java`
- `notification-service/src/main/java/com/omnicharge/notification_service/service/EmailService.java`

## 10. Frontend Architecture

Frontend folder:

```text
omnicharge-frontend/
```

Main files:

| File or folder | Purpose |
|---|---|
| `src/app/app.routes.ts` | All frontend routes |
| `src/app/app.config.ts` | Angular app configuration |
| `src/app/interceptors/auth.interceptor.ts` | Adds JWT token to API calls |
| `src/app/guards/auth.guard.ts` | Protects normal user pages |
| `src/app/guards/admin.guard.ts` | Protects admin pages |
| `src/app/services/auth.service.ts` | Register, login, logout, token cookies, idle timeout |
| `src/app/services/user.service.ts` | Profile, users, password, wallet APIs |
| `src/app/services/operator.service.ts` | Operator and plan APIs |
| `src/app/services/recharge.service.ts` | Recharge APIs |
| `src/app/services/payment.service.ts` | Payment and transaction APIs |
| `proxy.conf.json` | Sends Angular `/api` calls to API Gateway |

Frontend pages:

| Page | URL | Files | Purpose |
|---|---|---|---|
| Home | `/home` | `pages/home/*` | Landing page, clears session |
| User login | `/user/login` | `pages/user-login/*` | User login |
| User register | `/user/register` | `pages/user-register/*` | User registration |
| Admin login | `/admin/login` | `pages/admin-login/*` | Admin login |
| Admin register | `/admin/register` | `pages/admin-register/*` | Admin registration |
| User dashboard | `/dashboard` | `pages/user-dashboard/*` | User recharge summary and cancel |
| Recharge | `/recharge` | `pages/recharge/*` | Select operator, plan, mobile number |
| Payment | `/payment/:rechargeId` | `pages/payment/*` | Pay and top up wallet |
| History | `/history` | `pages/history/*` | Recharge and transaction history |
| Profile | `/profile` | `pages/profile/*` | Profile and password change |
| Admin dashboard | `/admin/dashboard` | `pages/admin-dashboard/*` | Admin management dashboard |

## 11. API Endpoints

Use API Gateway for normal calls:

```text
http://localhost:8080
```

### Auth APIs

| Method | Path | Role |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/register-admin` | Public with admin secret |
| POST | `/api/auth/user/login` | Public |
| POST | `/api/auth/admin/login` | Public |

### User APIs

| Method | Path | Role |
|---|---|---|
| GET | `/api/users/profile` | Logged in |
| GET | `/api/users/all` | Admin |
| PUT | `/api/users/promote/{userId}` | Admin |
| PUT | `/api/users/change-password` | Logged in |
| GET | `/api/users/profile/wallet` | Logged in |
| POST | `/api/users/profile/wallet/update` | Logged in/internal service |

### Operator APIs

| Method | Path | Role |
|---|---|---|
| POST | `/api/operators` | Admin |
| PUT | `/api/operators/{id}` | Admin |
| PATCH | `/api/operators/{id}` | Admin |
| DELETE | `/api/operators/{id}` | Admin |
| GET | `/api/operators/{id}` | Logged in |
| GET | `/api/operators` | Logged in |
| GET | `/api/operators/status/{status}` | Logged in |
| GET | `/api/operators/type/{type}` | Logged in |

### Plan APIs

| Method | Path | Role |
|---|---|---|
| POST | `/api/plans` | Admin |
| PUT | `/api/plans/{id}` | Admin |
| PATCH | `/api/plans/{id}` | Admin |
| DELETE | `/api/plans/{id}` | Admin |
| GET | `/api/plans/{id}` | Logged in |
| GET | `/api/plans` | Logged in |
| GET | `/api/plans/operator/{operatorId}` | Logged in |
| GET | `/api/plans/operator/{operatorId}/active` | Logged in |
| GET | `/api/plans/category/{category}` | Logged in |

### Recharge APIs

| Method | Path | Role |
|---|---|---|
| POST | `/api/recharge/initiate` | User/Admin |
| PUT | `/api/recharge/{id}/cancel` | Owner or admin |
| GET | `/api/recharge/my-history` | Logged in |
| GET | `/api/recharge/all` | Admin |
| GET | `/api/recharge/status/{status}` | Admin |
| GET | `/api/recharge/mobile/{mobileNumber}` | Admin |
| GET | `/api/recharge/{id}` | Owner or admin |
| PUT | `/api/recharge/update-status/{rechargeId}` | Internal/payment service |

### Transaction APIs

| Method | Path | Role |
|---|---|---|
| POST | `/api/transactions/pay` | Logged in |
| POST | `/api/transactions/wallet/topup` | Logged in |
| GET | `/api/transactions/my-transactions` | Logged in |
| GET | `/api/transactions/txn/{transactionId}` | Logged in |
| GET | `/api/transactions/recharge/{rechargeId}` | Logged in |
| GET | `/api/transactions/all` | Admin |
| GET | `/api/transactions/status/{status}` | Admin |
| GET | `/api/transactions/mobile/{mobileNumber}` | Admin |

## 12. Security Rules

Security is implemented in every backend service.

| Area | Files | What it does |
|---|---|---|
| Gateway JWT | `api-gateway/.../JwtAuthenticationFilter.java` | Blocks missing or invalid token before routing |
| User Service security | `user-service/.../SecurityConfig.java` | Public auth APIs, admin-only user list/promote |
| Operator Service security | `operator-service/.../SecurityConfig.java` | GET requires login, write requires admin |
| Recharge Service security | `recharge-service/.../SecurityConfig.java` | Admin filters, internal status update, owner checks |
| Payment Service security | `payment-service/.../SecurityConfig.java` | Admin transaction filters |
| Frontend route security | `auth.guard.ts`, `admin.guard.ts` | Prevents wrong pages in browser |
| Frontend API token | `auth.interceptor.ts` | Sends JWT token with requests |

## 13. Database Design

`init-db.sql` creates:

```sql
CREATE DATABASE user_db;
CREATE DATABASE operator_db;
CREATE DATABASE recharge_db;
CREATE DATABASE payment_db;
CREATE DATABASE sonar_db;
```

Service database ownership:

| Service | Database | Main entity files |
|---|---|---|
| User Service | `user_db` | `User.java`, `Role.java` |
| Operator Service | `operator_db` | `Operator.java`, `RechargePlan.java` |
| Recharge Service | `recharge_db` | `RechargeRequest.java`, `RechargeStatus.java` |
| Payment Service | `payment_db` | `Transaction.java`, `TransactionStatus.java`, `PaymentMethod.java` |
| Notification Service | No main project DB | Consumes messages and sends email |

## 14. RabbitMQ Queues

| Queue | Producer | Consumer | Purpose |
|---|---|---|---|
| `payment.queue` | Recharge Service | Payment Service | Create pending transaction after recharge starts |
| `notification.queue` | Payment Service | Notification Service | Notify user about payment/recharge/refund result |

RabbitMQ files:

| Service | Files |
|---|---|
| Recharge Service | `RabbitMQConfig.java`, `RechargeEventPublisher.java` |
| Payment Service | `RabbitMQConfig.java`, `RechargeEventConsumer.java`, `PaymentResultPublisher.java` |
| Notification Service | `RabbitMQConfig.java`, `NotificationConsumer.java` |

## 15. Redis Cache

Redis is used in Operator Service.

| File | Purpose |
|---|---|
| `operator-service/src/main/java/com/omnicharge/operator_service/config/CacheConfig.java` | Configures cache |
| `OperatorServiceImpl.java` | Caches operator reads and evicts cache on write |
| `RechargePlanServiceImpl.java` | Caches plan reads and evicts cache on write |

Cache examples:

- `operators`
- `operator-by-id`
- `plans`
- `plan-by-id`
- `plans-by-operator`

## 16. Environment Variables

Create `.env` from `.env.example`.

```powershell
Copy-Item .env.example .env
```

Important variables:

| Variable | Purpose |
|---|---|
| `POSTGRES_PASSWORD` | PostgreSQL password |
| `SPRING_DATASOURCE_PASSWORD` | Backend DB password |
| `RABBITMQ_DEFAULT_USER` | RabbitMQ username |
| `RABBITMQ_DEFAULT_PASS` | RabbitMQ password |
| `RABBITMQ_PASSWORD` | Backend RabbitMQ password |
| `JWT_SECRET` | JWT signing secret |
| `APP_ADMIN_SECRET_KEY` | Secret required for admin registration |
| `SONAR_DB_USER` | SonarQube DB user |
| `SONAR_DB_PASSWORD` | SonarQube DB password |
| `MAIL_USERNAME` | Notification email username |
| `MAIL_PASSWORD` | Notification email app password |
| `CONFIG_REPO_USERNAME` | Config repository username |
| `CONFIG_REPO_PASSWORD` | Config repository token/password |

Important security note:

Do not commit real passwords, mail app passwords, GitHub tokens, or JWT secrets. Keep real values only in `.env`.

## 17. Run The Project

### 17.1 Start Backend With Docker

From project root:

```powershell
cd D:\Capgemini\sprint
Copy-Item .env.example .env
docker compose up --build
```

If `.env` already exists, do not overwrite it unless you want to reset your local values.

### 17.2 Start Frontend

Open another terminal:

```powershell
cd D:\Capgemini\sprint\omnicharge-frontend
npm install
npm start
```

Open:

```text
http://localhost:4200
```

### 17.3 Useful URLs

| Tool | URL |
|---|---|
| Frontend | `http://localhost:4200` |
| Gateway Swagger | `http://localhost:8080/swagger-ui/index.html` |
| Eureka | `http://localhost:8761` |
| RabbitMQ dashboard | `http://localhost:15672` |
| Zipkin | `http://localhost:9411` |
| SonarQube | `http://localhost:9000` |

## 18. Run One Service Manually

Use Docker for infrastructure first, then run one service manually if developing.

Example:

```powershell
cd D:\Capgemini\sprint\user-service
mvn spring-boot:run
```

Manual startup order if you run everything yourself:

1. PostgreSQL
2. RabbitMQ
3. Redis
4. Zipkin
5. Eureka Server
6. Config Server
7. User Service
8. Operator Service
9. Recharge Service
10. Payment Service
11. Notification Service
12. API Gateway
13. Angular frontend

## 19. Testing

Run tests for one backend service:

```powershell
cd D:\Capgemini\sprint\user-service
mvn test
```

Run frontend tests:

```powershell
cd D:\Capgemini\sprint\omnicharge-frontend
npm test
```

Run backend coverage:

```powershell
cd D:\Capgemini\sprint\user-service
mvn clean verify
```

Open:

```text
target/site/jacoco/index.html
```

Test folders:

| Service | Test folder |
|---|---|
| API Gateway | `api-gateway/src/test/java` |
| User Service | `user-service/src/test/java` |
| Operator Service | `operator-service/src/test/java` |
| Recharge Service | `recharge-service/src/test/java` |
| Payment Service | `payment-service/src/test/java` |
| Notification Service | `notification-service/src/test/java` |
| Eureka Server | `eureka-server/src/test/java` |
| Config Server | `config-server/src/test/java` |

## 20. Swagger API Testing

When services are running:

| Service | Swagger |
|---|---|
| Gateway aggregated Swagger | `http://localhost:8080/swagger-ui/index.html` |
| User Service | `http://localhost:8081/swagger-ui.html` |
| Operator Service | `http://localhost:8082/swagger-ui.html` |
| Recharge Service | `http://localhost:8083/swagger-ui.html` |
| Payment Service | `http://localhost:8084/swagger-ui.html` |
| Notification Service | `http://localhost:8085/swagger-ui.html` |

For protected APIs:

1. Login first.
2. Copy JWT token.
3. In Swagger, click Authorize.
4. Enter:

```text
Bearer your-token-here
```

## 21. Health Checks

| Service | Health URL |
|---|---|
| API Gateway | `http://localhost:8080/actuator/health` |
| User Service | `http://localhost:8081/actuator/health` |
| Operator Service | `http://localhost:8082/actuator/health` |
| Recharge Service | `http://localhost:8083/actuator/health` |
| Payment Service | `http://localhost:8084/actuator/health` |
| Notification Service | `http://localhost:8085/actuator/health` |
| Eureka Server | `http://localhost:8761/actuator/health` |
| Config Server | `http://localhost:8888/actuator/health` |

## 22. Common Problems

| Problem | Reason | Fix |
|---|---|---|
| `401 Unauthorized` | Missing or invalid JWT | Login again and send `Authorization: Bearer <token>` |
| `403 Forbidden` | Logged in but wrong role | Use admin account for admin APIs |
| Service missing in Eureka | Service did not start or cannot reach Eureka | Check logs and `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` |
| Database connection failed | PostgreSQL not ready or wrong password | Check `.env`, Docker logs, and DB container |
| RabbitMQ error | RabbitMQ is down or password mismatch | Check RabbitMQ container and `.env` |
| Frontend API failing | Gateway not running or proxy not active | Start API Gateway and frontend with `npm start` |
| Port already used | Another app is using the same port | Stop old app/container or change port |
| SonarQube not opening | It starts slowly | Wait 2-3 minutes and check logs |

## 23. Extra Documentation

| Document | Purpose |
|---|---|
| [docs/FEATURE_LOCATION_MAP.md](docs/FEATURE_LOCATION_MAP.md) | Full feature-to-file map |
| [docs/SETUP.md](docs/SETUP.md) | Setup guide |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Architecture guide |
| [docs/API.md](docs/API.md) | API details |
| [docs/ENVIRONMENT.md](docs/ENVIRONMENT.md) | Environment variables |
| [docs/TESTING.md](docs/TESTING.md) | Testing guide |
| [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) | Debugging guide |

## 24. Best Learning Path

If you want to learn the project from zero to hero:

1. Read this README once.
2. Run the project with Docker.
3. Open frontend and register a normal user.
4. Register an admin using admin secret.
5. Login as admin and create one operator.
6. Create one recharge plan for that operator.
7. Login as user and start recharge.
8. Pay for recharge.
9. Open user history and admin dashboard.
10. Open Eureka and see registered services.
11. Open RabbitMQ and observe queues.
12. Read `user-service` first because it is easiest.
13. Read `operator-service` next.
14. Read `recharge-service` and understand Feign plus RabbitMQ publish.
15. Read `payment-service` last because it has the most complex logic.
16. Read `notification-service` to understand queue consumption.
17. Run tests for each service.
18. Use Swagger to test APIs manually.

## 25. License

This project was developed as part of the Capgemini Sprint program.
