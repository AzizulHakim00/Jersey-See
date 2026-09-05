# JerseySee Premium Ecommerce V2 Implementation Plan

Date: 2026-09-06
Design: `docs/superpowers/specs/2026-09-06-premium-ecommerce-v2-design.md`
Branch: `design/premium-ecommerce-v2`

## Goal

Implement the approved premium football ecommerce mockup as a stable server-rendered Spring Boot/Thymeleaf UI without changing core commerce rules. The final production site must use real local product images, visible manual demo credentials, restrained animation, responsive layouts, and one predictable premium style layer.

## Engineering strategy

- Preserve existing controllers, services, cart, orders, product filtering and role authorization.
- Use `jerseysee.css` only as the shared functional/base layer and add one final `storefront-premium-v2.css` override layer for approved pages.
- Stop loading `storefront-modern.css` / `storefront-final.css` from the navigation fragment.
- Add one small `storefront-premium-v2.js` enhancement script for product rails and reveal motion; keep all forms and links functional without JS.
- Use existing `/product-images/{storedName}` assets and explicit image dimensions/aspect ratios to prevent layout shifts.
- Keep public demo admin read-only while allowing normal form login with the deliberately public demo credentials.

## Task 1: Lock the V2 behavior with failing tests

Files:
- `src/test/java/bd/edu/seu/jerseysee/controller/ModernStorefrontContractTest.java`
- `src/test/java/bd/edu/seu/jerseysee/controller/StorefrontTemplateContractTest.java`
- `src/test/java/bd/edu/seu/jerseysee/config/PublicDemoProductionInitializationTest.java`
- `src/test/java/bd/edu/seu/jerseysee/config/PublicDemoAdminReadOnlyFilterTest.java`
- `src/test/java/bd/edu/seu/jerseysee/controller/PublicDemoLoginControllerTest.java`

Assertions:
1. Header contains Home, Shop, Player Edition, Retro and Boots in one shared storefront nav; no stylesheet injection from fragment.
2. Login visibly contains `customer@demo.local`, `admin@demo.local`, and `Demo123!` and contains no one-click demo forms/buttons or demo autofill hooks.
3. Footer contains `© 2026 Azizul Hakim Omor. All rights reserved.`
4. Product cards have lazy loading plus explicit width/height and asynchronous decoding.
5. Customer dashboard uses storefront navigation/account tabs and never the staff sidebar.
6. Approved pages load `storefront-premium-v2.css` and the premium enhancement script.
7. Premium stylesheet includes required responsive/reduced-motion rules and contains no `linear-gradient` or `radial-gradient` declarations.
8. Production public demo initialization creates `customer@demo.local` and `admin@demo.local` with `Demo123!` and correct CUSTOMER/ADMIN roles.
9. Public demo admin read-only protection follows `admin@demo.local`.

Verification: open PR after the test-only commit and confirm the PR CI is RED for the intended unmet V2 contract.

## Task 2: Align demo authentication with manual credentials

Files:
- `src/main/java/bd/edu/seu/jerseysee/config/PublicDemoAccountInitializer.java`
- `src/main/java/bd/edu/seu/jerseysee/config/PublicDemoAdminReadOnlyFilter.java`
- `src/main/java/bd/edu/seu/jerseysee/controller/PublicDemoLoginController.java`
- `src/main/resources/application-production.properties`

Changes:
- Seed customer `customer@demo.local` and admin `admin@demo.local` with `Demo123!` using the password encoder.
- Keep stable demo seed keys so an existing deployment migrates the seeded account identities safely on restart.
- Update read-only admin identity to the new demo email.
- Keep legacy one-click endpoint implementation only for compatibility if tests/routes still require it, but remove every UI entry point to it; normal `/login` is the supported user flow.
- Update production comments to describe manual portfolio demo accounts, not one-click access.

Verification: targeted public-demo tests pass while V2 template tests remain red until UI is implemented.

## Task 3: Build the shared premium shell and performance primitives

Files:
- `src/main/resources/templates/fragments/navigation.html`
- `src/main/resources/templates/fragments/footer.html`
- `src/main/resources/templates/fragments/product-card.html`
- `src/main/resources/templates/fragments/service-strip.html`
- `src/main/resources/static/js/app.js`
- create `src/main/resources/static/css/storefront-premium-v2.css`
- create `src/main/resources/static/js/storefront-premium-v2.js`

