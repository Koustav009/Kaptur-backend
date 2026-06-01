# 🤖 AI Agent Project Context Guide (Kaptur-backend)

Welcome, fellow AI Agent! This document is your **comprehensive source of truth** for the **Kaptur Backend** project. Before writing any code, read this guide to understand the project's architecture, conventions, and current state.

---

## 📌 Project Overview

**Kaptur Backend** is a Spring Boot REST API for a photo management application (Fotoowl Clone). It handles user authentication, event management, and photo uploads.

**Key Characteristics:**
- **Purpose:** Photo event management platform where users create events and manage photos
- **Target Audience:** Junior developers in learning phase (code must be verbose and educational)
- **Current State:** Core authentication and event CRUD implemented; file upload is placeholder

---

## 🛠️ Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Language** | Java | 21 (LTS) |
| **Framework** | Spring Boot | 4.0.5 |
| **Database** | PostgreSQL | - |
| **ORM** | Spring Data JPA / Hibernate | - |
| **Security** | Spring Security + JWT | jjwt 0.12.6 |
| **OAuth2** | Google Native Login (Token Verification) | google-api-client 2.7.2 |
| **API Docs** | SpringDoc OpenAPI / Swagger UI | 3.0.2 |
| **Build Tool** | Maven | Wrapper included |
| **Utilities** | Lombok | - |

---

## 📐 Architecture & Code Mandates

### Layered Architecture (Strict Separation)
```
controllers/ → services/ → repository/ → database
     ↑              ↑            ↑
   (Waiter)       (Chef)      (Pantry)
```

### Critical Rules
1. **DTO Usage:** NEVER expose JPA entities in controllers. Always use DTOs for request/response.
2. **Lombok Conventions:**
   - `@RequiredArgsConstructor` for constructor injection
   - `@Data` for DTOs
   - `@Getter`/`@Setter` for entities (avoid `@Data` to prevent circular reference issues)
3. **Validation:** Use Jakarta Validation annotations (`@NotBlank`, `@NotNull`, `@Valid`)
4. **Verbose Comments:** Every class and method must have educational comments explaining what and why
5. **Code Clarity:** Prioritize readable code over clever one-liners

---

## 📂 Project Structure

```
com.koustav.kaptur/
├── KapturApplication.java          # Entry point
├── controllers/                    # REST endpoints
│   ├── AuthController.java         # /auth/* endpoints
│   ├── EventController.java        # /events/* endpoints
│   ├── FileController.java         # /files/* (placeholder)
│   └── GlobalExceptionHandler.java # Global error handling
├── dto/                            # Data Transfer Objects
│   ├── AuthResponse.java           # JWT response
│   ├── EventRequest.java           # Event create/update request
│   ├── EventResponse.java          # Event response
│   ├── GoogleLoginRequest.java     # Google OAuth request
│   ├── LoginRequest.java           # Login request
│   └── RegisterRequest.java        # Registration request
├── model/                          # JPA Entities
│   ├── User.java                   # User entity
│   ├── Event.java                  # Event entity
│   ├── EventMembers.java           # Event membership (bridge table)
│   ├── EventMembersId.java         # Composite key for EventMembers
│   ├── EventPhotos.java            # Photo metadata entity
│   ├── CustomUserDetails.java      # Spring Security user adapter
│   ├── TusdHookRequest.java        # TUS upload hook request
│   ├── TusdHookResponse.java       # TUS upload hook response
│   └── enums/
│       ├── AuthProvider.java       # LOCAL, GOOGLE
│       ├── Role.java               # OWNER, MEMBER, GUEST
│       └── Status.java             # PENDING, ACCEPTED, REJECTED, BLOCKED
├── repository/                     # Data access layer
│   ├── UserRepository.java         # User queries
│   ├── EventRepository.java        # Event queries
│   ├── EventMembersRepository.java # Membership queries
│   └── EventPhotosRepository.java  # Photo queries
├── security/                       # Security layer
│   ├── SecurityConfig.java         # Main security configuration
│   ├── AuthTokenFilter.java        # JWT filter (runs on every request)
│   ├── JwtUtils.java               # JWT generation/validation
│   ├── CustomUserDetailsService.java # User lookup for auth
│   └── OAuth2AuthenticationSuccessHandler.java # (COMMENTED OUT - not used)
└── services/                       # Business logic
    ├── AuthService.java            # Authentication logic
    ├── EventService.java           # Event CRUD logic
    └── FileServices.java           # File handling (EMPTY - placeholder)
```

---

## 🔐 Authentication System

### How It Works

**JWT-Based Stateless Authentication:**
1. User logs in via `/auth/login` or `/auth/google`
2. Server validates credentials and returns JWT token
3. Client sends JWT in `Authorization: Bearer <token>` header
4. `AuthTokenFilter` validates token on every request
5. Security context is populated with user details

