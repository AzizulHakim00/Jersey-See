# JerseySee Premium Storefront Design

## Purpose

Transform the public JerseySee interface from a university-project presentation into a credible premium football-commerce storefront. The redesign must preserve the existing Spring Boot, Thymeleaf, Spring Security, controller, service, repository, entity, cart, order, payment, and staff workflows.

This specification covers the frontend phase only. Render/Aiven reliability and persistent production image storage are a separate follow-up phase after the frontend branch is reviewed.

## Visual Direction

The experience uses an original “stadium luxury” identity inspired by the quality and confidence of major football club stores without copying their trademarks, crests, layouts, or copyrighted photography.

- Palette: deep navy, warm ivory, cobalt blue, restrained crimson, and muted gold.
- Typography: editorial serif display headings paired with a clear sans-serif interface typeface.
- Shape language: clean rectangular commerce surfaces, subtle borders, restrained rounding, and deliberate whitespace.
- Photography/artwork: original brand-neutral football kit campaigns and product imagery.
- Motion: small hover, reveal, drawer, and cart-feedback transitions with reduced-motion support.

## Information Architecture

### Shared storefront shell

The public shell contains:

1. A slim announcement bar for delivery, quality, and exchange messages.
2. A premium primary header with JerseySee branding, main departments, search, account, and cart.
3. A secondary category bar for new season, player edition, home, away, custom printing, and sale.
4. A substantial footer with shopping links, customer support, account links, payment information, and brand positioning.

Authentication and role-based links remain driven by the existing global model attributes. No decorative control may imply a feature that does not exist.

### Homepage

The homepage becomes a merchandising page rather than a project introduction. It includes:

- Full-width seasonal campaign hero and clear shopping calls to action.
- Four concise service assurances.
- Collection tiles for new season, player edition, retro, training, footwear, and accessories.
- Featured product merchandising using real product data.
- Custom-printing campaign.
- Final service and brand section leading into the full footer.

### Catalog

The catalog retains all existing backend filters and pagination while presenting them as a real retail experience:

- Editorial collection header.
- Desktop filter sidebar and mobile filter drawer.
- Product count, active-filter clarity, and reset action.
- Four-column desktop product grid with responsive two- and one-column states.
- Consistent product cards showing image, club/collection, product name, base price, available sizes, and availability.
- Empty and error states styled as storefront states rather than assignment panels.

### Product detail

The product page uses a two-column commerce layout:

- Large product-image stage with controlled fallback artwork.
- Breadcrumbs and product identity.
- Price, edition, season, club/country, and concise product description.
- Size/variant selector that clearly communicates stock.
- Quantity controls, printing options, printing price, and validation.
- Customer add-to-cart action, guest sign-in action, or role-aware staff action.
- Delivery, exchange, secure-checkout, and customization assurances.

The existing server-authoritative cart and price rules remain unchanged.

### Cart and checkout

- Cart lines use product images, readable variant/printing summaries, quantity controls, removal, and a sticky order summary.
- Checkout uses a clean two-column layout with delivery information, payment choice, order review, and final total.
- Existing validation and CSRF behavior remain intact.
- All flows work without JavaScript; JavaScript adds progressive enhancement only.

### Authentication, account, and orders

- Login and registration use a premium split campaign layout.
- Customer profile and order pages use the same retail identity, with clearer account navigation and order-status presentation.
- Staff dashboards retain their operational information architecture but receive the shared color, type, spacing, form, and component polish required for visual consistency.

## Reusable Components

The implementation will consolidate repeated markup into Thymeleaf fragments where it reduces drift:

- storefront head metadata and font loading;
- announcement/header navigation;
- footer;
- product card;
- service assurance row;
- flash/validation messages.

`jerseysee.css` remains the primary stylesheet but is reorganized into tokens, shared shell, commerce components, page sections, staff components, and responsive rules. `app.js` handles navigation, drawers, filters, quantity controls, printing controls, image previews, confirmations, and accessibility state.

## Responsive and Accessibility Requirements

- Desktop target: 1280–1600 px with four-product merchandising rows.
- Tablet target: 768–1199 px with adjusted navigation and two- or three-column grids.
- Mobile target: 320–767 px with drawer navigation, touch-sized controls, one- or two-column product cards, and non-sticky summaries.
- Semantic landmarks, useful alt text, visible focus indicators, correct labels, keyboard-operable dialogs/drawers, sufficient contrast, and `prefers-reduced-motion` support are required.
- No essential content or action may depend only on hover, color, animation, or JavaScript.

## Data and Backend Boundaries

This phase does not change entity relationships, repository APIs, authentication rules, order pricing, payment behavior, or role permissions. It may add presentation-only model attributes or lightweight view helpers when necessary, but database behavior remains unchanged.

Product images continue to use the existing image endpoint during this frontend phase. Persistent hosted image storage is addressed in the deployment phase.

## Verification

The frontend phase is accepted only when:

- all existing MVC rendering and security tests pass;
- updated template-contract tests cover navigation, product cards, product detail purchase controls, cart, checkout, and no-JavaScript fallbacks;
- every Thymeleaf template resolves against its controller model;
- all POST forms retain CSRF integration;
- no authenticated action becomes visible to an unauthorized role;
- responsive screenshots are reviewed at desktop, tablet, and mobile widths;
- the production package builds successfully before the branch is offered for merge.

## GitHub Delivery

Frontend changes will be committed to `codex/premium-storefront`, pushed to GitHub, and offered through a pull request to `main`. The pull request will describe changed pages, verification evidence, known deployment follow-up work, and screenshots of the completed storefront.
