package com.assignment.savings.common.context;

import com.assignment.savings.account.api.CreateSavingsAccountRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;

public final class RequestContextHelper {

    private RequestContextHelper() {
    }

    // Reads transactionReference in one place so validation errors, exception
    // handling, and audit updates can all use the same request identifier
    public static String extractTransactionReference(HttpServletRequest request) {
        Object value = request.getAttribute("transactionReference");
        return value != null ? value.toString() : null;
    }

    public static String extractTransactionReference(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Object target = ex.getBindingResult().getTarget();

        if (target instanceof CreateSavingsAccountRequest createRequest
                && createRequest.transactionReference() != null) {
            return createRequest.transactionReference();
        }

        return extractTransactionReference(request);
    }
}
