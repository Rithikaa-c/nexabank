package com.nexa.bank.nexabank.service;

import com.nexa.bank.nexabank.model.Account;
import com.nexa.bank.nexabank.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.Period;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class AccountService {
    @Autowired
    private AccountRepository accountRepository;

    private final AccountRepository repo;

    public AccountService(AccountRepository repo) {
        this.repo = repo;
    }

    // -------------------------------------------------------------------------
    // Check if user is 18+
    // -------------------------------------------------------------------------
    public boolean isAdult(LocalDate dob) {
        return Period.between(dob, LocalDate.now()).getYears() >= 18;
    }

    // -------------------------------------------------------------------------
    // Securely hash PIN using SHA-256
    // -------------------------------------------------------------------------
    public String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pin.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing PIN", e);
        }
    }
   public  void resetLimitIfNewDay(Account acc) {
        if (acc.getLastLimitResetDate() == null ||
                !acc.getLastLimitResetDate().equals(LocalDate.now())) {

            acc.setDailyUsed(BigDecimal.ZERO);
            acc.setLastLimitResetDate(LocalDate.now());
            accountRepository.save(acc);
        }
    }


    // -------------------------------------------------------------------------
    // Generate Account Number
    // -------------------------------------------------------------------------
    public String generateAccountNumber(String type, long count) {
        String prefix = switch (type) {
            case "Savings" -> "SBIN";
            case "Current" -> "CURR";
            default -> "SAL";
        };
        return prefix + String.format("%07d", count + 1001);
    }

    // -------------------------------------------------------------------------
    // IFSC Mapping
    // -------------------------------------------------------------------------
    private final Map<String, String> branchIfscMap = Map.of(
            "Chennai - Tambaram", "SBIN004561",
            "Bangalore - Whitefield", "SBIN006792",
            "Mumbai - Andheri", "SBIN009343",
            "Delhi - Rohini", "SBIN002464"
    );

    // -------------------------------------------------------------------------
    // Hash Aadhar Number (SHA-256)
    // -------------------------------------------------------------------------
    public String hashAadhar(String aadhar) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(aadhar.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing Aadhar", e);
        }
    }

    // -------------------------------------------------------------------------
    // FINAL — Save Account after OTP verification
    // (FULLY MERGED VERSION - CORRECT)
    // -------------------------------------------------------------------------
    public Account createAccount(Account acc) {

        long count = repo.count();

        // 1. Generate Account Number
        acc.setAccountNumber(
                generateAccountNumber(acc.getAccountType(), count)
        );

        // 2. Hash PIN
        acc.setPin(
                hashPin(acc.getPin())
        );

        // 3. Hash Aadhar
        String hashedAadhar = hashAadhar(acc.getAadharNumber());
        acc.setAadharHash(hashedAadhar);
        acc.setAadharNumber(null); // remove plain text

        // 4. Account setup
        acc.setAccountStatus("ACTIVE");
        acc.setBalance(BigDecimal.ZERO);
        acc.setCreatedAt(LocalDate.now());
        acc.setIfscCode(branchIfscMap.get(acc.getBranchName()));

        // 5. Save in DB
        return repo.save(acc);
    }

}
