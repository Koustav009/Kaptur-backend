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
| **Utilities** | Lombok, UUID Creator (f4b6a3) | - |

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
3. **Validation:** Use Jakarta Validation annotations (`@NotBlank`, `@NotNull`, `@Email`, `@Valid`)
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
│   ├── AuthResponse.java           # JWT + safe user profile (no entity leak)
│   ├── EventRequest.java           # Event create/update request
│   ├── EventResponse.java          # Event response (UUID-based)
│   ├── GoogleLoginRequest.java     # Google OAuth request
│   ├── LoginRequest.java           # Login request (with validation)
│   ├── PhotoUploadRequest.java     # Init upload: filename, fileType, fileSizeInKb
│   ├── PhotoUploadResponse.java    # Init response: photoId (UUID), tusdUploadUrl
│   ├── PhotoResponse.java          # Photo listing: photoId (UUID), metadata + downloadUrl
│   └── RegisterRequest.java        # Registration request (with validation)
├── model/                          # JPA Entities
│   ├── User.java                   # User entity (UUID kptId PK)
│   ├── Event.java                  # Event entity (UUID evntId PK)
│   ├── EventMembersDtl.java        # Event membership (UUID memberDtlId PK)
│   ├── EventMembersHistory.java    # Membership history/archive (soft-deleted records)
│   ├── EventPhotos.java            # Photo metadata entity (UUID photoId PK)
│   ├── UserRoleMst.java            # Role master data (UUID roleId PK)
│   ├── CustomUserDetails.java      # Spring Security user adapter
│   ├── TusdHookRequest.java        # TUS upload hook request (from TUSd server)
│   ├── TusdHookResponse.java       # TUS hook response (with ChangeFileInfo for ID override)
│   └── enums/
│       ├── AuthProvider.java       # LOCAL, GOOGLE
│       ├── PhotoStatus.java        # PENDING, UPLOADING, COMPLETED, FAILED
│       ├── Role.java               # SUPER_ADMIN, USER, ADMIN, PHOTOMAN, GUEST
│       └── Status.java             # PENDING, ACCEPTED, REJECTED, BLOCKED
├── repository/                     # Data access layer
│   ├── UserRepository.java         # User queries (UUID-based)
│   ├── EventRepository.java        # Event queries (UUID-based)
│   ├── EventMembersRepository.java # Membership queries (UUID-based)
│   ├── EventPhotosRepository.java  # Photo queries (UUID-based)
│   └── UserRoleMstRepository.java  # Role master queries
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
2. Server validates credentials and returns JWT token + safe user profile (no password!)
3. Client sends JWT in `Authorization: Bearer <token>` header
4. `AuthTokenFilter` validates token on every request — extracts kptId UUID string from subject claim
5. Security context is populated with the kptId as the principal name
6. Controllers call `getCurrentUser()` which parses the kptId string → UUID, then loads the full User

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
Generate JWT with kptId.toString() as subject
    ↓
Return AuthResponse with JWT + safe user fields (kptId, email, name, imageUrl)
```

### Public ID Formats — All UUIDs
- **User kptId:** UUID v7 (time-ordered), e.g. `0192f3a4-5678-9abc-def0-123456789abc`
- **Event evntId:** UUID v7 (time-ordered), e.g. `0192f3b5-6789-abcd-ef01-234567890bcd`
- **Photo photoId:** UUID v4, e.g. `550e8400-e29b-41d4-a716-446655440000`
- **Role roleId:** UUID, auto-generated by Hibernate
- **Membership memberDtlId:** UUID, auto-generated by Hibernate

> **Breaking change from v1:** Old format used `KPT0000001` / `EVNT0000001` string prefixes. Now all IDs are standard UUIDs. UUID v7 (time-ordered) is used for entities that benefit from B-tree index locality (User, Event). UUID v4 is used where randomness is preferred (photos).

### ✅ Authentication Flow

```
JWT Generation (JwtUtils.buildJwt()) → Stores kptId.toString() as subject claim
    ↓
JWT Extraction (AuthTokenFilter) → Extracts subject → sets as principal name in SecurityContext
    ↓
Controller (getCurrentUser()) → UUID.fromString(authentication.getName()) → findByKptId(uuid)
```

**Current `getCurrentUser()` pattern:**
```java
private User getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String kptIdStr = authentication.getName(); // Returns kptId UUID string from JWT
    UUID kptId = UUID.fromString(kptIdStr);
    return userRepository.findByKptId(kptId)
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
  │                              ├────────── (N) EventMembersDtl
  │                              │               │
  │                              │               └── roleMst → UserRoleMst
  │                              │
  │                              └────────── (N) EventMembersHistory
  │                                              │
  │                                              └── roleMst → UserRoleMst
  │
  └─────────── (N) EventMembersDtl (N) ───────────┘
  └─────────── (N) EventMembersHistory (N) ───────┘

