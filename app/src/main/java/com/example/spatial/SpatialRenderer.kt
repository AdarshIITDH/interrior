package com.example.spatial

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import com.example.model.DoorStyle
import com.example.model.DimensionFormatter
import com.example.model.FinishType
import com.example.model.LedLighting
import com.example.model.UnitSystem
import com.example.model.WardrobeConfig
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldLaser
import com.example.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.sin

data class ProjectedPolygon(
    val points: List<Offset>,
    val averageDepth: Float,
    val fillColor: Color,
    val strokeColor: Color = Color.Transparent,
    val strokeWidth: Float = 0f
)

data class ARPlacementState(
    val distanceMeters: Float = 2.4f,
    val elevationOffsetMeters: Float = -0.1f,
    val lateralOffsetMeters: Float = 0.0f,
    val userRotationYDeg: Float = 0f,
    val scaleMultiplier: Float = 1.0f,
    val showDimensions: Boolean = true,
    val showFloorGrid: Boolean = true,
    val showDoorClearanceArc: Boolean = true,
    val isWallSnapped: Boolean = false,
    val isPlaced: Boolean = true
)

object SpatialRenderer {

    /**
     * Projects 3D world space coordinate (meters) to 2D screen coordinate (pixels)
     * using pinhole perspective projection model.
     */
    fun project3DTo2D(
        point: Vector3D,
        screenWidth: Float,
        screenHeight: Float,
        fovFocalLength: Float = 600f
    ): Offset? {
        if (point.z <= 0.15f) return null

        val screenCenterX = screenWidth / 2f
        val screenCenterY = screenHeight / 2f

        val projectedX = screenCenterX + (point.x / point.z) * fovFocalLength
        val projectedY = screenCenterY + (point.y / point.z) * fovFocalLength

        return Offset(projectedX, projectedY)
    }

