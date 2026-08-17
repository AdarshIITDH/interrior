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

        // Architectural shadow reveal lines (3mm shadow gaps)
        val seamColor = if (isWood) grainColor.copy(alpha = 0.85f) else Color(0xFF1E2024).copy(alpha = 0.70f)
        val carcassWallColor = shadowColor
        val carcassInteriorColor = baseFinishColor

        // Carcass Back Wall (Local Z = -halfD)
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

        // Carcass Left Outer Side Panel (Gable 25mm thick)
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

        // Carcass Right Outer Side Panel (Gable 25mm thick)
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

        // Carcass Top Fascia / Roof Panel (Local Y = -halfH)
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

        // Carcass Bottom Floor Board (Local Y = halfH - plinthHeight)
        val plinthHeight = 0.08f * placement.scaleMultiplier // 8cm recessed plinth
        val carcassBottomY = halfH - plinthHeight

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

        // Recessed Base Plinth / Kickplate (Recessed 3cm from front)
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

        // Internal Architecture: Top Loft Overhead Shelf (holds storage bins)
        val loftShelfY = -halfH + 0.35f * (h / 2.4f)
        addQuad(
            polygons,
            toWorld(-halfW * 0.97f, loftShelfY, -halfD * 0.95f),
            toWorld(halfW * 0.97f, loftShelfY, -halfD * 0.95f),
            toWorld(halfW * 0.97f, loftShelfY, halfD * 0.88f),
            toWorld(-halfW * 0.97f, loftShelfY, halfD * 0.88f),
            highlightColor,
            strokeColor = seamColor,
            strokeWidth = 1f,
            screenWidth = widthPx,
            screenHeight = heightPx,
            fov = fovFocalLength
        )

        // Fabric Storage Bins on top loft shelf
        val binCount = if (config.widthCm >= 200f) 3 else 2
        val binSpacing = (w * 0.86f) / binCount
        for (b in 0 until binCount) {
            val binCenterX = -halfW * 0.75f + (b * binSpacing) + (binSpacing * 0.4f)
            val binW = (binSpacing * 0.75f) / 2f
            val binH = 0.22f
            val binY = loftShelfY - binH
            val binColor = if (b % 2 == 0) Color(0xFFD4C5B9) else Color(0xFF8C827A)

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
        }

        // Modular Interior: Shelving Tower Bay (usually on left or center-right bay)
        val shelfBayLeft = if (bayCount == 3) halfW * 0.33f else 0f
        val shelfBayRight = halfW * 0.96f
        val shelfCount = config.shelvesCount.coerceIn(1, 6)
        val shelfAvailableHeight = carcassBottomY - loftShelfY
        val shelfSpacing = shelfAvailableHeight / (shelfCount + 1)

        for (i in 1..shelfCount) {
            val shelfY = loftShelfY + (i * shelfSpacing)
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

            // Folded clothing stack on middle shelves
            if (i in 1..2) {
                val stackCenterX = (shelfBayLeft + shelfBayRight) / 2f
                val stackW = (shelfBayRight - shelfBayLeft) * 0.35f
                val stackH = 0.08f
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
            }
        }

        // Modular Soft-Close Drawers (at bottom of tower bay)
        val drawerCount = config.drawersCount.coerceIn(0, 5)
        if (drawerCount > 0) {
            val drawerTotalHeight = (carcassBottomY - loftShelfY) * 0.45f
            val drawerHeight = drawerTotalHeight / drawerCount
            val drawerStartY = carcassBottomY - drawerTotalHeight

            for (dIdx in 0 until drawerCount) {
                val dTopY = drawerStartY + (dIdx * drawerHeight) + 0.005f
                val dBottomY = dTopY + (drawerHeight * 0.92f)

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
                    val grainY = (dTopY + dBottomY) / 2f - 0.02f
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
                val hLeft = toWorld(hMidX - 0.10f, handleY, halfD * 0.90f)
                val hRight = toWorld(hMidX + 0.10f, handleY, halfD * 0.90f)
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

        // Hanging Rails Bay (Hanging suits, coats, shirts with realistic coat hangers)
        val hangingBayLeft = -halfW * 0.96f
        val hangingBayRight = if (bayCount == 3) -halfW * 0.33f else 0f
        val railsCount = config.hangingRailsCount.coerceIn(1, 3)

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

        // Realistic Hanging Garments (Blazers, Suits, Shirts)
        val garmentColors = listOf(
            Color(0xFF1E293B), // Navy Suit
            Color(0xFF334155), // Charcoal Blazer
            Color(0xFF78350F), // Camel Coat
            Color(0xFFF8FAFC), // White Dress Shirt
            Color(0xFF475569)  // Slate Jacket
        )
        val clothesCount = ((hangingBayRight - hangingBayLeft) / 0.14f).toInt().coerceIn(2, 5)
        val clothesSpacing = (hangingBayRight - hangingBayLeft) / (clothesCount + 1)

        for (g in 1..clothesCount) {
            val gx = hangingBayLeft + (g * clothesSpacing)
            val gColor = garmentColors[g % garmentColors.size]
            val garmentLen = 0.55f // Blazer length

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

            // Realistic Garment Silhouette with Shoulder Cut
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

        // Realistic Bespoke Front Doors with Shaker Timber Panel Profiling & Wood Grain
        val openRatio = config.doorOpenRatio.coerceIn(0f, 1f)

        if (config.doorStyle != DoorStyle.OPEN_CONCEPT) {
            val isSliding = config.doorStyle == DoorStyle.SLIDING_BYPASS ||
                    config.doorStyle == DoorStyle.SLIDING_DOOR ||
                    config.doorStyle == DoorStyle.MIRROR_SLIDING_DOOR

            if (isSliding) {
                // Realistic Sliding Bypass Doors with Floor & Top Roller Channels
                val slideCount = if (config.widthCm >= 240f) 3 else 2
                val leafWidth = (w / slideCount) + 0.04f
                val slideOffset = (w * 0.70f / slideCount) * openRatio

                for (s in 0 until slideCount) {
                    val baseDoorX = -halfW + (s * (w / slideCount))
                    val curX = if (s == 0) baseDoorX + slideOffset else baseDoorX
                    val zLayer = if (s % 2 == 0) halfD * 0.98f else halfD * 1.02f
                    val doorColor = if (config.doorStyle == DoorStyle.MIRROR_SLIDING_DOOR) {
                        Color(0xFFE0E7FF).copy(alpha = 0.85f)
                    } else if (s % 2 == 0) baseFinishColor else highlightColor

                    // Sliding Door Outer Frame Panel
                    addQuad(
                        polygons,
                        toWorld(curX, -halfH, zLayer),
                        toWorld(curX + leafWidth, -halfH, zLayer),
                        toWorld(curX + leafWidth, carcassBottomY, zLayer),
                        toWorld(curX, carcassBottomY, zLayer),
                        doorColor,
                        strokeColor = seamColor,
                        strokeWidth = 1.4f,
                        screenWidth = widthPx,
                        screenHeight = heightPx,
                        fov = fovFocalLength
                    )

                    // Shaker Recessed Center Timber Panel (if not mirror glass)
                    if (config.doorStyle != DoorStyle.MIRROR_SLIDING_DOOR) {
                        val stileW = leafWidth * 0.12f
                        val railH = h * 0.06f
                        val innerDoorColor = if (isWood) shadowColor else highlightColor
                        addQuad(
                            polygons,
                            toWorld(curX + stileW, -halfH + railH, zLayer + 0.005f),
                            toWorld(curX + leafWidth - stileW, -halfH + railH, zLayer + 0.005f),
                            toWorld(curX + leafWidth - stileW, carcassBottomY - railH, zLayer + 0.005f),
                            toWorld(curX + stileW, carcassBottomY - railH, zLayer + 0.005f),
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
                                topY = -halfH + railH,
                                bottomY = carcassBottomY - railH,
                                zPos = zLayer + 0.006f,
                                grainColor = grainColor,
                                widthPx = widthPx,
                                heightPx = heightPx,
                                fov = fovFocalLength
                            )
                        }
                    }

                    // Integrated Full-Height Metal Edge Pull Handle
                    val handleX = curX + (if (s % 2 == 0) leafWidth - 0.03f else 0.03f)
                    drawSolidVerticalHandle(
                        polygons = polygons,
                        toWorld = ::toWorld,
                        xPos = handleX,
                        yCenter = 0f,
                        zPos = zLayer + 0.02f,
                        lengthM = h * 0.40f,
                        widthPx = widthPx,
                        heightPx = heightPx,
                        fov = fovFocalLength
                    )
                }

            } else {
                // Realistic Hinged Doors (2, 3, or 4 Door Leaves with 3D Hinges & Shaker Profile)
                val doorLeafCount = if (config.widthCm > 240f) 4 else 2
                val doorLeafWidth = w / doorLeafCount
                val doorAngleRad = (Math.PI * 0.52 * openRatio).toFloat()

                for (dIdx in 0 until doorLeafCount) {
                    val isLeftDoor = dIdx < (doorLeafCount / 2)
                    val hingeX = -halfW + (dIdx * doorLeafWidth) + (if (isLeftDoor) 0f else doorLeafWidth)
                    val sign = if (isLeftDoor) 1f else -1f

                    val tipX = hingeX + (sign * doorLeafWidth * cos(doorAngleRad))
                    val tipZ = halfD + (doorLeafWidth * sin(doorAngleRad))

                    val doorColor = if (dIdx % 2 == 0) baseFinishColor else highlightColor

                    // Main Door Leaf Panel
                    val (pLeftX, pLeftZ, pRightX, pRightZ) = if (isLeftDoor) {
                        listOf(hingeX, halfD, tipX, tipZ)
                    } else {
                        listOf(tipX, tipZ, hingeX, halfD)
                    }

                    addQuad(
                        polygons,
                        toWorld(pLeftX, -halfH, pLeftZ),
                        toWorld(pRightX, -halfH, pRightZ),
                        toWorld(pRightX, carcassBottomY, pRightZ),
                        toWorld(pLeftX, carcassBottomY, pLeftZ),
                        doorColor,
                        strokeColor = seamColor,
                        strokeWidth = 1.4f,
                        screenWidth = widthPx,
                        screenHeight = heightPx,
                        fov = fovFocalLength
                    )

                    // Shaker Center Inset Beveled Timber Panel
                    val stileW = doorLeafWidth * 0.14f
                    val railH = h * 0.06f
                    val insetLeftX = pLeftX + (pRightX - pLeftX) * 0.14f
                    val insetLeftZ = pLeftZ + (pRightZ - pLeftZ) * 0.14f
                    val insetRightX = pRightX - (pRightX - pLeftX) * 0.14f
                    val insetRightZ = pRightZ - (pRightZ - pLeftZ) * 0.14f
                    val innerDoorColor = if (isWood) shadowColor else highlightColor

                    addQuad(
                        polygons,
                        toWorld(insetLeftX, -halfH + railH, insetLeftZ + 0.005f),
                        toWorld(insetRightX, -halfH + railH, insetRightZ + 0.005f),
                        toWorld(insetRightX, carcassBottomY - railH, insetRightZ + 0.005f),
                        toWorld(insetLeftX, carcassBottomY - railH, insetLeftZ + 0.005f),
                        innerDoorColor,
                        strokeColor = seamColor,
                        strokeWidth = 1.1f,
                        screenWidth = widthPx,
                        screenHeight = heightPx,
                        fov = fovFocalLength
                    )

                    // Vertical Wood Grain lines on Hinged Door Center Panel
                    if (isWood) {
                        for (g in 1..3) {
                            val ratio = g * 0.25f
                            val gx = insetLeftX + (insetRightX - insetLeftX) * ratio
                            val gz = insetLeftZ + (insetRightZ - insetLeftZ) * ratio
                            val gTop = toWorld(gx, -halfH + railH + 0.02f, gz + 0.006f)
                            val gBtm = toWorld(gx, carcassBottomY - railH - 0.02f, gz + 0.006f)
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

                    // Vertical Pull Handle near door opening edge
                    val handleX = tipX - (sign * 0.035f * cos(doorAngleRad))
                    val handleZ = tipZ + 0.02f
                    drawSolidVerticalHandle(
                        polygons = polygons,
                        toWorld = ::toWorld,
                        xPos = handleX,
                        yCenter = 0f,
                        zPos = handleZ,
                        lengthM = h * 0.38f,
                        widthPx = widthPx,
                        heightPx = heightPx,
                        fov = fovFocalLength
                    )
                }
            }
        }

        // 5. Painter's Algorithm: Sort polygons by Average Depth (furthest first)
        polygons.sortByDescending { it.averageDepth }

        // 6. Draw Polygons
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
     * Draws procedural organic wood grain with sinusoidal multi-frequency wave flow,
     * realistic growth-ring variations, and fine timber pores.
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

    /**
     * Draws rich Cathedral Flame grain arches (the signature of crown-cut real timber veneers).
     */
    private fun drawWoodFlameCathedral(
        polygons: MutableList<ProjectedPolygon>,
        toWorld: (Float, Float, Float) -> Vector3D,
        centerX: Float,
        topY: Float,
        bottomY: Float,
        widthM: Float,
        zPos: Float,
        grainColor: Color,
        widthPx: Float,
        heightPx: Float,
        fov: Float
    ) {
        val totalH = bottomY - topY
        val archCount = 2
        for (a in 1..archCount) {
            val archH = totalH * (0.35f + a * 0.15f)
            val archTopY = topY + totalH * (0.12f + a * 0.14f)
            val archHalfW = (widthM * 0.32f) * (0.5f + a * 0.25f)

            val pts = mutableListOf<Offset>()
            var totalZ = 0f
            val steps = 8
            for (s in 0..steps) {
                val angle = (Math.PI * s / steps).toFloat()
                val ax = centerX + cos(angle) * archHalfW
                val ay = archTopY + (1f - sin(angle)) * archH
                val pt3D = toWorld(ax, ay, zPos)
                val screenPt = project3DTo2D(pt3D, widthPx, heightPx, fov)
                if (screenPt != null) {
                    pts.add(screenPt)
                    totalZ += pt3D.z
                }
            }
            if (pts.size >= 3) {
                polygons.add(
                    ProjectedPolygon(
                        points = pts,
                        averageDepth = totalZ / pts.size,
                        fillColor = Color.Transparent,
                        strokeColor = grainColor.copy(alpha = 0.38f),
                        strokeWidth = 1.1f
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

        // Drop shadow cast by handle onto wood panel
        val shadowTop3D = toWorld(xPos + 0.006f, yCenter - lengthM / 2f + 0.005f, zPos - 0.015f)
        val shadowBtm3D = toWorld(xPos + 0.006f, yCenter + lengthM / 2f + 0.005f, zPos - 0.015f)
        val sp1 = project3DTo2D(shadowTop3D, widthPx, heightPx, fov)
        val sp2 = project3DTo2D(shadowBtm3D, widthPx, heightPx, fov)
        if (sp1 != null && sp2 != null) {
            polygons.add(
                ProjectedPolygon(
                    points = listOf(sp1, sp2),
                    averageDepth = (shadowTop3D.z + shadowBtm3D.z) / 2f,
                    fillColor = Color.Transparent,
                    strokeColor = Color.Black.copy(alpha = 0.40f),
                    strokeWidth = 4f
                )
            )
        }

        val p1 = project3DTo2D(top3D, widthPx, heightPx, fov)
        val p2 = project3DTo2D(btm3D, widthPx, heightPx, fov)
        if (p1 != null && p2 != null) {
            // Main Handle Rod Body
            polygons.add(
                ProjectedPolygon(
                    points = listOf(p1, p2),
                    averageDepth = (top3D.z + btm3D.z) / 2f,
                    fillColor = Color.Transparent,
                    strokeColor = Color(0xFF1E2128),
                    strokeWidth = 4.5f
                )
            )
            // Metallic highlight reflection
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
