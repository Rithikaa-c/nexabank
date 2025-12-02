package com.nexa.bank.nexabank.controller;

import com.nexa.bank.nexabank.model.Loan;
import com.nexa.bank.nexabank.repository.LoanRepository;
import com.nexa.bank.nexabank.repository.AccountRepository;
import com.nexa.bank.nexabank.service.LoanService;
import com.nexa.bank.nexabank.service.AdminLoggerService;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminLoanRequestController {

    private final LoanService loanService;
    private final LoanRepository loanRepository;
    private final AccountRepository accountRepository;
    private final AdminLoggerService logger;   // ⭐ added logger

    public AdminLoanRequestController(LoanRepository loanRepository,
                                      AccountRepository accountRepository,
                                      LoanService loanService,
                                      AdminLoggerService logger) {
        this.loanRepository = loanRepository;
        this.accountRepository = accountRepository;
        this.loanService = loanService;
        this.logger = logger;
    }

    @GetMapping("/admin/loan-requests")
    public String showLoanRequests(Model model) {
        model.addAttribute("loanList", loanRepository.findAllByOrderByCreatedAtDesc());
        return "admin-loan-requests";
    }

    @PostMapping("/admin/loans/approve")
    public String approveLoan(@RequestParam Long loanId,
                              @RequestParam String reason,
                              RedirectAttributes ra,
                              HttpSession session) {

        try {
            loanService.approveLoan(loanId, reason);
            ra.addFlashAttribute("success", "Loan Approved & Disbursed!");

            // ⭐ LOG ADMIN ACTION
            String adminId = ((com.nexa.bank.nexabank.model.Admin)
                    session.getAttribute("loggedAdmin")).getAdminId();

            Loan loan = loanRepository.findById(loanId).orElse(null);

            if (loan != null) {
                logger.log(adminId, loan.getAccountNumber(),
                        "LOAN APPROVED (" + loan.getLoanType() + ")");
            }

        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "Approve failed: " + e.getMessage());
        }

        return "redirect:/admin/loan-requests";
    }

    @PostMapping("/admin/loans/reject")
    public String rejectLoan(@RequestParam Long loanId,
                             @RequestParam String reason,
                             RedirectAttributes ra,
                             HttpSession session) {

        Loan loan = loanRepository.findById(loanId).orElse(null);

        if (loan == null) {
            ra.addFlashAttribute("error", "Loan not found!");
            return "redirect:/admin/loan-requests";
        }

        try {
            loanService.rejectLoan(loanId, reason);
            ra.addFlashAttribute("success", "Loan Rejected.");

            // ⭐ LOG ADMIN ACTION
            String adminId = ((com.nexa.bank.nexabank.model.Admin)
                    session.getAttribute("loggedAdmin")).getAdminId();

            logger.log(adminId, loan.getAccountNumber(),
                    "LOAN REJECTED (" + loan.getLoanType() + ")");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error while rejecting loan: " + e.getMessage());
        }

        return "redirect:/admin/loan-requests";
    }
}
