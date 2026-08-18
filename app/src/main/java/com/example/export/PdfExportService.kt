package com.example.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.model.BOMCalculator
import com.example.model.BOMResult
import com.example.model.CutPanel
import com.example.model.CuttingScheduleGenerator
import com.example.model.HardwareItem
import com.example.model.HardwareScheduleGenerator
import com.example.model.WardrobeCostRates
import com.example.model.WardrobeProject
import com.example.spatial.TechnicalDrawingEngine
import com.example.spatial.TechnicalDrawingViewType
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Professional multi-page PDF generation engine for VisionSpace.
 * Exports Detailed Estimate PDFs and Complete Contractor Project PDFs.
 */
object PdfExportService {

    private const val PAGE_WIDTH = 595 // A4 standard point width (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 standard point height (72 dpi)

    fun formatInr(amount: Double): String {
        val longVal = Math.round(amount)
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        var str = format.format(longVal)
        str = str.replace("INR", "₹").replace(".00", "").trim()
        if (!str.startsWith("₹")) {
            str = "₹$str"
        }
        return str
    }

    /**
     * Generates a Detailed Estimate PDF document (6 pages).
     */
    fun generateEstimatePdf(project: WardrobeProject, outputFile: File): File {
        val pdfDoc = PdfDocument()
        val bomResult = BOMCalculator.calculateBOM(project.wardrobeConfig)
        val cuttingSchedule = CuttingScheduleGenerator.generateCuttingSchedule(project.wardrobeConfig)
        val hardwareSchedule = HardwareScheduleGenerator.generateHardwareSchedule(project.wardrobeConfig)

        val totalPages = 6

        // Page 1: Cover
        renderPage1Cover(pdfDoc, project, bomResult, 1, totalPages)

        // Page 2: Project Specification
        renderPage2Specification(pdfDoc, project, bomResult, 2, totalPages)

        // Page 3: Dimension Summary & Diagrams
        renderPage3DimensionSummary(pdfDoc, project, 3, totalPages)

        // Page 4: Detailed Bill of Materials (BOM)
        renderPage4DetailedBOM(pdfDoc, project, bomResult, 4, totalPages)

        // Page 5: Cutting Schedule & Hardware Breakdown
        renderPage5CuttingAndHardware(pdfDoc, project, cuttingSchedule, hardwareSchedule, 5, totalPages)

        // Page 6: Cost Summary & Contractor Disclaimer
        renderPage6CostSummaryAndDisclaimer(pdfDoc, project, bomResult, 6, totalPages)

        FileOutputStream(outputFile).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()
        return outputFile
    }

    /**
     * Generates a Complete Contractor Project PDF (10 pages including Technical Elevations).
     */
    fun generateCompleteProjectPdf(project: WardrobeProject, outputFile: File): File {
        val pdfDoc = PdfDocument()
        val bomResult = BOMCalculator.calculateBOM(project.wardrobeConfig)
        val cuttingSchedule = CuttingScheduleGenerator.generateCuttingSchedule(project.wardrobeConfig)
        val hardwareSchedule = HardwareScheduleGenerator.generateHardwareSchedule(project.wardrobeConfig)

        val totalPages = 10

        // Page 1: Cover
        renderPage1Cover(pdfDoc, project, bomResult, 1, totalPages)

        // Page 2: Project Specification
        renderPage2Specification(pdfDoc, project, bomResult, 2, totalPages)

        // Page 3: Dimension Summary
        renderPage3DimensionSummary(pdfDoc, project, 3, totalPages)

        // Page 4: Technical Drawing - Front Elevation
        renderTechnicalDrawingPage(pdfDoc, project, TechnicalDrawingViewType.FRONT_ELEVATION, 4, totalPages)

        // Page 5: Technical Drawing - Interior Elevation
        renderTechnicalDrawingPage(pdfDoc, project, TechnicalDrawingViewType.INTERIOR_ELEVATION, 5, totalPages)

        // Page 6: Technical Drawing - Plan & Side Elevation
        renderTechnicalDrawingPage(pdfDoc, project, TechnicalDrawingViewType.ALL_IN_ONE, 6, totalPages)

        // Page 7: Detailed Bill of Materials
        renderPage4DetailedBOM(pdfDoc, project, bomResult, 7, totalPages)

        // Page 8: Panel Cutting Schedule
        renderCuttingSchedulePage(pdfDoc, project, cuttingSchedule, 8, totalPages)

        // Page 9: Hardware Schedule & Lighting
        renderHardwareSchedulePage(pdfDoc, project, hardwareSchedule, 9, totalPages)

        // Page 10: Cost Summary & Disclaimer
        renderPage6CostSummaryAndDisclaimer(pdfDoc, project, bomResult, 10, totalPages)

        FileOutputStream(outputFile).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()
        return outputFile
    }

