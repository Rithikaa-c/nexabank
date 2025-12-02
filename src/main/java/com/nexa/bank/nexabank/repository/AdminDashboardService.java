package com.nexa.bank.nexabank.service;

import com.nexa.bank.nexabank.repository.AccountRepository;
import com.nexa.bank.nexabank.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AdminDashboardService {

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
}
