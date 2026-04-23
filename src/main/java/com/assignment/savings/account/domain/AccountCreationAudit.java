package com.assignment.savings.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "account_creation_audits")
public class AccountCreationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String transactionReference;

    @Column(length = 2)
    private String channelCode;

    @Column(nullable = false, columnDefinition = "text")
    private String requestPayload;

    @Column(nullable = false)
    private OffsetDateTime requestReceivedAt;

    @Column(columnDefinition = "text")
    private String responsePayload;

    private OffsetDateTime responseSentAt;

    private Integer httpStatus;

    @Column(length = 10)
    private String responseCode;

    @Column(length = 255)
    private String responseDescription;

    protected AccountCreationAudit() {
    }

    public AccountCreationAudit(
            String transactionReference,
            String channelCode,
            String requestPayload,
            OffsetDateTime requestReceivedAt
    ) {
        this.transactionReference = transactionReference;
        this.channelCode = channelCode;
        this.requestPayload = requestPayload;
        this.requestReceivedAt = requestReceivedAt;
    }

    public Long getId() {
        return id;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public OffsetDateTime getRequestReceivedAt() {
        return requestReceivedAt;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public OffsetDateTime getResponseSentAt() {
        return responseSentAt;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public String getResponseDescription() {
        return responseDescription;
    }

    public void markResponse(
            String responsePayload,
            OffsetDateTime responseSentAt,
            Integer httpStatus,
            String responseCode,
            String responseDescription
    ) {
        this.responsePayload = responsePayload;
        this.responseSentAt = responseSentAt;
        this.httpStatus = httpStatus;
        this.responseCode = responseCode;
        this.responseDescription = responseDescription;
    }
}
