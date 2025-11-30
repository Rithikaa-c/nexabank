package com.nexa.bank.nexabank.controller;

import com.nexa.bank.nexabank.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class ChatbotController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/chatbot")
    public @ResponseBody String chat(
            @RequestParam(name="message") String message,
            @RequestParam(name="accNumber", required=false) String accNumber) {

        if (message == null || message.trim().isEmpty()) {
            return "Please type a question.";
        }

        return chatService.getReply(message, accNumber);
    }
}
