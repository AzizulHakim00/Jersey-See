# JerseySee Deployment Reliability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make local IntelliJ startup and Render/Aiven deployment build reliably, initialize a usable secure demo storefront, persist hosted product images, and prove admin/customer/catalog/cart behavior with automated tests.

**Architecture:** Separate local, test, and production configuration so secrets cannot cross boundaries. Keep public controller URLs stable while selecting filesystem storage locally and database-backed image storage in production; use Actuator for datasource-aware health, GitHub Actions for builds, and Render Blueprint configuration for reproducible deployment.

**Tech Stack:** Java 17, Spring Boot 4.1.1, Spring MVC, Spring Data JPA, Spring Security, MySQL/Aiven, H2 tests, Thymeleaf, Spring Boot Actuator, Docker, Render, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-03-deployment-reliability-design.md`

## Global Constraints

- Preserve default IntelliJ MySQL database `localhost:3306/jerseysee` with `root/password` as explicitly approved by the user.
- Production must require `JERSEYSEE_DB_URL`, `JERSEYSEE_DB_USERNAME`, and `JERSEYSEE_DB_PASSWORD` and must never use local credential fallbacks.
- Test execution must use isolated H2 and require no network database or secret.
- Production image uploads must survive Render restarts without requiring a second cloud provider account.
- No real Aiven host, password, admin password, or other secret may be committed.

---

### Task 1: Restore configuration and build isolation

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.properties`
- Modify: `src/main/resources/application-intellij.properties`
- Modify: `src/main/resources/application-production.properties`
- Modify: `src/test/resources/application.properties`
- Create: `src/test/java/bd/edu/seu/jerseysee/config/ProfileConfigurationTest.java`

**Interfaces:**
- Consumes: Spring profile resolution and datasource properties.
- Produces: deterministic `intellij`, `test`, and `production` configuration; Actuator health endpoint.

- [ ] **Step 1: Add failing profile-isolation assertions**

```java
assertThat(testProperties).contains("jdbc:h2:mem:jerseysee");
assertThat(testProperties).doesNotContain("JERSEYSEE_DB_URL", "aivencloud.com");
assertThat(productionProperties).contains("${JERSEYSEE_DB_URL}", "app.demo-data.enabled=${JERSEYSEE_DEMO_DATA_ENABLED:false}");
```

- [ ] **Step 2: Run the profile test and confirm RED against GitHub main**

Run: `./mvnw -Dtest=ProfileConfigurationTest test`

Expected: FAIL because the current GitHub test properties reference production Aiven variables.

- [ ] **Step 3: Restore H2 tests and strict production properties**

Keep H2 `create-drop` settings under `src/test/resources`; add Actuator; expose `health,info`; bind `server.port=${PORT:8080}` and `server.address=0.0.0.0` only in production.

- [ ] **Step 4: Run configuration and context tests**

Run: `./mvnw -Dtest=ProfileConfigurationTest,JerseySeeApplicationTests test`

Expected: PASS without production database variables.

- [ ] **Step 5: Commit configuration repair**

```bash
git add pom.xml src/main/resources src/test/resources src/test/java/bd/edu/seu/jerseysee/config/ProfileConfigurationTest.java
git commit -m "fix: isolate local test and production profiles"
```

### Task 2: Persist production product images in MySQL

**Files:**
- Create: `src/main/java/bd/edu/seu/jerseysee/model/ProductImage.java`
- Create: `src/main/java/bd/edu/seu/jerseysee/repository/ProductImageRepository.java`
- Create: `src/main/java/bd/edu/seu/jerseysee/service/ProductImageStorage.java`
- Create: `src/main/java/bd/edu/seu/jerseysee/service/DatabaseProductImageStorage.java`
- Modify: `src/main/java/bd/edu/seu/jerseysee/service/FileStorageService.java`
- Modify: `src/main/java/bd/edu/seu/jerseysee/controller/ProductImageController.java`
- Modify: `src/main/java/bd/edu/seu/jerseysee/service/ProductService.java`
- Create: `src/test/java/bd/edu/seu/jerseysee/service/DatabaseProductImageStorageTest.java`
- Modify: `src/test/java/bd/edu/seu/jerseysee/controller/ProductImageControllerTest.java`

