# Spring AWS Portfolio

Personal full-stack cloud portfolio project built with React + TypeScript, Java + Spring Boot, PostgreSQL, Docker, AWS, GitHub Actions and Terraform.

## Current implementation checkpoint

**Phase 1A — project skeleton and architecture guardrails**

Implemented now:

- Java 21 / Spring Boot 4.1.1 backend skeleton.
- Maven build configuration.
- PostgreSQL 18 local service via Docker Compose.
- Flyway and JPA infrastructure configuration.
- Actuator health endpoint.
- baseline Spring context test.
- baseline ArchUnit dependency rules.
- accepted design documents under `docs/`.

Intentionally not implemented yet:

- Identity / Security.
- Assessment business logic.
- Group business logic.
- AI Integration.
- Frontend (Phase 1B).

## Root Java package

The implementation currently uses:

```text
dev.springawsportfolio.portfolio
```

The design documents used `com.<project>.portfolio` as a placeholder. The concrete package above is the Phase 1A implementation choice and can be renamed before the repository's public namespace is finalized.

## Local prerequisites

- JDK 21
- Docker + Docker Compose
- Internet access on first Maven Wrapper run

## Run PostgreSQL

```bash
docker compose up -d postgres
```

Check it with:

```bash
docker compose ps
```

PostgreSQL 18+ uses `/var/lib/postgresql` as the official container volume root; the Compose file follows that layout.

## Run backend tests

```bash
cd backend
./mvnw test
```

The Phase 1A context smoke test deliberately excludes database/JPA/Flyway auto-configuration. Database-backed integration tests will use Testcontainers PostgreSQL starting in Phase 2.

## Run backend locally

With PostgreSQL healthy:

```bash
cd backend
./mvnw spring-boot:run
```

Then check:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

## Database ownership

Flyway is the schema owner. Hibernate uses:

```text
spring.jpa.hibernate.ddl-auto=validate
```

No placeholder `V1` migration is created in Phase 1A. Phase 2 will begin directly with the accepted migration history:

```text
V1__create_identity_tables.sql
V2__create_spring_session_tables.sql
V3__create_assessment_tables.sql
V4__create_group_tables.sql
V5__create_domain_indexes.sql
V6__seed_sixteen_personality_v1.sql
```
