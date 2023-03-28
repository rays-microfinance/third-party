package com.sahay.third.party.repo;

import com.sahay.third.party.model.FundsTransferPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FundsTransferPaymentRepository extends JpaRepository<FundsTransferPayment, Integer> {
    Optional<FundsTransferPayment> findFundsTransferPaymentByBillerReference(String ref);
}
