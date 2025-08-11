# Partner PII Data Encrypt Utility
### ⚠️ **Important Warning**

Before running the encryption utility, ensure you take a **backup** of the following database tables:
- `partner`
- `partner_h`
- `partner_contact`

This is critical to allow recovery in case of any failure or inconsistency during the encryption process.

---

Additionally, for partners that are registered in the **PMS database** but **not present in Keycloak**, you **must** create the corresponding partner accounts in **Keycloak** **before** running the utility.

> This step is **mandatory** to ensure consistency between systems, because once the data is encrypted, it will no longer be possible to refer to the original (unencrypted) partner details required for creating user accounts in Keycloak.

> **Failure to add the Keycloak user will prevent the partner from logging into the PMS Revamp portal.**

## Overview
This module provides a command-line utility to encrypt Personally Identifiable Information (PII) fields in the Partner, PartnerH, and PartnerContact tables of the MOSIP PMS database. It is intended for use during data migration or security upgrades to ensure all sensitive partner data is encrypted at rest.

## Prerequisites
- JDK 21.0.3
- Maven 3.9.6
- Access to the MOSIP PMS database (PostgreSQL)
- Access to MOSIP KeyManager service

## Developer guide 

### Configuration
Edit the [`application.properties`](src/main/resources/application.properties) file to set up:
- Database connection (`javax.persistence.jdbc.*`)
- KeyManager URL (`mosip.kernel.keymanager.url`)
- Keycloak and Auth Adapter settings (if required)

#### KeyManager and API URLs
```properties
mosip.kernel.keymanager.url=https://dev.mosip.net
mosip.api.internal.url=https://dev.mosip.net
keycloak.external.url=https://dev.mosip.net
mosip.pms.client.secret=REPLACE_WITH_PMS_CLIENT_SECRET
```

#### Database Configuration
```properties
javax.persistence.jdbc.driver=org.postgresql.Driver
javax.persistence.jdbc.url=jdbc:postgresql://dev.mosip.net/mosip_pms
javax.persistence.jdbc.user=postgres
javax.persistence.jdbc.password=REPLACE_WITH_DB_PASSWORD
javax.persistence.jdbc.schema=pms
```

Note: Replace dev URLs and credentials with your environment values.

### Build & Run

#### Build and install the project:

```bash
cd pms-encrypt-pii-data
mvn clean install -DskipTests=true -Dmaven.javadoc.skip=true -Dgpg.skip=true
```

#### Run the utility:

##### Using the packaged JAR:
```bash
java -jar target/partner-pii-data-encrypt-utility-1.0.0-SNAPSHOT.jar
```

##### Or via Spring Boot Maven plugin:
```bash
mvn spring-boot:run
```

## Logging
Logs are output to the console. Adjust logging levels in `application.properties` as needed.

#### Verify Logs and Output
Expected log sequence on successful run:

- Application startup:
  > Started PartnerPiiDataEncryptUtilityApplication in XX seconds

- Encryption process:
  > Initiating encryption of Partner Pii data...
  > PiiDataEncryptionService: encryptPiiData - START

  > Starting PII encryption for partner records.
    If records found:
    > Successfully encrypted PII data for Partner IDs: [P001, P002, ...]
    > Total partners with successfully encrypted PII data: X
    If no records found:
    > No partner records found requiring PII encryption

  > Starting PII encryption for partner history records.
    > [Similar log structure as above for PartnerH records]

  > Starting PII encryption for partner contact records.
    > [Similar log structure as above for PartnerContact records]

  > PiiDataEncryptionService: Encryption completed - Partner records: X, PartnerH records: Y, PartnerContact records: Z
  > Partner PII data encryption process completed successfully.
  > PiiDataEncryptionService: encryptPiiData - END

This confirms the utility ran correctly and exited.

#### Database verification
- Verify encryption status by checking the following tables:
  - `partner`
  - `partner_h`
  - `partner_contact`

- For each table:
  - Fields like `email`, `contact`, and `address` should now contain encrypted values.
  - The `emailIdHash` field should be populated with a SHA-256 hash of the original email ID.

- Records with missing PII fields should be skipped (see logs).

## Notes
- The utility will automatically shut down after processing.
- For custom encryption logic, extend the service and utility classes as needed.

## License
This project is licensed under the terms of [Mozilla Public License 2.0](../../LICENSE)