    // =========================================================================
    // PAGE RENDERING METHODS
    // =========================================================================

    private fun renderPage1Cover(
        pdfDoc: PdfDocument,
        project: WardrobeProject,
        bom: BOMResult,
        pageNum: Int,
        totalPages: Int
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawPageHeaderFooter(canvas, project, pageNum, totalPages, isCover = true)

        val brandPaint = Paint().apply {
            color = Color.rgb(2, 132, 199)
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subHeadingPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 12f
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val costPaint = Paint().apply {
            color = Color.rgb(16, 185, 129)
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        var y = 90f
        canvas.drawText("VISIONSPACE", 50f, y, brandPaint)
        y += 28f
        canvas.drawText("WARDROBE DESIGN & ESTIMATE SPECIFICATION", 50f, y, subHeadingPaint)

        // Accent Cyan Rule
        y += 18f
        val rulePaint = Paint().apply {
            color = Color.rgb(2, 132, 199)
            strokeWidth = 3f
        }
        canvas.drawLine(50f, y, PAGE_WIDTH - 50f, y, rulePaint)

        // 3D Architectural Preview Box
        y += 25f
        val previewRect = RectF(50f, y, PAGE_WIDTH - 50f, y + 260f)
        val bgPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        val strokePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(previewRect, 12f, 12f, bgPaint)
        canvas.drawRoundRect(previewRect, 12f, 12f, strokePaint)

        // Render preview drawing into bitmap and draw onto canvas
        val previewBmp = TechnicalDrawingEngine.generateDrawingBitmap(
            project = project,
            viewType = TechnicalDrawingViewType.FRONT_ELEVATION,
            widthPx = 800,
            heightPx = 450,
            isDarkTheme = false
        )
        val srcRect = Rect(0, 0, previewBmp.width, previewBmp.height)
        val destRect = RectF(60f, y + 10f, PAGE_WIDTH - 60f, y + 250f)
        canvas.drawBitmap(previewBmp, srcRect, destRect, null)

        // Project Summary Fields
        y += 290f
        val col1 = 50f
        val col2 = 320f

        canvas.drawText("PROJECT NAME", col1, y, labelPaint)
        canvas.drawText("DATE OF SPECIFICATION", col2, y, labelPaint)
        y += 18f
        canvas.drawText(project.name, col1, y, valuePaint)
        val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale.US).format(Date(project.updatedAt))
        canvas.drawText(dateStr, col2, y, valuePaint)

        y += 35f
        canvas.drawText("OVERALL DIMENSIONS (W × H × D)", col1, y, labelPaint)
        canvas.drawText("CONFIGURATION & FINISH", col2, y, labelPaint)
        y += 18f
        canvas.drawText(project.formattedOverallDimensionsFtIn, col1, y, valuePaint)
        val configSummary = "${project.wardrobeConfig.sectionsCount} Sections • ${project.wardrobeConfig.doorStyle.title} • ${project.wardrobeConfig.finish.title}"
        canvas.drawText(configSummary, col2, y, valuePaint)

        y += 45f
        val costBox = RectF(50f, y, PAGE_WIDTH - 50f, y + 75f)
        val costBg = Paint().apply {
            color = Color.rgb(236, 253, 245)
            style = Paint.Style.FILL
        }
        val costBorder = Paint().apply {
            color = Color.rgb(16, 185, 129)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(costBox, 10f, 10f, costBg)
        canvas.drawRoundRect(costBox, 10f, 10f, costBorder)

        canvas.drawText("TOTAL ESTIMATED PROJECT COST", 70f, y + 28f, labelPaint.apply { color = Color.rgb(4, 120, 87) })
        canvas.drawText(formatInr(bom.totalCost), 70f, y + 58f, costPaint)

        pdfDoc.finishPage(page)
    }

    private fun renderPage2Specification(
        pdfDoc: PdfDocument,
        project: WardrobeProject,
        bom: BOMResult,
        pageNum: Int,
        totalPages: Int
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawPageHeaderFooter(canvas, project, pageNum, totalPages)

        var y = 80f
        drawSectionHeader(canvas, "1. PROJECT DETAILS & SITE ATTRIBUTES", y)
        y += 30f

        val details = listOf(
            "Project Name" to project.name,
            "Target Room" to project.roomName,
            "Site Photo Source" to if (project.siteCapture.calibration != null) "AR Calibrated Site Photo" else "Manual Site Snapshot",
            "Measurement Confidence" to project.siteCapture.confidence.title,
            "Site Verified Date" to if (project.siteCapture.confidence.isFabricationReady) SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US).format(Date()) else "Pending On-Site Tape Verification"
        )
        y = drawKeyValueTable(canvas, details, y)

        y += 25f
        drawSectionHeader(canvas, "2. WARDROBE TECHNICAL SPECIFICATION", y)
        y += 30f

        val cfg = project.wardrobeConfig
        val specs = listOf(
            "Overall Width" to "${CutPanel.formatInchesToFtIn(cfg.widthCm / 2.54f)} (${cfg.widthCm.toInt()} cm)",
            "Overall Height" to "${CutPanel.formatInchesToFtIn(cfg.heightCm / 2.54f)} (${cfg.heightCm.toInt()} cm)",
            "Overall Depth" to "${CutPanel.formatInchesToFtIn(cfg.depthCm / 2.54f)} (${cfg.depthCm.toInt()} cm)",
            "Number of Sections" to "${cfg.sectionsCount} Equal Vertical Bays",
            "Door / Shutter Style" to cfg.doorStyle.title,
            "Primary External Finish" to "${cfg.finish.title} (1.0mm Premium Textured Laminate)",
            "Internal Balancing Finish" to "0.8mm Anti-Fungal Frost White Laminate",
            "Carcass Core Board" to "18 mm BWP Marine Grade Plywood (IS:710)",
            "Shutter Core Board" to "18 mm High-Density Pinewood Blockboard (IS:1659)",
            "Backing Panel" to "6 mm High-Density Hardwood Plywood (Rebated)",
            "Edge Banding" to "2.0 mm Seamless PVC Matching Edge Band",
            "Adhesive System" to "Fevicol Marine / Probond Heat-Resistant D3"
        )
        drawKeyValueTable(canvas, specs, y)

        pdfDoc.finishPage(page)
    }

    private fun renderPage3DimensionSummary(
        pdfDoc: PdfDocument,
        project: WardrobeProject,
        pageNum: Int,
        totalPages: Int
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawPageHeaderFooter(canvas, project, pageNum, totalPages)

        var y = 80f
        drawSectionHeader(canvas, "3. DIMENSION SUMMARY & CLEARANCE MATRIX", y)
        y += 30f

        val cfg = project.wardrobeConfig
        val widthIn = cfg.widthCm / 2.54f
        val heightIn = cfg.heightCm / 2.54f
        val depthIn = cfg.depthCm / 2.54f
        val secWIn = widthIn / cfg.sectionsCount

        val rows = mutableListOf<Pair<String, String>>()
        rows.add("Overall Carcass Width" to CutPanel.formatInchesToFtIn(widthIn))
        rows.add("Overall Carcass Height" to CutPanel.formatInchesToFtIn(heightIn))
        rows.add("Overall Carcass Depth" to CutPanel.formatInchesToFtIn(depthIn))
        rows.add("Plinth Skirting Base Height" to "3\" (75 mm)")
        rows.add("Top Ceiling Assembly Margin" to "7\" (180 mm)")
        rows.add("Side Wall Allowance" to "3\" (75 mm) per side")

        for (i in 1..cfg.sectionsCount) {
            rows.add("Section $i Clear Width" to CutPanel.formatInchesToFtIn(secWIn))
        }

        rows.add("Loft Storage Bay Height" to "1' 8\" (500 mm)")
        rows.add("Hanging Rail Clear Drop" to "3' 6\" (1060 mm)")
        if (cfg.drawersCount > 0) {
            rows.add("Drawer Fascia Height" to "7½\" (190 mm) each")
        }
        if (cfg.hasShoeRack) {
            rows.add("Slanted Shoe Bay Depth" to "1' 2\" (350 mm)")
        }

        y = drawKeyValueTable(canvas, rows, y)

        pdfDoc.finishPage(page)
    }

    private fun renderPage4DetailedBOM(
        pdfDoc: PdfDocument,
        project: WardrobeProject,
        bom: BOMResult,
        pageNum: Int,
        totalPages: Int
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawPageHeaderFooter(canvas, project, pageNum, totalPages)

        var y = 80f
        drawSectionHeader(canvas, "4. DETAILED BILL OF MATERIALS (BOM)", y)
        y += 25f

        // Draw BOM Table Header
        val colSNo = 50f
        val colItem = 80f
        val colSpec = 220f
        val colQty = 375f
        val colUnit = 420f
        val colRate = 465f
        val colAmount = 525f

        val headerPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerBg = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }

        canvas.drawRect(50f, y, PAGE_WIDTH - 50f, y + 20f, headerBg)
        canvas.drawText("S.N.", colSNo, y + 14f, headerPaint)
        canvas.drawText("ITEM DESCRIPTION", colItem, y + 14f, headerPaint)
        canvas.drawText("SPECIFICATION", colSpec, y + 14f, headerPaint)
        canvas.drawText("QTY", colQty, y + 14f, headerPaint)
        canvas.drawText("UNIT", colUnit, y + 14f, headerPaint)
        canvas.drawText("RATE", colRate, y + 14f, headerPaint)
        canvas.drawText("AMOUNT", colAmount, y + 14f, headerPaint)

        y += 24f

        val rowPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9.5f
            isAntiAlias = true
        }
        val boldRowPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        bom.items.take(16).forEachIndexed { idx, item ->
            canvas.drawText("${idx + 1}", colSNo, y + 12f, rowPaint)
            val name = if (item.name.length > 22) item.name.substring(0, 20) + ".." else item.name
            canvas.drawText(name, colItem, y + 12f, boldRowPaint)

            val spec = if (item.details.length > 26) item.details.substring(0, 24) + ".." else item.details
            canvas.drawText(spec, colSpec, y + 12f, rowPaint)

            val qtyStr = String.format(Locale.US, "%.1f", item.quantity)
            canvas.drawText(qtyStr, colQty, y + 12f, rowPaint)
            canvas.drawText(item.unit, colUnit, y + 12f, rowPaint)
            canvas.drawText("₹${item.unitRate.toInt()}", colRate, y + 12f, rowPaint)
            canvas.drawText(formatInr(item.totalCost), colAmount, y + 12f, boldRowPaint)

            canvas.drawLine(50f, y + 18f, PAGE_WIDTH - 50f, y + 18f, linePaint)
            y += 20f
        }

