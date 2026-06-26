# 🤖 AI Agent Project Context Guide (Kaptur-backend)

Welcome, fellow AI Agent! This document is your **comprehensive source of truth** for the **Kaptur Backend** project. Before writing any code, read this guide to understand the project's architecture, conventions, and current state.

---

## 📌 Project Overview

**Kaptur Backend** is a Spring Boot REST API for a photo management application (Kaptur Backend). It handles user authentication, event management, and photo uploads with resumable upload support via TUS protocol.

**Key Characteristics:**
- **Purpose:** Photo event management platform where users create events and manage photos
- **Target Audience:** Junior developers in learning phase (code must be verbose and educational)
- **Current State:** Authentication, event CRUD, and photo upload with resumable TUSd integration all implemented.

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
| **File Upload** | TUS Protocol (resumable) via TUSd Go Server | Docker, port 1080 |
| **Storage** | S3-compatible (configured in TUSd container) | - |
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
│   ├── FileController.java         # Photo upload lifecycle: init, list, delete + TUSd hooks
│   └── GlobalExceptionHandler.java # Global error handling
├── dto/                            # Data Transfer Objects
│   ├── AuthResponse.java           # JWT response
│   ├── EventRequest.java           # Event create/update request
│   ├── EventResponse.java          # Event response
│   ├── GoogleLoginRequest.java     # Google OAuth request
│   ├── LoginRequest.java           # Login request
│   ├── PhotoUploadRequest.java     # Init upload: filename, fileType, fileSizeInKb
│   ├── PhotoUploadResponse.java    # Init response: photoId (UUID), tusdUploadUrl
│   ├── PhotoResponse.java          # Photo listing: metadata + downloadUrl
│   └── RegisterRequest.java        # Registration request
├── model/                          # JPA Entities
│   ├── User.java                   # User entity
│   ├── Event.java                  # Event entity
│   ├── EventMembers.java           # Event membership (bridge table)
│   ├── EventMembersId.java         # Composite key for EventMembers
│   ├── EventPhotos.java            # Photo metadata entity (lifecycle tracked)
│   ├── CustomUserDetails.java      # Spring Security user adapter
│   ├── TusdHookRequest.java        # TUS upload hook request (from TUSd server)
│   ├── TusdHookResponse.java       # TUS hook response (with ChangeFileInfo for ID override)
│   └── enums/
│       ├── AuthProvider.java       # LOCAL, GOOGLE
│       ├── PhotoStatus.java        # PENDING, UPLOADING, COMPLETED, FAILED
│       ├── Role.java               # OWNER, MEMBER, GUEST
│       └── Status.java             # PENDING, ACCEPTED, REJECTED, BLOCKED
├── repository/                     # Data access layer
│   ├── UserRepository.java         # User queries
│   ├── EventRepository.java        # Event queries
│   ├── EventMembersRepository.java # Membership queries
│   └── EventPhotosRepository.java  # Photo queries (by photoId, by eventId non-deleted)
├── security/                       # Security layer
│   ├── SecurityConfig.java         # Main security configuration
│   ├── AuthTokenFilter.java        # JWT filter (runs on every request)
│   ├── JwtUtils.java               # JWT generation/validation
│   ├── CustomUserDetailsService.java # User lookup for auth
│   └── OAuth2AuthenticationSuccessHandler.java # (COMMENTED OUT - not used)
└── services/                       # Business logic
    ├── AuthService.java            # Authentication logic
    ├── EventService.java           # Event CRUD logic
    └── FileServices.java           # Photo upload lifecycle, TUSd hook handling
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
- **Photo photoId:** UUID v4 string (e.g. `550e8400-e29b-41d4-a716-446655440000`)

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
  │                              ├────────── (N) EventPhotos
  │                              │               │
  │                              │               └── uploadedBy → User
  │                              │
  └─────────── (N) EventMembers (N) ───────────┘
                    │
                    ├── role: OWNER, MEMBER, GUEST
                    └── status: PENDING, ACCEPTED, REJECTED, BLOCKED
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
- `photoId` (String, unique, UUID v4)
- `event` (Event, FK)
- `uploadedBy` (User, FK)
- `filename` (String) — original filename from metadata
- `fileType` (String) — MIME type from metadata
- `fileSizeInKb` (Long)
- `photoPath` (String) — S3 object key, set by post-finish hook
- `photoStatus` (PhotoStatus: PENDING → UPLOADING → COMPLETED / FAILED)
- `isDeleted` (Boolean, default: false) — soft delete flag
- `createdAt` (LocalDateTime, auto-generated)
- `uploadCompletedAt` (LocalDateTime, set on post-finish)

