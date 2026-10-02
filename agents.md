# 🤖 AI Agent Project Context Guide (Kaptur-backend)

Spring Boot REST API for a photo/event management app. This is the source of truth for the project's architecture, conventions, and current state — read it before writing code. (Code examples are intentionally omitted; see the source and `bruno/Kaptur/` for concrete patterns.)

---

## 📌 Project Overview
- **Purpose:** photo event management platform — users create events and manage photos.
- **Audience:** junior developers in a learning phase — code must be verbose and educational.
- **State:** auth, event CRUD, and resumable photo upload (TUSd) all implemented.

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
**Strict layered separation:** `controllers/ → services/ → repository/ → database` (never skip layers).

### Critical Rules
1. **DTO Usage:** NEVER expose JPA entities in controllers. Always use DTOs for request/response.
2. **Lombok Conventions:** `@RequiredArgsConstructor` for constructor injection; `@Data` for DTOs; `@Getter`/`@Setter` for entities (avoid `@Data` on entities to prevent circular-reference issues).
3. **Validation:** Use Jakarta Validation annotations (`@NotBlank`, `@NotNull`, `@Email`, `@Valid`).
4. **Verbose Comments:** Every class and method must have educational comments explaining what and why.
5. **Code Clarity:** Prioritize readable code over clever one-liners.

---

## 📂 Project Structure
```
com.koustav.kaptur/
├── controllers/   AuthController, EventController, FileController, GlobalExceptionHandler
├── dto/           AuthResponse, LoginRequest, RegisterRequest, GoogleLoginRequest,
│                  EventRequest/Response, PhotoUploadRequest/Response, PhotoResponse
├── model/         User, Event, EventMembersDtl, EventMembersHistory, EventPhotos, UserRoleMst,
│                  CustomUserDetails, TusdHookRequest/Response, enums/ (AuthProvider, PhotoStatus, Role, Status)
├── repository/    User, Event, EventMembers, EventPhotos, UserRoleMst repositories
├── security/      SecurityConfig, AuthTokenFilter, JwtUtils, CustomUserDetailsService,
│                  OAuth2AuthenticationSuccessHandler (commented out / unused)
└── services/      AuthService, EventService, FileServices
```

---

## 🔐 Authentication System
**JWT-based, stateless.** Login (`/auth/login` or `/auth/google`) returns a JWT + safe user profile (no password). The client sends `Authorization: Bearer <token>`; `AuthTokenFilter` validates it on every request and sets the `kptId` (UUID string from the subject claim) as the SecurityContext principal name. Controllers resolve the caller via `getCurrentUser()`, which does `UUID.fromString(authentication.getName())` → `userRepository.findByKptId(uuid)`.

**Google native login (used by Flutter):** the app posts a Google ID token to `/auth/google`; `AuthService.googleLogin2()` verifies it with `GoogleIdTokenVerifier`, extracts email/name/picture/googleId, finds or creates the user, and issues our JWT.

### Public ID Formats — All UUIDs
- **User kptId:** UUIDv7 (time-ordered) · **Event evntId:** UUIDv7 · **Photo photoId:** UUIDv4 · **roleId / memberDtlId:** UUID (Hibernate-generated).
- **Breaking change from v1:** old `KPT0000001` / `EVNT0000001` string prefixes are gone — all IDs are standard UUIDs. UUIDv7 is used where B-tree index locality matters (User, Event); UUIDv4 where randomness is preferred (photos).

---

## 🗄️ Database Schema
- **Database:** `kaptur` · **Schema:** `kaptur_schema` · **DDL:** `spring.jpa.hibernate.ddl-auto=update` · **URL:** `jdbc:postgresql://localhost:5432/kaptur?currentSchema=kaptur_schema`.
- **Relationships:** `User 1—N Event`; `Event 1—N EventPhotos` (`uploadedBy → User`); `Event 1—N EventMembersDtl` and `Event 1—N EventMembersHistory` (each `roleMst → UserRoleMst`); `UserRoleMst 1—N` both membership tables.
- **Important:** A record in EventMembersDtl = an accepted member. There is no separate invitation/status field. The `Status` enum exists but is not currently used.

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
`/auth/**` · `/oauth2/**` · `/v3/api-docs/**` · `/swagger-ui/**` · `/swagger-ui.html` · `/tusd/**`

---

