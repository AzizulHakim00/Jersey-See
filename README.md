# JerseySee

JerseySee is a premium, server-rendered football ecommerce storefront and store-management system. It combines a supporter-focused shopping experience with layered MVC development, validation, Spring Security roles, JPA relationships, variant-level stock, order/payment workflows, invoice downloads, and safe product-image handling.

## Features

- Public football merchandise catalog with search and filters.
- Jersey, training, outerwear, footwear, football, cap, gadget, and accessory products with size variants and SKU-level inventory.
- Customer registration, login, profile editing, session cart, printing choices, checkout, order history, cancellation, and invoice download.
- Staff dashboard, catalog/product/variant management, stock visibility, order workflow controls, payment confirmation, and employee management.
- Simulated Cash on Delivery, cash, bKash, Nagad, and card payments; no real financial gateway is contacted.
- Validated image uploads (JPG/JPEG/PNG/WEBP/GIF, maximum 5 MB), controlled image display/download endpoints, and a built-in placeholder when no image is uploaded.

## Architecture

The application follows a simple AFSOS-style layered flow:

`Controller → Service → Repository → Entity`

Thymeleaf renders the pages, Spring Security protects routes and service methods, Spring Data JPA persists the data, and the cart remains in the HTTP session. DTOs hold validated form input. Product images use a profile-selected storage contract: local profiles write outside the classpath, while production stores validated image bytes in MySQL so Render restarts do not lose them.

### Main relationships

| Relationship | Meaning |
| --- | --- |
| `User 1 → 0..1 EmployeeProfile` | Staff users can have an employee code, position, salary, joining date, and active flag. |
| `User 1 → many CustomerOrder` | Customers own their order history. |
| `Category 1 → many Product` | A product belongs to one category. |
| `Product 1 → many ProductVariant` | Each size/SKU holds its own stock and optional price adjustment. |
| `CustomerOrder 1 → many OrderItem` | Items preserve product/SKU/price snapshots for historical accuracy. |
| `OrderItem many → 1 ProductVariant` | An order item refers to the purchased variant. |
| `CustomerOrder 1 → 1 Payment` | Each checkout creates one simulated payment record. |

`User.demoSeedKey`, `Category.demoSeedKey`, `Product.demoSeedKey`, and `ProductVariant.demoSeedKey` are nullable, unique, bounded internal identifiers used by optional sample-data initialization. Normal/admin-created records leave these fields `null`; they are not customer-facing fields. Local demo profiles can safely adopt rows created by older JerseySee demo releases only when their full known identity matches; unrelated same-name records remain untouched. Production can initialize the catalog without creating any known-password account.

### Role permissions

| Role | Main permissions |
| --- | --- |
| `CUSTOMER` | Browse, cart, checkout, own orders/invoices, and profile. |
| `SALESMAN` | Staff dashboard, stock/catalog view, staff order view, and permitted order-status changes. |
| `CASHIER` | Staff order/payment view and pending-payment confirmation. |
| `MANAGER` | Products, variants, inventory, staff profiles, orders, reports, and payments; cannot create/disable administrators. |
| `ADMIN` | Full store operations, including permitted non-admin staff management. The demo admin exists only through trusted local initialization, not the public staff form. |

## Prerequisites

- Java 17
- Maven Wrapper included in this repository (or Maven 3.9+)
- MySQL 8+ for the default profile
- No MySQL installation is required for the local H2 demo profile

## IntelliJ direct run (default)

The project is configured for the confirmed local classroom setup. No `.env`, terminal command, or manual profile selection is required.
This release intentionally defaults to a local-only demo profile; use the explicit `production` profile described below for any deployment.

| Setting | Direct-run value |
| --- | --- |
| MySQL URL | `jdbc:mysql://localhost:3306/jerseysee` |
| Username | `root` |
| Password | `password` |
| Default profile | `intellij` |
| Schema management | Hibernate `update` |
| Demo catalog/accounts | Enabled for the local database |

In IntelliJ IDEA:

1. Open the inner project folder containing `pom.xml`.
2. Allow IntelliJ to import/sync the Maven project.
3. Open `src/main/java/bd/edu/seu/jerseysee/JerseySeeApplication.java`.
4. Click the green Run button beside `main`.
5. Open `http://localhost:8080`.

The JDBC URL can create the `jerseysee` schema when the local `root` account has permission; an already-created schema is reused. The demo initializer refuses to run against a non-loopback MySQL host. If the database came from an earlier JerseySee ZIP, recognizable legacy demo rows are assigned ownership keys and reconciled automatically; unrelated data is never adopted.

## Quick local demo (H2)

The `demo` profile uses an in-memory H2 database, `create-drop` schema lifecycle, local placeholder artwork, and `demo-uploads/`. It creates deterministic, local-only sample users, staff profiles, categories, products, variants, low-stock/out-of-stock examples, and no external secrets.

