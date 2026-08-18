package com.example.export

import com.example.model.CutPanel
import com.example.model.DoorStyle
import com.example.model.WardrobeConfig
import com.example.model.WardrobeProject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AutoCAD DXF R12/R2000 Vector Export Service.
 * Writes precision CAD drawings in inches with organized architectural layers.
 */
object DxfExportService {

    fun generateDxfString(project: WardrobeProject): String {
        val sb = StringBuilder()
        val config = project.wardrobeConfig

        val widthIn = config.widthCm / 2.54f
        val heightIn = config.heightCm / 2.54f
        val depthIn = config.depthCm / 2.54f
        val sections = config.sectionsCount

        // 1. DXF Header & Layers Definition
        sb.append(
            """
0
SECTION
2
HEADER
9
${'$'}ACADVER
1
AC1009
9
${'$'}INSUNITS
70
1
0
ENDSEC
0
SECTION
2
TABLES
0
TABLE
2
LAYER
70
9
0
LAYER
2
VS_OUTLINE
70
0
62
7
6
CONTINUOUS
0
LAYER
2
VS_PANEL
70
0
62
4
6
CONTINUOUS
0
LAYER
2
VS_DOOR
70
0
62
2
6
CONTINUOUS
0
LAYER
2
VS_SHELF
70
0
62
3
6
CONTINUOUS
0
LAYER
2
VS_DRAWER
70
0
62
6
6
CONTINUOUS
0
LAYER
2
VS_HARDWARE
70
0
62
1
6
CONTINUOUS
0
LAYER
2
VS_DIMENSION
70
0
62
4
6
CONTINUOUS
0
LAYER
2
VS_TEXT
70
0
62
7
6
CONTINUOUS
0
LAYER
2
VS_REFERENCE
70
0
62
8
6
CONTINUOUS
0
ENDTAB
0
ENDSEC
0
SECTION
2
ENTITIES
            """.trimIndent()
        ).append("\n")

        // Spacing parameters in inches
        val viewGap = 40.0f
        var currentOriginX = 0.0f
        val baseOriginY = 0.0f

        // ==========================================
        // VIEW 1: FRONT ELEVATION
        // ==========================================
        writeDxfText(sb, "FRONT ELEVATION (CLOSED)", currentOriginX, heightIn + 15f, 4.0f, "VS_TEXT")

        // Outer Carcass
        writeDxfRect(sb, currentOriginX, baseOriginY, widthIn, heightIn, "VS_OUTLINE")

        // Skirting (3 inches)
        writeDxfLine(sb, currentOriginX, baseOriginY + 3f, currentOriginX + widthIn, baseOriginY + 3f, "VS_PANEL")

        // Shutters
        val shutterCount = if (config.doorStyle == DoorStyle.SLIDING_BYPASS || config.doorStyle == DoorStyle.SLIDING_DOOR || config.doorStyle == DoorStyle.MIRROR_SLIDING_DOOR) {
            sections
        } else {
            sections * 2
        }
        val shutterWidthIn = widthIn / shutterCount

        for (i in 0 until shutterCount) {
            val sx = currentOriginX + (i * shutterWidthIn)
            writeDxfRect(sb, sx + 0.15f, baseOriginY + 3.15f, shutterWidthIn - 0.3f, heightIn - 3.3f, "VS_DOOR")

            // Handle
            val hx = if (i % 2 == 0) sx + shutterWidthIn - 2.0f else sx + 2.0f
            val hy = baseOriginY + (heightIn / 2.0f)
            writeDxfLine(sb, hx, hy - 12f, hx, hy + 12f, "VS_HARDWARE")
        }

        // Dimensions (Overall Width & Height)
        writeDxfDimension(sb, currentOriginX, heightIn + 6f, currentOriginX + widthIn, heightIn + 6f, CutPanel.formatInchesToFtIn(widthIn), "VS_DIMENSION")
        writeDxfDimension(sb, currentOriginX - 8f, baseOriginY, currentOriginX - 8f, baseOriginY + heightIn, CutPanel.formatInchesToFtIn(heightIn), "VS_DIMENSION")

        currentOriginX += widthIn + viewGap

        // ==========================================
        // VIEW 2: INTERIOR ELEVATION
        // ==========================================
        writeDxfText(sb, "INTERIOR ELEVATION (OPEN)", currentOriginX, heightIn + 15f, 4.0f, "VS_TEXT")

        // Carcass Outline & Inner Board (18mm = 0.708 in)
        val boardIn = 0.708f
        writeDxfRect(sb, currentOriginX, baseOriginY, widthIn, heightIn, "VS_OUTLINE")
        writeDxfRect(sb, currentOriginX + boardIn, baseOriginY + 3f + boardIn, widthIn - (2 * boardIn), heightIn - 3f - (2 * boardIn), "VS_PANEL")

        // Partitions
        val innerW = widthIn - (2 * boardIn)
        val secW = innerW / sections
        for (s in 1 until sections) {
            val px = currentOriginX + boardIn + (s * secW)
            writeDxfLine(sb, px, baseOriginY + 3f + boardIn, px, baseOriginY + heightIn - boardIn, "VS_PANEL")
        }

        // Shelves & Fittings per section
        val innerH = heightIn - 3f - (2 * boardIn)
        val loftY = baseOriginY + 3f + boardIn + (innerH * 0.78f)

        for (s in 0 until sections) {
            val secLeft = currentOriginX + boardIn + (s * secW)
            val secRight = secLeft + secW

            // Top Loft Shelf
            writeDxfLine(sb, secLeft, loftY, secRight, loftY, "VS_SHELF")

            when (s % 3) {
                0 -> {
                    // Hanging Rod
                    val railY = loftY - 4.0f
                    writeDxfLine(sb, secLeft + 1f, railY, secRight - 1f, railY, "VS_HARDWARE")
                    // Drawers
                    if (config.drawersCount > 0) {
                        val dCount = minOf(config.drawersCount, 3)
                        val drawerH = 8.0f
                        for (d in 0 until dCount) {
                            val dy = baseOriginY + 3f + boardIn + (d * drawerH)
                            writeDxfRect(sb, secLeft + 0.5f, dy + 0.5f, secW - 1.0f, drawerH - 1.0f, "VS_DRAWER")
                        }
                    }
                }
                1 -> {
                    // Shelves
                    val shelfCount = (config.shelvesCount / sections).coerceIn(2, 4)
                    val step = (loftY - (baseOriginY + 3f + boardIn)) / (shelfCount + 1)
                    for (sh in 1..shelfCount) {
                        val sy = baseOriginY + 3f + boardIn + (sh * step)
                        writeDxfLine(sb, secLeft, sy, secRight, sy, "VS_SHELF")
                    }
                }
                2 -> {
                    // Mixed
                    val midY = baseOriginY + 3f + boardIn + (innerH * 0.45f)
                    writeDxfLine(sb, secLeft, midY, secRight, midY, "VS_SHELF")
                    writeDxfLine(sb, secLeft + 1f, loftY - 4f, secRight - 1f, loftY - 4f, "VS_HARDWARE")
                }
            }
        }

        // Interior Dimensions
        writeDxfDimension(sb, currentOriginX, heightIn + 6f, currentOriginX + widthIn, heightIn + 6f, CutPanel.formatInchesToFtIn(widthIn), "VS_DIMENSION")

        currentOriginX += widthIn + viewGap

        // ==========================================
        // VIEW 3: PLAN VIEW
        // ==========================================
        writeDxfText(sb, "PLAN / TOP VIEW", currentOriginX, depthIn + 15f, 4.0f, "VS_TEXT")

        // Wall Line
        writeDxfLine(sb, currentOriginX - 4f, depthIn + 2f, currentOriginX + widthIn + 4f, depthIn + 2f, "VS_REFERENCE")
        writeDxfText(sb, "WALL", currentOriginX + (widthIn / 2f) - 3f, depthIn + 4f, 2.5f, "VS_REFERENCE")

        // Carcass Box
        writeDxfRect(sb, currentOriginX, baseOriginY, widthIn, depthIn, "VS_OUTLINE")

        // Sliding or Hinged Plan
        if (config.doorStyle == DoorStyle.SLIDING_BYPASS || config.doorStyle == DoorStyle.SLIDING_DOOR || config.doorStyle == DoorStyle.MIRROR_SLIDING_DOOR) {
            writeDxfLine(sb, currentOriginX, baseOriginY + 0.8f, currentOriginX + widthIn, baseOriginY + 0.8f, "VS_DOOR")
            writeDxfLine(sb, currentOriginX, baseOriginY + 1.6f, currentOriginX + widthIn, baseOriginY + 1.6f, "VS_DOOR")
        }

        writeDxfDimension(sb, currentOriginX, depthIn + 8f, currentOriginX + widthIn, depthIn + 8f, CutPanel.formatInchesToFtIn(widthIn), "VS_DIMENSION")
        writeDxfDimension(sb, currentOriginX - 8f, baseOriginY, currentOriginX - 8f, baseOriginY + depthIn, CutPanel.formatInchesToFtIn(depthIn), "VS_DIMENSION")

        currentOriginX += widthIn + viewGap

        // ==========================================
        // VIEW 4: SIDE ELEVATION & SECTION
        // ==========================================
        writeDxfText(sb, "SIDE ELEVATION", currentOriginX, heightIn + 15f, 4.0f, "VS_TEXT")

        writeDxfRect(sb, currentOriginX, baseOriginY, depthIn, heightIn, "VS_OUTLINE")
        writeDxfLine(sb, currentOriginX, baseOriginY + 3f, currentOriginX + depthIn, baseOriginY + 3f, "VS_PANEL")
        writeDxfLine(sb, currentOriginX + 0.25f, baseOriginY, currentOriginX + 0.25f, baseOriginY + heightIn, "VS_PANEL") // 6mm back

        writeDxfDimension(sb, currentOriginX, heightIn + 6f, currentOriginX + depthIn, heightIn + 6f, CutPanel.formatInchesToFtIn(depthIn), "VS_DIMENSION")
        writeDxfDimension(sb, currentOriginX + depthIn + 8f, baseOriginY, currentOriginX + depthIn + 8f, baseOriginY + heightIn, CutPanel.formatInchesToFtIn(heightIn), "VS_DIMENSION")

        // ==========================================
        // TITLE BLOCK IN CAD
        // ==========================================
        val tbX = 0.0f
        val tbY = -35.0f
        val tbW = 120.0f
        val tbH = 22.0f

        writeDxfRect(sb, tbX, tbY, tbW, tbH, "VS_REFERENCE")
        writeDxfLine(sb, tbX, tbY + 11.0f, tbX + tbW, tbY + 11.0f, "VS_REFERENCE")
        writeDxfLine(sb, tbX + 40.0f, tbY, tbX + 40.0f, tbY + tbH, "VS_REFERENCE")
        writeDxfLine(sb, tbX + 80.0f, tbY, tbX + 80.0f, tbY + tbH, "VS_REFERENCE")

        writeDxfText(sb, "VISIONSPACE CAD", tbX + 2.0f, tbY + 14.0f, 3.5f, "VS_TEXT")
        writeDxfText(sb, "PROJECT: ${project.name}", tbX + 42.0f, tbY + 14.0f, 3.0f, "VS_TEXT")
        writeDxfText(sb, "DWG NO: VS-001-CAD", tbX + 82.0f, tbY + 14.0f, 3.0f, "VS_TEXT")

        val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date(project.updatedAt))
        writeDxfText(sb, "DATE: $dateStr", tbX + 2.0f, tbY + 3.5f, 2.5f, "VS_TEXT")
        writeDxfText(sb, "SCALE: 1:1 INCHES", tbX + 42.0f, tbY + 3.5f, 2.5f, "VS_TEXT")
        writeDxfText(sb, "FINISH: ${config.finish.title}", tbX + 82.0f, tbY + 3.5f, 2.5f, "VS_TEXT")

