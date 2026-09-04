# JerseySee Modern Football Storefront Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild JerseySee into a modern, responsive football ecommerce storefront with a single premium navbar, real product-photo carousel, cleaner merchandising, public demo accounts, Bangladesh pricing, persistent demo product images, and professional footer credit while preserving the existing Spring Boot commerce workflows.

**Architecture:** Keep the current Spring MVC + Thymeleaf + Spring Security + JPA architecture. UI behavior remains progressive-enhancement JavaScript in `static/js/app.js`; production catalog initialization remains idempotent in `DemoDataInitializer`, with a small production-safe resource image seeding helper writing through the existing database-backed `ProductImageRepository`/`ProductImage` model. Uploaded kit photos ship as classpath demo resources and are copied into the DB only for recognized managed demo products that have no image.

**Tech Stack:** Java 17, Spring Boot, Spring MVC, Thymeleaf, Spring Security, Spring Data JPA, MySQL/Aiven, H2 tests, vanilla JavaScript, CSS, Docker, GitHub Actions, Render.

**Spec:** `docs/superpowers/specs/2026-09-04-modern-football-storefront-design.md`

## Global Constraints

- Preserve Spring Boot controllers/services/domain boundaries and existing cart, checkout, orders, inventory, printing, staff/admin, Aiven and Render behavior.
- One compact sticky storefront header; do not keep the current second blue category navbar.
- Warm ivory/white dominant surface, midnight navy text/footer, restrained cobalt, burgundy and gold accents.
- Homepage hero must use a responsive product carousel with real uploaded kit photography.
- Fan Edition price: `৳750`; Player Edition price: `৳1,100`; Retro price: `৳1,299`.
- Public demo identities: `customer@jerseysee.demo` and `admin@jerseysee.demo`, password `Demo123!`; secret production administrator credentials remain separate.
- Production product imagery remains database-backed and must survive Render restarts.
- Do not scrape retailer photos; use supplied kit photos plus original JerseySee SVG artwork for product categories where no user photo exists.
- Footer copyright: `© 2026 JerseySee. Designed & developed by Azizul Hakim Omor.`
- Explicit responsive coverage for desktop, 1024px, 768px and 375–430px widths.
- Preserve semantic labels, keyboard focus, reduced-motion support, mobile navigation and accessible carousel controls.
- Development happens on `design/modern-football-storefront`, never directly on `main`.
- Because the execution sandbox cannot clone GitHub, GitHub Actions is the authoritative RED/GREEN Maven + Docker verifier.

---

### Task 1: Lock the new storefront contract with failing tests

**Files:**
- Modify: `src/test/java/bd/edu/seu/jerseysee/controller/StorefrontTemplateContractTest.java`
- Create: `src/test/java/bd/edu/seu/jerseysee/config/ModernDemoCatalogTest.java`

**Interfaces:**
- Consumes: existing templates, `DemoDataInitializer`, `UserRepository`, `ProductRepository`.
- Produces: explicit contracts for the single-navbar shell, carousel hooks, demo-login autofill hooks, professional footer credit, public demo accounts, requested BDT prices and managed demo product names.

- [ ] **Step 1: Add a failing storefront shell contract**

Add a test equivalent to:

```java
@Test
void modernStorefrontUsesSingleNavigationProductCarouselAndProfessionalFooter() throws IOException {
    String navigation = read(TEMPLATES.resolve("fragments/navigation.html"));
    String home = read(TEMPLATES.resolve("home/index.html"));
    String login = read(TEMPLATES.resolve("auth/login.html"));
    String footer = read(TEMPLATES.resolve("fragments/footer.html"));
    String javascript = read(STATIC.resolve("js/app.js"));

    assertThat(navigation)
            .contains("data-storefront-header", "data-primary-shop-nav")
            .doesNotContain("class=\"category-nav\"");
    assertThat(home).contains("data-product-carousel", "data-carousel-slide", "data-carousel-next", "data-carousel-prev");
    assertThat(login).contains("data-demo-account", "data-demo-email", "data-demo-password");
    assertThat(footer).contains("© 2026 JerseySee. Designed &amp; developed by Azizul Hakim Omor.");
    assertThat(javascript).contains("data-product-carousel", "data-demo-account", "prefers-reduced-motion");
}
```