Linux/macOS:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Open `http://localhost:8080`. Stop the process with `Ctrl+C`; the in-memory demo data is discarded. To use a local H2 file instead, keep it out of source control and override `DEMO_DB_URL`, for example `jdbc:h2:file:./demo-data/jerseysee;MODE=MySQL;AUTO_SERVER=TRUE`. The demo schema remains `create-drop`, so this is only for local convenience, not persistence.

### Demo credentials

All accounts use the local-only password `Demo123!`.

| Role | Email |
| --- | --- |
| Customer | `customer@demo.local` |
| Salesman | `salesman@demo.local` |
| Cashier | `cashier@demo.local` |
| Manager | `manager@demo.local` |
| Admin | `admin@demo.local` |

Never reuse these addresses or password outside a local demonstration.

## Local MySQL demo with different credentials

Direct IntelliJ startup uses the local settings above. To run the seeded demo against another local MySQL account, create that account and provide `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` with the explicit `mysql-demo` profile:

```sql
CREATE DATABASE jerseysee CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'jerseysee_app'@'localhost' IDENTIFIED BY 'choose-a-local-password';
GRANT ALL PRIVILEGES ON jerseysee.* TO 'jerseysee_app'@'localhost';
FLUSH PRIVILEGES;
```

Linux/macOS:

```bash
export DB_URL='jdbc:mysql://localhost:3306/jerseysee?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export DB_USERNAME='jerseysee_app'
export DB_PASSWORD='choose-a-local-password'
export APP_UPLOAD_DIR='uploads'
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql-demo
```

Windows PowerShell:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/jerseysee?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:DB_USERNAME = 'jerseysee_app'
$env:DB_PASSWORD = 'choose-a-local-password'
$env:APP_UPLOAD_DIR = 'uploads'
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=mysql-demo"
```

The explicit `mysql-demo` profile creates the same five demo accounts and sample catalog listed above in MySQL. It is idempotent and only reconciles rows carrying its private seed keys. The bundled `root/password` values are for the confirmed local classroom setup only and must be replaced for any real deployment.

## Render + Aiven production deployment

The `production` profile never loads the known local demo accounts. It uses Hibernate `update` to create or update application tables, so the dedicated database account must have schema-change permission. Configure these Render secrets:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'production'
$env:JERSEYSEE_DB_URL = 'jdbc:mysql://database-host:3306/defaultdb?sslMode=REQUIRED&serverTimezone=UTC'
$env:JERSEYSEE_DB_USERNAME = 'database-user'
$env:JERSEYSEE_DB_PASSWORD = 'replace-with-a-secret'
$env:JERSEYSEE_DEMO_CATALOG_ENABLED = 'true'
$env:APP_SEED_ADMIN_ENABLED = 'true'
$env:APP_SEED_ADMIN_EMAIL = 'your-admin@example.com'
$env:APP_SEED_ADMIN_PASSWORD = 'Use-A-Unique1!Password'
```

The committed `render.yaml` already selects the production profile, Docker runtime, database-aware `/actuator/health` check, and catalog-only initializer. Render must supply all values marked `sync: false`. The administrator password must be 8–72 characters with upper- and lowercase letters, a digit, and a symbol; it is stored only as a BCrypt hash. Customers register through the storefront, and the administrator creates other staff accounts through authorized management pages.

Production images are stored in the same MySQL service, not Render's ephemeral filesystem. Do not set `APP_UPLOAD_DIR` in production. Do not use local classroom credentials or activate `demo`, `mysql-demo`, or `intellij` on a hosted service.

## Build, test, and package

Linux/macOS:

```bash
./mvnw clean test
./mvnw -DskipTests package
java -jar target/jersey-see-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd -DskipTests package
java -jar target\jersey-see-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo
```

The final JAR command explicitly uses the disposable H2 demo profile. A launch with no explicit profile intentionally uses the same local classroom configuration as the IntelliJ green-button flow; deployments must select `production`.

## Release archive

Create the release ZIP outside the repository. The packaging scripts create exactly one top-level `JerseySee-Complete/` directory and refuse to overwrite an existing archive or write inside the repository. They exclude Git/IDE metadata, `.superpowers`, build output, uploads, demo databases/logs, local environment files, secrets, and nested ZIP files.

Linux/macOS:

```bash
./scripts/package-release.sh /absolute/path/JerseySee-Complete.zip
unzip -l /absolute/path/JerseySee-Complete.zip
```

Windows PowerShell:

```powershell
.\scripts\package-release.ps1 -OutputPath C:\path\to\JerseySee-Complete.zip
Expand-Archive -LiteralPath C:\path\to\JerseySee-Complete.zip -DestinationPath C:\temp\JerseySee-Complete
```

Use a fresh output location when regenerating a release after review; the scripts deliberately do not overwrite an existing ZIP.

## Routes and workflows

