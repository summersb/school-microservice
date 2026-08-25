-- Backs the Idempotency-Key header on POST /register/enroll. Lives in the same
-- shared school_db as everything else (see V1__init.sql) even though only
-- register-service ever reads or writes it: one migration set, applied once by
-- the db-migrate job, is simpler than giving register-service its own private
-- schema for a single bookkeeping table.
--
-- response_status/response_body are NULL while a request is being processed
-- and get filled in once it completes -- that's what lets a second request
-- with the same key tell "still running, wait" (NULL) apart from "done,
-- here's the answer" (filled in) without a separate status column.
CREATE TABLE idempotency_key (
    key             UUID PRIMARY KEY,
    request_hash    TEXT NOT NULL,
    response_status INT,
    response_body   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
