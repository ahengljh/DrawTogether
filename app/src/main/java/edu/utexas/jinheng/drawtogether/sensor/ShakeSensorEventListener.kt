package edu.utexas.jinheng.drawtogether.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeSensorEventListener : SensorEventListener {

    private var mListener: OnShakeListener? = null
    private var mShakeTimestamp: Long = 0
    private var mShakeCount: Int = 0

    fun setOnShakeListener(listener: OnShakeListener?) {
        this.mListener = listener
    }

    interface OnShakeListener {
        fun onShake(count: Int)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // ignore
    }

    override fun onSensorChanged(event: SensorEvent?) {
        mListener?.let { listener ->
            event?.let {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]

                val gX = x / SensorManager.GRAVITY_EARTH
                val gY = y / SensorManager.GRAVITY_EARTH
                val gZ = z / SensorManager.GRAVITY_EARTH

                // gForce will be close to 1 when there is no movement.
                val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

                if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                    val now = System.currentTimeMillis()

                    // ignore shake events too close to each other (500ms)
                    if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                        return
                    }

                    // reset the shake count after 3 seconds of no shakes
                    if (mShakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                        mShakeCount = 0
                    }

                    mShakeTimestamp = now
                    mShakeCount++

                    listener.onShake(mShakeCount)
                }
            }
        }
    }

    companion object {
        private const val SHAKE_THRESHOLD_GRAVITY = 2.7f
        private const val SHAKE_SLOP_TIME_MS = 500
        private const val SHAKE_COUNT_RESET_TIME_MS = 3000
    }
}