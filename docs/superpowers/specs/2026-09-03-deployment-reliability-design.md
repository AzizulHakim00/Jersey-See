# JerseySee Deployment Reliability Design

## Purpose

Make the GitHub project build, test, and deploy reproducibly while preserving the approved IntelliJ one-click workflow. A deployed instance must prove database readiness, initialize a usable storefront safely, keep uploaded product images across restarts, and expose clear diagnostics when configuration is incomplete.

## Runtime Profiles

- `intellij` remains the default local profile and connects to `localhost:3306/jerseysee` with the user-approved `root/password` classroom defaults.
- `test` uses isolated in-memory H2 configuration and never reads production database credentials.
- `production` requires `JERSEYSEE_DB_URL`, `JERSEYSEE_DB_USERNAME`, and `JERSEYSEE_DB_PASSWORD`; it never falls back to local credentials.
- Demo catalog creation in production is an explicit `JERSEYSEE_DEMO_DATA_ENABLED` choice. Seed ownership keys keep the process idempotent and prevent unrelated database rows from being overwritten.

## Hosted Image Persistence

Production product images are stored in the application database rather than Render's ephemeral filesystem. The existing public display and authorized download URLs remain unchanged. Local profiles may continue to use the filesystem while the storage service presents one controller-facing contract.

Database storage is selected because the project already requires Aiven MySQL and the user should not need a second cloud account or secret to deploy the academic application. The existing five-megabyte validation, decoded-image checks, safe filenames, content type, content length, and replacement behavior remain enforced.

## Health and Deployment Contract

- Add Spring Boot Actuator and expose only `health` and `info`.
- Render checks `/actuator/health`; datasource health must be part of readiness.
- The Docker image builds with Java 17, runs the packaged JAR as a non-root user, binds to Render's `PORT`, and has a container health check.
- `render.yaml` documents every required environment variable and enables a usable demo catalog explicitly for the hosted demonstration.
- A GitHub Actions workflow runs the complete Maven test suite and package build on pushes and pull requests.

## Functional Recovery Gate

Deployment is accepted only when automated MVC tests prove that:

- the demo administrator can authenticate and reach the role dashboard;
- an active product appears in the catalog;
- a customer can add an in-stock variant to the session cart;
- protected staff routes reject customer access;
- image upload, display, replacement, and download work through the selected storage backend.

## Documentation

The README provides two short paths: IntelliJ local MySQL and Render/Aiven. It lists exact environment variables, the health URL, seeded demo accounts, first-login expectations, image persistence behavior, and common failure messages. No real database password or private service URL is committed.

