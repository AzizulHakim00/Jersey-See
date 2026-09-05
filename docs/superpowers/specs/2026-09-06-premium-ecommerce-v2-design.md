# JerseySee Premium Ecommerce V2 Design

Date: 2026-09-06
Status: approved visual direction, implementation specification
Reference: the latest approved composite mockup supplied in the project conversation

## Goal

Rebuild the JerseySee presentation layer so the deployed application reads immediately as a premium football ecommerce store rather than a generic content site. Preserve the existing Spring Boot, Thymeleaf, database, authentication, order, product, cart and admin behavior while replacing the visual hierarchy, page composition and interaction polish.

The finished UI should closely match the approved mockup: clean white commerce surfaces, restrained navy branding, compact typography, real product photography, subtle gold accents, disciplined spacing and responsive layouts that do not overlap or shift during load.

## Chosen approach

Use the existing server-rendered architecture and clean the relevant Thymeleaf templates while consolidating visual behavior into one final premium stylesheet and one small interaction script. Do not introduce React, a CSS framework, client-side routing, remote font dependencies or heavy animation libraries.

This approach is preferred over layering more overrides because the current storefront loads `jerseysee.css`, `storefront-modern.css` and `storefront-final.css` through different templates, which increases cascade ambiguity. The V2 pages will use the stable shared/base styles plus one final premium commerce layer, with explicit component classes and predictable breakpoints.

## Visual system

### Brand and palette

- Primary navy: `#071a3d` / `#0a234f`
- Commerce blue: `#1557d6`
- Accent crimson: `#c92345`
- Premium gold: `#c9a457`
- Warm off-white: `#f7f5ef`
- Surface white: `#ffffff`
- Text navy: `#07142c`
- Muted copy: `#667085`
- Border: `#e4e7ec`
- Success: soft green only for inventory/order states

No decorative gradients. Shadows remain subtle and low-opacity. Corners stay medium and consistent rather than overly rounded.

### Typography

Use the existing local/system font stack only. Commerce headings should be strong but not oversized. Serif display type is reserved for a small number of premium campaign statements; product names, navigation, prices, filters and dashboard content use a clean sans-serif stack. Avoid the excessively large poster typography seen in the rejected implementation.

### Motion

Motion is functional and restrained:

- navbar active underline slides/fades in
- nav icons move 1-2px and change tone on hover/focus
- buttons use a short press transform and focus ring
- product cards lift by 2-3px and reveal secondary affordances
- hero/image carousel crossfades and translates subtly
- sections reveal only once with short opacity/translate transitions
- accordions use height/opacity transitions
- respect `prefers-reduced-motion: reduce`

No continuous bouncing, zoom loops, parallax, cursor-following effects or long animations.

## Shared header

All storefront and customer-facing pages use one horizontal header.

Desktop structure:

1. compact shield logo + JerseySee wordmark
2. Home
3. Shop
4. Player Edition
5. Retro
6. Boots
7. About (optional lightweight anchor/page if route exists; otherwise omit rather than create dead links)
8. flexible search field
9. account icon
10. bag icon with count

The header is sticky with a white surface and a thin border. It must remain one row on desktop. Mobile collapses the navigation into a drawer while keeping logo, search/account/bag affordances usable. No duplicate nav bands.

## Homepage

### Hero

Use real football imagery from the already-seeded/uploaded kit assets where possible. The hero should visually match the approved mockup: dark stadium/football context, product/person focal image, left-aligned campaign message and a single primary CTA.

Preferred message hierarchy:

- eyebrow: `FOOTBALL LIVES HERE`
- display: `Wear The Passion`
- supporting copy about premium jerseys, player editions and retro classics
- CTA: `Shop Now`

The hero should occupy roughly 45-55vh on desktop, not a full viewport.

### Trust strip

Four compact commerce benefits immediately below the hero:

- BDT pricing
- Free delivery over ৳5,000
- 7-day size exchange
- Custom name & number

