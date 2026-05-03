# Architecture Guide

Architecture means how the project is arranged and how each part talks to the other parts.

OmniCharge uses microservices. That means the backend is split into smaller services. Each service has one main responsibility.

## High Level Diagram

```text
Browser
  |
  v
Angular Frontend
  |
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
  |          v
  |       RabbitMQ payment.queue
  |
  +--> Payment Service -----> PostgreSQL payment_db
             |
             v
          RabbitMQ notification.queue
             |
             v
      Notification Service
```

## Why There Is An API Gateway

Without a gateway, the frontend would need to remember many backend URLs:

```text
8081 for users
8082 for operators
8083 for recharge
8084 for payment
```

With the gateway, the frontend can call one main backend:

```text
http://localhost:8080
```

The gateway then forwards the request to the correct service.

## Why There Is Eureka

Eureka is like a contact list for services.

When a service starts, it registers itself with Eureka. Other services can discover it by name instead of hardcoding exact locations.

Dashboard:

```text
http://localhost:8761
```

## Why There Is RabbitMQ

RabbitMQ is used when one service should not wait for another service to finish immediately.

Example:

1. Recharge Service creates a recharge request.
2. Recharge Service sends a message to `payment.queue`.
3. Payment Service reads that message and processes payment.
4. Payment Service sends a message to `notification.queue`.
5. Notification Service reads that message and sends a notification.

This makes the flow asynchronous.

## Why There Is Redis

Redis is used by Operator Service for caching.

Cache means storing frequently used data in a faster place. Operators and plans are read often, so caching helps avoid repeated database work.

## Why Each Service Has Its Own Database

In microservices, each service should own its own data.

| Service | Database |
|---|---|
| User Service | `user_db` |
| Operator Service | `operator_db` |
| Recharge Service | `recharge_db` |
| Payment Service | `payment_db` |

The file `init-db.sql` creates these databases when the PostgreSQL container starts for the first time.

## Service Responsibilities

| Service | Responsibility |
|---|---|
| User Service | Users, admins, login, JWT token, profile, wallet |
| Operator Service | Operators and recharge plans |
| Recharge Service | Recharge creation, cancellation, history, status |
| Payment Service | Payment processing and transaction history |
| Notification Service | Consumes notification messages and sends alerts |
| API Gateway | Routes requests and checks JWT |
| Config Server | Provides shared configuration |
| Eureka Server | Tracks which services are running |

## Request Flow For Login

```text
Frontend
  -> POST /api/auth/user/login
  -> API Gateway
  -> User Service
  -> PostgreSQL user_db
  -> User Service returns JWT token
  -> Frontend stores token
```

## Request Flow For Recharge

```text
Frontend
  -> POST /api/recharge/initiate
  -> API Gateway
  -> Recharge Service
  -> Operator Service validates operator and plan
  -> Recharge Service saves recharge as PENDING
  -> Recharge Service publishes message to RabbitMQ
  -> Payment Service consumes message
  -> Payment Service creates transaction
  -> Payment Service updates recharge status
  -> Payment Service publishes notification message
  -> Notification Service consumes message
```

## Port List

| Component | Port |
|---|---:|
| Angular frontend | 4200 |
| API Gateway | 8080 |
| User Service | 8081 |
| Operator Service | 8082 |
| Recharge Service | 8083 |
| Payment Service | 8084 |
| Notification Service | 8085 |
| Eureka Server | 8761 |
| Config Server | 8888 |
| PostgreSQL | 5432 |
| RabbitMQ | 5672 |
| RabbitMQ dashboard | 15672 |
| Redis | 6379 |
| Zipkin | 9411 |
| SonarQube | 9000 |