### Google Native Login Flow (Used for Flutter)
```
Flutter App → POST /auth/google (with Google ID Token)
    ↓
AuthService.googleLogin2()
    ↓
GoogleIdTokenVerifier verifies token with Google
    ↓
Extract email, name, picture, googleId from payload
    ↓
Find or create user in database
    ↓
Generate JWT with kptId as subject
    ↓
Return AuthResponse with JWT
```

### Public ID Formats
- **User kptId:** `KPT0000001` (KPT + 7-digit zero-padded ID)
- **Event evntid:** `EVNT0000001` (EVNT + 7-digit zero-padded ID)

### ✅ Authentication Flow (Verified Working)

The authentication system is **aligned and functional**. JWT stores `kptId`, and all components correctly use `kptId` for user lookup.

**Consistent Flow:**
```
JWT Generation (JwtUtils.buildJwt()) → Stores kptId (e.g., "KPT0000001")
    ↓
JWT Extraction (AuthTokenFilter) → Extracts kptId → sets as username in SecurityContext
    ↓
Controller Query (getCurrentUser()) → Queries findByKptId(kptId) ✅
```

**Code Reference (`EventController.getCurrentUser()`):**
```java
private User getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String kptId = authentication.getName(); // Returns kptId from JWT
    return userRepository.findByKptId(kptId) // ✅ Correctly queries by kptId
        .orElseThrow(() -> new RuntimeException("User not found with ID: " + kptId));
}
```

---

## 🗄️ Database Schema

### PostgreSQL Configuration
- **Database:** `kaptur`
- **Schema:** `kaptur_schema`
- **DDL Mode:** `spring.jpa.hibernate.ddl-auto=update` (auto-creates/updates tables)
- **Connection:** `jdbc:postgresql://localhost:5432/kaptur?currentSchema=kaptur_schema`

### Entity Relationships

```
User (1) ←─────────────── (N) Event
  │                              │
  │                              │
  └─────────── (N) EventMembers (N) ───────────┘
                    │
                    ├── role: OWNER, MEMBER, GUEST
                    └── status: PENDING, ACCEPTED, REJECTED, BLOCKED

Event (1) ←─────────────── (N) EventPhotos
                              │
                              └── uploadedBy → User
```

### Key Entities

