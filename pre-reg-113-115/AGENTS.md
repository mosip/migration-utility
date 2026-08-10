# AGENTS.md

[Back to repo root](../AGENTS.md)

## Purpose

Re-encrypts MOSIP pre-registration demographic and document data when the
encryption key name changed from `REGISTRATION` (MOSIP 1.1.3) to
`PRE_REGISTRATION` (MOSIP 1.1.5+). Used during the 1.1.3 → 1.1.5 upgrade, and
still applicable for a 1.1.5 → 1.2.0.1 upgrade even if 1.1.5 still uses the
old key name. It supports two scenarios: re-encrypting across two separate
environments (different key sets), or re-encrypting in place after an
in-environment application upgrade (same key set).

The companion SQL script for this module — `../db_scripts/ArchivalScript.sql`
— lives one level up, at the repo root, not inside this directory.

## Layout

```text
pre-reg-113-115/
├── README.md              # scenarios, prerequisites, docker run steps, properties reference
├── Dockerfile
├── pom.xml                # artifactId "ReEncryptUtility", groupId com.ReEncryptUtility
├── mvnw / mvnw.cmd
├── .gitignore
└── src/main/
    ├── java/com/reencryptutility/
    │   ├── ReEncryptUtilityApplication.java   # Spring Boot entry point (CommandLineRunner)
    │   ├── dto/                               # CryptoManagerRequestDTO/ResponseDTO, RequestWrapper, ResponseWrapper
    │   ├── entity/                            # DemographicEntity, DocumentEntity
    │   ├── repository/                        # DemographicRepository, DocumentRepository
    │   └── service/
    │       ├── ReEncrypt.java                 # core re-encryption logic, invoked from Application.run()
    │       ├── Database.java, DatabaseRouter.java, DatabaseThreadContext.java, RoutingDataSource.java
    └── resources/application.properties       # primary/secondary datasource, object store, crypto settings
```

There is no `src/test` directory in this module.

## How to run

Build locally with the wrapper (Maven not required on PATH):

```shell
./mvnw install -DskipTests=true -Dmaven.javadoc.skip=true -Dgpg.skip=true
java -jar target/ReEncryptUtility-1.2.1-SNAPSHOT.jar
```

The jar name follows Spring Boot's default `artifactId-version` pattern from
`pom.xml` (`ReEncryptUtility-1.2.1-SNAPSHOT.jar`); confirm the exact filename
in `target/` after a build, since `pom.xml` version bumps change it.

Or use the published Docker image, per `README.md`:

```shell
docker pull mosipdev/pre-reg-113-115:develop
docker run re-encrypt-utility
```

Building the image locally:

```console
docker build -t name:tag -f Dockerfile .
```

### Configuration (`src/main/resources/application.properties`)

The checked-in file already contains **placeholder dev/QA values**
(`datasource.primary.password=kamesh`, `secretKey=abc123`,
`object.store.s3.secretkey=minioadmin`) — do not replace them with real
credentials in a commit. Before a real run, set (per `README.md`):

- `isNewDatabase=true` for the two-environment scenario, `false` for the
  same-environment scenario.
- `datasource.primary.*` (source DB) and, for the two-environment scenario,
  `datasource.secondary.*` (destination DB).
- `object.store.s3.*` (source MinIO/S3) and, for the two-environment
  scenario, `destinationObjectStore.s3.*` (destination).
- `mosip.base.url`, `decryptBaseUrl`, `encryptBaseUrl`,
  `decryptAppId`/`decryptReferenceId`, `encryptAppId`/`encryptReferenceId`.

### Prerequisites (per `README.md`)

1. Properties from the upstream
   [`pre-reg-113-115-application-default.properties`](https://github.com/mosip/mosip-config/blob/develop1-v3/pre-reg-113-115-application-default.properties).
2. A running kernel Config Server.
3. A running Key Manager service.
4. `mosip_prereg` database and MinIO reachable in both source and
   destination environments.

## Archival script (`db_scripts/ArchivalScript.sql`, repo root)

Moves `prereg.applicant_demographic`, `prereg.applicant_document`, and
`prereg.reg_appointment` rows whose `cr_dtimes`/`upd_dtimes` fall in a
`startDate`/`endDate` window into the corresponding `*_consumed` /
`CONSUMED` tables, **then deletes the moved rows from the source tables**.
To run it:

1. Edit the `startDate`/`endDate` literals inside the script (they are
   hardcoded `DECLARE` values at the top of the `DO $$ ... END $$;` block).
2. Execute it directly against the target Postgres database:

   ```shell
   PGHOST="your-host"
   PGUSER="your-username"
   PGDATABASE="your-database"
   psql -h "$PGHOST" -U "$PGUSER" -d "$PGDATABASE" -f ArchivalScript.sql
   ```

This script is destructive and irreversible outside of a database backup —
see the Do-not rules below.

## CI

`.github/workflows/push-trigger.yml` builds this module via
`SERVICE_LOCATION: pre-reg-113-115` (job `build-maven-pre-reg-113-115`), then
builds and pushes a Docker image as `pre-reg-113-115` (job
`build-dockers-pre-reg-113-115`). Both run on pushes to `master`, `1.*`,
`develop*`, `MOSIP*`, `release*`, and on pull requests.

## Agent rules

### Do

1. Use `./mvnw` (the wrapper committed in this directory) rather than
   assuming a system Maven install.
2. Keep `application.properties` placeholder-only; document required keys
   in `README.md`/this file instead of hardcoding real endpoints.
3. When editing `ReEncrypt.java` or the routing datasource classes, keep the
   primary/secondary datasource split intact — `isNewDatabase` controls
   whether the secondary datasource is used at all.

### Do not

1. Do not run `../db_scripts/ArchivalScript.sql` against any database
   without first confirming `startDate`/`endDate` and having a verified
   backup — it permanently deletes rows from `prereg.applicant_demographic`,
   `prereg.applicant_document`, and `prereg.reg_appointment` after copying
   them to `*_consumed`/`CONSUMED` tables.
2. Do not run this utility's re-encryption flow against a production
   `mosip_prereg` database without dry-running it against a non-production
   copy first — a decrypt/re-encrypt pass touches every migrated record.
3. Do not commit real credentials into `application.properties`.
4. Do not assume a `src/test` directory exists — there isn't one.
