package com.assignment.savings.account.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "savings_accounts")
public class SavingsAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(nullable = false, length = 9)
    private String customerId;

    @Column(nullable = false, length = 100)
    private String customerName;

    @Column(length = 30)
    private String accountNickName;

    @Column(nullable = false, length = 2)
    private String accountType;

    @Column(nullable = false, length = 2)
    private String channelCode;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 50)
    private String transactionReference;

    @Column(nullable = false)
    private OffsetDateTime submittedAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false, length = 20)
    private String status;

    protected SavingsAccount() {
    }

    public SavingsAccount(
            String accountNumber,
            String customerId,
            String customerName,
            String accountNickName,
            String  accountType,
            String channelCode,
            String currency,
            String transactionReference,
            OffsetDateTime submittedAt,
            OffsetDateTime createdAt,
            String status
    ) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.customerName = customerName;
        this.accountNickName = accountNickName;
        this.accountType = accountType;
        this.channelCode = channelCode;
        this.currency = currency;
        this.transactionReference = transactionReference;
        this.submittedAt = submittedAt;
        this.createdAt = createdAt;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getAccountNickName() {
        return accountNickName;
    }

    public String  getAccountType() {
        return accountType;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public String getCurrency() {
        return currency;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public String getStatus() {
        return status;
    }
}
