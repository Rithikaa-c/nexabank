package com.nexa.bank.nexabank.controller;

import com.nexa.bank.nexabank.model.Transaction;
import com.nexa.bank.nexabank.repository.TransactionRepository;
import com.nexa.bank.nexabank.service.AdminLoggerService;
import com.nexa.bank.nexabank.service.RollbackService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminRollbackController {

    private final TransactionRepository transactionRepository;
    private final RollbackService rollbackService;
    @Autowired
    private AdminLoggerService logger;

    @Autowired
    public AdminRollbackController(TransactionRepository transactionRepository,
                                   RollbackService rollbackService) {
        this.transactionRepository = transactionRepository;
        this.rollbackService = rollbackService;
    }

    // Show all TRANSFER transactions
    @GetMapping("/rollback")
    public String rollbackList(Model model, HttpSession session) {

        if (session.getAttribute("loggedAdmin") == null) {
            return "redirect:/admin-login";
        }

        // Load only TRANSFER type
        List<Transaction> transfers = transactionRepository.findAll()
                .stream()
                .filter(t -> "TRANSFER".equalsIgnoreCase(t.getType()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());

        model.addAttribute("transfers", transfers);

        return "admin-rollback";
    }

    // Perform rollback
    @PostMapping("/rollback")
    public String performRollback(@RequestParam String transactionId,
                                  RedirectAttributes ra,
                                  HttpSession session) {

        if (session.getAttribute("loggedAdmin") == null) {
            ra.addFlashAttribute("error", "Please login as admin.");
            return "redirect:/admin-login";
        }

        String adminId = ((com.nexa.bank.nexabank.model.Admin)
                session.getAttribute("loggedAdmin")).getAdminId();

        try {
            rollbackService.rollbackTransfer(transactionId, adminId);

            logger.log(adminId, transactionId, "ROLLBACK PERFORMED");

            ra.addFlashAttribute("success",
                    "Rollback successful for transaction " + transactionId);
        }
        catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Rollback failed: " + ex.getMessage());
        }

        return "redirect:/admin/rollback";
    }
}
