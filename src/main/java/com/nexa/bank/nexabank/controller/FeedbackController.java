package com.nexa.bank.nexabank.controller;

import com.nexa.bank.nexabank.model.Feedback;
import com.nexa.bank.nexabank.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class FeedbackController {

    @Autowired
    private FeedbackRepository repo;

    // Save feedback
    @PostMapping("/feedback")
    @ResponseBody
    public ResponseEntity<?> saveFeedback(@RequestBody Feedback fb) {
        repo.save(fb);
        return ResponseEntity.ok("Saved");
    }

}