| Area | Routes / workflow |
| --- | --- |
| Public | `/`, `/catalog`, `/products/{id}`, `/register`, `/login`, `/product-images/{storedName}` |
| Customer | `/cart` → `/checkout` → `/orders` → `/orders/{id}` → invoice download; `/profile` for account details. |
| Product image | Manager/admin upload from staff product form; inline view at `/product-images/{storedName}`; controlled download at `/products/{id}/image/download`. |
| Staff orders | `/staff/orders` and `/staff/orders/{id}`; salesman/manager/admin can perform permitted status transitions. |
| Payments | `/staff/payments`; cashier/admin can confirm eligible pending payments. |
| Management | `/staff/products` (manager/admin) and `/staff/employees` (manager/admin, with server-side role rules). |

Checkout recalculates prices and validates stock on the server, decreases variant stock transactionally, then creates the order, line-item snapshots, and simulated payment. Cash on delivery starts pending; an authorized staff member can confirm eligible payments. No card/bKash/Nagad details leave the application.

## Upload and download rules

- Accepted image types: JPG, JPEG, PNG, WEBP, GIF; maximum size: 5 MB.
- The server checks extension, MIME type, and decodable image content.
- Stored filenames are generated UUID names; the original filename is used only as safe download metadata.
- Local location is `uploads/` (or `demo-uploads/` in demo mode), outside `src/main/resources`; both are ignored by Git.
- Production stores image bytes in the `product_images` MySQL table, so container restarts do not remove them.
- Replacing/deleting a product image removes the old stored object only after the product database operation can safely proceed.

## Project tree

```text
src/main/java/bd/edu/seu/jerseysee/
  config/        security and profile-gated initialization
  controller/    MVC routes
  dto/           validated form objects
  model/         JPA entities and enums
  repository/    Spring Data repositories
  service/       business rules, storage, checkout, invoice generation
src/main/resources/
  application.properties       shared configuration and local-demo default profile
  application-intellij.properties  one-click local MySQL classroom values
  application-demo.properties  local H2 demo configuration
  application-production.properties  required non-demo deployment values
  static/                      CSS, JavaScript, placeholder assets
  templates/                   Thymeleaf views
src/test/                       unit, JPA, service, and MVC tests
```

## Screenshots for a viva

Take screenshots from your own running demo for: home/catalog filters, product detail and cart, checkout/order history, each role dashboard, staff products/stock, employee list, payments, and invoice download. Use local placeholder images or assets you are allowed to use; the repository intentionally does not include third-party copyrighted product photography.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Maven cannot download dependencies / `Unknown host repo.maven.apache.org` | Check DNS/network/proxy access to Maven Central, then rerun the Maven Wrapper. This environment currently cannot resolve Maven Central, so Maven test/package execution cannot be claimed as passed here. |
| Port 8080 already in use | Stop the other process or add `--server.port=8081`. |
| `Access denied for user 'root'` | The direct-run configuration expects the confirmed password `password`. If the MySQL account changes, edit the local values in `application-intellij.properties` or run the explicit `mysql-demo` profile with `DB_PASSWORD`. |
| `Public Key Retrieval is not allowed` | Keep the complete local JDBC URL in `application.properties`, including `allowPublicKeyRetrieval=true`. |
| Tables exist but catalog is empty | Confirm the Run console says the `intellij` default profile is active and that no different profile was selected in the Run Configuration. |
| Demo startup reports an existing email/category collision | An unrelated unkeyed row owns a reserved demo value and did not match the controlled legacy-demo identity. Rename that local row or use a non-demo profile; it will not be overwritten. |
| MySQL authentication/connection failure | Confirm MySQL is running on port 3306 and that the `root/password` login can access the `jerseysee` schema. |
| H2 demo starts with no data | Include `demo` exactly in the active profile and check startup logs for `DemoDataInitializer`. |
| Upload rejected | Use an allowed, decodable image at 5 MB or less and ensure the upload directory is writable. |
| Local upload/image missing after restart | Preserve the configured local upload directory; do not commit it. Production images are stored in MySQL and should survive Render restarts. |
| Render remains unhealthy | Open `/actuator/health`, verify all three `JERSEYSEE_DB_*` secrets, and confirm the JDBC URL uses Aiven's port and `sslMode=REQUIRED`. |

## Security notes

- Passwords are BCrypt hashes; only the documented local demo password is known in source and it is explicitly property-gated.
- Public registration always creates `CUSTOMER`; browser input cannot select a staff or administrator role.
- URL security and method-level authorization both protect management and ownership-sensitive actions.
- State-changing forms use POST with Spring Security CSRF protection.
- Order totals, stock, and payment status are server-controlled; browser totals are never trusted.
- Do not ship `.git`, `.idea`, `target`, `uploads`, `demo-uploads`, local databases, `.env`, or secrets in a release archive.

## Verification

GitHub Actions runs `./mvnw clean verify` for every push and pull request, then builds the production Docker image. The suite covers service rules, JPA relationships, seed ownership and legacy migration, real admin/customer authentication, premium page rendering, catalog/cart journeys, access control, health reporting, and both filesystem and MySQL-backed image storage. The local Codex workspace cannot resolve Maven Central, so GitHub's clean Ubuntu runner is the authoritative build environment for this repository.
