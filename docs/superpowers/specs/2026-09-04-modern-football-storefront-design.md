# JerseySee Modern Football Storefront — Design Specification

Date: 2026-09-04
Status: Approved direction, implementation pending
Branch: design/modern-football-storefront

## 1. Goal

Transform JerseySee from a visually heavy, assignment-like storefront into a polished, responsive football ecommerce experience that feels credible as a live commercial site. Preserve the existing Spring Boot architecture, authentication, cart, checkout, orders, inventory, admin/staff workflows, Aiven persistence, and Render deployment.

## 2. Design Direction

The approved direction is **Modern Football Club Store**: premium ecommerce first, football identity second.

The visual system will use:
- dominant warm ivory/white surfaces,
- deep midnight navy for typography, footer, and key structural areas,
- cobalt as a restrained interaction/accent color,
- burgundy/crimson for selected premium highlights,
- warm gold for small details only,
- significantly less use of full-width saturated color blocks.

The redesign must avoid giant box mosaics, duplicated navigation bars, oversized decorative headings, and generic vector-shirt artwork as primary merchandising content.

## 3. Navigation

### Desktop
- Replace the current two-tier navigation with one compact sticky header.
- Warm light background with dark navy typography.
- Brand mark and JerseySee wordmark on the left.
- Primary navigation: New In, Jerseys, Player Edition, Retro, Boots, Accessories.
- Search, account, and cart on the right.
- A restrained dropdown/mega-menu provides Home Kits, Away Kits, Third Kits, clubs, and collections instead of a second blue category strip.
- Announcement content is reduced to a slim rotating/service strip or a single concise line so the top of the site does not feel crowded.

### Mobile
- Compact header with brand, search, account/cart actions, and hamburger.
- Slide-in mobile navigation drawer.
- Touch-friendly controls and no horizontal overflow.

## 4. Brand Mark

Replace the current jersey-avatar style mark with a cleaner football-fashion identity.

Requirements:
- simple JS monogram/crest concept,
- legible at favicon and mobile header sizes,
- no dependence on detailed jersey illustration,
- navy/cobalt/gold palette,
- original artwork rather than copying a club crest or sportswear logo.

The brand wordmark remains “JerseySee”.

## 5. Homepage

### 5.1 Product Hero Slideshow
Immediately below the header, add a photo-led full-width carousel using the uploaded football-kit images.

Behavior:
- 3–5 featured slides,
- automatic rotation with a calm interval,
- previous/next controls,
- pagination indicators,
- keyboard accessible,
- pause when user interacts or prefers reduced motion,
- swipe support on touch devices,
- responsive image crop.

Each slide includes:
- club/product name,
- edition label,
- BDT price,
- concise campaign copy,
- Shop Now CTA.

The uploaded product photos, not generic vector shirts, are the primary visual focus.

### 5.2 Merchandise Structure
Replace the current large colored collection mosaic with lighter, image-led commerce sections:
1. New Arrivals horizontal product rail.
2. Fan / Player / Retro edition tabs.
3. Top Clubs logo/text rail and product subsets.
4. Football Essentials: boots, footballs, trainingwear, accessories.
5. Custom Name & Number feature, simplified and less boxy.
6. Delivery / exchange / payment trust strip.
7. Compact editorial banner before footer.

Product cards use:
- large real product image,
- subtle edition badge,
- club/product title,
- clear BDT price,
- sizes/stock hint where useful,
- subtle hover zoom and lift,
- minimal borders and restrained shadows.

## 6. Catalog Page

The catalog remains filterable but becomes visually lighter.

Changes:
- remove the oversized dark “Find your colours” banner,
- compact catalog header with title, count, sort, and active filters,
- desktop filters in a clean left column or top filter bar,
- mobile filter drawer,
- product grid emphasizes images and pricing,
- empty-state handling remains clear but not visually dominant.

## 7. Product Detail

Keep current product functionality while improving presentation:
- image gallery/photo stage,
- edition/club/season hierarchy,
- large BDT price,
- size selector,
- printing options,
- stock status,
- add-to-cart CTA,
- delivery/exchange information,
- mobile sticky purchase CTA where appropriate.

## 8. Login / Authentication

Replace the current 50/50 split-screen poster layout with a premium ecommerce login experience.

Desktop:
- centered or lightly asymmetric card layout,
- restrained football photography/texture rather than giant vector shirt art,
- JerseySee logo/wordmark at top,
- refined input controls with icons only when useful,
- password visibility toggle,
- strong primary sign-in button,
- register link and back-to-store link.

Demo access:
- show a “Demo Accounts” area below the form,
- Demo Customer and Demo Admin cards,
- one-click autofill buttons,
- public demo credentials must be isolated from any secret production administrator account,
- destructive actions for the public demo admin must be restricted or protected so visitors cannot damage the live demo dataset.

## 9. Footer

Create a compact premium footer with:
- JerseySee brand summary,
- Shop links,
- Account links,
- Customer care links,
- deployment/project credit area,
- optional social/project link placeholders only where real links are available.

Copyright line:
`© 2026 JerseySee. Designed & developed by Azizul Hakim Omor.`

