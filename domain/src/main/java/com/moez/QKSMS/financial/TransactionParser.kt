package org.prauga.messages.financial

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionParser @Inject constructor() {

    // Matches: Rs.1,234.56 | INR 1234 | ₹1,234 | Rs 500.00
    private val amountRegex = Regex(
        """(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // Debit keywords
    private val debitKeywords = listOf(
        "debited", "debit", "paid", "payment of", "spent", "withdrawn",
        "charged", "purchase", "transferred to", "sent to"
    )

    // Credit keywords
    private val creditKeywords = listOf(
        "credited", "credit", "received", "refund", "cashback",
        "deposited", "added to", "transferred from"
    )

    // Merchant: text after "at ", "to ", "from ", "with " — grab up to 30 chars, stop at punctuation
    private val merchantRegex = Regex(
        """(?:at|to|from|with|for)\s+([A-Za-z0-9& ._\-]{2,30})""",
        RegexOption.IGNORE_CASE
    )

    // Account: last 4 digits in patterns like XX1234 / xxxx1234 / ending 1234 / a/c 1234
    private val accountRegex = Regex(
        """(?:XX|xx|X{4}|x{4}|ending\s*|a/?c\s*)(\d{4})""",
        RegexOption.IGNORE_CASE
    )

    fun parse(messageId: Long, body: String, date: Long): Transaction? {
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

        return Transaction(
            messageId = messageId,
            amount = amount,
            type = type,
            merchant = merchant,
            account = account,
            date = date,
            rawBody = body
        )
    }
}
