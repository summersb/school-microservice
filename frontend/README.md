# Student Enrollment System — Frontend

React + TypeScript + Vite single-page app for the student-enrollment microservices sample.

## Local development

```
npm install
npm run dev
```

This starts the Vite dev server (default `http://localhost:5173`). API calls to `/auth-api`,
`/student-api`, `/faculty-api`, `/classes-api`, `/schedule-api`, and `/register-api` are proxied
by Vite (see `vite.config.ts`) to `http://localhost:8080`, so the backend stack (or the Apache
proxy container in front of it) needs to be reachable there — e.g. via `docker compose up` in the
repository root.

## Production build

```
npm run build
```

Produces static assets in `dist/`, which is what the Apache Docker image copies in to serve the
app under `/ui`, proxying the `*-api` paths to the backend services.

## Notes

- The auth token is kept in memory only (React state) — no `localStorage`/`sessionStorage` — so a
  page refresh logs the user out. This is a deliberate simplification for this sample.
- Demo accounts (password `password123`): `admin` (ADMIN), `susan.miller` / `james.anderson`
  (FACULTY), `alice.johnson`, `brian.smith`, `carla.diaz`, `david.kim`, `emma.wilson` (STUDENT).