## 📤 Photo Upload — Complete Flow & Client Integration Guide
The Spring Boot server does **NOT** handle file bytes. A separate [TUSd Go server](https://github.com/tus/tusd) (`localhost:1080`, Docker) does all file I/O and stores to S3. Spring Boot manages only **photo metadata** and the **upload lifecycle** via TUSd hooks.

### Upload Sequence (client must implement)
1. **Init** — `POST /events/{evntId}/photos/init` (Bearer JWT) with `{filename, fileType, fileSizeInKb}` → `201` `{photoId (UUID), tusdUploadUrl}`.
2. **Create TUS upload** — `POST {tusdUploadUrl}` with `Upload-Length` and `Upload-Metadata` (`photoId`, `filename`, `filetype`). **`Upload-Metadata` values MUST be Base64-encoded.** TUSd then calls `/tusd/hooks` (`pre-create`): Spring validates the photo is `PENDING` and overrides the upload ID to our UUID → `PENDING → UPLOADING`.
3. **Upload chunks** — `PATCH {tusdUploadUrl}/{photoId}` with `Upload-Offset` + `Content-Type: application/offset+octet-stream` → `204` to continue, `200` when complete.
4. **Resume** — `HEAD {tusdUploadUrl}/{photoId}` returns `Upload-Offset`/`Upload-Length`; resume the PATCH from that offset (resumability is fully TUSd-handled).
5. **Completion (automatic)** — TUSd `post-finish` hook → Spring marks `COMPLETED` and stores the S3 key. No client action.
6. **List** — `GET /events/{evntId}/photos` (Bearer) → `200` array; each item's `downloadUrl` (`{tusd.base-url}/{photoId}`) streams from S3.
7. **Delete** — `DELETE /events/{evntId}/photos/{photoId}` (Bearer) → `200 {message}`. Only the uploader or the event creator may delete.

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
All endpoints return consistent error JSON: `{ "error": "Human-readable error message" }`

| Status | Condition |
|--------|-----------|
| 400 | Validation failure, bad request |
| 401 | Missing or expired JWT |
| 403 | Not a member of the event / not authorized |
| 404 | Event or photo not found |
| 409 | Photo already being uploaded (status not PENDING) |

### Key Design Decisions for Client Developers
1. **All IDs are UUIDs** — no more `KPT0000001`-style strings; all identifiers are standard UUIDs sent as strings in JSON.
2. **`photoId` is a UUID** — generated server-side, used as the TUSd upload ID; always the same for the same photo across all operations.
3. **`eventId` in photo endpoints is a UUID string** — the event's UUID, NOT an old-style string ID.
4. **`tusdUploadUrl`** is just the base URL. The TUS protocol's `POST` to the base URL creates the upload resource.
5. **`Upload-Metadata`** values MUST be Base64-encoded strings in the TUS protocol.
6. **Resumability** is fully handled by TUSd. Store the upload URL (`tusdUploadUrl + "/" + photoId`) and use `HEAD` to get the current offset.
7. **Event membership is binary** — existence in EventMembersDtl = accepted member. No pending/rejected states.
8. **Files served via TUSd** — the download URL points to TUSd, which streams from S3.
9. **Photo/video support** — `fileType` MIME string determines media type. No separate enum.

---

## 🔧 Configuration Reference
- **Server:** `server.port=8080`.
- **Database:** `spring.datasource.url=jdbc:postgresql://localhost:5432/kaptur?currentSchema=kaptur_schema` (user/password `postgres`), `ddl-auto=update`, `hibernate.default_schema=kaptur_schema`.
- **JWT:** `app.jwtSecret`, `app.jwtExpirationMs` (access, ~15 min), `app.jwtRefreshExpirationMs` (refresh, 30 days).
- **Google OAuth:** `google.client-id`.
- **TUSd:** `tusd.base-url=http://localhost:1080/files`.
- **Security:** secrets currently live in `application.properties` — move them to env/secret storage before production.

---

## 📋 Development Workflows
- **Run:** `.\mvnw.cmd spring-boot:run` (Windows) / `./mvnw spring-boot:run` (mac/Linux).
- **Build:** `.\mvnw.cmd clean package`. **Test:** `.\mvnw.cmd test`.
- **Database:** `ddl-auto=update` manages the schema; Flyway is configured but its migrations directory is empty. **`USER_ROLE_MST` must be seeded manually (see backend README) before events can be created** — there is no auto-seeding.

---

## 🚨 Known Issues & Technical Debt
1. **OAuth2 Browser Flow Disabled** — `SecurityConfig.java` / `OAuth2AuthenticationSuccessHandler.java` are entirely commented out (native Google login is used for Flutter instead).
2. **AuthTokenFilter Uses Stub Principal** — `AuthTokenFilter.java` builds a bare Spring Security `User` instead of loading `CustomUserDetails`; controllers work around it via `userRepository.findByKptId()`. **Impact:** `@PreAuthorize` role checks won't work (authorities are always empty).
3. **No Role-Based Access Control on Events** — `EventController`/`EventService` only check if the user is the creator; the `roleMst` relationship is not used to grant member-level permissions. **Missing:** invite system + role-based member permissions.
4. **Test Coverage** — currently only `KapturApplicationTests.java` (context load). **Missing:** service unit tests + controller integration tests.
5. **Role Seed Data Required** — `USER_ROLE_MST` must be manually seeded before events can be created; no auto-seeding mechanism exists.

---

## 🎯 Adding a New Feature — Required Steps
1. **Enum** (if needed) under `model/enums/`.
2. **Entity** — UUID PK; `@Getter`/`@Setter` (not `@Data`); LAZY `@ManyToOne`.
3. **Repository** — `extends JpaRepository<Entity, UUID>`.
4. **DTOs** — `@Data` request (with validation) + response (never leak entities).
5. **Service** — `@Service`, `@Transactional` on writes, business logic only.
6. **Controller** — `@RestController` wiring DTOs; resolve the caller via `getCurrentUser()`.
7. **Bruno request file — REQUIRED, in the same change as the endpoint (every time):**
   - Add `bruno/Kaptur/<Name>.yml` (OpenCollection YAML) mirroring `Login.yml` / `CreateEvent.yml`, with sections: `info` (name, `type: http`, seq, tags), `http` (method, url `http://127.0.0.1:8080/...`, body, auth), `runtime` (a `res.status` assertion + an `after-response` script to capture returned tokens/IDs), `settings`, `docs` (purpose, expected status, run order).
   - Public endpoints use `auth: inherit`; protected use `auth: {type: bearer, token: "{{token}}"}`.
   - Chain IDs/tokens with `bru.setVar("token", res.body.accessToken)` / `bru.setVar("eventId", res.body.evntId)` / `bru.setVar("photoId", res.body.photoId)` and reference them as `{{token}}` / `{{eventId}}` / `{{photoId}}`.
   - An endpoint is **NOT done** until its Bruno file exists, its embedded JSON body is valid, and it is listed in the API Reference above.

---

## 💡 Best Practices for This Codebase
1. **Always use DTOs** - Never return entities directly. `AuthResponse` uses safe fields (kptId, email, name, imageUrl), not the full User entity.
2. **Use @Transactional** - For methods that modify multiple entities.
3. **Soft delete** - Use the `isDeleted` flag instead of hard delete.
4. **UUID PKs** - All entities use UUID primary keys. Use `UuidCreator.getTimeOrderedEpoch()` for UUIDv7 where B-tree performance matters.
5. **Verbose comments** - Explain what and why, not just what.
6. **Educational code** - Step-by-step logic, descriptive variables.
7. **Error handling** - Throw RuntimeException with clear messages.
8. **Security** - Always validate user ownership/membership before modifications.
9. **LAZY fetching** - All `@ManyToOne` relationships use `FetchType.LAZY`. Exclude from `@ToString` and `@EqualsAndHashCode`.
10. **No composite keys** - All entities use single UUID PKs. Unique constraints are defined via `@UniqueConstraint`.

---

## 🔍 Quick Reference: Common Tasks (no code)
- **Get current user:** parse `authentication.getName()` (kptId UUID string) → `UUID.fromString` → `userRepository.findByKptId(uuid)`, throw if absent.
- **Generate UUID PK:** UUIDv7 = `UuidCreator.getTimeOrderedEpoch()`; UUIDv4 = `UUID.randomUUID()`; set on the entity before save.
- **Ownership check:** compare `entity.getCreatedBy().getKptId()` with `currentUser.getKptId()`.
- **Validate event membership:** a row in `EventMembersDtl` for (event, user) = accepted member.
- **Look up a role:** `userRoleMstRepository.findByRoleCode(...)`.

---

**Last Updated:** 2026-07-04 (UUID migration complete — all entities use UUID PKs, DTOs aligned, role-based membership with UserRoleMst)
**Scanned By:** AI Agent (Full Project Alignment Pass)
