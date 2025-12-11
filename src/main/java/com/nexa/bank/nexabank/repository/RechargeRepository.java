package com.nexa.bank.nexabank.repository;

import com.nexa.bank.nexabank.model.Recharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RechargeRepository extends JpaRepository<Recharge, Long> {
    List<Recharge> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
}
