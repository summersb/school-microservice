# Student Enrollment Microservices Sample

A sample microservices system demonstrating a student-enrollment domain: six
independent Kotlin/Quarkus services backed by a single shared Postgres
database, a React/Vite frontend, and an Apache httpd proxy that fronts
everything behind one port.

## Services

| Service            | Owns                                      | Notes |
|---------------------|--------------------------------------------|-------|
| `auth-service`       | User credentials                          | Issues HS256 JWTs on login |
| `student-service`    | Student records                           | CRUD, admin-only writes |
| `faculty-service`    | Faculty/teacher records                   | CRUD, admin-only writes |
| `classes-service`    | Course catalog for each semester          | Validates `facultyId` against faculty-service on create/update |
| `schedule-service`   | Each student's current + past enrollments | Public API is read-only; writes only via internal endpoints |
| `register-service`   | Nothing (stateless orchestrator)          | Coordinates student/classes/schedule to enroll or drop a class |

## The database

All five data-owning services share one Postgres database, `school_db`, so
the tables reference each other with real foreign keys:

| Table            | Owned by           | Links to |
|------------------|--------------------|----------|
| `faculty`        | `faculty-service`  | — |
| `student`        | `student-service`  | — |
| `course_class`   | `classes-service`  | `faculty_id` → `faculty.id` |
| `schedule_entry` | `schedule-service` | `student_id` → `student.id`, `class_id` → `course_class.id` |
| `app_user`       | `auth-service`     | `student_id` → `student.id`, `faculty_id` → `faculty.id` |

Delete behaviour follows from what each link means:

- Deleting a **student** or a **class** cascades to their `schedule_entry`
  rows -- an enrollment is meaningless without both ends.
- Deleting a **student** or **faculty** member leaves their `app_user`
  login in place but sets the link to `NULL` (both columns are nullable; an
  `ADMIN` account points at neither).
- Deleting a **faculty** member who still teaches a class is refused
  (`ON DELETE RESTRICT`), because `course_class.faculty_id` is `NOT NULL`.
  Each service turns such a refusal into a `409 Conflict` rather than a 500.

The schema lives once, in `db/migration/`, and is applied by exactly one
thing: the `db-migrate` job in `docker-compose.yml`. It runs the official
Flyway image against `school_db`, exits, and the five data-owning services
wait on `service_completed_successfully` before they start. They run with
`quarkus.flyway.migrate-at-start=false` and only ever read and write rows.

Keeping migration out of the services means there is no start-up race for the
shared database, no service needs DDL rights on it, and a rolling deploy can
never have two services carrying different migration sets fight over the
schema.

Dev mode and tests are a separate database and a separate story: Quarkus Dev
Services starts a throwaway Postgres per service, and nothing else is around
to build the schema in it. So the same migrations are also copied onto each
data-owning service's classpath at build time (the maven-resources-plugin
execution in the parent `pom.xml`), and Flyway is re-enabled there via
`%dev.` and `%test.` overrides.

Services still own their tables in code -- each one maps only its own entity
and never queries another service's table. All cross-service communication
still happens over HTTP; the foreign keys add integrity at the storage layer
without collapsing the service boundaries.

`register-service` demonstrates the "services working together" requirement
most directly: `POST /register-api/register/enroll` calls student-service to
confirm the student is active, classes-service to confirm the class has
room, reserves the seat, then calls schedule-service to record the
enrollment -- releasing the seat again if the last step fails.

Internal-only endpoints (`/internal/...` on classes-service and
schedule-service, used exclusively by register-service) are protected by a
shared-secret header (`X-Internal-Token`) instead of a user JWT, and are not
meant to be reachable through the public proxy paths.

## Running it

Requires Docker (with Compose v2) and, on the host, nothing else -- all
builds happen inside containers.

```bash
docker compose up --build
```

If you previously ran the older per-service-database version of this project,
drop the volume first -- Postgres only creates `POSTGRES_DB` when its data
directory is empty, so an existing `postgres-data` volume would still hold the
old `auth_db`/`student_db`/... and never get `school_db`:

```bash
docker compose down -v
docker compose up --build
```

First build takes a while (Maven downloads dependencies for six services,
npm installs the frontend). Once everything is up:

