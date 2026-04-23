package com.assignment.savings.account.service;

import com.assignment.savings.account.api.CreateSavingsAccountRequest;
import com.assignment.savings.account.domain.SavingsAccountRepository;
import com.assignment.savings.common.exception.BusinessRuleViolationException;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AccountCreationValidator {

    private final SavingsAccountRepository savingsAccountRepository;
    private final OffensiveNicknameChecker offensiveNicknameChecker;

    public AccountCreationValidator(
            SavingsAccountRepository savingsAccountRepository,
            OffensiveNicknameChecker offensiveNicknameChecker
    ) {
        this.savingsAccountRepository = savingsAccountRepository;
        this.offensiveNicknameChecker = offensiveNicknameChecker;
    }

    // Groups create-account business rule checks so the main service can focus
    // on request orchestration, persistence, and response handling.
    public ValidatedAccountCreation validate(CreateSavingsAccountRequest request) {
        String normalizedCustomerId = request.customerId().trim();
        String normalizedTransactionReference = request.transactionReference().trim();

        long existingAccountCount = savingsAccountRepository.countByCustomerId(normalizedCustomerId);
        if (existingAccountCount >= 5) {
            throw new BusinessRuleViolationException(
                    normalizedTransactionReference,
                    "1001",
                    "A customer cannot create more than 5 accounts"
            );
        }

        String normalizedBranchCode = request.branchCode().trim();
        validateBranchCode(normalizedBranchCode, normalizedTransactionReference);

        String normalizedChannelCode = request.channelCode().trim();
        validateChannelCode(normalizedChannelCode, normalizedTransactionReference);

        String normalizedAccountType = request.accountType().trim();
        validateAccountType(normalizedAccountType, normalizedTransactionReference);

        String normalizedNickName = normalizeNickName(request.accountNickName());
        if (offensiveNicknameChecker.containsOffensiveNickname(normalizedNickName)) {
            throw new BusinessRuleViolationException(
                    normalizedTransactionReference,
                    "1002",
                    "accountNickName contains offensive language"
            );
        }

        return new ValidatedAccountCreation(
                normalizedCustomerId,
                normalizedTransactionReference,
                normalizedBranchCode,
                normalizedChannelCode,
                normalizedAccountType,
                normalizedNickName,
                request.customerName().trim(),
                request.currency().trim().toUpperCase(Locale.ROOT)
        );
    }

    private void validateBranchCode(String branchCode, String transactionReference) {
        if (!branchCode.matches("\\d{4}")) {
            throw new BusinessRuleViolationException(
                    transactionReference,
                    "1003",
                    "branchCode must be a 4-digit number"
            );
        }

        int value = Integer.parseInt(branchCode);
        if (value < 1 || value > 1999) {
            throw new BusinessRuleViolationException(
                    transactionReference,
                    "1004",
                    "branchCode must be between 0001 and 1999"
            );
        }
    }

    private void validateChannelCode(String channelCode, String transactionReference) {
        if (!channelCode.equals("01") && !channelCode.equals("02") && !channelCode.equals("03")) {
            throw new BusinessRuleViolationException(
                    transactionReference,
                    "1006",
                    "channelCode must be one of 01, 02 or 03"
            );
        }
    }

    private void validateAccountType(String accountType, String transactionReference) {
        if (!accountType.equals("01") && !accountType.equals("02")
                && !accountType.equals("03") && !accountType.equals("04")) {
            throw new BusinessRuleViolationException(
                    transactionReference,
                    "1008",
                    "accountType must be one of 01, 02, 03 or 04"
            );
        }
    }

    private String normalizeNickName(String accountNickName) {
        return StringUtils.hasText(accountNickName) ? accountNickName.trim() : null;
    }

    public record ValidatedAccountCreation(
            String customerId,
            String transactionReference,
            String branchCode,
            String channelCode,
            String accountType,
            String accountNickName,
            String customerName,
            String currency
    ) {
    }
}
