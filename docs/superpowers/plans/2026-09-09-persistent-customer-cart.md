# Persistent Customer Cart Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the HTTP-session shopping cart with one MySQL-backed cart per authenticated customer, and make checkout consume and clear that cart atomically only after a successful order is persisted.

**Architecture:** Persist normalized cart lines in `customer_cart_item`, keyed to the authenticated `User` and selected `ProductVariant`. `CartService` becomes the single cart persistence boundary, controllers stop using `@SessionAttributes`, `OrderService` locks the customer cart and product variants in one transaction, and Flyway adds the production table while Hibernate DDL remains disabled in production.

**Tech Stack:** Java 17, Spring Boot 4.1.1, Spring MVC, Spring Security, Spring Data JPA/Hibernate, MySQL, H2 tests, Flyway, Maven, Docker, GitHub Actions, Render.

**Spec:** `docs/superpowers/specs/2026-09-09-persistent-customer-cart-design.md`

## Global Constraints

- Production remains on Render Free; no paid plan change.
- `JERSEYSEE_PUBLIC_DEMO_ENABLED=false` remains enforced and demo seeders must not overwrite database state.
- `spring.jpa.hibernate.ddl-auto=none` remains in the production profile.
- Customer identity must always come from the authenticated principal; no cart endpoint accepts a customer ID from the browser.
- Existing cart quantity range remains 1..10 per logical line.
- Same variant + printing type/name/number remains one logical cart line.
- Cart prices are derived from current product/variant data; stale prices are not persisted in the cart table.
- A failed checkout must leave order state, stock and cart state unchanged.
- Existing database-backed product images, profile edits, staff data, orders and payments must not regress.

---