Also show BDT pricing/payment note without visually competing with the copyright.

## 10. Demo Catalog and Bangladesh Pricing

The existing generic “Metro City / Heritage United” products will be replaced or reconciled with realistic football-store demo entries using the uploaded images.

Primary jersey pricing:
- Fan Edition: ৳750
- Player Edition: ৳1,100
- Retro: ৳1,299

Uploaded clubs/kits include:
- Barcelona home, away, third,
- Real Madrid home, away, third, retro,
- Arsenal home, away, third,
- Chelsea home, away, third,
- Liverpool home, away,
- Manchester City home, away, third,
- Manchester United away, third,
- AC Milan retro,
- Juventus 94/95 retro.

Additional sample products will use realistic local pricing ranges, for example:
- training football: about ৳899,
- match football: about ৳1,299,
- mini supporter ball: about ৳499,
- futsal/entry footwear: about ৳2,299–৳2,650,
- premium boots: about ৳3,499,
- training top: about ৳899,
- training trouser: about ৳999,
- coach jacket: about ৳1,850,
- accessories: about ৳199–৳550.

These are demo prices and should be presented as catalog data, not as claims about official club merchandise pricing.

## 11. Product Images and Persistence

The 22 uploaded kit images will be normalized to consistent filenames and attached to the seeded demo products.

Production persistence requirements:
- continue using the existing database-backed product-image storage path,
- no `/tmp` production dependence,
- product images survive Render restarts,
- preserve content type and original filename metadata,
- seed only when appropriate and do not overwrite administrator-uploaded images unless the product is recognized as managed demo data.

For footballs/boots where user-provided imagery is absent, use original neutral JerseySee product artwork generated specifically for the demo or existing original placeholders, not scraped retailer photos.

## 12. Demo Accounts

Public demo identities:
- Demo Customer: customer@jerseysee.demo
- Demo Admin: admin@jerseysee.demo
- shared demo password may be `Demo123!` for the public demo environment.

Production security rule:
- the secret administrator seeded via environment variables remains separate,
- the public demo admin must not expose credentials for the secret production admin,
- destructive admin operations for the demo identity should be guarded if public admin access is enabled.

## 13. Responsive Behavior

Explicit breakpoints will be tested around:
- large desktop,
- 1024px tablet/compact desktop,
- 768px tablet,
- 375–430px mobile.

Requirements:
- no clipped headings,
- no horizontal scrolling,
- carousel remains usable by touch,
- product grids reduce columns progressively,
- filters become a drawer on mobile,
- login becomes single-column,
- navigation becomes a drawer,
- tap targets remain comfortable,
- footer stacks cleanly.

## 14. Accessibility and Motion

- semantic navigation and form labels,
- visible keyboard focus,
- carousel buttons and slide state announced appropriately,
- alt text for real product images,
- `prefers-reduced-motion` support,
- sufficient color contrast,
- no essential information communicated by color alone.

## 15. Architecture / Files Expected to Change

Likely areas:
- `templates/fragments/navigation.html`
- `templates/fragments/footer.html`
- `templates/home/index.html`
- `templates/auth/login.html`
- `templates/catalog/list.html`
- `templates/catalog/detail.html`
- `templates/fragments/product-card.html`
- `static/css/jerseysee.css`
- `static/js/app.js`
- `static/images/brand-mark.svg`
- demo catalog/seed classes under `config/`
- database image seeding support if needed
- rendering/storefront/data initialization tests
- demo product image resources

Existing controller/service/domain boundaries should be preserved unless a small helper is needed for demo image seeding or safe demo-admin restrictions.

## 16. Testing Strategy

Implementation follows TDD for behavioral changes.

Required verification:
- template contract tests for new navigation, footer, login demo controls, and carousel hooks,
- demo data tests for requested BDT pricing and public demo accounts,
- product-image persistence tests for seeded image bytes/metadata,
- security tests for demo-admin restrictions if public admin access is enabled,
- existing cart/order/checkout/security tests remain green,
- full `mvn clean verify`,
- production Docker build,
- GitHub Actions green on the feature branch and main after merge.

Manual/live verification after Render deploy:
- homepage loads with products,
- carousel works,
- images load from the production database-backed route,
- mobile viewport is usable,
- customer demo login works,
- admin demo login works only within its allowed scope,
- prices show in BDT,
- footer copyright is visible,
- health endpoint remains healthy.

## 17. Deployment and LinkedIn

After tests and Docker build pass:
- merge the implementation PR to `main`,
- allow the existing Render auto-deploy configuration to deploy the new commit,
- verify the public website and health endpoint,
- share the final live link and demo credentials,
- prepare a concise LinkedIn achievement post referencing the completed live project.

Publishing to LinkedIn is only performed if a connected LinkedIn publishing integration is available and the user explicitly authorizes that external post action at execution time.

## 18. Non-Goals

This refresh will not:
- replace Spring Boot with a frontend framework,
- introduce real payment processing,
- claim the demo kit photos are official merchandise,
- copy a specific club shop or sportswear website,
- refactor unrelated backend modules,
- weaken existing deployment/database safety controls.
