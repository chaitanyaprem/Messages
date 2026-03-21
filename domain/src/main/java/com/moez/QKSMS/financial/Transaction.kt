package org.prauga.messages.financial

data class Transaction(
    val messageId: Long,
    val amount: Double,
    val type: TransactionType,
    val merchant: String?,
    val account: String?,
    val date: Long,
    val rawBody: String
)

enum class TransactionType { DEBIT, CREDIT, UNKNOWN }
