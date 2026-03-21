package org.prauga.messages.feature.financial

import android.os.Bundle
import android.view.MenuItem

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.android.AndroidInjection
import kotlinx.coroutines.launch
import org.prauga.messages.databinding.FinancialActivityBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class FinancialActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FinancialActivityBinding
    private lateinit var viewModel: FinancialViewModel
    private val adapter = FinancialTransactionAdapter()

    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        binding = FinancialActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(org.prauga.messages.R.string.financial_title)

        binding.transactionList.adapter = adapter

        viewModel = ViewModelProvider(this, viewModelFactory)[FinancialViewModel::class.java]

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: FinancialState) {
        val monthLabel = monthFormat.format(Calendar.getInstance().time)
        binding.summaryPeriod.text = monthLabel

        binding.totalDebits.text = "₹${numberFormat.format(state.totalDebits)}"
        binding.totalCredits.text = "₹${numberFormat.format(state.totalCredits)}"

        adapter.submitList(state.transactions)

        binding.transactionList.isVisible = state.transactions.isNotEmpty() && !state.loading
        binding.emptyState.isVisible = state.transactions.isEmpty() && !state.loading
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
