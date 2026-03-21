package org.prauga.messages.feature.financial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.prauga.messages.financial.AccountType
import org.prauga.messages.financial.Transaction
import org.prauga.messages.financial.TransactionType
import org.prauga.messages.repository.FinancialRepository
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

data class AccountGroup(
    val label: String,           // "HDFC ••••4321" or "UPI" or "Unknown"
    val accountType: AccountType,
    val totalDebits: Double,
    val totalCredits: Double,
    val transactionCount: Int
)

data class FinancialState(
    val allTransactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val accountGroups: List<AccountGroup> = emptyList(),
    val selectedAccount: String? = null,  // null = all accounts
    val totalDebits: Double = 0.0,
    val totalCredits: Double = 0.0,
    val selectedMonth: Calendar = Calendar.getInstance(),
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
                val all = financialRepository.getTransactions(500)
                val state = _state.value
                _state.value = buildState(all, state.selectedMonth, state.selectedAccount)
            } catch (e: Exception) {
                Timber.e(e, "FinancialViewModel: failed to load")
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    fun previousMonth() {
        val cal = _state.value.selectedMonth.clone() as Calendar
        cal.add(Calendar.MONTH, -1)
        applyMonth(cal)
    }

    fun nextMonth() {
        val cal = _state.value.selectedMonth.clone() as Calendar
        cal.add(Calendar.MONTH, 1)
        // Don't go into the future
        if (cal.after(Calendar.getInstance())) return
        applyMonth(cal)
    }

    fun selectAccount(label: String?) {
        val s = _state.value
        _state.value = buildState(s.allTransactions, s.selectedMonth, label)
    }

    private fun applyMonth(cal: Calendar) {
        val s = _state.value
        _state.value = buildState(s.allTransactions, cal, s.selectedAccount)
    }

    private fun buildState(
        all: List<Transaction>,
        month: Calendar,
        selectedAccount: String?
    ): FinancialState {
        // Month boundaries
        val start = (month.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = (month.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val monthTxns = all.filter { it.date in start..end }

        // Build account groups from ALL month transactions
        val accountGroups = monthTxns
            .groupBy { it.accountLabel ?: "Unknown" }
            .map { (label, txns) ->
                AccountGroup(
                    label = label,
                    accountType = txns.first().accountType,
                    totalDebits = txns.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount },
                    totalCredits = txns.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount },
                    transactionCount = txns.size
                )
            }
            .sortedByDescending { it.totalDebits + it.totalCredits }

        // Filter by selected account
        val filtered = if (selectedAccount == null) monthTxns
        else monthTxns.filter { (it.accountLabel ?: "Unknown") == selectedAccount }

        val totalDebits = filtered.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
        val totalCredits = filtered.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }

        return FinancialState(
            allTransactions = all,
            filteredTransactions = filtered,
            accountGroups = accountGroups,
            selectedAccount = selectedAccount,
            totalDebits = totalDebits,
            totalCredits = totalCredits,
            selectedMonth = month,
            loading = false
        )
    }
}