**User**
- `id` (Long, PK, auto-increment)
- `kptId` (String, unique, format: KPT0000001)
- `email` (String, unique)
- `password` (String, null for Google users)
- `name` (String)
- `provider` (AuthProvider: LOCAL, GOOGLE)
- `providerId` (String, Google's unique ID)
- `imageUrl` (String, profile picture URL)

**Event**
- `id` (Long, PK, auto-increment)
- `evntid` (String, unique, format: EVNT0000001)
- `createdBy` (User, ManyToOne)
- `eventTitle` (String, required)
- `description` (String)
- `eventDate` (LocalDate, required)
- `eventLocation` (String)
- `isActive` (Boolean, default: true)
- `isDeleted` (Boolean, default: false) — soft delete flag
- `createdAt` (LocalDateTime, auto-generated)
- `updatedAt` (LocalDateTime, auto-updated)

**EventMembers** (Composite PK: EventMembersId)
- `event` (Event, FK)
- `user` (User, FK)
- `role` (Role: OWNER, MEMBER, GUEST)
- `status` (Status: PENDING, ACCEPTED, REJECTED, BLOCKED)
- `joinedAt` (LocalDateTime)

**EventPhotos**
- `id` (Long, PK, auto-increment)
- `photoId` (String, unique)
- `event` (Event, FK)
- `photoPath` (String, file storage path)
- `uploadedBy` (User, FK)
- `filename` (String)
- `fileType` (String)
- `fileSizeInKb` (Long)

---

## 🔌 API Endpoints

### Authentication (`/auth`)
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/auth/register` | Register new user | No |
| POST | `/auth/login` | Login with email/password | No |
| POST | `/auth/google` | Google native login | No |
| GET | `/auth/test` | Health check | No |

### Events (`/events`)
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/events` | Create event | Yes |
| GET | `/events` | Get user's events | Yes |
| GET | `/events/{id}` | Get event by ID | Yes |
| PUT | `/events/{id}` | Update event | Yes |
| DELETE | `/events/{id}` | Soft delete event | Yes |

### Files (`/files`)
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| - | `/files` | Placeholder (not implemented) | - |

### Public Paths (No Auth Required)
- `/auth/**`
- `/oauth2/**`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`
- `/tusd/**`

---

## 🔧 Configuration Reference

### application.properties
```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/kaptur?currentSchema=kaptur_schema
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.default_schema=kaptur_schema

# JWT
app.jwtSecret=9a4f2c8d3b7a1e5f8g2h6i0j4k9l3m7n5o1p4q8r2s6t0u4v8w2x6y0z
app.jwtExpirationMs=86400000  # 24 hours

# Google OAuth
google.client-id=73541242112-gedinu67qd0thfi5o2iflieh8gk38j2i.apps.googleusercontent.com
```

---

## 📋 Development Workflows

### Running the Application
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Mac/Linux
./mvnw spring-boot:run
```

### Building
```bash
.\mvnw.cmd clean package
```

### Testing
```bash
.\mvnw.cmd test
```

### Database
- Uses `ddl-auto=update` for automatic schema management
- Flyway is configured but migrations directory is empty
- Schema: `kaptur_schema` in PostgreSQL

---

## 🚨 Known Issues & Technical Debt

### 1. File Upload Not Implemented
- **Location:** `FileController.java`, `FileServices.java`
- **Status:** Placeholder only
- **Models Ready:** `TusdHookRequest` and `TusdHookResponse` exist for TUS protocol
- **Next Steps:** Implement file upload logic, integrate with storage (S3 or local)

### 2. OAuth2 Browser Flow Disabled
- **Location:** `SecurityConfig.java`, `OAuth2AuthenticationSuccessHandler.java`
- **Status:** Entirely commented out
- **Reason:** Using native Google login for Flutter instead of browser-based OAuth2

### 3. Missing Validation Annotations
- **Location:** `LoginRequest.java`, `RegisterRequest.java`, `GoogleLoginRequest.java`
- **Issue:** No `@NotBlank` or `@Email` validation
- **Impact:** Invalid data can reach service layer

### 4. No Role-Based Access Control
- **Location:** `EventController.java`
- **Issue:** No checks for MEMBER/GUEST roles on event operations
- **Current:** Only checks if user is the creator (OWNER)
- **Missing:** Invite system, role-based permissions

### 5. Test Coverage
- **Current:** Only `KapturApplicationTests.java` (context loads test)
- **Missing:** Unit tests for services, integration tests for controllers

---

## 🎯 Extension Patterns

When adding new features, follow this pattern:

### 1. Define Enums (if needed)
```java
// src/main/java/com/koustav/kaptur/model/enums/NewEnum.java
public enum NewEnum {
    VALUE1, VALUE2
}
```

### 2. Create Entity
```java
// src/main/java/com/koustav/kaptur/model/NewEntity.java
@Entity
@Table(name = "new_entities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // ... fields
}
```

### 3. Create Repository
```java
// src/main/java/com/koustav/kaptur/repository/NewEntityRepository.java
@Repository
public interface NewEntityRepository extends JpaRepository<NewEntity, Long> {
    // Custom query methods
}
```

### 4. Create DTOs
```java
// src/main/java/com/koustav/kaptur/dto/NewEntityRequest.java
@Data
public class NewEntityRequest {
    // Request fields with validation
}

// src/main/java/com/koustav/kaptur/dto/NewEntityResponse.java
@Data
@Builder
public class NewEntityResponse {
    // Response fields
}
```

### 5. Create Service
```java
// src/main/java/com/koustav/kaptur/services/NewEntityService.java
@Service
@RequiredArgsConstructor
public class NewEntityService {
    private final NewEntityRepository repository;
    
    @Transactional
    public NewEntityResponse create(NewEntityRequest request, User currentUser) {
        // Business logic
    }
}
```

### 6. Create Controller
```java
// src/main/java/com/koustav/kaptur/controllers/NewEntityController.java
@RestController
@RequestMapping("/new-entities")
@RequiredArgsConstructor
public class NewEntityController {
    private final NewEntityService service;
    
    @PostMapping
    public ResponseEntity<NewEntityResponse> create(@Valid @RequestBody NewEntityRequest request) {
        User currentUser = getCurrentUser();
        return new ResponseEntity<>(service.create(request, currentUser), HttpStatus.CREATED);
    }
    
    private User getCurrentUser() {
        // Get from SecurityContext
    }
}
```

---

## 💡 Best Practices for This Codebase

1. **Always use DTOs** - Never return entities directly
2. **Use @Transactional** - For methods that modify multiple entities
3. **Soft delete** - Use `isDeleted` flag instead of hard delete
4. **Public IDs** - Generate `kptId`/`evntid` after first save (need DB ID)
5. **Verbose comments** - Explain what and why, not just what
6. **Educational code** - Step-by-step logic, descriptive variables
7. **Error handling** - Throw RuntimeException with clear messages
8. **Security** - Always validate user ownership before modifications

---

## 🔍 Quick Reference: Common Tasks

### Get Current User
```java
private User getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String identifier = authentication.getName(); // This is kptId, NOT email!
    return userRepository.findByKptId(identifier)
        .orElseThrow(() -> new RuntimeException("User not found"));
}
```

### Generate Public ID
```java
// After saving entity to get auto-generated ID
String kptId = String.format("KPT%07d", user.getId());
String evntid = String.format("EVNT%07d", event.getId());
```

### Check Ownership
```java
if (!event.getCreatedBy().getId().equals(currentUser.getId())) {
    throw new RuntimeException("Not authorized");
}
```

---

## 📚 Related Documentation

- **GEMINI.md** - Gemini-specific mandates (similar content)
- **README.md** - Getting started guide for developers
- **Swagger UI** - http://localhost:8080/swagger-ui/index.html

---

**Last Updated:** 2026-06-01 (Authentication bug verified fixed)
**Scanned By:** AI Agent (Comprehensive Full-Project Scan)
