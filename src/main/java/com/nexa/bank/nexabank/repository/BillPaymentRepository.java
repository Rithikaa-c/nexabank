package com.nexa.bank.nexabank.repository;

import com.nexa.bank.nexabank.model.BillPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillPaymentRepository extends JpaRepository<BillPayment, Long> {
    List<BillPayment> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
}
