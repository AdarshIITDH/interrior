package com.example.model

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Mathematical engine for image perspective calibration and 3D-to-wall mapping.
 */
object PhotoCalibrationEngine {

    /**
     * Maps a point on normalized wall space (-0.5..0.5, -0.5..0.5)
     * to a pixel position on the image quadrilateral defined by 4 corner points.
     */
    fun mapWallToImagePoint(
        u: Float, // -0.5 (left) to 0.5 (right)
        v: Float, // -0.5 (top) to 0.5 (bottom)
        calibration: SiteCalibration,
        imageWidthPx: Float,
        imageHeightPx: Float
    ): Offset {
        // Bilinear quadrilateral interpolation
        val normU = (u + 0.5f).coerceIn(0f, 1f)
        val normV = (v + 0.5f).coerceIn(0f, 1f)

        val tlX = calibration.topLeft.x * imageWidthPx
        val tlY = calibration.topLeft.y * imageHeightPx

        val trX = calibration.topRight.x * imageWidthPx
        val trY = calibration.topRight.y * imageHeightPx

        val brX = calibration.bottomRight.x * imageWidthPx
        val brY = calibration.bottomRight.y * imageHeightPx

        val blX = calibration.bottomLeft.x * imageWidthPx
        val blY = calibration.bottomLeft.y * imageHeightPx

        // Top edge point at normU
        val topX = tlX + (trX - tlX) * normU
        val topY = tlY + (trY - tlY) * normU

        // Bottom edge point at normU
        val bottomX = blX + (brX - blX) * normU
        val bottomY = blY + (brY - blY) * normU

        // Vertical interpolation along normV
        val finalX = topX + (bottomX - topX) * normV
        val finalY = topY + (bottomY - topY) * normV

        return Offset(finalX, finalY)
    }

    /**
     * Calculates pixel-to-meter scale factor from 4 calibrated points and real reference width.
     */
    fun calculateScaleFactor(
        calibration: SiteCalibration,
        imageWidthPx: Float,
        imageHeightPx: Float
    ): Float {
        val blX = calibration.bottomLeft.x * imageWidthPx
        val blY = calibration.bottomLeft.y * imageHeightPx
        val brX = calibration.bottomRight.x * imageWidthPx
        val brY = calibration.bottomRight.y * imageHeightPx

        val pixelDist = sqrt((brX - blX) * (brX - blX) + (brY - blY) * (brY - blY))
        val realWidthMeters = (calibration.referenceWallWidthCm / 100f).coerceAtLeast(0.5f)

        return pixelDist / realWidthMeters // pixels per meter
    }
}
