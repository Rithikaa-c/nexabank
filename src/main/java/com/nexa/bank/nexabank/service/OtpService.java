package com.nexa.bank.nexabank.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;


import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;
import org.thymeleaf.TemplateEngine;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class OtpService {
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    private Map<String, String> otpStore = new HashMap<>();
    private Map<String, Long> otpTime = new HashMap<>();

    public void generateAndSendOtp(String email, JavaMailSender mailSender) {

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        otpStore.put(email, otp);
        otpTime.put(email, System.currentTimeMillis() + (2 * 60 * 1000)); // 2 minutes

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Your Nexa Bank OTP");
        message.setText("Your OTP is: " + otp);

        mailSender.send(message);
    }
    // -----------------------------------------------
// SEND OTP TO GUARDIAN FOR MINOR ACCOUNT LINKING
// -----------------------------------------------
    public void sendGuardianOtp(String email) {

        // generate random 6-digit OTP
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        // store OTP with 2-minute validity
        otpStore.put(email, otp);
        otpTime.put(email, System.currentTimeMillis() + (2 * 60 * 1000));

        // Send email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Parent/Guardian Verification OTP - Nexa Bank");
        message.setText("Your OTP for verifying as Guardian is: " + otp);

        mailSender.send(message);
    }
    public void sendMinorOtp(String minorEmail) {
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        otpStore.put(minorEmail, otp);
        otpTime.put(minorEmail, System.currentTimeMillis() + (2 * 60 * 1000));

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(minorEmail);
        msg.setSubject("Nexa Bank - Minor Account OTP");
        msg.setText("Your OTP is: " + otp);

        mailSender.send(msg);
    }

    public boolean verifyOtp(String email, String enteredOtp) {
        if (!otpStore.containsKey(email)) return false;

        if (System.currentTimeMillis() > otpTime.get(email)) {
            otpStore.remove(email);
            otpTime.remove(email);
            return false;
        }
        return otpStore.get(email).equals(enteredOtp);
    }
    public String generateCaptcha() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder captcha = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            captcha.append(characters.charAt(new Random().nextInt(characters.length())));
        }
        return captcha.toString();
    }
    public void sendWelcomeEmail(String toEmail, String name, String accNumber, String branch, String ifsc) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("🎉 Welcome to NexaBank – Your Account Details");

            Context ctx = new Context();
            ctx.setVariable("name", name);
            ctx.setVariable("accNumber", accNumber);
            ctx.setVariable("branch", branch);
            ctx.setVariable("ifsc", ifsc);

            String html = templateEngine.process("email-welcome.html", ctx);
            helper.setText(html, true);

            mailSender.send(message);

        } catch (Exception e) {
            System.out.println("Error sending welcome email: " + e.getMessage());
        }
    }



}

