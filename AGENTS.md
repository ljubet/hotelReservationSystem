# AGENTS.md

## Project snapshot
- Spring Boot app with a single entry point at `src/main/java/com/example/hotelreservationsystem/HotelReservationSystemApplication.java`.
- Gradle build with Java toolchain 25 and Spring Boot 4.0.6 in `build.gradle`.
- Resources live under `src/main/resources/` (only `application.properties` currently sets `spring.application.name`).

## Architecture and boundaries
- Current codebase is a minimal Spring Boot skeleton: no controllers, services, or JPA entities yet.
- The main package root is `com.example.hotelreservationsystem`; place new components under this package to get component scanning via `@SpringBootApplication`.

## Dependencies and integration points
- Web layer: `org.springframework.boot:spring-boot-starter-webmvc` (Spring MVC stack).
- Security: `org.springframework.boot:spring-boot-starter-security` (security filters will apply by default).
- Persistence: `org.springframework.boot:spring-boot-starter-data-jpa` with `com.h2database:h2` runtime.
- Developer tooling: `spring-boot-h2console` and Lombok (`lombok` compileOnly/annotationProcessor).

## Build and test workflow
- Gradle wrapper is present (`gradlew`, `gradlew.bat`); typical tasks include `test` and `bootRun`.
- Tests use JUnit 5 (`useJUnitPlatform()` in `build.gradle`) with a default `@SpringBootTest` in `src/test/java/com/example/hotelreservationsystem/HotelReservationSystemApplicationTests.java`.

## Conventions and patterns
- Use the package root `com.example.hotelreservationsystem` for components so auto-configuration and scanning apply.
- Keep config in `src/main/resources/application.properties`; only the application name is set today.
- If adding JPA entities/repositories, follow Spring Data JPA conventions under the same package root.

