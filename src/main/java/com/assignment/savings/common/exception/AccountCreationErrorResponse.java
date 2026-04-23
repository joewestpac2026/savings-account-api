package com.assignment.savings.common.exception;

public record AccountCreationErrorResponse(
        String responseCode,
        String responseDescription,
        String transactionReference
) {
}
