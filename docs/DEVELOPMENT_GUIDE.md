# Medikit Microservice Development Guide

This document defines the conventions every Medikit microservice must follow.

## Module structure

Each service lives under `services/<service-name>` and is registered in the root `pom.xml` `<modules>`.

```
services/<name>/
├── pom.xml
├── Dockerfile
├── src/main/java/com/medikit/<name>/...
├── src/main/resources/application.yml
└── src/test/java/com/medikit/<name>/...
```

## pom.xml

- Parent: `com.medikit:medikit:1.0.0-SNAPSHOT` with `relativePath ../../pom.xml`.
- Must depend on `com.medikit:common` (version `${project.version}`).
- Must include `spring-cloud-starter-netflix-eureka-client`.
- Include `spring-boot-maven-plugin` (already managed by parent).
- Add DB (postgresql), redis, kafka, springdoc, test deps as needed.

## application.yml

- `server.port` unique per service (see port map below).
- `spring.application.name` matches module name.
- `eureka.client.service-url.defaultZone: ${EUREKA_SERVER:http://admin:admin@localhost:8761/eureka/}`
- Every env-sensitive value must use `${ENV_VAR:default}` syntax.
- `management.endpoints.web.exposure.include: health,info,metrics,prometheus`

## Port map

| Service              | Port |
|----------------------|------|
| api-gateway          | 8080 |
| discovery-server     | 8761 |
| config-server        | 8888 |
| user-service         | 8101 |
| product-service      | 8102 |
| inventory-service    | 8103 |
| cart-service         | 8104 |
| order-service        | 8105 |
| payment-service      | 8106 |
| delivery-service     | 8107 |
| notification-service | 8108 |
| prescription-service | 8109 |
| search-service       | 8110 |

## Package & naming

- Root package: `com.medikit.<name>`.
- Sub-packages: `controller`, `service`, `repository`, `entity`, `dto`, `config`, `security`, `consumer`.
- Use records for DTOs. Suffix DTOs with `Request` / `Response`.
- Controllers: `@RestController`, base path `/api/v1`.
- Services annotated `@Service`, transactional where appropriate.
- Use the shared exceptions from `com.medikit.common.web`:
  `NotFoundException`, `BadRequestException`, `ConflictException`, `ForbiddenException`.

## Events / Kafka

- Use `com.medikit.common.event.EventPublisher` (`publish(topic, key, payload)`).
- Topic names come from `com.medikit.common.event.Topics`.
- Events are sent as JSON strings.

## Redis

- Use `StringRedisTemplate`. Key prefix: `medikit:<domain>:...`.
- Use in caching, rate limiting, distributed locks, idempotency.

## Caching

- Prefer Redis-backed caching. Evict cache on mutations.

## Tests

- Unit tests with Mockito/JUnit 5 in `src/test/java`.
- Follow the existing test style in the repo.

## Dockerfile

Multi-stage build:

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY services/common/pom.xml services/common/pom.xml
COPY services/<name>/pom.xml services/<name>/pom.xml
RUN mvn -q dependency:go-offline -B
COPY . .
RUN mvn -q package -pl services/<name> -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/services/<name>/target/*.jar app.jar
EXPOSE <port>
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```
