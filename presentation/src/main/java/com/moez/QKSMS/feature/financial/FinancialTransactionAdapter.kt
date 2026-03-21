package org.prauga.messages.feature.financial

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.prauga.messages.databinding.FinancialTransactionItemBinding
import org.prauga.messages.financial.Transaction
import org.prauga.messages.financial.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FinancialTransactionAdapter : RecyclerView.Adapter<FinancialTransactionAdapter.ViewHolder>() {

    private val items = mutableListOf<Transaction>()
    private val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    fun submitList(transactions: List<Transaction>) {
        items.clear()
        items.addAll(transactions)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FinancialTransactionItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: FinancialTransactionItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            val isDebit = transaction.type == TransactionType.DEBIT
            val isCredit = transaction.type == TransactionType.CREDIT
            val color = when {
                isDebit -> Color.parseColor("#E53935")
                isCredit -> Color.parseColor("#43A047")
                else -> Color.GRAY
            }

            binding.typeDot.backgroundTintList =
                android.content.res.ColorStateList.valueOf(color)

            binding.merchant.text = transaction.merchant
                ?.replaceFirstChar { it.uppercase() }
                ?: "Unknown"

            binding.account.text = transaction.account
                ?.let { "••••$it" }
                ?: ""

            val prefix = when {
                isDebit -> "−₹"
                isCredit -> "+₹"
                else -> "₹"
            }
            binding.amount.text = "$prefix${numberFormat.format(transaction.amount)}"
            binding.amount.setTextColor(color)

            binding.date.text = dateFormat.format(Date(transaction.date))
        }
    }
}
