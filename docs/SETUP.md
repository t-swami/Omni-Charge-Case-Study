# Setup Guide

This document explains how to run OmniCharge from the beginning.

## 1. Install Required Tools

Install these first:

| Tool | Check command |
|---|---|
| Java 21 | `java -version` |
| Maven | `mvn -version` |
| Node.js | `node -v` |
| npm | `npm -v` |
| Docker Desktop | `docker --version` |
| Docker Compose | `docker compose version` |

If a command says it is not recognized, that tool is not installed or not added to your system path.

## 2. Open The Project

Open PowerShell and go to the project folder:

```powershell
cd D:\Capgemini\sprint
```

## 3. Create Environment File

The project uses a `.env` file for passwords and configuration values.

If `.env` does not exist, create it from the example:

```powershell
Copy-Item .env.example .env
```

For learning on your own machine, the example values are enough. For real use, replace them with strong private values.

## 4. Start Backend With Docker Compose

Run:

```powershell
docker compose up --build
```

This command builds and starts the backend services and supporting tools.

The first run can take several minutes because Docker downloads images and Maven downloads dependencies.

## 5. Check Backend Health

Open these URLs:

| What to check | URL |
|---|---|
| Eureka dashboard | http://localhost:8761 |
| API Gateway health | http://localhost:8080/actuator/health |
| User Service health | http://localhost:8081/actuator/health |
| Operator Service health | http://localhost:8082/actuator/health |
| Recharge Service health | http://localhost:8083/actuator/health |
| Payment Service health | http://localhost:8084/actuator/health |
| Notification Service health | http://localhost:8085/actuator/health |

If health shows `UP`, that service is running.

## 6. Start The Frontend

Open a second PowerShell window:

```powershell
cd D:\Capgemini\sprint\omnicharge-frontend
npm install
npm start
```

Open:

```text
http://localhost:4200
```

## 7. Stop The Project

In the terminal where Docker Compose is running, press:

```text
Ctrl + C
```

Then run:

```powershell
docker compose down
```

To remove database data also, run this only when you are sure:

```powershell
docker compose down -v
```

## Running Services Manually

For development, you can run one service manually:

```powershell
cd D:\Capgemini\sprint\user-service
mvn spring-boot:run
```

Manual running is harder for beginners because services depend on PostgreSQL, Eureka, Config Server, RabbitMQ, and Redis. Docker Compose is recommended first.

## Recommended Startup Order Without Docker

If you run everything manually, start in this order:

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
