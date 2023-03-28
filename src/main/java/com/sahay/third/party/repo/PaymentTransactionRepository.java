package com.sahay.third.party.repo;


import com.sahay.third.party.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Integer> {

    Optional<PaymentTransaction> findPaymentTransactionByReference(String transRef);
    Optional<PaymentTransaction> findPaymentTransactionByReferenceOrAgencyTransactionNumber(String transRef, String stageRef);
    Optional<PaymentTransaction> findPaymentTransactionByReferenceAndAgencyTransactionNumber(String transRef, String stageRef);
}
