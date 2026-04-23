package com.assignment.savings.account.api;

import java.time.OffsetDateTime;

public record CreateSavingsAccountResponse(
        String responseCode,
        String responseDescription,
        Long id,
        String customerId,
        String accountNumber,
        String accountType,
        String customerName,
        String accountNickName,
        String channelCode,
        String branchCode,
        String currency,
        String transactionReference,
        OffsetDateTime createdAt,
        String status
) {
}
