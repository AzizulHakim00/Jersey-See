# JerseySee Premium Storefront Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the assignment-style public interface with the approved premium football ecommerce storefront without changing pricing, authentication, inventory, or authorization behavior.

**Architecture:** Keep the existing Spring MVC controllers and Thymeleaf model contracts. Build the visual system from reusable Thymeleaf fragments, original SVG campaign assets, a reorganized CSS token/component layer, and progressively enhanced JavaScript whose controls remain functional without JavaScript.

**Tech Stack:** Java 17, Spring Boot 4.1.1, Spring MVC, Thymeleaf, Spring Security, HTML5, CSS, vanilla JavaScript, JUnit 5, AssertJ, MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-03-premium-storefront-design.md`

## Global Constraints

- Preserve existing controller URLs, form field names, CSRF behavior, role checks, server-authoritative pricing, and stock rules.
- Use an original deep-navy, warm-ivory, cobalt, crimson, and muted-gold identity without club trademarks or copyrighted store layouts.
- Essential navigation, filtering, customization, authentication, cart, checkout, and order actions must work without JavaScript.
- Support desktop 1280–1600 px, tablet 768–1199 px, and mobile 320–767 px.
- Retain visible focus states, semantic landmarks, useful alternative text, sufficient contrast, keyboard operation, and reduced-motion support.

---

### Task 1: Lock the storefront contract in tests

**Files:**
- Modify: `src/test/java/bd/edu/seu/jerseysee/controller/StorefrontTemplateContractTest.java`
- Modify: `src/test/java/bd/edu/seu/jerseysee/controller/PageRenderingTest.java`

**Interfaces:**
- Consumes: existing template paths and MVC model fixtures.
- Produces: assertions for `data-storefront-header`, `data-commerce-card`, `data-mobile-menu`, `data-filter-drawer`, service assurances, purchase controls, cart summary, checkout review, account navigation, and authorization-aware actions.

- [ ] **Step 1: Write failing template contract assertions**

```java
assertThat(home).contains("data-campaign-hero", "data-service-strip", "data-commerce-card");
assertThat(navigation).contains("data-storefront-header", "data-mobile-menu");
assertThat(catalog).contains("data-filter-drawer", "data-commerce-grid");
assertThat(detail).contains("data-product-stage", "data-purchase-panel");
assertThat(cart).contains("data-cart-lines", "data-order-summary");
```

- [ ] **Step 2: Run the focused contract tests and confirm RED**

Run: `./mvnw -Dtest=StorefrontTemplateContractTest,PageRenderingTest test`

Expected: FAIL because the approved commerce data hooks and sections do not exist in the current templates.

- [ ] **Step 3: Preserve controller/model rendering coverage**

Add MockMvc assertions for the home, catalog, detail, cart, checkout, login, registration, profile, and order pages so each returns its existing view name with representative model data.

- [ ] **Step 4: Run focused tests again**

Run: `./mvnw -Dtest=StorefrontTemplateContractTest,PageRenderingTest test`

Expected: the new contract assertions remain RED while all unchanged MVC setup compiles.

- [ ] **Step 5: Commit the RED contract**

```bash
git add src/test/java/bd/edu/seu/jerseysee/controller/StorefrontTemplateContractTest.java src/test/java/bd/edu/seu/jerseysee/controller/PageRenderingTest.java
git commit -m "test: define premium storefront contract"
```

### Task 2: Build the shared premium commerce shell

**Files:**
- Create: `src/main/resources/templates/fragments/footer.html`
- Create: `src/main/resources/templates/fragments/product-card.html`
- Create: `src/main/resources/templates/fragments/service-strip.html`
- Modify: `src/main/resources/templates/fragments/navigation.html`
- Modify: `src/main/resources/templates/fragments/messages.html`
- Modify: `src/main/resources/static/images/brand-mark.svg`
- Create: `src/main/resources/static/images/hero-kit.svg`
- Create: `src/main/resources/static/images/custom-printing.svg`
- Modify: `src/main/resources/static/css/jerseysee.css`
- Modify: `src/main/resources/static/js/app.js`

**Interfaces:**
- Consumes: `currentUser`, `currentRole`, and `cartItemCount` global model attributes; `Product` fields and `/product-images/{storedName}`.
- Produces: Thymeleaf fragments `navigation`, `footer`, `productCard(product)`, and `serviceStrip`; JavaScript hooks `data-mobile-menu`, `data-filter-drawer`, `aria-expanded`, and `aria-hidden`.

- [ ] **Step 1: Implement the shell fragments with server-valid links and forms**

```html
<header class="storefront-header" data-storefront-header>
  <button type="button" data-mobile-menu-toggle aria-expanded="false" aria-controls="mobileMenu">Menu</button>
  <nav id="mobileMenu" data-mobile-menu aria-label="Primary navigation">...</nav>
