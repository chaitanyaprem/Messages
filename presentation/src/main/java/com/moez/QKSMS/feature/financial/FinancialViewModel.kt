package org.prauga.messages.feature.financial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.prauga.messages.financial.Transaction
import org.prauga.messages.repository.FinancialRepository
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

data class FinancialState(
    val transactions: List<Transaction> = emptyList(),
    val totalDebits: Double = 0.0,
    val totalCredits: Double = 0.0,
    val loading: Boolean = true
)

class FinancialViewModel @Inject constructor(
    private val financialRepository: FinancialRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FinancialState())
    val state: StateFlow<FinancialState> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val startOfMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val transactions = financialRepository.getTransactions(100)
                val debits = financialRepository.getTotalDebits(startOfMonth)
                val credits = financialRepository.getTotalCredits(startOfMonth)

                _state.value = FinancialState(
                    transactions = transactions,
                    totalDebits = debits,
                    totalCredits = credits,
                    loading = false
                )
            } catch (e: Exception) {
                Timber.e(e, "FinancialViewModel: failed to load")
                _state.value = _state.value.copy(loading = false)
            }
        }
    }
}
