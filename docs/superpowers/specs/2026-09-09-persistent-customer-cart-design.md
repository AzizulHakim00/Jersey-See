# Persistent Customer Cart Design

## Goal

Make every authenticated customer's shopping cart database-backed so add/update/remove actions persist immediately and survive logout, login, browser restarts, session expiration, Render restarts, and application redeploys. Checkout must consume the persistent cart transactionally and clear it only after the order has been created successfully.

This extends the production persistence protection already deployed for customer profiles, staff/admin product edits, stock, product status, product images, orders, and payments. The remaining session-only cart state will no longer be authoritative.

## Current Problem

The current cart is stored in `ShoppingCart`, a serializable in-memory object placed in the HTTP session by both `CartController` and `OrderController` using `@SessionAttributes("shoppingCart")`. `GlobalModelAttributes` also derives the navbar cart count from that session object. Therefore the cart disappears when the session disappears and cannot be recovered after logout/login or application restart.

The rest of the important ecommerce write paths are already repository-backed: product/catalog changes use JPA, customer/staff account changes use JPA, orders and order workflow state use JPA, payments use JPA, and product image bytes are stored in the database. The architectural gap is the cart.

## Chosen Architecture

Use one table of persistent cart lines keyed directly to the authenticated customer instead of introducing a separate cart header table. A customer's cart is the set of `customer_cart_item` rows whose `customer_id` matches that user.

This keeps the design small while still providing exactly one logical cart per customer. It avoids creating an otherwise empty cart header row for every account and simplifies clearing a cart after successful checkout.

### New JPA entity: `CustomerCartItem`

Create `bd.edu.seu.jerseysee.model.CustomerCartItem` with these persisted fields:

- `id: Long` — auto-generated primary key.
- `lineId: String` — stable UUID string used by existing cart update/remove URLs; unique and not null.
- `customer: User` — required many-to-one relationship to the owning customer.
- `productVariant: ProductVariant` — required many-to-one relationship to the selected variant.
- `quantity: int` — validated range 1 through 10.
- `printingType: PrintingType` — required enum.
- `printingName: String` — nullable, maximum 50 characters.
- `printingNumber: String` — nullable, maximum 2 characters.

Do **not** persist product name, image name, SKU, size, unit price, printing charge, or line subtotal in the cart table. Those are derived from the current `ProductVariant`/`Product` records whenever the cart is loaded. This prevents stale cart prices and ensures checkout always uses current catalog data.

A product/variant that changes price after being added will show and charge the current price. If stock becomes insufficient or a product becomes inactive, checkout will reject the cart without deleting it so the customer can remove or adjust the affected line.

## Database Migration

Production currently has `spring.jpa.hibernate.ddl-auto=none`, so adding a JPA entity alone cannot create the new table safely.

Add Flyway for explicit production schema migration:

- Add `org.flywaydb:flyway-core` and MySQL support (`org.flywaydb:flyway-mysql`) under Spring Boot dependency management.
- Keep Flyway disabled by default in `application.properties` so existing development/test profiles that use Hibernate schema creation continue to behave as they do now.
- Enable Flyway in `application-production.properties`.
- Enable `baseline-on-migrate` for the existing non-empty production schema, with baseline version `1`.
- Add `src/main/resources/db/migration/V2__create_customer_cart_item.sql`.

The migration will create `customer_cart_item` with indexes on `customer_id` and `product_variant_id`, a unique constraint on `line_id`, a foreign key to the user table, and a foreign key to `product_variant`.

Deleting a customer should cascade-delete that customer's cart rows. Deleting a product variant should also cascade-delete cart rows that reference that variant so administrative product deletion is not blocked by abandoned carts.

The migration must not modify or recreate any existing customer, product, order, payment, or image table and must not enable the disabled production demo seeders.

### Transition from the current session cart

There is no reliable way to migrate an already-open in-memory HTTP session cart through the deployment that replaces the application process, because those cart objects are not in MySQL and the process restart can discard them before the new code can read them. Therefore this deployment has one explicit transition rule: carts created **after** the persistent-cart release are durable; any cart that exists only in an old server session before the release may be lost once during that deployment.

