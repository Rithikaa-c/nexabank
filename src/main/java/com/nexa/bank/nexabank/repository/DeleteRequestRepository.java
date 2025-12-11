package com.nexa.bank.nexabank.repository;

import com.nexa.bank.nexabank.model.DeleteRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeleteRequestRepository extends JpaRepository<DeleteRequest, Long> {

    // Fetch by status (PENDING / APPROVED / REJECTED)
    List<DeleteRequest> findByStatus(String status);

    // Fetch ALL delete requests for this account
    List<DeleteRequest> findByAccountNumber(String accountNumber);
}
