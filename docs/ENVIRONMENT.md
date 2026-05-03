# Environment Variables

Environment variables are values that are passed to the application from outside the code.

They are useful for passwords, database URLs, tokens, and settings that can change between computers.

## Important Security Rule

Do not commit real secrets.

Examples of secrets:

- Database passwords
- Mail passwords
- JWT secret keys
- GitHub tokens
- API tokens

Use `.env.example` for fake or learning values. Use `.env` for real local values.

## Files

| File | Purpose |
|---|---|
| `.env.example` | Safe template showing required variable names |
| `.env` | Real local values used by Docker Compose |
| `docker-compose.yml` | Reads values from `.env` |
| `application.properties` | Default service configuration |

## Variables Used By Docker Compose

| Variable | Used by | Meaning |
|---|---|---|
| `POSTGRES_PASSWORD` | PostgreSQL | Password for the PostgreSQL server |
| `SPRING_DATASOURCE_PASSWORD` | Backend services | Password used by services to connect to PostgreSQL |
| `RABBITMQ_DEFAULT_USER` | RabbitMQ | RabbitMQ username |
| `RABBITMQ_DEFAULT_PASS` | RabbitMQ | RabbitMQ password |
| `RABBITMQ_PASSWORD` | Backend services | Password used by services to connect to RabbitMQ |
| `JWT_SECRET` | Backend services | Secret key used to sign and verify JWT tokens |
| `APP_ADMIN_SECRET_KEY` | User Service | Secret needed to create an admin account |
| `SONAR_DB_USER` | SonarQube database | SonarQube database username |
| `SONAR_DB_PASSWORD` | SonarQube database | SonarQube database password |
| `MAIL_USERNAME` | Notification Service | Email account username |
| `MAIL_PASSWORD` | Notification Service | Email app password |
| `CONFIG_REPO_USERNAME` | Config Server | Username for external config repository, if used |
| `CONFIG_REPO_PASSWORD` | Config Server | Token or password for external config repository, if used |

## How To Create `.env`

From the project root:

```powershell
Copy-Item .env.example .env
```

Then edit `.env` if needed.

## Local Development Defaults

When running services manually, many `application.properties` files use localhost defaults:

| Setting | Default |
|---|---|
| PostgreSQL host | `localhost` |
| PostgreSQL port | `5432` |
| RabbitMQ host | `localhost` |
| RabbitMQ port | `5672` |
| Redis host | `localhost` |
| Redis port | `6379` |
| Eureka URL | `http://localhost:8761/eureka/` |
| Config Server URL | `http://localhost:8888` |

Inside Docker Compose, service names are used instead of localhost. For example, a backend container connects to PostgreSQL using host name `postgres`.
