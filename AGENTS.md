# AGENTS.md

## Repository Overview

`migration-utility` is a small collection of standalone, one-off tools used
during MOSIP version upgrades to migrate or re-encrypt data that lives in
Postgres databases (and, in one case, MinIO/S3 object stores). It is not a
long-running MOSIP service — each tool here is run manually or as a
short-lived job during a specific upgrade window, then normally left idle.

The repo has two independent Java/Spring Boot modules plus two standalone SQL
scripts, each covering a different upgrade path:

| Path | Purpose | Guide |
|------|---------|-------|
| [`pms-115-120/`](pms-115-120/AGENTS.md) | Migrates/publishes Partner Management Service (PMS) data to IDA via websub, for the 1.1.5 → 1.2.0.1 upgrade | [pms-115-120/AGENTS.md](pms-115-120/AGENTS.md) |
| [`pre-reg-113-115/`](pre-reg-113-115/AGENTS.md) | Re-encrypts pre-registration demographic/document data when the encryption key changed from `REGISTRATION` to `PRE_REGISTRATION` (1.1.3 → 1.1.5/1.2.0.1) | [pre-reg-113-115/AGENTS.md](pre-reg-113-115/AGENTS.md) |
| `db_scripts/ArchivalScript.sql` | Standalone SQL script that moves pre-registration records into `*_CONSUMED` tables | Documented below and in [pre-reg-113-115/AGENTS.md](pre-reg-113-115/AGENTS.md) (it is the companion script for that module) |

There is no shared parent build (no root `pom.xml`) — the two Java modules
build and release independently, each with their own Maven project and
Dockerfile.

## Technology Stack

- Java 21, Spring Boot 3.2.3, Maven
- PostgreSQL (JDBC/Hibernate) as the data source for both Java modules
- MinIO/S3-compatible object storage (`pre-reg-113-115` only)
- MOSIP kernel libraries (`kernel-core`, `kernel-dataaccess-hibernate`,
  `kernel-websubclient-api`, `khazana`) pulled from MOSIP's Maven
  repositories, plus Spring Cloud Config for externalized configuration
- Docker (`eclipse-temurin:21-jre-alpine` base images) for packaging
- GitHub Actions (reusable workflows hosted in `mosip/kattu`) for CI

## Build & Test Commands

There is no root-level build — build each module from inside its own
directory. See the per-module guides for exact commands:

- [pms-115-120/AGENTS.md](pms-115-120/AGENTS.md)
- [pre-reg-113-115/AGENTS.md](pre-reg-113-115/AGENTS.md)

Neither module has a `src/test` directory in this repository — there are no
automated unit or integration tests to run. `mvn test` will complete without
executing any project tests; it does not run tests packaged in transitive
dependencies.

## Configuration

Both modules use Spring Cloud Config plus local `application*.properties`
files that are already checked into the repo with **example/dev
credentials** (e.g. `mosip.pms.secretKey=abc123`,
`javax.persistence.jdbc.password=mosip123`,
`object.store.s3.secretkey=minioadmin`). These are placeholder values for the
MOSIP dev/QA sandbox, not real secrets — but treat them as such: never
replace them with real production credentials in a committed file. Real
deployments override these via Spring Cloud Config (see
`spring.cloud.config.uri` / `spring.cloud.config.label`) or environment
variables passed at Docker runtime.

See each module's guide for the specific properties files involved.

## Project Structure Notes

```text
migration-utility/
├── db_scripts/ArchivalScript.sql       # standalone SQL, pre-reg archival
├── pms-115-120/                        # PMS -> IDA migration utility (Java)
│   ├── utility/                        # the actual Maven project
│   └── db_scripts/                     # SQL run against the PMS DB
├── pre-reg-113-115/                    # pre-reg re-encryption utility (Java)
└── .github/workflows/                  # CI (build, release, tag)
```

Note the asymmetry: for `pms-115-120` the Maven project lives one level down,
in `pms-115-120/utility/`; for `pre-reg-113-115` the Maven project is at the
top of that directory. This is reflected in the CI `SERVICE_LOCATION` values
(see below) and in each module's own guide.

## Development Workflow