- [ ] **Step 2: Add a failing demo-catalog behavior test**

Create `ModernDemoCatalogTest` using the demo H2 profile and assert:

```java
assertThat(userRepository.findByEmail("customer@jerseysee.demo")).isPresent();
assertThat(userRepository.findByEmail("admin@jerseysee.demo")).isPresent();
assertThat(productRepository.findAll()).anySatisfy(product -> {
    assertThat(product.getName()).contains("Barcelona");
    assertThat(product.getBasePrice()).isEqualByComparingTo("750.00");
});
assertThat(productRepository.findAll()).anySatisfy(product -> {
    assertThat(product.getJerseyEdition()).isEqualTo(JerseyEdition.PLAYER);
    assertThat(product.getBasePrice()).isEqualByComparingTo("1100.00");
});
assertThat(productRepository.findAll()).anySatisfy(product -> {
    assertThat(product.getJerseyEdition()).isEqualTo(JerseyEdition.RETRO);
    assertThat(product.getBasePrice()).isEqualByComparingTo("1299.00");
});
```

- [ ] **Step 3: Push the test-only commit and verify RED in GitHub Actions**

Expected: Maven verify fails because the current navbar still has `.category-nav`, the home page lacks carousel hooks, the login lacks demo autofill controls, footer credit is absent, and demo catalog still uses `@demo.local` plus legacy prices.

- [ ] **Step 4: Commit**

```bash
git add src/test/java/bd/edu/seu/jerseysee/controller/StorefrontTemplateContractTest.java \
        src/test/java/bd/edu/seu/jerseysee/config/ModernDemoCatalogTest.java
git commit -m "test: define modern storefront contracts"
```

---

### Task 2: Replace the two-row header and weak brand mark

**Files:**
- Modify: `src/main/resources/templates/fragments/navigation.html`
- Modify: `src/main/resources/static/images/brand-mark.svg`
- Modify: `src/main/resources/static/css/jerseysee.css`
- Modify: `src/main/resources/static/js/app.js`

**Interfaces:**
- Consumes: existing `currentUser`, `currentRole`, `cartCount`, `/catalog` query routes, logout POST flow.
- Produces: `data-primary-shop-nav`, responsive mobile drawer, compact search/account/cart actions, accessible dropdown category menu, original JS monogram crest.

- [ ] **Step 1: Implement one sticky light header**

Replace the current primary + blue category nav pair with one structure:

```html
<header class="store-header" th:fragment="navigation(activePage)" data-storefront-header>
    <div class="store-promise"><div class="section-shell">Free delivery over ৳5,000 · Easy 7-day size exchange</div></div>
    <div class="store-nav section-shell">
        <button class="menu-toggle" type="button" data-mobile-menu-toggle aria-controls="mobileMenu" aria-expanded="false">...</button>
        <a class="brand" th:href="@{/}">...</a>
        <nav id="mobileMenu" class="primary-shop-nav" data-primary-shop-nav data-mobile-menu aria-label="Primary navigation">...</nav>
        <div class="nav-actions">...</div>
    </div>
</header>
```

Primary links: `New In`, `Jerseys`, `Player Edition`, `Retro`, `Boots`, `Accessories`; keep dashboard/profile/orders links in an authenticated account section/drawer rather than crowding the shopping links.

- [ ] **Step 2: Replace brand-mark SVG**

Use an original minimal shield/monogram with paths only, containing the approved palette values `#07142c`, `#1646c8`, `#b99552`, `#f7f3ea`; avoid a jersey/avatar silhouette.

- [ ] **Step 3: Add responsive header CSS**

Add scoped `.store-header`, `.store-promise`, `.store-nav`, `.primary-shop-nav`, `.nav-cluster`, `.mobile-account-links` rules. At `max-width: 900px`, hide desktop shopping links, display the hamburger and expose a fixed/sliding drawer; at `max-width: 520px`, shorten search to an icon/expandable control and keep cart/account touch targets at least 44px.

- [ ] **Step 4: Preserve keyboard/mobile behavior**

Reuse existing `data-mobile-menu-toggle` logic; ensure Escape closes the drawer and focus-visible styles remain intact.

- [ ] **Step 5: Run GitHub Actions checkpoint**

