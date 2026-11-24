package pt.isec.amov.cityfix.services.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import kotlin.math.abs

/**
 * Detects abrupt movement on the accelerometer's Z axis.
 * The initial threshold and cooldown are configurable and should be tuned
 * with real devices and road conditions.
 */
class ImpactDetector(
    context: Context,
    private val thresholdMetersPerSecondSquared: Float = DEFAULT_THRESHOLD_METERS_PER_SECOND_SQUARED,
    private val cooldownMillis: Long = DEFAULT_COOLDOWN_MILLIS,
    private val onImpactDetected: () -> Unit
) : SensorEventListener {
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var filteredGravityZ: Float? = null
    private var lastImpactTimestampNanos = 0L
    private var isListening = false

    fun start(): Boolean {
        if (isListening) return true
        val sensor = accelerometer ?: return false
        isListening = sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_GAME
        )
        return isListening
    }

    fun stop() {
        if (isListening) {
            sensorManager.unregisterListener(this)
            isListening = false
        }
        filteredGravityZ = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER || event.values.size < 3) return

        val zAcceleration = event.values[2]
        val previousGravity = filteredGravityZ
        if (previousGravity == null) {
            filteredGravityZ = zAcceleration
            return
        }

        val updatedGravity = GRAVITY_FILTER_ALPHA * previousGravity +
            (1f - GRAVITY_FILTER_ALPHA) * zAcceleration
        filteredGravityZ = updatedGravity

        // Remove the slowly changing gravity component; keep sudden Z-axis changes.
        val verticalAcceleration = abs(zAcceleration - updatedGravity)
        val cooldownNanos = cooldownMillis * NANOS_PER_MILLISECOND
        val cooldownElapsed = lastImpactTimestampNanos == 0L ||
            event.timestamp - lastImpactTimestampNanos >= cooldownNanos

        if (verticalAcceleration >= thresholdMetersPerSecondSquared && cooldownElapsed) {
            lastImpactTimestampNanos = event.timestamp
            mainHandler.post { onImpactDetected() }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        // Provisional value: tune with device testing; the assignment gives no threshold.
        const val DEFAULT_THRESHOLD_METERS_PER_SECOND_SQUARED = 8f
        const val DEFAULT_COOLDOWN_MILLIS = 10_000L
        private const val GRAVITY_FILTER_ALPHA = 0.8f
        private const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
