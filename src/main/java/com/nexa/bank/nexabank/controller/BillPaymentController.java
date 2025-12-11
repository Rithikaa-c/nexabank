package com.nexa.bank.nexabank.controller;

import com.nexa.bank.nexabank.service.BillPaymentService;
import com.nexa.bank.nexabank.service.AccountService;
import com.nexa.bank.nexabank.model.BillPayment;
import com.nexa.bank.nexabank.repository.AccountRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
public class BillPaymentController {

    private final BillPaymentService billPaymentService;
    private final AccountRepository accountRepo;
    private final AccountService accountService;

    public BillPaymentController(BillPaymentService billPaymentService,
                                 AccountRepository accountRepo,
                                 AccountService accountService) {
        this.billPaymentService = billPaymentService;
        this.accountRepo = accountRepo;
        this.accountService = accountService;
    }

    // Load bill payment form
    @GetMapping("/bill-payment")
    public String showBillPayment(@RequestParam String accountNumber, Model model) {
        model.addAttribute("accountNumber", accountNumber);
        return "bill-payment";
    }

    @PostMapping("/bill-payment")
    public String processBillPayment(@RequestParam String accountNumber,
                                     @RequestParam String billType,
                                     @RequestParam String consumerNumber,
                                     @RequestParam(required = false) String providerName,
                                     @RequestParam BigDecimal amount,
                                     RedirectAttributes ra,
                                     Model model) {

        // reset daily used when needed
        var accOpt = accountRepo.findByAccountNumber(accountNumber);
        if (accOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Account not found.");
            return "redirect:/bill-payment?accountNumber=" + accountNumber;
        }
        var acc = accOpt.get();
        accountService.resetLimitIfNewDay(acc);

        try {
            BillPayment saved = billPaymentService.payBill(accountNumber, billType, consumerNumber, providerName == null ? billType : providerName, amount);

            ra.addFlashAttribute("success", "Bill paid successfully!");
            ra.addFlashAttribute("accountNumber", accountNumber);
            ra.addFlashAttribute("amount", amount);
            ra.addFlashAttribute("providerName", providerName);
            ra.addFlashAttribute("consumerNumber", consumerNumber);

            return "redirect:/bill-success?accountNumber=" + accountNumber + "&id=" + saved.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/bill-payment?accountNumber=" + accountNumber;
        }
    }
    @GetMapping("/bill-success")
    public String billSuccess(@RequestParam String accountNumber,
                              @RequestParam Long id,
                              Model model) {

        BillPayment payment = billPaymentService.getPaymentById(id);

        if (payment == null) {
            model.addAttribute("error", "Receipt not found.");
            return "bill-success";
        }

        model.addAttribute("accountNumber", payment.getAccountNumber());
        model.addAttribute("id", payment.getId());
        model.addAttribute("billType", payment.getBillType());
        model.addAttribute("providerName", payment.getProviderName());
        model.addAttribute("consumerNumber", payment.getConsumerNumber());
        model.addAttribute("amount", payment.getAmount());
        model.addAttribute("dateTime", payment.getCreatedAt());   // ✅ FIXED

        model.addAttribute("message", "Bill paid successfully!");

        return "bill-success";
    }

}
