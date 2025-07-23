package io.mosip.pms.encrypt.piidata.constants;

import lombok.Getter;

@Getter
public enum ErrorCode {
    ENCRYPTION_FAILED("PMS-EU-001", "Failed to encrypt data using Key Manager."),
    API_NULL_RESPONSE("PMS-EU-002", "API returned a null response.");

    private final String errorCode;
    private final String errorMessage;

    ErrorCode(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

}
