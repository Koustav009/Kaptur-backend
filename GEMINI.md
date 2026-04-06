# Project Overview: Fotoowl Clone Backend

This is a backend project for a Fotoowl clone, built with **Spring Boot** and **Java 21**. It follows a standard Maven project structure and provides a RESTful API for handling authentication and potentially other features related to photo management.

## Main Technologies
- **Framework:** Spring Boot 4.0.x
- **Language:** Java 21
- **Database:** MySQL (using Spring Data JPA / Hibernate)
- **Security:** Spring Security
- **Monitoring:** Spring Boot Actuator
- **Utilities:** Project Lombok (for boilerplate reduction)
- **Build Tool:** Maven (via `mvnw` wrapper)

## Architecture
The project follows a typical layered architecture for Spring Boot applications:
- **`com.fotoowl.clone.controllers`**: REST Controllers for handling HTTP requests.
- **`com.fotoowl.clone.services`**: Business logic layer.
- **`com.fotoowl.clone.repository`**: Data access layer (Spring Data JPA repositories).
- **`com.fotoowl.clone.model`**: Domain entities and data models.

## Building and Running

### Build the Project
To compile and package the project into a JAR file:
```powershell
.\mvnw.cmd clean install
```
*(On Unix-like systems, use `./mvnw` instead)*

### Run the Application
To start the Spring Boot application:
```powershell
.\mvnw.cmd spring-boot:run
```

### Run Tests
To execute the unit and integration tests:
```powershell
.\mvnw.cmd test
```

## Development Conventions
- **Naming:** Follow standard Java camelCase for variables/methods and PascalCase for classes.
- **Lombok:** Use Lombok annotations like `@Data`, `@RequiredArgsConstructor`, and `@Getter`/`@Setter` to reduce boilerplate.
- **Dependency Injection:** Prefer constructor injection (facilitated by Lombok's `@RequiredArgsConstructor`).
- **API Versioning:** Currently, APIs are prefixed with their specific functional area (e.g., `/auth`).
- **Configuration:** Database settings, server port, and JPA properties are managed in `src/main/resources/application.properties`.

## Database Configuration
The application is configured to connect to a local MySQL instance:
- **URL:** `jdbc:mysql://localhost:3306/fotoowl`
- **Username:** `root`
- **Password:** `12345`
- **Hibernate DDL Auto:** `update` (automatically manages schema changes)