Expected: navbar-related new contract turns GREEN; carousel/login/catalog contracts remain RED.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/templates/fragments/navigation.html \
        src/main/resources/static/images/brand-mark.svg \
        src/main/resources/static/css/jerseysee.css \
        src/main/resources/static/js/app.js
git commit -m "feat: rebuild premium storefront navigation"
```

---

### Task 3: Rebuild the homepage around a real product slideshow and lighter merchandising

**Files:**
- Modify: `src/main/resources/templates/home/index.html`
- Modify: `src/main/resources/static/css/jerseysee.css`
- Modify: `src/main/resources/static/js/app.js`
- Modify: `src/main/resources/templates/fragments/product-card.html`

**Interfaces:**
- Consumes: `featuredProducts`, existing catalog routes, product image route `/products/{id}/image` used by product cards.
- Produces: `data-product-carousel`, `data-carousel-slide`, `data-carousel-next`, `data-carousel-prev`, `data-carousel-dots`; lighter product rails and section hierarchy.

- [ ] **Step 1: Replace the vector hero with a semantic carousel**

The first section after navigation is:

```html
<section class="product-carousel" data-product-carousel aria-roledescription="carousel" aria-label="Featured football kits">
  <div class="carousel-viewport">
    <article class="carousel-slide is-active" data-carousel-slide aria-hidden="false">...</article>
    <article class="carousel-slide" data-carousel-slide aria-hidden="true">...</article>
    <article class="carousel-slide" data-carousel-slide aria-hidden="true">...</article>
  </div>
  <button type="button" data-carousel-prev aria-label="Previous featured product">...</button>
  <button type="button" data-carousel-next aria-label="Next featured product">...</button>
  <div data-carousel-dots>...</div>
</section>
```

Slides link to managed seeded products when available; use Thymeleaf `featuredProducts` to render real `/products/{id}/image` photos dynamically rather than hard-coding club-image paths into the hero.

- [ ] **Step 2: Replace the colored collection mosaic**

Remove `collection-grid` from the homepage. Add photo/product-led sections:
- New Arrivals product rail from `featuredProducts`.
- Edition links rendered as three low-height editorial cards: Fan, Player, Retro.
- Top Clubs text rail linking catalog `clubOrCountry` queries.
- Football Essentials small category cards for Boots, Footballs, Training, Accessories.
- Compact custom-printing split section.
- Trust strip and editorial CTA.

- [ ] **Step 3: Make product cards less boxy**

Use image-first cards with no thick background panels: neutral photo surface, minimal 1px border only where needed, restrained shadow, edition tag, BDT price, subtle arrow, 4/5 desktop image ratio and 1/1.15 mobile ratio.

- [ ] **Step 4: Implement carousel JavaScript**

For each `[data-product-carousel]`:

```javascript
const slides = [...carousel.querySelectorAll('[data-carousel-slide]')];
const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
let index = Math.max(0, slides.findIndex(slide => slide.classList.contains('is-active')));
const show = next => { /* wrap index; update is-active + aria-hidden + dots */ };
prev?.addEventListener('click', () => show(index - 1));
next?.addEventListener('click', () => show(index + 1));
if (!reduceMotion && slides.length > 1) timer = window.setInterval(() => show(index + 1), 6500);
```

Pause auto-advance on pointer/focus interaction and provide touch swipe threshold without blocking normal vertical scroll.

- [ ] **Step 5: Add responsive carousel/home CSS**

Desktop: split copy/product composition with photo on 50–60% width. Tablet: retain two-column hero with smaller type. Mobile: full-bleed image with readable overlay or stacked copy, 44px controls, no clipped headings.

- [ ] **Step 6: Run GitHub Actions checkpoint**

Expected: carousel/template contract GREEN; remaining demo catalog/login contracts may still be RED.

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/templates/home/index.html \
        src/main/resources/templates/fragments/product-card.html \
        src/main/resources/static/css/jerseysee.css \
        src/main/resources/static/js/app.js
git commit -m "feat: add photo-led responsive storefront carousel"
```

---

### Task 4: Redesign login and footer, including demo account autofill

**Files:**
- Modify: `src/main/resources/templates/auth/login.html`
- Modify: `src/main/resources/templates/fragments/footer.html`
- Modify: `src/main/resources/static/css/jerseysee.css`
- Modify: `src/main/resources/static/js/app.js`

