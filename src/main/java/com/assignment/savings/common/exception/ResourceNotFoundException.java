package com.assignment.savings.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final String transactionReference;
    private final String responseCode;

    public ResourceNotFoundException(String transactionReference, String responseCode, String message) {
        super(message);
        this.transactionReference = transactionReference;
        this.responseCode = responseCode;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public String getResponseCode() {
        return responseCode;
    }
}
