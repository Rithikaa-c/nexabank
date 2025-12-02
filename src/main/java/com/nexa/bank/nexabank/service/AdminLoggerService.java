package com.nexa.bank.nexabank.service;

import com.nexa.bank.nexabank.model.AdminLog;
import com.nexa.bank.nexabank.repository.AdminLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminLoggerService {

    private final AdminLogRepository repo;

    public AdminLoggerService(AdminLogRepository repo) {
        this.repo = repo;
    }

    public void log(String adminId, String accountNumber, String action) {
        AdminLog log = new AdminLog(adminId, accountNumber, action);
        repo.save(log);
    }

    // Overload → if no account involved
    public void log(String adminId, String action) {
        AdminLog log = new AdminLog(adminId, null, action);
        repo.save(log);
    }
}
