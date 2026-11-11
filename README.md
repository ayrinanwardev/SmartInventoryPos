# Smart Inventory POS

**An offline-first point-of-sale and inventory system for small shops.**
Android (Kotlin, Jetpack Compose) on the counter, a Spring Boot API behind it, PostgreSQL on Supabase.
Sales keep working when the internet doesn't, and sync without ever being counted twice.

![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-4169E1?logo=postgresql&logoColor=white)
[![CI](https://github.com/ayrinanwardev/SmartInventoryPos/actions/workflows/ci.yml/badge.svg)](https://github.com/ayrinanwardev/SmartInventoryPos/actions/workflows/ci.yml)
![Tests](https://img.shields.io/badge/tests-89-success)
[![Board](https://img.shields.io/badge/planning-project%20board-1d76db?logo=github)](https://github.com/users/ayrinanwardev/projects/1)

## Demo: one app, three roles

<table>
  <tr>
    <th width="33%">👔 Manager</th>
    <th width="33%">🧾 Cashier</th>
    <th width="33%">🛒 Customer</th>
  </tr>
  <tr>
    <td><img src="docs/media/manager.gif" width="100%" alt="Manager adds Basmati Rice, sells 3 with a ৳10 discount for ৳230, checks low stock and today's revenue"></td>
    <td><img src="docs/media/cashier.gif" width="100%" alt="Cashier sells 2 lentils with a ৳20 discount, removes 2 damaged units successfully, then is blocked from removing 25"></td>
    <td><img src="docs/media/customer.gif" width="100%" alt="New customer signs up and buys 2 sugar at full price; the cart has no discount field"></td>
  </tr>
  <tr>
    <td valign="top">Adds a product, sells 3 × ৳80 with ৳10 off (<b>৳230</b>), stock drops 20 → 17, checks low stock and today's revenue.</td>
    <td valign="top">Sells 2 × ৳120 with ৳20 off (<b>৳220</b>), removes 2 damaged units ✅, then tries 25. The <b>server</b> refuses ❌: cashiers may change at most 20 at once.</td>
    <td valign="top">Signs up and buys at full price. No discount field, and the server would ignore one anyway.</td>
  </tr>
</table>

<p align="center">
  <i>Recorded on a real phone (Xiaomi, Android 13) against the real backend and Supabase database.</i><br>
  <a href="docs/media/demo.mp4"><b>▶ Watch the full 2.5-minute demo (MP4)</b></a>
</p>

---

## Contents

- [Demo](#demo-one-app-three-roles)
- [What it does](#what-it-does)
- [Architecture](#architecture)
- [Data model](#data-model)
- [Engineering decisions](#engineering-decisions)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Run it locally](#run-it-locally)
- [Testing](#testing) (including [multi-threaded tests](#multi-threaded-tests))
- [How work is planned](#how-work-is-planned)
- [API](#api)
- [Security](#security)
- [Known limitations and roadmap](#known-limitations-and-roadmap)

## What it does

One app, three roles. The server decides what each role may do; the app only reflects it.

| | Manager | Cashier | Customer |
|---|:---:|:---:|:---:|
| Sell, search by name or barcode | ✅ | ✅ | ✅ (self-checkout) |
| Give a discount | ✅ | ✅ | ❌ always ৳0 |
| Adjust stock | ✅ any amount | ✅ up to ±20 per change | ❌ |
| Add, edit, deactivate products | ✅ | ❌ | ❌ |
| Low-stock list | ✅ | ✅ | ❌ |
| Sales history, today's revenue | ✅ all sales | ❌ | own orders only |

Anyone can sign up, and a public sign-up always creates a **customer**. Staff accounts are created by the system, never through the app.

## Architecture

### System

```mermaid
flowchart LR
    subgraph Phone["📱 Android app"]
        UI["Compose UI<br/>+ ViewModels"]
        UC["Use cases<br/>(domain)"]
        Room[("Room<br/>offline cache +<br/>pending sales")]
        WM["WorkManager<br/>SyncWorker"]
        UI --> UC
        UC --> Room
        WM --> Room
    end

    subgraph Server["☕ Spring Boot API"]
        SEC["Spring Security<br/>JWT filter"]
        SVC["Services<br/>+ policies"]
        JPA["JPA / Hibernate<br/>pessimistic locks"]
        FLY["Flyway<br/>migrations"]
        SEC --> SVC --> JPA
    end

    subgraph Supabase["🐘 Supabase"]
        PG[("PostgreSQL 17<br/>RLS on, public API locked")]
    end

    UC -- "HTTPS + JWT<br/>Retrofit / OkHttp" --> SEC
    WM -- "POST /api/sales<br/>(idempotent by clientId)" --> SEC
    JPA -- "JDBC over TLS<br/>HikariCP, session pooler" --> PG
    FLY -. "schema on startup" .-> PG
```

The phone never talks to the database. Every rule (prices, discounts, stock limits, who may do what) is enforced by the API, so a modified app can't bypass it.

### Android app: clean architecture

```mermaid
flowchart TB
    subgraph ui["ui: Compose"]
        Screens["Screens<br/>Login · POS · Stock · Manage · My orders"]
        VM["ViewModels<br/>StateFlow UI state"]
        Screens --> VM
    end
    subgraph domain["domain: pure Kotlin, no Android imports"]
        UseCases["Use cases<br/>Checkout · SyncPendingSales · RefreshCatalog"]
        Models["Models<br/>Cart · Money · Product · Role"]
        RepoIf["Repository interfaces"]
        UseCases --> Models
        UseCases --> RepoIf
    end
    subgraph data["data"]
        RepoImpl["Repository implementations"]
        Local["Room DAOs"]
        Remote["Retrofit API<br/>+ auth interceptor"]
        Session["Encrypted session<br/>Android Keystore"]
        RepoImpl --> Local
        RepoImpl --> Remote
    end
    VM --> UseCases
    RepoImpl -. implements .-> RepoIf
```

Dependencies point inward. The domain layer has no Android or framework code, so the business rules (cart maths, discount checks, sync decisions) are unit-tested on the JVM in milliseconds. Hilt wires the layers together.

### Offline-first checkout

The core flow: a sale is saved on the phone first, then uploaded. Losing the network mid-sale can't lose the sale or charge it twice.

```mermaid
sequenceDiagram
    autonumber
    actor C as Cashier
    participant App as App (CheckoutUseCase)
    participant DB as Room
    participant W as SyncWorker
    participant API as Spring Boot
    participant PG as PostgreSQL

    C->>App: Checkout
    App->>DB: Save sale as PENDING with a new clientId (UUID),<br/>reduce local stock (one transaction)
    App-->>C: "Sale saved" (works offline)
    App->>W: Schedule sync (needs network, exponential backoff)
    W->>API: POST /api/sales {clientId, items, discount}
    API->>PG: Sale with this clientId already exists?
    alt First time
        API->>PG: SELECT … FOR UPDATE each product, check stock,<br/>apply discount policy, insert sale (one transaction)
        API-->>W: 201 Created
    else Retry after a timeout
        API-->>W: Same sale as before (nothing written)
    end
    W->>DB: Mark SYNCED, or REJECTED on 400/404/409
    W->>API: Refresh catalog (server stock wins)
```

## Data model

Five tables, created and versioned by Flyway (`backend/src/main/resources/db/migration`). Hibernate only validates them at startup and never changes the schema.

```mermaid
erDiagram
    USERS ||--o{ SALES : "makes (cashier_id)"
    SALES ||--|{ SALE_ITEMS : contains
    PRODUCTS ||--o{ SALE_ITEMS : "sold as"
    PRODUCTS ||--o{ STOCK_ADJUSTMENTS : "changed by"

    USERS {
        bigint id PK
        varchar username UK
        varchar password_hash "BCrypt"
        varchar role "MANAGER | CASHIER | CUSTOMER"
    }
    PRODUCTS {
        bigint id PK
        varchar name
        varchar barcode UK "optional"
        numeric price
        int stock "CHECK stock >= 0"
        int low_stock_threshold
        boolean active "soft delete"
        timestamptz updated_at
    }
    SALES {
        bigint id PK
        uuid client_id UK "idempotency key from the phone"
        bigint cashier_id FK
        numeric subtotal
        numeric discount
        numeric total
        timestamptz created_at
    }
    SALE_ITEMS {
        bigint id PK
        bigint sale_id FK
        bigint product_id FK
        int qty
        numeric unit_price "price at time of sale"
    }
    STOCK_ADJUSTMENTS {
        bigint id PK
        bigint product_id FK
        int delta
        int stock_after
        varchar reason
        varchar performed_by
        varchar role
        timestamptz created_at
    }
```

`sale_items.unit_price` copies the price at the moment of sale, so changing a product's price later never rewrites past sales. `stock_adjustments` is an append-only audit log: who changed stock, by how much, and why.

## Engineering decisions

The trade-offs worth talking about, and why I made them.

**1. Idempotent checkout instead of "hope the network works."**
The phone generates a UUID (`clientId`) per sale, and the database has a unique constraint on it. If an upload times out after the server already saved the sale, the retry returns the saved sale instead of creating a second one. This is what makes automatic background retries safe.

**2. Pessimistic row locks for stock, taken in a fixed order.**
Two cashiers selling the last item at the same moment must not both succeed. `SaleService` locks each product row (`SELECT … FOR UPDATE`) inside the sale transaction, and a `CHECK (stock >= 0)` constraint is the last line of defence. Overselling returns `409 Conflict`. Locks are taken in ascending product id, so two sales listing the same products in opposite order can't deadlock ([#3](https://github.com/ayrinanwardev/SmartInventoryPos/issues/3)), and restocking locks the row too, so it can't overwrite a sale ([#5](https://github.com/ayrinanwardev/SmartInventoryPos/issues/5)). Optimistic locking would have meant retry loops on the hottest rows; with a handful of tills, a short lock is simpler and correct. All of this is proven by [multi-threaded tests on real PostgreSQL](#testing).

**3. The server is the source of truth; the phone is a cache.**
The app reduces stock locally so the screen is right while offline, then refreshes from the server after every sync. A sale the server rejects (for example, out of stock) is marked `REJECTED`, shown to the user, and its local stock change is undone by the next refresh. No merge logic, no conflicts to resolve by hand.

**4. Business rules as small, swappable policies.**
Discounts and stock limits live in `DiscountPolicy` and `StockAdjustmentPolicy`: single-method interfaces with one role-based implementation each. New rules (a discount cap, a loyalty discount) become a new class, not edits scattered through services. Both are unit-tested in isolation.

**5. Kept Spring Boot when moving to Supabase.**
Supabase can expose the database straight to the app (REST and row-level security), which would remove the backend entirely. I rejected that: the stock locking, idempotency and role rules would have to be rewritten as SQL functions and RLS policies. Instead only the database moved. A Flyway callback (`afterMigrate.sql`) turns on RLS and revokes Supabase's public roles on every table, so the auto-generated REST API can't read `users.password_hash`.

**6. Connection handling for a hosted database.**
The Supabase pooler and home routers silently drop idle connections after about 2 minutes, which showed up as random "cannot reach server" errors. Fixed with HikariCP keep-alives (30 s), a 5-minute connection lifetime and a JDBC socket timeout, so a dead connection fails fast instead of hanging.

## Tech stack

| Layer | Choices |
|---|---|
| **Android** | Kotlin 2.2 · Jetpack Compose (Material 3) · MVVM with `StateFlow` · Hilt · Room · Retrofit + OkHttp · WorkManager · Android Keystore (AES-GCM) for the session token · min SDK 24 |
| **Backend** | Kotlin · Spring Boot 3.5 · Spring Security (stateless JWT, jjwt) · Spring Data JPA / Hibernate · Flyway · Bean Validation · BCrypt |
| **Database** | PostgreSQL 17 on Supabase (session pooler, TLS) · H2 for tests |
| **Testing** | JUnit 5 · Spring Boot Test · Testcontainers (PostgreSQL) · kotlinx-coroutines-test |
| **Delivery** | GitHub Issues + Projects · Conventional Commits with issue ids · GitHub Actions CI |

## Project structure

```
SmartInventoryPos/
├── app/                                  Android app
│   └── src/main/java/com/example/smartinventorypos/
│       ├── domain/        models, repository interfaces, use cases (pure Kotlin)
│       ├── data/          Room (local), Retrofit (remote), repositories, encrypted session
│       ├── sync/          WorkManager worker and scheduler for pending sales
│       ├── ui/            Compose screens, ViewModels, navigation, theme
│       └── di/            Hilt modules
├── backend/                              Spring Boot API
│   ├── src/main/kotlin/smart_inventory_backend/
│   │   ├── auth/          JWT, security config, users, sign-up and login
│   │   ├── product/       catalogue, create-or-restock, soft delete
│   │   ├── sale/          checkout, discount policy, history, daily report
│   │   ├── stock/         stock adjustments, adjustment policy, audit log
│   │   └── common/        error handling, paging, clock
│   ├── src/main/resources/db/   Flyway migrations + PostgreSQL-only lock-down script
│   └── README.md          database setup (Supabase or local PostgreSQL)
└── docs/media/            demo recording
```

## Run it locally

**Requirements:** JDK 17+, Android Studio, and a PostgreSQL database (a free Supabase project, or PostgreSQL on your machine).

**1. Backend**

```bash
cp backend/.env.example backend/.env    # then fill in DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET
./gradlew :backend:bootRun
```

Flyway creates the tables on first start. To get demo accounts on a **local** database, run with the `dev` profile (`SPRING_PROFILES_ACTIVE=dev`), which seeds a manager and a cashier. Never use `dev` against a shared or hosted database. Full database setup, including Supabase, is in [backend/README.md](backend/README.md).

**2. Android app**

Open the project in Android Studio and run the `app` configuration.

- **Emulator:** works out of the box (the app uses `10.0.2.2`, the emulator's address for your PC).
- **Phone over USB:** `adb reverse tcp:8080 tcp:8080`
- **Hosted backend:** add `pos.api.url=https://your-host/` to `local.properties`.

## Testing

```bash
./gradlew :backend:test          # 60 tests: API, domain rules, time zone, JWT, PostgreSQL concurrency
./gradlew :app:testDebugUnitTest # 29 tests: cart, money, use cases, app concurrency
```

All of them run in [CI](https://github.com/ayrinanwardev/SmartInventoryPos/actions/workflows/ci.yml) on every pull request.

**API tests** drive the real HTTP API end to end: login and wrong passwords, role checks, overselling (409), idempotent retries, the cashier's ±20 limit (403), and that customers get no discount.

### Multi-threaded tests

Tills sell at the same time and the phone retries uploads in the background, so the rules must hold under real concurrency. H2 doesn't lock rows or detect deadlocks like PostgreSQL, so the backend's concurrency tests run on **real PostgreSQL** (Testcontainers in CI). Each test starts N threads behind a latch so they fire at the same instant, then checks every outcome.

| Scenario | Threads | Must hold |
|---|---|---|
| Many cashiers sell the last item | 16 | exactly 1 sale; the rest get `INSUFFICIENT_STOCK` |
| 40 sales of a 20-unit product | 40 | exactly 20 sales, stock 0, never negative |
| Sales listing the same products in opposite order | 8 × 25 rounds | no deadlocks (was 166 of 200 failing, [#3](https://github.com/ayrinanwardev/SmartInventoryPos/issues/3)) |
| The same sale arriving 10 times at once | 10 | one sale, every copy gets it back (was a 500, [#4](https://github.com/ayrinanwardev/SmartInventoryPos/issues/4)) |
| Restocking while selling | 30 | no lost updates (was 89 instead of 130, [#5](https://github.com/ayrinanwardev/SmartInventoryPos/issues/5)) |
| Simultaneous stock adjustments | 30 | all applied; replaying the audit log reproduces every level |

On the app side ([#8](https://github.com/ayrinanwardev/SmartInventoryPos/issues/8)): a burst of checkout taps saves one sale; 8 threads adding to the cart never lose an item; 200 checkouts on real threads while sync runs overlap all end `SYNCED`, each stored once on the server.

To run the PostgreSQL tests locally without Docker, point them at an **empty, throwaway** database (they truncate every table):

```bash
TEST_DB_URL=jdbc:postgresql://localhost:5432/pos_concurrency_test TEST_DB_USERNAME=postgres TEST_DB_PASSWORD=... ./gradlew :backend:test
```

With neither Docker nor `TEST_DB_URL`, they're skipped.

## How work is planned

Every change starts as a [GitHub issue](https://github.com/ayrinanwardev/SmartInventoryPos/issues), is tracked on the [project board](https://github.com/users/ayrinanwardev/projects/1), and lands through a pull request that closes it. Bugs get a failing test first. Commits follow a protocol that names the issue:

```
fix(sale): lock products in id order so checkouts can't deadlock (#3)
```

It's checked by a local `commit-msg` hook and again in CI. Details in [CONTRIBUTING.md](CONTRIBUTING.md).

## API

All endpoints except `/api/auth/**` need `Authorization: Bearer <token>`. Tokens last 24 hours.

| Method | Path | Who | Purpose |
|---|---|---|---|
| `POST` | `/api/auth/login` | anyone | Get a JWT |
| `POST` | `/api/auth/register` | anyone | Sign up (always a customer) |
| `GET` | `/api/products?q=` | signed in | Search active products |
| `GET` | `/api/products/low-stock` | manager, cashier | Products at or under their threshold |
| `POST` | `/api/products` | manager | Create, or restock if the barcode or name exists |
| `PUT` | `/api/products/{id}` | manager | Edit |
| `DELETE` | `/api/products/{id}` | manager | Deactivate (soft delete; history is kept) |
| `POST` | `/api/products/{id}/stock` | manager, cashier | Adjust stock with a reason |
| `GET` | `/api/stock/adjustments` | manager | Stock audit log |
| `POST` | `/api/sales` | signed in | Checkout (idempotent by `clientId`) → `201` |
| `GET` | `/api/sales/mine` | signed in | Own sales or orders |
| `GET` | `/api/sales` | manager | All sales, paged |
| `GET` | `/api/sales/report/today` | manager | Today's revenue |

Errors return a stable JSON shape with a machine-readable code (for example `STOCK_CHANGE_LIMIT_EXCEEDED`), which the app maps to a readable message.

## Security

- Passwords are hashed with **BCrypt**; the API is **stateless** (JWT, no server sessions).
- Authorisation is checked **on the server** with `@PreAuthorize` and the policy classes, never trusted from the app.
- The session token is stored **encrypted** with a key held in the **Android Keystore**.
- Secrets (database password, JWT secret) come from **environment variables** or a git-ignored `.env`. Nothing secret is committed. Outside the `dev` profile the server **refuses to start without `JWT_SECRET`** ([#7](https://github.com/ayrinanwardev/SmartInventoryPos/issues/7)).
- On Supabase, **row-level security is on and public roles are revoked** on every table, so the database is reachable only through this API.

## Known limitations and roadmap

Honest status. Each item is an [open issue](https://github.com/ayrinanwardev/SmartInventoryPos/issues) on the [board](https://github.com/users/ayrinanwardev/projects/1):

- [ ] **Hosting** ([#9](https://github.com/ayrinanwardev/SmartInventoryPos/issues/9)). The backend runs on a PC, so the phone reaches it over USB (`adb reverse`). Next: a Dockerfile and deployment next to the database.
- [ ] **Offline sale price and time** ([#10](https://github.com/ayrinanwardev/SmartInventoryPos/issues/10)). An offline sale is charged at the server's price and time when it syncs; the phone should send both.
- [ ] **Sync retry on 403** ([#11](https://github.com/ayrinanwardev/SmartInventoryPos/issues/11)). A 403 during sync is retried forever instead of being marked rejected.
- [ ] **Stock in product edit** ([#12](https://github.com/ayrinanwardev/SmartInventoryPos/issues/12)). Editing a product can put back an old stock value; stock should change only through audited adjustments.
- [ ] **Refresh tokens** ([#13](https://github.com/ayrinanwardev/SmartInventoryPos/issues/13)). Tokens last 24 hours and carry the role, so a demoted user keeps access until expiry.
- [ ] **Discount limits** ([#14](https://github.com/ayrinanwardev/SmartInventoryPos/issues/14)). Staff may give any discount up to the subtotal; shops need per-role caps.
