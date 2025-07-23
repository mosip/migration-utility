package io.mosip.pms.pii.encrypt.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;

public class PartnerDataEncryptException extends BaseUncheckedException {

    private static final long serialVersionUID = 1L;

    public PartnerDataEncryptException() {}

    public PartnerDataEncryptException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }
}
