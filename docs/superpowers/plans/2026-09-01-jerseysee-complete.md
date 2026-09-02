# JerseySee Complete Store Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete, testable football merchandise storefront and role-based store-management application inside the existing JerseySee Spring Boot project.

**Architecture:** Use server-rendered Spring MVC with a simple Controller → Service → Repository → Entity structure. Keep the cart in the HTTP session, persist users/catalog/orders/payments with JPA, and isolate uploaded-image handling behind a focused storage service.

**Tech Stack:** Java 17, Spring Boot 4.1.1, Maven, Spring MVC, Thymeleaf, Spring Security, Spring Data JPA, Bean Validation, MySQL, H2 tests, HTML/CSS/vanilla JavaScript.

**Spec:** `docs/superpowers/specs/2026-09-01-jerseysee-complete-design.md`

## Global Constraints

- Preserve `bd.edu.seu.jerseysee` and the `jersey-see` artifact name.
- Keep code understandable for a university viva; no React, JWT, microservices, or real payment gateway.
- Use `BigDecimal` for money, DTO validation for forms, BCrypt passwords, CSRF-protected POST mutations, and `@Transactional` checkout.
- Product images accept only validated JPG/JPEG/PNG/WEBP/GIF content up to 5 MB and live under configurable `app.upload.dir`.
- Every behavioral production change follows a failing-test → minimal-code → passing-test cycle.

---

### Task 1: Compilable Baseline and Domain Contracts

**Files:**
- Modify: `pom.xml`, `src/main/resources/application.properties`
- Create: `src/test/resources/application.properties`
- Replace legacy skeleton packages with focused `model`, `model/enums`, `dto`, and `repository` files described by the design.
- Test: `src/test/java/bd/edu/seu/jerseysee/model/DomainRelationshipTest.java`

**Interfaces:**
- Produces repositories for `User`, `EmployeeProfile`, `Category`, `Product`, `ProductVariant`, `CustomerOrder`, `OrderItem`, and `Payment`.
- Produces enums `Role`, `ProductType`, `KitType`, `JerseyEdition`, `SleeveType`, `SizeOption`, `PrintingType`, `OrderStatus`, `PaymentMethod`, and `PaymentStatus`.

- [ ] Write a JPA test that persists the core relationships and verifies a product variant and order item can be reloaded with stable values.
- [ ] Run `./mvnw -Dtest=DomainRelationshipTest test` and confirm compilation/test failure because the domain does not exist.
- [ ] Add dependencies/configuration and the minimum entities, enums, and repositories needed by the test.
- [ ] Re-run the focused test and then `./mvnw test`; both must pass.

### Task 2: Authentication, Profiles, and Staff Management

**Files:**
- Create: `config/SecurityConfig.java`, `config/DataInitializer.java`
- Create: `service/CustomUserDetailsService.java`, `service/UserService.java`, `service/EmployeeService.java`
- Create: `dto/RegistrationDTO.java`, `dto/ProfileDTO.java`, `dto/EmployeeDTO.java`
- Create: `controller/AuthController.java`, `controller/ProfileController.java`, `controller/EmployeeController.java`, `controller/DashboardController.java`
- Test: `service/UserServiceTest.java`, `controller/SecurityAccessTest.java`

**Interfaces:**
- `UserService.register(RegistrationDTO)` returns the persisted customer and rejects duplicate or mismatched credentials.
- `EmployeeService.create(EmployeeDTO)` creates a staff user and linked profile while protecting administrator operations.

- [ ] Write tests for normalized customer registration, BCrypt storage, duplicate rejection, protected staff creation, and role route access.
- [ ] Run focused tests and verify expected failures because services/security are absent.
- [ ] Implement the minimum DTOs, services, configuration, and controllers to pass.
- [ ] Re-run focused and full test suites.

### Task 3: Catalog, Variants, and Image Files

**Files:**
- Create: `dto/ProductDTO.java`, `dto/ProductVariantDTO.java`, `dto/CatalogFilter.java`
- Create: `service/ProductService.java`, `service/FileStorageService.java`
- Create: `controller/HomeController.java`, `controller/ProductController.java`, `controller/AdminProductController.java`, `controller/ProductImageController.java`
- Create: `exception/InvalidFileException.java`, `exception/ResourceNotFoundException.java`
- Test: `service/ProductServiceTest.java`, `service/FileStorageServiceTest.java`, `controller/ProductImageControllerTest.java`

