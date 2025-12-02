package com.nexa.bank.nexabank.service;

import com.nexa.bank.nexabank.model.Account;
import com.nexa.bank.nexabank.model.Transaction;
import com.nexa.bank.nexabank.repository.AccountRepository;
import com.nexa.bank.nexabank.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RollbackService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final EmailService emailService;

    @Autowired
    public RollbackService(TransactionRepository transactionRepository,
                           AccountRepository accountRepository,
                           EmailService emailService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.emailService = emailService;
    }

    /**
     * Rollback a transfer transaction.
     */
    @Transactional
    public void rollbackTransfer(String txnId, String adminId) {

        // Load original transaction
        Transaction original = transactionRepository.findByTransactionId(txnId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + txnId));

        // Only allow transfer reversal
        if (!"TRANSFER".equalsIgnoreCase(original.getType())) {
            throw new IllegalArgumentException("Only TRANSFER transactions can be rolled back.");
        }

        // Prevent double rollback
        if (Boolean.TRUE.equals(original.getRolledBack())) {
            throw new IllegalArgumentException("This transaction has already been rolled back.");
        }

        String senderAcc = original.getSenderAccountNumber();
        String receiverAcc = original.getReceiverAccountNumber();

        BigDecimal amount = original.getAmount();

        // Lock accounts in consistent order to avoid deadlocks
        Account firstLock;
        Account secondLock;

        if (senderAcc.compareTo(receiverAcc) < 0) {
            firstLock = accountRepository.findByAccountNumberForUpdate(senderAcc)
                    .orElseThrow(() -> new IllegalArgumentException("Sender not found: " + senderAcc));
            secondLock = accountRepository.findByAccountNumberForUpdate(receiverAcc)
                    .orElseThrow(() -> new IllegalArgumentException("Receiver not found: " + receiverAcc));
        } else {
            firstLock = accountRepository.findByAccountNumberForUpdate(receiverAcc)
                    .orElseThrow(() -> new IllegalArgumentException("Receiver not found: " + receiverAcc));
            secondLock = accountRepository.findByAccountNumberForUpdate(senderAcc)
                    .orElseThrow(() -> new IllegalArgumentException("Sender not found: " + senderAcc));
        }

        // Identify roles after locking
        Account sender = firstLock.getAccountNumber().equals(senderAcc) ? firstLock : secondLock;
        Account receiver = firstLock.getAccountNumber().equals(receiverAcc) ? firstLock : secondLock;

        // Ensure receiver has enough balance
        if (receiver.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Recipient does not have enough balance to rollback.");
        }

        // Perform reverse transfer
        receiver.setBalance(receiver.getBalance().subtract(amount)); // debit
        sender.setBalance(sender.getBalance().add(amount)); // credit

        accountRepository.save(receiver);
        accountRepository.save(sender);

        // Mark original as rolled back
        original.setRolledBack(true);
        original.setReason((original.getReason() == null ? "" : original.getReason() + " | ") +
                "REVERSED_BY_ADMIN:" + adminId + " at " + LocalDateTime.now());
        transactionRepository.save(original);

        // Create a reversal transaction
        Transaction reversal = new Transaction();
        reversal.setTransactionId("RBK-" + UUID.randomUUID().toString().substring(0, 8));
        reversal.setType("ROLLBACK");

        reversal.setSenderAccountNumber(receiverAcc);  // money moved FROM
        reversal.setReceiverAccountNumber(senderAcc);  // money moved TO

        reversal.setAccountNumber(senderAcc);
        reversal.setAmount(amount);
        reversal.setReason("Rollback of " + original.getTransactionId() + " by Admin " + adminId);
        reversal.setCreatedAt(LocalDateTime.now());
        reversal.setTransactionTime(LocalDateTime.now());
        reversal.setBalanceAfter(sender.getBalance());

        transactionRepository.save(reversal);

        // Try sending emails (should NOT fail the rollback)
        try {
            String senderEmail = sender.getEmail();
            String receiverEmail = receiver.getEmail();

            // Sender email (credited)
            emailService.sendEmail(
                    senderEmail,
                    "₹" + amount + " Credited Back - Rollback Completed",
                    "Hello " + sender.getHolderName() + ",\n\n" +
                            "An amount of ₹" + amount + " has been returned to your account.\n" +
                            "Transaction reversed: " + original.getTransactionId() + "\n\n" +
                            "Regards,\nNexaBank"
            );

            // Receiver email (debited)
            emailService.sendEmail(
                    receiverEmail,
                    "₹" + amount + " Debited (Rollback)",
                    "Hello " + receiver.getHolderName() + ",\n\n" +
                            "An amount of ₹" + amount + " has been debited from your account during a rollback.\n" +
                            "Transaction reversed: " + original.getTransactionId() + "\n\n" +
                            "Regards,\nNexaBank"
            );

        } catch (Exception ex) {
            System.err.println("Email sending failed during rollback: " + ex.getMessage());
        }
    }
}
