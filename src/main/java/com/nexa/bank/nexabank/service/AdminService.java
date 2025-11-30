package com.nexa.bank.nexabank.service;

import com.nexa.bank.nexabank.model.Admin;
import com.nexa.bank.nexabank.repository.AdminRepository;
import com.nexa.bank.nexabank.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final AdminRepository repo;

    @Autowired
    private TransactionRepository transactionRepository;

    public AdminService(AdminRepository repo) {
        this.repo = repo;
    }

    // -----------------------------------------
    // VALIDATE LOGIN
    // -----------------------------------------
    public Admin validateLogin(String adminId, String password) {
        return repo.findByAdminId(adminId)
                .filter(a -> a.getPassword().equals(password))
                .orElse(null);
    }

    // -----------------------------------------
    // SAVE ADMIN (Last login Time + IP)
    // -----------------------------------------
    public void saveAdmin(Admin admin) {
        repo.save(admin);
    }

    // -----------------------------------------
    // WEEKLY ACTIVITY ANALYTICS
    // -----------------------------------------
    public Map<String, Integer> getWeeklyActivity() {

        // Query returns rows: [dayName, count]
        List<Object[]> raw = transactionRepository.getLast7DaysActivity();

        // LinkedHashMap keeps order
        Map<String, Integer> result = new LinkedHashMap<>();

        // Ensure all 7 days appear
        List<String> days = List.of(
                "Monday", "Tuesday", "Wednesday",
                "Thursday", "Friday", "Saturday", "Sunday"
        );

        // Default 0 for all days
        for (String d : days) {
            result.put(d, 0);
        }

        // Fill actual data
        for (Object[] row : raw) {
            String day = row[0].toString();
            Integer count = ((Number) row[1]).intValue();
            result.put(day, count);
        }

        return result;
    }
}
