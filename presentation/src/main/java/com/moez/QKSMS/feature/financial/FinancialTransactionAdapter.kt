package org.prauga.messages.feature.financial

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.prauga.messages.databinding.FinancialDateHeaderItemBinding
import org.prauga.messages.databinding.FinancialTransactionItemBinding
import org.prauga.messages.financial.Transaction
import org.prauga.messages.financial.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class FinancialListItem {
    data class Header(val dateLabel: String) : FinancialListItem()
    data class Item(val transaction: Transaction) : FinancialListItem()
}

class FinancialTransactionAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<FinancialListItem>()

    private val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    private val headerFormat = SimpleDateFormat("EEEE, dd MMM", Locale.getDefault())
    private val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }

    fun submitList(transactions: List<Transaction>) {
        items.clear()
        // Group by day and insert headers
        var lastDay = ""
        transactions.sortedByDescending { it.date }.forEach { txn ->
            val day = headerFormat.format(Date(txn.date))
            if (day != lastDay) {
                items.add(FinancialListItem.Header(day))
                lastDay = day
            }
            items.add(FinancialListItem.Item(txn))
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is FinancialListItem.Header -> VIEW_TYPE_HEADER
        is FinancialListItem.Item -> VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder(FinancialDateHeaderItemBinding.inflate(inflater, parent, false))
        } else {
            ItemViewHolder(FinancialTransactionItemBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is FinancialListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is FinancialListItem.Item -> (holder as ItemViewHolder).bind(item.transaction)
        }
    }

    override fun getItemCount() = items.size

    inner class HeaderViewHolder(private val binding: FinancialDateHeaderItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(header: FinancialListItem.Header) {
            binding.dateLabel.text = header.dateLabel
        }
    }

    inner class ItemViewHolder(private val binding: FinancialTransactionItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            val isDebit = transaction.type == TransactionType.DEBIT
            val isCredit = transaction.type == TransactionType.CREDIT
            val color = when {
                isDebit -> Color.parseColor("#E53935")
                isCredit -> Color.parseColor("#43A047")
                else -> Color.GRAY
            }
            binding.typeDot.backgroundTintList = ColorStateList.valueOf(color)
            binding.merchant.text = transaction.merchant
                ?.replaceFirstChar { it.uppercase() } ?: "Unknown"
            binding.account.text = transaction.accountLabel ?: ""
            binding.account.visibility = if (transaction.accountLabel != null) View.VISIBLE else View.GONE
            val prefix = when {
                isDebit -> "−₹"; isCredit -> "+₹"; else -> "₹"
            }
            binding.amount.text = "$prefix${numberFormat.format(transaction.amount)}"
            binding.amount.setTextColor(color)
            binding.date.text = dateFormat.format(Date(transaction.date))
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }
}
