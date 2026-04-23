package com.assignment.savings.account.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSavingsAccountRequest(
        @NotBlank(message = "customerId is required")
        @Size(min = 9, max = 9, message = "customerId must be exactly 9 characters")
        String customerId,

        @NotBlank(message = "customerName is required")
        @Size(max = 100, message = "customerName must be at most 100 characters")
        String customerName,

        @Size(min = 5, max = 30, message = "accountNickName must be between 5 and 30 characters")
        String accountNickName,

        @NotBlank(message = "accountType is required")
        @Size(min = 2, max = 2, message = "accountType must be exactly 2 characters")
        String accountType,

        @NotBlank(message = "channelCode is required")
        @Size(min = 2, max = 2, message = "channelCode must be exactly 2 characters")
        String channelCode,

        @NotBlank(message = "branchCode is required")
        @Size(min = 4, max = 4, message = "branchCode must be exactly 4 characters")
        String branchCode,

        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
        String currency,

        @NotBlank(message = "transactionReference is required")
        @Size(max = 50, message = "transactionReference must be at most 50 characters")
        String transactionReference
) {
}