**Interfaces:**
- Consumes: existing `/login` POST fields `username` and `password`, registration route, current user/footer state.
- Produces: `data-demo-account`, `data-demo-email`, `data-demo-password`; copyright credit.

- [ ] **Step 1: Replace split-screen login**

Use a single centered commerce auth shell with a restrained side/photo accent only on wide screens. Keep the exact POST field names and CSRF behavior. Add demo cards below the form:

```html
<div class="demo-login-grid" aria-label="Demo accounts">
  <button type="button" class="demo-account" data-demo-account data-demo-email="customer@jerseysee.demo" data-demo-password="Demo123!">
    <span>Customer demo</span><strong>Browse, cart &amp; checkout</strong><small>Use demo account</small>
  </button>
  <button type="button" class="demo-account" data-demo-account data-demo-email="admin@jerseysee.demo" data-demo-password="Demo123!">
    <span>Admin demo</span><strong>Review store operations</strong><small>Use demo account</small>
  </button>
</div>
```

- [ ] **Step 2: Add autofill behavior**

```javascript
document.querySelectorAll('[data-demo-account]').forEach(button => {
  button.addEventListener('click', () => {
    const email = document.getElementById('username');
    const password = document.getElementById('password');
    if (!email || !password) return;
    email.value = button.dataset.demoEmail || '';
    password.value = button.dataset.demoPassword || '';
    email.dispatchEvent(new Event('input', { bubbles: true }));
    password.dispatchEvent(new Event('input', { bubbles: true }));
    password.focus();
  });
});
```

- [ ] **Step 3: Replace footer newsletter slab with compact commerce footer**

Keep shop/account/customer care links, add project credit, and render:

```html
<span>© 2026 JerseySee. Designed &amp; developed by Azizul Hakim Omor.</span>
```

- [ ] **Step 4: Run GitHub Actions checkpoint**

Expected: login/footer template contracts GREEN; demo data behavior remains RED until Task 5.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/templates/auth/login.html \
        src/main/resources/templates/fragments/footer.html \
        src/main/resources/static/css/jerseysee.css \
        src/main/resources/static/js/app.js
git commit -m "feat: polish authentication and footer"
```

---

### Task 5: Replace legacy demo users/catalog with Bangladesh football-store data

**Files:**
- Modify: `src/main/java/bd/edu/seu/jerseysee/config/DemoDataInitializer.java`
- Modify: `src/test/java/bd/edu/seu/jerseysee/config/DemoDataInitializerTest.java`
- Test: `src/test/java/bd/edu/seu/jerseysee/config/ModernDemoCatalogTest.java`

**Interfaces:**
- Consumes: existing deterministic `demoSeedKey` reconciliation, category/product/variant repositories, password encoder.
- Produces: stable managed product seed keys, public demo user emails, requested BDT prices and realistic club catalog.

- [ ] **Step 1: Switch public demo identity emails**

Seed at minimum:

```java
ensureCustomer("demo.user.customer", "Demo Customer", "customer@jerseysee.demo");
ensureStaff("demo.user.admin", "Demo Administrator", "admin@jerseysee.demo", Role.ADMIN,
        "DEMO-ADM-001", "Administrator", "55000.00");
