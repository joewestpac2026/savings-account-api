package com.assignment.savings.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final String responseCode;

    public ResourceNotFoundException(
            String responseCode,
            String message
    ) {
        super(message);
        this.responseCode = responseCode;
    }

    public String getResponseCode() {
        return responseCode;
    }
}