Changes:
- Remove old modern/final stylesheet tags from fragments.
- Implement one-row white ecommerce header with compact brand, routes, search, account and bag controls.
- Add Boots route using existing catalog filtering.
- Add restrained animated underline/icon/button/card interactions.
- Build dark compact footer with real routes and owner credit.
- Give product images `width`, `height`, `loading="lazy"`, `decoding="async"` and stable aspect ratio.
- Remove obsolete demo autofill JS.
- Add IntersectionObserver one-shot reveals and accessible product rail scroll controls, respecting reduced motion.
- Build responsive CSS at 1220/980/720/480/360px without decorative gradients.

## Task 4: Rebuild public storefront pages to the approved mockup

Files:
- `src/main/resources/templates/home/index.html`
- `src/main/resources/templates/catalog/list.html`
- `src/main/resources/templates/catalog/detail.html`

Homepage:
- `FOOTBALL LIVES HERE` + `Wear The Passion` campaign hierarchy.
- Real persisted jersey image in the hero with eager/high-priority loading and reserved dimensions.
- BDT/free-delivery/exchange/custom-printing trust strip.
- Four compact category cards.
- Five-up featured jersey rail on wide desktop, 3 on tablet, ~1.5-2 on mobile.
- Restrained secondary football campaign near page bottom.

Catalog:
- Compact All Products heading/count, functional left filters, mobile filter drawer, stable responsive product grid.
- Do not invent sort/rating/wishlist behavior absent from backend.

Detail:
- Two-column image/commerce layout, stable hero image, size and printing controls, trust blocks and details accordions.
- Guest `Sign in to buy`; CUSTOMER `Add to bag`; staff management link remains role protected.

## Task 5: Rebuild login, customer account, cart and checkout

Files:
- `src/main/resources/templates/auth/login.html`
- `src/main/resources/templates/auth/register.html`
- `src/main/resources/templates/dashboard/index.html`
- `src/main/resources/templates/profile/edit.html`
- `src/main/resources/templates/orders/list.html`
- `src/main/resources/templates/orders/detail.html`
- `src/main/resources/templates/cart/view.html`
- `src/main/resources/templates/orders/checkout.html`

Changes:
- Split football/login composition matching the approved mockup.
- Plain visible demo credential text only; no clickable one-click login UI.
- Exact owner credit on login/footer.
- Customer dashboard uses top storefront nav + account tabs, compact real metrics and recent orders.
- Profile/orders use the same account navigation rhythm.
- Cart/checkout receive the same premium surface/card/form styling and stable product thumbnails.
- Registration visually matches login while retaining all existing validation.

## Task 6: Polish admin/staff shell

Files:
- `src/main/resources/templates/fragments/admin-sidebar.html`
- `src/main/resources/templates/dashboard/index.html`
- add premium stylesheet inclusion to relevant staff list/detail pages where necessary.

Changes:
- Keep a fixed-width operational sidebar only for staff/admin.
- Ensure desktop main content is offset and never hidden underneath it.
- Switch sidebar to a mobile drawer below the staff breakpoint.
- Use actual dashboard data only: orders, active orders, products, low stock, pending payments, revenue.
- Keep existing Products, Orders, Payments and Employees functionality unchanged.

## Task 7: PR verification and merge

Verification:
- PR CI must pass `./mvnw clean verify`.
- Production Docker build must pass.
- Review changed files for duplicate CSS, dead routes, one-click demo UI, accidental secrets and stale copyright text.
- Confirm premium CSS has no decorative gradient declarations.
- Confirm target templates load the premium V2 CSS before paint and no fragment injects stylesheet tags.
- Merge only after all checks are green.

## Task 8: Render deployment and live production smoke test

Wait for Render auto-deploy of the merged `main` commit, then verify:
- `/actuator/health` -> 200 / UP.
- Homepage contains `Wear The Passion` and the Azizul Hakim Omor copyright.
- Catalog has products and Player/Retro filters return non-zero results.
- Persisted product image returns `Content-Type: image/*`.
- Guest product page contains `Sign in to buy`.
- Login page visibly shows both manual demo usernames and `Demo123!`, with no demo-login forms/buttons.
- POST normal `/login` with customer credentials succeeds and customer dashboard/account tabs render.
- POST normal `/login` with admin credentials succeeds and admin dashboard renders.
- Public demo admin remains read-only for staff mutations.
- Authenticated customer product page contains `Add to bag`.

Only report completion after this live smoke test passes.