# Contributing

Every change starts as a GitHub issue, lands through a pull request, and every commit names its issue.
Work is planned on the [project board](https://github.com/users/ayrinanwardev/projects/1) and in [issues](https://github.com/ayrinanwardev/SmartInventoryPos/issues).

## Workflow

1. **Issue first.** Open (or pick) an issue that says what's wrong or what's needed and how we'll know it's done.
2. **Branch per issue:** `<type>/<issue>-<short-slug>`, for example `fix/12-checkout-deadlock`.
3. **Test first for bugs.** Add a test that fails on `main` and reproduces the bug, then fix it.
4. **Pull request** titled like a commit, with `Fixes #<issue>` in the description so merging closes the issue.
5. **CI must be green**: commit messages, backend tests (including PostgreSQL concurrency tests) and app unit tests.

## Commit messages

```
<type>(<scope>): <summary in the imperative> (#<issue>)

<optional body: why the change was needed and what it changes>
```

| Part | Allowed values |
|---|---|
| `type` | `feat`, `fix`, `test`, `refactor`, `perf`, `docs`, `build`, `ci`, `chore` |
| `scope` | optional: `sale`, `product`, `stock`, `auth`, `app`, `sync`, `db`, `ci`, `docs` |
| `(#<issue>)` | required: the issue this commit belongs to |

Examples:

```
fix(sale): lock products in id order so checkouts can't deadlock (#12)
test(sale): reproduce concurrent duplicate sales on PostgreSQL (#13)
docs: explain the commit protocol (#9)
```

The same rule is checked twice:

- **Locally**, by the hook in `.githooks/commit-msg`. Turn it on once per clone:
  ```bash
  git config core.hooksPath .githooks
  ```
- **In CI**, on every pull request, for each commit on the branch.

## Running the tests

```bash
./gradlew :backend:test           # API, domain rules and multi-threaded PostgreSQL tests
./gradlew :app:testDebugUnitTest  # Android domain and use-case tests
```

The multi-threaded tests in `backend/src/test/kotlin/smart_inventory_backend/concurrency` need real PostgreSQL,
because H2 doesn't lock rows or detect deadlocks the same way. They use, in order:

1. `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD`, if set: point these at an **empty, throwaway** database.
   The tests truncate every table.
2. Otherwise a Testcontainers PostgreSQL, when Docker is available (it is on GitHub Actions).
3. Otherwise they are skipped.
