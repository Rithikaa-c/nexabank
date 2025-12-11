package com.nexa.bank.nexabank.controller;

import com.nexa.bank.nexabank.model.Transaction;
import com.nexa.bank.nexabank.model.Account;
import com.nexa.bank.nexabank.service.AccountService;
import com.nexa.bank.nexabank.service.TransactionService;
import com.nexa.bank.nexabank.service.EmailService;
import com.nexa.bank.nexabank.service.PdfService;
import com.nexa.bank.nexabank.repository.AccountRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class TransferController {
    private final AccountService accountService;

    private final TransactionService transactionService;
    private final AccountRepository accountRepo;
    private final EmailService emailService;
    private final PdfService pdfService;

    public TransferController(TransactionService transactionService,
                              AccountRepository accountRepo,
                              EmailService emailService,
                              PdfService pdfService,
                              AccountService accountService) {

        this.transactionService = transactionService;
        this.accountRepo = accountRepo;
        this.emailService = emailService;
        this.pdfService = pdfService;
        this.accountService = accountService; // ⭐ FIXED
    }


    // LOAD PAGE
    @GetMapping("/transfer")
    public String showTransferPage(@RequestParam String accountNumber, Model model) {
        model.addAttribute("accountNumber", accountNumber);
        return "transfer";
    }

    @PostMapping("/transfer")
    public String processTransfer(@RequestParam String accountNumber,
                                  @RequestParam String fromAccount,
                                  @RequestParam String toAccount,
                                  @RequestParam BigDecimal amount,
                                  @RequestParam(required = false) String remarks,
                                  @RequestParam String recipientName,
                                  @RequestParam String pin,
                                  Model model) {

        try {
            // ⭐ Fetch sender account
            Account sender = accountRepo.findByAccountNumber(fromAccount)
                    .orElseThrow(() -> new RuntimeException("Sender account not found"));

            // ⭐ PIN CHECK — same as deposit
            String hashedPin = accountService.hashPin(pin);

            if (!hashedPin.equals(sender.getPin())) {
                model.addAttribute("error", "Incorrect PIN");
                model.addAttribute("accountNumber", accountNumber);
                return "transfer";
            }


            // Reset usage if date changed
            if (sender.getLastLimitResetDate() == null ||
                    !sender.getLastLimitResetDate().equals(LocalDate.now())) {

                sender.setDailyUsed(BigDecimal.ZERO);
                sender.setLastLimitResetDate(LocalDate.now());
                accountRepo.save(sender);
            }

            // Check active limit
            if (sender.getDailyLimit() != null && sender.getDailyLimit().compareTo(BigDecimal.ZERO) > 0) {

                BigDecimal remainingLimit = sender.getDailyLimit().subtract(sender.getDailyUsed());

                if (amount.compareTo(remainingLimit) > 0) {
                    model.addAttribute("error",
                            "Transfer failed! Daily limit exceeded. Remaining limit: ₹" + remainingLimit);
                    model.addAttribute("accountNumber", accountNumber);
                    return "transfer";
                }
            }
            // ---------------------- END LIMIT VALIDATION ----------------------


            // Perform transfer
            Transaction tx = transactionService.transfer(
                    fromAccount,
                    toAccount,
                    amount,
                    remarks
            );

            // After success add used amount
            sender.setDailyUsed(sender.getDailyUsed().add(amount));
            accountRepo.save(sender);

            Account receiver = accountRepo.findByAccountNumber(toAccount).orElseThrow();

            // Timestamp
            LocalDateTime now = LocalDateTime.now();
            String date = now.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            String time = now.format(DateTimeFormatter.ofPattern("hh:mm:ss a"));

            // ------------------------------------------
            // GENERATE PDF RECEIPT
            // ------------------------------------------
            byte[] pdf = pdfService.generateTransferReceipt(
                    tx.getTransactionId(),
                    sender.getHolderName(),
                    fromAccount,
                    receiver.getHolderName(),
                    toAccount,
                    amount,
                    tx.getBalanceAfter(),
                    date,
                    time,
                    remarks
            );

            // Email sender
            String senderMessage =
                    "<h2 style='color:#0D1B3D;'>Transfer Successful</h2>" +
                            "<p><b>Sent To:</b> " + receiver.getHolderName() + "</p>" +
                            "<p><b>Amount:</b> ₹" + amount + "</p>" +
                            "<p><b>Updated Balance:</b> ₹" + tx.getBalanceAfter() + "</p>";

            emailService.sendWithAttachment(
                    sender.getEmail(),
                    "Nexa Bank - Transfer Receipt",
                    senderMessage,
                    pdf,
                    "Transfer_Receipt.pdf"
            );

            // Email receiver (no attachment)
            String receiverMsg =
                    "<h2 style='color:#0D1B3D;'>Credit Alert</h2>" +
                            "<p><b>From:</b> " + sender.getHolderName() + "</p>" +
                            "<p><b>Amount Received:</b> ₹" + amount + "</p>";

            emailService.sendWithAttachment(
                    receiver.getEmail(),
                    "Nexa Bank - Amount Received",
                    receiverMsg,
                    null,
                    null
            );

            // Display success on UI
            model.addAttribute("success", "Transfer Successful!");
            model.addAttribute("transactionId", tx.getTransactionId());
            model.addAttribute("date", date);
            model.addAttribute("time", time);
            model.addAttribute("fromAccount", fromAccount);
            model.addAttribute("toAccount", toAccount);
            model.addAttribute("recipientName", receiver.getHolderName());
            model.addAttribute("amount", amount);
            model.addAttribute("updatedBalance", tx.getBalanceAfter());
            model.addAttribute("accountNumber", accountNumber);

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("accountNumber", accountNumber);
        }

        return "transfer";
    }

}
