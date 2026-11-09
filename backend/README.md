# Backend: Spring Boot on Supabase PostgreSQL

The Spring Boot API is unchanged. Only where the database lives has moved: from a PostgreSQL on
your PC to [Supabase](https://supabase.com) (hosted PostgreSQL). The Android app still talks only
to this backend, never to Supabase directly.

```
Android app  ──HTTP/JWT──▶  Spring Boot (this folder)  ──JDBC/TLS──▶  Supabase PostgreSQL
```

## 1. Create the Supabase project

1. supabase.com → New project. Pick the region closest to your users (Mumbai `ap-south-1` or
   Singapore `ap-southeast-1` for Bangladesh). Save the database password.
2. Project → **Connect** → **Session pooler**. Copy the connection string. It looks like
   `postgresql://postgres.abcdefghijkl:[YOUR-PASSWORD]@aws-0-ap-south-1.pooler.supabase.com:5432/postgres`

Use the **session pooler**, not "Direct connection" (IPv6-only on the free plan, so it fails on
most home and office networks) and not the "Transaction pooler" (needs `prepareThreshold=0`).

## 2. Configure the backend

```bash
cp backend/.env.example backend/.env
```

Fill in `backend/.env` from the connection string:

| Connection string part | `.env` key |
|---|---|
| `aws-0-…pooler.supabase.com:5432/postgres` | `DB_URL=jdbc:postgresql://aws-0-….pooler.supabase.com:5432/postgres?sslmode=require` |
| `postgres.abcdefghijkl` (the part before `:`) | `DB_USERNAME` |
| your database password | `DB_PASSWORD` |

Also set `JWT_SECRET` (`openssl rand -base64 48`). `backend/.env` is git-ignored.

Every value can also be passed as a real environment variable, which is how a hosting service
(Render, Railway, Fly.io…) would supply it.

## 3. Run

```bash
./gradlew :backend:bootRun
```

On first start Flyway creates the tables in Supabase and locks them away from Supabase's public
Data API (see "Security" below). Check them in Supabase → Table Editor.

**Don't use the `dev` profile against Supabase.** It seeds `manager/manager123` and
`cashier/cashier123` into an empty database. That's fine on your PC; on a hosted database those
become real accounts with well-known passwords.

Run the app as before (emulator → `10.0.2.2:8080`, USB phone → `adb reverse tcp:8080 tcp:8080`).

## 4. (Optional) copy your existing local data

Do this after step 3 has created the tables, while Supabase is still empty:

```bash
LOCAL_DB='postgresql://postgres:LOCALPASS@localhost:5432/pos' \
SUPABASE_DB='postgresql://postgres.PROJECTREF:PASSWORD@aws-0-….pooler.supabase.com:5432/postgres?sslmode=require' \
./backend/scripts/copy-local-data-to-supabase.sh
```

It copies all rows in one transaction, keeps ids and sequence positions, refuses to run if
Supabase already has data, and prints row counts for both sides. It needs `pg_dump` and `psql`;
on Windows, run it from Git Bash. Your `pg_dump` should not be newer than Supabase's PostgreSQL
major version (Supabase → Settings → Infrastructure shows it).

## Going back to local PostgreSQL

Delete or rename `backend/.env`. With no `DB_URL`, the backend uses
`jdbc:postgresql://localhost:5432/pos` exactly as before.

## Security: why `afterMigrate.sql` exists

Supabase automatically publishes every table in the `public` schema through a REST API (PostgREST)
to the `anon` and `authenticated` roles. With Supabase's default grants, anyone with the project's
anon key could read `users.password_hash`.

`src/main/resources/db/vendor/postgresql/afterMigrate.sql` runs after every Flyway migration and:

1. turns on Row Level Security for every table (no policies, so the API roles see nothing), and
2. revokes all privileges from `anon` and `authenticated`, including on tables added later.

The backend connects as the tables' owner, which RLS doesn't restrict, so the app keeps working.
It runs only on PostgreSQL; the H2 test database skips it.

## Configuration reference

| Variable | Default | Notes |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/pos` | Add `?sslmode=require` for Supabase |
| `DB_USERNAME` | `postgres` | Supabase pooler: `postgres.<project-ref>` |
| `DB_PASSWORD` | empty | |
| `DB_POOL_SIZE` | `5` | Supabase plans cap connections |
| `JWT_SECRET` | built-in dev secret (logs a warning) | ≥ 32 chars in real use |
| `PORT` | `8080` | Hosting services usually set this |
