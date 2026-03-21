package org.prauga.messages.feature.financial

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.android.AndroidInjection
import kotlinx.coroutines.launch
import org.prauga.messages.R
import org.prauga.messages.common.base.QkThemedActivity
import org.prauga.messages.common.widget.QkTextView
import org.prauga.messages.databinding.FinancialActivityBinding
import org.prauga.messages.financial.Transaction
import org.prauga.messages.financial.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class FinancialActivity : QkThemedActivity<FinancialActivityBinding>(FinancialActivityBinding::inflate) {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: FinancialViewModel

    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 0; maximumFractionDigits = 0
    }
    private val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.financial_title)

        viewModel = ViewModelProvider(this, viewModelFactory)[FinancialViewModel::class.java]

        // Cashiro deep link
        binding.cashiroCard.setOnClickListener {
            val intent = packageManager.getLaunchIntentForPackage("com.ritesh.cashiro")
                ?: Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("https://github.com/chaitanyaprem/Cashiro")
                }
            startActivity(intent)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: FinancialState) {
        if (state.loading) return

        binding.monthLabel.text = state.monthLabel

        val hasData = state.totalDebits > 0 || state.totalCredits > 0
        binding.emptyState.isVisible = !hasData

        binding.totalDebits.text = "₹${numberFormat.format(state.totalDebits)}"
        binding.totalCredits.text = "₹${numberFormat.format(state.totalCredits)}"

        // Top merchants
        binding.merchantsList.removeAllViews()
        if (state.topMerchants.isEmpty()) {
            binding.merchantsCard.isVisible = false
        } else {
            binding.merchantsCard.isVisible = true
            state.topMerchants.forEachIndexed { index, merchant ->
                val row = layoutInflater.inflate(
                    R.layout.financial_transaction_item, binding.merchantsList, false
                )
                row.findViewById<QkTextView>(R.id.merchant).text =
                    merchant.name.replaceFirstChar { it.uppercase() }
                row.findViewById<QkTextView>(R.id.account).text =
                    "${merchant.count} transaction${if (merchant.count > 1) "s" else ""}"
                row.findViewById<QkTextView>(R.id.amount).apply {
                    text = "₹${numberFormat.format(merchant.amount)}"
                    setTextColor(Color.parseColor("#E53935"))
                }
                row.findViewById<View>(R.id.typeDot).backgroundTintList =
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#E53935"))
                row.findViewById<QkTextView>(R.id.date).text = ""
                // Hide divider on last item
                if (index == state.topMerchants.lastIndex) {
                    row.findViewById<View?>(R.id.date)?.visibility = View.GONE
                }
                binding.merchantsList.addView(row)
            }
        }

        // Recent transactions
        binding.recentList.removeAllViews()
        state.recentTransactions.forEach { txn ->
            binding.recentList.addView(buildTransactionRow(txn))
        }
    }

    private fun buildTransactionRow(txn: Transaction): View {
        val row = layoutInflater.inflate(
            R.layout.financial_transaction_item, null, false
        )
        val isDebit = txn.type == TransactionType.DEBIT
        val isCredit = txn.type == TransactionType.CREDIT
        val color = when {
            isDebit -> Color.parseColor("#E53935")
            isCredit -> Color.parseColor("#43A047")
            else -> Color.GRAY
        }
        row.findViewById<View>(R.id.typeDot).backgroundTintList =
            android.content.res.ColorStateList.valueOf(color)
        row.findViewById<QkTextView>(R.id.merchant).text =
            txn.merchant?.replaceFirstChar { it.uppercase() } ?: "Unknown"
        row.findViewById<QkTextView>(R.id.account).apply {
            text = txn.accountLabel ?: ""
            isVisible = txn.accountLabel != null
        }
        val prefix = when { isDebit -> "−₹"; isCredit -> "+₹"; else -> "₹" }
        row.findViewById<QkTextView>(R.id.amount).apply {
            text = "$prefix${numberFormat.format(txn.amount)}"
            setTextColor(color)
        }
        row.findViewById<QkTextView>(R.id.date).text = dateFormat.format(Date(txn.date))
        return row
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
