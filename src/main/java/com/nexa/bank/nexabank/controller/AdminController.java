package com.nexa.bank.nexabank.controller;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.nexa.bank.nexabank.model.*;
import com.nexa.bank.nexabank.repository.AccountRepository;
import com.nexa.bank.nexabank.repository.AdminLogRepository;
import com.nexa.bank.nexabank.repository.ComplaintRepository;
import com.nexa.bank.nexabank.repository.TransactionRepository;
import com.nexa.bank.nexabank.service.AdminDashboardService;
import com.nexa.bank.nexabank.service.AdminService;
import com.nexa.bank.nexabank.service.EmailService;
import com.nexa.bank.nexabank.service.OtpService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
public class AdminController {
    private final JavaMailSender mailSender;
    @Autowired
    private ComplaintRepository complaintRepository;

    private final EmailService emailService;
    private final AdminLogRepository logRepository;
    private final AdminService adminService;

    private final AdminDashboardService dashboardService;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    private final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private final OtpService otpService;

    public AdminController(AdminService adminService,
                           AdminDashboardService dashboardService,
                           AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           EmailService emailService,
                           AdminLogRepository logRepository,
                           OtpService otpService,
                           JavaMailSender mailSender) {

        this.adminService = adminService;
        this.dashboardService = dashboardService;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.emailService = emailService;
        this.logRepository = logRepository;
        this.otpService = otpService;
        this.mailSender = mailSender;
    }

    // ------------------------------------------
    // ADMIN LOGIN PAGE
    // ------------------------------------------
    @GetMapping("/admin-login")
    public String showLogin(Model model) {
        String captcha = otpService.generateCaptcha();   // OPTIONAL: if you want captcha
        model.addAttribute("captcha", captcha);
        return "admin-login";
    }

    // ------------------------------------------
    @PostMapping("/admin/login")
    public String loginAdmin(@RequestParam String adminId,
                             @RequestParam String password,
                             @RequestParam(required = false) String captchaInput,
                             @RequestParam(required = false) String captcha,
                             Model model,
                             HttpSession session) {

        // (Optional) CAPTCHA validation
        if (captcha != null && captchaInput != null && !captcha.equals(captchaInput)) {
            model.addAttribute("captchaError", "Incorrect CAPTCHA");
            model.addAttribute("captcha", otpService.generateCaptcha());
            return "admin-login";
        }

        // Validate admin credentials
        Admin admin = adminService.validateLogin(adminId, password);

        if (admin == null) {
            model.addAttribute("error", "Invalid Admin ID or Password");
            model.addAttribute("captcha", otpService.generateCaptcha());
            return "admin-login";
        }

        // Hold admin temporarily until OTP verified
        session.setAttribute("pendingAdmin", admin);

        // Send OTP email
        otpService.generateAndSendOtp(admin.getEmail(), mailSender);


        // Pass email to OTP page
        model.addAttribute("email", admin.getEmail());

        return "admin-login-otp";   // Your new OTP page
    }

    @PostMapping("/admin/verify-login-otp")
    public String verifyAdminOtp(@RequestParam String email,
                                 @RequestParam String otp,
                                 HttpSession session,
                                 HttpServletRequest request,
                                 Model model) {

        boolean isValid = otpService.verifyOtp(email, otp);

        if (!isValid) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Invalid OTP. Try Again.");
            return "admin-login-otp";
        }

        // Fetch pending admin
        Admin admin = (Admin) session.getAttribute("pendingAdmin");

        if (admin == null) {
            model.addAttribute("error", "Session expired. Please login again.");
            return "admin-login";
        }

        // ⭐ GET LOGIN TIME + IP
        LocalDateTime loginTime = LocalDateTime.now();
        String ip = request.getRemoteAddr();

        admin.setLastLoginTime(loginTime);
        admin.setLastLoginIp(ip);

// Save to DB
        adminService.saveAdmin(admin);

        // Save to DB
        adminService.saveAdmin(admin);

        // Move admin to logged session
        session.removeAttribute("pendingAdmin");
        session.setAttribute("loggedAdmin", admin);
        model.addAttribute("weeklyActivity", adminService.getWeeklyActivity());


