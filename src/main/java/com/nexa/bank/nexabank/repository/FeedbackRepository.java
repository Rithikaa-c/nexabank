package com.nexa.bank.nexabank.repository;

import com.nexa.bank.nexabank.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}