**Interfaces:**
- `FileStorageService.store(MultipartFile)` returns immutable stored/original-name/content-type/size metadata.
- `ProductService.create(ProductDTO, MultipartFile)` and `update(...)` manage catalog data and image replacement.
- `ProductService.search(CatalogFilter, Pageable)` returns active catalog results.

- [ ] Write tests rejecting oversized, non-image, path-like, extension/MIME-mismatched, and undecodable files; test valid upload/display/download and per-size stock.
- [ ] Run focused tests and confirm expected failures.
- [ ] Implement storage, product/variant services, filtering, controllers, and exception handling.
- [ ] Re-run focused and full test suites.

### Task 4: Cart, Checkout, Orders, Payments, and Invoice Download

**Files:**
- Create: `cart/ShoppingCart.java`, `cart/CartItem.java`
- Create: `dto/AddToCartDTO.java`, `dto/CheckoutDTO.java`, `dto/PaymentConfirmationDTO.java`
- Create: `service/CartService.java`, `service/OrderService.java`, `service/PaymentService.java`, `service/InvoiceService.java`
- Create: `controller/CartController.java`, `controller/OrderController.java`, `controller/PaymentController.java`
- Test: `service/CartServiceTest.java`, `service/OrderServiceTest.java`, `controller/OrderOwnershipTest.java`

**Interfaces:**
- `CartService.add(ShoppingCart, AddToCartDTO)` validates variant, printing, quantity, and calculates line values.
- `OrderService.checkout(User, ShoppingCart, CheckoutDTO)` transactionally revalidates/decrements stock and returns a complete order/payment.
- `InvoiceService.createTextInvoice(CustomerOrder)` returns downloadable UTF-8 invoice bytes.

- [ ] Write tests for price calculation, custom printing requirements, insufficient stock, authoritative server totals, stock decrement, cart clearing after success, and order ownership.
- [ ] Run focused tests and confirm expected failures.
- [ ] Implement the minimum cart/order/payment/invoice behavior and controllers.
- [ ] Re-run focused and full test suites.

### Task 5: Responsive Storefront and Role Dashboards

**Files:**
- Create all Thymeleaf pages under `src/main/resources/templates/` for storefront, auth, profile, cart, checkout, orders, products, employees, payments, dashboards, and errors.
- Create: `templates/fragments/navigation.html`, `templates/fragments/messages.html`, `templates/fragments/admin-sidebar.html`
- Create: `static/css/jerseysee.css`, `static/js/app.js`, `static/images/product-placeholder.svg`, `static/images/brand-mark.svg`
- Test: `controller/PageRenderingTest.java`

**Interfaces:**
- Templates consume controller model attributes and Spring Security/Thymeleaf authorization metadata.
- JavaScript only enhances navigation, file preview, filters, quantity controls, and confirmation/logout modals.

- [ ] Write MockMvc rendering tests for public catalog, product detail, authenticated cart, each staff dashboard, validation errors, and 403/404 pages.
- [ ] Run rendering tests and confirm expected missing-view failures.
- [ ] Implement semantic responsive templates, CSS system, SVG assets, and small JavaScript enhancements.
- [ ] Re-run rendering and full test suites.

### Task 6: Demo Data, Documentation, and Release Verification

**Files:**
- Modify: `config/DataInitializer.java`, `.gitignore`
- Create: `README.md`, `src/main/resources/application-demo.properties`
- Create release archive: `JerseySee-Complete.zip`
- Test: complete Maven suite plus application smoke test.

**Interfaces:**
- Demo profile starts with H2 and sample accounts/products; default profile documents MySQL settings through environment variables.

- [ ] Add deterministic demo categories, products, variants, and local-only accounts when the demo profile is active.
- [ ] Document setup, MySQL creation, demo mode, role credentials, upload/download behavior, workflows, and project structure.
- [ ] Run `./mvnw clean test` and `./mvnw -DskipTests package` from a clean state.
- [ ] Start the packaged app with the demo profile, verify the health of public/login/catalog endpoints, then stop it.
- [ ] Build a clean ZIP excluding `.git`, `.idea`, `target`, runtime uploads, and secrets; inspect its contents before delivery.
