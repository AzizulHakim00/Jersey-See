# JerseySee Complete Store Design

## Purpose

JerseySee is a university-level football merchandise store and small store-management system. It must be complete enough to demonstrate layered Spring Boot development, validation, security, JPA relationships, inventory, ordering, payment, and file handling, while remaining simple enough to explain in a viva.

## Technical Direction

- Preserve the existing `jersey-see` project and `bd.edu.seu.jerseysee` base package.
- Follow the understandable AFSOS pattern: Controller → Service → Repository → Entity, with DTOs for validated form input.
- Use Spring Boot 4.1.1, Java 17, Maven, Thymeleaf, Spring Security, Spring Data JPA, Bean Validation, MySQL, H2 for tests, HTML, CSS, and small vanilla JavaScript enhancements.
- Do not use React, REST-only architecture, JWT, microservices, Lombok-dependent domain behavior, or a real financial gateway.
- Use server-rendered pages and one authentication system for every role.

## Roles and Authorization

`CUSTOMER` can browse the shop, manage a session cart, check out, view their own orders, download their own invoice, and update their profile.

`SALESMAN` can use the staff dashboard, browse products and stock, view orders, and update order workflow states except payment confirmation and system administration.

`CASHIER` can view orders and payments, confirm a pending payment, and download invoices.

`MANAGER` can manage products, categories, variants, inventory, staff profiles, orders, and reports, but cannot create or disable administrators.

`ADMIN` has full access, including creating, enabling, and disabling non-admin staff accounts.

Public visitors can access the home page, catalog, product detail, registration, login, static assets, and product image-display endpoints. Customers register only as `CUSTOMER`; staff accounts are created by authorized management users.

## Domain Model

### User and Employee

`User` stores name, normalized unique email, BCrypt password, phone, address, role, enabled state, and creation timestamp. A staff user may have one `EmployeeProfile` with employee code, position, salary, joining date, and active state.

Relationships:

- `User 1 → 0..1 EmployeeProfile`
- `User 1 → many CustomerOrder`

### Catalog and Inventory

`Category` stores a unique name and description. `Product` stores category, name, description, brand, product type, club/country, season, kit type, jersey edition, sleeve type, base price, featured state, active state, and optional image metadata. Jersey-specific fields are nullable for non-jersey merchandise.

`ProductVariant` stores product, size, SKU, stock quantity, and optional price adjustment. Stock is tracked per variant rather than as a product-level list.

Relationships:

- `Category 1 → many Product`
- `Product 1 → many ProductVariant`

### Cart, Orders, and Payments

The cart is session-based to keep the project simple. Each `CartItem` references a product variant and records quantity, printing option, custom name, and custom number. The service recalculates prices from persisted products and never trusts browser-submitted totals.

`CustomerOrder` captures customer, delivery details, subtotal, delivery fee, total, status, and timestamps. `OrderItem` snapshots product name, SKU, size, unit price, printing data/charge, quantity, and subtotal so historical orders remain stable when catalog data changes.

`Payment` has a one-to-one relationship with an order and stores method, amount, optional transaction ID, status, and payment date. Payment is simulated. Cash on delivery starts pending; cash, bKash, Nagad, and card may be confirmed by a cashier or authorized manager/admin.

Relationships:

- `CustomerOrder 1 → many OrderItem`
- `OrderItem many → 1 ProductVariant`
- `CustomerOrder 1 → 1 Payment`

## Product Image Upload and Download

- Admin/manager product forms accept JPG, JPEG, PNG, WEBP, or GIF images up to 5 MB.
- The backend verifies extension, declared MIME type, and decodable image content.
- Files receive generated UUID names; original names are retained only as safe download metadata.
- Files are stored in a configurable `app.upload.dir` directory outside `src/main/resources`.
- A controlled `/product-images/{storedName}` endpoint displays images inline.
- A controlled `/products/{id}/image/download` endpoint downloads the original image name.
- Replacing or deleting an image removes the old stored file only after the database operation can proceed safely.
- Missing images use a built-in football-themed placeholder.

## Main Flows

### Registration and Login

The registration DTO validates name, email, phone, address, password strength, and password confirmation. The service normalizes email, rejects duplicates, hashes the password, and assigns `CUSTOMER`. Spring Security redirects each successful login to `/dashboard`, which routes content according to role.

### Catalog and Product Management

Public catalog pages support keyword, category, product-type, club/country, edition, kit-type, size, availability, minimum price, and maximum price filters. Admin/manager pages create and update products, upload/replace/download images, and add/update/delete variants with unique SKUs.

### Cart and Checkout

Customers choose a variant, quantity, and one of `NONE`, `PLAYER`, or `CUSTOM` printing. Custom printing requires a validated name and number. Checkout validates stock and delivery data, creates the order/payment transactionally, decrements variant stock, and clears the cart only after success.

### Staff Operations

Dashboards display role-appropriate counts, recent orders, low-stock variants, and sales/payment summaries. Order transitions are constrained to a simple workflow. Cashiers confirm payments. Admin/manager manage employees, while admin alone can create another admin or disable accounts with administrator privileges protected.

## UI Design

The storefront uses a navy/ink foundation, white surfaces, electric lime and blue football accents, large product photography, rounded product cards, filter chips, and responsive navigation. Staff pages use a collapsible dark sidebar, summary cards, searchable tables, status badges, validated forms, flash messages, confirmation modals, and the AFSOS-style logout popup.

All pages must remain usable on mobile widths. Forms show field-specific backend errors beside inputs. Uploaded product images use fixed aspect-ratio containers with `object-fit: cover` to protect layout consistency.

## Error Handling and Safety

- Invalid form submissions re-render with preserved input and field errors.
- Missing records return a friendly 404 page; unauthorized access returns a 403 page.
- File validation errors never disclose server paths.
- Service methods enforce ownership and role rules in addition to URL security.
- Checkout uses `@Transactional`, checks stock again on the server, prevents non-positive quantities, and stores money as `BigDecimal`.
- State-changing actions use POST and retain CSRF protection.

## Test Strategy

- DTO validation tests cover required fields, email, password confirmation, quantity, printing, and file rules.
- Service tests cover registration normalization, duplicate users, pricing, stock validation, checkout totals, stock decrement, role restrictions, and payment confirmation.
- Repository/JPA tests verify unique constraints and principal relationships with H2.
- MockMvc integration tests verify public/private routes, role access, registration, product image upload/download, cart/checkout, and ownership.
- A full Maven test run and package build are required before delivery.

## Deliverables

- Complete source project, tests, README, MySQL setup instructions, H2 demo profile, sample seed data, and configurable upload directory.
- A ZIP archive that excludes IDE metadata, build output, runtime uploads, secrets, and Git internals.
- Documented demonstration credentials for local sample data only.
