package com.example.prayer.sensor

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Sensor Manager for reading device orientation (Compass Azimuth) smoothly
 * with sensor fusion (Rotation Vector / Accelerometer + Magnetometer) and calibration status.
 */
class CompassSensorManager(context: Context) : SensorEventListener {

    private val appContext: Context = context.applicationContext
    private val sensorManager: SensorManager? = try {
        appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    } catch (e: Exception) {
        null
    }

    private var rotationVectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var magnetometerSensor: Sensor? = null

    private val _azimuthFlow = MutableStateFlow(0f)
    val azimuthFlow: StateFlow<Float> = _azimuthFlow.asStateFlow()

    private val _sensorAccuracyFlow = MutableStateFlow(SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
    val sensorAccuracyFlow: StateFlow<Int> = _sensorAccuracyFlow.asStateFlow()

    private val _isSensorAvailableFlow = MutableStateFlow(true)
    val isSensorAvailableFlow: StateFlow<Boolean> = _isSensorAvailableFlow.asStateFlow()

    private var gravityValues = FloatArray(3)
    private var geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private var smoothedAzimuth = 0f
    private val alpha = 0.15f // Low pass filter factor (0.0 to 1.0)
    private var geomagneticField: GeomagneticField? = null

    init {
        try {
            rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            if (rotationVectorSensor == null) {
                accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                magnetometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
                _isSensorAvailableFlow.value = accelerometerSensor != null && magnetometerSensor != null
            } else {
                _isSensorAvailableFlow.value = true
            }
        } catch (e: Exception) {
            _isSensorAvailableFlow.value = false
        }
    }

    fun setLocation(latitude: Double, longitude: Double, altitude: Double = 0.0) {
        try {
            geomagneticField = GeomagneticField(
                latitude.toFloat(),
                longitude.toFloat(),
                altitude.toFloat(),
                System.currentTimeMillis()
            )
        } catch (e: Exception) {
            geomagneticField = null
        }
    }

    fun startListening() {
        try {
            val sm = sensorManager ?: return
            if (rotationVectorSensor != null) {
                sm.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
            } else {
                accelerometerSensor?.let {
                    sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                }
                magnetometerSensor?.let {
                    sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                }
            }
        } catch (e: Exception) {
            // Ignore if sensors fail to bind
        }
    }

    fun stopListening() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        var azimuthDeg = 0f
        var hasNewValue = false

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val rad = orientation[0]
            azimuthDeg = Math.toDegrees(rad.toDouble()).toFloat()
            hasNewValue = true
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravityValues, 0, 3)
            hasGravity = true
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagneticValues, 0, 3)
            hasGeomagnetic = true
        }

        if (!hasNewValue && hasGravity && hasGeomagnetic) {
            val r = FloatArray(9)
            val i = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, i, gravityValues, geomagneticValues)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)
                val rad = orientation[0]
                azimuthDeg = Math.toDegrees(rad.toDouble()).toFloat()
                hasNewValue = true
            }
        }

        if (hasNewValue) {
            // Apply True North declination correction if available
            val declination = geomagneticField?.declination ?: 0f
            azimuthDeg += declination

            // Normalize to [0, 360)
            azimuthDeg = (azimuthDeg + 360f) % 360f

            // Smooth using circular low-pass filter
            smoothedAzimuth = smoothAngle(smoothedAzimuth, azimuthDeg, alpha)
            _azimuthFlow.value = smoothedAzimuth
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        _sensorAccuracyFlow.value = accuracy
    }

    private fun smoothAngle(current: Float, target: Float, factor: Float): Float {
        var diff = target - current
        while (diff < -180f) diff += 360f
        while (diff > 180f) diff -= 360f
        val newAngle = current + diff * factor
        return (newAngle + 360f) % 360f
    }
}
