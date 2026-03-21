package org.prauga.messages.interactor

import org.prauga.messages.categorization.CategorizationEngine
import org.prauga.messages.model.Message
import org.prauga.messages.repository.MessageRepository
import timber.log.Timber
import javax.inject.Inject

class BackfillMessageCategories @Inject constructor(
    private val messageRepo: MessageRepository,
    private val categorizationEngine: CategorizationEngine
) {
    fun execute() {
        try {
            val uncategorized = messageRepo.getUncategorizedMessages()
            if (uncategorized.isEmpty()) {
                Timber.d("BackfillMessageCategories: nothing to backfill")
                return
            }

            Timber.d("BackfillMessageCategories: backfilling ${uncategorized.size} messages")

            // Build all updates in memory first, then write in a single Realm transaction
            val updates = mutableMapOf<Long, Message.MessageCategory>()
            uncategorized.forEach { (id, address, body) ->
                val category = categorizationEngine.categorize(address, body)
                if (category != Message.MessageCategory.UNKNOWN) {
                    updates[id] = category
                }
            }

            if (updates.isNotEmpty()) {
                messageRepo.updateMessageCategories(updates)
                Timber.d("BackfillMessageCategories: categorized ${updates.size}/${uncategorized.size} messages")
            } else {
                Timber.d("BackfillMessageCategories: no messages matched a category")
            }
        } catch (e: Exception) {
            Timber.e(e, "BackfillMessageCategories: failed")
        }
    }
}