1. Pick the module you're changing and read its `AGENTS.md` first — the two
   modules are unrelated in code (different Java packages, no shared
   dependency between them) and target different MOSIP upgrade paths.
2. Make changes inside that module's directory only; do not introduce
   cross-module dependencies.
3. Build the module locally with Maven (see the module guide) before
   committing — there is no CI step you can run locally that substitutes for
   this, since both modules use MOSIP-internal `kernel-*` artifacts that only
   resolve against MOSIP's Nexus/Maven repositories.
4. If you touch SQL under `db_scripts/`, treat it as production-impacting:
   review it manually against a non-production database first. See the
   Repository-Specific Considerations section below.

## Pull Request Guidelines

- CI (`.github/workflows/push-trigger.yml`) runs on `pull_request` events of
  type `opened`, `reopened`, `synchronize`, and builds **both** modules
  independently via reusable workflows in `mosip/kattu`
  (`build-maven-migration-utility` for `pms-115-120/utility`,
  `build-maven-pre-reg-113-115` for `pre-reg-113-115`), followed by Docker
  image builds for each. A PR that only touches one module still triggers
  builds for both, so check the Actions output for the module you actually
  changed.
- Keep commits scoped to one module where possible, since the two utilities
  are released and versioned independently.
- Update the relevant module's `README.md` if you change setup steps,
  properties, or deployment instructions — this `AGENTS.md` tree summarizes
  behavior but the READMEs are still the canonical operator-facing docs.

## Repository-Specific Considerations

- **This repo runs destructive SQL against real MOSIP databases.** Both
  `db_scripts/ArchivalScript.sql` and `pms-115-120/db_scripts/migration-scripts.sql`
  are meant to be hand-run by an operator during a planned upgrade window,
  not executed automatically. `ArchivalScript.sql` `DELETE`s rows from
  `prereg.applicant_demographic`, `prereg.applicant_document`, and
  `prereg.reg_appointment` after copying them into `*_consumed`/`CONSUMED`
  tables — see the Do-not rules below and in
  [pre-reg-113-115/AGENTS.md](pre-reg-113-115/AGENTS.md).
- Both Java modules disable Hibernate's own datasource/entity-manager
  autoconfiguration (`DataSourceAutoConfiguration`,
  `DataSourceTransactionManagerAutoConfiguration`,
  `HibernateJpaAutoConfiguration` are excluded in each `Application` /
  `ReEncryptUtilityApplication` class) and wire persistence manually — do not
  "fix" this by re-enabling autoconfiguration without checking why it was
  excluded.
- Each module's Docker image renames its jar to a fixed name inside the
  image (`Utility-0.0.1-SNAPSHOT.jar` for `pms-115-120`,
  `ReEncryptUtility.jar` for `pre-reg-113-115`) regardless of the Maven
  version in `pom.xml`. Don't assume the jar filename mentioned in a README
  matches the version in `pom.xml`.

## Agent rules

### Do

1. Read the relevant module's `AGENTS.md` and `README.md` before editing
   anything in it.
2. Keep changes to one module per PR/commit where practical.
3. Treat every SQL script under `db_scripts/` as something that runs once,
   by hand, against a real environment — review it as carefully as
   production-impacting code, even though it isn't wired into any pipeline.
4. Verify Maven builds locally from inside the correct module directory
   before assuming CI will catch problems.
5. Preserve the `-D` system-property-before-`-jar` ordering in any Java
   command example you write or edit (e.g.
   `java -Dloader.path=... -jar app.jar`), matching the existing Dockerfiles.

### Do not

1. Do not run `ArchivalScript.sql` or `migration-scripts.sql` against a
   database you are not certain is disposable/non-production — they
   permanently delete or move rows.
2. Do not commit real credentials into any `application*.properties` file;
   the existing dev/QA values in this repo are illustrative only.
3. Do not merge the two modules' dependencies or source trees together —
   they are independently versioned and released.
4. Do not assume there are tests to run — there is no `src/test` in either
   module today. Do not report "tests pass" or "tests added" unless you
   actually added a test directory and ran it.
5. Do not add a nested `AGENTS.md` for `db_scripts/` folders — they are
   single-file, hand-run SQL with no build/run process of their own; the
   parent module guide (or this file) is the right place to document them.
