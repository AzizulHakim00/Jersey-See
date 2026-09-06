# JerseySee Real Media, Dashboard Polish, and Performance Repair Design

## Goal
Make the live JerseySee site visually match the approved premium ecommerce preview more closely by replacing remaining SVG/cartoon placeholders with the user's supplied real football photography, improving both customer/admin dashboards, and removing unnecessary frontend motion that makes the UI feel laggy.

## Approved media source
Use only media supplied by the user in this conversation for this repair. Do not generate replacement artwork. The source images include the supplied player photos, retro football cover photos, football/ball photos, Barcelona away kit photo, training top photo, and three boot/sneaker photos. The two visibly watermarked stock-ball images are not used in prominent storefront placement.

## Asset mapping
- Homepage hero: `messi login page images.jpg`, cropped/positioned as a dark photographic hero with text on the left and the player on the right.
- Login visual: `messi login page images.jpg`, vertical crop, replacing `auth-footballer.svg`.
- Retro category / secondary campaign: `cover 2 images.jpg` first choice, `cover.jpg` fallback.
- Football boots category: `sneaker 3 images.jfif` first choice; `sneaaker 1 images.jfif` and `snsker 2 images.jfif` used for additional product/category support.
- Match essentials / football: `gootball imags.jfif` or `football images.jfif`, avoiding any supplied watermarked stock image.
- Training/new arrivals: `top images.jfif` and `barccelona away 2.jfif` where appropriate.
- Existing real product photos in the database remain authoritative for actual jersey product cards and product detail pages.

All committed media should be normalized into web-friendly static assets, preferably WebP, with dimensions appropriate to the target placement. Do not upscale low-resolution product images beyond what is needed for CSS layout.

## Homepage
- Replace the SVG footballer in the hero with the supplied real player image.
- Use a photographic hero treatment that preserves the approved left-aligned headline / CTA composition.
- Stop using `product-placeholder.svg`, `hero-kit.svg`, or `custom-printing.svg` as visible homepage fallback imagery when suitable supplied media exists.
- Keep the category grid but use dedicated photographic assets for Player Edition / Retro / Boots / New Arrivals so it no longer depends on arbitrary `featuredProducts[n]` ordering.
- Use lazy loading for below-the-fold category/campaign images; keep hero eager/high priority.

## Login
- Replace `auth-footballer.svg` with the supplied vertical player photograph.
- Preserve the approved split-screen login layout, demo credentials, and manual login form.
- Use CSS background/overlay treatment rather than large decorative SVG layers.

## Customer dashboard
Bring the page closer to the approved preview:
- compact account title/welcome row;
- horizontal account tabs;
- four compact summary cards with simple icon chips and minimal copy;
- recent orders as the primary content block;
- stronger spacing, lighter borders, white cards on a neutral background;
- optional subtle photographic banner/background using supplied football media without compromising readability;
- keep current working customer actions and links unchanged.

## Admin dashboard
Bring the page closer to the approved preview:
- clean top header and Add Product action;
- compact metric cards;
- recent orders table as primary content;
- clear status chips and View actions;
- low-stock/quick actions secondary panel;
- retain the existing fixed admin sidebar on desktop and drawer behavior only on smaller screens;
- keep all admin permissions and CRUD routes unchanged.

## Performance and smoothness
- Remove `scroll-behavior: smooth` globally.
- Remove or neutralize IntersectionObserver reveal animations for ordinary page sections.
- Remove transform-heavy hover effects on navigation, dashboard cards, and nonessential controls.
- Keep product-rail button scrolling but use instant/low-cost scrolling rather than prolonged smooth animation.
- Do not auto-animate homepage/dashboard content.
- Keep image decoding async and lazy loading below the fold.
- Keep mobile navigation/sidebar functionality; only remove unnecessary animation cost, not behavior.

## Testing
Add/adjust contract tests to prove:
1. homepage references photographic `/images/media/...` assets and no longer references `auth-footballer.svg` in the hero;
2. login references the photographic login media asset;
3. homepage category cards use dedicated media assets instead of SVG placeholders;
4. dashboard template loads the repair/premium dashboard stylesheet and contains the compact dashboard structure for both roles;
5. premium JavaScript no longer creates an IntersectionObserver reveal system or smooth product-rail scrolling;
6. the existing security, customer operation, admin operation, checkout, image persistence, and page-rendering tests still pass.

## Deployment
Implement on a dedicated branch, verify with the repository Maven suite and production Docker build in GitHub Actions, merge only after green CI, allow Render auto-deploy, then verify the Render deployment status and live health endpoint when accessible.