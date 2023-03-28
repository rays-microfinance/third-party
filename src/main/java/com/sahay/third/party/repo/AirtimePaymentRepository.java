package com.sahay.third.party.repo;

import com.sahay.third.party.model.AirtimePayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AirtimePaymentRepository extends JpaRepository<AirtimePayment, Integer> {

    Optional<AirtimePayment> findAirtimePaymentByBillerReference(String ref);
}