    /**
     * Renders a Realistic 3D Bespoke Wooden Wardrobe in AR Space.
     */
    fun renderWardrobeScene(
        drawScope: DrawScope,
        config: WardrobeConfig,
        placement: ARPlacementState,
        deviceOrientation: DeviceOrientationState,
        textMeasurer: TextMeasurer,
        animationTick: Float = 0f,
        unitSystem: UnitSystem = UnitSystem.FEET_INCHES
    ) {
        val widthPx = drawScope.size.width
        val heightPx = drawScope.size.height
        val fovFocalLength = widthPx * 1.15f

        // Convert wardrobe dimensions from cm to meters with scale multiplier
        val w = (config.widthCm / 100f) * placement.scaleMultiplier
        val h = (config.heightCm / 100f) * placement.scaleMultiplier
        val d = (config.depthCm / 100f) * placement.scaleMultiplier

        val halfW = w / 2f
        val halfH = h / 2f
        val halfD = d / 2f

        // Combined device orientation + manual user rotation
        val combinedYaw = (Math.toRadians(placement.userRotationYDeg.toDouble()) + deviceOrientation.yaw).toFloat()
        val combinedPitch = (deviceOrientation.pitch * 0.35f)
        val combinedRoll = (deviceOrientation.roll * 0.25f)

        // Translation to 3D world position
        val worldOrigin = Vector3D(
            x = placement.lateralOffsetMeters,
            y = placement.elevationOffsetMeters,
            z = placement.distanceMeters
        )

        // Transformation Matrix
        val rotY = Matrix4x4.rotationY(combinedYaw)
        val rotX = Matrix4x4.rotationX(combinedPitch)
        val rotZ = Matrix4x4.rotationZ(combinedRoll)
        val modelMatrix = rotY * rotX * rotZ

        fun toWorld(lx: Float, ly: Float, lz: Float): Vector3D {
            val rotated = modelMatrix.transformPoint(Vector3D(lx, ly, lz))
            return worldOrigin + rotated
        }

        // 1. Draw Holographic Floor AR Grid if enabled
        if (placement.showFloorGrid) {
            drawHolographicFloorGrid(
                drawScope = drawScope,
                worldOrigin = worldOrigin,
                modelMatrix = modelMatrix,
                widthM = w,
                depthM = d,
                screenWidth = widthPx,
                screenHeight = heightPx,
                fov = fovFocalLength,
                animTick = animationTick
            )
        }

        // 2. Draw Realistic Soft Contact Ground Shadow
        drawContactShadow(
            drawScope = drawScope,
            worldOrigin = worldOrigin,
            modelMatrix = modelMatrix,
            halfW = halfW,
            halfD = halfD,
            halfH = halfH,
            screenWidth = widthPx,
            screenHeight = heightPx,
            fov = fovFocalLength
        )

        // 3. Draw Door Clearance Radius Arc on Floor if doors open
        if (placement.showDoorClearanceArc && config.doorStyle != DoorStyle.OPEN_CONCEPT && config.doorOpenRatio > 0.05f) {
            drawDoorClearanceZone(
                drawScope = drawScope,
                worldOrigin = worldOrigin,
                modelMatrix = modelMatrix,
                halfW = halfW,
                halfH = halfH,
                halfD = halfD,
                clearanceCm = config.requiredFloorClearanceCm,
                screenWidth = widthPx,
                screenHeight = heightPx,
                fov = fovFocalLength
            )
        }

        // 4. Generate 3D Polygons for Realistic Bespoke Furniture
        val polygons = mutableListOf<ProjectedPolygon>()

        val baseFinishColor = config.finish.primaryColor
        val shadowColor = config.finish.secondaryColor
        val highlightColor = config.finish.highlightColor
        val grainColor = config.finish.grainColor
        val isWood = config.finish.isWood
        val isGlass = config.finish.isGlass

        // Architectural reveal lines & carcass colors
        val seamColor = if (isWood) grainColor.copy(alpha = 0.85f) else Color(0xFF1E2024).copy(alpha = 0.70f)
        val carcassInteriorColor = baseFinishColor

        // Carcass Wall Thickness (25mm gables)
        val gableThick = 0.025f
        val plinthHeight = 0.08f * placement.scaleMultiplier // 8cm recessed base
        val carcassBottomY = halfH - plinthHeight
        val carcassTopY = -halfH + 0.035f // 35mm top fascia

        // ==========================================
        // A. CARCASS EXTERIOR & STRUCTURAL BOX
        // ==========================================

        // 1. Carcass Back Wall (Local Z = -halfD)
        val backColor = if (isGlass) shadowColor.copy(alpha = 0.40f) else shadowColor
        addQuad(
            polygons,
            toWorld(-halfW, -halfH, -halfD),
            toWorld(halfW, -halfH, -halfD),
            toWorld(halfW, halfH, -halfD),
            toWorld(-halfW, halfH, -halfD),
            backColor,
            strokeColor = seamColor,
            strokeWidth = 1f,
            screenWidth = widthPx,
            screenHeight = heightPx,
            fov = fovFocalLength
        )

        // Back Wall Vertical Timber Planks Texture
        if (isWood) {
            val backPlanks = 6
            val plankW = w / backPlanks
            for (p in 1 until backPlanks) {
                val px = -halfW + p * plankW
                val pTop = toWorld(px, -halfH, -halfD + 0.005f)
                val pBtm = toWorld(px, halfH, -halfD + 0.005f)
                val s1 = project3DTo2D(pTop, widthPx, heightPx, fovFocalLength)
                val s2 = project3DTo2D(pBtm, widthPx, heightPx, fovFocalLength)
                if (s1 != null && s2 != null) {
                    polygons.add(
                        ProjectedPolygon(
                            points = listOf(s1, s2),
                            averageDepth = (pTop.z + pBtm.z) / 2f,
                            fillColor = Color.Transparent,
                            strokeColor = grainColor.copy(alpha = 0.45f),
                            strokeWidth = 1.2f
                        )
                    )
                }
            }
        }

        // 2. Carcass Left Outer Gable (25mm solid wood panel)
        val leftPanelColor = if (isGlass) baseFinishColor.copy(alpha = 0.50f) else shadowColor
        addQuad(
            polygons,
            toWorld(-halfW, -halfH, -halfD),
            toWorld(-halfW, -halfH, halfD),
            toWorld(-halfW, halfH, halfD),
            toWorld(-halfW, halfH, -halfD),
            leftPanelColor,
            strokeColor = seamColor,
            strokeWidth = 1.4f,
            screenWidth = widthPx,
            screenHeight = heightPx,
            fov = fovFocalLength
        )

        // Left Gable Front Edge-Banding
        addQuad(
            polygons,
            toWorld(-halfW, -halfH, halfD),
            toWorld(-halfW + gableThick, -halfH, halfD),
            toWorld(-halfW + gableThick, carcassBottomY, halfD),
            toWorld(-halfW, carcassBottomY, halfD),
            highlightColor,
            strokeColor = seamColor,
            strokeWidth = 1f,
            screenWidth = widthPx,
            screenHeight = heightPx,
            fov = fovFocalLength
        )

        // Wood grain stripes on Left Panel
        if (isWood) {
            val grainSteps = 4
            for (g in 1..grainSteps) {
                val zPos = -halfD + (g * (d / (grainSteps + 1)))
                val gTop = toWorld(-halfW - 0.002f, -halfH, zPos)
                val gBtm = toWorld(-halfW - 0.002f, halfH, zPos)
                val s1 = project3DTo2D(gTop, widthPx, heightPx, fovFocalLength)
                val s2 = project3DTo2D(gBtm, widthPx, heightPx, fovFocalLength)
                if (s1 != null && s2 != null) {
                    polygons.add(
                        ProjectedPolygon(
                            points = listOf(s1, s2),
                            averageDepth = (gTop.z + gBtm.z) / 2f,
                            fillColor = Color.Transparent,
                            strokeColor = grainColor.copy(alpha = 0.35f),
                            strokeWidth = 1f
                        )
                    )
                }
            }
        }

        // 3. Carcass Right Outer Gable (25mm solid wood panel)
        val rightPanelColor = if (isGlass) baseFinishColor.copy(alpha = 0.50f) else baseFinishColor
        addQuad(
            polygons,
            toWorld(halfW, -halfH, -halfD),
            toWorld(halfW, -halfH, halfD),
            toWorld(halfW, halfH, halfD),
            toWorld(halfW, halfH, -halfD),
            rightPanelColor,
            strokeColor = seamColor,
            strokeWidth = 1.4f,
            screenWidth = widthPx,
            screenHeight = heightPx,
            fov = fovFocalLength
        )

        // Right Gable Front Edge-Banding
        addQuad(
            polygons,
            toWorld(halfW - gableThick, -halfH, halfD),
            toWorld(halfW, -halfH, halfD),
            toWorld(halfW, carcassBottomY, halfD),
            toWorld(halfW - gableThick, carcassBottomY, halfD),
            highlightColor,
            strokeColor = seamColor,
            strokeWidth = 1f,
            screenWidth = widthPx,
            screenHeight = heightPx,
            fov = fovFocalLength
        )

        // Wood grain stripes on Right Panel
        if (isWood) {
            val grainSteps = 4
            for (g in 1..grainSteps) {
                val zPos = -halfD + (g * (d / (grainSteps + 1)))
                val gTop = toWorld(halfW + 0.002f, -halfH, zPos)
                val gBtm = toWorld(halfW + 0.002f, halfH, zPos)
                val s1 = project3DTo2D(gTop, widthPx, heightPx, fovFocalLength)
                val s2 = project3DTo2D(gBtm, widthPx, heightPx, fovFocalLength)
                if (s1 != null && s2 != null) {
                    polygons.add(
                        ProjectedPolygon(
                            points = listOf(s1, s2),
                            averageDepth = (gTop.z + gBtm.z) / 2f,
                            fillColor = Color.Transparent,
                            strokeColor = grainColor.copy(alpha = 0.35f),
                            strokeWidth = 1f
                        )
                    )
                }
            }
        }

        // 4. Carcass Top Fascia / Roof Panel (Local Y = -halfH)
        val topColor = if (isGlass) highlightColor.copy(alpha = 0.60f) else highlightColor
        addQuad(
            polygons,
            toWorld(-halfW, -halfH, -halfD),
            toWorld(halfW, -halfH, -halfD),
            toWorld(halfW, -halfH, halfD),
            toWorld(-halfW, -halfH, halfD),
            topColor,
            strokeColor = seamColor,
            strokeWidth = 1.4f,
            screenWidth = widthPx,
            screenHeight = heightPx,
            fov = fovFocalLength
        )

        // Top Cornice Front Lip
        addQuad(
            polygons,
            toWorld(-halfW, -halfH, halfD),
            toWorld(halfW, -halfH, halfD),
            toWorld(halfW, carcassTopY, halfD),
            toWorld(-halfW, carcassTopY, halfD),
            baseFinishColor,
            strokeColor = seamColor,
            strokeWidth = 1.2f,
            screenWidth = widthPx,
            screenHeight = heightPx,
            fov = fovFocalLength
        )

        // 5. Carcass Bottom Floor Board (Local Y = carcassBottomY)
        addQuad(
            polygons,
            toWorld(-halfW, carcassBottomY, -halfD),
            toWorld(halfW, carcassBottomY, -halfD),
            toWorld(halfW, carcassBottomY, halfD),
            toWorld(-halfW, carcassBottomY, halfD),
            carcassInteriorColor,
            strokeColor = seamColor,
            strokeWidth = 1.2f,
            screenWidth = widthPx,
            screenHeight = heightPx,
            fov = fovFocalLength
        )

        // 6. Recessed Base Plinth / Kickplate (Recessed 3cm from front)
        addQuad(
            polygons,
            toWorld(-halfW * 0.98f, carcassBottomY, halfD * 0.92f),
            toWorld(halfW * 0.98f, carcassBottomY, halfD * 0.92f),
            toWorld(halfW * 0.98f, halfH, halfD * 0.92f),
            toWorld(-halfW * 0.98f, halfH, halfD * 0.92f),
            shadowColor,
            strokeColor = seamColor,
            strokeWidth = 1.2f,
            screenWidth = widthPx,
            screenHeight = heightPx,
            fov = fovFocalLength
        )

        // ==========================================
        // B. MODULAR INTERIOR ARCHITECTURE
        // ==========================================

        val usableInteriorLeft = -halfW + gableThick
        val usableInteriorRight = halfW - gableThick

        // Top Loft Overhead Shelf (holds storage bins)
        val loftShelfY = -halfH + 0.35f * (h / 2.4f)
        addQuad(
            polygons,
            toWorld(usableInteriorLeft, loftShelfY, -halfD * 0.95f),
            toWorld(usableInteriorRight, loftShelfY, -halfD * 0.95f),
            toWorld(usableInteriorRight, loftShelfY, halfD * 0.88f),
            toWorld(usableInteriorLeft, loftShelfY, halfD * 0.88f),
            highlightColor,
            strokeColor = seamColor,
            strokeWidth = 1f,
            screenWidth = widthPx,
            screenHeight = heightPx,
            fov = fovFocalLength
        )

        // Loft Shelf Front Edge Banding
        addQuad(
            polygons,
            toWorld(usableInteriorLeft, loftShelfY, halfD * 0.88f),
            toWorld(usableInteriorRight, loftShelfY, halfD * 0.88f),
            toWorld(usableInteriorRight, loftShelfY + 0.02f, halfD * 0.88f),
            toWorld(usableInteriorLeft, loftShelfY + 0.02f, halfD * 0.88f),
            baseFinishColor,
            strokeColor = seamColor,
            strokeWidth = 0.8f,
            screenWidth = widthPx,
            screenHeight = heightPx,
            fov = fovFocalLength
        )

        // Fabric Storage Bins on top loft shelf
        val binCount = if (config.widthCm >= 200f) 3 else 2
        val binSpacing = (usableInteriorRight - usableInteriorLeft) / binCount
        for (b in 0 until binCount) {
            val binCenterX = usableInteriorLeft + (b * binSpacing) + (binSpacing * 0.5f)
            val binW = (binSpacing * 0.78f) / 2f
            val binH = 0.20f
            val binY = loftShelfY - binH
            val binColor = if (b % 2 == 0) Color(0xFFD4C5B9) else Color(0xFF8C827A)

            // Front Bin Face
            addQuad(
                polygons,
                toWorld(binCenterX - binW, binY, halfD * 0.5f),
                toWorld(binCenterX + binW, binY, halfD * 0.5f),
                toWorld(binCenterX + binW, loftShelfY, halfD * 0.5f),
                toWorld(binCenterX - binW, loftShelfY, halfD * 0.5f),
                binColor,
                strokeColor = Color(0xFF5A524C),
                strokeWidth = 1f,
                screenWidth = widthPx,
                screenHeight = heightPx,
                fov = fovFocalLength
            )

            // Bin Leather Handle
            val bhLeft = toWorld(binCenterX - 0.04f, loftShelfY - binH * 0.45f, halfD * 0.52f)
            val bhRight = toWorld(binCenterX + 0.04f, loftShelfY - binH * 0.45f, halfD * 0.52f)
            val bp1 = project3DTo2D(bhLeft, widthPx, heightPx, fovFocalLength)
            val bp2 = project3DTo2D(bhRight, widthPx, heightPx, fovFocalLength)
            if (bp1 != null && bp2 != null) {
                polygons.add(
                    ProjectedPolygon(
                        points = listOf(bp1, bp2),
                        averageDepth = (bhLeft.z + bhRight.z) / 2f,
                        fillColor = Color.Transparent,
                        strokeColor = Color(0xFF3E2723),
                        strokeWidth = 3f
                    )
                )
            }
        }

        // Modular Internal Vertical Partition Divider (creates 2 or 3 bays)
        val bayCount = if (config.widthCm >= 240f) 3 else 2
        val dividerXPositions = if (bayCount == 3) {
            listOf(-halfW * 0.33f, halfW * 0.33f)
        } else {
            listOf(0f)
        }

        for (divX in dividerXPositions) {
            // Divider Side Wall
            addQuad(
                polygons,
                toWorld(divX, loftShelfY, -halfD * 0.95f),
                toWorld(divX, loftShelfY, halfD * 0.88f),
                toWorld(divX, carcassBottomY, halfD * 0.88f),
                toWorld(divX, carcassBottomY, -halfD * 0.95f),
                baseFinishColor,
                strokeColor = seamColor,
                strokeWidth = 1f,
                screenWidth = widthPx,
                screenHeight = heightPx,
                fov = fovFocalLength
            )

            // Divider Front Edge Banding
            addQuad(
                polygons,
                toWorld(divX - 0.01f, loftShelfY, halfD * 0.88f),
                toWorld(divX + 0.01f, loftShelfY, halfD * 0.88f),
                toWorld(divX + 0.01f, carcassBottomY, halfD * 0.88f),
                toWorld(divX - 0.01f, carcassBottomY, halfD * 0.88f),
                highlightColor,
                strokeColor = seamColor,
                strokeWidth = 0.8f,
                screenWidth = widthPx,
                screenHeight = heightPx,
                fov = fovFocalLength
            )
        }

        // ==========================================
        // C. SHELVES, ACCESSORIES & DRAWERS TOWER (RIGHT BAY)
        // ==========================================

        val shelfBayLeft = if (bayCount == 3) halfW * 0.33f + 0.01f else 0.01f
        val shelfBayRight = usableInteriorRight
        val shelfCount = config.shelvesCount.coerceIn(1, 6)
        val shelfAvailableHeight = carcassBottomY - loftShelfY
        val shelfSpacing = shelfAvailableHeight / (shelfCount + 1)

        // Drawers at bottom of shelf bay
        val drawerCount = config.drawersCount.coerceIn(0, 5)
        val drawerTotalHeight = if (drawerCount > 0) (carcassBottomY - loftShelfY) * 0.42f else 0f
        val drawerStartY = carcassBottomY - drawerTotalHeight

        // Shelves placed above drawers
        val availableShelfHeight = (drawerStartY - loftShelfY)
        val effectiveShelfCount = if (drawerCount > 0) shelfCount.coerceIn(1, 4) else shelfCount
        val effectiveShelfSpacing = availableShelfHeight / (effectiveShelfCount + 1)

        for (i in 1..effectiveShelfCount) {
            val shelfY = loftShelfY + (i * effectiveShelfSpacing)
            addQuad(
                polygons,
                toWorld(shelfBayLeft, shelfY, -halfD * 0.94f),
                toWorld(shelfBayRight, shelfY, -halfD * 0.94f),
                toWorld(shelfBayRight, shelfY, halfD * 0.88f),
                toWorld(shelfBayLeft, shelfY, halfD * 0.88f),
                highlightColor,
                strokeColor = seamColor,
                strokeWidth = 0.8f,
                screenWidth = widthPx,
                screenHeight = heightPx,
                fov = fovFocalLength
            )

            // Front Edge-Banding on Shelves (visible wood veneer edge)
            addQuad(
                polygons,
                toWorld(shelfBayLeft, shelfY, halfD * 0.88f),
                toWorld(shelfBayRight, shelfY, halfD * 0.88f),
                toWorld(shelfBayRight, shelfY + 0.018f, halfD * 0.88f),
                toWorld(shelfBayLeft, shelfY + 0.018f, halfD * 0.88f),
                baseFinishColor,
                strokeColor = seamColor,
                strokeWidth = 0.8f,
                screenWidth = widthPx,
                screenHeight = heightPx,
                fov = fovFocalLength
            )

            // Folded clothing stack on shelves
            if (i in 1..2) {
                val stackCenterX = (shelfBayLeft + shelfBayRight) / 2f
                val stackW = (shelfBayRight - shelfBayLeft) * 0.32f
                val stackH = 0.07f
                val stackColor = if (i == 1) Color(0xFF4A5568) else Color(0xFFCBD5E1)
                addQuad(
                    polygons,
                    toWorld(stackCenterX - stackW, shelfY - stackH, halfD * 0.4f),
                    toWorld(stackCenterX + stackW, shelfY - stackH, halfD * 0.4f),
                    toWorld(stackCenterX + stackW, shelfY, halfD * 0.4f),
                    toWorld(stackCenterX - stackW, shelfY, halfD * 0.4f),
                    stackColor,
                    strokeColor = Color.Black.copy(alpha = 0.2f),
                    strokeWidth = 0.6f,
                    screenWidth = widthPx,
                    screenHeight = heightPx,
                    fov = fovFocalLength
                )

                // Fold seam line
                val f1 = toWorld(stackCenterX - stackW, shelfY - stackH * 0.5f, halfD * 0.405f)
                val f2 = toWorld(stackCenterX + stackW, shelfY - stackH * 0.5f, halfD * 0.405f)
                val sf1 = project3DTo2D(f1, widthPx, heightPx, fovFocalLength)
                val sf2 = project3DTo2D(f2, widthPx, heightPx, fovFocalLength)
                if (sf1 != null && sf2 != null) {
                    polygons.add(
                        ProjectedPolygon(
                            points = listOf(sf1, sf2),
                            averageDepth = (f1.z + f2.z) / 2f,
                            fillColor = Color.Transparent,
                            strokeColor = Color.Black.copy(alpha = 0.25f),
                            strokeWidth = 1f
                        )
                    )
                }
            }
        }

        // Modular Soft-Close Drawers
        if (drawerCount > 0) {
            val drawerHeight = drawerTotalHeight / drawerCount

            for (dIdx in 0 until drawerCount) {
                val dTopY = drawerStartY + (dIdx * drawerHeight) + 0.006f
                val dBottomY = dTopY + (drawerHeight * 0.90f)

                // Solid Drawer Front Face with Timber Grain
                val drawerColor = if (dIdx % 2 == 0) baseFinishColor else highlightColor
                addQuad(
                    polygons,
                    toWorld(shelfBayLeft + 0.01f, dTopY, halfD * 0.88f),
                    toWorld(shelfBayRight - 0.01f, dTopY, halfD * 0.88f),
                    toWorld(shelfBayRight - 0.01f, dBottomY, halfD * 0.88f),
                    toWorld(shelfBayLeft + 0.01f, dBottomY, halfD * 0.88f),
                    drawerColor,
                    strokeColor = seamColor,
                    strokeWidth = 1.2f,
                    screenWidth = widthPx,
                    screenHeight = heightPx,
                    fov = fovFocalLength
                )

                // Horizontal Wood Grain lines on drawer front
                if (isWood) {
                    val grainY = (dTopY + dBottomY) / 2f - 0.015f
                    val gStart = toWorld(shelfBayLeft + 0.03f, grainY, halfD * 0.885f)
                    val gEnd = toWorld(shelfBayRight - 0.03f, grainY, halfD * 0.885f)
                    val s1 = project3DTo2D(gStart, widthPx, heightPx, fovFocalLength)
                    val s2 = project3DTo2D(gEnd, widthPx, heightPx, fovFocalLength)
                    if (s1 != null && s2 != null) {
                        polygons.add(
                            ProjectedPolygon(
                                points = listOf(s1, s2),
                                averageDepth = (gStart.z + gEnd.z) / 2f,
                                fillColor = Color.Transparent,
                                strokeColor = grainColor.copy(alpha = 0.40f),
                                strokeWidth = 1f
                            )
                        )
                    }
                }

                // Sleek Brushed Brass / Matte Black Drawer Handle
                val handleY = (dTopY + dBottomY) / 2f
                val hMidX = (shelfBayLeft + shelfBayRight) / 2f
                val hLeft = toWorld(hMidX - 0.08f, handleY, halfD * 0.90f)
                val hRight = toWorld(hMidX + 0.08f, handleY, halfD * 0.90f)
                val p1 = project3DTo2D(hLeft, widthPx, heightPx, fovFocalLength)
                val p2 = project3DTo2D(hRight, widthPx, heightPx, fovFocalLength)
                if (p1 != null && p2 != null) {
                    polygons.add(
                        ProjectedPolygon(
                            points = listOf(p1, p2),
                            averageDepth = (hLeft.z + hRight.z) / 2f,
                            fillColor = Color.Transparent,
                            strokeColor = Color(0xFF1E2128),
                            strokeWidth = 3.5f
                        )
                    )
                }
            }
        }

        // ==========================================
        // D. HANGING RAILS & SUITS/GARMENTS (LEFT BAY)
        // ==========================================

        val hangingBayLeft = usableInteriorLeft
        val hangingBayRight = if (bayCount == 3) -halfW * 0.33f - 0.01f else -0.01f

        val railY = loftShelfY + 0.10f
        val railP1 = toWorld(hangingBayLeft, railY, 0f)
        val railP2 = toWorld(hangingBayRight, railY, 0f)
        val rp1 = project3DTo2D(railP1, widthPx, heightPx, fovFocalLength)
        val rp2 = project3DTo2D(railP2, widthPx, heightPx, fovFocalLength)
        if (rp1 != null && rp2 != null) {
            polygons.add(
                ProjectedPolygon(
                    points = listOf(rp1, rp2),
                    averageDepth = (railP1.z + railP2.z) / 2f,
                    fillColor = Color.Transparent,
                    strokeColor = Color(0xFFE2E8F0),
                    strokeWidth = 4.5f
                )
            )
        }

        // Hanging Garments (Blazers, Suits, Shirts)
        val garmentColors = listOf(
            Color(0xFF1E293B), // Navy Suit
            Color(0xFF8C532B), // Camel Coat
            Color(0xFF334155), // Charcoal Blazer
            Color(0xFFF8FAFC), // White Dress Shirt
            Color(0xFF475569)  // Slate Jacket
        )
        val clothesCount = ((hangingBayRight - hangingBayLeft) / 0.13f).toInt().coerceIn(2, 5)
        val clothesSpacing = (hangingBayRight - hangingBayLeft) / (clothesCount + 1)

        for (g in 1..clothesCount) {
            val gx = hangingBayLeft + (g * clothesSpacing)
            val gColor = garmentColors[g % garmentColors.size]
            val garmentLen = 0.52f // Blazer length

            // Triangular Wooden Coat Hanger
            val hangerTop = toWorld(gx, railY, 0f)
            val hangerLeft = toWorld(gx - 0.08f, railY + 0.04f, 0f)
            val hangerRight = toWorld(gx + 0.08f, railY + 0.04f, 0f)
            val hpTop = project3DTo2D(hangerTop, widthPx, heightPx, fovFocalLength)
            val hpLeft = project3DTo2D(hangerLeft, widthPx, heightPx, fovFocalLength)
            val hpRight = project3DTo2D(hangerRight, widthPx, heightPx, fovFocalLength)
            if (hpTop != null && hpLeft != null && hpRight != null) {
                polygons.add(
                    ProjectedPolygon(
                        points = listOf(hpLeft, hpTop, hpRight),
                        averageDepth = hangerTop.z,
                        fillColor = Color.Transparent,
                        strokeColor = Color(0xFFC4A076),
                        strokeWidth = 2.5f
                    )
                )
            }

            // Garment Silhouette with Shoulder Cut
            addQuad(
                polygons,
                toWorld(gx - 0.07f, railY + 0.05f, 0f),
                toWorld(gx + 0.07f, railY + 0.05f, 0f),
                toWorld(gx + 0.09f, railY + 0.05f + garmentLen, 0f),
                toWorld(gx - 0.09f, railY + 0.05f + garmentLen, 0f),
                gColor,
                strokeColor = Color.Black.copy(alpha = 0.35f),
                strokeWidth = 0.8f,
                screenWidth = widthPx,
                screenHeight = heightPx,
                fov = fovFocalLength
            )

            // Center Placket / Collar Line on Clothes
            val placketTop = toWorld(gx, railY + 0.05f, 0.01f)
            val placketBtm = toWorld(gx, railY + 0.05f + garmentLen, 0.01f)
            val pp1 = project3DTo2D(placketTop, widthPx, heightPx, fovFocalLength)
            val pp2 = project3DTo2D(placketBtm, widthPx, heightPx, fovFocalLength)
            if (pp1 != null && pp2 != null) {
                polygons.add(
                    ProjectedPolygon(
                        points = listOf(pp1, pp2),
                        averageDepth = placketTop.z,
                        fillColor = Color.Transparent,
                        strokeColor = Color.White.copy(alpha = 0.30f),
                        strokeWidth = 1f
                    )
                )
            }
        }

        // Bottom Shoe Tier Shelf in Left Bay
        val shoeShelfY = carcassBottomY - 0.16f
        addQuad(
            polygons,
            toWorld(hangingBayLeft, shoeShelfY, -halfD * 0.94f),
            toWorld(hangingBayRight, shoeShelfY, -halfD * 0.94f),
            toWorld(hangingBayRight, shoeShelfY, halfD * 0.88f),
            toWorld(hangingBayLeft, shoeShelfY, halfD * 0.88f),
            highlightColor,
            strokeColor = seamColor,
            strokeWidth = 0.8f,
            screenWidth = widthPx,
            screenHeight = heightPx,
            fov = fovFocalLength
        )

        // ==========================================
        // E. WARDROBE DOORS WITH PRECISE ROTATION & 3D VOLUME
        // ==========================================

        val openRatio = config.doorOpenRatio.coerceIn(0f, 1f)

        if (config.doorStyle != DoorStyle.OPEN_CONCEPT) {
            val isSliding = config.doorStyle == DoorStyle.SLIDING_BYPASS ||
                    config.doorStyle == DoorStyle.SLIDING_DOOR ||
                    config.doorStyle == DoorStyle.MIRROR_SLIDING_DOOR

            if (isSliding) {
                // Realistic Sliding Doors on Front/Rear Glide Channels
                val slideCount = if (config.widthCm >= 240f) 3 else 2
                val leafWidth = (w / slideCount) + 0.03f
                val maxSlideDist = (w / slideCount) * 0.85f
                val slideOffset = maxSlideDist * openRatio

                // Top & Bottom Aluminum Tracks
                addQuad(
                    polygons,
                    toWorld(-halfW, carcassTopY, halfD * 0.96f),
                    toWorld(halfW, carcassTopY, halfD * 0.96f),
                    toWorld(halfW, carcassTopY + 0.02f, halfD * 1.03f),
                    toWorld(-halfW, carcassTopY + 0.02f, halfD * 1.03f),
                    Color(0xFF64748B),
                    strokeColor = seamColor,
                    strokeWidth = 0.8f,
                    screenWidth = widthPx,
                    screenHeight = heightPx,
                    fov = fovFocalLength
                )

                for (s in 0 until slideCount) {
                    val baseDoorX = -halfW + (s * (w / slideCount))
                    val curX = if (s == 0) baseDoorX + slideOffset else baseDoorX
                    val zLayer = if (s % 2 == 0) halfD + 0.012f else halfD - 0.005f
                    val doorColor = if (config.doorStyle == DoorStyle.MIRROR_SLIDING_DOOR) {
                        Color(0xFFE0E7FF).copy(alpha = 0.85f)
                    } else if (s % 2 == 0) baseFinishColor else highlightColor

                    // Sliding Door Front Face
                    addQuad(
                        polygons,
                        toWorld(curX, carcassTopY, zLayer),
                        toWorld(curX + leafWidth, carcassTopY, zLayer),
                        toWorld(curX + leafWidth, carcassBottomY, zLayer),
                        toWorld(curX, carcassBottomY, zLayer),
                        doorColor,
                        strokeColor = seamColor,
                        strokeWidth = 1.4f,
                        screenWidth = widthPx,
                        screenHeight = heightPx,
                        fov = fovFocalLength
                    )

                    // Shaker Center Panel (if wood/textured)
                    if (config.doorStyle != DoorStyle.MIRROR_SLIDING_DOOR) {
                        val stileW = leafWidth * 0.12f
                        val railH = h * 0.06f
                        val innerDoorColor = if (isWood) shadowColor else highlightColor
                        addQuad(
                            polygons,
                            toWorld(curX + stileW, carcassTopY + railH, zLayer + 0.004f),
                            toWorld(curX + leafWidth - stileW, carcassTopY + railH, zLayer + 0.004f),
                            toWorld(curX + leafWidth - stileW, carcassBottomY - railH, zLayer + 0.004f),
                            toWorld(curX + stileW, carcassBottomY - railH, zLayer + 0.004f),
                            innerDoorColor,
                            strokeColor = seamColor,
                            strokeWidth = 1f,
                            screenWidth = widthPx,
                            screenHeight = heightPx,
                            fov = fovFocalLength
                        )

                        // Vertical Wood Grain Veins on Center Panel
                        if (isWood) {
                            drawOrganicWoodGrain(
                                polygons = polygons,
                                toWorld = ::toWorld,
                                leftX = curX + stileW,
                                rightX = curX + leafWidth - stileW,
                                topY = carcassTopY + railH,
                                bottomY = carcassBottomY - railH,
                                zPos = zLayer + 0.005f,
                                grainColor = grainColor,
                                widthPx = widthPx,
                                heightPx = heightPx,
                                fov = fovFocalLength
                            )
                        }
                    }

                    // Recessed Full-Height Metal Edge Pull Handle
                    val handleX = curX + (if (s % 2 == 0) leafWidth - 0.025f else 0.025f)
                    drawSolidVerticalHandle(
                        polygons = polygons,
                        toWorld = ::toWorld,
                        xPos = handleX,
                        yCenter = (carcassTopY + carcassBottomY) / 2f,
                        zPos = zLayer + 0.015f,
                        lengthM = h * 0.38f,
                        widthPx = widthPx,
                        heightPx = heightPx,
                        fov = fovFocalLength
                    )
                }

            } else {
                // ========================================================
                // REALISTIC 3D HINGED DOORS WITH FULL ROTATION GEOMETRY
                // ========================================================
                val doorLeafCount = if (config.widthCm > 240f) 4 else 2
                val doorLeafWidth = w / doorLeafCount
                val maxOpenAngle = Math.PI * 0.46 // ~83 degrees swing
                val doorAngle = (maxOpenAngle * openRatio).toFloat()
                val doorThick = 0.018f // 18mm door thickness

                for (dIdx in 0 until doorLeafCount) {
                    // Symmetrical pair arrangement:
                    // 2-door: Leaf 0 hinges at Left, Leaf 1 hinges at Right
                    // 4-door: Bay 1 (Leaf 0 Left, Leaf 1 Right), Bay 2 (Leaf 2 Left, Leaf 3 Right)
                    val isLeftHinged = if (doorLeafCount == 4) {
                        dIdx == 0 || dIdx == 2
                    } else {
                        dIdx == 0
                    }

                    val hingeX = if (doorLeafCount == 4) {
                        when (dIdx) {
                            0 -> -halfW
                            1 -> -halfW + 2 * doorLeafWidth
                            2 -> -halfW + 2 * doorLeafWidth
                            else -> halfW
                        }
                    } else {
                        if (isLeftHinged) -halfW else halfW
                    }

                    // swingSign: +1 if opening away to right, -1 if opening away to left
                    val swingSign = if (isLeftHinged) 1f else -1f

                    /**
                     * Exact 3D local coordinate transform for door point:
                     * u: distance along the door face (0 to doorLeafWidth)
                     * v: thickness offset perpendicular to door (0 = back face, doorThick = front face)
                     * y: vertical height coordinate
                     */
                    fun doorPoint(u: Float, y: Float, v: Float): Vector3D {
                        val cosA = cos(doorAngle)
                        val sinA = sin(doorAngle)

                        // Tangent vector along door face
                        val tx = swingSign * cosA
                        val tz = sinA

                        // Normal vector pointing outward from front face
                        val nx = -swingSign * sinA
                        val nz = cosA

                        val lx = hingeX + (u * tx) + (v * nx)
                        val lz = halfD + (u * tz) + (v * nz)
                        return toWorld(lx, y, lz)
                    }

                    val doorColor = if (dIdx % 2 == 0) baseFinishColor else highlightColor

                    // 1. Front Door Face (v = doorThick)
                    addQuad(
                        polygons,
                        doorPoint(0f, carcassTopY, doorThick),
                        doorPoint(doorLeafWidth, carcassTopY, doorThick),
                        doorPoint(doorLeafWidth, carcassBottomY, doorThick),
                        doorPoint(0f, carcassBottomY, doorThick),
                        doorColor,
                        strokeColor = seamColor,
                        strokeWidth = 1.4f,
                        screenWidth = widthPx,
                        screenHeight = heightPx,
                        fov = fovFocalLength
                    )

                    // 2. Back Door Face (v = 0f) - visible when door is open!
                    if (openRatio > 0.05f) {
                        addQuad(
                            polygons,
                            doorPoint(0f, carcassTopY, 0f),
                            doorPoint(doorLeafWidth, carcassTopY, 0f),
                            doorPoint(doorLeafWidth, carcassBottomY, 0f),
                            doorPoint(0f, carcassBottomY, 0f),
                            shadowColor,
                            strokeColor = seamColor,
                            strokeWidth = 1f,
                            screenWidth = widthPx,
                            screenHeight = heightPx,
                            fov = fovFocalLength
                        )
                    }

                    // 3. Leading Outer Edge of Door Leaf (u = doorLeafWidth)
                    addQuad(
                        polygons,
                        doorPoint(doorLeafWidth, carcassTopY, 0f),
                        doorPoint(doorLeafWidth, carcassTopY, doorThick),
                        doorPoint(doorLeafWidth, carcassBottomY, doorThick),
                        doorPoint(doorLeafWidth, carcassBottomY, 0f),
                        highlightColor,
                        strokeColor = seamColor,
                        strokeWidth = 1f,
                        screenWidth = widthPx,
                        screenHeight = heightPx,
                        fov = fovFocalLength
                    )

                    // 4. Shaker Recessed Beveled Panel on Front Face
                    val stileW = doorLeafWidth * 0.13f
                    val railH = h * 0.06f
                    val innerColor = if (isWood) shadowColor else highlightColor
                    addQuad(
                        polygons,
                        doorPoint(stileW, carcassTopY + railH, doorThick + 0.003f),
                        doorPoint(doorLeafWidth - stileW, carcassTopY + railH, doorThick + 0.003f),
                        doorPoint(doorLeafWidth - stileW, carcassBottomY - railH, doorThick + 0.003f),
                        doorPoint(stileW, carcassBottomY - railH, doorThick + 0.003f),
                        innerColor,
                        strokeColor = seamColor,
                        strokeWidth = 1f,
                        screenWidth = widthPx,
                        screenHeight = heightPx,
                        fov = fovFocalLength
                    )

                    // 5. Vertical Wood Grain Lines on Door Panel
                    if (isWood) {
                        for (g in 1..3) {
                            val ratio = g * 0.25f
                            val gu = stileW + (doorLeafWidth - 2 * stileW) * ratio
                            val gTop = doorPoint(gu, carcassTopY + railH + 0.02f, doorThick + 0.004f)
                            val gBtm = doorPoint(gu, carcassBottomY - railH - 0.02f, doorThick + 0.004f)
                            val s1 = project3DTo2D(gTop, widthPx, heightPx, fovFocalLength)
                            val s2 = project3DTo2D(gBtm, widthPx, heightPx, fovFocalLength)
                            if (s1 != null && s2 != null) {
                                polygons.add(
                                    ProjectedPolygon(
                                        points = listOf(s1, s2),
                                        averageDepth = (gTop.z + gBtm.z) / 2f,
                                        fillColor = Color.Transparent,
                                        strokeColor = grainColor.copy(alpha = 0.40f),
                                        strokeWidth = 1.1f
                                    )
                                )
                            }
                        }
                    }

                    // 6. Solid Vertical Handle Firmly Mounted on Front Face near opening edge
                    val handleU = doorLeafWidth - 0.04f
                    val handleLen = h * 0.35f
                    val handleMidY = (carcassTopY + carcassBottomY) / 2f
                    val hTop = doorPoint(handleU, handleMidY - handleLen / 2f, doorThick + 0.022f)
                    val hBtm = doorPoint(handleU, handleMidY + handleLen / 2f, doorThick + 0.022f)
                    val hp1 = project3DTo2D(hTop, widthPx, heightPx, fovFocalLength)
                    val hp2 = project3DTo2D(hBtm, widthPx, heightPx, fovFocalLength)
                    if (hp1 != null && hp2 != null) {
                        polygons.add(
                            ProjectedPolygon(
                                points = listOf(hp1, hp2),
                                averageDepth = (hTop.z + hBtm.z) / 2f,
                                fillColor = Color.Transparent,
                                strokeColor = Color(0xFF1E2128),
                                strokeWidth = 4.2f
                            )
                        )
                        // Handle highlight glint
                        polygons.add(
                            ProjectedPolygon(
                                points = listOf(hp1.copy(x = hp1.x + 1f), hp2.copy(x = hp2.x + 1f)),
                                averageDepth = (hTop.z + hBtm.z) / 2f,
                                fillColor = Color.Transparent,
                                strokeColor = Color(0xFFE2E8F0).copy(alpha = 0.75f),
                                strokeWidth = 1.5f
                            )
                        )
                    }

                    // 7. Soft-Close Concealed Metallic Hinges when doors open
                    if (openRatio > 0.15f) {
                        val hingeYPositions = listOf(
                            carcassTopY + 0.15f,
                            (carcassTopY + carcassBottomY) / 2f,
                            carcassBottomY - 0.15f
                        )
                        for (hy in hingeYPositions) {
                            val hngP1 = doorPoint(0f, hy, 0.005f)
                            val hngP2 = doorPoint(0.025f, hy, 0.005f)
                            val hs1 = project3DTo2D(hngP1, widthPx, heightPx, fovFocalLength)
                            val hs2 = project3DTo2D(hngP2, widthPx, heightPx, fovFocalLength)
                            if (hs1 != null && hs2 != null) {
                                polygons.add(
                                    ProjectedPolygon(
                                        points = listOf(hs1, hs2),
                                        averageDepth = (hngP1.z + hngP2.z) / 2f,
                                        fillColor = Color.Transparent,
                                        strokeColor = Color(0xFF94A3B8),
                                        strokeWidth = 3.5f
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // F. PAINTER'S ALGORITHM: SORT & RENDER POLYGONS
        // ==========================================

        // Sort polygons by Average Depth (furthest first, closest last)
        polygons.sortByDescending { it.averageDepth }

        for (poly in polygons) {
            if (poly.points.size < 2) continue

            val path = Path().apply {
                moveTo(poly.points[0].x, poly.points[0].y)
                for (p in 1 until poly.points.size) {
                    lineTo(poly.points[p].x, poly.points[p].y)
                }
                if (poly.points.size > 2) {
                    close()
                }
            }

            if (poly.fillColor != Color.Transparent && poly.points.size > 2) {
                drawScope.drawPath(path, poly.fillColor, style = Fill)
            }

            if (poly.strokeColor != Color.Transparent && poly.strokeWidth > 0f) {
                drawScope.drawPath(
                    path,
                    poly.strokeColor,
                    style = Stroke(width = poly.strokeWidth)
                )
            }
        }

        // 7. Integrated Ambient LED Glow Strip
        if (config.ledLighting != LedLighting.NONE && (config.doorOpenRatio > 0.1f || config.doorStyle == DoorStyle.OPEN_CONCEPT)) {
            val ledLightY = loftShelfY + 0.02f
            val ledP1 = project3DTo2D(toWorld(-halfW * 0.94f, ledLightY, 0f), widthPx, heightPx, fovFocalLength)
            val ledP2 = project3DTo2D(toWorld(halfW * 0.94f, ledLightY, 0f), widthPx, heightPx, fovFocalLength)

            if (ledP1 != null && ledP2 != null) {
                drawScope.drawLine(
                    color = config.ledLighting.color.copy(alpha = 0.90f),
                    start = ledP1,
                    end = ledP2,
                    strokeWidth = 4f
                )
                drawScope.drawLine(
                    color = config.ledLighting.color.copy(alpha = 0.25f),
                    start = ledP1,
                    end = ledP2,
                    strokeWidth = 14f
                )
            }
        }

        // 8. 3D Architectural Drafting Dimension Markers & Scale Verification
        if (placement.showDimensions) {
            draw3DDimensionHUD(
                drawScope = drawScope,
                toWorld = ::toWorld,
                halfW = halfW,
                halfH = halfH,
                halfD = halfD,
                config = config,
                screenWidth = widthPx,
                screenHeight = heightPx,
                fov = fovFocalLength,
                textMeasurer = textMeasurer,
                unitSystem = unitSystem
            )
        }
    }

    /**
     * Draws procedural organic wood grain with sinusoidal multi-frequency wave flow.
     */
    private fun drawOrganicWoodGrain(
        polygons: MutableList<ProjectedPolygon>,
        toWorld: (Float, Float, Float) -> Vector3D,
        leftX: Float,
        rightX: Float,
        topY: Float,
        bottomY: Float,
        zPos: Float,
        grainColor: Color,
        grainDensity: Int = 4,
        isVertical: Boolean = true,
        waveAmplitude: Float = 0.005f,
        widthPx: Float,
        heightPx: Float,
        fov: Float
    ) {
        for (g in 1..grainDensity) {
            val ratio = g.toFloat() / (grainDensity + 1)
            val segments = 7
            val pts = mutableListOf<Offset>()
            var totalZ = 0f

            for (s in 0..segments) {
                val t = s.toFloat() / segments
                val waveOffset = (sin(t * 8.5 + g * 1.7) * waveAmplitude + sin(t * 21.0 + g * 3.2) * (waveAmplitude * 0.45)).toFloat()

                val pt3D = if (isVertical) {
                    val curX = leftX + (rightX - leftX) * ratio + waveOffset
                    val curY = topY + (bottomY - topY) * t
                    toWorld(curX, curY, zPos)
                } else {
                    val curX = leftX + (rightX - leftX) * t
                    val curY = topY + (bottomY - topY) * ratio + waveOffset
                    toWorld(curX, curY, zPos)
                }

                val screenPt = project3DTo2D(pt3D, widthPx, heightPx, fov)
                if (screenPt != null) {
                    pts.add(screenPt)
                    totalZ += pt3D.z
                }
            }

            if (pts.size >= 2) {
                val alpha = if (g % 2 == 0) 0.48f else 0.30f
                polygons.add(
                    ProjectedPolygon(
                        points = pts,
                        averageDepth = totalZ / pts.size,
                        fillColor = Color.Transparent,
                        strokeColor = grainColor.copy(alpha = alpha),
                        strokeWidth = if (g % 2 == 0) 1.2f else 0.85f
                    )
                )
            }
        }
    }

    private fun drawSolidVerticalHandle(
        polygons: MutableList<ProjectedPolygon>,
        toWorld: (Float, Float, Float) -> Vector3D,
        xPos: Float,
        yCenter: Float,
        zPos: Float,
        lengthM: Float,
        widthPx: Float,
        heightPx: Float,
        fov: Float
    ) {
        val top3D = toWorld(xPos, yCenter - lengthM / 2f, zPos)
        val btm3D = toWorld(xPos, yCenter + lengthM / 2f, zPos)

        val p1 = project3DTo2D(top3D, widthPx, heightPx, fov)
        val p2 = project3DTo2D(btm3D, widthPx, heightPx, fov)
        if (p1 != null && p2 != null) {
            polygons.add(
                ProjectedPolygon(
                    points = listOf(p1, p2),
                    averageDepth = (top3D.z + btm3D.z) / 2f,
                    fillColor = Color.Transparent,
                    strokeColor = Color(0xFF1E2128),
                    strokeWidth = 4.5f
                )
            )
            polygons.add(
                ProjectedPolygon(
                    points = listOf(p1.copy(x = p1.x + 1f), p2.copy(x = p2.x + 1f)),
                    averageDepth = (top3D.z + btm3D.z) / 2f,
                    fillColor = Color.Transparent,
                    strokeColor = Color(0xFFCBD5E1).copy(alpha = 0.85f),
                    strokeWidth = 1.6f
                )
            )
        }
    }

    private fun addQuad(
        polygons: MutableList<ProjectedPolygon>,
        p1: Vector3D,
        p2: Vector3D,
        p3: Vector3D,
        p4: Vector3D,
        fillColor: Color,
        strokeColor: Color,
        strokeWidth: Float,
        screenWidth: Float,
        screenHeight: Float,
        fov: Float
    ) {
        val s1 = project3DTo2D(p1, screenWidth, screenHeight, fov) ?: return
        val s2 = project3DTo2D(p2, screenWidth, screenHeight, fov) ?: return
        val s3 = project3DTo2D(p3, screenWidth, screenHeight, fov) ?: return
        val s4 = project3DTo2D(p4, screenWidth, screenHeight, fov) ?: return

        val avgDepth = (p1.z + p2.z + p3.z + p4.z) / 4f
        polygons.add(
            ProjectedPolygon(
                points = listOf(s1, s2, s3, s4),
                averageDepth = avgDepth,
                fillColor = fillColor,
                strokeColor = strokeColor,
                strokeWidth = strokeWidth
            )
        )
    }

    private fun drawHolographicFloorGrid(
        drawScope: DrawScope,
        worldOrigin: Vector3D,
        modelMatrix: Matrix4x4,
        widthM: Float,
        depthM: Float,
        screenWidth: Float,
        screenHeight: Float,
        fov: Float,
        animTick: Float
    ) {
        val gridExtentM = 1.6f
        val stepM = 0.40f
        val floorY = worldOrigin.y + (depthM * 0.5f)

        val gridLines = mutableListOf<Pair<Offset, Offset>>()

        for (gx in -4..4) {
            val lx = gx * stepM
            val start3D = worldOrigin + modelMatrix.transformPoint(Vector3D(lx, floorY - worldOrigin.y, -gridExtentM))
            val end3D = worldOrigin + modelMatrix.transformPoint(Vector3D(lx, floorY - worldOrigin.y, gridExtentM))

            val s1 = project3DTo2D(start3D, screenWidth, screenHeight, fov)
            val s2 = project3DTo2D(end3D, screenWidth, screenHeight, fov)
            if (s1 != null && s2 != null) {
                gridLines.add(s1 to s2)
            }
        }

        for (gz in -4..4) {
            val lz = gz * stepM
            val start3D = worldOrigin + modelMatrix.transformPoint(Vector3D(-gridExtentM, floorY - worldOrigin.y, lz))
            val end3D = worldOrigin + modelMatrix.transformPoint(Vector3D(gridExtentM, floorY - worldOrigin.y, lz))

            val s1 = project3DTo2D(start3D, screenWidth, screenHeight, fov)
            val s2 = project3DTo2D(end3D, screenWidth, screenHeight, fov)
            if (s1 != null && s2 != null) {
                gridLines.add(s1 to s2)
            }
        }

        for ((start, end) in gridLines) {
            drawScope.drawLine(
                color = CyanGlow.copy(alpha = 0.20f),
                start = start,
                end = end,
                strokeWidth = 1f
            )
        }

        // Central AR Reticle Target Ring
        val centerFloor = project3DTo2D(Vector3D(worldOrigin.x, floorY, worldOrigin.z), screenWidth, screenHeight, fov)
        if (centerFloor != null) {
            val pulseRadius = 28f + (sin(animTick.toDouble()) * 3f).toFloat()
            drawScope.drawCircle(
                color = CyanNeon.copy(alpha = 0.4f),
                radius = pulseRadius,
                center = centerFloor,
                style = Stroke(width = 1.8f)
            )
            drawScope.drawCircle(
                color = CyanNeon,
                radius = 3.5f,
                center = centerFloor
            )
        }
    }

    private fun drawContactShadow(
        drawScope: DrawScope,
        worldOrigin: Vector3D,
        modelMatrix: Matrix4x4,
        halfW: Float,
        halfD: Float,
        halfH: Float,
        screenWidth: Float,
        screenHeight: Float,
        fov: Float
    ) {
        val floorY = halfH + 0.01f
        val p1 = project3DTo2D(worldOrigin + modelMatrix.transformPoint(Vector3D(-halfW * 1.03f, floorY, -halfD * 1.03f)), screenWidth, screenHeight, fov)
        val p2 = project3DTo2D(worldOrigin + modelMatrix.transformPoint(Vector3D(halfW * 1.03f, floorY, -halfD * 1.03f)), screenWidth, screenHeight, fov)
        val p3 = project3DTo2D(worldOrigin + modelMatrix.transformPoint(Vector3D(halfW * 1.03f, floorY, halfD * 1.03f)), screenWidth, screenHeight, fov)
        val p4 = project3DTo2D(worldOrigin + modelMatrix.transformPoint(Vector3D(-halfW * 1.03f, floorY, halfD * 1.03f)), screenWidth, screenHeight, fov)

        if (p1 != null && p2 != null && p3 != null && p4 != null) {
            val path = Path().apply {
                moveTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                lineTo(p4.x, p4.y)
                close()
            }
            drawScope.drawPath(path, Color(0x66000000), style = Fill)
        }
    }

    private fun drawDoorClearanceZone(
        drawScope: DrawScope,
        worldOrigin: Vector3D,
        modelMatrix: Matrix4x4,
        halfW: Float,
        halfH: Float,
        halfD: Float,
        clearanceCm: Float,
        screenWidth: Float,
        screenHeight: Float,
        fov: Float
    ) {
        val clearanceM = clearanceCm / 100f
        val floorY = halfH + 0.02f

        val cp1 = project3DTo2D(worldOrigin + modelMatrix.transformPoint(Vector3D(-halfW, floorY, halfD)), screenWidth, screenHeight, fov)
        val cp2 = project3DTo2D(worldOrigin + modelMatrix.transformPoint(Vector3D(halfW, floorY, halfD)), screenWidth, screenHeight, fov)
        val cp3 = project3DTo2D(worldOrigin + modelMatrix.transformPoint(Vector3D(halfW, floorY, halfD + clearanceM)), screenWidth, screenHeight, fov)
        val cp4 = project3DTo2D(worldOrigin + modelMatrix.transformPoint(Vector3D(-halfW, floorY, halfD + clearanceM)), screenWidth, screenHeight, fov)

        if (cp1 != null && cp2 != null && cp3 != null && cp4 != null) {
            val path = Path().apply {
                moveTo(cp1.x, cp1.y)
                lineTo(cp2.x, cp2.y)
                lineTo(cp3.x, cp3.y)
                lineTo(cp4.x, cp4.y)
                close()
            }
            drawScope.drawPath(path, EmeraldLaser.copy(alpha = 0.08f), style = Fill)
            drawScope.drawPath(
                path,
                EmeraldLaser.copy(alpha = 0.40f),
                style = Stroke(width = 1.2f)
            )
        }
    }

    private fun draw3DDimensionHUD(
        drawScope: DrawScope,
        toWorld: (Float, Float, Float) -> Vector3D,
        halfW: Float,
        halfH: Float,
        halfD: Float,
        config: WardrobeConfig,
        screenWidth: Float,
        screenHeight: Float,
        fov: Float,
        textMeasurer: TextMeasurer,
        unitSystem: UnitSystem = UnitSystem.FEET_INCHES
    ) {
        val hudOffsetM = 0.07f

        // Width Dimension (Top Front Edge)
        val wStart = toWorld(-halfW, -halfH - hudOffsetM, halfD)
        val wEnd = toWorld(halfW, -halfH - hudOffsetM, halfD)
        val wp1 = project3DTo2D(wStart, screenWidth, screenHeight, fov)
        val wp2 = project3DTo2D(wEnd, screenWidth, screenHeight, fov)

        if (wp1 != null && wp2 != null) {
            drawScope.drawLine(
                color = CyanNeon,
                start = wp1,
                end = wp2,
                strokeWidth = 1.8f
            )
            drawScope.drawCircle(CyanNeon, radius = 3f, center = wp1)
            drawScope.drawCircle(CyanNeon, radius = 3f, center = wp2)

            val midX = (wp1.x + wp2.x) / 2f
            val midY = (wp1.y + wp2.y) / 2f
            val widthText = "↔ " + DimensionFormatter.format(config.widthCm, unitSystem)
            val textLayout = textMeasurer.measure(
                text = widthText,
                style = TextStyle(color = TextPrimary, fontSize = 11.sp)
            )
            drawScope.drawRect(
                color = Color(0xEE0F172A),
                topLeft = Offset(midX - textLayout.size.width / 2f - 6f, midY - textLayout.size.height / 2f - 3f),
                size = androidx.compose.ui.geometry.Size(
                    textLayout.size.width.toFloat() + 12f,
                    textLayout.size.height.toFloat() + 6f
                )
            )
            drawScope.drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(midX - textLayout.size.width / 2f, midY - textLayout.size.height / 2f)
            )
        }

        // Height Dimension (Right Front Edge)
        val hStart = toWorld(halfW + hudOffsetM, -halfH, halfD)
        val hEnd = toWorld(halfW + hudOffsetM, halfH, halfD)
        val hp1 = project3DTo2D(hStart, screenWidth, screenHeight, fov)
        val hp2 = project3DTo2D(hEnd, screenWidth, screenHeight, fov)

        if (hp1 != null && hp2 != null) {
            drawScope.drawLine(
                color = CyanNeon,
                start = hp1,
                end = hp2,
                strokeWidth = 1.8f
            )
            drawScope.drawCircle(CyanNeon, radius = 3f, center = hp1)
            drawScope.drawCircle(CyanNeon, radius = 3f, center = hp2)

            val midX = (hp1.x + hp2.x) / 2f
            val midY = (hp1.y + hp2.y) / 2f
            val heightText = "↕ " + DimensionFormatter.format(config.heightCm, unitSystem)
            val textLayout = textMeasurer.measure(
                text = heightText,
                style = TextStyle(color = TextPrimary, fontSize = 11.sp)
            )
            drawScope.drawRect(
                color = Color(0xEE0F172A),
                topLeft = Offset(midX - textLayout.size.width / 2f - 6f, midY - textLayout.size.height / 2f - 3f),
                size = androidx.compose.ui.geometry.Size(
                    textLayout.size.width.toFloat() + 12f,
                    textLayout.size.height.toFloat() + 6f
                )
            )
            drawScope.drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(midX - textLayout.size.width / 2f, midY - textLayout.size.height / 2f)
            )
        }

        // Depth Dimension (Bottom Right Depth Edge)
        val dStart = toWorld(halfW + hudOffsetM, halfH, -halfD)
        val dEnd = toWorld(halfW + hudOffsetM, halfH, halfD)
        val dp1 = project3DTo2D(dStart, screenWidth, screenHeight, fov)
        val dp2 = project3DTo2D(dEnd, screenWidth, screenHeight, fov)

        if (dp1 != null && dp2 != null) {
            drawScope.drawLine(
                color = CyanNeon,
                start = dp1,
                end = dp2,
                strokeWidth = 1.8f
            )
            drawScope.drawCircle(CyanNeon, radius = 3f, center = dp1)
            drawScope.drawCircle(CyanNeon, radius = 3f, center = dp2)

            val midX = (dp1.x + dp2.x) / 2f
            val midY = (dp1.y + dp2.y) / 2f
            val depthText = "⤢ " + DimensionFormatter.format(config.depthCm, unitSystem)
            val textLayout = textMeasurer.measure(
                text = depthText,
                style = TextStyle(color = TextPrimary, fontSize = 11.sp)
            )
            drawScope.drawRect(
                color = Color(0xEE0F172A),
                topLeft = Offset(midX - textLayout.size.width / 2f - 6f, midY - textLayout.size.height / 2f - 3f),
                size = androidx.compose.ui.geometry.Size(
                    textLayout.size.width.toFloat() + 12f,
                    textLayout.size.height.toFloat() + 6f
                )
            )
            drawScope.drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(midX - textLayout.size.width / 2f, midY - textLayout.size.height / 2f)
            )
        }
    }
}