        // Close DXF
        sb.append(
            """
0
ENDSEC
0
EOF
            """.trimIndent()
        ).append("\n")

        return sb.toString()
    }

    fun exportDxfFile(project: WardrobeProject, outputFile: File): File {
        val dxfContent = generateDxfString(project)
        FileOutputStream(outputFile).use { fos ->
            fos.write(dxfContent.toByteArray(Charsets.UTF_8))
        }
        return outputFile
    }

    private fun writeDxfLine(sb: StringBuilder, x1: Float, y1: Float, x2: Float, y2: Float, layer: String) {
        sb.append("0\nLINE\n8\n$layer\n10\n$x1\n20\n$y1\n30\n0.0\n11\n$x2\n21\n$y2\n31\n0.0\n")
    }

    private fun writeDxfRect(sb: StringBuilder, x: Float, y: Float, w: Float, h: Float, layer: String) {
        writeDxfLine(sb, x, y, x + w, y, layer)
        writeDxfLine(sb, x + w, y, x + w, y + h, layer)
        writeDxfLine(sb, x + w, y + h, x, y + h, layer)
        writeDxfLine(sb, x, y + h, x, y, layer)
    }

    private fun writeDxfText(sb: StringBuilder, text: String, x: Float, y: Float, height: Float, layer: String) {
        sb.append("0\nTEXT\n8\n$layer\n10\n$x\n20\n$y\n30\n0.0\n40\n$height\n1\n$text\n")
    }

    private fun writeDxfDimension(sb: StringBuilder, x1: Float, y1: Float, x2: Float, y2: Float, label: String, layer: String) {
        // Line
        writeDxfLine(sb, x1, y1, x2, y2, layer)
        // Extension ticks
        writeDxfLine(sb, x1 - 1f, y1 - 1f, x1 + 1f, y1 + 1f, layer)
        writeDxfLine(sb, x2 - 1f, y2 - 1f, x2 + 1f, y2 + 1f, layer)
        // Label Text
        val midX = (x1 + x2) / 2.0f
        val midY = (y1 + y2) / 2.0f + 1.5f
        writeDxfText(sb, label, midX - 3.0f, midY, 2.5f, layer)
    }
}