### Task 1: Production schema migration and persistence contract

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.properties`
- Modify: `src/main/resources/application-production.properties`
- Create: `src/main/resources/db/migration/V2__create_customer_cart_item.sql`
- Test: `src/test/java/bd/edu/seu/jerseysee/config/PersistentCartMigrationContractTest.java`

**Interfaces:**
- Produces the `customer_cart_item` table used by all later tasks.
- Production uses Flyway baseline version 1 and applies V2 while Hibernate production DDL remains `none`.

- [ ] **Step 1: Write the failing migration/config contract test**

Create a test that asserts Flyway dependencies/config are present, production enables Flyway with baseline version 1, `ddl-auto=none` remains, public-demo reseeding remains disabled, and V2 creates `customer_cart_item` with `line_id`, `customer_id`, `product_variant_id`, quantity/printing fields, unique line ID, indexes, and `ON DELETE CASCADE` foreign keys.

- [ ] **Step 2: Run the targeted test and verify RED**

Run: `./mvnw --batch-mode --no-transfer-progress -Dtest=PersistentCartMigrationContractTest test`

Expected: FAIL because Flyway configuration and the V2 migration do not exist yet.

- [ ] **Step 3: Add the minimal Flyway dependency/config/migration implementation**

Add `flyway-core` and `flyway-mysql`; default `spring.flyway.enabled=false`; production `spring.flyway.enabled=true`, `spring.flyway.baseline-on-migrate=true`, `spring.flyway.baseline-version=1`; create only the new cart table/indexes/foreign keys in V2.

- [ ] **Step 4: Verify GREEN**

Run the targeted test, then `./mvnw --batch-mode --no-transfer-progress test -Dtest=DeploymentContractTest,ProductionPersistenceProtectionTest,PersistentCartMigrationContractTest`.

- [ ] **Step 5: Commit**

Commit message: `Add persistent cart database migration`

### Task 2: Persistent cart entity and ownership-safe repository

**Files:**
- Create: `src/main/java/bd/edu/seu/jerseysee/model/CustomerCartItem.java`
- Create: `src/main/java/bd/edu/seu/jerseysee/repository/CustomerCartItemRepository.java`
- Modify: `src/main/java/bd/edu/seu/jerseysee/cart/CartItem.java`
- Test: `src/test/java/bd/edu/seu/jerseysee/repository/CustomerCartItemRepositoryTest.java`

**Interfaces:**
- Produces `CustomerCartItemRepository.findDetailedByCustomerId(Long)`, `findByLineIdAndCustomerId(String, Long)`, `findByCustomerIdForUpdate(Long)`, `totalQuantityForCustomer(Long)`, and `deleteAllByCustomerId(Long)`.
- `CartItem` gains a constructor/factory that accepts the persisted stable `lineId` while preserving current view getters.

- [ ] **Step 1: Write repository persistence/ownership tests first**

Cover save/reload, two-customer isolation, stable line ID, detailed variant/product loading, total quantity, and checkout write-lock lookup.

- [ ] **Step 2: Verify RED**

Run: `./mvnw --batch-mode --no-transfer-progress -Dtest=CustomerCartItemRepositoryTest test`

Expected: FAIL because the entity/repository do not exist.

- [ ] **Step 3: Implement the entity, repository and stable projection constructor**

Entity fields: generated `id`, unique UUID `lineId`, required customer FK, required productVariant FK, quantity, `PrintingType`, nullable printing name/number. Do not persist price or display snapshot fields.

Repository read query should fetch variant/product for display. Checkout lock query should lock cart rows in deterministic row-ID order without relying on browser data.

- [ ] **Step 4: Verify GREEN and existing domain contracts**

Run targeted repository test plus `DomainRelationshipTest` and `AssociationFetchContractTest`.

- [ ] **Step 5: Commit**

Commit message: `Add database cart entity and repository`

### Task 3: Refactor CartService to make MySQL the cart source of truth

**Files:**
- Modify: `src/main/java/bd/edu/seu/jerseysee/service/CartService.java`
- Modify: `src/test/java/bd/edu/seu/jerseysee/service/CartServiceTest.java`
- Create: `src/test/java/bd/edu/seu/jerseysee/service/PersistentCartServiceTest.java`

**Interfaces:**
- `ShoppingCart getCart(User customer)`
- `void add(User customer, AddToCartDTO input)`
- `void updateQuantity(User customer, String lineId, int quantity)`
- `void remove(User customer, String lineId)`
- `int getTotalQuantity(User customer)`
- package-visible/internal locked cart-row loading/clearing support for checkout.

- [ ] **Step 1: Write failing service tests**

Test immediate database write on add, merge behavior, reload after persistence-context clear, quantity update persistence, removal persistence, ownership isolation, current price after product price change, inactive product rejection and navbar count.

- [ ] **Step 2: Verify RED**

Run: `./mvnw --batch-mode --no-transfer-progress -Dtest=PersistentCartServiceTest test`

Expected: FAIL because `CartService` still accepts/mutates a session `ShoppingCart`.

- [ ] **Step 3: Implement minimal database-backed CartService**

Validate enabled customer role, resolve variants from the repository, normalize printing selections using the existing rules, save/flush persistent rows, and project them into `ShoppingCart` using current catalog values. Every line lookup must include the current customer's ID.

- [ ] **Step 4: Verify GREEN and adapt existing CartServiceTest**

Preserve unit-price and printing validation coverage; remove assumptions that mutation of an in-memory session object is authoritative.

- [ ] **Step 5: Commit**

Commit message: `Persist customer cart operations`

### Task 4: Make checkout consume and clear the database cart atomically

**Files:**
- Modify: `src/main/java/bd/edu/seu/jerseysee/service/OrderService.java`
- Modify: `src/test/java/bd/edu/seu/jerseysee/service/OrderServiceTest.java`
- Modify: `src/test/java/bd/edu/seu/jerseysee/model/CheckoutPersistenceTest.java`
- Create: `src/test/java/bd/edu/seu/jerseysee/service/PersistentCartCheckoutTest.java`

**Interfaces:**
- Replace `checkout(User, ShoppingCart, CheckoutDTO)` with `checkout(User, CheckoutDTO)`.
- Within the existing transaction, load/lock customer cart rows, lock variants by sorted variant ID, create order/payment, decrement stock, delete the locked cart rows, and commit once.

- [ ] **Step 1: Write failing atomic-checkout tests**

Cover successful order + stock decrement + cart clear; insufficient stock leaves cart/stock/order unchanged; invalid checkout leaves cart unchanged; second checkout after successful first checkout fails as empty rather than creating a duplicate order.

- [ ] **Step 2: Verify RED**

Run: `./mvnw --batch-mode --no-transfer-progress -Dtest=PersistentCartCheckoutTest test`

- [ ] **Step 3: Implement transactional DB-cart checkout**

Use persistent cart lines only. Re-run printing validation and derive current prices. Delete cart rows only after the order has been persisted inside the same transaction. Any exception must roll back all changes.

- [ ] **Step 4: Verify GREEN plus existing order/payment tests**

Run targeted checkout test, `OrderServiceTest`, `CheckoutPersistenceTest`, `PaymentServiceTest`.

- [ ] **Step 5: Commit**

Commit message: `Checkout persistent carts atomically`

### Task 5: Remove all HTTP-session cart state from MVC and navbar

**Files:**
- Modify: `src/main/java/bd/edu/seu/jerseysee/controller/CartController.java`
- Modify: `src/main/java/bd/edu/seu/jerseysee/controller/OrderController.java`
- Modify: `src/main/java/bd/edu/seu/jerseysee/controller/GlobalModelAttributes.java`
- Modify relevant tests under `src/test/java/bd/edu/seu/jerseysee/controller/`
- Create: `src/test/java/bd/edu/seu/jerseysee/controller/PersistentCartWebFlowTest.java`

**Interfaces:**
- Controllers resolve the customer from `Authentication` via `UserService`.
- Model attribute name `shoppingCart` remains for templates, but each request receives a fresh projection loaded from the database.
- `cartCount` is `CartService.getTotalQuantity(customer)` only for enabled customers; anonymous/staff count is zero.

- [ ] **Step 1: Write failing web-flow tests**

Test authenticated add -> cart view -> simulated new session/login -> same cart visible; navbar count from DB; remove/update survive session replacement; checkout form reloads DB cart; no source controller contains `@SessionAttributes("shoppingCart")`.

- [ ] **Step 2: Verify RED**

Run: `./mvnw --batch-mode --no-transfer-progress -Dtest=PersistentCartWebFlowTest test`

- [ ] **Step 3: Refactor controllers/model advice**

Remove session factories and `HttpSession` cart inspection. On validation errors, reload the database cart before rendering. Successful checkout no longer calls `ShoppingCart.clear()`.

- [ ] **Step 4: Verify GREEN and controller regression suite**

Run `PersistentCartWebFlowTest`, `OrderControllerTest`, `DemoCustomerOperationsTest`, `PageRenderingTest`, `SecurityAccessTest`, and storefront smoke/template tests.

- [ ] **Step 5: Commit**

Commit message: `Remove session cart state from web flow`

### Task 6: Live regression, review, merge and Render deployment

**Files:**
- Modify: `.github/workflows/live-smoke.yml`
- Test: full Maven and Docker CI plus production smoke.

**Interfaces:**
- Live smoke must verify customer authentication and persistent cart recovery after logout/login without leaving accumulating test data.

- [ ] **Step 1: Extend live smoke safely**

Use a cookie jar, CSRF extraction, the existing demo customer account and an existing active in-stock product variant discovered from the rendered catalog/product page. Add one identifiable cart line, log out, log back in, assert the line/cart count remains, then remove the test line through the normal application endpoint. Do not submit a real production order during smoke.

- [ ] **Step 2: Run complete verification**

Run: `./mvnw --batch-mode --no-transfer-progress clean verify`

Run: `docker build --tag jerseysee:verify .`

Expected: both PASS.

- [ ] **Step 3: Review full PR diff**

Confirm no demo reseeding re-enabled, no production DDL update/create, no session cart remains, no user-supplied customer ownership IDs, and no unrelated UI changes.

- [ ] **Step 4: Merge only after PR CI is green**

Merge the reviewed exact head SHA to `main`.

- [ ] **Step 5: Verify Render deployment**

Confirm auto-deployment of the exact merge commit reaches `LIVE`; inspect startup logs for successful Flyway baseline/migration and no schema/foreign-key failure.

- [ ] **Step 6: Verify production behavior freshly**

Require successful live smoke on the exact merge commit, including cart persistence across logout/login. Then perform/observe one controlled redeploy/restart and rerun the cart recovery smoke to prove the cart survives application restart as well as session replacement.

- [ ] **Step 7: Completion gate**

Only claim completion after Maven, Docker, exact Render deploy, Flyway migration, live customer-login/cart-recovery smoke and post-redeploy recovery are all fresh and green.