Do not add a permanent session fallback or dual-write mechanism. After the release, MySQL is the single source of truth for cart state.

## Repository Layer

Create `CustomerCartItemRepository extends JpaRepository<CustomerCartItem, Long>` with focused ownership-safe queries:

- load all lines for one customer with variant/product data eagerly available;
- find one line by `lineId` and customer ID;
- delete one line by ownership-safe lookup;
- delete all lines for a customer;
- calculate total quantity for a customer's navbar count;
- load all customer lines using `PESSIMISTIC_WRITE` for checkout so concurrent checkout/cart mutation cannot consume the same cart twice.

No controller or service method will accept a customer ID from the browser. The owner always comes from the authenticated principal resolved through `UserService`.

## Cart Service

Refactor `CartService` from mutating a session `ShoppingCart` to owning the database cart workflow.

Public behavior:

- `ShoppingCart getCart(User customer)` — query persistent rows and build the existing `ShoppingCart`/`CartItem` view model from current product/variant data.
- `void add(User customer, AddToCartDTO input)` — validate current variant/product availability and printing selection, then immediately insert or merge a persistent line.
- `void updateQuantity(User customer, String lineId, int quantity)` — ownership-safe update followed by `saveAndFlush`.
- `void remove(User customer, String lineId)` — ownership-safe deletion followed by flush.
- `int getTotalQuantity(User customer)` — database-backed navbar count.
- package/service-level checkout support that loads the customer's persistent lines under a database write lock and clears them within the checkout transaction only after order creation succeeds.

The existing line-merging rule remains: the same variant plus the same printing type/name/number is one cart line, and adding it again increases quantity up to the existing maximum of 10.

`ShoppingCart` and `CartItem` remain lightweight view/domain projection classes so Thymeleaf templates need minimal change. They stop being stored in the HTTP session.

## Controller Changes

### `CartController`

Remove `@SessionAttributes("shoppingCart")` and remove the controller-created `@ModelAttribute("shoppingCart")` session object.

Each request resolves the authenticated customer from `Authentication` through `UserService` and calls `CartService`:

- `GET /cart` loads the persistent cart into the model as `shoppingCart`.
- `POST /cart/items` saves the add action immediately, then redirects.
- quantity updates save immediately.
- removal deletes immediately.
- validation errors reload the database cart before rendering the page.

Authorization remains customer-only.

### `OrderController`

Remove `@SessionAttributes("shoppingCart")` and its session cart factory.

- `GET /checkout` loads the current persistent cart from `CartService` for display.
- `POST /checkout` resolves the authenticated customer and asks `OrderService` to checkout that customer's database cart. It no longer accepts a client/session `ShoppingCart` as the source of truth.
- On validation or business-rule failure, the persistent cart remains intact and is reloaded for the checkout page.
- On successful checkout, the database cart is cleared inside the same transaction as order creation.

### `GlobalModelAttributes`

Remove `HttpSession` cart inspection. For an authenticated enabled customer, obtain `cartCount` from `CartService.getTotalQuantity(customer)`. Anonymous users and non-customer staff receive a cart count of zero without a cart query.

This makes navbar cart state consistent across browser sessions and server restarts.

## Checkout Transaction

Checkout is the most important data-integrity boundary.

`OrderService.checkout` will take the authenticated `User` plus `CheckoutDTO`; it will not trust a session cart argument.

Within one Spring transaction:

1. Validate that the actor is an enabled customer.
2. Load and lock the customer's persistent cart lines.
3. Reject an empty cart.
4. Lock referenced `ProductVariant` rows in deterministic ID order using the existing stock-locking behavior.
5. Revalidate product active state, quantities, printing selection, current stock, and checkout data.
6. Calculate all prices from the current variant/product records.
7. Create and persist the `CustomerOrder`, `OrderItem` snapshots, and `Payment` exactly as today.
8. Decrement variant stock.
9. Delete the customer's persistent cart lines.
10. Commit once.