        pdfDoc.finishPage(page)
    }

    private fun renderPage5CuttingAndHardware(
        pdfDoc: PdfDocument,
        project: WardrobeProject,
        cuttingSchedule: List<CutPanel>,
        hardwareSchedule: List<HardwareItem>,
        pageNum: Int,
        totalPages: Int
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawPageHeaderFooter(canvas, project, pageNum, totalPages)

        var y = 80f
        drawSectionHeader(canvas, "5. PANEL CUTTING SCHEDULE & OPTIMIZATION", y)
        y += 25f

        // Cutting Table
        val colPart = 50f
        val colQty = 180f
        val colDim = 220f
        val colThick = 310f
        val colMat = 370f
        val colEdge = 480f

        val headerPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerBg = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }

        canvas.drawRect(50f, y, PAGE_WIDTH - 50f, y + 18f, headerBg)
        canvas.drawText("PART NAME", colPart, y + 13f, headerPaint)
        canvas.drawText("QTY", colQty, y + 13f, headerPaint)
        canvas.drawText("SIZE (L × W)", colDim, y + 13f, headerPaint)
        canvas.drawText("THICK", colThick, y + 13f, headerPaint)
        canvas.drawText("MATERIAL", colMat, y + 13f, headerPaint)
        canvas.drawText("EDGE BAND", colEdge, y + 13f, headerPaint)

        y += 22f

        val rowPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        cuttingSchedule.take(8).forEach { panel ->
            canvas.drawText(panel.partName, colPart, y + 11f, rowPaint)
            canvas.drawText("${panel.quantity} pc", colQty, y + 11f, rowPaint)
            canvas.drawText("${panel.lengthFtIn} × ${panel.widthFtIn}", colDim, y + 11f, rowPaint)
            canvas.drawText("${panel.thicknessMm} mm", colThick, y + 11f, rowPaint)
            val matStr = if (panel.material.length > 16) panel.material.substring(0, 14) + ".." else panel.material
            canvas.drawText(matStr, colMat, y + 11f, rowPaint)
            val edgeStr = if (panel.edgeBanding.length > 16) panel.edgeBanding.substring(0, 14) + ".." else panel.edgeBanding
            canvas.drawText(edgeStr, colEdge, y + 11f, rowPaint)

            canvas.drawLine(50f, y + 16f, PAGE_WIDTH - 50f, y + 16f, linePaint)
            y += 18f
        }

        y += 25f
        drawSectionHeader(canvas, "6. HARDWARE & ACCESSORIES SCHEDULE", y)
        y += 25f

        hardwareSchedule.take(6).forEach { hw ->
            canvas.drawText(hw.itemName, 50f, y + 12f, rowPaint.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
            canvas.drawText("${hw.quantity} ${hw.unit}", 340f, y + 12f, rowPaint)
            canvas.drawText("@ ₹${hw.unitRateInr.toInt()}", 420f, y + 12f, rowPaint)
            canvas.drawText(formatInr(hw.totalAmountInr), 490f, y + 12f, rowPaint.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })

            canvas.drawLine(50f, y + 18f, PAGE_WIDTH - 50f, y + 18f, linePaint)
            y += 20f
        }

        pdfDoc.finishPage(page)
    }

    private fun renderPage6CostSummaryAndDisclaimer(
        pdfDoc: PdfDocument,
        project: WardrobeProject,
        bom: BOMResult,
        pageNum: Int,
        totalPages: Int
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawPageHeaderFooter(canvas, project, pageNum, totalPages)

        var y = 80f
        drawSectionHeader(canvas, "7. COMMERCIAL COST SUMMARY & RECAP", y)
        y += 25f

        val pricing = project.pricingConfig
        val materialTotal = bom.items.filter { it.category == com.example.model.BOMCategory.BOARDS || it.category == com.example.model.BOMCategory.LAMINATES || it.category == com.example.model.BOMCategory.ADHESIVES }.sumOf { it.totalCost }
        val hardwareTotal = bom.items.filter { it.category == com.example.model.BOMCategory.HARDWARE || it.category == com.example.model.BOMCategory.HANDLES || it.category == com.example.model.BOMCategory.LIGHTING || it.category == com.example.model.BOMCategory.ACCESSORIES }.sumOf { it.totalCost }
        val labourTotal = bom.labourCost
        val installTotal = pricing.installationCharge
        val transportTotal = pricing.transportationCharge

        val subtotal = materialTotal + hardwareTotal + labourTotal + installTotal + transportTotal
        val discount = pricing.discountAmount
        val gstAmount = if (pricing.isGstEnabled) ((subtotal - discount) * (pricing.gstPercent / 100.0)) else 0.0
        val finalGrandTotal = subtotal - discount + gstAmount

        val summaryRows = listOf(
            "Total Raw Materials & Surface Finishes" to formatInr(materialTotal),
            "Architectural Hardware & Smart Accessories" to formatInr(hardwareTotal),
            "Carpentry Fabrication & Carcass Joinery Labour" to formatInr(labourTotal),
            "Site Handling & Modular Installation" to formatInr(installTotal),
            "Logistics & Packaging Transportation" to formatInr(transportTotal),
            "------------------------------------------------" to "-------------",
            "Project Subtotal" to formatInr(subtotal),
            "Special Courtesy Discount" to if (discount > 0) "- ${formatInr(discount)}" else "₹0",
            "Taxation (GST ${if (pricing.isGstEnabled) "${pricing.gstPercent.toInt()}%" else "0%"})" to if (gstAmount > 0) formatInr(gstAmount) else "₹0",
            "ESTIMATED TOTAL PAYABLE" to formatInr(finalGrandTotal)
        )

        y = drawKeyValueTable(canvas, summaryRows, y, isSummary = true)

        y += 35f
        drawSectionHeader(canvas, "8. CARPENTRY & FABRICATION DISCLAIMER", y)
        y += 25f

        val disclaimerPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 10f
            isAntiAlias = true
        }

        val disclaimer = listOf(
            "• This estimate is mathematically computed from the configured VisionSpace 3D parametric joinery model and standard local market rates.",
            "• Actual site conditions, wall out-of-plumb deviations, material brand availability, contractor margins and taxes may vary.",
            "• Critical site tape measurements MUST be independently taken and confirmed on-site before cutting expensive plywood or glass panels.",
            "• Standard 10% material waste factor is incorporated to ensure adequate sheet yields."
        )

        disclaimer.forEach { line ->
            canvas.drawText(line, 50f, y, disclaimerPaint)
            y += 16f
        }

        pdfDoc.finishPage(page)
    }

    private fun renderTechnicalDrawingPage(
        pdfDoc: PdfDocument,
        project: WardrobeProject,
        viewType: TechnicalDrawingViewType,
        pageNum: Int,
        totalPages: Int
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawPageHeaderFooter(canvas, project, pageNum, totalPages)

        // Render vector drawing into high-res bitmap
        val drawingBmp = TechnicalDrawingEngine.generateDrawingBitmap(
            project = project,
            viewType = viewType,
            widthPx = 1600,
            heightPx = 1100,
            isDarkTheme = false
        )

        val srcRect = Rect(0, 0, drawingBmp.width, drawingBmp.height)
        val destRect = RectF(40f, 75f, PAGE_WIDTH - 40f, PAGE_HEIGHT - 65f)
        canvas.drawBitmap(drawingBmp, srcRect, destRect, null)

        pdfDoc.finishPage(page)
    }

    private fun renderCuttingSchedulePage(
        pdfDoc: PdfDocument,
        project: WardrobeProject,
        cuttingSchedule: List<CutPanel>,
        pageNum: Int,
        totalPages: Int
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawPageHeaderFooter(canvas, project, pageNum, totalPages)

        var y = 80f
        drawSectionHeader(canvas, "COMPLETE PANEL CUTTING SCHEDULE", y)
        y += 25f

        val colPart = 50f
        val colQty = 180f
        val colDim = 220f
        val colThick = 310f
        val colMat = 370f
        val colEdge = 480f

        val headerPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerBg = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }

        canvas.drawRect(50f, y, PAGE_WIDTH - 50f, y + 18f, headerBg)
        canvas.drawText("PART NAME", colPart, y + 13f, headerPaint)
        canvas.drawText("QTY", colQty, y + 13f, headerPaint)
        canvas.drawText("SIZE (L × W)", colDim, y + 13f, headerPaint)
        canvas.drawText("THICK", colThick, y + 13f, headerPaint)
        canvas.drawText("MATERIAL", colMat, y + 13f, headerPaint)
        canvas.drawText("EDGE BAND", colEdge, y + 13f, headerPaint)

        y += 22f

        val rowPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        cuttingSchedule.forEach { panel ->
            canvas.drawText(panel.partName, colPart, y + 11f, rowPaint)
            canvas.drawText("${panel.quantity} pc", colQty, y + 11f, rowPaint)
            canvas.drawText("${panel.lengthFtIn} × ${panel.widthFtIn}", colDim, y + 11f, rowPaint)
            canvas.drawText("${panel.thicknessMm} mm", colThick, y + 11f, rowPaint)
            val matStr = if (panel.material.length > 16) panel.material.substring(0, 14) + ".." else panel.material
            canvas.drawText(matStr, colMat, y + 11f, rowPaint)
            val edgeStr = if (panel.edgeBanding.length > 16) panel.edgeBanding.substring(0, 14) + ".." else panel.edgeBanding
            canvas.drawText(edgeStr, colEdge, y + 11f, rowPaint)

            canvas.drawLine(50f, y + 16f, PAGE_WIDTH - 50f, y + 16f, linePaint)
            y += 20f
        }

        pdfDoc.finishPage(page)
    }

    private fun renderHardwareSchedulePage(
        pdfDoc: PdfDocument,
        project: WardrobeProject,
        hardwareSchedule: List<HardwareItem>,
        pageNum: Int,
        totalPages: Int
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        drawPageHeaderFooter(canvas, project, pageNum, totalPages)

        var y = 80f
        drawSectionHeader(canvas, "COMPLETE HARDWARE & LIGHTING SCHEDULE", y)
        y += 25f

        val rowPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9.5f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        hardwareSchedule.forEach { hw ->
            canvas.drawText(hw.itemName, 50f, y + 12f, rowPaint.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
            canvas.drawText("${hw.quantity} ${hw.unit}", 340f, y + 12f, rowPaint)
            canvas.drawText("@ ₹${hw.unitRateInr.toInt()}", 420f, y + 12f, rowPaint)
            canvas.drawText(formatInr(hw.totalAmountInr), 490f, y + 12f, rowPaint.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })

            canvas.drawLine(50f, y + 18f, PAGE_WIDTH - 50f, y + 18f, linePaint)
            y += 22f
        }

        pdfDoc.finishPage(page)
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private fun drawPageHeaderFooter(
        canvas: Canvas,
        project: WardrobeProject,
        pageNum: Int,
        totalPages: Int,
        isCover: Boolean = false
    ) {
        val headerPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 9f
            isAntiAlias = true
        }
        val footerPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 9f
            isAntiAlias = true
        }
        val rulePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }

        // Header (pages 2+)
        if (!isCover) {
            canvas.drawText("VisionSpace Architectural Joinery Specification", 50f, 40f, headerPaint)
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(project.updatedAt))
            canvas.drawText(dateStr, PAGE_WIDTH - 120f, 40f, headerPaint)
            canvas.drawLine(50f, 48f, PAGE_WIDTH - 50f, 48f, rulePaint)
        }

        // Footer
        canvas.drawLine(50f, PAGE_HEIGHT - 45f, PAGE_WIDTH - 50f, PAGE_HEIGHT - 45f, rulePaint)
        canvas.drawText("VisionSpace • ${project.name}", 50f, PAGE_HEIGHT - 30f, footerPaint)
        canvas.drawText("Page $pageNum of $totalPages", PAGE_WIDTH - 110f, PAGE_HEIGHT - 30f, footerPaint)
    }

    private fun drawSectionHeader(canvas: Canvas, title: String, y: Float) {
        val bgPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }
        val textPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val rect = RectF(50f, y - 14f, PAGE_WIDTH - 50f, y + 10f)
        canvas.drawRoundRect(rect, 4f, 4f, bgPaint)
        canvas.drawText(title, 60f, y + 3f, textPaint)
    }

    private fun drawKeyValueTable(
        canvas: Canvas,
        rows: List<Pair<String, String>>,
        startY: Float,
        isSummary: Boolean = false
    ): Float {
        var y = startY
        val keyPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 10f
            isAntiAlias = true
        }
        val valPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            strokeWidth = 0.8f
        }

        rows.forEach { (key, value) ->
            if (key.startsWith("---")) {
                canvas.drawLine(50f, y + 4f, PAGE_WIDTH - 50f, y + 4f, Paint().apply { color = Color.rgb(15, 23, 42); strokeWidth = 1.5f })
                y += 12f
                return@forEach
            }

            canvas.drawText(key, 50f, y + 10f, keyPaint)
            canvas.drawText(value, 320f, y + 10f, valPaint)

            canvas.drawLine(50f, y + 16f, PAGE_WIDTH - 50f, y + 16f, linePaint)
            y += 20f
        }

        return y
    }
}