</header>
```

Keep the POST logout form visible and usable without JavaScript, and gate role links with the existing Thymeleaf role expressions.

- [ ] **Step 2: Add original brand-neutral campaign SVG assets**

Create accessible decorative kit artwork using navy, cobalt, crimson, ivory, and gold shapes. Do not include club crests, sponsor marks, or copied kit patterns.

- [ ] **Step 3: Reorganize the stylesheet around tokens and shared components**

```css
:root {
  --ink: #07142c;
  --ivory: #f7f3ea;
  --cobalt: #1646c8;
  --crimson: #b51f3b;
  --gold: #b99552;
  --focus: #ffcf4a;
}
```

Retain existing staff selectors, add commerce shell/card/button/form states, focus-visible rules, breakpoints, and `@media (prefers-reduced-motion: reduce)`.

- [ ] **Step 4: Implement keyboard-safe progressive enhancements**

Use event listeners to toggle menu/filter state, synchronize `aria-expanded`, close drawers on Escape, restore focus, and never add `hidden` to essential server-rendered controls until JavaScript has initialized.

- [ ] **Step 5: Run contract tests**

Run: `./mvnw -Dtest=StorefrontTemplateContractTest test`

Expected: shared-shell assertions PASS; page-specific assertions remain RED.

- [ ] **Step 6: Commit the shell**

```bash
git add src/main/resources/templates/fragments src/main/resources/static
git commit -m "feat: add premium commerce shell"
```

### Task 3: Rebuild home, catalog, and product merchandising

**Files:**
- Modify: `src/main/resources/templates/home/index.html`
- Modify: `src/main/resources/templates/catalog/list.html`
- Modify: `src/main/resources/templates/catalog/detail.html`

**Interfaces:**
- Consumes: existing `featuredProducts`, catalog `products`, `filter`, `categories`, detail `product`, `cartItemForm`, variants, roles, and validation errors.
- Produces: premium campaign homepage, complete filterable commerce grid, and role-aware product purchase panel without controller changes.

- [ ] **Step 1: Replace the homepage assignment hero with merchandising sections**

Render a seasonal hero, service strip, six collection links that resolve to supported catalog queries, featured products through `productCard(product)`, custom-printing campaign, and footer.

- [ ] **Step 2: Rebuild the catalog without changing query parameters**

Keep `keyword`, `categoryId`, `productType`, `clubOrCountry`, `edition`, `kitType`, `size`, `available`, `minimumPrice`, `maximumPrice`, and pagination in every form/link; expose the same form as a desktop sidebar and mobile drawer.

- [ ] **Step 3: Rebuild the detail purchase experience**

Keep `variantId`, `quantity`, `printingType`, `customName`, and `customNumber` form names; show stock per variant, server validation, customer/guest/staff actions, image fallback, and delivery/exchange assurances.

- [ ] **Step 4: Run rendering and contract tests**

Run: `./mvnw -Dtest=StorefrontTemplateContractTest,PageRenderingTest test`

Expected: home/catalog/detail assertions PASS with no Thymeleaf rendering exceptions.

- [ ] **Step 5: Commit merchandising pages**

```bash
git add src/main/resources/templates/home src/main/resources/templates/catalog
git commit -m "feat: rebuild storefront merchandising"
```

### Task 4: Rebuild cart, checkout, authentication, and account pages

**Files:**
- Modify: `src/main/resources/templates/cart/view.html`
- Modify: `src/main/resources/templates/orders/checkout.html`
- Modify: `src/main/resources/templates/orders/list.html`
- Modify: `src/main/resources/templates/orders/detail.html`
- Modify: `src/main/resources/templates/auth/login.html`
- Modify: `src/main/resources/templates/auth/register.html`
- Modify: `src/main/resources/templates/profile/edit.html`
- Modify: `src/main/resources/templates/dashboard/index.html`
- Modify: `src/main/resources/templates/error.html`
- Modify: `src/main/resources/templates/error/403.html`
- Modify: `src/main/resources/templates/error/404.html`

**Interfaces:**
- Consumes: existing form objects, cart lines/totals, checkout fields, order/payment data, role dashboard data, flash messages, validation errors, and CSRF injection.
- Produces: premium customer journey and visually consistent account/staff entry points with unchanged form actions and HTTP methods.

- [ ] **Step 1: Implement cart and checkout layouts**

Render product image, variant, printing, quantity update/removal, subtotal, server-calculated totals, delivery fields, payment method, order review, and sticky desktop summary; keep each POST action as a real HTML form.

- [ ] **Step 2: Implement auth and account layouts**

Use split campaign panels for login/register and consistent account navigation/status components for profile and orders; display `#fields` errors next to their exact fields.

- [ ] **Step 3: Polish dashboard and errors without widening permissions**

Retain every existing role expression and route. Apply shared cards, spacing, typography, buttons, tables, and clear recovery actions.

- [ ] **Step 4: Run rendering, security, and order tests**

Run: `./mvnw -Dtest=StorefrontTemplateContractTest,PageRenderingTest,SecurityAccessTest,OrderControllerTest,OrderOwnershipTest test`

Expected: PASS.

- [ ] **Step 5: Commit the customer journey**

```bash
git add src/main/resources/templates
git commit -m "feat: complete premium customer journey"
```

### Task 5: Verify responsive frontend and push the review branch

**Files:**
- Modify: `README.md`
- Create: `docs/screenshots/storefront-desktop.png`
- Create: `docs/screenshots/storefront-tablet.png`
- Create: `docs/screenshots/storefront-mobile.png`

**Interfaces:**
- Consumes: fully rendered application and demo seed profile.
- Produces: reviewable screenshots, usage notes, green frontend test evidence, and GitHub branch `codex/premium-storefront`.

- [ ] **Step 1: Run the complete test suite and package build**

Run: `./mvnw clean test` and `./mvnw -DskipTests package`

Expected: both commands exit 0.

- [ ] **Step 2: Start the demo profile and capture responsive screenshots**

Run: `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo`

Capture `/` at 1440x1000, 834x1112, and 390x844; inspect navigation, hero, product cards, focus states, overflow, and touch targets.

- [ ] **Step 3: Record frontend usage and screenshots in README**

Document the premium storefront sections, demo start command, customer purchase path, staff path, and screenshot links without changing deployment instructions yet.

- [ ] **Step 4: Commit evidence and push**

```bash
git add README.md docs/screenshots
git commit -m "docs: add premium storefront preview"
git push -u origin codex/premium-storefront
```

- [ ] **Step 5: Open the frontend pull request**

Create a pull request to `main` summarizing public pages, accessibility/progressive enhancement, tests, screenshots, and the separate deployment follow-up.