### Category cards

A 4-card category row on desktop, 2 columns on tablet, horizontally scrollable or 1-2 columns on mobile:

- Player Edition
- Retro Classics
- Football Boots
- National Teams / New Arrivals depending on available catalog data

Use real imagery and dark image overlays only where necessary for text legibility.

### Featured jerseys

Horizontal product rail with 5 cards visible on wide desktop, 3 on tablet and 1.2-2 on mobile. Cards include real image, club/name, edition, BDT price and optional status badge. Arrow controls must be keyboard accessible and hide/disable correctly when scrolling is not possible.

### Secondary campaign

One restrained dark football banner near the lower page to add premium editorial rhythm without making the entire page look like a poster.

## Catalog / Shop

Desktop layout uses a left filter column and a responsive product grid. The filter area is visually compact and cannot push product content below the fold unnecessarily.

Filters use existing backend parameters only:

- category/product type
- edition
- club/country
- price where supported
- size/availability only if the backend already supports them; otherwise do not fake controls

Mobile filters open in a drawer/sheet.

Product grid:

- 4 columns on large desktop
- 3 on regular desktop/tablet landscape
- 2 on tablet/mobile landscape
- 1-2 on narrow mobile depending on viewport

Cards maintain a stable image aspect ratio to prevent layout shift. Images use `loading="lazy"` below the fold and explicit dimensions/aspect-ratio. Prices use BDT. Any unavailable wishlist/rating functionality is not fabricated.

## Product detail

Two-column desktop layout:

- left: main product image with optional thumbnail list from existing product imagery
- right: club/edition, product name, price, stock state, size selector, custom-print controls, add-to-bag action

Below the buy box show compact trust items and accordion sections for product details, delivery/exchange and custom printing.

For guests, the purchase box should clearly show `Sign in to buy`; authenticated customers get `Add to bag`. Staff retain management controls.

## Login

Use the approved split layout:

- left: premium dark football visual, logo and short football-centric statement
- right: compact email/password form on a white surface

The rejected one-click demo account cards are removed completely. There must be no `/demo-login/customer` or `/demo-login/admin` buttons/forms on the page and no JavaScript that auto-fills or submits demo credentials.

Under the normal sign-in form, show a small plain-text block titled `Demo credentials (for testing only)`. These credentials are intentionally public demo-only accounts and are displayed visibly so a reviewer can type them manually into the regular login form:

- Customer demo
  - Username: `customer@demo.local`
  - Password: `Demo123!`
- Admin demo
  - Username: `admin@demo.local`
  - Password: `Demo123!`

The implementation must ensure those two deliberate demo accounts actually exist in the portfolio/demo deployment and authenticate through the normal `/login` form. They must not be represented by hidden data attributes, clickable credential cards, one-click authentication endpoints or client-side autofill. Do not expose any non-demo account, environment secret, database credential or production-only password.

The page should include a small footer credit:

`© 2026 Azizul Hakim Omor. All rights reserved.`

## Customer account / dashboard

Do not use a fixed full-height left sidebar that obscures the page.

Use the normal storefront header and a compact account navigation row/tabs:

- Dashboard
- Orders
- Addresses/Profile based on existing routes
- Wishlist only if the application actually supports it

The dashboard contains compact metrics, recent orders and a small shopping CTA. It should feel like an ecommerce account area, not a back-office admin panel.

## Admin dashboard

Admin/staff pages may keep a left operational sidebar because this is appropriate for management, but it must be width-controlled and never cover the main content. The main area starts after the sidebar at desktop widths and the sidebar becomes a drawer on smaller screens.

Dashboard hierarchy:

- page title and primary action
- product/order/customer/revenue metrics from actual available model data
- recent orders table
- existing stock/payment operational panels

Do not invent analytics that the backend does not provide.

## Footer

Use a navy footer with compact columns:

