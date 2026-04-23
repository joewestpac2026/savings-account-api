package com.assignment.savings.account.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, Long> {

    long countByCustomerId(String customerId);

    Optional<SavingsAccount> findByTransactionReferenceAndCustomerId(
            String transactionReference,
            String customerId
    );

    List<SavingsAccount> findByCustomerId(String customerId);

    List<SavingsAccount> findByCustomerIdAndAccountNumberStartingWith(String customerId, String prefix);

    List<SavingsAccount> findByAccountNumberStartingWith(String prefix);
}
