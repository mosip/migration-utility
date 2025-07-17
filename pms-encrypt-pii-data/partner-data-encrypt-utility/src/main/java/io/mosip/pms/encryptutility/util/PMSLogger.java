package io.mosip.pms.encryptutility.util;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.logger.logback.factory.Logfactory;

public class PMSLogger {
    /**
     * Instantiates a new pms logger.
     */
    private PMSLogger() {
    }

    /**
     * Method to get the rolling file logger for the class provided.
     *
     * @param clazz
     *            the clazz
     * @return the logger
     */
    public static Logger getLogger(Class<?> clazz) {
        return Logfactory.getSlf4jLogger(clazz);
    }
} 