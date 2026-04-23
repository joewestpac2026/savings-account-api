package com.assignment.savings.common.exception;

import com.assignment.savings.account.domain.AccountCreationAuditRepository;
import com.assignment.savings.common.context.ReqResPayloadBuilder;
import com.assignment.savings.common.context.RequestContextHelper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Keeps API error responses consistent across validation, business rule,
// lookup, and database failure scenarios
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger ACCOUNT_AUDIT_LOGGER = LoggerFactory.getLogger("ACCOUNT_AUDIT");

    private final AccountCreationAuditRepository accountCreationAuditRepository;

    public GlobalExceptionHandler(AccountCreationAuditRepository accountCreationAuditRepository) {
        this.accountCreationAuditRepository = accountCreationAuditRepository;
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<AccountCreationErrorResponse> handleBusinessRuleViolation(BusinessRuleViolationException ex) {
        AccountCreationErrorResponse errorResponse = new AccountCreationErrorResponse(
                ex.getResponseCode(),
                ex.getMessage(),
                ex.getTransactionReference()
        );

        updateAudit(
                ex.getTransactionReference(),
                errorResponse,
                HttpStatus.BAD_REQUEST.value()
        );

        ACCOUNT_AUDIT_LOGGER.warn(
                "event=ACCOUNT_CREATION_BUSINESS_REJECTED transactionReference={} responseCode={} responseDescription={}",
                errorResponse.transactionReference(),
                errorResponse.responseCode(),
                errorResponse.responseDescription()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getResponseCode(),
                ex.getMessage()
        );

        ACCOUNT_AUDIT_LOGGER.warn(
                "event=ACCOUNT_LOOKUP_NOT_FOUND responseCode={} responseDescription={}",
                errorResponse.responseCode(),
                errorResponse.responseDescription()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        if ("id".equals(ex.getName())) {
            ErrorResponse errorResponse = new ErrorResponse(
                    "1009",
                    "id must be a numeric value"
            );

            ACCOUNT_AUDIT_LOGGER.warn(
                    "event=ACCOUNT_LOOKUP_INVALID_ID responseCode={} responseDescription={}",
                    errorResponse.responseCode(),
                    errorResponse.responseDescription()
            );

            return ResponseEntity.badRequest().body(errorResponse);
        }

        ErrorResponse errorResponse = new ErrorResponse(
                "1000",
                "Invalid request parameter"
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AccountCreationErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");

        AccountCreationErrorResponse errorResponse = new AccountCreationErrorResponse(
                "1000",
                message,
                RequestContextHelper.extractTransactionReference(ex, request)
        );

        updateAudit(
                errorResponse.transactionReference(),
                errorResponse,
                HttpStatus.BAD_REQUEST.value()
        );

        ACCOUNT_AUDIT_LOGGER.warn(
                "event=ACCOUNT_CREATION_VALIDATION_FAILED transactionReference={} responseCode={} responseDescription={}",
                errorResponse.transactionReference(),
                errorResponse.responseCode(),
                errorResponse.responseDescription()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    public ResponseEntity<AccountCreationErrorResponse> handleCannotGetJdbcConnection(
            CannotGetJdbcConnectionException ex,
            HttpServletRequest request
    ) {
        AccountCreationErrorResponse errorResponse = new AccountCreationErrorResponse(
                "3001",
                "Database connection is unavailable",
                RequestContextHelper.extractTransactionReference(request)
        );

        updateAudit(
                errorResponse.transactionReference(),
                errorResponse,
                HttpStatus.SERVICE_UNAVAILABLE.value()
        );

        ACCOUNT_AUDIT_LOGGER.error(
                "event=DATABASE_CONNECTION_UNAVAILABLE transactionReference={} responseCode={} responseDescription={}",
                errorResponse.transactionReference(),
                errorResponse.responseCode(),
                errorResponse.responseDescription()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<AccountCreationErrorResponse> handleDataAccessException(
            DataAccessException ex,
            HttpServletRequest request
    ) {
        AccountCreationErrorResponse errorResponse = new AccountCreationErrorResponse(
                "3002",
                "Database is temporarily unavailable",
                RequestContextHelper.extractTransactionReference(request)
        );

        updateAudit(
                errorResponse.transactionReference(),
                errorResponse,
                HttpStatus.SERVICE_UNAVAILABLE.value()
        );

        ACCOUNT_AUDIT_LOGGER.error(
                "event=DATABASE_TEMPORARILY_UNAVAILABLE transactionReference={} responseCode={} responseDescription={}",
                errorResponse.transactionReference(),
                errorResponse.responseCode(),
                errorResponse.responseDescription()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    // Updates the latest audit row for account creation flows so failed requests
    // still persist their final response details
    private void updateAudit(
            String transactionReference,
            AccountCreationErrorResponse errorResponse,
            int httpStatus
    ) {
        if (transactionReference == null || transactionReference.isBlank()) {
            return;
        }

        accountCreationAuditRepository
                .findTopByTransactionReferenceOrderByIdDesc(transactionReference)
                .ifPresent(audit -> {
                    audit.markResponse(
                            ReqResPayloadBuilder.buildErrorResponsePayload(errorResponse),
                            OffsetDateTime.now(),
                            httpStatus,
                            errorResponse.responseCode(),
                            errorResponse.responseDescription()
                    );
                    accountCreationAuditRepository.save(audit);
                });
    }
}