```

Keep salesman/cashier/manager local/demo identities as needed for staff workflow tests, but do not expose them on the public login page.

- [ ] **Step 2: Seed requested jersey catalog**

Use deterministic product keys such as:

```text
demo.product.barcelona-home-fan
demo.product.barcelona-away-player
demo.product.barcelona-third-player
demo.product.real-madrid-home-fan
demo.product.real-madrid-away-player
demo.product.real-madrid-third-player
demo.product.real-madrid-retro
demo.product.arsenal-home-fan
demo.product.arsenal-away-player
demo.product.arsenal-third-player
demo.product.chelsea-home-fan
demo.product.chelsea-away-player
demo.product.chelsea-third-player
demo.product.liverpool-home-fan
demo.product.liverpool-away-player
demo.product.manchester-city-home-fan
demo.product.manchester-city-away-player
demo.product.manchester-city-third-player
demo.product.manchester-united-away-player
demo.product.manchester-united-third-player
demo.product.ac-milan-retro
demo.product.juventus-94-95-retro
```

Prices:
- all `JerseyEdition.FAN` examples: `750.00`
- all `JerseyEdition.PLAYER` examples: `1100.00`
- all `JerseyEdition.RETRO` examples: `1299.00`

- [ ] **Step 3: Seed football essentials**

Add categories/products around:
- Training Football `899.00`
- Match Football `1299.00`
- Mini Supporter Ball `499.00`
- Indoor Futsal Shoes `2299.00`
- Training Sneakers `2650.00`
- Premium Football Boots `3499.00`
- Training Top `899.00`
- Training Trousers `999.00`
- Coach Jacket `1850.00`
- Supporter Cap `450.00`
- Wristband `199.00`
- Gym Sack `399.00`
- Shin Guard `550.00`

Every product keeps at least two deterministic variants so existing stock/variant tests remain meaningful.

- [ ] **Step 4: Update legacy seed tests intentionally**

Replace assertions tied to `metro-city-home-fan`, old prices, and `@demo.local` with the new canonical product/user keys. Preserve collision/idempotency tests by applying them to `demo.product.barcelona-home-fan` and its canonical SKUs.

- [ ] **Step 5: Run GitHub Actions checkpoint**

Expected: new demo behavior tests GREEN, existing deterministic/idempotency tests GREEN.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/bd/edu/seu/jerseysee/config/DemoDataInitializer.java \
        src/test/java/bd/edu/seu/jerseysee/config/DemoDataInitializerTest.java \
        src/test/java/bd/edu/seu/jerseysee/config/ModernDemoCatalogTest.java
git commit -m "feat: seed Bangladesh football demo catalog"
```

---

### Task 6: Persist supplied kit photography into the production database

**Files:**
- Create: `src/main/java/bd/edu/seu/jerseysee/config/DemoProductImageInitializer.java`
- Create: `src/test/java/bd/edu/seu/jerseysee/config/DemoProductImageInitializerTest.java`
- Create binary resources under: `src/main/resources/demo-images/`
- Create original SVG resources: `src/main/resources/demo-images/jerseysee-match-ball.svg`, `src/main/resources/demo-images/jerseysee-football-boots.svg`

**Interfaces:**
- Consumes: `ProductRepository.findByDemoSeedKey(String)`, `ProductImageRepository`, `ProductImage`, classpath `ResourceLoader`.
- Produces: idempotent DB image rows and product metadata for managed demo products only.

- [ ] **Step 1: Write a failing image seeding test**

Test production-like initialization against H2 by constructing the initializer directly with repositories and a `DefaultResourceLoader`. Assert:

```java
Product product = productRepository.findByDemoSeedKey("demo.product.barcelona-home-fan").orElseThrow();
assertThat(product.getStoredImageName()).startsWith("demo-");
assertThat(product.getImageContentType()).isEqualTo("image/jpeg");
assertThat(product.getImageSize()).isPositive();
assertThat(productImageRepository.findById(product.getStoredImageName())).isPresent();
```

Run the initializer twice and assert no duplicate image rows. Also assert an existing custom `storedImageName` is left unchanged.

- [ ] **Step 2: Implement a resource-image mapping**

Use a deterministic map, e.g.:

```java
Map.ofEntries(
    entry("demo.product.barcelona-home-fan", new DemoImage("barcelona-home.jpg", "image/jpeg")),
    entry("demo.product.real-madrid-home-fan", new DemoImage("real-madrid-home.jpg", "image/jpeg")),
    entry("demo.product.ac-milan-retro", new DemoImage("ac-milan-retro.jpg", "image/jpeg"))
)
```

For each recognized managed product with no image metadata, load `classpath:/demo-images/<file>`, save `new ProductImage(storedName, bytes)`, then set `storedImageName`, `originalImageName`, `imageContentType`, `imageSize` and save product. Use deterministic stored names like `demo-barcelona-home-fan.jpg` so reruns stay stable.

- [ ] **Step 3: Add the 22 supplied kit photos**

Normalize the user-supplied `.jfif` files to `.jpg` resource names without re-encoding when the bytes are valid JPEG. Map each to its matching demo product.

- [ ] **Step 4: Add original neutral football/boot SVGs**

Create simple original JerseySee SVG illustrations with no third-party marks, used only for demo items that lack supplied photography.

- [ ] **Step 5: Run GitHub Actions checkpoint**

