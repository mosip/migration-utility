# AGENTS.md

[Back to repo root](../AGENTS.md)

## Purpose

Migrates and publishes Partner Management Service (PMS) data — approved
API keys, MISP licenses, partner records, policies — from PMS to IDA over
websub, as part of the MOSIP 1.1.5.5 → 1.2.0.1-B1 upgrade. It can run once as
a migration, or be scheduled as a cronjob (via Kubernetes) to keep publishing
data changes at intervals.

## Layout

```text
pms-115-120/
├── README.md                                             # operator-facing setup/run doc
├── Steps_to_deploy_pms-migration-utility_in_v2_deployment.pdf
├── db_scripts/
│   ├── README.md
│   └── migration-scripts.sql       # creates pms.last_sync_time_stamp for cronjob mode
└── utility/                        # <-- the actual Maven project lives here, not at pms-115-120/
    ├── pom.xml                     # artifactId "Utility", groupId io.mosip.pms
    ├── Dockerfile
    ├── .gitignore
    └── src/main/
        ├── java/io/mosip/pms/ida/
        │   ├── Application.java            # Spring Boot entry point (CommandLineRunner)
        │   ├── constant/                   # EventType, PmsConstant
        │   ├── dao/                        # JPA entities + repositories (Partner, AuthPolicy, MISPLicenseEntity, ...)
        │   ├── dto/                        # request/response/event DTOs
        │   ├── service/PMSDataMigrationService.java  # core migration logic, invoked from Application.run()
        │   ├── util/                       # CertUtil, MapperUtils, RestUtil, UtilityLogger
        │   └── websub/WebSubPublisher.java # publishes migrated events to the websub hub
        └── resources/
            ├── bootstrap.properties        # spring.cloud.config.* wiring, server.port=8081
            └── application-dev.properties  # dev-profile DB/auth/websub settings (placeholder creds)
```

There is no `src/test` directory in this module.

## How to run

Build and run locally, from inside `utility/`:

```shell
cd utility
mvn install -DskipTests=true -Dmaven.javadoc.skip=true -Dgpg.skip=true
java -jar target/Utility-0.0.1-SNAPSHOT.jar
```

Build a Docker image:

```console
docker build -t name:tag -f Dockerfile .
```

The Dockerfile's `CMD` (see `utility/Dockerfile`) shows the actual runtime
invocation pattern used in deployment — note system properties (`-D...`)
come before `-jar`:

```shell
java -jar -Dloader.path="$loader_path_env" \
  -Dspring.cloud.config.label="$spring_config_label_env" \
  -Dspring.cloud.config.name="$spring_config_name_env" \
  -Dspring.profiles.active="$active_profile_env" \
  -Dspring.cloud.config.uri="$spring_config_url_env" \
  Utility-0.0.1-SNAPSHOT.jar
```

(That example is copied verbatim from the shipped Dockerfile, including the
unconventional `-D` flags after `-jar` — Spring Boot's own launcher still
accepts them there because it re-parses `-D` args itself, but do not copy
that pattern into new, non-Spring-Boot Java commands: for a plain `java`
invocation, `-D` flags must precede `-jar` to be recognized by the JVM.)

Properties needed by this utility are documented upstream in
[`pms-migration-utility-default.properties`](https://github.com/mosip/mosip-config/blob/develop-v3/pms-migration-utility-default.properties).
Local defaults (dev profile) live in
`utility/src/main/resources/application-dev.properties` and already contain
placeholder dev credentials (`secretKey=abc123`,
`javax.persistence.jdbc.password=mosip123`) — do not replace these with real
credentials in a commit.

### Before running, per `README.md`

1. Add the roles listed in the repo `README.md` to Keycloak for the
   `mosip-partner-client` (PUBLISH_*) and `mosip-ida-client` (SUBSCRIBE_*)
   clients.
2. Run the seed SQL in `README.md` against the PMS database (creates a
   default policy group/partner/policy so the migration has something to
   publish).
3. For cronjob mode, set
   `mosip.pms.utility.run.mode=cronjob` and run
   `db_scripts/migration-scripts.sql` against `mosip_pms` first — it creates
   the `pms.last_sync_time_stamp` table the cronjob uses to track its last
   run.

## CI

`.github/workflows/push-trigger.yml` builds this module via
`SERVICE_LOCATION: pms-115-120/utility` (job
`build-maven-migration-utility`), then builds and pushes a Docker image as
`pms-migration-utility` (job `build-dockers`). Both run on pushes to
`master`, `1.*`, `develop*`, `MOSIP*`, `release*`, and on pull requests.

## Agent rules

### Do

1. Run all Maven/Docker commands from inside `utility/`, not from
   `pms-115-120/` — that's where `pom.xml` and `Dockerfile` actually live.
2. Keep `-D` system properties before `-jar` in any new plain `java`
   command you write (the shipped Dockerfile's ordering is Spring
   Boot-specific and should not be imitated outside a Spring Boot fat jar).
3. If you change what `db_scripts/migration-scripts.sql` creates, update
   `db_scripts/README.md` in the same change — they describe the same
   script.
4. Treat `application-dev.properties` values as placeholders only.

### Do not

1. Do not run `db_scripts/migration-scripts.sql` against a production
   `mosip_pms`/`mosip_authdevice`/`mosip_regdevice` database without a
   verified backup — it creates a new table and (per its own header) is part
   of a live-database schema change during an upgrade window.
2. Do not remove the `@EnableAutoConfiguration(exclude = {...})` line in
   `Application.java` without understanding why datasource/JPA
   autoconfiguration is disabled — this module wires persistence manually.
3. Do not add real secrets/passwords to `application-dev.properties` or any
   other file under `src/main/resources/`.
4. Do not assume a `src/test` directory exists — there isn't one; don't
   report tests as added/passing unless you created and ran them yourself.
