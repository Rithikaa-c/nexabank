package com.nexa.bank.nexabank.repository;

import com.nexa.bank.nexabank.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByEmail(String email);

    Optional<Admin> findByAdminId(String adminId);
}
