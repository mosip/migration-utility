package io.mosip.pms.encrypt.piidata;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.pms.encrypt.piidata.service.EncryptPIIDataService;
import io.mosip.pms.encrypt.piidata.util.PMSLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({ "${mosip.auth.adapter.impl.basepackage}","io.mosip.pms.common.*", "io.mosip.pms.encrypt.piidata.*",
        "io.mosip.kernel.templatemanager.velocity.builder"})
public class EncryptPIIDataUtilityApplication implements CommandLineRunner {

    private static final Logger LOGGER = PMSLogger.getLogger(EncryptPIIDataUtilityApplication.class);

    private static final int SUCCESS_EXIT_CODE = 0;
    private static final int ERROR_EXIT_CODE = 1;

    @Autowired
    private EncryptPIIDataService encryptPIIDataService;

    @Autowired
    private ApplicationContext applicationContext;

    public static void main(String[] args) {
        LOGGER.info("Starting Encrypt PII Data Utility Application...");
        SpringApplication.run(EncryptPIIDataUtilityApplication.class, args);
    }

    @Override
    public void run(String... args) {
        int exitCode = SUCCESS_EXIT_CODE;

        try {
            LOGGER.info("Initiating encryption of Partner PII data...");
            encryptPIIDataService.encryptData();
            LOGGER.info("Partner PII data encryption process completed successfully.");

        } catch (Exception e) {
            LOGGER.error("Error during partner PII data encryption: {}", e.getMessage(), e);
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
