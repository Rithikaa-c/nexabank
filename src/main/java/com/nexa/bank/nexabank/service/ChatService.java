package com.nexa.bank.nexabank.service;

import com.nexa.bank.nexabank.model.Account;
import com.nexa.bank.nexabank.model.Transaction;
import com.nexa.bank.nexabank.repository.AccountRepository;
import com.nexa.bank.nexabank.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ChatService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // -------------------------------------------------------------------
    // SPELL CORRECTION
    // -------------------------------------------------------------------
    private static final Map<String, String> keywords = Map.ofEntries(
            Map.entry("balnce", "balance"),
            Map.entry("balane", "balance"),
            Map.entry("accont", "account"),
            Map.entry("accout", "account"),
            Map.entry("accnt", "account"),
            Map.entry("statment", "statement"),
            Map.entry("crdit", "credit"),
            Map.entry("creditt", "credit"),
            Map.entry("crd", "card"),
            Map.entry("trnsfer", "transfer"),
            Map.entry("transfr", "transfer"),
            Map.entry("withdrwal", "withdraw"),
            Map.entry("withdral", "withdraw"),
            Map.entry("depo", "deposit"),
            Map.entry("depost", "deposit"),
            Map.entry("transacton", "transaction")
    );

    private String normalize(String msg) {
        for (var e : keywords.entrySet()) {
            if (msg.contains(e.getKey())) {
                msg = msg.replace(e.getKey(), e.getValue());
            }
        }
        return msg;
    }

    // -------------------------------------------------------------------
    // BUTTON CHIPS
    // -------------------------------------------------------------------
    private String chips(String... options) {
        StringBuilder sb = new StringBuilder("<br><div style='margin-top:8px;'>");
        for (String o : options) {
            sb.append("<button class='chat-chip' ")
                    .append("style='margin:4px; padding:6px 12px; background:#062045; color:white; ")
                    .append("border:0; border-radius:8px; cursor:pointer;'>")
                    .append(o)
                    .append("</button>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    // -------------------------------------------------------------------
    // MAIN CHATBOT LOGIC
    // -------------------------------------------------------------------
    public String getReply(String message, String accNumber) {

        if (message == null) message = "";
        String m = normalize(message.trim().toLowerCase(Locale.ROOT));

        Account acc = null;
        if (accNumber != null && !accNumber.isEmpty()) {
            acc = accountRepository.findByAccountNumber(accNumber).orElse(null);
        }

        // WELCOME MESSAGE
        if (m.equals("__welcome__")) {
            return "👋 <b>Nexa AI Assistant</b> here!<br>"
                    + "How can I help you today?"
                    + chips("Balance", "Last Login", "Credit Score", "Daily Limit", "Transactions", "Help");
        }

        // GREETINGS
        if (m.matches("^(hi|hello|hey|hlo|hai|helo).*$")) {
            return "Hello! 😊<br>How can I assist you?"
                    + chips("Balance", "Last Login", "Transactions", "Help");
        }

        // HELP MENU
        if (m.contains("help") || m.contains("what can")) {
            return """
                    Here’s what I can help you with:<br><br>
                    • 💰 Balance<br>
                    • 🕒 Last Login<br>
                    • 🔐 Card Freeze / Status<br>
                    • 📊 Credit Score<br>
                    • 💸 Deposit / Withdraw / Transfer<br>
                    • 📄 Statements / Transactions<br>
                    • 🧒 Guardian / Minor Account<br>
                    """ + chips("Balance", "Transfer", "Transactions", "Credit Score");
        }

        // -------------------------------------------------------------------
        // BALANCE
        // -------------------------------------------------------------------
        if (m.contains("balance") && acc != null) {
            return "🔥 <b>Your current balance:</b> ₹" + acc.getBalance()
                    + chips("Transactions", "Daily Limit", "Credit Score");
        }

        // -------------------------------------------------------------------
        // LAST LOGIN
        // -------------------------------------------------------------------
        if (m.contains("last login") && acc != null) {

            if (acc.getLastLoginTime() == null)
                return "This is your first login ✔"
                        + chips("Balance", "Transactions");

            DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

            return "⏱ <b>Last login:</b> "
                    + acc.getLastLoginTime().format(f)
                    + "<br>📍 From: <b>" + acc.getLastLoginIp() + "</b>"
                    + chips("Balance", "Credit Score", "Transactions");
        }

        // -------------------------------------------------------------------
        // ACCOUNT STATUS
        // -------------------------------------------------------------------
        if (m.contains("account status") && acc != null) {
            return "🔐 Your account status: <b>" + acc.getAccountStatus() + "</b>"
                    + chips("Balance", "Last Login");
        }

        // -------------------------------------------------------------------
        // CREDIT SCORE (REMOVED ANALYZE SPENDING)
        // -------------------------------------------------------------------
        if (m.contains("credit score") && acc != null) {

            String remark = "Average 🙂";
            int score = acc.getCreditScore();

            if (score >= 750) remark = "Great 👍";
            else if (score >= 650) remark = "Good 🙂";
            else remark = "Needs Improvement ⚠";

            return "📊 <b>Your credit score:</b> " + score
                    + " — " + remark
                    + chips("Balance", "Transactions");
        }

        // -------------------------------------------------------------------
        // DAILY LIMIT
        // -------------------------------------------------------------------
        if (m.contains("daily limit") && acc != null) {
            return "🔒 <b>Your daily limit:</b> ₹" + acc.getDailyLimit()

                    + chips("Balance", "Transactions", "Credit Score");
        }

        // -------------------------------------------------------------------
        // DEPOSIT HELP
        // -------------------------------------------------------------------
        if (m.contains("deposit")) {
            return """
                    To deposit money:<br>
                    1️⃣ Open <b>Deposit</b><br>
                    2️⃣ Enter amount<br>
                    3️⃣ Confirm OTP<br>
                    """ + chips("Balance", "Withdraw", "Transfer");
        }

        // -------------------------------------------------------------------
        // WITHDRAW HELP
        // -------------------------------------------------------------------
        if (m.contains("withdraw")) {
            return """
                    To withdraw:<br>
                    1️⃣ Go to <b>Withdraw</b><br>
                    2️⃣ Enter amount<br>
                    3️⃣ Confirm<br><br>
                    Note: Card must be ACTIVE.
                    """ + chips("Balance", "Transfer", "Daily Limit");
        }

        // -------------------------------------------------------------------
        // TRANSFER HELP
        // -------------------------------------------------------------------
        if (m.contains("transfer")) {
            return """
                    To transfer:<br>
                    1️⃣ Open <b>Transfer</b><br>
                    2️⃣ Enter receiver account<br>
                    3️⃣ Enter amount<br>
                    4️⃣ Confirm<br>
                    """ + chips("Balance", "Daily Limit", "Transactions");
        }

        // -------------------------------------------------------------------
        // CARD STATUS (FIXED)
        // -------------------------------------------------------------------
        if ((m.contains("card status") || m.contains("freeze") || m.contains("unfreeze")) && acc != null) {

            if (acc.isCardFrozen()) {
                return "❄️ <b>Your card is currently FROZEN.</b>"
                        + chips("Balance", "Daily Limit", "Transactions");
            } else {
                return "💳 <b>Your card is ACTIVE.</b>"
                        + chips("Balance", "Daily Limit", "Transactions");
            }
        }

        // -------------------------------------------------------------------
        // MINOR / GUARDIAN (FIXED)
        // -------------------------------------------------------------------
        if (m.contains("guardian") || m.contains("minor")) {
            if (acc == null) return "I couldn't load your account.";

            if (acc.isMinor()) {
                return "🧒 <b>This is a minor account.</b><br>"
                        + "Guardian A/C: <b>" + acc.getGuardianAccountNumber() + "</b>"
                        + chips("Balance", "Card Status");
            } else {
                return "✔ This is an adult account."
                        + chips("Balance", "Card Status");
            }
        }

        // -------------------------------------------------------------------
        // LAST 3 TRANSACTIONS (REMOVED ANALYZE SPENDING)
        // -------------------------------------------------------------------
        if (m.contains("statement") || m.contains("transactions") || m.contains("passbook")) {

            if (acc == null) return "I couldn't find your account.";

            List<Transaction> list = transactionRepository.getAllTransactionsForAccount(accNumber);

            if (list.isEmpty()) {
                return "No recent transactions found."
                        + chips("Balance", "Daily Limit");
            }

            StringBuilder sb = new StringBuilder("📄 <b>Last 3 Transactions:</b><br><br>");

            list.stream().limit(3).forEach(t ->
                    sb.append("• <b>").append(t.getType()).append("</b>")
                            .append(" — ₹").append(t.getAmount())
                            .append("<br>")
            );

            return sb.toString() + chips("Balance", "Credit Score");
        }

        // -------------------------------------------------------------------
        // UNKNOWN MESSAGE HANDLING
        // -------------------------------------------------------------------
        return "I’m not sure I understood that 🤔"
                + "<br>Try asking:"
                + "<br>• <b>balance</b>"
                + "<br>• <b>last login</b>"
                + "<br>• <b>credit score</b>"
                + chips("Balance", "Transfer", "Help");
    }
}
