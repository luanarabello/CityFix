package pt.isec.amov.cityfix.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    fun showNotification(
        context: Context,
        channelId: String,
        channelName: String,
        notificationId: Int,
        title: String,
        body: String,
        smallIcon: Int = android.R.drawable.ic_dialog_info
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.i("AMOV NOTIF", "Notification sent [$channelId]: $title - $body")
        } catch (e: SecurityException) {
            Log.e("AMOV NOTIF", "Missing POST_NOTIFICATIONS permission", e)
        }
    }
}
