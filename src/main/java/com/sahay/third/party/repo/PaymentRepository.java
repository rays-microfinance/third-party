package com.sahay.third.party.repo;

import com.sahay.third.party.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Optional<Payment> findPaymentByBillerReference(String ref);

    Optional<Payment> findPaymentByBillerReferenceOrTransRef(String billRef, String ref);

    Optional<Payment> findPaymentByBillerReferenceAndTransRef(String billRef, String ref);

    List<Payment> findPaymentsByPhoneNumber(String phone);


    Optional<Payment> findPaymentByTransRefOrAccountNumberOrPhoneNumber(String ref, String account, String phone);
}
