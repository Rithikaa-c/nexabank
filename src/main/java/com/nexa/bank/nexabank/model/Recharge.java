package com.nexa.bank.nexabank.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recharges")
public class Recharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountNumber;   // payer account
    private String mobileNumber;
    private String operator;        // Operator or DTH provider (eg. Airtel, Jio, TataSky)
    private String rechargeType;    // PREPAID / POSTPAID / DTH
    private BigDecimal amount;
    private LocalDateTime createdAt;

    @PrePersist
    public void pre() { this.createdAt = LocalDateTime.now(); }

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getRechargeType() { return rechargeType; }
    public void setRechargeType(String rechargeType) { this.rechargeType = rechargeType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
