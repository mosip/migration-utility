package io.mosip.pms.encrypt.piidata.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;

public class EncryptPIIDataException extends BaseUncheckedException {

    private static final long serialVersionUID = 1L;

    public EncryptPIIDataException() {}

    public EncryptPIIDataException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }
}
