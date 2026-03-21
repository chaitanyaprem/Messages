package org.prauga.messages.repository

import org.prauga.messages.financial.Transaction

interface FinancialRepository {
    fun getTransactions(limit: Int = 100): List<Transaction>
    fun getTotalDebits(sinceMs: Long): Double
    fun getTotalCredits(sinceMs: Long): Double
}
