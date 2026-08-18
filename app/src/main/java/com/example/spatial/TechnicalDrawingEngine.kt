package com.example.spatial

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.example.model.CutPanel
import com.example.model.DoorStyle
import com.example.model.FinishType
import com.example.model.LedLighting
import com.example.model.WardrobeConfig
import com.example.model.WardrobeProject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

enum class TechnicalDrawingViewType(val title: String, val drawingNumber: String) {
    FRONT_ELEVATION("Front Elevation", "VS-001"),
    INTERIOR_ELEVATION("Interior Elevation", "VS-002"),
    PLAN_VIEW("Plan / Top View", "VS-003"),
    SIDE_ELEVATION("Side & Section View", "VS-004"),
    ALL_IN_ONE("Complete Architectural Drawing", "VS-100")
}

/**
 * Dedicated mathematical vector 2D technical drawing engine for VisionSpace.
 * Produces crisp architectural blueprints with CAD dimensions, title blocks, and hatching.
 */
object TechnicalDrawingEngine {

    private const val DEFAULT_WIDTH_2K = 2560
    private const val DEFAULT_HEIGHT_2K = 1440

    /**
     * Generates a 2K high-resolution technical drawing bitmap with white background and black vector lines.
     */
    fun generateDrawingBitmap(
        project: WardrobeProject,
        viewType: TechnicalDrawingViewType = TechnicalDrawingViewType.ALL_IN_ONE,
        widthPx: Int = DEFAULT_WIDTH_2K,
        heightPx: Int = DEFAULT_HEIGHT_2K,
        isDarkTheme: Boolean = false
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColor = if (isDarkTheme) Color.rgb(15, 23, 42) else Color.WHITE
        val primaryLineColor = if (isDarkTheme) Color.rgb(226, 232, 240) else Color.rgb(15, 23, 42)
        val dimensionColor = if (isDarkTheme) Color.rgb(0, 240, 255) else Color.rgb(2, 132, 199)
        val hiddenLineColor = if (isDarkTheme) Color.rgb(148, 163, 184) else Color.rgb(100, 116, 139)
        val hatchColor = if (isDarkTheme) Color.rgb(30, 41, 59) else Color.rgb(241, 245, 249)

        canvas.drawColor(bgColor)

        val config = project.wardrobeConfig

        // 1. Draw Architectural Outer Frame & Grid
        drawArchitecturalBorder(canvas, widthPx.toFloat(), heightPx.toFloat(), primaryLineColor)

        // 2. Draw Bottom Title Block
        drawTitleBlock(canvas, project, viewType, widthPx.toFloat(), heightPx.toFloat(), primaryLineColor, dimensionColor)

        // 3. Drawing Canvas Work Area (above title block, inside borders)
        val margin = 50f
        val titleBlockHeight = 180f
        val workRect = RectF(margin + 20f, margin + 20f, widthPx - margin - 20f, heightPx - margin - titleBlockHeight - 30f)

        when (viewType) {
            TechnicalDrawingViewType.FRONT_ELEVATION -> {
                drawSingleFrontElevation(canvas, config, workRect, primaryLineColor, dimensionColor, isDarkTheme)
            }
            TechnicalDrawingViewType.INTERIOR_ELEVATION -> {
                drawSingleInteriorElevation(canvas, config, workRect, primaryLineColor, dimensionColor, isDarkTheme)
            }
            TechnicalDrawingViewType.PLAN_VIEW -> {
                drawSinglePlanView(canvas, config, workRect, primaryLineColor, dimensionColor, isDarkTheme)
            }
            TechnicalDrawingViewType.SIDE_ELEVATION -> {
                drawSingleSideView(canvas, config, workRect, primaryLineColor, dimensionColor, isDarkTheme)
            }
            TechnicalDrawingViewType.ALL_IN_ONE -> {
                drawAllInOneViews(canvas, config, workRect, primaryLineColor, dimensionColor, hiddenLineColor, hatchColor, isDarkTheme)
            }
        }

        return bitmap
    }

    private fun drawArchitecturalBorder(canvas: Canvas, w: Float, h: Float, color: Int) {
        val paint = Paint().apply {
            this.color = color
            this.style = Paint.Style.STROKE
            this.strokeWidth = 3f
            this.isAntiAlias = true
        }

        val margin = 40f
        canvas.drawRect(margin, margin, w - margin, h - margin, paint)

        // Inner fine border
        paint.strokeWidth = 1.2f
        canvas.drawRect(margin + 8f, margin + 8f, w - margin - 8f, h - margin - 8f, paint)
    }