- brand and short positioning statement
- Shop
- Account / Help
- Customer care
- optional newsletter field only if it is non-functional visual UI with clear no-submit behavior, otherwise omit
- restrained social-style icons only if links are real; no dead links

Required ownership line:

`© 2026 Azizul Hakim Omor. All rights reserved.`

Optional secondary line:

`JerseySee · Football culture, refined.`

## Performance and layout stability

- no remote image hotlinks
- prefer already-uploaded/local product images
- hero image gets eager/high-priority loading
- below-fold product images use lazy loading
- explicit `width`, `height` or `aspect-ratio` on all important images
- reserve carousel and card dimensions before images arrive
- avoid DOM measurements during initial render where CSS can solve layout
- one passive scroll listener at most; prefer IntersectionObserver for reveals
- event delegation for repeated product/nav interactions
- no third-party animation packages
- keep JS enhancement optional: core links/forms work without JavaScript
- avoid FOUC by loading the final premium stylesheet in `<head>` and not injecting styles from fragments

## Responsive acceptance

Test widths: 1440, 1280, 1024, 768, 430, 390 and 360 px.

At every width:

- no horizontal page scrolling
- header never becomes two accidental rows on desktop
- mobile menu never overlaps content after close
- product cards never collapse to text-only blocks
- images never exceed their containers
- dashboard content is not hidden under navigation/sidebar
- forms remain fully reachable without zooming
- tap targets are at least ~40px

## Accessibility

- keyboard-visible focus states
- semantic headings
- button vs link semantics preserved
- descriptive alt text for product imagery
- icon-only controls include labels
- mobile menu and accordions expose state with `aria-expanded`
- color is not the sole indicator for order/stock state
- reduced-motion support

## Testing strategy

Before implementation, extend the existing storefront contract tests with assertions for:

1. the single-row premium header routes
2. visible `customer@demo.local` / `Demo123!` and `admin@demo.local` / `Demo123!` credential text on the login page
3. absence of one-click `/demo-login/customer` and `/demo-login/admin` forms/buttons and absence of demo autofill behavior
4. successful authentication of both deliberate demo accounts through the normal `/login` flow in the demo/portfolio configuration
5. Azizul Hakim Omor copyright line
6. stable image attributes / real product image routes
7. customer dashboard without the staff sidebar
8. premium stylesheet/script inclusion on target pages

Then implement page changes until those tests pass.

Verification after implementation:

- `./mvnw clean verify`
- production Docker build
- review PR diff for dead links, duplicated CSS and exposure of anything except the two deliberate demo credentials
- merge only when CI is green
- Render auto-deploy from `main`
- live smoke test: health, homepage, catalog, player/retro filters, product image, product detail, manual customer login using the displayed credentials, manual admin login using the displayed credentials, customer dashboard, admin dashboard and ownership footer

## Files expected to change

Primary:

- `src/main/resources/templates/fragments/navigation.html`
- `src/main/resources/templates/fragments/footer.html`
- `src/main/resources/templates/home/index.html`
- `src/main/resources/templates/catalog/list.html`
- `src/main/resources/templates/catalog/detail.html`
- `src/main/resources/templates/auth/login.html`
- `src/main/resources/templates/dashboard/index.html`
- relevant profile/orders/cart templates where shared account navigation needs to remain consistent
- new/final `src/main/resources/static/css/storefront-premium-v2.css`
- new/final `src/main/resources/static/js/storefront-premium-v2.js`
- storefront contract tests
- demo/public-portfolio authentication configuration only as needed to make the displayed deliberate demo credentials authenticate through normal login

Existing backend domain logic should remain unchanged unless a small view-model or demo-auth configuration adjustment is strictly required to render/use already-approved demo data.

## Definition of done

The project is complete only when the deployed Render site visually follows the approved mockup, the live pages are responsive and stable, the two visible manual demo credentials work through the regular login form, ownership credit is correct, real product images render, customer/admin workflows remain functional, Maven and Docker CI are green, and a live post-deploy smoke test passes.