package pt.isec.amov.cityfix.utils

import android.location.Location
import pt.isec.amov.cityfix.model.GeoLocation

object DistanceUtils {
    val PRACA_REPUBLICA_COIMBRA = GeoLocation(40.2096, -8.4193)

    const val ANONYMOUS_RADIUS_METERS = 10_000f
    const val GROUP_RADIUS_METERS = 10f
    const val OPERATOR_PROXIMITY_METERS = 20f

    fun distanceMeters(a: GeoLocation, b: GeoLocation): Float {
        val results = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
        return results[0]
    }

    fun isWithinRadius(
        a: GeoLocation?,
        b: GeoLocation?,
        radiusMeters: Float
    ): Boolean {
        if (a == null || b == null) return false
        return distanceMeters(a, b) <= radiusMeters
    }

    fun isWithinAnonymousRadius(location: GeoLocation?): Boolean {
        return isWithinRadius(location, PRACA_REPUBLICA_COIMBRA, ANONYMOUS_RADIUS_METERS)
    }
}
