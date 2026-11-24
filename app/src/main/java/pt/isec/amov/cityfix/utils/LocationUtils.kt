package pt.isec.amov.cityfix.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import pt.isec.amov.cityfix.model.GeoLocation
import java.util.Locale
import kotlin.coroutines.resume

object LocationUtils {
    suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String = suspendCancellableCoroutine { continuation->
        val geocoder = Geocoder(context, Locale.getDefault())
        val defaultResult = "Lat: %.4f, Lng: %.4f".format(lat, lng)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            // API 33+ (Modern asynchronous implementation)
            geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    val addressLine= addresses.firstOrNull()?.getAddressLine(0) ?: defaultResult
                    if (continuation.isActive) {
                        continuation.resume(addressLine)
                    }
                }
                override fun onError(errorMessage: String?) {
                    if (continuation.isActive) {
                        continuation.resume(defaultResult)
                    }
                }
            })
        } else {

            // Versions prior to API 33 (Synchronous implementation with deprecation suppression)
            @Suppress("DEPRECATION")
            try {
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                val addressLine= addresses?.firstOrNull()?.getAddressLine(0) ?: defaultResult
                continuation.resume(addressLine)
            } catch (e: Exception) {
                e.printStackTrace()
                continuation.resume(defaultResult)
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): GeoLocation? = suspendCancellableCoroutine{ continuation->
        val client = LocationServices.getFusedLocationProviderClient(context)
        val tokenSource = CancellationTokenSource()
        continuation.invokeOnCancellation { tokenSource.cancel() } // cancela o GPS se o ecrã fechar

        try{
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                .addOnSuccessListener { location ->
                    if (continuation.isActive) {
                        continuation.resume(location?.let { GeoLocation(it.latitude, it.longitude) })
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        } catch(e: SecurityException){
            if (continuation.isActive) continuation.resume(null)
        }
    }

    suspend fun geocodeAddress(context: Context, address: String): GeoLocation? {
        if (address.isBlank()) return null
        return suspendCancellableCoroutine { continuation ->
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationName(address, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        val loc = addresses.firstOrNull()?.let {
                            GeoLocation(it.latitude, it.longitude)
                        }
                        if (continuation.isActive) {
                            continuation.resume(loc)
                        }
                    }

                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                })
            } else {
                @Suppress("DEPRECATION")
                try {
                    val addresses = geocoder.getFromLocationName(address, 1)
                    val loc = addresses?.firstOrNull()?.let {
                        GeoLocation(it.latitude, it.longitude)
                    }
                    if (continuation.isActive) {
                        continuation.resume(loc)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        }
    }
}