If any step fails, the transaction rolls back: no order is partially created, stock is not partially decremented, and the cart is not cleared.

This also makes double-submission safer because the first checkout holds the cart/variant locks until completion; a later request sees the cleared cart rather than creating a second order from the same lines.

## Existing Persistence Behavior to Preserve

The cart work must not regress the persistence fixes already deployed:

- `JERSEYSEE_PUBLIC_DEMO_ENABLED` remains `false` in production.
- production public-demo initializers remain disabled.
- `spring.jpa.hibernate.ddl-auto=none` remains in production.
- product images remain database-backed.
- customer profile edits remain database-backed.
- admin product price/stock/status/featured/image edits remain database-backed.
- employee creation and staff changes remain database-backed.
- order creation/cancellation/status updates remain database-backed.
- payment confirmations remain database-backed.

No production seeder may overwrite user or staff changes after this migration.

## Error Handling

Preserve the current customer-facing validation messages where practical.

Specific persistent-cart behavior:

- Unknown or other-customer `lineId` -> same safe "cart line not found" style error; never reveal another customer's cart.
- Disabled/inactive product -> reject checkout/add with an availability error.
- Insufficient stock -> reject checkout and keep the cart unchanged.
- Quantity outside 1..10 -> reject without modifying the database line.
- Invalid printing name/number/type -> reject without modifying the database line.
- Database/migration failure -> deployment must fail rather than silently falling back to session storage.

There will be no session-cart fallback, because two competing sources of truth would recreate the persistence problem.

## Test Strategy

Implementation follows TDD. Required regression coverage:

1. A new cart item is written to the database for the authenticated customer.
2. Loading a cart in a new request/context returns the previously saved item.
3. Cart quantity and removal changes persist after repository reload.
4. Two customers cannot read/update/remove each other's cart lines.
5. Navbar cart count comes from persistent rows, not HTTP session state.
6. A changed product price is reflected when the persisted cart is reloaded.
7. Successful checkout creates the order, decrements stock, and clears persistent cart rows in one transaction.
8. Failed checkout (insufficient stock or invalid checkout data) leaves persistent cart rows unchanged.
9. Checkout cannot create a second order from the same cleared cart.
10. Production profile has Flyway enabled while Hibernate DDL remains `none` and public-demo reseeding remains disabled.
11. The migration SQL contains the required table, ownership constraints, and cascade behavior.
12. Existing `CartServiceTest`, order tests, security tests, storefront tests, persistence-protection tests, and deployment contract tests remain green after adaptation.

The live production smoke workflow will be extended to exercise customer login, add-to-cart, logout/login, and cart recovery before checkout-related assertions. Test-created cart data must be cleaned up through the application flow where practical so repeated smoke runs are safe.

## Deployment and Verification

After implementation:

1. Run the targeted new cart persistence tests and verify the RED -> GREEN sequence.
2. Run the complete Maven `clean verify` suite.
3. Build the production Docker image.
4. Review the full PR diff for accidental seeder/config regressions.
5. Merge only with all checks green.
6. Let Render auto-deploy the exact merge commit.
7. Confirm the Render deployment reaches `LIVE` and Flyway migration succeeds.
8. Run a fresh live smoke test against the deployed commit.
9. Verify a customer cart survives logout/login in production.
10. Verify a persisted cart remains present across a controlled Render redeploy/restart without resetting customer/admin data.

## Success Criteria

The implementation is complete only when all of the following are true:

- customer cart contents are stored in MySQL, not the HTTP session;
- add/update/remove actions persist immediately;
- cart contents and navbar count survive logout/login, browser/session changes, and Render restart/redeploy;
- checkout reads only the database cart and clears it only on successful commit;
- failed checkout preserves the cart;
- current catalog price/availability/stock are revalidated at checkout;
- one customer cannot access another customer's cart lines;
- existing admin/customer/staff/order/payment persistence remains unchanged;
- Maven, Docker, Render deploy, and live production smoke verification all pass on the same final commit.
