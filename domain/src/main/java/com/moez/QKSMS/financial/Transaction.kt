package org.prauga.messages.financial

data class Transaction(
    val messageId: Long,
    val amount: Double,
    val type: TransactionType,
    val merchant: String?,
    val account: String?,           // last 4 digits e.g. "4321"
    val accountLabel: String?,      // display label e.g. "HDFC ••••4321"
    val accountType: AccountType,
    val date: Long,
    val rawBody: String,
    val sender: String = ""
)

enum class TransactionType { DEBIT, CREDIT, UNKNOWN }
enum class AccountType { BANK, CREDIT_CARD, UPI, UNKNOWN }
