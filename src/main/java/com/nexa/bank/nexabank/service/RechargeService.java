package com.nexa.bank.nexabank.service;

import com.nexa.bank.nexabank.model.Recharge;
import com.nexa.bank.nexabank.model.Transaction;
import com.nexa.bank.nexabank.model.Account;
import com.nexa.bank.nexabank.repository.RechargeRepository;
import com.nexa.bank.nexabank.repository.TransactionRepository;
import com.nexa.bank.nexabank.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class RechargeService {

    private final AccountRepository accountRepo;
    private final RechargeRepository rechargeRepo;
    private final TransactionRepository txRepo;
    private final PdfService pdfService;
    private final EmailService emailService;

    public RechargeService(AccountRepository accountRepo,
                           RechargeRepository rechargeRepo,
                           TransactionRepository txRepo,
                           PdfService pdfService,
                           EmailService emailService) {
        this.accountRepo = accountRepo;
        this.rechargeRepo = rechargeRepo;
        this.txRepo = txRepo;
        this.pdfService = pdfService;
        this.emailService = emailService;
    }

    @Transactional
    public Recharge doRecharge(String accountNumber,
                               String mobileNumber,
                               String operator,
                               String rechargeType,
                               BigDecimal amount) throws Exception {

        Account acc = accountRepo.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // daily limit check
        if (acc.getDailyLimit() != null && acc.getDailyLimit().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal remaining = acc.getDailyLimit().subtract(acc.getDailyUsed());
            if (amount.compareTo(remaining) > 0) {
                throw new RuntimeException("Daily limit exceeded. Remaining limit: ₹" + remaining);
            }
        }

        if (acc.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance!");
        }

        if (acc.isCardFrozen()) {
            throw new RuntimeException("Your card is frozen. Cannot do recharge.");
        }

        // Deduct & save account
        BigDecimal newBalance = acc.getBalance().subtract(amount);
        acc.setBalance(newBalance);
        acc.setDailyUsed(acc.getDailyUsed().add(amount));
        accountRepo.save(acc);

        // Save recharge record
        Recharge r = new Recharge();
        r.setAccountNumber(accountNumber);
        r.setMobileNumber(mobileNumber);
        r.setOperator(operator);
        r.setRechargeType(rechargeType);
        r.setAmount(amount);
        Recharge saved = rechargeRepo.save(r);

        // TX entry
        Transaction tx = new Transaction();
        tx.setTransactionId("TXN-" + java.util.UUID.randomUUID().toString().substring(0,8).toUpperCase());
        tx.setType("RECHARGE");
        tx.setAccountNumber(accountNumber);
        tx.setAmount(amount);
        tx.setReason(rechargeType + " recharge to " + mobileNumber + " (" + operator + ")");
        tx.setBalanceAfter(newBalance);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setTransactionTime(LocalDateTime.now());
        txRepo.save(tx);

        // PDF receipt
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a"));

        byte[] pdf = pdfService.generateTransferReceipt(
                tx.getTransactionId(),
                acc.getHolderName(),
                accountNumber,
                operator,
                mobileNumber,
                amount,
                newBalance,
                date,
                time,
                "Recharge"
        );

        String html = "<h2>Recharge Successful</h2>" +
                "<p>Your "+ rechargeType +" recharge has been processed.</p>" +
                "<p><b>Amount:</b> ₹" + amount + "</p>" +
                "<p><b>Number/Provider:</b> " + mobileNumber + " / " + operator + "</p>";

        emailService.sendWithAttachment(
                acc.getEmail(),
                "Nexa Bank - Recharge Receipt",
                html,
                pdf,
                "Recharge_" + tx.getTransactionId() + ".pdf"
        );

        return saved;
    }
}
