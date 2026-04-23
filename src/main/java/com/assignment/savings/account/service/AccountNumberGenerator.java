package com.assignment.savings.account.service;

import com.assignment.savings.account.domain.SavingsAccount;
import com.assignment.savings.account.domain.SavingsAccountRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AccountNumberGenerator {

    // Westpac bank code used by this implementation.
    private static final String BANK_CODE = "03";

    private final SavingsAccountRepository savingsAccountRepository;

    public AccountNumberGenerator(SavingsAccountRepository savingsAccountRepository) {
        this.savingsAccountRepository = savingsAccountRepository;
    }

    // Accounts for the same customer and branch share the same base account number,
    // while the suffix increments as additional accounts are opened under that base
    public String generateForCustomer(String customerId, String branchCode) {
        String branchPrefix = BANK_CODE + "-" + branchCode + "-";

        List<SavingsAccount> customerBranchAccounts =
                savingsAccountRepository.findByCustomerIdAndAccountNumberStartingWith(customerId, branchPrefix);

        String customerBaseNumber = customerBranchAccounts.isEmpty()
                ? getNextCustomerBaseNumber(branchCode)
                : extractCustomerBaseNumber(customerBranchAccounts.get(0).getAccountNumber());

        String suffix = String.format("%03d", customerBranchAccounts.size());

        return BANK_CODE + "-" + branchCode + "-" + customerBaseNumber + "-" + suffix;
    }

    // When a customer has no existing accounts in the branch, allocate the next
    // available 7-digit base number within that branch
    private String getNextCustomerBaseNumber(String branchCode) {
        String branchPrefix = BANK_CODE + "-" + branchCode + "-";

        List<SavingsAccount> branchAccounts = savingsAccountRepository.findByAccountNumberStartingWith(branchPrefix);

        int nextNumber = branchAccounts.stream()
                .map(SavingsAccount::getAccountNumber)
                .map(this::extractCustomerBaseNumber)
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0) + 1;

        return String.format("%07d", nextNumber);
    }

    private String extractCustomerBaseNumber(String accountNumber) {
        String[] parts = accountNumber.split("-");
        if (parts.length != 4) {
            throw new IllegalStateException("Invalid account number format: " + accountNumber);
        }
        return parts[2];
    }
}