    private fun drawTitleBlock(
        canvas: Canvas,
        project: WardrobeProject,
        viewType: TechnicalDrawingViewType,
        w: Float,
        h: Float,
        lineColor: Int,
        accentColor: Int
    ) {
        val margin = 48f
        val blockH = 160f
        val blockW = 800f
        val right = w - margin
        val bottom = h - margin
        val left = right - blockW
        val top = bottom - blockH

        val borderPaint = Paint().apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

        val fillPaint = Paint().apply {
            color = if (lineColor == Color.WHITE) Color.rgb(15, 23, 42) else Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }

        canvas.drawRect(left, top, right, bottom, fillPaint)
        canvas.drawRect(left, top, right, bottom, borderPaint)

        // Subdivisions
        val midY1 = top + 50f
        val midY2 = top + 105f
        val colX1 = left + 280f
        val colX2 = left + 540f

        canvas.drawLine(left, midY1, right, midY1, borderPaint)
        canvas.drawLine(left, midY2, right, midY2, borderPaint)
        canvas.drawLine(colX1, midY1, colX1, bottom, borderPaint)
        canvas.drawLine(colX2, midY1, colX2, bottom, borderPaint)

        // Text Paints
        val brandPaint = Paint().apply {
            color = accentColor
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headingPaint = Paint().apply {
            color = lineColor
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 12f
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = lineColor
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Row 1: Logo & Title
        canvas.drawText("VISIONSPACE", left + 20f, top + 36f, brandPaint)
        canvas.drawText("ARCHITECTURAL JOINERY DRAWING", left + 260f, top + 34f, headingPaint)

        // Row 2
        canvas.drawText("PROJECT", left + 15f, midY1 + 20f, labelPaint)
        canvas.drawText(project.name, left + 15f, midY1 + 42f, valuePaint)

        canvas.drawText("DRAWING TITLE", colX1 + 15f, midY1 + 20f, labelPaint)
        canvas.drawText(viewType.title, colX1 + 15f, midY1 + 42f, valuePaint)

        canvas.drawText("DRAWING NO.", colX2 + 15f, midY1 + 20f, labelPaint)
        canvas.drawText(viewType.drawingNumber, colX2 + 15f, midY1 + 42f, brandPaint.apply { textSize = 18f })

        // Row 3
        val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date(project.updatedAt))
        canvas.drawText("DATE: $dateStr", left + 15f, midY2 + 28f, valuePaint.apply { textSize = 13f })
        canvas.drawText("SCALE: N.T.S.", colX1 + 15f, midY2 + 28f, valuePaint)
        canvas.drawText("UNITS: FEET & INCHES", colX2 + 15f, midY2 + 28f, valuePaint)
    }

    private fun drawAllInOneViews(
        canvas: Canvas,
        config: WardrobeConfig,
        bounds: RectF,
        lineColor: Int,
        dimColor: Int,
        hiddenColor: Int,
        hatchColor: Int,
        isDarkTheme: Boolean
    ) {
        val gap = 60f
        val cellW = (bounds.width() - gap) / 2f
        val cellH = (bounds.height() - gap) / 2f

        // Quadrants:
        // Top-Left: Front Elevation
        val rectFront = RectF(bounds.left, bounds.top, bounds.left + cellW, bounds.top + cellH)
        drawSingleFrontElevation(canvas, config, rectFront, lineColor, dimColor, isDarkTheme)

        // Top-Right: Interior Elevation
        val rectInterior = RectF(bounds.left + cellW + gap, bounds.top, bounds.right, bounds.top + cellH)
        drawSingleInteriorElevation(canvas, config, rectInterior, lineColor, dimColor, isDarkTheme)

        // Bottom-Left: Plan View
        val rectPlan = RectF(bounds.left, bounds.top + cellH + gap, bounds.left + cellW, bounds.bottom)
        drawSinglePlanView(canvas, config, rectPlan, lineColor, dimColor, isDarkTheme)

        // Bottom-Right: Side Elevation & Section View
        val rectSide = RectF(bounds.left + cellW + gap, bounds.top + cellH + gap, bounds.right, bounds.bottom)
        drawSingleSideView(canvas, config, rectSide, lineColor, dimColor, isDarkTheme)
    }

    private fun drawSingleFrontElevation(
        canvas: Canvas,
        config: WardrobeConfig,
        rect: RectF,
        lineColor: Int,
        dimColor: Int,
        isDarkTheme: Boolean
    ) {
        drawViewTitle(canvas, "1. FRONT ELEVATION (CLOSED)", rect.left + 20f, rect.top + 25f, lineColor)

        val paddingX = 70f
        val paddingY = 80f
        val drawW = rect.width() - (paddingX * 2)
        val drawH = rect.height() - (paddingY * 2)

        val widthIn = config.widthCm / 2.54f
        val heightIn = config.heightCm / 2.54f
        val sections = config.sectionsCount

        val scale = minOf(drawW / widthIn, drawH / heightIn)
        val boxW = widthIn * scale
        val boxH = heightIn * scale

        val left = rect.left + paddingX + (drawW - boxW) / 2f
        val top = rect.top + paddingY + (drawH - boxH) / 2f
        val right = left + boxW
        val bottom = top + boxH

        val solidPaint = Paint().apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        val fillShutterPaint = Paint().apply {
            color = if (isDarkTheme) Color.rgb(24, 33, 47) else Color.rgb(245, 247, 250)
            style = Paint.Style.FILL
        }

        // Carcass Outline
        canvas.drawRect(left, top, right, bottom, fillShutterPaint)
        canvas.drawRect(left, top, right, bottom, solidPaint)

        // Plinth Skirting
        val plinthH = 3.0f * scale
        canvas.drawRect(left, bottom - plinthH, right, bottom, solidPaint)

        // Shutters / Doors
        val shutterCount = if (config.doorStyle == DoorStyle.SLIDING_BYPASS || config.doorStyle == DoorStyle.SLIDING_DOOR || config.doorStyle == DoorStyle.MIRROR_SLIDING_DOOR) {
            sections
        } else {
            sections * 2
        }

        val doorW = boxW / shutterCount
        val doorTop = top
        val doorBottom = bottom - plinthH

        for (i in 0 until shutterCount) {
            val dLeft = left + (i * doorW)
            val dRight = dLeft + doorW

            // Shutter border
            canvas.drawRect(dLeft + 2f, doorTop + 2f, dRight - 2f, doorBottom - 2f, solidPaint.apply { strokeWidth = 2f })

            // Handle
            val handleX = if (i % 2 == 0) dRight - 16f else dLeft + 16f
            val handleMidY = (doorTop + doorBottom) / 2f
            canvas.drawRect(handleX - 3f, handleMidY - 30f, handleX + 3f, handleMidY + 30f, solidPaint.apply { strokeWidth = 3f })
        }

        // Dimensions
        drawHorizontalDimension(canvas, left, right, top - 35f, CutPanel.formatInchesToFtIn(widthIn), dimColor)
        drawVerticalDimension(canvas, left - 40f, top, bottom, CutPanel.formatInchesToFtIn(heightIn), dimColor)

        // Sub-dimensions for doors
        for (i in 0 until shutterCount) {
            val dLeft = left + (i * doorW)
            val dRight = dLeft + doorW
            drawHorizontalDimension(canvas, dLeft, dRight, bottom + 25f, CutPanel.formatInchesToFtIn(widthIn / shutterCount), dimColor, textSize = 11f)
        }
    }

    private fun drawSingleInteriorElevation(
        canvas: Canvas,
        config: WardrobeConfig,
        rect: RectF,
        lineColor: Int,
        dimColor: Int,
        isDarkTheme: Boolean
    ) {
        drawViewTitle(canvas, "2. INTERIOR ELEVATION (OPEN)", rect.left + 20f, rect.top + 25f, lineColor)

        val paddingX = 70f
        val paddingY = 80f
        val drawW = rect.width() - (paddingX * 2)
        val drawH = rect.height() - (paddingY * 2)

        val widthIn = config.widthCm / 2.54f
        val heightIn = config.heightCm / 2.54f
        val sections = config.sectionsCount

        val scale = minOf(drawW / widthIn, drawH / heightIn)
        val boxW = widthIn * scale
        val boxH = heightIn * scale

        val left = rect.left + paddingX + (drawW - boxW) / 2f
        val top = rect.top + paddingY + (drawH - boxH) / 2f
        val right = left + boxW
        val bottom = top + boxH

        val solidPaint = Paint().apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        val finePaint = Paint().apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
            isAntiAlias = true
        }
        val boardThicknessPx = (18f / 25.4f) * scale

        // Carcass Outline (18mm walls)
        canvas.drawRect(left, top, right, bottom, solidPaint)
        canvas.drawRect(left + boardThicknessPx, top + boardThicknessPx, right - boardThicknessPx, bottom - (3f * scale) - boardThicknessPx, finePaint)

        // Skirting
        canvas.drawRect(left, bottom - (3f * scale), right, bottom, solidPaint)

        // Section Dividers
        val sectionW = (boxW - (boardThicknessPx * 2)) / sections
        for (s in 1 until sections) {
            val divX = left + boardThicknessPx + (s * sectionW)
            canvas.drawRect(divX - (boardThicknessPx / 2), top + boardThicknessPx, divX + (boardThicknessPx / 2), bottom - (3f * scale) - boardThicknessPx, finePaint)
        }

        // Interior fittings per section
        val innerTop = top + boardThicknessPx
        val innerBottom = bottom - (3f * scale) - boardThicknessPx
        val innerH = innerBottom - innerTop

        for (s in 0 until sections) {
            val secLeft = left + boardThicknessPx + (s * sectionW)
            val secRight = secLeft + sectionW

            // Top Loft Shelf
            val loftShelfY = innerTop + (innerH * 0.22f)
            canvas.drawRect(secLeft, loftShelfY, secRight, loftShelfY + boardThicknessPx, finePaint)

            // Section details
            when (s % 3) {
                0 -> { // Hanging Section
                    val railY = loftShelfY + boardThicknessPx + 35f
                    canvas.drawLine(secLeft + 10f, railY, secRight - 10f, railY, solidPaint.apply { strokeWidth = 4f })
                    // Draw hanger symbols
                    val step = (secRight - secLeft) / 4f
                    for (h in 1..3) {
                        val hx = secLeft + (h * step)
                        drawHangerIcon(canvas, hx, railY + 2f, lineColor)
                    }
                    // Bottom Drawers if configured
                    if (config.drawersCount > 0) {
                        val dCount = minOf(config.drawersCount, 3)
                        val drawerH = 45f
                        val drawerStartY = innerBottom - (dCount * drawerH)
                        for (d in 0 until dCount) {
                            val dy = drawerStartY + (d * drawerH)
                            canvas.drawRect(secLeft + 5f, dy + 2f, secRight - 5f, dy + drawerH - 2f, finePaint)
                            // Drawer Pull
                            canvas.drawLine((secLeft + secRight) / 2f - 20f, dy + drawerH / 2f, (secLeft + secRight) / 2f + 20f, dy + drawerH / 2f, solidPaint.apply { strokeWidth = 3f })
                        }
                    }
                }
                1 -> { // Shelving Section
                    val shelfCount = (config.shelvesCount / sections).coerceIn(2, 5)
                    val sStep = (innerBottom - (loftShelfY + boardThicknessPx)) / (shelfCount + 1)
                    for (sh in 1..shelfCount) {
                        val sy = loftShelfY + boardThicknessPx + (sh * sStep)
                        canvas.drawRect(secLeft, sy, secRight, sy + boardThicknessPx, finePaint)
                    }
                }
                2 -> { // Long Coat / Mixed Section
                    val midShelfY = innerTop + (innerH * 0.55f)
                    canvas.drawRect(secLeft, midShelfY, secRight, midShelfY + boardThicknessPx, finePaint)
                    val railY = loftShelfY + boardThicknessPx + 35f
                    canvas.drawLine(secLeft + 10f, railY, secRight - 10f, railY, solidPaint.apply { strokeWidth = 4f })
                    drawHangerIcon(canvas, (secLeft + secRight) / 2f, railY + 2f, lineColor)

                    // Shoe rack or drawers below
                    if (config.hasShoeRack) {
                        val shoeY = innerBottom - 40f
                        canvas.drawLine(secLeft + 10f, shoeY + 15f, secRight - 10f, shoeY - 15f, finePaint)
                        canvas.drawText("SHOE TIER", (secLeft + secRight) / 2f - 25f, shoeY + 30f, Paint().apply { color = lineColor; textSize = 10f })
                    }
                }
            }
        }

        // Dimensions
        drawHorizontalDimension(canvas, left, right, top - 35f, CutPanel.formatInchesToFtIn(widthIn), dimColor)
        drawVerticalDimension(canvas, right + 40f, top, bottom, CutPanel.formatInchesToFtIn(heightIn), dimColor)
    }

