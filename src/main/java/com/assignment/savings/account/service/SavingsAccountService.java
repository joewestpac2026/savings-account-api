package com.assignment.savings.account.service;

import com.assignment.savings.account.api.CreateSavingsAccountRequest;
import com.assignment.savings.account.api.SavingsAccountResponse;
import com.assignment.savings.account.api.CreateSavingsAccountResponse;
import com.assignment.savings.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import com.assignment.savings.common.context.ReqResPayloadBuilder;
import java.time.OffsetDateTime;
import com.assignment.savings.account.domain.SavingsAccount;
import com.assignment.savings.account.domain.SavingsAccountRepository;
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
    private final AccountCreationValidator accountCreationValidator;
    private final AccountNumberGenerator accountNumberGenerator;
    private static final String ACCOUNTS_CACHE = "accounts";
    private final AccountCreationAuditRepository accountCreationAuditRepository;
    private static final Logger ACCOUNT_AUDIT_LOGGER = LoggerFactory.getLogger("ACCOUNT_AUDIT");



    public SavingsAccountService(
            SavingsAccountRepository savingsAccountRepository,
            AccountCreationValidator accountCreationValidator,
            AccountNumberGenerator accountNumberGenerator,
            AccountCreationAuditRepository accountCreationAuditRepository
    ) {
        this.savingsAccountRepository = savingsAccountRepository;
        this.accountCreationValidator = accountCreationValidator;
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

    // Keep the service focused on orchestration by delegating account creation
    // business rule checks before persisting the new account.
    private CreateSavingsAccountResponse createNewAccount(CreateSavingsAccountRequest request) {
        AccountCreationValidator.ValidatedAccountCreation validated =
                accountCreationValidator.validate(request);

        OffsetDateTime now = OffsetDateTime.now();

        SavingsAccount account = new SavingsAccount(
                accountNumberGenerator.generateForCustomer(
                        validated.customerId(),
                        validated.branchCode()
                ),
                validated.customerId(),
                validated.customerName(),
                validated.accountNickName(),
                validated.accountType(),
                validated.channelCode(),
                validated.currency(),
                validated.transactionReference(),
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

    private String extractBranchCode(String accountNumber) {
        String[] parts = accountNumber.split("-");
        if (parts.length != 4) {
            throw new IllegalStateException("Invalid account number format: " + accountNumber);
        }
        return parts[1];
    }
}