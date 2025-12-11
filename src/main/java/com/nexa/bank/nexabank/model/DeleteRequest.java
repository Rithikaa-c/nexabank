package com.nexa.bank.nexabank.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "delete_requests")
public class DeleteRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountNumber;

    private String reason;  // Customer reason

    // Admin note / approval reason
    private String note;

    private LocalDateTime requestedAt = LocalDateTime.now();

    // PENDING / APPROVED / REJECTED
    private String status = "PENDING";

    // ==== GETTERS & SETTERS ====

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
