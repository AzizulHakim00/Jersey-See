# JerseySee Real Media, Dashboard Polish, and Performance Repair Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace remaining cartoon/SVG storefront media with the user's supplied real football photography, polish customer/admin dashboards toward the approved preview, and remove unnecessary frontend motion that causes a laggy feel.

**Architecture:** Keep all Spring MVC routes, security, database models, and commerce behavior unchanged. Add optimized static photographic assets under `src/main/resources/static/images/media/`, reference them directly from Thymeleaf templates, and make narrowly scoped CSS/JS changes for visual polish and reduced motion cost. Validate the media/template/performance contracts with repository tests, then rely on existing integration tests for business behavior.

**Tech Stack:** Java 17, Spring Boot, Thymeleaf, CSS, vanilla JavaScript, Maven, Docker, GitHub Actions, Render.

**Spec:** `docs/superpowers/specs/2026-09-06-real-media-dashboard-performance-design.md`

## Global Constraints

- Use only user-supplied photographic media for this repair; do not generate new images.
- Do not change admin/customer authorization rules, CRUD routes, checkout logic, database schema, or existing commerce behavior.
- Do not use visibly watermarked stock images in prominent storefront placement.
- Hero media must be eager/high-priority; below-the-fold media must remain lazy/async where applicable.
- Preserve responsive mobile navigation/sidebar behavior while reducing unnecessary animation.

---

### Task 1: Add failing media and performance contract tests

**Files:**
- Modify: `src/test/java/bd/edu/seu/jerseysee/controller/HomeHeroMediaContractTest.java`
- Modify: `src/test/java/bd/edu/seu/jerseysee/controller/StorefrontTemplateContractTest.java`
- Modify: `src/test/java/bd/edu/seu/jerseysee/controller/ModernStorefrontContractTest.java`

**Interfaces:**
- Consumes: existing template/static file text contracts.
- Produces: failing assertions for `/images/media/home-hero.webp`, `/images/media/login-player.webp`, dedicated category assets, dashboard repair stylesheet, and reduced-motion JavaScript.

- [ ] **Step 1: Extend the homepage media contract** to assert that `home/index.html` contains `/images/media/home-hero.webp` and no longer contains `/images/auth-footballer.svg` inside the homepage hero.
- [ ] **Step 2: Extend template contracts** to assert the login template contains `/images/media/login-player.webp`, the homepage category grid references dedicated photographic category assets, and dashboard template loads `/css/storefront-repair.css`.
- [ ] **Step 3: Extend the storefront JS contract** to assert `storefront-premium-v2.js` does not contain `IntersectionObserver` and does not request `behavior: "smooth"` for product rail scrolling.
- [ ] **Step 4: Commit tests only** and run GitHub Actions on the branch. Expected result: Maven verification fails specifically because the new media/template/performance expectations are not implemented yet.

### Task 2: Prepare and commit optimized user-supplied photographic assets

**Files:**
- Create: `src/main/resources/static/images/media/home-hero.webp`
- Create: `src/main/resources/static/images/media/login-player.webp`
- Create: `src/main/resources/static/images/media/category-player.webp`
- Create: `src/main/resources/static/images/media/category-retro.webp`
- Create: `src/main/resources/static/images/media/category-boots.webp`
- Create: `src/main/resources/static/images/media/category-new-arrivals.webp`
- Create: `src/main/resources/static/images/media/campaign-retro.webp`
- Create: `src/main/resources/static/images/media/product-ball.webp`
- Create: `src/main/resources/static/images/media/product-boot-black.webp`
- Create: `src/main/resources/static/images/media/product-boot-white.webp`
- Create: `src/main/resources/static/images/media/product-training-top.webp`

**Interfaces:**
- Consumes: user-uploaded files mounted in `/mnt/data`.
- Produces: normalized static asset paths referenced by templates and CSS.

- [ ] **Step 1:** Convert/crop `messi login page images.jpg` into a wide dark hero crop and a vertical login crop without upscaling beyond source resolution.
- [ ] **Step 2:** Convert `cover 2 images.jpg` / `cover.jpg` into retro category/campaign WebP assets.
- [ ] **Step 3:** Convert the supplied boot images into category/product WebP assets.
- [ ] **Step 4:** Convert supplied football and training-top images into WebP assets; avoid the two visibly watermarked stock-ball images.
- [ ] **Step 5:** Commit binaries through GitHub Git data APIs so exact bytes are preserved.

