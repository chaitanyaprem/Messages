package org.prauga.messages.feature.financial

import android.os.Bundle
import android.view.MenuItem
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import dagger.android.AndroidInjection
import kotlinx.coroutines.launch
import org.prauga.messages.R
import org.prauga.messages.common.base.QkThemedActivity
import org.prauga.messages.databinding.FinancialActivityBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class FinancialActivity : QkThemedActivity<FinancialActivityBinding>(FinancialActivityBinding::inflate) {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: FinancialViewModel
    private val adapter = FinancialTransactionAdapter()
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.financial_title)

        binding.transactionList.adapter = adapter

        viewModel = ViewModelProvider(this, viewModelFactory)[FinancialViewModel::class.java]

        binding.prevMonth.setOnClickListener { viewModel.previousMonth() }
        binding.nextMonth.setOnClickListener { viewModel.nextMonth() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: FinancialState) {
        // Month label
        binding.monthLabel.text = monthFormat.format(state.selectedMonth.time)

        // Disable next if already current month
        val now = java.util.Calendar.getInstance()
        binding.nextMonth.isEnabled = state.selectedMonth.get(java.util.Calendar.YEAR) < now.get(java.util.Calendar.YEAR) ||
                state.selectedMonth.get(java.util.Calendar.MONTH) < now.get(java.util.Calendar.MONTH)
        binding.nextMonth.alpha = if (binding.nextMonth.isEnabled) 1f else 0.3f

        // Summary
        binding.totalDebits.text = "₹${numberFormat.format(state.totalDebits)}"
        binding.totalCredits.text = "₹${numberFormat.format(state.totalCredits)}"

        // Account chips — rebuild only when groups change
        val chipGroup = binding.accountChipGroup
        if (chipGroup.childCount - 1 != state.accountGroups.size) {
            chipGroup.removeAllViews()

            // "All" chip
            val allChip = Chip(this).apply {
                text = "All"
                isCheckable = true
                isChecked = state.selectedAccount == null
                setOnClickListener { viewModel.selectAccount(null) }
            }
            chipGroup.addView(allChip)

            state.accountGroups.forEach { group ->
                val chip = Chip(this).apply {
                    text = "${group.label} (${group.transactionCount})"
                    isCheckable = true
                    isChecked = state.selectedAccount == group.label
                    setOnClickListener { viewModel.selectAccount(group.label) }
                }
                chipGroup.addView(chip)
            }
        } else {
            // Just update checked state
            (chipGroup.getChildAt(0) as? Chip)?.isChecked = state.selectedAccount == null
            state.accountGroups.forEachIndexed { i, group ->
                (chipGroup.getChildAt(i + 1) as? Chip)?.isChecked = state.selectedAccount == group.label
            }
        }

        binding.accountScrollView.isVisible = state.accountGroups.isNotEmpty()

        // Transactions
        adapter.submitList(state.filteredTransactions)
        binding.transactionList.isVisible = state.filteredTransactions.isNotEmpty() && !state.loading
        binding.emptyState.isVisible = state.filteredTransactions.isEmpty() && !state.loading
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
