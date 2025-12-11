package com.nexa.bank.nexabank.service;

import com.nexa.bank.nexabank.model.BillPayment;
import com.nexa.bank.nexabank.model.Transaction;
import com.nexa.bank.nexabank.model.Account;
import com.nexa.bank.nexabank.repository.BillPaymentRepository;
import com.nexa.bank.nexabank.repository.TransactionRepository;
import com.nexa.bank.nexabank.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

@Service
public class BillPaymentService {

    private final AccountRepository accountRepo;
    private final BillPaymentRepository billRepo;
    private final TransactionRepository txRepo;
    private final PdfService pdfService;
    private final EmailService emailService;
    public BillPayment getPaymentById(Long id) {
        return billRepo.findById(id).orElse(null);

    }

    public BillPaymentService(AccountRepository accountRepo,
                              BillPaymentRepository billRepo,
                              TransactionRepository txRepo,
                              PdfService pdfService,
                              EmailService emailService) {
        this.accountRepo = accountRepo;
        this.billRepo = billRepo;
        this.txRepo = txRepo;
        this.pdfService = pdfService;
        this.emailService = emailService;
    }

    @Transactional
    public BillPayment payBill(String accountNumber,
                               String billType,
                               String consumerNumber,
                               String providerName,
                               BigDecimal amount) throws Exception {

        Account acc = accountRepo.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // daily limit reset handled by AccountService in controllers usually
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
            throw new RuntimeException("Your card is frozen. Cannot make payments.");
        }

        // Deduct
        BigDecimal newBalance = acc.getBalance().subtract(amount);
        acc.setBalance(newBalance);

        // update daily used
        acc.setDailyUsed(acc.getDailyUsed().add(amount));
        accountRepo.save(acc);

        // Save bill record
        BillPayment bill = new BillPayment();
        bill.setAccountNumber(accountNumber);
        bill.setBillType(billType);
        bill.setConsumerNumber(consumerNumber);
        bill.setProviderName(providerName);
        bill.setAmount(amount);
        BillPayment saved = billRepo.save(bill);

        // Create transaction record (type BILL_PAYMENT)
        Transaction tx = new Transaction();
        tx.setTransactionId("TXN-" + java.util.UUID.randomUUID().toString().substring(0,8).toUpperCase());
        tx.setType("BILL_PAYMENT");
        tx.setAccountNumber(accountNumber);
        tx.setAmount(amount);
        tx.setReason(billType + " bill payment to " + providerName);
        tx.setBalanceAfter(newBalance);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setTransactionTime(LocalDateTime.now());
        txRepo.save(tx);

        // Generate receipt PDF
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a"));

        byte[] pdf = pdfService.generateTransferReceipt(
                tx.getTransactionId(),
                acc.getHolderName(),
                accountNumber,
                providerName,
                "BILL_PROVIDER",
                amount,
                newBalance,
                date,
                time,
                "Bill payment - " + consumerNumber
        );

        // Send email with attachment
        String html = "<h2>Bill Payment Successful</h2>" +
                "<p>Your " + billType + " bill has been paid successfully.</p>" +
                "<p><b>Amount:</b> ₹" + amount + "</p>" +
                "<p><b>Provider:</b> " + providerName + "</p>" +
                "<p><b>Consumer No:</b> " + consumerNumber + "</p>";

        emailService.sendWithAttachment(
                acc.getEmail(),
                "Nexa Bank - Bill Payment Receipt",
                html,
                pdf,
                "BillPayment_" + tx.getTransactionId() + ".pdf"
        );

        return saved;
    }
}
