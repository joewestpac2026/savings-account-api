package com.assignment.savings.common.exception;

public record ErrorResponse(
        String responseCode,
        String responseDescription
) {
}