**Interfaces:**
- Consumes: validated image bytes, stored UUID filename, original filename, content type, and product lifecycle.
- Produces: `ProductImageStorage.store(...)`, `load(String)`, and `delete(String)` with filesystem implementation outside production and database implementation under `@Profile("production")`.

- [ ] **Step 1: Write failing storage lifecycle tests**

Prove upload bytes can be loaded after a new transaction, replacement removes the previous row only after commit, deletion removes the blob, missing names return `ResourceNotFoundException`, and a five-megabyte boundary is enforced by the existing validator.

- [ ] **Step 2: Run focused tests and confirm RED**

Run: `./mvnw -Dtest=DatabaseProductImageStorageTest,ProductImageControllerTest test`

Expected: FAIL because no database image storage implementation exists.

- [ ] **Step 3: Implement the database image entity and storage contract**

```java
@Entity
@Table(name = "product_images")
class ProductImage {
    @Id private String storedName;
    @Lob @Column(nullable = false, columnDefinition = "LONGBLOB") private byte[] content;
}
```

Return a Spring `ByteArrayResource` from database storage and retain safe `PathResource` behavior locally. Controllers continue to set content type, length, content disposition, and `X-Content-Type-Options: nosniff`.

- [ ] **Step 4: Run image and product service tests**

Run: `./mvnw -Dtest=DatabaseProductImageStorageTest,FileStorageServiceTest,ProductImageControllerTest,ProductServiceTest test`

Expected: PASS.

- [ ] **Step 5: Commit persistent storage**

```bash
git add src/main/java/bd/edu/seu/jerseysee/model/ProductImage.java src/main/java/bd/edu/seu/jerseysee/repository/ProductImageRepository.java src/main/java/bd/edu/seu/jerseysee/service src/main/java/bd/edu/seu/jerseysee/controller/ProductImageController.java src/test/java
git commit -m "feat: persist production product images"
```

### Task 3: Prove authentication, catalog, cart, and role recovery

**Files:**
- Modify: `src/main/java/bd/edu/seu/jerseysee/config/DemoDataInitializer.java`
- Create: `src/test/java/bd/edu/seu/jerseysee/controller/StorefrontSmokeTest.java`
- Modify: `src/test/java/bd/edu/seu/jerseysee/config/DemoDataInitializerTest.java`

**Interfaces:**
- Consumes: demo seed keys, BCrypt authentication, session cart, active product variants, and role authorization.
- Produces: repeatable demo users/catalog and end-to-end MockMvc evidence for the previously reported failures.

- [ ] **Step 1: Write failing user-journey smoke tests**

```java
mockMvc.perform(formLogin().user("admin@demo.local").password("Demo123!"))
       .andExpect(authenticated().withRoles("ADMIN"));
mockMvc.perform(get("/products")).andExpect(status().isOk());
mockMvc.perform(post("/cart/items").with(csrf()).param("variantId", variantId).param("quantity", "1"))
       .andExpect(redirectedUrl("/cart"));
```

Also assert a customer receives 403 for employee/product-management endpoints.

- [ ] **Step 2: Run the smoke tests and capture the exact failure**

Run: `./mvnw -Dtest=StorefrontSmokeTest,DemoDataInitializerTest test`

Expected: RED if seed reconciliation, authentication, catalog loading, cart binding, or role routing is broken.

- [ ] **Step 3: Apply only the root-cause seed or flow corrections identified by the tests**

Keep seed ownership keys, encode known demo passwords through `PasswordEncoder`, ensure active products have active in-stock variants, and avoid modifying unrelated users/categories/products.

