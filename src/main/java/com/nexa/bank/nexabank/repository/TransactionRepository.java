package com.nexa.bank.nexabank.repository;

import com.nexa.bank.nexabank.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    // Fetch ALL types of transactions involving this account
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.accountNumber = :acc
            OR t.senderAccountNumber = :acc
            OR t.receiverAccountNumber = :acc
            ORDER BY t.createdAt DESC
           """)
    List<Transaction> getAllTransactionsForAccount(@Param("acc") String accountNumber);
    @Query("SELECT t FROM Transaction t WHERE t.senderAccountNumber = :acc OR t.receiverAccountNumber = :acc ORDER BY t.createdAt DESC")
    List<Transaction> getPassbook(@Param("acc") String acc);
    @Query("SELECT COUNT(t) FROM Transaction t")
    Long countAllTransactions();
    @Query(value = """
    SELECT t.dayName, t.cnt FROM (
        SELECT 
            DAYNAME(transaction_time) AS dayName,
            COUNT(*) AS cnt
        FROM transactions
        WHERE transaction_time >= CURDATE() - INTERVAL 7 DAY
        GROUP BY dayName
    ) AS t
    ORDER BY FIELD(t.dayName,
        'Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday')
    """, nativeQuery = true)
    List<Object[]> getLast7DaysActivity();
    // add near other method signatures
    Optional<Transaction> findByTransactionId(String transactionId);

    @Query("SELECT t FROM Transaction t WHERE t.type = :type ORDER BY t.createdAt DESC")
    List<Transaction> findByTypeOrderByCreatedAtDesc(@Param("type") String type);
    // Last 7 days deposits grouped by day (yyyy-mm-dd + sum)
    @Query(value = """
  SELECT DATE(transaction_time) AS dt, COALESCE(SUM(amount),0) AS total
  FROM transactions
  WHERE type = 'DEPOSIT' AND transaction_time >= CURDATE() - INTERVAL 6 DAY
  GROUP BY DATE(transaction_time)
  ORDER BY DATE(transaction_time);
""", nativeQuery = true)
    List<Object[]> sumDailyDepositsLast7Days();

    // Last 7 days withdrawals
    @Query(value = """
  SELECT DATE(transaction_time) AS dt, COALESCE(SUM(amount),0) AS total
  FROM transactions
  WHERE type = 'WITHDRAWAL' AND transaction_time >= CURDATE() - INTERVAL 6 DAY
  GROUP BY DATE(transaction_time)
  ORDER BY DATE(transaction_time);
""", nativeQuery = true)
    List<Object[]> sumDailyWithdrawalsLast7Days();

    // Last 7 days loan repayments (transactions type REPAYMENT)
    @Query(value = """
  SELECT DATE(transaction_time) AS dt, COALESCE(SUM(amount),0) AS total
  FROM transactions
  WHERE type = 'REPAYMENT' AND transaction_time >= CURDATE() - INTERVAL 6 DAY
  GROUP BY DATE(transaction_time)
  ORDER BY DATE(transaction_time);
""", nativeQuery = true)
    List<Object[]> sumDailyRepaymentsLast7Days();

    // Most active accounts (top N by txn count)
    @Query(value = """
  SELECT account_number, COUNT(*) as cnt
  FROM (
     SELECT account_number FROM transactions
     UNION ALL
     SELECT sender_account_number as account_number FROM transactions WHERE sender_account_number IS NOT NULL
     UNION ALL
     SELECT receiver_account_number as account_number FROM transactions WHERE receiver_account_number IS NOT NULL
  ) t
  GROUP BY account_number
  ORDER BY cnt DESC
  LIMIT :limit
""", nativeQuery = true)
    List<Object[]> findTopActiveAccounts(@Param("limit") int limit);

    // Today's hourly heatmap (0-23) using transaction_time
    @Query(value = """
  SELECT HOUR(transaction_time) as hr, COUNT(*) as cnt
  FROM transactions
  WHERE DATE(transaction_time) = CURDATE()
  GROUP BY hr
  ORDER BY hr;
""", nativeQuery = true)
    List<Object[]> getTodayHourlyActivity();

}
