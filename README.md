# demo1 — Spring Boot HR & Payroll API

A production-shaped Spring Boot 4 REST API for employee (*karyawan*) and payroll management, built as a deliberate walkthrough of the patterns a real backend needs: stateless JWT authentication, database-driven role-based access control, encrypted personal data, Redis caching, stored-procedure access, external API integration, and a full CI/CD path from GitHub Actions to an Ubuntu server.

**Live API documentation: https://api-dev.morpkhai.web.id/docs**

The documentation is interactive — you can authenticate and call every endpoint directly from the page. See [Interactive API documentation](#interactive-api-documentation) below.

---

## Table of contents

- [Why this project is worth reading](#why-this-project-is-worth-reading)
- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [Core features](#core-features)
- [Interactive API documentation](#interactive-api-documentation)
- [Endpoint overview](#endpoint-overview)
- [Roles and access model](#roles-and-access-model)
- [Getting started](#getting-started)
- [Configuration and profiles](#configuration-and-profiles)
- [Testing](#testing)
- [CI/CD and deployment](#cicd-and-deployment)
- [Project conventions](#project-conventions)
- [Further documentation](#further-documentation)

---

## Why this project is worth reading

Most tutorial-grade Spring Boot projects stop at CRUD. The parts of this repository that carry real transferable value are:

| Topic | Where to look |
|---|---|
| **A self-describing authorization model** — an endpoint that introspects the running application's own `@PreAuthorize` annotations and returns the live role/endpoint matrix | `service/RoleMapService.java`, `GET /api/rolemap/matriks` |
| **Field-level encryption at rest** — AES/GCM applied transparently through a JPA `AttributeConverter`, so `nik` and `npwp` are never stored or logged in plaintext | `security/CryptoConverter.java`, `entity/DetailKaryawan.java` |
| **Brute-force lockout that survives restarts** — attempt counters live in Redis with a fixed 15-minute window, not in application memory | `security/LoginAttemptService.java` |
| **Four ways to reach the same stored procedure** — JdbcTemplate and JPA, for PostgreSQL and SQL Server, side by side for comparison | `repository/storeprocedure/` |
| **Two implementations of the same CRUD surface** — Spring Data JPA (`/api/karyawan`) versus raw `JdbcTemplate` (`/api/karyawan2`), so the trade-offs are visible rather than argued about | `service/impl/KaryawanServiceImpl.java`, `service/Karyawan2Service.java` |
| **Optimistic locking done honestly for a stateless API** — `@Version` alone is not enough when every request is a fresh transaction; the payroll service documents and handles the gap | `service/impl/PayrollServiceImpl.java` |
| **An executable access-control matrix** — RBAC is not described in prose, it is asserted in runnable HTTP files, one per role | `http/role-*.http`, `http/runner.py` |
| **Fail-fast configuration** — the application refuses to start when a required environment variable is missing or left as an unresolved placeholder | `config/prop/AppConfigProperties.java` |

---

## Tech stack

| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Build | Maven (`./mvnw`) |
| Database | PostgreSQL (schema `masesas`) |
| Cache & rate state | Redis |
| Authentication | Hand-rolled JWT via jjwt 0.12.6, `STATELESS` sessions |
| Authorization | Spring Security with a role hierarchy and method-level `@PreAuthorize` |
| API documentation | springdoc-openapi + Scalar (no Swagger UI) |
| Boilerplate | Lombok |
| Testing | JUnit 5, Mockito, AssertJ, MockMvc, JaCoCo |
| Supply chain | OWASP Dependency-Check, Maven Enforcer |
| Delivery | GitHub Actions → GHCR → Docker Compose on Ubuntu |

---

## Architecture

A conventional layered structure. Controllers stay thin, business logic lives in services, and data access is isolated behind repositories.

```
com.masesas.exercises.demo1
├── config/         Spring configuration (security, redis, jdbc, clock, openapi)
│   └── prop/       Validated @ConfigurationProperties — fails fast on bad config
├── controller/     REST endpoints
├── dto/            Request and response payloads
├── entity/         JPA entities
├── model/          Non-JPA models (stored-procedure results)
├── projection/     Query projections
├── repository/     Spring Data + JdbcTemplate
│   └── storeprocedure/   Stored-procedure access, per engine and per style
├── security/       JWT issuing/filtering, user details, login lockout, crypto
├── service/        Service interfaces
│   ├── impl/       Implementations
│   └── support/    Shared helpers and validators
└── exception/      Domain exceptions and the global handler
```

Request flow for an authenticated call:

```
HTTP request
  → JwtAuthFilter          validates the bearer token, populates SecurityContext
  → SecurityFilterChain    whitelist check, then authentication required
  → @PreAuthorize          role check on the controller method
  → Controller             validates input (@Valid), delegates
  → Service                business rules, caching, transactions
  → Repository             JPA / JdbcTemplate / stored procedure
  → GlobalExceptionHandler maps any failure to a uniform error contract
```

---

## Core features

### Authentication

Two independent identity types share one token format: **karyawan** (staff) and **customer**. Each has its own login endpoint and its own table, resolved by `AppUserDetailsService`. Tokens are signed JWTs with a 15-minute TTL and no server-side session — the API is fully stateless and horizontally scalable.

Passwords are stored using Spring Security's `DelegatingPasswordEncoder` with BCrypt at strength 12, so hashes carry their algorithm as a `{bcrypt}` prefix and can be migrated later without a flag day.

### Account lockout

Five consecutive failed logins for a username lock it for 15 minutes. The counter lives in Redis under `demo1:login-attempt:<username>`, so the lock is shared across instances and survives a restart. A successful login clears the counter. Locked accounts receive `423 Locked`; every other credential failure receives a deliberately identical `401` message so the API never reveals whether an account exists.

### Role-based access control

Roles come from the database (`karyawan_role` → `role`), not from a hardcoded list. Authorization is declared per method with `@PreAuthorize`, and a role hierarchy grants `SUPERADMIN` everything the other roles hold:

```
SUPERADMIN > ADMIN, MANAGER, MARKETING, SALES, HR, KARYAWAN, CUSTOMER
```

Unauthenticated callers are not rejected outright — they are given an explicit `GUEST` principal, which keeps public and private routes on the same code path.

The `/api/rolemap` endpoints reflect over the running application's own handler mappings and annotations to produce the current access matrix. The documentation cannot drift from the implementation, because it *is* the implementation.

### Data protection

`nik` and `npwp` in `detail_karyawan` are encrypted with AES/GCM through a JPA `AttributeConverter`, transparently on write and read. The key is supplied as `CRYPTO_KEY` and never appears in source.

Removing the `@Convert` annotation without first decrypting existing rows will make stored data unreadable.

### Caching

Employee reads are cached in Redis via `@Cacheable`, with `@CacheEvict` on every write path so a stale entry cannot outlive a mutation. `RedisCacheService` wraps the lower-level operations and a configurable key prefix keeps environments from colliding on a shared Redis instance.

### Payroll

Composite-key entity (`PayrollId`), a `DRAFT → APPROVED` status workflow, and optimistic locking through `@Version`. Approved payroll rows are protected from further edits at the entity level, not just in the service.

### Stored procedures

`sp_proses_karyawan` is invoked four ways — `JdbcTemplate` and JPA, against PostgreSQL and SQL Server — including `OUT` parameter handling and result-set mapping. The SQL for each engine is in the repository root (`stored_procedure_*.sql`).

### External API integration

`DummyJsonService` consumes the public DummyJSON API through a `RestTemplate` with explicit connect and read timeouts, mapping responses into typed DTOs and translating upstream failures into the API's own error contract.

### File upload

Avatar upload with a 2 MB ceiling enforced at both the multipart and application layers. Stored paths are normalized and resolved against a configured base directory, so a crafted filename cannot escape it.

### Error contract

Every error — validation, domain, authentication, or unexpected — is returned in one shape:

```json
{
  "timestamp": "2026-08-17T16:59:51.732764Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Username atau password salah"
}
```

Stack traces, exception class names, and internal messages are disabled in all environments.

### Security headers

Both filter chains set `Content-Security-Policy`, `Referrer-Policy: no-referrer`, HSTS with `includeSubDomains`, `X-Frame-Options: DENY`, and `X-Content-Type-Options: nosniff`. CORS uses an explicit origin allowlist — never `*` — because credentials are permitted.

---

## Interactive API documentation

**https://api-dev.morpkhai.web.id/docs**

| Path | Content |
|---|---|
| `/docs` | Interactive documentation UI (Scalar) |
| `/docs/openapi` | OpenAPI 3.1 specification, JSON |
| `/docs/openapi.yaml` | The same specification, YAML |
| `/docs/scalar.js` | UI bundle, served from the jar — no CDN request |

The page is public and requires no token. It is enabled in every profile and can be switched off per environment with `scalar.enabled=false`, without touching code.

**Authenticating from the page:** click **Authorize**, choose `karyawanAuth` or `customerAuth`, and submit your username and password. Scalar performs an OAuth2 password-flow request against `/api/auth/karyawan/token`, reads `access_token` from the response, and attaches `Authorization: Bearer …` to every subsequent **Try it** call. The token persists across page reloads.

Nothing in the specification is written by hand — it is generated at runtime from the controller annotations, DTO schemas, and the security configuration. Telemetry and remote font loading are disabled, so the page makes no outbound requests.

---

## Endpoint overview

| Method | Path | Access |
|---|---|---|
| `POST` | `/api/auth/karyawan/login` | Public |
| `POST` | `/api/auth/karyawan/token` | Public — OAuth2 password flow, form-urlencoded |
| `POST` | `/api/auth/customer/register` | Public |
| `POST` | `/api/auth/customer/login` | Public |
| `POST` | `/api/auth/customer/token` | Public — OAuth2 password flow |
| `GET` | `/api/rolemap`, `/api/rolemap/matriks`, `/api/rolemap/{role}` | Public |
| `GET` | `/api/customer/me` | `CUSTOMER` |
| `GET/POST/PUT/DELETE` | `/api/karyawan/**` | `ADMIN`, `MANAGER`, and others per method |
| `GET/POST/PUT/DELETE` | `/api/karyawan2/**` | JdbcTemplate variant of the same surface |
| `GET/PUT` | `/api/sp/karyawan/**` | Stored-procedure access |
| `GET` | `/api/sp/karyawan/statistik/**` | Aggregated statistics |
| `GET/POST/PUT/DELETE` | `/api/payroll/**` | `ADMIN`, `MANAGER`, `HR` |
| `GET` | `/api/dummyjson/products/**` | External API proxy |
| `GET` | `/docs`, `/docs/openapi` | Public |

The authoritative, always-current list is `GET /api/rolemap/matriks` — or the live documentation.

---

## Roles and access model

| Role | Reachable endpoints | Scope |
|---|---|---|
| `SUPERADMIN` | 52 | Everything — inherits every role below |
| `ADMIN` | 51 | Full employee management, deletion, detail records |
| `MANAGER` | 48 | Employee management and payroll, no deletion |
| `HR` | 45 | Payroll and employee reads |
| `KARYAWAN` | 36 | Own-scope reads |
| `MARKETING` | 36 | Employee reads |
| `SALES` | 36 | Employee reads |
| `CUSTOMER` | 32 | Customer self-service only |
| `GUEST` | 12 | Assigned to unauthenticated requests; public routes only |

The counts are not maintained by hand — they come from `GET /api/rolemap/matriks`, which derives them from the running application. Every role's total includes the 12 public endpoints.

A user may hold several roles at once — `manager.sales@masesas.test` in the seed data exercises that path.

---

## Getting started

### Prerequisites

- JDK 21
- PostgreSQL with a `masesas` schema
- Redis
- Docker (optional — only for container runs)

### Setup

```bash
git clone https://github.com/masesas/spring-basic-sp.git
cd spring-basic-sp

cp .env.example .env
# Fill in DB_*, REDIS_*, JWT_SECRET, CRYPTO_KEY, DEMO_PASSWORD
```

`.env` is git-ignored and never committed. `CRYPTO_KEY` must be a base64-encoded AES key; `JWT_SECRET` must be long enough for HS512.

### Database

Run the SQL files in the repository root, in this order:

```bash
psql -h <host> -U <user> -d <database> -f seeder.sql
psql -h <host> -U <user> -d <database> -v pwd_hash="$HASH" -f rbac_masesas.sql
psql -h <host> -U <user> -d <database> -f rbac_role_penuh_masesas.sql
psql -h <host> -U <user> -d <database> -f rbac_superadmin_masesas.sql
psql -h <host> -U <user> -d <database> -f stored_procedure_postgresql.sql
```

Generate `$HASH` from your `DEMO_PASSWORD`:

```bash
HASH="{bcrypt}$(htpasswd -bnBC 12 "" "$DEMO_PASSWORD" | tr -d ':\n')"
```

### Run

```bash
./mvnw spring-boot:run          # http://localhost:8080
```

Then open http://localhost:8080/docs.

### Commands

```bash
./mvnw clean compile                  # compile
./mvnw test                           # full test suite
./mvnw test -Dtest=RbacGuestTest      # a single test class
./mvnw spring-boot:run                # run the application
./mvnw verify -Powasp                 # dependency vulnerability scan
python3 http/runner.py                # run the RBAC access matrix end to end
```

---

## Configuration and profiles

Four property files, each with one job:

| File | Purpose |
|---|---|
| `application.properties` | Environment-independent policy: security rules, error contract, documentation, paging |
| `application-local.properties` | Local development — activated automatically for `mvnw test` and `spring-boot:run` |
| `application-dev.properties` | The `demo1-dev` container |
| `application-prod.properties` | The `demo1-prod` container |

No scenario runs without a profile. Environment-specific values — hosts, credentials, pool sizes, log levels — belong in the profile file, never in the shared one.

Secrets are read from `.env` locally and from GitHub Variables/Secrets in CI, rendered into the container environment by `scripts/render-env.sh`. `AppConfigProperties` validates them at startup with `@Validated` and a custom `@NotPlaceholder` constraint, so a missing or unsubstituted variable stops the application immediately instead of surfacing as a confusing runtime failure later.

---

## Testing

```bash
./mvnw test
```

The suite covers RBAC per role (guest, customer, karyawan, superadmin, and the database-driven role mapping), the security whitelist, JWT issuing and the token endpoints, crypto round-trips, cache behaviour, payroll rules, image storage, the external API client, and the error contract.

Beyond JUnit, `http/role-*.http` holds one runnable file per role, where each endpoint is called with that role's token and the expected status is asserted with `client.test(...)`. `python3 http/runner.py` executes them all and exits non-zero on any failed assertion, so the access matrix is verified rather than assumed. See [http/README.md](./http/README.md).

Coverage is measured with JaCoCo.

---

## CI/CD and deployment

| | Development | Production |
|---|---|---|
| Trigger branch | `main` | `production` |
| Spring profile | `dev` | `prod` |
| Image | `ghcr.io/masesas/demo1-dev` | `ghcr.io/masesas/demo1-prod` |
| Tag | `<ddMMyyyy>-latest` and `<ddMMyyyy>-<sha7>` | same |
| Container | `demo1-dev` | `demo1-prod` |
| Host port | `8080` | `8081` |

Three workflows:

- `ci.yml` — build and test on every pull request
- `deploy-dev.yml` — on push to `main`: test, build a multi-stage image, push to GHCR, deploy over SSH
- `deploy-prod.yml` — the same path for `production`

Merging a pull request *is* a push, so merging to `main` deploys to the development server. Image tags use `TZ=Asia/Jakarta` so the date does not slip by a day against the UTC runner.

The image is a two-stage build; the container runs with graceful shutdown, giving in-flight requests time to complete before the JVM exits. Full plans in [docs/ci-cd/](./docs/ci-cd/).

---

## Project conventions

Three rules are enforced throughout this repository — see [CLAUDE.md](./CLAUDE.md) for the full statement.

1. **No `record` types.** All DTOs and models are plain classes with Lombok (`@Data` + `@NoArgsConstructor` + `@AllArgsConstructor` where Jackson is involved, `@Getter` + `final` fields where it is not).
2. **Simplest thing that works.** No interface for a single implementation, no speculative configuration, no error handling for conditions that cannot occur.
3. **No comments in Java files.** Naming carries the meaning; context belongs in markdown. `.properties` and `.sql` files are exempt.

Other standing conventions: constructor injection via `@RequiredArgsConstructor` and `final` fields (never `@Autowired` on a field), time obtained from the injected `Clock` bean so it can be controlled in tests, always-parameterized SQL, and English for technical names with Indonesian retained for domain terms (`Karyawan`, `Rekening`, `Payroll`).

Commit messages follow `<type>: <description>` with types `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`.

---

## Further documentation

| Document | Content |
|---|---|
| [CLAUDE.md](./CLAUDE.md) | Working rules and standards for this repository |
| [docs/api-docs/README.md](./docs/api-docs/README.md) | How the Scalar documentation is wired |
| [docs/ci-cd/README.md](./docs/ci-cd/README.md) | CI/CD implementation plans, one per file |
| [http/README.md](./http/README.md) | The executable RBAC access matrix |
| `IMPLEMENTATION-PLAN-RBAC-FASE*.md` | The staged RBAC design history |
