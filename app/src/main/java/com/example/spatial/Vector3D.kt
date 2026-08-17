package com.example.spatial

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vector3D(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vector3D) = Vector3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3D) = Vector3D(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3D(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vector3D(x / scalar, y / scalar, z / scalar)

    fun dot(other: Vector3D): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3D): Vector3D = Vector3D(
        x = y * other.z - z * other.y,
        y = z * other.x - x * other.z,
        z = x * other.y - y * other.x
    )

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalized(): Vector3D {
        val len = length()
        return if (len > 0.00001f) this / len else Vector3D(0f, 0f, 1f)
    }

    companion object {
        val ZERO = Vector3D(0f, 0f, 0f)
        val UP = Vector3D(0f, -1f, 0f)
        val FORWARD = Vector3D(0f, 0f, 1f)
        val RIGHT = Vector3D(1f, 0f, 0f)
    }
}

class Matrix4x4(val values: FloatArray = FloatArray(16) { if (it % 5 == 0) 1f else 0f }) {

    operator fun times(other: Matrix4x4): Matrix4x4 {
        val result = FloatArray(16)
        for (row in 0..3) {
            for (col in 0..3) {
                var sum = 0f
                for (k in 0..3) {
                    sum += this.values[row * 4 + k] * other.values[k * 4 + col]
                }
                result[row * 4 + col] = sum
            }
        }
        return Matrix4x4(result)
    }

    fun transformPoint(v: Vector3D): Vector3D {
        val x = values[0] * v.x + values[1] * v.y + values[2] * v.z + values[3]
        val y = values[4] * v.x + values[5] * v.y + values[6] * v.z + values[7]
        val z = values[8] * v.x + values[9] * v.y + values[10] * v.z + values[11]
        val w = values[12] * v.x + values[13] * v.y + values[14] * v.z + values[15]
        return if (w != 0f && w != 1f) {
            Vector3D(x / w, y / w, z / w)
        } else {
            Vector3D(x, y, z)
        }
    }

    companion object {
        fun identity(): Matrix4x4 = Matrix4x4()

        fun translation(tx: Float, ty: Float, tz: Float): Matrix4x4 {
            val m = Matrix4x4()
            m.values[3] = tx
            m.values[7] = ty
            m.values[11] = tz
            return m
        }

        fun rotationY(angleRad: Float): Matrix4x4 {
            val m = Matrix4x4()
            val c = cos(angleRad)
            val s = sin(angleRad)
            m.values[0] = c
            m.values[2] = s
            m.values[8] = -s
            m.values[10] = c
            return m
        }

        fun rotationX(angleRad: Float): Matrix4x4 {
            val m = Matrix4x4()
            val c = cos(angleRad)
            val s = sin(angleRad)
            m.values[5] = c
            m.values[6] = -s
            m.values[9] = s
            m.values[10] = c
            return m
        }

        fun rotationZ(angleRad: Float): Matrix4x4 {
            val m = Matrix4x4()
            val c = cos(angleRad)
            val s = sin(angleRad)
            m.values[0] = c
            m.values[1] = -s
            m.values[4] = s
            m.values[5] = c
            return m
        }

        fun scale(sx: Float, sy: Float, sz: Float): Matrix4x4 {
            val m = Matrix4x4()
            m.values[0] = sx
            m.values[5] = sy
            m.values[10] = sz
            return m
        }
    }
}
