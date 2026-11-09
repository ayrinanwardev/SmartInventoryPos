#!/usr/bin/env bash
# Copies the rows from your local PostgreSQL into Supabase, once.
#
# Order of operations:
#   1. Start the backend against Supabase once (WITHOUT the dev profile), so Flyway creates
#      the empty tables there. Stop it.
#   2. Run this script. It refuses to run if Supabase already has data, so it can't duplicate rows.
#
# Usage (Git Bash on Windows works too; needs pg_dump and psql on PATH):
#   LOCAL_DB='postgresql://postgres:LOCALPASS@localhost:5432/pos' \
#   SUPABASE_DB='postgresql://postgres.PROJECTREF:PASSWORD@HOST.pooler.supabase.com:5432/postgres?sslmode=require' \
#   ./backend/scripts/copy-local-data-to-supabase.sh
set -euo pipefail

: "${LOCAL_DB:?Set LOCAL_DB to your local PostgreSQL URL}"
: "${SUPABASE_DB:?Set SUPABASE_DB to the Supabase session-pooler URL}"

# Parents before children, so foreign keys are satisfied as rows arrive.
TABLES=(users products sales sale_items stock_adjustments)

echo "Checking that Supabase has the schema and no data yet..."
existing=$(psql "$SUPABASE_DB" -v ON_ERROR_STOP=1 -tAc "
  SELECT (SELECT count(*) FROM users) + (SELECT count(*) FROM products) + (SELECT count(*) FROM sales)
       + (SELECT count(*) FROM sale_items) + (SELECT count(*) FROM stock_adjustments)") || {
  echo "Could not read the tables in Supabase. Start the backend against Supabase once first (step 1)." >&2
  exit 1
}
if [[ "$existing" != "0" ]]; then
  echo "Supabase already has $existing rows. Refusing to copy, to avoid duplicates." >&2
  exit 1
fi

table_args=()
for t in "${TABLES[@]}"; do table_args+=(--table="public.$t"); done

echo "Copying ${TABLES[*]} ..."
# --data-only: Flyway already created the schema. Identity sequence positions are included,
# so new rows continue after the copied ids. One transaction: all rows arrive, or none.
pg_dump "$LOCAL_DB" --data-only --no-owner --no-privileges "${table_args[@]}" \
  | psql "$SUPABASE_DB" -v ON_ERROR_STOP=1 --single-transaction -q >/dev/null

echo "Row counts (local -> Supabase):"
for t in "${TABLES[@]}"; do
  printf '  %-18s %s -> %s\n' "$t" \
    "$(psql "$LOCAL_DB" -tAc "SELECT count(*) FROM $t")" \
    "$(psql "$SUPABASE_DB" -tAc "SELECT count(*) FROM $t")"
done
echo "Done."
