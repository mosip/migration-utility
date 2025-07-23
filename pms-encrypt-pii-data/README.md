# Partner PII Data Encrypt Utility

## Overview
This module provides a command-line utility to encrypt Personally Identifiable Information (PII) fields in the Partner, PartnerH, and PartnerContact tables of the MOSIP PMS database. It is intended for use during data migration or security upgrades to ensure all sensitive partner data is encrypted at rest.

## Prerequisites
- JDK 21.0.3
- Maven 3.9.6
- Access to the MOSIP PMS database (PostgreSQL)
- Access to MOSIP KeyManager service

## Build & Run (for developers)
1. Build and install:
    ```
    $ mvn install -DskipTests=true -Dmaven.javadoc.skip=true -Dgpg.skip=true
    ```
2. Build Docker image:
    ```
    $ docker build -f Dockerfile .
    ```
3. Run the utility:
    ```
    $ java -jar target/partner-pii-data-encrypt-utility-1.0.0-SNAPSHOT.jar
    ```

### For Spring Boot 3.x
- Specify the ANT Path Matcher for using existing ANT path patterns:
  ```
  spring.mvc.pathmatch.matching-strategy=ANT_PATH_MATCHER
  ```
- To unmask values in actuator env URL:
  ```
  management.endpoint.env.show-values=ALWAYS
  ```

## Configuration
Edit the [`application.properties`](src/main/resources/application.properties) file to set up:
- Database connection (`javax.persistence.jdbc.*`)
- KeyManager URL (`mosip.kernel.keymanager.url`)
- Keycloak and Auth Adapter settings (if required)

Example (partial):
```properties
javax.persistence.jdbc.url=jdbc:postgresql://<host>/<db>
javax.persistence.jdbc.user=postgres
javax.persistence.jdbc.password=<password>
mosip.kernel.keymanager.url=https://<keymanager-url>
```

### Add auth-adapter in the classpath to run the service
```
<dependency>
    <groupId>io.mosip.kernel</groupId>
    <artifactId>kernel-auth-adapter</artifactId>
    <version>${kernel.auth.adapter.version}</version>
</dependency>
```

## Logging
Logs are output to the console. Adjust logging levels in `application.properties` as needed.

## Notes
- The utility will automatically shut down after processing.
- For custom encryption logic, extend the service and utility classes as needed.

## License
This project is licensed under the terms of [Mozilla Public License 2.0](../../LICENSE)
