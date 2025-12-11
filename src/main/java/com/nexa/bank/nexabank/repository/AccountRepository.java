package com.nexa.bank.nexabank.repository;

import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.nexa.bank.nexabank.model.Account;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
@Transactional

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Account findByEmail(String email);
    Optional<Account> findByAccountNumber(String accountNumber);

    @Query("SELECT COUNT(a) FROM Account a")
    Long countTotalAccounts();

    @Query("SELECT COUNT(a) FROM Account a WHERE a.accountStatus = 'ACTIVE' AND a.cardFrozen = false")
    Long countActiveAccounts();

    @Query("SELECT COUNT(a) FROM Account a WHERE a.cardFrozen = true")
    Long countFrozenAccounts();
    @Modifying
    @Query("DELETE FROM Account a WHERE a.accountNumber = :acc")
    void deleteByAccountNumber(@Param("acc") String acc);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber = :acc")
    Optional<Account> findByAccountNumberForUpdate(@Param("acc") String acc);
    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a")
    BigDecimal getTotalBalances();
    @Modifying
    @Query("UPDATE Account a SET a.cardFrozen = :status WHERE a.accountNumber = :acc")
    void updateFreezeStatus(@Param("acc") String acc, @Param("status") boolean status);
}

