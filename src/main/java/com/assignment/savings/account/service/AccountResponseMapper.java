package com.assignment.savings.account.service;

import com.assignment.savings.account.api.CreateSavingsAccountResponse;
import com.assignment.savings.account.api.SavingsAccountResponse;
import com.assignment.savings.account.domain.SavingsAccount;
import org.springframework.stereotype.Component;

@Component
public class AccountResponseMapper {

    // Keeps response construction in one place so the service can focus on
    // orchestration rather than DTO mapping details.
    public SavingsAccountResponse toLookupResponse(SavingsAccount account) {
        return new SavingsAccountResponse(
                "0000",
                "Success",
                account.getId(),
                account.getCustomerId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getCustomerName(),
                account.getAccountNickName(),
                account.getChannelCode(),
                extractBranchCode(account.getAccountNumber()),
                account.getCurrency(),
                account.getCreatedAt(),
                account.getStatus()
        );
    }

    public CreateSavingsAccountResponse toCreateResponse(SavingsAccount account) {
        return new CreateSavingsAccountResponse(
                "0000",
                "Success",
                account.getId(),
                account.getCustomerId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getCustomerName(),
                account.getAccountNickName(),
                account.getChannelCode(),
                extractBranchCode(account.getAccountNumber()),
                account.getCurrency(),
                account.getTransactionReference(),
                account.getCreatedAt(),
                account.getStatus()
        );
    }

    private String extractBranchCode(String accountNumber) {
        String[] parts = accountNumber.split("-");
        if (parts.length != 4) {
            throw new IllegalStateException("Invalid account number format: " + accountNumber);
        }
        return parts[1];
    }
}
