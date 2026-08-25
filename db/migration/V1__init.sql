-- Schema for the whole system. All five tables live in one database
-- (`school_db`), so the links between them are real foreign keys rather than
-- loose UUID columns that only line up by convention.
--
-- These files live once, here in /db/migration, and are applied once by the
-- one-shot `db-migrate` job in docker-compose.yml before any service is
-- allowed to start. They're also copied into each data-owning service's
-- resources at build time (see the maven-resources-plugin execution in the
-- parent pom), purely so that service's own Dev Services database gets the
-- same schema in `mvn quarkus:dev` and `mvn test`.
--
-- Tables are created parent-first so each REFERENCES clause has something to
-- point at.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE faculty (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name         VARCHAR(100)  NOT NULL,
    last_name          VARCHAR(100)  NOT NULL,
    email              VARCHAR(255)  NOT NULL UNIQUE,
    department         VARCHAR(100)  NOT NULL,
    title              VARCHAR(100)  NOT NULL,
    hire_date          DATE          NOT NULL,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_faculty_email ON faculty (email);

CREATE TABLE student (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name         VARCHAR(100)  NOT NULL,
    last_name          VARCHAR(100)  NOT NULL,
    email              VARCHAR(255)  NOT NULL UNIQUE,
    date_of_birth      DATE          NOT NULL,
    enrollment_status  VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_student_email ON student (email);

-- A class is taught by exactly one faculty member. ON DELETE RESTRICT because
-- faculty_id is NOT NULL and a class with no instructor is meaningless -- so
-- deleting a faculty member who still teaches something is refused by the
-- database (faculty-service turns that into a 409 rather than a 500).
CREATE TABLE course_class (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_code   VARCHAR(20)   NOT NULL,
    title         VARCHAR(200)  NOT NULL,
    description   TEXT,
    semester      VARCHAR(20)   NOT NULL,
    faculty_id    UUID          NOT NULL,
    faculty_name  VARCHAR(200)  NOT NULL,
    capacity      INT           NOT NULL,
    seats_taken   INT           NOT NULL DEFAULT 0,
    room          VARCHAR(50),
    day_of_week   VARCHAR(20),
    start_time    VARCHAR(10),
    end_time      VARCHAR(10),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_course_class_faculty
        FOREIGN KEY (faculty_id) REFERENCES faculty (id) ON DELETE RESTRICT
);

CREATE INDEX idx_course_class_semester ON course_class (semester);
CREATE INDEX idx_course_class_faculty_id ON course_class (faculty_id);

-- One row per (student, class) enrollment. Both links cascade: an enrollment
-- has no meaning once either the student or the class it points at is gone.
CREATE TABLE schedule_entry (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id     UUID          NOT NULL,
    class_id       UUID          NOT NULL,
    course_code    VARCHAR(20)   NOT NULL,
    course_title   VARCHAR(200)  NOT NULL,
    semester       VARCHAR(20)   NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'ENROLLED',
    enrolled_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_schedule_entry_student
        FOREIGN KEY (student_id) REFERENCES student (id) ON DELETE CASCADE,
    CONSTRAINT fk_schedule_entry_class
        FOREIGN KEY (class_id) REFERENCES course_class (id) ON DELETE CASCADE
);

CREATE INDEX idx_schedule_entry_student_id ON schedule_entry (student_id);
CREATE INDEX idx_schedule_entry_student_id_status ON schedule_entry (student_id, status);
CREATE INDEX idx_schedule_entry_class_id ON schedule_entry (class_id);

-- A login account optionally points at the student or faculty record it
-- belongs to (an ADMIN account points at neither). Both columns are nullable,
-- so ON DELETE SET NULL leaves the account intact but unlinked.
CREATE TABLE app_user (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20) NOT NULL,
    student_id    UUID NULL,
    faculty_id    UUID NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_app_user_student
        FOREIGN KEY (student_id) REFERENCES student (id) ON DELETE SET NULL,
    CONSTRAINT fk_app_user_faculty
        FOREIGN KEY (faculty_id) REFERENCES faculty (id) ON DELETE SET NULL
);

CREATE INDEX idx_app_user_username ON app_user (username);
CREATE INDEX idx_app_user_student_id ON app_user (student_id);
CREATE INDEX idx_app_user_faculty_id ON app_user (faculty_id);
