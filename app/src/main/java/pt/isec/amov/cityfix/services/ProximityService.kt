package pt.isec.amov.cityfix.services

import android.Manifest
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.CityFixApp
import pt.isec.amov.cityfix.R as AppR
import pt.isec.amov.cityfix.model.GeoLocation
import pt.isec.amov.cityfix.model.Occurrence
import pt.isec.amov.cityfix.ui.components.displayLabel
import pt.isec.amov.cityfix.utils.DistanceUtils

class ProximityService : Service() {

    private val locationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    private val repository by lazy {
        (applicationContext as CityFixApp).occurrenceRepository
    }
    private val notifiedOccurrences = mutableSetOf<String>()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { checkProximity(it) }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_menu_mylocation)
            .setContentTitle(getString(AppR.string.notif_proximity_active_title))
            .setContentText(getString(AppR.string.notif_proximity_active_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(FOREGROUND_ID, notification)
        startLocationUpdates()
    }

    @RequiresPermission(
        anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION]
    )
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000)
            .setMinUpdateIntervalMillis(10000)
            .build()
        try {
            locationClient.requestLocationUpdates(request, locationCallback, mainLooper)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun checkProximity(userLoc: Location) = serviceScope.launch {
        val currentLocation = GeoLocation(userLoc.latitude, userLoc.longitude)
        repository.loadOccurrences().getActive().forEach { occurrence ->
            val occLoc = occurrence.location ?: return@forEach
            val distance = DistanceUtils.distanceMeters(currentLocation, occLoc)
            Log.i("AMOV", "Distance to occurrence ${occurrence.id}: ${distance}m")

            if (distance <= DistanceUtils.OPERATOR_PROXIMITY_METERS) {
                if (notifiedOccurrences.add(occurrence.id)) {
                    sendNotification(occurrence, distance)
                }
            } else {
                notifiedOccurrences.remove(occurrence.id)
            }
        }
    }

    private fun sendNotification(occurrence: Occurrence, distance: Float) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(getString(AppR.string.notif_proximity_alert_title))
            .setContentText(
                getString(
                    AppR.string.notif_proximity_alert_text,
                    occurrence.situation.displayLabel(this),
                    distance.toInt()
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(occurrence.id.hashCode(), notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(AppR.string.notif_proximity_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "cityfix_proximity_channel"
        private const val FOREGROUND_ID = 1337
    }
}