### Task 3: Replace homepage/login SVG media with supplied photographs

**Files:**
- Modify: `src/main/resources/templates/home/index.html`
- Modify: `src/main/resources/templates/auth/login.html`

**Interfaces:**
- Consumes: Task 2 static media paths.
- Produces: photographic hero/login/category/campaign markup.

- [ ] **Step 1:** Change homepage hero media to `/images/media/home-hero.webp` with eager loading, high fetch priority, async decode, and an object-position suited to the approved left-copy/right-player composition.
- [ ] **Step 2:** Make all four homepage category cards use dedicated photographic assets instead of `featuredProducts[n]` or SVG placeholders.
- [ ] **Step 3:** Change the secondary campaign to `/images/media/campaign-retro.webp` so no SVG fallback is visible on the homepage.
- [ ] **Step 4:** Change login media to `/images/media/login-player.webp` while preserving form and demo credentials.
- [ ] **Step 5:** Keep existing real database-backed product card/product detail images unchanged.

### Task 4: Rebuild dashboard presentation toward approved preview

**Files:**
- Modify: `src/main/resources/templates/dashboard/index.html`
- Modify: `src/main/resources/static/css/storefront-repair.css`
- Modify: `src/main/resources/static/css/storefront-premium-v2.css`

**Interfaces:**
- Consumes: current dashboard model fields and existing navigation/sidebar fragments.
- Produces: compact preview-like customer/admin dashboard presentation without route/model changes.

- [ ] **Step 1:** Add `storefront-repair.css` to the dashboard template for both customer and admin roles.
- [ ] **Step 2:** Customer dashboard: replace verbose metric-card copy with compact preview-like summary cards, keep tabs and recent orders primary, add a concise welcome/account header and optional photographic visual treatment using supplied media.
- [ ] **Step 3:** Admin dashboard: compact header and metrics, emphasize recent orders table, retain Add Product and low-stock actions, and make spacing/status chips match the approved preview.
- [ ] **Step 4:** Add responsive CSS so desktop content never sits behind the nav/sidebar and mobile/tablet layouts remain readable without horizontal sliders.
- [ ] **Step 5:** Reduce heavy box shadows, transitions, and card transforms on dashboard/navigation components.

### Task 5: Remove laggy nonessential frontend motion

**Files:**
- Modify: `src/main/resources/static/js/storefront-premium-v2.js`
- Modify: `src/main/resources/static/css/storefront-premium-v2.css`
- Modify: `src/main/resources/static/css/storefront-repair.css`

**Interfaces:**
- Consumes: existing product rail and mobile nav behavior.
- Produces: same functional interactions with less animation/observer overhead.

- [ ] **Step 1:** Remove the IntersectionObserver reveal system and make content visible immediately.
- [ ] **Step 2:** Change product rail button scrolling to `behavior: "auto"`.
- [ ] **Step 3:** Remove global `scroll-behavior: smooth` and neutralize `data-premium-reveal` opacity/transform animation CSS.
- [ ] **Step 4:** Keep lightweight hover state changes but remove nonessential translate/scale animation from nav/dashboard cards.
- [ ] **Step 5:** Preserve mobile menu, sidebar drawer, password toggle, cart quantity controls, filters, and other functional JavaScript in `app.js`.

### Task 6: Verify, review, merge, and deploy

**Files:**
- No new production files expected beyond Tasks 1–5.

**Interfaces:**
- Consumes: completed branch.
- Produces: green CI, merged `main`, Render live deploy.

- [ ] **Step 1:** Run/observe branch GitHub Actions Maven verification and Docker build; fix only evidence-based failures.
- [ ] **Step 2:** Review branch diff for accidental business/security/data changes and confirm only media/templates/CSS/JS/tests/docs changed.
- [ ] **Step 3:** Open PR against `main`, ensure it is mergeable and CI is green, then merge.
- [ ] **Step 4:** Observe fresh `main` CI and Render auto-deploy for the exact merge commit.
- [ ] **Step 5:** Verify Render deploy status is `live`; check `/actuator/health`, homepage, login, and dashboard routes when external HTTP access is available.