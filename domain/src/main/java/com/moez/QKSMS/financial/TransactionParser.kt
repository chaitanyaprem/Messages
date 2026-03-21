package org.prauga.messages.financial

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionParser @Inject constructor() {

    private val amountRegex = Regex(
        """(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val debitKeywords = listOf(
        "debited", "debit", "paid", "payment of", "spent", "withdrawn",
        "charged", "purchase", "transferred to", "sent to"
    )
    private val creditKeywords = listOf(
        "credited", "credit", "received", "refund", "cashback",
        "deposited", "added to", "transferred from"
    )

    // Merchant: text after "at ", "to ", "from ", "with ", "for "
    private val merchantRegex = Regex(
        """(?:at|to|from|with|for)\s+([A-Za-z0-9& ._\-]{2,30})""",
        RegexOption.IGNORE_CASE
    )

    // Last 4 digits: XX1234 / xxxx1234 / ending 1234 / a/c 1234 / ac 1234
    private val accountRegex = Regex(
        """(?:XX+|xx+|ending\s*|a/?c\s*no\.?\s*)(\d{4})\b""",
        RegexOption.IGNORE_CASE
    )

    // UPI VPA detection
    private val upiRegex = Regex("""[a-zA-Z0-9.\-_]+@[a-zA-Z0-9]+""")

    // Credit card keywords
    private val creditCardKeywords = listOf(
        "credit card", "creditcard", "cc ", " cc\n", "card ending", "card no"
    )

    // Known bank sender ID prefixes → friendly name
    private val bankSenderMap = mapOf(
        "HDFC" to "HDFC",
        "ICICI" to "ICICI",
        "SBI" to "SBI",
        "AXIS" to "Axis",
        "KOTAK" to "Kotak",
        "INDUS" to "IndusInd",
        "YES" to "Yes Bank",
        "BOI" to "Bank of India",
        "PNB" to "PNB",
        "CANARA" to "Canara",
        "UNION" to "Union Bank",
        "PAYTM" to "Paytm",
        "AMAZON" to "Amazon Pay",
        "PHONEPE" to "PhonePe",
        "GPAY" to "Google Pay"
    )

    fun parse(messageId: Long, body: String, date: Long, sender: String = ""): Transaction? {
        val amountMatch = amountRegex.find(body) ?: return null
        val amountStr = amountMatch.groupValues[1].replace(",", "")
        val amount = amountStr.toDoubleOrNull() ?: return null

        val upper = body.uppercase()
        val type = when {
            debitKeywords.any { upper.contains(it.uppercase()) } -> TransactionType.DEBIT
            creditKeywords.any { upper.contains(it.uppercase()) } -> TransactionType.CREDIT
            else -> TransactionType.UNKNOWN
        }

        val merchant = merchantRegex.find(body)
            ?.groupValues?.get(1)
            ?.trim()
            ?.take(30)
            ?.ifBlank { null }

        val account = accountRegex.find(body)?.groupValues?.get(1)

        // Determine account type
        val isUpi = upiRegex.containsMatchIn(body) || upper.contains("UPI") || upper.contains("VPA")
        val isCreditCard = creditCardKeywords.any { upper.contains(it.uppercase()) }
        val accountType = when {
            isUpi -> AccountType.UPI
            isCreditCard -> AccountType.CREDIT_CARD
            account != null -> AccountType.BANK
            else -> AccountType.UNKNOWN
        }

        // Build friendly account label from sender ID + last 4
        val bankName = bankSenderMap.entries
            .firstOrNull { sender.uppercase().contains(it.key) }
            ?.value
            ?: sender.take(6).ifBlank { null }

        val accountLabel = when {
            account != null && bankName != null -> "$bankName ••••$account"
            account != null -> "••••$account"
            bankName != null -> bankName
            else -> null
        }

        return Transaction(
            messageId = messageId,
            amount = amount,
            type = type,
            merchant = merchant,
            account = account,
            accountLabel = accountLabel,
            accountType = accountType,
            date = date,
            rawBody = body,
            sender = sender
        )
    }
}
