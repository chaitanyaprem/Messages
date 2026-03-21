package org.prauga.messages.categorization

import org.prauga.messages.model.Message
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategorizationEngine @Inject constructor() {

    // Basic rule-based categorization. This can be expanded significantly.
    fun categorize(sender: String, body: String): Message.MessageCategory {
        val upperBody = body.uppercase()
        val upperSender = sender.uppercase()

        // Rule 1: OTP Detection (high priority)
        if (isOtp(upperBody)) {
            return Message.MessageCategory.OTP
        }

        // Rule 2: Transactional Detection
        if (isTransactional(upperSender, upperBody)) {
            return Message.MessageCategory.TRANSACTIONAL
        }

        // Rule 3: Promotional Detection
        if (isPromotional(upperSender, upperBody)) {
            return Message.MessageCategory.PROMOTIONAL
        }
        
        // Rule 4: Check for personal numbers (basic)
        // If sender is a standard phone number length, it's likely personal.
        // This is a naive check and can be improved.
        if (sender.length >= 10 && sender.all { it.isDigit() || it == '+' }) {
            return Message.MessageCategory.PERSONAL
        }

        // Default to personal if sender looks like a person's name, but this is unreliable.
        // For now, if sender ID is short and not a number, it could be promotional.
        if (sender.length < 10 && sender.any{it.isLetter()}) {
             return Message.MessageCategory.PROMOTIONAL
        }


        return Message.MessageCategory.UNKNOWN
    }

    private fun isOtp(body: String): Boolean {
        // Look for keywords and typical OTP structure (e.g., 6-8 digit numbers)
        val otpKeywords = listOf("OTP", "VERIFICATION CODE", "VERIFY", "SECURITY CODE", "PASSCODE")
        if (otpKeywords.any { body.contains(it) }) {
            return true
        }
        // Regex to find 4-8 digit standalone numbers, common for OTPs
        if ("""\b\d{4,8}\b""".toRegex().containsMatchIn(body)) {
            return body.length < 160 // OTP messages are usually short
        }
        return false
    }

    private fun isTransactional(sender: String, body: String): Boolean {
        val transactionalKeywords = listOf(
            "DEBITED", "CREDITED", "TRANSACTION", "AC", "ACCOUNT", "ACCT", "INR", "RS.",
            "BOOKED", "CONFIRMED", "DELIVERED", "SHIPPED", "ORDER", "FLIGHT", "TRAIN"
        )
        if (transactionalKeywords.any { body.contains(it) }) {
            return true
        }
        
        // Sender IDs for banks often end with BANK
        if(sender.endsWith("BANK")){
            return true
        }

        return false
    }

    private fun isPromotional(sender: String, body: String): Boolean {
        val promotionalKeywords = listOf(
            "OFFER", "DISCOUNT", "SALE", "CASHBACK", "REWARD", "WIN", "CONGRATULATIONS", "LIMITED TIME"
        )
        if (promotionalKeywords.any { body.contains(it) }) {
            return true
        }
        
        // Indian promotional sender IDs often have a specific format e.g.(XX-XXXXXX)
        if ("""[A-Z]{2}-[A-Z0-9]{6}""".toRegex().matches(sender)) {
            return true
        }
        
        return false
    }
}
