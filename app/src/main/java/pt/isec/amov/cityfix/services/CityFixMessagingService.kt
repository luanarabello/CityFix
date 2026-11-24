package pt.isec.amov.cityfix.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.utils.NotificationHelper

class CityFixMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM registration token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: getString(R.string.app_name)

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: getString(R.string.notif_info_title)

        NotificationHelper.showNotification(
            context = this,
            channelId = "fcm_notifications",
            channelName = getString(R.string.notif_status_channel_name),
            notificationId = System.currentTimeMillis().toInt(),
            title = title,
            body = body
        )
    }

    private companion object {
        const val TAG = "AMOV: FCM"
    }
}