UserRoleMst (1) ── (N) EventMembersDtl
UserRoleMst (1) ── (N) EventMembersHistory
```

### Key Entities

**User** — Table: `USERS`
| Field | Type | Notes |
|-------|------|-------|
| `kptId` | UUID (PK) | UUIDv7 time-ordered, assigned at creation |
| `email` | String | Unique, not null |
| `password` | String | Null for Google OAuth users |
| `name` | String | Not null |
| `provider` | AuthProvider enum | LOCAL or GOOGLE |
| `providerId` | String | Google's unique `sub` claim |
| `imageUrl` | String | Profile picture URL |

**Event** — Table: `EVENTS`
| Field | Type | Notes |
|-------|------|-------|
| `evntId` | UUID (PK) | UUIDv7 time-ordered |
| `createdBy` | User (ManyToOne) | FK → USERS, LAZY fetch |
| `eventTitle` | String | Not null |
| `description` | String | Nullable |
| `eventDate` | LocalDate | Nullable |
| `eventLocation` | String | Nullable |
| `isActive` | Boolean | Default true |
| `isDeleted` | Boolean | Soft delete flag, default false |
| `createdAt` | LocalDateTime | Auto-set via @CreationTimestamp |
| `updatedAt` | LocalDateTime | Auto-set via @UpdateTimestamp |

**EventMembersDtl** — Table: `EVENT_MEMBERS_DTL` (unique constraint on event_id + user_id)
| Field | Type | Notes |
|-------|------|-------|
| `memberDtlId` | UUID (PK) | Auto-generated |
| `event` | Event (ManyToOne) | FK → EVENTS, LAZY |
| `user` | User (ManyToOne) | FK → USERS, LAZY |
| `roleMst` | UserRoleMst (ManyToOne) | FK → USER_ROLE_MST, LAZY |
| `joinedAt` | LocalDateTime | Auto-set to now() if null |
| `assignedBy` | String | Nullable, max 50 chars |
| `remarks` | String | Nullable, max 255 chars |
| `createdAt` | LocalDateTime | Set via @PrePersist |
| `updatedAt` | LocalDateTime | Set via @PrePersist + @PreUpdate |

> **Important:** A record in EventMembersDtl = accepted member. There is no separate invitation/status field. The `Status` enum exists but is not currently used.

**EventMembersHistory** — Table: `EVENT_MEMBERS_HISTORY`
| Field | Type | Notes |
|-------|------|-------|
| `historyId` | UUID (PK) | Auto-generated |
| `memberDtlId` | UUID | References the original EventMembersDtl record ID (no FK constraint) |
| `event` | Event (ManyToOne) | FK → EVENTS, LAZY |
| `user` | User (ManyToOne) | FK → USERS, LAZY |
| `roleMst` | UserRoleMst (ManyToOne) | FK → USER_ROLE_MST, LAZY |
| `isDeleted` | Boolean | Default true |
| `deletedAt` | LocalDateTime | Set via @PrePersist |
| Other fields | — | Mirrors EventMembersDtl structure |

**EventPhotos** — Table: `EVENT_PHOTOS`
| Field | Type | Notes |
|-------|------|-------|
| `photoId` | UUID (PK) | UUIDv4, also used as TUSd upload ID |
| `event` | Event (ManyToOne) | FK → EVENTS, LAZY |
| `uploadedBy` | User (ManyToOne) | FK → USERS, LAZY |
| `filename` | String | Not null |
| `fileType` | String | MIME type, not null |
| `fileSizeInKb` | Long | Not null |
| `photoPath` | String | S3 object key, set by post-finish hook |
| `photoStatus` | PhotoStatus enum | PENDING → UPLOADING → COMPLETED / FAILED |
| `isDeleted` | Boolean | Soft delete flag, default false |
| `createdAt` | LocalDateTime | Auto-set via @CreationTimestamp |
| `uploadCompletedAt` | LocalDateTime | Set on post-finish hook |

**UserRoleMst** — Table: `USER_ROLE_MST`
| Field | Type | Notes |
|-------|------|-------|
| `roleId` | UUID (PK) | Auto-generated |
| `roleCode` | Role enum | Unique, e.g. SUPER_ADMIN, USER, ADMIN, PHOTOMAN, GUEST |
| `roleName` | String | Human-readable, e.g. "Administrator" |
| `description` | String | Nullable |
| `isSystemRole` | Boolean | System roles cannot be deleted |
| `isActive` | Boolean | Default true |
| `createdAt` | LocalDateTime | Set via @PrePersist |
| `updatedAt` | LocalDateTime | Set via @PrePersist + @PreUpdate |
| `createdBy` | String | Nullable |
| `updatedBy` | String | Nullable |

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
| GET | `/events/{id}` | Get event by UUID | Yes |
| PUT | `/events/{id}` | Update event | Yes |
| DELETE | `/events/{id}` | Soft delete event | Yes |

### Photos (`/events/{eventId}/photos`)
**`eventId` in all photo endpoints is the UUID string (e.g. `550e8400-e29b-41d4-a716-446655440000`).**

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
    "photoId": "550e8400-e29b-41d4-a716-446655440000",
    "filename": "vacation.jpg",
    "fileType": "image/jpeg",
    "fileSizeInKb": 2048,
    "photoPath": "a1b2c3d4e5f6...",
    "photoStatus": "COMPLETED",
    "uploadedByKptId": "0192f3a4-5678-9abc-def0-123456789abc",
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
| `photoId` | UUID | UUID v4 assigned by server — also the TUSd upload ID |
| `tusdUploadUrl` | String | TUSd base URL (e.g. `http://localhost:1080/files/`) |

