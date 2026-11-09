-- Flyway callback: runs after every migrate on PostgreSQL (H2 tests never see it). Safe to run repeatedly.
--
-- Supabase publishes the public schema through its Data API (PostgREST) to the "anon" and
-- "authenticated" roles. This app reaches the database only through the Spring backend, so those
-- roles must not reach any table here, above all users.password_hash.
--
-- Two independent locks, so a mistake in one doesn't expose data:
--   1. Row Level Security on, with no policies: API roles see zero rows.
--   2. No privileges for the API roles on existing or future tables/sequences.
-- The backend connects as the tables' owner, which RLS does not restrict, so the app is unaffected.
-- On a plain local PostgreSQL the API roles don't exist and only step 1 applies (harmlessly).

DO $$
DECLARE
    t record;
    api_roles text;
BEGIN
    FOR t IN
        SELECT tablename FROM pg_tables
        WHERE schemaname = 'public' AND tableowner = current_user
    LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', t.tablename);
    END LOOP;

    SELECT string_agg(quote_ident(rolname), ', ') INTO api_roles
    FROM pg_roles
    WHERE rolname IN ('anon', 'authenticated');

    IF api_roles IS NOT NULL THEN
        EXECUTE format('REVOKE ALL ON ALL TABLES IN SCHEMA public FROM %s', api_roles);
        EXECUTE format('REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM %s', api_roles);
        EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES FROM %s', api_roles);
        EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM %s', api_roles);
    END IF;
END
$$;
