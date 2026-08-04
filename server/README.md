# PulsePay — Server (Spring Boot)

Spring Boot 4.1.0 back-end for the PulsePay payment platform.

## Stack
| Layer | Technology |
|-------|-----------|
| Runtime | Java 25 (JDK 26 toolchain) |
| Framework | Spring Boot 4.1.0 |
| Database | PostgreSQL 16 |
| Migrations | Liquibase 5 (64 changesets) |
| Auth | JWT (jjwt 0.12.6) |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Build | Gradle 9 |

## Prerequisites
- JDK 26 installed
- PostgreSQL 16 running (see `docker-compose.yaml`)

## Quick start
```bash
cd server

# Start PostgreSQL via Docker
docker-compose up -d

# Run the application
./gradlew bootRun
```

## Useful URLs (once running)
| URL | Description |
|-----|-------------|
| `http://localhost:8080/swagger-ui/index.html` | Swagger UI |
| `http://localhost:8080/v3/api-docs` | OpenAPI JSON |
| `http://localhost:8080/actuator/health` | Health check |

## DBeaver connection
```
Host:     localhost
Port:     5432
Database: pulsedatabase
Username: pulseuser
Password: pulsepassword
```

## Gradle tasks
```bash
./gradlew bootRun        # Start dev server
./gradlew clean test     # Run all tests
./gradlew build          # Full build + tests
```