    private fun drawSinglePlanView(
        canvas: Canvas,
        config: WardrobeConfig,
        rect: RectF,
        lineColor: Int,
        dimColor: Int,
        isDarkTheme: Boolean
    ) {
        drawViewTitle(canvas, "3. PLAN / TOP VIEW", rect.left + 20f, rect.top + 25f, lineColor)

        val paddingX = 70f
        val paddingY = 60f
        val drawW = rect.width() - (paddingX * 2)
        val drawH = rect.height() - (paddingY * 2)

        val widthIn = config.widthCm / 2.54f
        val depthIn = config.depthCm / 2.54f

        val scale = minOf(drawW / widthIn, drawH / (depthIn * 2.2f))
        val boxW = widthIn * scale
        val boxD = depthIn * scale

        val left = rect.left + paddingX + (drawW - boxW) / 2f
        val top = rect.top + paddingY + (drawH - boxD) / 2f
        val right = left + boxW
        val bottom = top + boxD

        val solidPaint = Paint().apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        val wallPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
        }
        val dashPaint = Paint().apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
            isAntiAlias = true
        }

        // Rear Wall Hatching Line
        canvas.drawLine(left - 30f, top - 15f, right + 30f, top - 15f, wallPaint)
        canvas.drawText("BRICK MASONRY WALL", (left + right) / 2f - 60f, top - 25f, Paint().apply { color = Color.rgb(100, 116, 139); textSize = 11f })

        // Wardrobe Box
        canvas.drawRect(left, top, right, bottom, solidPaint)

        // Sliding Track vs Hinged Swing
        if (config.doorStyle == DoorStyle.SLIDING_BYPASS || config.doorStyle == DoorStyle.SLIDING_DOOR || config.doorStyle == DoorStyle.MIRROR_SLIDING_DOOR) {
            // Dual sliding tracks
            canvas.drawLine(left, bottom - 10f, right, bottom - 10f, solidPaint.apply { strokeWidth = 2f })
            canvas.drawLine(left, bottom - 20f, right, bottom - 20f, solidPaint.apply { strokeWidth = 2f })
            canvas.drawText("DOUBLE SLIDING TRACK", (left + right) / 2f - 60f, bottom - 26f, Paint().apply { color = dimColor; textSize = 10f })
        } else {
            // Door swing arcs
            val sections = config.sectionsCount
            val doorW = boxW / (sections * 2)
            for (i in 0 until (sections * 2)) {
                val hingeX = left + (i * doorW)
                val swingPath = Path().apply {
                    moveTo(hingeX, bottom)
                    lineTo(hingeX, bottom + doorW)
                    arcTo(RectF(hingeX - doorW, bottom - doorW, hingeX + doorW, bottom + doorW), 90f, -90f, false)
                }
                canvas.drawPath(swingPath, dashPaint)
            }
        }

        // Dimensions
        drawHorizontalDimension(canvas, left, right, top - 45f, CutPanel.formatInchesToFtIn(widthIn), dimColor)
        drawVerticalDimension(canvas, left - 40f, top, bottom, CutPanel.formatInchesToFtIn(depthIn), dimColor)
    }

    private fun drawSingleSideView(
        canvas: Canvas,
        config: WardrobeConfig,
        rect: RectF,
        lineColor: Int,
        dimColor: Int,
        isDarkTheme: Boolean
    ) {
        drawViewTitle(canvas, "4. SIDE ELEVATION & SECTION", rect.left + 20f, rect.top + 25f, lineColor)

        val paddingX = 70f
        val paddingY = 80f
        val drawW = rect.width() - (paddingX * 2)
        val drawH = rect.height() - (paddingY * 2)

        val depthIn = config.depthCm / 2.54f
        val heightIn = config.heightCm / 2.54f

        val scale = minOf(drawW / (depthIn * 1.5f), drawH / heightIn)
        val boxD = depthIn * scale
        val boxH = heightIn * scale

        val left = rect.left + paddingX + (drawW - boxD) / 2f
        val top = rect.top + paddingY + (drawH - boxH) / 2f
        val right = left + boxD
        val bottom = top + boxH

        val solidPaint = Paint().apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        val finePaint = Paint().apply {
            color = lineColor
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
            isAntiAlias = true
        }

        // Side Carcass Box
        canvas.drawRect(left, top, right, bottom, solidPaint)

        // Skirting
        val plinthH = 3.0f * scale
        canvas.drawRect(left, bottom - plinthH, right, bottom, solidPaint)

        // Back Panel Groove (6mm thickness at rear)
        val backGroovePx = 6f
        canvas.drawLine(left + backGroovePx, top, left + backGroovePx, bottom - plinthH, finePaint)
        canvas.drawText("6mm BACK", left + 8f, (top + bottom) / 2f, Paint().apply { color = Color.rgb(100, 116, 139); textSize = 10f })

        // 18mm Front Shutter Profile
        canvas.drawRect(right - 10f, top, right, bottom - plinthH, finePaint)

        // Shelves Section (Horizontal tick marks)
        val shelfY1 = top + (boxH * 0.22f)
        val shelfY2 = top + (boxH * 0.55f)
        canvas.drawLine(left + backGroovePx, shelfY1, right - 10f, shelfY1, finePaint)
        canvas.drawLine(left + backGroovePx, shelfY2, right - 10f, shelfY2, finePaint)

        // Dimensions
        drawHorizontalDimension(canvas, left, right, top - 35f, CutPanel.formatInchesToFtIn(depthIn), dimColor)
        drawVerticalDimension(canvas, right + 40f, top, bottom, CutPanel.formatInchesToFtIn(heightIn), dimColor)
    }

    private fun drawHorizontalDimension(
        canvas: Canvas,
        x1: Float,
        x2: Float,
        y: Float,
        text: String,
        dimColor: Int,
        textSize: Float = 13f
    ) {
        val paint = Paint().apply {
            color = dimColor
            strokeWidth = 1.6f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = dimColor
            this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Extension lines
        canvas.drawLine(x1, y - 8f, x1, y + 8f, paint)
        canvas.drawLine(x2, y - 8f, x2, y + 8f, paint)

        // Dimension line
        canvas.drawLine(x1, y, x2, y, paint)

        // Arrowheads
        drawArrow(canvas, x1, y, isLeft = true, paint)
        drawArrow(canvas, x2, y, isLeft = false, paint)

        // Text
        canvas.drawText(text, (x1 + x2) / 2f, y - 6f, textPaint)
    }

    private fun drawVerticalDimension(
        canvas: Canvas,
        x: Float,
        y1: Float,
        y2: Float,
        text: String,
        dimColor: Int,
        textSize: Float = 13f
    ) {
        val paint = Paint().apply {
            color = dimColor
            strokeWidth = 1.6f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = dimColor
            this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Extension lines
        canvas.drawLine(x - 8f, y1, x + 8f, y1, paint)
        canvas.drawLine(x - 8f, y2, x + 8f, y2, paint)

        // Dimension line
        canvas.drawLine(x, y1, x, y2, paint)

        // Arrowheads
        drawVerticalArrow(canvas, x, y1, isUp = true, paint)
        drawVerticalArrow(canvas, x, y2, isUp = false, paint)

        // Rotated Text
        canvas.save()
        canvas.rotate(-90f, x, (y1 + y2) / 2f)
        canvas.drawText(text, x, ((y1 + y2) / 2f) - 6f, textPaint)
        canvas.restore()
    }

    private fun drawArrow(canvas: Canvas, x: Float, y: Float, isLeft: Boolean, paint: Paint) {
        val dir = if (isLeft) 1f else -1f
        canvas.drawLine(x, y, x + (dir * 12f), y - 4f, paint)
        canvas.drawLine(x, y, x + (dir * 12f), y + 4f, paint)
    }

    private fun drawVerticalArrow(canvas: Canvas, x: Float, y: Float, isUp: Boolean, paint: Paint) {
        val dir = if (isUp) 1f else -1f
        canvas.drawLine(x, y, x - 4f, y + (dir * 12f), paint)
        canvas.drawLine(x, y, x + 4f, y + (dir * 12f), paint)
    }

    private fun drawHangerIcon(canvas: Canvas, x: Float, y: Float, color: Int) {
        val p = Paint().apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
            isAntiAlias = true
        }
        val path = Path().apply {
            moveTo(x, y + 10f)
            lineTo(x - 16f, y + 26f)
            lineTo(x + 16f, y + 26f)
            close()
            moveTo(x, y + 10f)
            lineTo(x, y + 2f)
        }
        canvas.drawPath(path, p)
    }

    private fun drawViewTitle(canvas: Canvas, title: String, x: Float, y: Float, color: Int) {
        val paint = Paint().apply {
            this.color = color
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(title, x, y, paint)
    }
}
