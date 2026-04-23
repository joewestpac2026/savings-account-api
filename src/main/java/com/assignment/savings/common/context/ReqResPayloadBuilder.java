package com.assignment.savings.common.context;

import com.assignment.savings.account.api.CreateSavingsAccountRequest;
import com.assignment.savings.account.api.CreateSavingsAccountResponse;
import com.assignment.savings.common.exception.AccountCreationErrorResponse;
import com.assignment.savings.common.exception.ErrorResponse;

// Centralises the persisted request and response payload formats so audit
// records stay consistent across success and failure flows
public final class ReqResPayloadBuilder {

    private ReqResPayloadBuilder() {
    }

    public static String buildRequestPayload(CreateSavingsAccountRequest request) {
        return """
                {
                  "customerId": "%s",
                  "customerName": "%s",
                  "accountNickName": "%s",
                  "accountType": "%s",
                  "channelCode": "%s",
                  "branchCode": "%s",
                  "currency": "%s",
                  "transactionReference": "%s"
                }
                """.formatted(
                safe(request.customerId()),
                safe(request.customerName()),
                safe(request.accountNickName()),
                safe(request.accountType()),
                safe(request.channelCode()),
                safe(request.branchCode()),
                safe(request.currency()),
                safe(request.transactionReference())
        ).trim();
    }

    public static String buildSuccessResponsePayload(CreateSavingsAccountResponse response) {
        return """
                {
                  "responseCode": "%s",
                  "responseDescription": "%s",
                  "id": %s,
                  "customerId": "%s",
                  "accountNumber": "%s",
                  "accountType": "%s",
                  "customerName": "%s",
                  "accountNickName": "%s",
                  "channelCode": "%s",
                  "branchCode": "%s",
                  "currency": "%s",
                  "transactionReference": "%s",
                  "createdAt": "%s",
                  "status": "%s"
                }
                """.formatted(
                safe(response.responseCode()),
                safe(response.responseDescription()),
                response.id(),
                safe(response.customerId()),
                safe(response.accountNumber()),
                safe(response.accountType()),
                safe(response.customerName()),
                safe(response.accountNickName()),
                safe(response.channelCode()),
                safe(response.branchCode()),
                safe(response.currency()),
                safe(response.transactionReference()),
                String.valueOf(response.createdAt()),
                safe(response.status())
        ).trim();
    }

    public static String buildErrorResponsePayload(AccountCreationErrorResponse errorResponse) {
        return """
                {
                  "responseCode": "%s",
                  "responseDescription": "%s",
                  "transactionReference": "%s"
                }
                """.formatted(
                safe(errorResponse.responseCode()),
                safe(errorResponse.responseDescription()),
                safe(errorResponse.transactionReference())
        ).trim();
    }

    public static String buildErrorResponsePayload(ErrorResponse errorResponse) {
        return """
                {
                  "responseCode": "%s",
                  "responseDescription": "%s"
                }
                """.formatted(
                safe(errorResponse.responseCode()),
                safe(errorResponse.responseDescription())
        ).trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }
}
