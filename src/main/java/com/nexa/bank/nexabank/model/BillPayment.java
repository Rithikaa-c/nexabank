package com.nexa.bank.nexabank.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bill_payments")
public class BillPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountNumber;      // payer account
    private String billType;           // ELECTRICITY, WATER, GAS, BROADBAND, DTH, INSURANCE...
    private String consumerNumber;     // customer/consumer no. at provider
    private String providerName;       // optional (eg. "TNEB", "Jio")
    private BigDecimal amount;
    private LocalDateTime createdAt;

    @PrePersist
    public void pre() { this.createdAt = LocalDateTime.now(); }

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getBillType() { return billType; }
    public void setBillType(String billType) { this.billType = billType; }

    public String getConsumerNumber() { return consumerNumber; }
    public void setConsumerNumber(String consumerNumber) { this.consumerNumber = consumerNumber; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
