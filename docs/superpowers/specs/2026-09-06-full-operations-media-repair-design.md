# JerseySee Full Operations and Media Repair Design

## Goal
Make JerseySee fully operational for both admin and customer accounts, fix navigation/layout defects, remove placeholder/coming-soon imagery, and align the live storefront, login, customer dashboard, and admin dashboard with the approved premium preview.

## Approved behavior
- `admin@demo.local` is a normal ADMIN account with full admin mutations.
- Local/seeded admin accounts also retain full ADMIN capabilities.
- `customer@demo.local` is a normal CUSTOMER account with full cart, checkout, orders, cancellations, and profile operations.
- Customer and staff pages must never render content beneath or behind their navigation chrome.
- The desktop storefront header is a fixed one-row commerce header with no unnecessary horizontal slider/scroll control.
- Staff uses the operational sidebar; customer uses the retail navbar and account tabs.
- Missing product/homepage/login visuals are replaced with bundled premium media assets and/or seeded product imagery; no visible “IMAGE COMING SOON” copy remains on normal seeded catalog content.
- Production demo catalog/image initialization must be idempotent and must repair seeded products that still point at placeholder imagery.
- Existing Spring Security role boundaries remain: CUSTOMER cannot access staff routes; ADMIN can access all staff routes.
- Existing Render/Aiven deployment configuration remains compatible.

## Architecture
Keep the current Spring Boot MVC + Thymeleaf structure. Remove the special public-demo admin mutation blocker instead of weakening role-based security. Consolidate page spacing around the two existing navigation systems: sticky retail header for storefront/customer pages and fixed staff sidebar for admin/staff pages. Improve demo media by using deterministic bundled static/demo assets and the existing `ProductImage` database persistence path.

## Components
### Authorization
Remove `PublicDemoAdminReadOnlyFilter` from the security chain and retire the filter class/tests that assert read-only behavior. Keep route-level role checks and `@PreAuthorize` constraints.

### Admin operations
Exercise product CRUD, employee CRUD, order status changes, and payment actions under ADMIN. Fix any controller/template mismatch discovered by tests. Admin dashboard quick actions must point to functional routes.

### Customer operations
Exercise add-to-cart, quantity update/remove, checkout, order list/detail/cancel/invoice, and profile update under CUSTOMER. Fix any controller/template mismatch discovered by tests.

### Navigation/layout
Retail header remains sticky and must reserve its own document flow height. Customer pages begin below the header without negative margins or transformed wrappers. Staff sidebar occupies its own fixed width and staff content uses a deterministic left offset; desktop sidebar navigation itself does not use a horizontal slider.

### Media
Use product-specific demo images where available and premium branded fallback artwork otherwise. The initializer must replace legacy placeholder/coming-soon stored images for seeded products and persist valid image metadata. Homepage category/hero/campaign/login visuals should use concrete bundled assets instead of generic placeholders.

## Testing
Add focused MockMvc/security tests for demo admin mutations and customer operations, template contract tests for navbar/sidebar layout classes and absence of coming-soon copy, and initializer tests proving seeded product images are repaired idempotently. Run full Maven verification and production Docker build before merge/deploy.

## Deployment acceptance
After tests pass, merge the repair branch to `main`, allow Render to deploy, verify `/actuator/health`, homepage, login, demo admin dashboard/actions, and demo customer shopping flow on production.