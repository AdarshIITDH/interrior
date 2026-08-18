package com.example.model

import java.text.NumberFormat
import java.util.Locale

/**
 * Measurement confidence level for site photos and AR scans.
 */
enum class MeasurementConfidence(
    val title: String,
    val badgeLabel: String,
    val isFabricationReady: Boolean
) {
    VISUAL_ONLY(
        title = "Visual Placement",
        badgeLabel = "Visual only",
        isFabricationReady = false
    ),
    MANUALLY_CALIBRATED(
        title = "Calibrated Space",
        badgeLabel = "Calibrated",
        isFabricationReady = false
    ),
    AR_CALIBRATED(
        title = "AR Spatial Scan",
        badgeLabel = "AR measured",
        isFabricationReady = false
    ),
    USER_VERIFIED(
        title = "Site Dimensions Verified",
        badgeLabel = "Verified ✓",
        isFabricationReady = true
    )
}

/**
 * Spatial calibration metadata saved during temporary AR camera capture.
 */
data class SiteCaptureCalibration(
    val imageWidthPx: Int = 1920,
    val imageHeightPx: Int = 1080,
    val focalLengthX: Float? = 1450f,
    val focalLengthY: Float? = 1450f,
    val principalPointX: Float? = 960f,
    val principalPointY: Float? = 540f,
    val cameraPoseMatrix: FloatArray? = null,
    val wallPlanePoseMatrix: FloatArray? = null,
    val floorPlanePoseMatrix: FloatArray? = null,
    val wallWidthMeters: Float? = 3.2f,
    val wallHeightMeters: Float? = 2.6f,
    val trackingQuality: String = "GOOD",
    val captureTimestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SiteCaptureCalibration

        if (imageWidthPx != other.imageWidthPx) return false
        if (imageHeightPx != other.imageHeightPx) return false
        if (focalLengthX != other.focalLengthX) return false
        if (focalLengthY != other.focalLengthY) return false
        if (cameraPoseMatrix != null) {
            if (other.cameraPoseMatrix == null) return false
            if (!cameraPoseMatrix.contentEquals(other.cameraPoseMatrix)) return false
        } else if (other.cameraPoseMatrix != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageWidthPx
        result = 31 * result + imageHeightPx
        result = 31 * result + (focalLengthX?.hashCode() ?: 0)
        result = 31 * result + (cameraPoseMatrix?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * 2D normalized point on image [0..1]
 */
data class ImagePoint(val x: Float, val y: Float)

/**
 * Planar wall calibration from 4 selected corner points.
 */
data class SiteCalibration(
    val topLeft: ImagePoint = ImagePoint(0.15f, 0.15f),
    val topRight: ImagePoint = ImagePoint(0.85f, 0.15f),
    val bottomRight: ImagePoint = ImagePoint(0.85f, 0.85f),
    val bottomLeft: ImagePoint = ImagePoint(0.15f, 0.85f),
    val referenceWallWidthCm: Float = 300f,
    val referenceWallHeightCm: Float = 240f,
    val referenceWallDepthCm: Float = 60f,
    val isCalibrated: Boolean = false,
    val verifiedAt: Long? = null
)

/**
 * Site Photo Capture representation.
 */
data class SiteCapture(
    val imagePath: String? = null,
    val imageWidthPx: Int = 1920,
    val imageHeightPx: Int = 1080,
    val captureTimestamp: Long = System.currentTimeMillis(),
    val calibration: SiteCaptureCalibration? = null,
    val manualCalibration: SiteCalibration = SiteCalibration(),
    val confidence: MeasurementConfidence = MeasurementConfidence.VISUAL_ONLY,
    val brightness: Float = 1.0f,
    val shadowIntensity: Float = 0.5f,
    val warmth: Float = 1.0f
)

/**
 * User-configurable pricing parameters for estimates.
 */
data class PricingConfiguration(
    val labourRatePerSqFt: Double = WardrobeCostRates.LABOUR_RATE_PER_SQFT,
    val installationCharge: Double = 3500.0,
    val transportationCharge: Double = 1200.0,
    val discountAmount: Double = 0.0,
    val isGstEnabled: Boolean = false,
    val gstPercent: Double = 18.0,
    val otherCharges: Double = 0.0
)

/**
 * Comprehensive authorative model for an entire VisionSpace Wardrobe Project.
 */
data class WardrobeProject(
    val id: String = "proj_${System.currentTimeMillis()}",
    val name: String = "Bedroom Wardrobe",
    val roomName: String = "Master Bedroom",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val siteCapture: SiteCapture = SiteCapture(),
    val wardrobeConfig: WardrobeConfig = WardrobeConfig(),
    val pricingConfig: PricingConfiguration = PricingConfiguration(),
    val siteOffsetNormalizedX: Float = 0.5f,
    val siteOffsetNormalizedY: Float = 0.7f,
    val siteScaleMultiplier: Float = 1.0f,
    val siteYawRotationDeg: Float = 0.0f,
    val sitePerspectiveTiltDeg: Float = 0.0f,
    val verifiedSiteWidthInches: Float? = null,
    val verifiedSiteHeightInches: Float? = null,
    val verifiedSiteDepthInches: Float? = null
) {
    val overallWidthInches: Float get() = wardrobeConfig.widthCm / 2.54f
    val overallHeightInches: Float get() = wardrobeConfig.heightCm / 2.54f
    val overallDepthInches: Float get() = wardrobeConfig.depthCm / 2.54f

    val formattedOverallDimensionsFtIn: String
        get() = DimensionFormatter.formatDimensions(
            wCm = wardrobeConfig.widthCm,
            hCm = wardrobeConfig.heightCm,
            dCm = wardrobeConfig.depthCm,
            unitSystem = UnitSystem.FEET_INCHES
        )
}
