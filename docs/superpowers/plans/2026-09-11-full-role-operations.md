# Full Role Operations Repair Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make CUSTOMER, SALESMAN, CASHIER, MANAGER and ADMIN workflows complete, role-safe and production-verifiable while eliminating the Render 502 startup regression.

**Architecture:** Keep the existing Spring Boot MVC, Thymeleaf, Spring Security and MySQL architecture. Complete the existing employee-management boundary rather than adding a new subsystem, preserve the current order/payment/product permission matrix, and extend live smoke coverage so production role failures are detected automatically.

**Tech Stack:** Java 17, Spring Boot 4.1.1, Spring MVC, Spring Security, Spring Data JPA, Thymeleaf, Flyway, MySQL, Maven, GitHub Actions, Render.

**Spec:** `docs/superpowers/specs/2026-09-06-full-operations-media-repair-design.md`

## Global Constraints

- CUSTOMER cannot access staff routes.
- SALESMAN handles order operations but cannot manage products, staff or payments.
- CASHIER handles payment operations and read-only order visibility but cannot mutate order status.
- MANAGER manages products, SALESMAN/CASHIER accounts and order status, but cannot create/remove administrators or managers.
- ADMIN has full staff operations but administrator-account creation/removal remains protected.
- Staff account disablement must disable authentication as well as the employee profile.
- Staff removal is ADMIN-only, must reject self-removal, and must never remove an ADMIN account through staff management.
- Existing customer cart, checkout, order, invoice and profile ownership rules remain unchanged.
- Production schema remains Flyway-managed and `ddl-auto=none`.

---

### Task 1: Restore Render availability

**Files:**
- Runtime configuration: Render environment for `srv-dac0oqafngtc73fij2e0`

- [x] **Step 1:** Confirm the 502 root cause in Render logs.
- [x] **Step 2:** Restore `SPRING_MAIN_LAZY_INITIALIZATION=true`, leaving Flyway enabled.
- [ ] **Step 3:** Verify Tomcat binds to `$PORT` on `0.0.0.0`, Flyway remains at schema v2, and no port-scan timeout follows.
- [ ] **Step 4:** Verify `/actuator/health`, login and authenticated dashboard behavior.

### Task 2: Complete staff account lifecycle with TDD

**Files:**
- Create: `src/main/java/bd/edu/seu/jerseysee/dto/EmployeeUpdateDTO.java`
- Modify: `src/main/java/bd/edu/seu/jerseysee/service/EmployeeService.java`
- Modify: `src/main/java/bd/edu/seu/jerseysee/controller/EmployeeController.java`
- Modify: `src/main/resources/templates/staff/employees/list.html`
- Create: `src/main/resources/templates/staff/employees/edit.html`
- Test: `src/test/java/bd/edu/seu/jerseysee/controller/EmployeeLifecycleWebTest.java`
- Test: `src/test/java/bd/edu/seu/jerseysee/service/EmployeeServiceLifecycleTest.java`

- [x] **Step 1:** Add failing web-route tests for update, enable/disable and admin-only removal.
- [ ] **Step 2:** Verify the new tests fail against the create-only implementation.
- [ ] **Step 3:** Add an update DTO with optional password rotation and normal employment-field validation.
- [ ] **Step 4:** Implement update, enable/disable and guarded removal in `EmployeeService`.
- [ ] **Step 5:** Add controller routes and Thymeleaf controls/forms.
- [ ] **Step 6:** Add service tests for role restrictions, self-protection, password preservation/rotation and synchronized active/enabled state.
- [ ] **Step 7:** Run full Maven verification.

### Task 3: Lock down the role-operation matrix

**Files:**
- Modify tests under `src/test/java/bd/edu/seu/jerseysee/controller/`
- Modify `.github/workflows/live-smoke.yml`

- [ ] **Step 1:** Add role-matrix tests proving CUSTOMER is blocked from staff routes.
- [ ] **Step 2:** Prove SALESMAN can access staff orders and order-status mutation but not payments/products/employees.
- [ ] **Step 3:** Prove CASHIER can access/confirm payments but cannot mutate order status or manage products/employees.
- [ ] **Step 4:** Prove MANAGER can manage products/orders and SALESMAN/CASHIER accounts, but cannot delete staff or create a MANAGER.
- [ ] **Step 5:** Prove ADMIN can perform all permitted staff operations.
- [ ] **Step 6:** Extend live smoke to exercise the production admin/customer paths and a temporary staff account lifecycle with cleanup.

### Task 4: Production release and verification

**Files:**
- PR branch `fix/full-role-operations-20260911`

- [ ] **Step 1:** Require green Maven and Docker CI.
- [ ] **Step 2:** Review the final PR diff for authorization or data-loss regressions.
- [ ] **Step 3:** Merge to `main` and let Render auto-deploy.
- [ ] **Step 4:** Verify Render deploy status, startup logs, Flyway, health, dashboard, customer operations and role-specific staff operations.
- [ ] **Step 5:** Confirm there are no new HTTP 500/502 responses or application exceptions after the production smoke run.