### PhotoResponse DTO

| Field | Type | Description |
|-------|------|-------------|
| `photoId` | UUID | UUID v4 public identifier |
| `filename` | String | Original filename |
| `fileType` | String | MIME type |
| `fileSizeInKb` | Long | File size in KB |
| `photoPath` | String | S3 object key (null until upload completes) |
| `photoStatus` | String | `PENDING`, `UPLOADING`, `COMPLETED`, or `FAILED` |
| `uploadedByKptId` | UUID | kptId of the uploader |
| `uploadedByName` | String | Display name of the uploader |
| `createdAt` | ISO DateTime | When the photo record was created |
| `uploadCompletedAt` | ISO DateTime | When the upload finished (null if not COMPLETED) |
| `downloadUrl` | String | `http://localhost:1080/files/{photoId}` for streaming |

### AuthResponse DTO

| Field | Type | Description |
|-------|------|-------------|
| `accessToken` | String | JWT token |
| `tokenType` | String | "Bearer" (default) |
| `kptId` | UUID | User's public ID |
| `email` | String | User's email |
| `name` | String | User's display name |
| `imageUrl` | String | Profile picture URL (null for local users) |

### EventResponse DTO

| Field | Type | Description |
|-------|------|-------------|
| `evntId` | UUID | Event's public ID |
| `eventTitle` | String | Event title |
| `description` | String | Event description |
| `eventDate` | LocalDate | Event date |
| `eventLocation` | String | Event location |
| `creatorId` | UUID | Creator's kptId |
| `creatorName` | String | Creator's display name |
| `createdAt` | ISO DateTime | Creation timestamp |
| `updatedAt` | ISO DateTime | Last update timestamp |

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

1. **All IDs are UUIDs** — No more `KPT0000001`-style strings. All identifiers are standard UUIDs sent as strings in JSON.
2. **`photoId` is a UUID** — generated server-side, used as the TUSd upload ID. Always the same for the same photo across all operations.
3. **`eventId` in photo endpoints is a UUID string** — the event's UUID, NOT an old-style string ID.
4. **`tusdUploadUrl`** is just the base URL. Client appends nothing — the TUS protocol's `POST` to the base URL creates the upload resource.
5. **`Upload-Metadata`** values MUST be Base64-encoded strings in the TUS protocol.
6. **Resumability** is fully handled by TUSd. Store the upload URL (`tusdUploadUrl + "/" + photoId`) and use `HEAD` to get the current offset.
7. **Event membership is binary** — existence in EventMembersDtl = accepted member. No pending/rejected states.
8. **Files served via TUSd** — download URL points to TUSd, which streams from S3.
9. **Photo/video support** — fileType MIME string determines media type. No separate enum.

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
- **Important:** The `USER_ROLE_MST` table must be seeded with role data before creating events. Run a SQL seed script or insert manually:

```sql
INSERT INTO kaptur_schema.USER_ROLE_MST (role_id, role_code, role_name, description, is_system_role, is_active, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'SUPER_ADMIN', 'Super Administrator', 'Full system access', true, true, now(), now()),
  (gen_random_uuid(), 'USER', 'User', 'Standard user', true, true, now(), now()),
  (gen_random_uuid(), 'ADMIN', 'Administrator', 'Event administrator', true, true, now(), now()),
  (gen_random_uuid(), 'PHOTOMAN', 'Photographer', 'Photo contributor', true, true, now(), now()),
  (gen_random_uuid(), 'GUEST', 'Guest', 'Read-only access', true, true, now(), now());
```

---

## 🚨 Known Issues & Technical Debt

### 1. OAuth2 Browser Flow Disabled
- **Location:** `SecurityConfig.java`, `OAuth2AuthenticationSuccessHandler.java`
- **Status:** Entirely commented out
- **Reason:** Using native Google login for Flutter instead of browser-based OAuth2

