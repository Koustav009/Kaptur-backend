# Gemini Mandates: Kaptur Backend

This file serves as the foundational guide for the Gemini agent. These instructions take absolute precedence over general defaults.

## Project Context
A modern Spring Boot backend for a photo management application, emphasizing security, scalability, and ease of understanding for developers in a learning phase.

## Tech Stack & Specifications
- **Java Version:** 21 (LTS)
- **Spring Boot:** 4.0.5
- **Database:** PostgreSQL (local instance `Kaptur`)
- **Security:** Spring Security with JWT (jjwt 0.12.6) and OAuth2 (Google Login).
- **API Documentation:** SpringDoc / Swagger UI (accessible at `/swagger-ui/index.html`).
- **Build Tool:** Maven (utilize `mvnw.cmd` on Windows).

## Core Architectural Mandates
1. **Layered Architecture:** Strictly maintain the separation between `controllers`, `services`, `repository`, and `model`.
2. **DTO Usage:** Always use Data Transfer Objects (DTOs) for request/response payloads. Never expose raw JPA entities in controllers.
3. **Lombok Usage:** Use `@RequiredArgsConstructor` for constructor-based dependency injection. Use `@Data` for DTOs and `@Entity` with `@Getter`/`@Setter` for models.
4. **Validation:** Implement proper request validation using Spring's validation annotations where applicable.

## 🎓 Junior Developer Learning Mandate (CRITICAL)
The user is in a **learning phase**. Gemini MUST adhere to these stylistic rules:
- **Verbose Commenting:** Every class and non-trivial method must have comments explaining *what* it does and *why* it exists in simple, easy-to-understand terms.
- **Code Clarity:** Prioritize readable code over clever "one-liners." Use descriptive variable names.
- **Educational Explanations:** When suggesting changes or writing code, provide a brief "Why we do this" section in the response.

## Development Workflows
- **Database Changes:** Rely on `spring.jpa.hibernate.ddl-auto=update` for local development.
- **Testing:** Run tests using `.\mvnw.cmd test`. Always verify changes with unit tests in `src/test/java`.
- **Environment:** Respect settings in `src/main/resources/application.properties`. Do not hardcode secrets like JWT keys.

## API Standards
- **Prefixes:** Group related endpoints under common paths (e.g., `/auth/**`).
- **Responses:** Always return `ResponseEntity<?>` from controllers to ensure consistent HTTP status codes.
