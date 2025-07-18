package io.mosip.pms.pii.encryptutility.exception;

import io.mosip.kernel.core.exception.BaseUncheckedException;

public class PartnerPiiEncryptUtilityException extends BaseUncheckedException {

    private static final long serialVersionUID = 1L;

    public PartnerPiiEncryptUtilityException() {}

    public PartnerPiiEncryptUtilityException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }
}