- **Frontend:** http://localhost:8080/ui
- **Each backend directly through the proxy:** `http://localhost:8080/auth-api`,
  `/student-api`, `/faculty-api`, `/classes-api`, `/schedule-api`,
  `/register-api`
- **OpenAPI/Swagger UI per service** (also through the proxy): e.g.
  `http://localhost:8080/student-api/q/swagger-ui`

Give the backend containers a few seconds to finish starting after `docker
compose up` reports them as running -- Quarkus + Flyway migrations take a
moment, and the proxy has no health-gating, so an immediate request can
briefly 502.

### Demo logins

All seeded accounts share the password `password123`:

| Username          | Role    |
|-------------------|---------|
| `admin`           | ADMIN   |
| `susan.miller`    | FACULTY |
| `james.anderson`  | FACULTY |
| `alice.johnson`   | STUDENT |
| `brian.smith`     | STUDENT |
| `carla.diaz`      | STUDENT |
| `david.kim`       | STUDENT |
| `emma.wilson`     | STUDENT |

Sample data includes 10 students, 6 faculty, and 12 classes split across a
current semester (`FALL2026`) and a past one (`SPRING2026`), plus a handful
of pre-seeded enrollments so the UI has data to show immediately.

## Local development without Docker

Each backend service can run standalone via Quarkus Dev Services, which
spins up an ephemeral Postgres container automatically (Docker must still be
running locally, but the docker-compose stack does not need to be up):

```bash
cd student-service
mvn quarkus:dev
```

Each service defaults to its own port in dev mode so several can run at
once: auth 8081, student 8082, faculty 8083, classes 8084, schedule 8085,
register 8086.

For the frontend alone:

```bash
cd frontend
npm install
npm run dev
```

This proxies `/*-api` requests to `http://localhost:8080` (see
`frontend/vite.config.ts`), so point it at either the full docker-compose
stack or a manually-assembled set of locally-running services on those
ports.

## Tests

**Per-service unit tests** run against Quarkus Dev Services (Docker
required, no compose stack needed) and mock any calls to *other* services
via `@InjectMock` on the generated REST client interfaces -- `classes-service`
mocks its call to faculty-service, and `register-service` mocks all three of
its downstream clients:

```bash
mvn test                       # runs every service's unit tests
mvn test -pl register-service  # just one service
```

**Integration tests** are black-box tests that hit the real running system
through the Apache proxy (`http://localhost:8080`) -- no mocks. They are
skipped by default so a plain `mvn test` never fails just because the stack
isn't running:

```bash
docker compose up --build -d
mvn test -pl integration-tests -DskipITs=false
```

They cover: full login → browse catalog → enroll → appears in schedule →
seat count drops → drop → disappears from current schedule → seat count
restored; admin CRUD on students through the proxy; unauthenticated
requests being rejected by every service; a bad-password login being
rejected; and the frontend being served at `/ui`.

## Architecture notes / where things live

- `db/migration/` -- the single source of truth for the `school_db` schema
  (`V1__init.sql`) and its sample data (`V2__seed_data.sql`), copied into
  each data-owning service at build time.
- `proxy/httpd-app.conf` -- the `Alias /ui` + SPA fallback + `ProxyPass`
  rules for each `/*-api` prefix.
- `docker-compose.yml` -- wires `postgres`, the six services, and `proxy`
  together on one Docker network; `.env` holds the dev-only secrets
  (`JWT_SECRET`, `INTERNAL_API_TOKEN`) and the shared database's name and
  credentials (`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`).
- Every service's `application.properties` leaves its datasource
  credentials unset by default -- Quarkus Dev Services fills them in for
  `mvn quarkus:dev` / `mvn test`, and docker-compose injects the same
  `QUARKUS_DATASOURCE_*` env vars (pointing at `school_db`) into every
  data-owning container.

## Known limitations (sample scope)

- All demo accounts share one password; `POST /auth-api/auth/register`
  exists for creating more.
- Sharing one database means sharing one Postgres role, so nothing at the
  database level stops a service from reading another's tables -- that
  boundary is now a code convention rather than a permission. Per-service
  roles with table-level grants would restore it, at the cost of also
  granting `REFERENCES` across the foreign keys.
- No token refresh -- JWTs are valid for 8 hours.
- No pagination on list endpoints (fine at this seed-data scale).
- No health-check gating between the proxy and the backends in
  docker-compose -- see the startup note above.
