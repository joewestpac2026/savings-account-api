package com.assignment.savings.account.api;

import java.time.OffsetDateTime;

public record SavingsAccountResponse(
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
        OffsetDateTime createdAt,
        String status
) {
}