Expected: image initializer tests GREEN; all existing image persistence/controller tests stay GREEN.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/bd/edu/seu/jerseysee/config/DemoProductImageInitializer.java \
        src/test/java/bd/edu/seu/jerseysee/config/DemoProductImageInitializerTest.java \
        src/main/resources/demo-images
git commit -m "feat: persist demo product photography"
```

---

### Task 7: Lighten catalog/product pages and complete responsive behavior

**Files:**
- Modify: `src/main/resources/templates/catalog/list.html`
- Modify: `src/main/resources/templates/catalog/detail.html`
- Modify: `src/main/resources/static/css/jerseysee.css`
- Modify: `src/main/resources/static/js/app.js` only if filter behavior requires it.
- Modify: `src/test/java/bd/edu/seu/jerseysee/controller/StorefrontTemplateContractTest.java` only for behavior contracts, not style snapshots.

**Interfaces:**
- Consumes: existing `CatalogFilter`, pagination query parameters, filter drawer hooks, purchase panel hooks.
- Produces: compact catalog header, image-led grid, lighter filters, responsive product detail without breaking existing hooks.

- [ ] **Step 1: Add a failing contract that the oversized catalog hero is gone**

Assert catalog still contains `data-filter-drawer`, `data-commerce-grid`, all filter query parameter preservation, but does not contain `premium-catalog-hero` and instead contains `compact-catalog-header`.

- [ ] **Step 2: Implement compact catalog header**

Replace the large dark hero with breadcrumb/title/count/sort area. Keep desktop filter rail, but reduce visual weight; preserve mobile filter drawer.

- [ ] **Step 3: Refine product detail**

Keep `data-product-stage`, `data-purchase-panel`, `data-size-selector`, printing fields and stock logic. Reduce giant typography, use real photo stage, clearer price/size CTA hierarchy and responsive single-column mobile layout.

- [ ] **Step 4: Consolidate responsive CSS**

Add explicit styles for `1200px`, `900px`, `768px`, `520px` with no horizontal overflow, progressive product grid columns, single-column login/product detail, mobile filter drawer, carousel control sizing, footer stacking.

- [ ] **Step 5: Run GitHub Actions checkpoint**

Expected: all Maven tests GREEN.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/templates/catalog/list.html \
        src/main/resources/templates/catalog/detail.html \
        src/main/resources/static/css/jerseysee.css \
        src/main/resources/static/js/app.js \
        src/test/java/bd/edu/seu/jerseysee/controller/StorefrontTemplateContractTest.java
git commit -m "feat: refine responsive catalog experience"
```

---

### Task 8: Final verification, PR, merge and deployment check

**Files:**
- Verify: `.github/workflows/ci.yml`
- Verify: `Dockerfile`
- Verify: `render.yaml`
- No production change unless verification identifies a real issue.

**Interfaces:**
- Consumes: completed feature branch.
- Produces: green PR, merged `main`, Render auto-deployment target commit, final live-link/demo credential handoff.

- [ ] **Step 1: Open/update the implementation PR**

PR title: `Modernize JerseySee football storefront`.

PR body must summarize navigation, carousel, demo catalog/pricing, persistent images, responsive/auth/footer work and explicitly state no payment-processing change.

- [ ] **Step 2: Verify branch CI**

Required jobs: Maven `clean verify` and production Docker build. Do not merge if either is failing.

- [ ] **Step 3: Review diff for security/data hazards**

Confirm:
- no Aiven/Render passwords or JDBC secrets committed,
- public demo credentials are only `Demo123!`, not the secret production administrator password,
- product photo resources contain no user secrets,
- `render.yaml` still targets `main` with auto-deploy.

- [ ] **Step 4: Merge to `main`**

Use squash merge after CI is green.

- [ ] **Step 5: Verify main CI and deployment readiness**

Wait for main Maven and Docker jobs. Confirm the new commit is the Render auto-deploy target. If a Render dashboard connector/browser is available, inspect the deploy and health endpoint; otherwise verify the public service URL when known and report the exact remaining dashboard-only action without claiming success prematurely.

- [ ] **Step 6: LinkedIn handoff**

Prepare the achievement post with the verified live URL. Publishing requires a connected LinkedIn publishing integration and explicit confirmation at execution time; never claim publication without a successful external write response.
