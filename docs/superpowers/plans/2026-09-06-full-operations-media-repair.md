# JerseySee Full Operations and Media Repair Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore full ADMIN and CUSTOMER operations, correct navigation/layout defects, replace placeholder media, and verify the repaired application through tests, Docker, and production smoke checks.

**Architecture:** Preserve the current Spring Boot MVC, Thymeleaf, and database model. Remove only the special demo-admin mutation guard, keep role authorization intact, repair layout in the premium stylesheet/templates, and use the existing demo image/database seeding path for complete catalog media.

**Tech Stack:** Java 17, Spring Boot, Spring Security, Spring MVC, Thymeleaf, JPA/Hibernate, MySQL/Aiven, Maven, Docker, Render.

**Spec:** `docs/superpowers/specs/2026-09-06-full-operations-media-repair-design.md`

## Global Constraints
- Demo admin is fully writable now.
- Demo customer must perform normal customer operations.
- Local/seeded admins must remain fully writable.
- Role boundaries must remain enforced.
- No visible normal seeded product should show “IMAGE COMING SOON”.
- Desktop navigation must not require a horizontal slider.
- Do not introduce a paid Render change.

---

### Task 1: Remove demo-admin read-only restriction

**Files:**
- Modify: `src/main/java/bd/edu/seu/jerseysee/config/SecurityConfig.java`
- Delete: `src/main/java/bd/edu/seu/jerseysee/config/PublicDemoAdminReadOnlyFilter.java`
- Modify/Create test under: `src/test/java/bd/edu/seu/jerseysee/config/`

**Interfaces:**
- Consumes: existing Spring Security role checks.
- Produces: ADMIN requests to `/staff/**` are governed only by normal role/CSRF/controller rules.

- [ ] Write a failing security test that authenticates as `admin@demo.local` with `ROLE_ADMIN`, performs a representative POST to a staff mutation route with CSRF, and asserts it is not blocked by the public-demo filter.
- [ ] Run the focused test and confirm it fails with 403 under the old filter.
- [ ] Remove filter injection and `.addFilterBefore(...)` from `SecurityConfig`; delete the filter class.
- [ ] Run security/config tests and confirm normal CUSTOMER-vs-STAFF boundaries still pass.
- [ ] Commit with `fix: allow demo admin operations`.

### Task 2: Verify and repair admin CRUD flows

**Files:**
- Inspect/modify as required: `AdminProductController.java`, `EmployeeController.java`, `OrderController.java`, `PaymentController.java`
- Inspect/modify matching templates under `templates/staff/**`
- Tests under `src/test/java/bd/edu/seu/jerseysee/controller/`

**Interfaces:**
- Produces: ADMIN can create/edit products, manage employees, change order status, and perform payment actions through rendered forms.

- [ ] Add MockMvc tests for the admin forms/actions using `@WithMockUser(roles="ADMIN")` and CSRF.
- [ ] Run focused tests and record any controller/template/validation failure.
- [ ] Apply the minimum controller/template correction for each reproduced failure.
- [ ] Re-run focused admin tests until green.
- [ ] Commit with `fix: restore admin management flows`.

### Task 3: Verify and repair customer operations

**Files:**
- Inspect/modify as required: `CartController.java`, `OrderController.java`, `ProfileController.java`
- Inspect/modify matching templates under `templates/cart`, `templates/orders`, `templates/profile`, `templates/catalog`
- Tests under `src/test/java/bd/edu/seu/jerseysee/controller/`

**Interfaces:**
- Produces: CUSTOMER can add/update/remove cart items, checkout, view/cancel orders, download invoice, and update profile.

- [ ] Add/extend customer MockMvc tests for cart mutation, checkout, cancellation, and profile update.
- [ ] Run focused tests and identify actual failures.
- [ ] Correct only the reproduced controller/template/service integration issues.
- [ ] Re-run customer tests until green.
- [ ] Commit with `fix: restore customer commerce flows`.

### Task 4: Fix navbar/sidebar overlap and remove unwanted slider behavior

**Files:**
- Modify: `src/main/resources/static/css/storefront-premium-v2.css`
- Modify if needed: `templates/fragments/navigation.html`, `templates/fragments/admin-sidebar.html`, `templates/dashboard/index.html`
- Contract tests under `src/test/java/bd/edu/seu/jerseysee/controller/`

**Interfaces:**
- Produces: retail/customer content starts below sticky header; staff content starts right of sidebar; no desktop horizontal nav scroller.

- [ ] Add contract assertions for one-row header, staff offset, and absence of horizontal nav overflow/slider markup.
- [ ] Run contract tests and confirm the problematic rules/markup fail assertions.
- [ ] Normalize header height/flow and staff content offset; remove any unnecessary desktop scroll/slider rule or control.
- [ ] Run rendering/contract tests.
- [ ] Commit with `fix: stabilize customer and admin navigation layout`.

### Task 5: Replace placeholder imagery and repair seeded database media

**Files:**
- Modify: `src/main/java/bd/edu/seu/jerseysee/config/DemoProductImageInitializer.java`
- Modify/add bundled assets under `src/main/resources/demo-images/` and `src/main/resources/static/images/`
- Modify homepage/login/product templates as needed.
- Modify tests: `DemoProductImageInitializerTest.java`, storefront template/rendering tests.

**Interfaces:**
- Produces: seeded products own valid persisted `ProductImage` records; homepage/login/category visuals have concrete assets; no normal seeded content uses coming-soon text.

- [ ] Add initializer test for repairing a seeded product whose stored image is missing/legacy placeholder.
- [ ] Add template contract assertion that seeded storefront templates do not contain visible “IMAGE COMING SOON”.
- [ ] Run focused tests and confirm failure.
- [ ] Change image initializer so seeded products are repaired when metadata points to missing/placeholder images while remaining idempotent for valid persisted images.
- [ ] Replace homepage/login generic placeholder references/copy with premium bundled assets already in repo or newly added deterministic SVG assets.
- [ ] Run initializer and storefront tests.
- [ ] Commit with `fix: complete seeded storefront media`.

### Task 6: Full verification and production handoff

**Files:**
- No feature files unless verification exposes a root-cause bug.

**Interfaces:**
- Produces: a releasable branch with all tests/builds green.

- [ ] Run `./mvnw -B verify` and require exit 0.
- [ ] Run `docker build -t jerseysee:repair .` and require exit 0.
- [ ] Review diff against `main` for unrelated changes.
- [ ] Create/update PR from `fix/full-operations-and-media` to `main` and wait for CI.
- [ ] Merge only after CI is green.
- [ ] Verify Render deploy and smoke-test `/actuator/health`, `/`, `/login`, demo admin dashboard/actions, and demo customer shopping flow.
- [ ] Report exact final commit/deploy status and any remaining platform-only cold-start limitation.
