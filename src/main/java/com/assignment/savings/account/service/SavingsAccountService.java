package com.assignment.savings.account.service;

import com.assignment.savings.account.api.CreateSavingsAccountRequest;
import com.assignment.savings.account.api.SavingsAccountResponse;
import com.assignment.savings.account.api.CreateSavingsAccountResponse;
import com.assignment.savings.common.exception.BusinessRuleViolationException;
import com.assignment.savings.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import com.assignment.savings.common.context.ReqResPayloadBuilder;
import java.time.OffsetDateTime;
import com.assignment.savings.account.domain.SavingsAccount;
import com.assignment.savings.account.domain.SavingsAccountRepository;
import java.util.Locale;
import com.assignment.savings.account.domain.OffensiveNickname;
import com.assignment.savings.account.domain.OffensiveNicknameRepository;
import java.util.List;
import org.springframework.util.StringUtils;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import com.assignment.savings.account.domain.AccountCreationAudit;
import com.assignment.savings.account.domain.AccountCreationAuditRepository;
import com.assignment.savings.common.logging.MaskingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class SavingsAccountService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final OffensiveNicknameRepository offensiveNicknameRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private static final String ACCOUNTS_CACHE = "accounts";
    private final AccountCreationAuditRepository accountCreationAuditRepository;
    private static final Logger ACCOUNT_AUDIT_LOGGER = LoggerFactory.getLogger("ACCOUNT_AUDIT");

    public SavingsAccountService(
            SavingsAccountRepository savingsAccountRepository,
            OffensiveNicknameRepository offensiveNicknameRepository,
            AccountNumberGenerator accountNumberGenerator,
            AccountCreationAuditRepository accountCreationAuditRepository
    ) {
        this.savingsAccountRepository = savingsAccountRepository;
        this.offensiveNicknameRepository = offensiveNicknameRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.accountCreationAuditRepository = accountCreationAuditRepository;
    }

    @CachePut(value = ACCOUNTS_CACHE, key = "#result.id")
    public CreateSavingsAccountResponse createAccount(CreateSavingsAccountRequest request) {
        String normalizedTransactionReference = request.transactionReference().trim();
        String normalizedChannelCode = request.channelCode().trim();

        // Persist the inbound payload before business processing so failed requests
        // can still be traced in the audit table.
        AccountCreationAudit audit = new AccountCreationAudit(
                normalizedTransactionReference,
                normalizedChannelCode,
                ReqResPayloadBuilder.buildRequestPayload(request),
                OffsetDateTime.now()
        );
        accountCreationAuditRepository.save(audit);

        ACCOUNT_AUDIT_LOGGER.info(
                "event=ACCOUNT_CREATION_REQUEST_RECEIVED transactionReference={} channelCode={} customerId={} customerName={} accountType={} branchCode={}",
                normalizedTransactionReference,
                normalizedChannelCode,
                MaskingHelper.maskCustomerId(request.customerId()),
                MaskingHelper.maskCustomerName(request.customerName()),
                request.accountType(),
                request.branchCode()
        );

        // Reuse an existing account when the same transaction reference is submitted
        // again for the same customer, so account creation stays idempotent.
        CreateSavingsAccountResponse response = savingsAccountRepository
                .findByTransactionReferenceAndCustomerId(
                        normalizedTransactionReference,
                        request.customerId().trim()
                )
                .map(this::toCreateResponse)
                .orElseGet(() -> createNewAccount(request));

        audit.markResponse(
                ReqResPayloadBuilder.buildSuccessResponsePayload(response),
                OffsetDateTime.now(),
                201,
                response.responseCode(),
                response.responseDescription()
        );
        accountCreationAuditRepository.save(audit);

        ACCOUNT_AUDIT_LOGGER.info(
                "event=ACCOUNT_CREATION_RESPONSE_SENT transactionReference={} responseCode={} status={} customerId={} customerName={} accountNumber={}",
                response.transactionReference(),
                response.responseCode(),
                response.status(),
                MaskingHelper.maskCustomerId(response.customerId()),
                MaskingHelper.maskCustomerName(response.customerName()),
                MaskingHelper.maskAccountNumber(response.accountNumber())
        );

        return response;
    }

    // Lookup responses are cached by internal account id, assuming account retrieval
    // is likely to become more read-heavy than account creation.
    @Cacheable(value = ACCOUNTS_CACHE, key = "#id")
    public SavingsAccountResponse getAccountById(Long id) {
        SavingsAccount account = savingsAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        null,
                        "2001",
                        "Savings account not found for id: " + id
                ));

        return toResponse(account);
    }

    // These validations are based on additional research into how bank account
    // opening flows are typically constrained in real systems.
    private CreateSavingsAccountResponse createNewAccount(CreateSavingsAccountRequest request) {

        String normalizedCustomerId = request.customerId().trim();
        String normalizedTransactionReference = request.transactionReference().trim();

        long existingAccountCount = savingsAccountRepository
                .countByCustomerId(normalizedCustomerId);

        if (existingAccountCount >= 5) {
            throw new BusinessRuleViolationException(
                    normalizedTransactionReference,
                    "1001",
                    "A customer cannot create more than 5 accounts"
            );
        }

        // Normalise and validate request fields before generating the account
        String normalizedBranchCode = request.branchCode().trim();
        validateBranchCode(normalizedBranchCode, normalizedTransactionReference);

        String normalizedChannelCode = request.channelCode().trim();
        validateChannelCode(normalizedChannelCode, normalizedTransactionReference);

        String normalizedAccountType = request.accountType().trim();
        validateAccountType(normalizedAccountType, normalizedTransactionReference);

        String normalizedNickName = normalizeNickName(request.accountNickName());
        if (containsOffensiveNickname(normalizedNickName)) {
            throw new BusinessRuleViolationException(
                    normalizedTransactionReference,
                    "1002",
                    "accountNickName contains offensive language"
            );
        }

        OffsetDateTime now = OffsetDateTime.now();

        SavingsAccount account = new SavingsAccount(
                accountNumberGenerator.generateForCustomer(
                        normalizedCustomerId,
                        normalizedBranchCode
                ),
                normalizedCustomerId,
                request.customerName().trim(),
                normalizedNickName,
                normalizedAccountType,
                normalizedChannelCode,
                request.currency().trim().toUpperCase(Locale.ROOT),
                normalizedTransactionReference,
                now,
                now,
                "ACTIVE"
        );

        SavingsAccount savedAccount = savingsAccountRepository.save(account);
        return toCreateResponse(savedAccount);
    }

    private SavingsAccountResponse toResponse(SavingsAccount account) {
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

    private CreateSavingsAccountResponse toCreateResponse(SavingsAccount account) {
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

    private String extractBranchCode(String accountNumber) {
        String[] parts = accountNumber.split("-");
        if (parts.length != 4) {
            throw new IllegalStateException("Invalid account number format: " + accountNumber);
        }
        return parts[1];
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

    private boolean containsOffensiveNickname(String accountNickName) {
        if (!StringUtils.hasText(accountNickName)) {
            return false;
        }

        String normalized = accountNickName.toLowerCase(Locale.ROOT);
        List<OffensiveNickname> offensiveNicknames = offensiveNicknameRepository.findAll();

        return offensiveNicknames.stream()
                .map(OffensiveNickname::getValue)
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::contains);
    }

}