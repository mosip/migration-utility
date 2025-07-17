package io.mosip.pms.encryptutility.constants;

import lombok.Getter;

@Getter
public enum ErrorCode {
    ENCRYPTION_FAILED("PMS-EU-001", "Failed to encrypt data using Key Manager."),
    API_NULL_RESPONSE("PMS-EU-002", "API returned a null response."),
    API_NOT_ACCESSIBLE_EXCEPTION("PMS-EU-003", "Error while accessing the API.");

    private final String errorCode;
    private final String errorMessage;

    ErrorCode(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

}
