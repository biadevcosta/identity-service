# identity-service

Authorization server of the hospital appointment system. Registers users, authenticates them with a
password, and issues the **RS256 JWT access token** that `scheduling-service` and `history-service`
validate with the shared public key. It also acts as a resource server for its own ADMIN route.

Port: **8080**. Database: `identity_db` (MySQL).

## What it does

| Endpoint | Auth | Purpose |
|---|---|---|
| `POST /auth/login` | public | email + password → `{ accessToken, refreshToken, tokenType, expiresIn }` |
| `POST /auth/refresh` | public | `{ refreshToken }` → a **new** token pair; the presented refresh token is revoked (rotation) |
| `POST /users` | `ADMIN` bearer token | register a `DOCTOR` / `NURSE` / `PATIENT` / `ADMIN` → `201 { id, email, role, fullName }` |
| `GET /users/{id}` | public | `{ id, name, email, role }` — used by the other services to resolve names |
| `GET /actuator/health` | public | health probe |
| `GET /swagger-ui.html`, `GET /v3/api-docs` | public | interactive API docs (springdoc-openapi) |

**Access token claims:** `iss` (`hospital-identity`), `aud` (`hospital-services`), `sub` (userId),
`role`, `iat`, `exp` (1 h), and `patientId` (only for `PATIENT` users, equal to `sub`).

**Refresh token:** opaque (256-bit random, Base64URL), stored only as a SHA-256 hash, single-use
(rotated on every refresh), revocable, TTL 7 days.

## Architecture (Clean Architecture)

```
domain/          User, RefreshToken, Role, exception/*        — pure Java, no framework
application/     usecase/  RegisterUserUseCase, AuthenticateUseCase, RefreshAccessTokenUseCase, TokenMinter
                 port/     UserRepository, PasswordHasher, TokenIssuer, RefreshTokenRepository, RefreshTokenHasher
                 command/  RegisterUserCommand ; AuthTokens
infrastructure/  persistence/  Spring Data JDBC entities + mappers + *RepositoryImpl (@Version -> INSERT/UPDATE)
                 security/     Argon2PasswordHasher, NimbusJwtTokenIssuer (RS256), Sha256RefreshTokenHasher, SecurityConfig
                 web/          AuthController, UserController, DomainExceptionHandler (RFC 7807 ProblemDetail)
                 config/       JwtProperties, Argon2Properties, RsaKeys, SecurityBeansConfig, UseCaseConfig
                 bootstrap/    AdminSeeder (creates the dev admin on startup if absent)
```

Rule placement:

| Rule | Where |
|---|---|
| `POST /users` restricted to `ADMIN` | `@PreAuthorize("hasRole('ADMIN')")` on the controller + resource server |
| unknown email and wrong password fail identically | `AuthenticateUseCase` → `InvalidCredentialsException` (401) |
| `crm`/`specialty` required for a doctor, rejected for others | `User.register` (domain) |
| password hashed, never stored in clear | `PasswordHasher` port → `Argon2PasswordHasher` (Argon2id) |
| refresh token rotation + "no side effects on failure" | `RefreshAccessTokenUseCase` (validate all, revoke last) |

## Password hashing

**Argon2id** via Spring Security's `Argon2PasswordEncoder`, wrapped in a `DelegatingPasswordEncoder`
so hashes carry a `{argon2id}` prefix (future algorithm migration stays open). Parameters
(`application.yaml` → `security.password.argon2`): `m = 19456 KiB`, `t = 2`, `p = 1` (OWASP baseline).

## RSA key pair (RS256)

```bash
# from the repo root
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out keys/private.pem
openssl rsa -in keys/private.pem -pubout -out keys/public.pem

cp keys/private.pem identity-service/src/main/resources/private.pem   # git-ignored, signs
cp keys/public.pem  identity-service/src/main/resources/public.pem    # committed, validates
# also copy keys/public.pem into scheduling-service and history-service src/main/resources/
```

`private.pem` is **git-ignored** (`.gitignore`) — regenerate it after a fresh clone. The integration
test does **not** need it: it uses a committed test pair (`src/test/resources/test-*.pem`).

## Configuration (`src/main/resources/application.yaml`)

| Key | Default | Notes |
|---|---|---|
| `server.port` | `8080` | |
| `spring.datasource.url` | `jdbc:mysql://localhost:3309/identity_db` | root/root; Flyway runs `V1`, `V2` |
| `security.jwt.private-key` / `public-key` | `classpath:private.pem` / `classpath:public.pem` | RS256 |
| `security.jwt.issuer` / `audience` | `hospital-identity` / `hospital-services` | |
| `security.jwt.access-ttl-seconds` / `refresh-ttl-seconds` | `3600` / `604800` | |
| `app.admin.email` / `app.admin.password` | `admin@hospital.local` / `admin12345` | dev admin, seeded on startup if absent |

## Run locally

```bash
cd identity-service
docker compose up -d mysql-identity        # MySQL on localhost:3309
./mvnw spring-boot:run                      # service on 8080
```

Or everything in containers (builds the image, waits for MySQL):

```bash
cd identity-service
docker compose up --build
```

`docker compose` also starts **phpMyAdmin** on <http://localhost:8082> (auto-logged in as `root`/`root`,
database `identity_db`) to browse the tables.

## API docs (OpenAPI / Swagger)

`springdoc-openapi` exposes the live spec — no separate file to keep in sync:

| URL | What |
|---|---|
| <http://localhost:8080/swagger-ui.html> | Swagger UI. Click **Authorize**, paste an `accessToken` from `POST /auth/login`, then call `POST /users`. |
| <http://localhost:8080/v3/api-docs> | raw OpenAPI 3.1 JSON |

All three doc paths (`/swagger-ui/**`, `/v3/api-docs/**`) are `permitAll` in `SecurityConfig`.

Quick check:

```bash
# log in as the seeded admin
curl -s localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@hospital.local","password":"admin12345"}'

# register a doctor (ADMIN token from the login response)
curl -s localhost:8080/users -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"email":"doc@hospital.local","password":"secret12345","role":"DOCTOR","fullName":"Dr House","crm":"CRM-1","specialty":"Diagnostics"}'
```

Paste an access token into <https://jwt.io> to inspect the claims.

## Tests

```bash
./mvnw test      # 54 unit tests (domain, use cases, adapters, controllers, seeder) — no Docker
./mvnw verify    # + IdentityIntegrationTest (Testcontainers MySQL) + JaCoCo 80% line gate
```

`IdentityIntegrationTest` needs a running Docker daemon; it **self-skips** (`assumeTrue`) when Docker
is unavailable, so `./mvnw verify` still passes. Current line coverage: **~98%**.
Excluded from the gate: `IdentityApplication`, `infrastructure/config/**`, `infrastructure/security/SecurityConfig`.

An **Insomnia** collection covering login, refresh, register and the profile lookup is in
`identity.insomnia.json`.
