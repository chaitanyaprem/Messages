package org.prauga.messages.repository

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import org.prauga.messages.financial.Transaction
import org.prauga.messages.financial.TransactionParser
import org.prauga.messages.financial.TransactionType
import org.prauga.messages.model.Message
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinancialRepositoryImpl @Inject constructor(
    private val transactionParser: TransactionParser
) : FinancialRepository {

    private fun fetchTransactions(): List<Transaction> {
        val realm = Realm.getDefaultInstance()
        return try {
            val messages: RealmResults<Message> = realm.where(Message::class.java)
                .equalTo("categoryString", "TRANSACTIONAL")
                .sort("date", Sort.DESCENDING)
                .findAll()
            messages.mapNotNull { msg: Message ->
                try {
                    transactionParser.parse(msg.id, msg.body, msg.date)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse message ${msg.id}")
                    null
                }
            }
        } finally {
            realm.close()
        }
    }

    override fun getTransactions(limit: Int): List<Transaction> {
        return fetchTransactions().take(limit)
    }

    override fun getTotalDebits(sinceMs: Long): Double {
        return fetchTransactions()
            .filter { it.date >= sinceMs && it.type == TransactionType.DEBIT }
            .sumOf { it.amount }
    }

    override fun getTotalCredits(sinceMs: Long): Double {
        return fetchTransactions()
            .filter { it.date >= sinceMs && it.type == TransactionType.CREDIT }
            .sumOf { it.amount }
    }
}
