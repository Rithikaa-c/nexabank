package com.nexa.bank.nexabank.model;
import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;

    private String accountNumber;
    private String holderName;
    private String pin;
    private BigDecimal balance;
    private String accountStatus;
    private String accountType;
    private String email;
    private String phoneNumber;
    private String branchName;
    private String ifscCode;
    private LocalDate createdAt;
      // stores linked guardian account number
    private boolean minor; // true = minor account, false = major account
    private String guardianAccountNumber; // Parent/guardian account number
    private java.time.LocalDateTime guardianVerifiedAt; // when guardian verified OTP
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }

    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }
    private LocalDate dob; // For age checking
    public boolean isMinor() {
        return minor;
    }

    public void setMinor(boolean minor) {
        this.minor = minor;
    }

    public String getGuardianAccountNumber() {
        return guardianAccountNumber;
    }

    public void setGuardianAccountNumber(String guardianAccountNumber) {
        this.guardianAccountNumber = guardianAccountNumber;
    }

    public LocalDateTime getGuardianVerifiedAt() {
        return guardianVerifiedAt;
    }

    public void setGuardianVerifiedAt(LocalDateTime guardianVerifiedAt) {
        this.guardianVerifiedAt = guardianVerifiedAt;
    }

    @Column(nullable = false)
    private String gender;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(nullable = false)
    private String aadharHash; // Hashed Aadhar stored, NOT plain text

    // Temporary field for receiving aadhar input (not stored in DB)
    @Transient
    private String aadharNumber;

    private boolean cardFrozen = false;

    public boolean isCardFrozen() {
        return cardFrozen;
    }
    private BigDecimal dailyLimit = BigDecimal.ZERO;
    private BigDecimal dailyUsed = BigDecimal.ZERO;
    private LocalDate lastLimitResetDate;

    public void setCardFrozen(boolean cardFrozen) {
        this.cardFrozen = cardFrozen;
    }
    private Integer creditScore = 720;  // default good score

    public Integer getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(Integer creditScore) {
        this.creditScore = creditScore;
    }
    @Column(name = "frozen")
    private boolean frozen;

    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }

}
