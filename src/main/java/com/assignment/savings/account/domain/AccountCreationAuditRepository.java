package com.assignment.savings.account.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountCreationAuditRepository extends JpaRepository<AccountCreationAudit, Long> {

    Optional<AccountCreationAudit> findTopByTransactionReferenceOrderByIdDesc(String transactionReference);
}
