package org.prauga.messages.interactor

import org.prauga.messages.repository.ConversationRepository
import org.prauga.messages.repository.MessageRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Deletes OTP messages older than [OTP_MIN_AGE_HOURS] hours on every app start.
 * OTPs expire quickly and have no long-term value — keeping them just adds noise.
 */
class DeleteOldOtpMessages @Inject constructor(
    private val messageRepo: MessageRepository,
    private val conversationRepo: ConversationRepository
) {
    companion object {
        const val OTP_MIN_AGE_HOURS = 24
    }

    fun execute() {
        try {
            val ids = messageRepo.getOtpMessageIds(OTP_MIN_AGE_HOURS)
            if (ids.isEmpty()) {
                Timber.d("DeleteOldOtpMessages: no old OTPs to delete")
                return
            }
            Timber.d("DeleteOldOtpMessages: deleting ${ids.size} OTP messages older than ${OTP_MIN_AGE_HOURS}h")
            messageRepo.deleteMessages(ids)
            // Refresh conversation snippets for any threads that were affected
            conversationRepo.updateConversations()
            Timber.d("DeleteOldOtpMessages: done")
        } catch (e: Exception) {
            Timber.e(e, "DeleteOldOtpMessages: failed")
        }
    }
}
