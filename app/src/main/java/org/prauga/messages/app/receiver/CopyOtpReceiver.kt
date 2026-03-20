package org.prauga.messages.app.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import org.prauga.messages.app.R

class CopyOtpReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val otpCode = intent.getStringExtra("otpCode") ?: return

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("OTP", otpCode)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, context.getString(R.string.otp_copied, otpCode), Toast.LENGTH_SHORT)
            .show()

        // Auto-dismiss the OTP notification after copying
        val threadId = intent.getLongExtra("threadId", -1L)
        if (threadId != -1L) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(threadId.toInt())
        }
    }
}
