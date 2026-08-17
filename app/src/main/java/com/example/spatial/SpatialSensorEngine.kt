package com.example.spatial

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2

data class DeviceOrientationState(
    val pitch: Float = 0f, // vertical tilt in radians
    val roll: Float = 0f,  // sideways tilt in radians
    val yaw: Float = 0f,   // compass azimuth in radians
    val rotationMatrix: FloatArray = FloatArray(16) { if (it % 5 == 0) 1f else 0f },
    val isSensorActive: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DeviceOrientationState
        if (pitch != other.pitch) return false
        if (roll != other.roll) return false
        if (yaw != other.yaw) return false
        if (!rotationMatrix.contentEquals(other.rotationMatrix)) return false
        if (isSensorActive != other.isSensorActive) return false
        return true
    }

    override fun hashCode(): Int {
        var result = pitch.hashCode()
        result = 31 * result + roll.hashCode()
        result = 31 * result + yaw.hashCode()
        result = 31 * result + rotationMatrix.contentHashCode()
        result = 31 * result + isSensorActive.hashCode()
        return result
    }
}

class SpatialSensorEngine(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

    private val _orientationState = MutableStateFlow(DeviceOrientationState())
    val orientationState: StateFlow<DeviceOrientationState> = _orientationState.asStateFlow()

    private val rawMatrix = FloatArray(16)
    private val remappedMatrix = FloatArray(16)
    private val orientationAngles = FloatArray(3)

    // Low-pass filter smoothing coefficient (0.0 to 1.0)
    private var smoothedPitch = 0f
    private var smoothedRoll = 0f
    private var smoothedYaw = 0f
    private val smoothingFactor = 0.15f

    fun startListening() {
        if (sensorManager != null && rotationSensor != null) {
            sensorManager.registerListener(
                this,
                rotationSensor,
                SensorManager.SENSOR_DELAY_GAME
            )
            _orientationState.value = _orientationState.value.copy(isSensorActive = true)
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
        _orientationState.value = _orientationState.value.copy(isSensorActive = false)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR ||
            event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR
        ) {
            SensorManager.getRotationMatrixFromVector(rawMatrix, event.values)
            // Remap coordinates for standard camera portrait orientation:
            SensorManager.remapCoordinateSystem(
                rawMatrix,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z,
                remappedMatrix
            )
            SensorManager.getOrientation(remappedMatrix, orientationAngles)

            val rawYaw = orientationAngles[0]
            val rawPitch = orientationAngles[1]
            val rawRoll = orientationAngles[2]

            // Apply low pass filter for rock-solid stability
            smoothedYaw += smoothingFactor * (rawYaw - smoothedYaw)
            smoothedPitch += smoothingFactor * (rawPitch - smoothedPitch)
            smoothedRoll += smoothingFactor * (rawRoll - smoothedRoll)

            _orientationState.value = DeviceOrientationState(
                pitch = smoothedPitch,
                roll = smoothedRoll,
                yaw = smoothedYaw,
                rotationMatrix = remappedMatrix.clone(),
                isSensorActive = true
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
