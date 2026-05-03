# Troubleshooting Guide

This document lists common problems and simple fixes.

## Docker Compose Fails To Start

Try:

```powershell
docker compose down
docker compose up --build
```

If containers still fail, check logs:

```powershell
docker compose logs service-name
```

Example:

```powershell
docker compose logs user-service
```

## Port Is Already In Use

Error example:

```text
Port 8080 is already allocated
```

That means another application is already using the port.

Fix options:

- Stop the other application.
- Stop old Docker containers with `docker compose down`.
- Change the port mapping in `docker-compose.yml`.

## Database Connection Failed

Check PostgreSQL is running:

```powershell
docker compose ps postgres
```

Check logs:

```powershell
docker compose logs postgres
```

If database data is broken during local development, reset volumes:

```powershell
docker compose down -v
docker compose up --build
```

This deletes local database data.

## Service Does Not Appear In Eureka

Open:

```text
http://localhost:8761
```

If a service is missing:

1. Check that the service started.
2. Check the service logs.
3. Check `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`.
4. Make sure Eureka started before the service.

## API Returns 401 Unauthorized

This usually means the request has no valid JWT token.

Fix:

1. Login again.
2. Copy the token from login response.
3. Send this header:

```text
Authorization: Bearer your-token-here
```

## API Returns 403 Forbidden

This means you are logged in, but your role is not allowed.

Example:

- A normal user tries to create an operator.
- Only admin can create operators.

Login as admin or use a user with the correct role.

## RabbitMQ Connection Error

Check RabbitMQ:

```text
http://localhost:15672
```

Default local dashboard login usually comes from `.env`.

Also check:

```powershell
docker compose logs rabbitmq
```

## Redis Connection Error

Check Redis container:

```powershell
docker compose ps redis
```

Restart if needed:

```powershell
docker compose restart redis
```

## Frontend Cannot Call Backend

The Angular frontend uses `proxy.conf.json` to send `/api` calls to:

```text
http://localhost:8080
```

Check:

1. API Gateway is running.
2. Frontend was started with `npm start`.
3. The browser URL is `http://localhost:4200`.
4. The backend health URL works: `http://localhost:8080/actuator/health`.

## Maven Build Fails

Try:

```powershell
mvn clean test
```

If dependencies fail to download, check your internet connection and Maven settings.

## npm install Fails

Try:

```powershell
cd D:\Capgemini\sprint\omnicharge-frontend
npm cache verify
npm install
```

Make sure Node.js and npm are installed.

## SonarQube Does Not Open

SonarQube can take a few minutes to start.

Check:

```powershell
docker compose logs sonarqube
```

Then open:

```text
http://localhost:9000
```
