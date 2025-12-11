package com.nexa.bank.nexabank.service;

import com.nexa.bank.nexabank.repository.AccountRepository;
import com.nexa.bank.nexabank.repository.ComplaintRepository;
import com.nexa.bank.nexabank.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

@Service
public class AdminDashboardService {
    @Autowired
    private ComplaintRepository complaintRepo;

    private final AccountRepository accountRepo;
    private final TransactionRepository txRepo;

    public AdminDashboardService(AccountRepository accountRepo,
                                 TransactionRepository txRepo) {
        this.accountRepo = accountRepo;
        this.txRepo = txRepo;
    }

    // ⭐ Total amount in bank
    public BigDecimal getTotalBankBalance() {
        BigDecimal total = accountRepo.getTotalBalances();
        return total == null ? BigDecimal.ZERO : total;
    }

    // ⭐ Total accounts
    public Long getTotalAccounts() {
        return accountRepo.countTotalAccounts();
    }

    // ⭐ Active accounts
    public Long getActiveAccounts() {
        return accountRepo.countActiveAccounts();
    }

    // ⭐ Frozen accounts
    public Long getFrozenAccounts() {
        return accountRepo.countFrozenAccounts();
    }

    // ⭐ Total transactions
    public Long getTransactionCount() {
        return txRepo.countAllTransactions();
    }
    // convert List<Object[]> (dateString, total) to map date->BigDecimal, ensures all 7 days present
    public Map<String, BigDecimal> getDailySums(List<Object[]> rows) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // initialize last 7 days
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            map.put(d.format(fmt), BigDecimal.ZERO);
        }
        if (rows == null) return map;
        for (Object[] r : rows) {
            // r[0] -> date (String or java.sql.Date), r[1] -> BigDecimal/Number
            String dateStr = String.valueOf(r[0]);
            BigDecimal total = r[1] == null ? BigDecimal.ZERO : new BigDecimal(r[1].toString());
            if (map.containsKey(dateStr)) map.put(dateStr, total);
        }
        return map;
    }

    public Map<String, BigDecimal> getLast7DaysDeposits() {
        List<Object[]> rows = accountRepo instanceof Object ? txRepo.sumDailyDepositsLast7Days() : txRepo.sumDailyDepositsLast7Days();
        return getDailySums(rows);
    }

    public Map<String, BigDecimal> getLast7DaysWithdrawals() {
        List<Object[]> rows = txRepo.sumDailyWithdrawalsLast7Days();
        return getDailySums(rows);
    }

    public Map<String, BigDecimal> getLast7DaysRepayments() {
        List<Object[]> rows = txRepo.sumDailyRepaymentsLast7Days();
        return getDailySums(rows);
    }

    // Top active accounts (returns map account->count)
    public List<Map.Entry<String, Long>> getTopActiveAccounts(int limit) {
        List<Object[]> rows = txRepo.findTopActiveAccounts(limit);
        List<Map.Entry<String, Long>> res = new ArrayList<>();
        for (Object[] r : rows) {
            String acc = r[0] == null ? "UNKNOWN" : r[0].toString();
            Long cnt = r[1] == null ? 0L : Long.valueOf(r[1].toString());
            res.add(new AbstractMap.SimpleEntry<>(acc, cnt));
        }
        return res;
    }

    // Today's hourly heatmap - returns array size 24
    public long[] getTodayHourlyHeatmap() {
        long[] arr = new long[24];
        List<Object[]> rows = txRepo.getTodayHourlyActivity();
        for (Object[] r : rows) {
            int hr = r[0] == null ? 0 : Integer.parseInt(r[0].toString());
            long cnt = r[1] == null ? 0L : Long.parseLong(r[1].toString());
            if (hr >= 0 && hr < 24) arr[hr] = cnt;
        }
        return arr;
    }

    // Pending complaints
    public Long getPendingComplaintsCount() {
        return complaintRepo.countByStatus("Pending");
    }

}