---

## 🔌 Complete API Reference

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
| GET | `/events/{id}` | Get event by ID (`id` is internal Long) | Yes |
| PUT | `/events/{id}` | Update event | Yes |
| DELETE | `/events/{id}` | Soft delete event | Yes |

### Photos (`/events/{eventId}/photos`)
**`eventId` in all photo endpoints is the public `evntid` string (e.g. `EVNT0000001`), NOT the internal Long ID.**

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/events/{eventId}/photos/init` | Initiate photo upload | Yes |
| GET | `/events/{eventId}/photos` | List event photos | Yes |
| DELETE | `/events/{eventId}/photos/{photoId}` | Soft delete a photo | Yes |

### TUSd Hooks (called by TUSd Go server)
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/tusd/hooks` | TUSd lifecycle hooks | No |

### Public Paths (No Auth Required)
- `/auth/**`
- `/oauth2/**`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`
- `/tusd/**`

---

## 📤 Photo Upload — Complete Flow & Client Integration Guide

### Architecture Overview

The Spring Boot server does **NOT** handle file bytes. A separate [TUSd Go server](https://github.com/tus/tusd) (running at `localhost:1080` in Docker) manages all file I/O and stores files directly to S3. Spring Boot only manages **photo metadata** and the **upload lifecycle** via TUSd hooks.

```
                      ┌─────────────────┐
                      │  Flutter Client  │
                      └───┬──────┬──────┘
                          │      │
              ┌───────────┘      └──────────────┐
              ▼                                 ▼
   ┌────────────────────┐           ┌────────────────────┐
   │  Spring Boot :8080 │           │  TUSd Go :1080     │
   │  (metadata mgmt)   │◀──hooks──│  (file I/O → S3)   │
   └────────────────────┘           └────────────────────┘
```

### Upload Sequence (Client Must Implement)

```
STEP 1: Initialize Upload
─────────────────────────
POST /events/{evntId}/photos/init
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "filename": "vacation.jpg",
  "fileType": "image/jpeg",
  "fileSizeInKb": 2048
}

→ 201 Created
{
  "photoId": "550e8400-e29b-41d4-a716-446655440000",
  "tusdUploadUrl": "http://localhost:1080/files/"
}


STEP 2: Create TUS Upload on TUSd
──────────────────────────────────
POST http://localhost:1080/files/
Upload-Length: 2097152
Upload-Metadata: photoId NTUwZTg0MDAtZTI5Yi00MWQ0LWE3MTYtNDQ2NjU1NDQwMDAw,filename dmFjYXRpb24uanBn,filetype aW1hZ2UvanBlZw==
Tus-Resumable: 1.0.0

→ 201 Created
Location: http://localhost:1080/files/550e8400-e29b-41d4-a716-446655440000

IMPORTANT: photoId value in Upload-Metadata must be Base64-encoded.
  Example: echo -n "550e8400-e29b-41d4-a716-446655440000" | base64
  Result:  NTUwZTg0MDAtZTI5Yi00MWQ0LWE3MTYtNDQ2NjU1NDQwMDAw

Behind the scenes: TUSd calls POST /tusd/hooks (pre-create).
Spring Boot validates the photoId exists in PENDING status,
and overrides the upload ID to match our UUID photoId.
Status transitions: PENDING → UPLOADING.


STEP 3: Upload File Chunks (Resumable)
───────────────────────────────────────
PATCH http://localhost:1080/files/550e8400-e29b-41d4-a716-446655440000
Upload-Offset: 0
Content-Type: application/offset+octet-stream
Content-Length: <chunk size>

<binary data>

→ 204 No Content (continue with next chunk)
→ OR → 200 OK (upload complete)


STEP 4: Resuming an Interrupted Upload
───────────────────────────────────────
HEAD http://localhost:1080/files/550e8400-e29b-41d4-a716-446655440000
Tus-Resumable: 1.0.0

→ 200 OK
Upload-Offset: 1048576
Upload-Length: 2097152

Resume PATCH from offset 1048576.


STEP 5: On Completion (automatic)
──────────────────────────────────
TUSd calls POST /tusd/hooks (post-finish).
Spring Boot marks photo as COMPLETED and stores the S3 key.
No client action needed for this step.


STEP 6: List Event Photos
──────────────────────────
GET /events/{evntId}/photos
Authorization: Bearer <jwt>

→ 200 OK
[
  {
    "id": 1,
    "photoId": "550e8400-e29b-41d4-a716-446655440000",
    "filename": "vacation.jpg",
    "fileType": "image/jpeg",
    "fileSizeInKb": 2048,
    "photoPath": "a1b2c3d4e5f6...",
    "photoStatus": "COMPLETED",
    "uploadedByKptId": "KPT0000001",
    "uploadedByName": "John Doe",
    "createdAt": "2026-06-02T10:30:00",
    "uploadCompletedAt": "2026-06-02T10:35:00",
    "downloadUrl": "http://localhost:1080/files/550e8400-e29b-41d4-a716-446655440000"
  }
]

To display/download the file: GET {downloadUrl}
TUSd streams it directly from S3.


STEP 7: Delete a Photo
───────────────────────
DELETE /events/{evntId}/photos/{photoId}
Authorization: Bearer <jwt>

→ 200 OK
{ "message": "Photo deleted successfully" }

Permission: Only the photo uploader or the event creator can delete.
```

### PhotoUploadRequest DTO

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `filename` | String | `@NotBlank` | Original filename (e.g. "vacation.jpg") |
| `fileType` | String | `@NotBlank` | MIME type (e.g. "image/jpeg", "video/mp4") |
| `fileSizeInKb` | Long | `@NotNull` | File size in kilobytes |

### PhotoUploadResponse DTO

| Field | Type | Description |
|-------|------|-------------|
| `photoId` | String | UUID v4 assigned by server — also the TUSd upload ID |
| `tusdUploadUrl` | String | TUSd base URL (e.g. `http://localhost:1080/files/`) |

### PhotoResponse DTO

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Internal DB ID |
| `photoId` | String | UUID v4 public identifier |
| `filename` | String | Original filename |
| `fileType` | String | MIME type |
| `fileSizeInKb` | Long | File size in KB |
| `photoPath` | String | S3 object key (null until upload completes) |
| `photoStatus` | String | `PENDING`, `UPLOADING`, `COMPLETED`, or `FAILED` |
| `uploadedByKptId` | String | `KPT0000001` of the uploader |
| `uploadedByName` | String | Display name of the uploader |
| `createdAt` | ISO DateTime | When the photo record was created |
| `uploadCompletedAt` | ISO DateTime | When the upload finished (null if not COMPLETED) |
| `downloadUrl` | String | `http://localhost:1080/files/{photoId}` for streaming |

### PhotoStatus Lifecycle

```
PENDING ──(pre-create hook passes)──▶ UPLOADING ──(post-finish hook)──▶ COMPLETED
   │                                      │
   └──(pre-create hook fails)─────────────┴──(post-terminate hook)─────▶ FAILED
```

### Error Responses

All endpoints return consistent error JSON:
```json
{
  "error": "Human-readable error message"
}
```

Common errors:
| Status | Condition |
|--------|-----------|
| 400 | Validation failure, bad request |
| 401 | Missing or expired JWT |
| 403 | Not a member of the event / not authorized |
| 404 | Event or photo not found |
| 409 | Photo already being uploaded (status not PENDING) |

### Key Design Decisions for Client Developers

1. **`photoId` is a UUID** — generated server-side, used as the TUSd upload ID. Always the same for the same photo across all operations.
2. **`eventId` in photo endpoints is `evntid`** — the public string like `EVNT0000001`, NOT the internal numeric ID.
3. **`tusdUploadUrl`** is just the base URL. Client appends nothing — the TUS protocol's `POST` to the base URL creates the upload resource.
4. **`Upload-Metadata`** values MUST be Base64-encoded strings in the TUS protocol.
5. **Resumability** is fully handled by TUSd. Store the upload URL (`tusdUploadUrl + "/" + photoId`) and use `HEAD` to get the current offset.
6. **Event membership required** — user must be an ACCEPTED member of the event to upload or view photos.
7. **Files served via TUSd** — download URL points to TUSd, which streams from S3. No additional auth on TUSd download (files are public by URL).
8. **Photo/video support** — fileType MIME string determines media type. No separate enum.

### Full TUS Upload Example (curl)

```bash
# Step 1: Init via Spring Boot
curl -X POST http://localhost:8080/events/EVNT0000001/photos/init \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"filename":"photo.jpg","fileType":"image/jpeg","fileSizeInKb":2048}'

# Response: {"photoId":"abc-123-...","tusdUploadUrl":"http://localhost:1080/files/"}

# Step 2: Create upload on TUSd
curl -X POST http://localhost:1080/files/ \
  -H "Upload-Length: 2097152" \
  -H "Upload-Metadata: photoId $(echo -n 'abc-123-...' | base64)" \
  -H "Tus-Resumable: 1.0.0"

# Step 3: Upload chunks
curl -X PATCH http://localhost:1080/files/abc-123-... \
  -H "Upload-Offset: 0" \
  -H "Content-Type: application/offset+octet-stream" \
  --data-binary @photo.jpg
```

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

# TUSd Server (resumable file uploads, stores to S3)
tusd.base-url=http://localhost:1080/files
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

### 1. OAuth2 Browser Flow Disabled
- **Location:** `SecurityConfig.java`, `OAuth2AuthenticationSuccessHandler.java`
- **Status:** Entirely commented out
- **Reason:** Using native Google login for Flutter instead of browser-based OAuth2

### 2. Missing Validation Annotations
- **Location:** `LoginRequest.java`, `RegisterRequest.java`, `GoogleLoginRequest.java`
- **Issue:** No `@NotBlank` or `@Email` validation
- **Impact:** Invalid data can reach service layer

### 3. No Role-Based Access Control on Events
- **Location:** `EventController.java`
- **Issue:** No checks for MEMBER/GUEST roles on event operations
- **Current:** Only checks if user is the creator (OWNER)
- **Missing:** Invite system, role-based permissions

### 4. Test Coverage
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
4. **Public IDs** - Use `KPT0000001` for users, `EVNT0000001` for events, UUID for photos
5. **Verbose comments** - Explain what and why, not just what
6. **Educational code** - Step-by-step logic, descriptive variables
7. **Error handling** - Throw RuntimeException with clear messages
8. **Security** - Always validate user ownership/membership before modifications

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
// Photos use UUID (generated BEFORE save, no double-write needed)
String photoId = UUID.randomUUID().toString();
```

### Check Ownership
```java
if (!event.getCreatedBy().getId().equals(currentUser.getId())) {
    throw new RuntimeException("Not authorized");
}
```

### Validate Event Membership
```java
List<EventMembers> memberships = eventMembersRepository.findByEventId(eventId);
boolean isMember = memberships.stream()
    .anyMatch(m -> m.getUser().getId().equals(userId) && m.getStatus() == Status.ACCEPTED);
```

---

## 📚 Related Documentation

- **README.md** - Getting started guide for developers
- **Swagger UI** - http://localhost:8080/swagger-ui/index.html
- **TUS Protocol** - https://tus.io/protocols/resumable-upload
- **TUSd** - https://github.com/tus/tusd

---

**Last Updated:** 2026-06-02 (Photo upload with TUSd resumable support implemented)
**Scanned By:** AI Agent (Comprehensive Full-Project Scan)