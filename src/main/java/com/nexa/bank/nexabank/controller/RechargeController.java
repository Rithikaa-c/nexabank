package com.nexa.bank.nexabank.controller;

import com.nexa.bank.nexabank.service.RechargeService;
import com.nexa.bank.nexabank.service.AccountService;
import com.nexa.bank.nexabank.model.Recharge;
import com.nexa.bank.nexabank.repository.AccountRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
public class RechargeController {

    private final RechargeService rechargeService;
    private final AccountRepository accountRepo;
    private final AccountService accountService;

    public RechargeController(RechargeService rechargeService,
                              AccountRepository accountRepo,
                              AccountService accountService) {
        this.rechargeService = rechargeService;
        this.accountRepo = accountRepo;
        this.accountService = accountService;
    }

    @GetMapping("/recharge")
    public String showRecharge(@RequestParam String accountNumber, Model model) {
        model.addAttribute("accountNumber", accountNumber);
        return "recharge";
    }

    @PostMapping("/recharge")
    public String doRecharge(@RequestParam String accountNumber,
                             @RequestParam String mobileNumber,
                             @RequestParam String operator,
                             @RequestParam String rechargeType,
                             @RequestParam BigDecimal amount,
                             RedirectAttributes ra) {

        // 1. Validate Account
        var accOpt = accountRepo.findByAccountNumber(accountNumber);
        if (accOpt.isEmpty()) {
            ra.addFlashAttribute("error", "❌ Account not found!");
            return "redirect:/recharge?accountNumber=" + accountNumber;
        }

        var acc = accOpt.get();
        accountService.resetLimitIfNewDay(acc);

        try {
            // 2. Perform Recharge & Save
            Recharge saved = rechargeService.doRecharge(
                    accountNumber,
                    mobileNumber,
                    operator,
                    rechargeType,
                    amount
            );

            // 3. Flash attributes to show on success page
            ra.addFlashAttribute("success", "Recharge successful!");
            ra.addFlashAttribute("accountNumber", accountNumber);
            ra.addFlashAttribute("mobileNumber", mobileNumber);
            ra.addFlashAttribute("operator", operator);
            ra.addFlashAttribute("amount", amount);
            ra.addFlashAttribute("rechargeType", rechargeType);  // ⭐ FIXED

            // 4. Redirect to Success Screen
            return "redirect:/recharge-success?accountNumber=" + accountNumber + "&id=" + saved.getId();

        } catch (Exception e) {
            // 5. On error
            ra.addFlashAttribute("error", "⚠ " + e.getMessage());
            return "redirect:/recharge?accountNumber=" + accountNumber;
        }
    }


    @GetMapping("/recharge-success")
    public String rechargeSuccess(@RequestParam String accountNumber,
                                  @RequestParam Long id,
                                  Model model) {
        model.addAttribute("accountNumber", accountNumber);
        model.addAttribute("id", id);
        model.addAttribute("dateTime", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));

        model.addAttribute("message", model.getAttribute("success"));
        model.addAttribute("error", model.getAttribute("error"));
        return "recharge-success";
    }
}
