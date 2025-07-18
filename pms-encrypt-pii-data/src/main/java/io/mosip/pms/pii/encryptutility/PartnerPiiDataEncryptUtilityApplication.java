package io.mosip.pms.pii.encryptutility;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.pms.pii.encryptutility.service.PiiDataEncryptionService;
import io.mosip.pms.pii.encryptutility.util.PMSLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({ "${mosip.auth.adapter.impl.basepackage}","io.mosip.pms.common.*", "io.mosip.pms.pii.encryptutility.*",
        "io.mosip.kernel.templatemanager.velocity.builder"})
public class PartnerPiiDataEncryptUtilityApplication implements CommandLineRunner {

    private static final Logger LOGGER = PMSLogger.getLogger(PartnerPiiDataEncryptUtilityApplication.class);

    private static final int SUCCESS_EXIT_CODE = 0;
    private static final int ERROR_EXIT_CODE = 1;

    @Autowired
    private PiiDataEncryptionService piiDataEncryptionService;

    @Autowired
    private ApplicationContext applicationContext;

    public static void main(String[] args) {
        LOGGER.info("Starting Partner PII Data Encryption Utility Application...");
        SpringApplication.run(PartnerPiiDataEncryptUtilityApplication.class, args);
    }

    @Override
    public void run(String... args) {
        int exitCode = SUCCESS_EXIT_CODE;

        try {
            LOGGER.info("Initiating encryption of Partner Pii data...");
            piiDataEncryptionService.encryptPiiData();
            LOGGER.info("Partner Pii data encryption process completed successfully.");

        } catch (Exception e) {
            LOGGER.error("Error during partner Pii data encryption: {}", e.getMessage(), e);
            exitCode = ERROR_EXIT_CODE;
        }

        shutdownApplication(exitCode);
    }

    private void shutdownApplication(int exitCode) {
        LOGGER.info("Shutting down application with exit code: {}", exitCode);
        SpringApplication.exit(applicationContext, () -> exitCode);
        System.exit(exitCode);
    }
}