### 2. AuthTokenFilter Uses Stub Principal
- **Location:** `AuthTokenFilter.java`
- **Issue:** Creates a bare Spring Security `User` object instead of loading `CustomUserDetails` from DB. Controllers work around this by calling `userRepository.findByKptId()` directly in `getCurrentUser()`.
- **Impact:** Role-based access control (`@PreAuthorize`) won't work since authorities are always empty.

### 3. No Role-Based Access Control on Events
- **Location:** `EventController.java`, `EventService.java`
- **Issue:** Only checks if user is the creator. Does not use the `roleMst` relationship to grant MEMBER-level permissions.
- **Missing:** Invite system, role-based permissions for members.

### 4. Test Coverage
- **Current:** Only `KapturApplicationTests.java` (context loads test)
- **Missing:** Unit tests for services, integration tests for controllers

### 5. Role Seed Data Required
- The `USER_ROLE_MST` table must be manually seeded before the app can create events. No auto-seeding mechanism exists yet.

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

### 2. Create Entity (UUID PK)
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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    // ... fields
}
```

### 3. Create Repository (UUID type parameter)
```java
// src/main/java/com/koustav/kaptur/repository/NewEntityRepository.java
@Repository
public interface NewEntityRepository extends JpaRepository<NewEntity, UUID> {
    // Custom query methods with UUID params
}
```

### 4. Create DTOs
```java
// src/main/java/com/koustav/kaptur/dto/NewEntityRequest.java
@Data
public class NewEntityRequest {
    @NotBlank(message = "Field is required")
    private String field;
}

// src/main/java/com/koustav/kaptur/dto/NewEntityResponse.java
@Data
@Builder
public class NewEntityResponse {
    private UUID id;
    private String field;
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
    private final UserRepository userRepository;
    
    @PostMapping
    public ResponseEntity<NewEntityResponse> create(@Valid @RequestBody NewEntityRequest request) {
        User currentUser = getCurrentUser();
        return new ResponseEntity<>(service.create(request, currentUser), HttpStatus.CREATED);
    }
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID kptId = UUID.fromString(authentication.getName());
        return userRepository.findByKptId(kptId)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
```

---

## 💡 Best Practices for This Codebase

1. **Always use DTOs** - Never return entities directly. `AuthResponse` uses safe fields (kptId, email, name, imageUrl), not the full User entity.
2. **Use @Transactional** - For methods that modify multiple entities
3. **Soft delete** - Use `isDeleted` flag instead of hard delete
4. **UUID PKs** - All entities use UUID primary keys. Use `UuidCreator.getTimeOrderedEpoch()` for UUIDv7 where B-tree performance matters.
5. **Verbose comments** - Explain what and why, not just what
6. **Educational code** - Step-by-step logic, descriptive variables
7. **Error handling** - Throw RuntimeException with clear messages
8. **Security** - Always validate user ownership/membership before modifications
9. **LAZY fetching** - All `@ManyToOne` relationships use `FetchType.LAZY`. Exclude from `@ToString` and `@EqualsAndHashCode`.
10. **No composite keys** - All entities use single UUID PKs. Unique constraints are defined via `@UniqueConstraint`.

---

## 🔍 Quick Reference: Common Tasks

### Get Current User
```java
private User getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String kptIdStr = authentication.getName(); // UUID string from JWT subject
    UUID kptId = UUID.fromString(kptIdStr);
    return userRepository.findByKptId(kptId)
        .orElseThrow(() -> new RuntimeException("User not found with ID: " + kptIdStr));
}
```

### Generate UUID PK
```java
// For UUIDv7 (time-ordered, index-friendly):
import com.github.f4b6a3.uuid.UuidCreator;
UUID uuid = UuidCreator.getTimeOrderedEpoch();

// For UUIDv4 (random):
UUID uuid = UUID.randomUUID();

// Set on entity before save:
entity.setEvntId(UuidCreator.getTimeOrderedEpoch());
repository.save(entity);
```

### Check Ownership (UUID comparison)
```java
if (!event.getCreatedBy().getKptId().equals(currentUser.getKptId())) {
    throw new RuntimeException("Not authorized");
}
```

### Validate Event Membership (existence = accepted)
```java
List<EventMembersDtl> memberships = eventMembersRepository.findByEventId(eventUuid);
boolean isMember = memberships.stream()
    .anyMatch(m -> m.getUser().getKptId().equals(userUuid));
if (!isMember) {
    throw new RuntimeException("You must be a member of this event");
}
```

### Look Up Role from Master Table
```java
UserRoleMst adminRole = userRoleMstRepository.findByRoleCode(Role.ADMIN)
    .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
```

---

**Last Updated:** 2026-07-04 (UUID migration complete — all entities use UUID PKs, DTOs aligned, role-based membership with UserRoleMst)
**Scanned By:** AI Agent (Full Project Alignment Pass)