- [ ] **Step 4: Run smoke, security, cart, and seed tests**

Run: `./mvnw -Dtest=StorefrontSmokeTest,DemoDataInitializerTest,SecurityAccessTest,CartServiceTest,UserServiceTest test`

Expected: PASS.

- [ ] **Step 5: Commit functional recovery**

```bash
git add src/main/java/bd/edu/seu/jerseysee/config/DemoDataInitializer.java src/test/java
git commit -m "fix: verify demo storefront user journeys"
```

### Task 4: Add reproducible CI and Render deployment

**Files:**
- Create: `.github/workflows/ci.yml`
- Modify: `Dockerfile`
- Modify: `render.yaml`
- Modify: `.dockerignore`
- Modify: `.gitignore`

**Interfaces:**
- Consumes: Maven wrapper, production profile, Render `PORT`, Aiven environment variables, Actuator health.
- Produces: green GitHub build and Render service with `/actuator/health` readiness.

- [ ] **Step 1: Add GitHub Actions build workflow**

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: maven
      - run: chmod +x mvnw && ./mvnw --batch-mode clean verify
```

- [ ] **Step 2: Harden the Docker runtime**

Build the JAR in a Maven/Temurin stage, copy it into a Java 17 JRE stage, create a non-root `jerseysee` user, expose 8080, and run with the production profile.

- [ ] **Step 3: Correct the Render Blueprint**

Set `healthCheckPath: /actuator/health`; require the three Aiven variables; set `JERSEYSEE_DEMO_DATA_ENABLED=true` for the hosted demonstration; keep passwords `sync: false`; remove `/tmp` upload configuration because production images use MySQL.

- [ ] **Step 4: Validate deployment files**

Run: `docker build -t jerseysee:verify .` and `docker run --rm -e SPRING_PROFILES_ACTIVE=production jerseysee:verify` without DB variables.

Expected: image build exits 0; container exits quickly with a clear missing-production-configuration error rather than connecting to localhost.

- [ ] **Step 5: Commit CI and deployment**

```bash
git add .github/workflows/ci.yml Dockerfile render.yaml .dockerignore .gitignore
git commit -m "ci: add verified Render deployment"
```

### Task 5: Document and release the repaired deployment

**Files:**
- Modify: `README.md`
- Modify: `RENDER-AIVEN-QUICKSTART.txt`

**Interfaces:**
- Consumes: final local and hosted configuration contract.
- Produces: exact IntelliJ and Render/Aiven runbooks, validation evidence, and updated GitHub pull request.

- [ ] **Step 1: Document one-click IntelliJ startup**

State the required local MySQL service, `jerseysee` database behavior, `root/password`, Run action, URL, and demo credentials.

- [ ] **Step 2: Document Render/Aiven deployment**

List `JERSEYSEE_DB_URL`, `JERSEYSEE_DB_USERNAME`, `JERSEYSEE_DB_PASSWORD`, optional seed-admin variables, demo toggle, MySQL JDBC TLS URL format, `/actuator/health`, and image persistence.

- [ ] **Step 3: Run the entire release gate**

Run: `./mvnw clean verify`, `./mvnw -DskipTests package`, `docker build -t jerseysee:verify .`, and a repository secret scan for committed JDBC hosts/passwords.

Expected: all builds/tests exit 0 and the secret scan finds only documented variable names and approved local `root/password` classroom defaults.

- [ ] **Step 4: Push and update the pull request**

```bash
git push origin codex/premium-storefront
```

Update the pull request with CI URL, health endpoint, deployment variables, functional smoke evidence, and screenshots.

- [ ] **Step 5: Verify the hosted service after Render deploys**

Check `/actuator/health`, homepage product visibility, admin login/dashboard, customer login, product detail, add-to-cart, and a small product image upload/display/download. Record any platform-level blocker separately from application defects.

