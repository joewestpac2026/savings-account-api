package com.assignment.savings.common.logging;

public final class MaskingHelper {

    private MaskingHelper() {
    }

    // The audit table keeps full request/response payloads for controlled review,
    // but operational logs are masked because log files usually have broader access
    public static String maskCustomerName(String customerName) {
        if (customerName == null || customerName.isBlank()) {
            return "";
        }

        return customerName.substring(0, 1) + "***";
    }

    public static String maskCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return "";
        }

        if (customerId.length() <= 3) {
            return "***";
        }

        return "*".repeat(customerId.length() - 3) + customerId.substring(customerId.length() - 3);
    }

    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return "";
        }

        if (accountNumber.length() <= 4) {
            return "***";
        }

        return "***" + accountNumber.substring(accountNumber.length() - 4);
    }
}
