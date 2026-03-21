package org.prauga.messages.feature.financial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.prauga.messages.financial.Transaction
import org.prauga.messages.financial.TransactionType
import org.prauga.messages.repository.FinancialRepository
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

data class TopMerchant(val name: String, val amount: Double, val count: Int)

data class FinancialState(
    val totalDebits: Double = 0.0,
    val totalCredits: Double = 0.0,
    val topMerchants: List<TopMerchant> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val monthLabel: String = "",
    val loading: Boolean = true
)

class FinancialViewModel @Inject constructor(
    private val financialRepository: FinancialRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FinancialState())
    val state: StateFlow<FinancialState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cal = Calendar.getInstance()
                val start = (cal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val end = cal.timeInMillis

                val all = financialRepository.getTransactions(500)
                val thisMonth = all.filter { it.date in start..end }

                val debits = thisMonth.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
                val credits = thisMonth.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }

                // Top 3 merchants by spend this month
                val topMerchants = thisMonth
                    .filter { it.type == TransactionType.DEBIT && it.merchant != null }
                    .groupBy { it.merchant!! }
                    .map { (name, txns) -> TopMerchant(name, txns.sumOf { it.amount }, txns.size) }
                    .sortedByDescending { it.amount }
                    .take(3)

                val monthName = android.text.format.DateFormat.format("MMMM yyyy", cal).toString()

                _state.value = FinancialState(
                    totalDebits = debits,
                    totalCredits = credits,
                    topMerchants = topMerchants,
                    recentTransactions = thisMonth.sortedByDescending { it.date }.take(5),
                    monthLabel = monthName,
                    loading = false
                )
            } catch (e: Exception) {
                Timber.e(e, "FinancialViewModel: failed to load")
                _state.value = _state.value.copy(loading = false)
            }
        }
    }
}
