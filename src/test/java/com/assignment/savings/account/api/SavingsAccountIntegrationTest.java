package com.assignment.savings.account.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.assignment.savings.account.domain.AccountCreationAudit;
import com.assignment.savings.account.domain.AccountCreationAuditRepository;
import com.assignment.savings.account.domain.OffensiveNickname;
import com.assignment.savings.account.domain.OffensiveNicknameRepository;
import com.assignment.savings.account.domain.SavingsAccount;
import com.assignment.savings.account.domain.SavingsAccountRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;


// Uses a real PostgreSQL-backed Spring context so the tests cover request
// handling, persistence, and audit behaviour together
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SavingsAccountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SavingsAccountRepository savingsAccountRepository;

    @Autowired
    private AccountCreationAuditRepository accountCreationAuditRepository;

    @Autowired
    private OffensiveNicknameRepository offensiveNicknameRepository;

    @Test
    void createAccount_persistsSavingsAccountAndAuditRecordInPostgres() throws Exception {
        String unique = unique();
        String transactionReference = "TXN-INT-" + unique;
        String customerId = customerIdFrom(unique);

        String requestBody = buildRequestBody(
                customerId,
                "Joe Wu",
                "Westpac engineer",
                "01",
                "02",
                "0278",
                "NZD",
                transactionReference
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.transactionReference").value(transactionReference))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        SavingsAccount savedAccount = savingsAccountRepository
                .findByTransactionReferenceAndCustomerId(transactionReference, customerId)
                .orElseThrow();

        assertTrue(savedAccount.getAccountNumber().startsWith("03-0278-"));
        assertEquals("ACTIVE", savedAccount.getStatus());

        AccountCreationAudit audit = accountCreationAuditRepository
                .findTopByTransactionReferenceOrderByIdDesc(transactionReference)
                .orElseThrow();

        assertEquals("02", audit.getChannelCode());
        assertTrue(audit.getRequestPayload().contains(transactionReference));
        assertTrue(audit.getResponsePayload().contains("\"responseCode\": \"0000\""));
        assertEquals(201, audit.getHttpStatus());
        assertEquals("0000", audit.getResponseCode());
    }

    @Test
    void createAccount_returnsExistingAccountForIdempotentRequest() throws Exception {
        String unique = unique();
        String transactionReference = "TXN-INT-" + unique;
        String customerId = customerIdFrom(unique);

        String requestBody = buildRequestBody(
                customerId,
                "Joe Wu",
                "Holiday Fund",
                "01",
                "02",
                "0278",
                "NZD",
                transactionReference
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.transactionReference").value(transactionReference));

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responseCode").value("0000"))
                .andExpect(jsonPath("$.transactionReference").value(transactionReference));

        long matchingAccounts = savingsAccountRepository.findByCustomerId(customerId).stream()
                .filter(account -> transactionReference.equals(account.getTransactionReference()))
                .count();

        assertEquals(1L, matchingAccounts);
    }


    @Test
    void createAccount_rejectsOffensiveNicknameAndPersistsAuditRecord() throws Exception {
        String unique = unique();
        String transactionReference = "TXN-INT-" + unique;
        String customerId = customerIdFrom(unique);
        String offensiveWord = "bad" + unique.toLowerCase();

        offensiveNicknameRepository.save(new OffensiveNickname(offensiveWord));

        String requestBody = buildRequestBody(
                customerId,
                "Joe Wu",
                offensiveWord + "engineer",
                "01",
                "02",
                "0278",
                "NZD",
                transactionReference
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value("1002"))
                .andExpect(jsonPath("$.responseDescription").value("accountNickName contains offensive language"))
                .andExpect(jsonPath("$.transactionReference").value(transactionReference));

        assertTrue(savingsAccountRepository
                .findByTransactionReferenceAndCustomerId(transactionReference, customerId)
                .isEmpty());

        AccountCreationAudit audit = accountCreationAuditRepository
                .findTopByTransactionReferenceOrderByIdDesc(transactionReference)
                .orElseThrow();

        assertEquals("02", audit.getChannelCode());
        assertTrue(audit.getRequestPayload().contains(transactionReference));
        assertTrue(audit.getResponsePayload().contains("\"responseCode\": \"1002\""));
        assertEquals(400, audit.getHttpStatus());
        assertEquals("1002", audit.getResponseCode());
    }

    @Test
    void createAccount_rejectsWhenCustomerAlreadyHasFiveAccounts() throws Exception {
        String unique = unique();
        String customerId = customerIdFrom(unique);

        // Seed account numbers must stay unique because the shared local test database
        // is reused across runs instead of being cleared each time
        int baseNumber = (int) (System.currentTimeMillis() % 1_000_000);

        for (int i = 1; i <= 5; i++) {
            String existingAccountNumber = "03-0278-%07d-000".formatted(baseNumber + i);
            savingsAccountRepository.save(new SavingsAccount(
                    existingAccountNumber,
                    customerId,
                    "Joe Wu",
                    "Existing Fund " + i,
                    "01",
                    "02",
                    "NZD",
                    "TXN-SEED-" + unique + "-" + i,
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    "ACTIVE"
            ));
        }


        String transactionReference = "TXN-INT-" + unique;
        String requestBody = buildRequestBody(
                customerId,
                "Joe Wu",
                "Sixth Fund",
                "01",
                "02",
                "0278",
                "NZD",
                transactionReference
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value("1001"))
                .andExpect(jsonPath("$.responseDescription").value("A customer cannot create more than 5 accounts"))
                .andExpect(jsonPath("$.transactionReference").value(transactionReference));

        AccountCreationAudit audit = accountCreationAuditRepository
                .findTopByTransactionReferenceOrderByIdDesc(transactionReference)
                .orElseThrow();

        assertEquals(400, audit.getHttpStatus());
        assertEquals("1001", audit.getResponseCode());
    }

    @Test
    void createAccount_returnsValidationErrorForInvalidRequestBody() throws Exception {
        String unique = unique();
        String transactionReference = "TXN-INT-" + unique;
        String customerId = customerIdFrom(unique);

        String requestBody = buildRequestBody(
                customerId,
                "Joe Wu",
                "engineer",
                "1",
                "02",
                "0278",
                "NZD",
                transactionReference
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value("1000"))
                .andExpect(jsonPath("$.responseDescription")
                        .value("accountType: accountType must be exactly 2 characters"))
                .andExpect(jsonPath("$.transactionReference").value(transactionReference));
    }

    @Test
    void createAccount_rejectsInvalidBranchCodeAndPersistsAuditRecord() throws Exception {
        String unique = unique();
        String transactionReference = "TXN-INT-" + unique;
        String customerId = customerIdFrom(unique);

        String requestBody = buildRequestBody(
                customerId,
                "Joe Wu",
                "Holiday Fund",
                "01",
                "02",
                "2000",
                "NZD",
                transactionReference
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value("1004"))
                .andExpect(jsonPath("$.responseDescription")
                        .value("branchCode must be between 0001 and 1999"))
                .andExpect(jsonPath("$.transactionReference").value(transactionReference));

        AccountCreationAudit audit = accountCreationAuditRepository
                .findTopByTransactionReferenceOrderByIdDesc(transactionReference)
                .orElseThrow();

        assertEquals("02", audit.getChannelCode());
        assertNotNull(audit.getResponsePayload());
        assertEquals(400, audit.getHttpStatus());
        assertEquals("1004", audit.getResponseCode());
    }

    private String buildRequestBody(
            String customerId,
            String customerName,
            String accountNickName,
            String accountType,
            String channelCode,
            String branchCode,
            String currency,
            String transactionReference
    ) {
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
                customerId,
                customerName,
                accountNickName,
                accountType,
                channelCode,
                branchCode,
                currency,
                transactionReference
        );
    }

    private String unique() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String customerIdFrom(String unique) {
        return "9" + unique.substring(0, 8);
    }

}