        return "redirect:/admin-dashboard";
    }

    @PostMapping("/admin/resend-login-otp")
    public String resendAdminOtp(@RequestParam String email, Model model) {

        otpService.generateAndSendOtp(email, mailSender);

        model.addAttribute("email", email);
        model.addAttribute("message", "OTP resent successfully!");
        return "admin-login-otp";
    }

    // ------------------------------------------
    // DASHBOARD
    // ------------------------------------------
    @GetMapping("/admin-dashboard")
    public String adminDashboard(HttpSession session, Model model) {

        Admin admin = (Admin) session.getAttribute("loggedAdmin");
        if (admin == null) return "redirect:/admin-login";

        model.addAttribute("loggedAdmin", admin);
        model.addAttribute("totalAccounts", dashboardService.getTotalAccounts());
        model.addAttribute("activeAccounts", dashboardService.getActiveAccounts());
        model.addAttribute("frozenAccounts", dashboardService.getFrozenAccounts());
        model.addAttribute("transactionCount", dashboardService.getTransactionCount());

        // ⭐ Weekly Activity Chart Data
        model.addAttribute("weeklyActivity", adminService.getWeeklyActivity());

        // Format login time
        if (admin.getLastLoginTime() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
            model.addAttribute("formattedLastLogin", admin.getLastLoginTime().format(formatter));
        } else {
            model.addAttribute("formattedLastLogin", "First Login");
        }

        // Format IP
        String ip = admin.getLastLoginIp();
        if (ip == null) ip = "Unknown";
        if (ip.equals("0:0:0:0:0:0:0:1"))
            model.addAttribute("formattedIp", "Localhost (127.0.0.1)");
        else
            model.addAttribute("formattedIp", ip);

        return "admin-dashboard";
    }

    // ------------------------------------------
    // VIEW ALL ACCOUNTS
    // ------------------------------------------
    @GetMapping("/admin/accounts")
    public String viewAllAccounts(HttpSession session, Model model) {

        if (session.getAttribute("loggedAdmin") == null)
            return "redirect:/admin-login";

        model.addAttribute("accounts", accountRepository.findAll());
        return "admin-accounts";
    }

    // ------------------------------------------
    // VIEW ALL TRANSACTIONS
    // ------------------------------------------
    @GetMapping("/admin/transactions")
    public String viewAllTransactions(HttpSession session, Model model) {

        if (session.getAttribute("loggedAdmin") == null)
            return "redirect:/admin-login";

        model.addAttribute("transactions", transactionRepository.findAll());
        return "admin-transactions";
    }

    // ------------------------------------------
    // LOGOUT
    // ------------------------------------------
    @GetMapping("/admin/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin-login";
    }

    // ---------------------------------------------------------
    // FREEZE / UNFREEZE ACCOUNT PAGE
    // ---------------------------------------------------------
    @GetMapping("/admin/freeze-account")
    public String freezePage(HttpSession session, Model model) {

        if (session.getAttribute("loggedAdmin") == null)
            return "redirect:/admin-login";

        model.addAttribute("accounts", accountRepository.findAll());
        return "admin-freeze";
    }

    // ---------------------------------------------------------
    // FREEZE ACCOUNT
    // ---------------------------------------------------------
    @PostMapping("/admin/freeze")
    public String freezeAccount(@RequestParam String accountNumber,
                                HttpSession session) {

        String adminId = ((Admin) session.getAttribute("loggedAdmin")).getAdminId();

        accountRepository.updateFreezeStatus(accountNumber, true);

        Account acc = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Log action
        logRepository.save(new AdminLog(adminId, accountNumber, "FREEZE"));

        // Send email
        emailService.sendAccountFreezeEmail(
                acc.getEmail(),
                acc.getHolderName(),
                accountNumber,
                true
        );

        return "redirect:/admin/freeze-account";
    }

    // ---------------------------------------------------------
    // UNFREEZE ACCOUNT
    // ---------------------------------------------------------
    @PostMapping("/admin/unfreeze")
    public String unfreezeAccount(@RequestParam String accountNumber,
                                  HttpSession session) {

        String adminId = ((Admin) session.getAttribute("loggedAdmin")).getAdminId();

        accountRepository.updateFreezeStatus(accountNumber, false);

        Account acc = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Log action
        logRepository.save(new AdminLog(adminId, accountNumber, "UNFREEZE"));

        // Send email
        emailService.sendAccountFreezeEmail(
                acc.getEmail(),
                acc.getHolderName(),
                accountNumber,
                false
        );

        return "redirect:/admin/freeze-account";
    }

    // ---------------------------------------------------------
    // ADMIN LOGS PAGE
    // ---------------------------------------------------------
    @GetMapping("/admin/logs")
    public String viewLogs(HttpSession session, Model model) {

        if (session.getAttribute("loggedAdmin") == null)
            return "redirect:/admin-login";

        model.addAttribute("logs", logRepository.findAll());
        return "admin-logs";
    }

    // ---------------------------------------------------------
    // REPORTS PAGE (UI ONLY)
    // ---------------------------------------------------------
    @GetMapping("/admin/reports")
    public String reportsPage(HttpSession session, Model model) {

        if (session.getAttribute("loggedAdmin") == null)
            return "redirect:/admin-login";

        model.addAttribute("accounts", accountRepository.findAll());
        model.addAttribute("transactions", transactionRepository.findAll());

        return "admin-reports";
    }

    // =========================================================
    // --------------------- CSV REPORTS -----------------------
    // =========================================================

    // ACCOUNTS CSV
    @GetMapping("/admin/reports/accounts/csv")
    public void downloadAccountsCSV(HttpServletResponse response) throws IOException {

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=accounts.csv");

        List<Account> accounts = accountRepository.findAll();
        PrintWriter writer = response.getWriter();

        writer.println("Account Number,Holder Name,Email,Balance,Status");

        for (Account acc : accounts) {
            writer.println(
                    acc.getAccountNumber() + "," +
                            acc.getHolderName() + "," +
                            acc.getEmail() + "," +
                            acc.getBalance() + "," +
                            (acc.isCardFrozen() ? "Frozen" : "Active")
            );
        }
        writer.flush();
    }

    // TRANSACTIONS CSV  (uses transactionTime)
    @GetMapping("/admin/reports/transactions/csv")
    public void downloadTransactionsCSV(HttpServletResponse response) throws IOException {

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=transactions.csv");

        List<Transaction> txList = transactionRepository.findAll();
        PrintWriter writer = response.getWriter();

        writer.println("Txn ID,Account Number,Type,Amount,Transaction Time");

        for (Transaction t : txList) {
            String time = "";
            if (t.getTransactionTime() != null) {
                time = t.getTransactionTime().format(dateTimeFormatter);
            }

            writer.println(
                    t.getTransactionId() + "," +
                            t.getAccountNumber() + "," +
                            t.getType() + "," +
                            t.getAmount() + "," +
                            time
            );
        }
        writer.flush();
    }

    // ADMIN LOGS CSV
    @GetMapping("/admin/reports/logs/csv")
    public void downloadLogsCSV(HttpServletResponse response) throws IOException {

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=admin_logs.csv");

        List<AdminLog> logs = logRepository.findAll();
        PrintWriter writer = response.getWriter();

        writer.println("Admin ID,Account Number,Action,Time");

        for (AdminLog log : logs) {
            String time = "";
            if (log.getTimestamp() != null) {
                time = log.getTimestamp().format(dateTimeFormatter);
            }

            writer.println(
                    log.getAdminId() + "," +
                            log.getAccountNumber() + "," +
                            log.getAction() + "," +
                            time
            );
        }
        writer.flush();
    }

    // =========================================================
    // --------------------- PDF REPORTS -----------------------
    // =========================================================

    // ACCOUNTS PDF
    @GetMapping("/admin/reports/accounts/pdf")
    public void downloadAccountsPDF(HttpServletResponse response) throws Exception {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=accounts.pdf");

        List<Account> accounts = accountRepository.findAll();

        Document document = new Document();
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        document.add(new Paragraph("Accounts Report"));
        document.add(new Paragraph(" ")); // empty line

        PdfPTable table = new PdfPTable(5); // 5 columns
        table.addCell("Account Number");
        table.addCell("Holder Name");
        table.addCell("Email");
        table.addCell("Balance");
        table.addCell("Status");

        for (Account acc : accounts) {
            table.addCell(acc.getAccountNumber());
            table.addCell(acc.getHolderName());
            table.addCell(acc.getEmail());
            table.addCell(acc.getBalance().toString());
            table.addCell(acc.isCardFrozen() ? "Frozen" : "Active");
        }

        document.add(table);
        document.close();
    }

    // TRANSACTIONS PDF
    @GetMapping("/admin/reports/transactions/pdf")
    public void downloadTransactionsPDF(HttpServletResponse response) throws Exception {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=transactions.pdf");

        List<Transaction> txList = transactionRepository.findAll();

        Document document = new Document();
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        document.add(new Paragraph("Transactions Report"));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(5); // Txn ID, Account, Type, Amount, Time
        table.addCell("Txn ID");
        table.addCell("Account Number");
        table.addCell("Type");
        table.addCell("Amount");
        table.addCell("Transaction Time");

        for (Transaction t : txList) {
            String time = "";
            if (t.getTransactionTime() != null) {
                time = t.getTransactionTime().format(dateTimeFormatter);
            }

            table.addCell(t.getTransactionId());
            table.addCell(t.getAccountNumber());
            table.addCell(t.getType());
            table.addCell(t.getAmount().toString());
            table.addCell(time);
        }

        document.add(table);
        document.close();
    }

    // ADMIN LOGS PDF
    @GetMapping("/admin/reports/logs/pdf")
    public void downloadLogsPDF(HttpServletResponse response) throws Exception {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=admin_logs.pdf");

        List<AdminLog> logs = logRepository.findAll();

        Document document = new Document();
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        document.add(new Paragraph("Admin Logs Report"));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4); // Admin ID, Account, Action, Time
        table.addCell("Admin ID");
        table.addCell("Account Number");
        table.addCell("Action");
        table.addCell("Time");

        for (AdminLog log : logs) {
            String time = "";
            if (log.getTimestamp() != null) {
                time = log.getTimestamp().format(dateTimeFormatter);
            }

            table.addCell(log.getAdminId());
            table.addCell(log.getAccountNumber());
            table.addCell(log.getAction());
            table.addCell(time);
        }

        document.add(table);
        document.close();
    }
    @GetMapping("/admin/complaints")
    public String viewAllComplaints(HttpSession session, Model model) {

        if (session.getAttribute("loggedAdmin") == null)
            return "redirect:/admin-login";

        List<Complaint> complaints = complaintRepository.findAll();

        // Sort complaints DESCENDING by ID (latest first)
        complaints.sort((a, b) -> b.getId().compareTo(a.getId()));

        model.addAttribute("complaints", complaints);

        return "admin-complaints";
    }

    @GetMapping("/admin/close-complaint")
    public String closeComplaint(@RequestParam Long id, HttpSession session) {

        Complaint complaint = complaintRepository.findById(id).orElse(null);

        if (complaint != null) {

            complaint.setStatus("Resolved");
            complaintRepository.save(complaint);

            // ⭐ Log admin action
            String adminId = ((Admin) session.getAttribute("loggedAdmin")).getAdminId();
            logRepository.save(new AdminLog(adminId, complaint.getAccountNumber(), "COMPLAINT_CLOSE"));
        }

        return "redirect:/admin/complaints";
    }

    @GetMapping("/admin/reply-complaint")
    public String replyForm(@RequestParam Long id, Model model) {
        Complaint complaint = complaintRepository.findById(id).orElse(null);
        model.addAttribute("complaint", complaint);
        return "admin-reply";
    }
    @PostMapping("/admin/send-reply")
    public String sendReply(@RequestParam Long id,
                            @RequestParam String reply,
                            HttpSession session) {

        Complaint complaint = complaintRepository.findById(id).orElse(null);

        if (complaint != null) {

            // Save reply
            complaint.setAdminReply(reply);
            complaint.setStatus("Replied");
            complaintRepository.save(complaint);

            // ⭐ Log admin action
            String adminId = ((Admin) session.getAttribute("loggedAdmin")).getAdminId();
            logRepository.save(new AdminLog(adminId, complaint.getAccountNumber(), "COMPLAINT_REPLY"));

            // Send email notification
            emailService.sendComplaintReplyEmail(
                    complaint.getEmail(),
                    complaint.getId(),
                    complaint.getType(),
                    reply
            );
        }

        return "redirect:/admin/complaints";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin-dashboard";  // your file name
    }

    @GetMapping("/admin/activity-heatmap")
    public String heatmap(HttpSession session, Model model) {

        Admin logged = (Admin) session.getAttribute("loggedAdmin");

        if (logged == null) {
            return "redirect:/admin-login";
        }

        // Weekly data
        Map<String, Integer> weekly = adminService.getWeeklyActivity();
        List<Integer> weeklyList = new ArrayList<>(weekly.values());

        // Find most active day
        int max = Collections.max(weekly.values());
        String mostActiveDay = weekly.entrySet()
                .stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("");

        // Send to HTML
        model.addAttribute("weeklyData", weeklyList);
        model.addAttribute("mostActiveDay", mostActiveDay);

        // 🔥 IMPORTANT: Add admin session for dashboard
        model.addAttribute("loggedAdmin", logged);

        return "admin-heatmap";
    }


}
