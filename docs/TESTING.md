# Testing And Code Quality

Testing means checking that the code works as expected.

This project has backend tests for Spring Boot services and frontend tests for Angular.

## Backend Tests

Run tests for one service:

```powershell
cd D:\Capgemini\sprint\user-service
mvn test
```

Run tests for another service by changing the folder:

```powershell
cd D:\Capgemini\sprint\operator-service
mvn test
```

## Backend Services With Tests

| Service | Test command |
|---|---|
| API Gateway | `cd api-gateway; mvn test` |
| User Service | `cd user-service; mvn test` |
| Operator Service | `cd operator-service; mvn test` |
| Recharge Service | `cd recharge-service; mvn test` |
| Payment Service | `cd payment-service; mvn test` |
| Notification Service | `cd notification-service; mvn test` |
| Eureka Server | `cd eureka-server; mvn test` |
| Config Server | `cd config-server; mvn test` |

## Run Tests For All Backend Services

From the project root in PowerShell:

```powershell
$services = @(
  "api-gateway",
  "user-service",
  "operator-service",
  "recharge-service",
  "payment-service",
  "notification-service",
  "eureka-server",
  "config-server"
)

foreach ($service in $services) {
  Push-Location $service
  mvn test
  Pop-Location
}
```

## Frontend Tests

```powershell
cd D:\Capgemini\sprint\omnicharge-frontend
npm test
```

## Build Checks

Backend package build:

```powershell
cd D:\Capgemini\sprint\user-service
mvn clean package
```

Frontend build:

```powershell
cd D:\Capgemini\sprint\omnicharge-frontend
npm run build
```

## Code Coverage

The backend services use JaCoCo for coverage.

Run:

```powershell
cd D:\Capgemini\sprint\user-service
mvn clean verify
```

Then open the generated report:

```text
target/site/jacoco/index.html
```

## SonarQube

SonarQube checks code quality, bugs, vulnerabilities, test coverage, and code smells.

Start SonarQube with Docker Compose:

```powershell
docker compose up -d sonarqube sonarqube-db
```

Open:

```text
http://localhost:9000
```

Run the scan script:

```powershell
powershell -ExecutionPolicy Bypass .\sonar-scan.ps1
```

If SonarQube does not open immediately, wait a few minutes. It starts slowly.
