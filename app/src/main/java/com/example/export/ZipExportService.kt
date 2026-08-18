package com.example.export

import android.content.Context
import android.graphics.Bitmap
import com.example.model.BOMCalculator
import com.example.model.CuttingScheduleGenerator
import com.example.model.HardwareScheduleGenerator
import com.example.model.WardrobeProject
import com.example.spatial.TechnicalDrawingEngine
import com.example.spatial.TechnicalDrawingViewType
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Service to bundle all project artifacts into a single ZIP package for sharing with contractors.
 */
object ZipExportService {

    fun createProjectZipPackage(
        context: Context,
        project: WardrobeProject,
        sitePreviewBitmap: Bitmap? = null,
        onProgress: (stage: String) -> Unit = {}
    ): File {
        val sanitizedName = project.name.replace("\\s+".toRegex(), "_").replace("[^a-zA-Z0-9_-]".toRegex(), "")
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(project.updatedAt))
        val zipFileName = "VisionSpace_${sanitizedName}_$dateStr.zip"

        val tempDir = File(context.cacheDir, "project_export_${System.currentTimeMillis()}").apply { mkdirs() }
        val outputZipFile = File(context.cacheDir, zipFileName)

        val zipOut = ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZipFile)))

        try {
            // 1. Site Preview Image
            onProgress("Exporting Site Preview...")
            val sitePreviewFile = File(tempDir, "01_Site_Preview.jpg")
            val bmpToSave = sitePreviewBitmap ?: TechnicalDrawingEngine.generateDrawingBitmap(
                project = project,
                viewType = TechnicalDrawingViewType.FRONT_ELEVATION,
                widthPx = 1920,
                heightPx = 1080,
                isDarkTheme = false
            )
            FileOutputStream(sitePreviewFile).use { fos ->
                bmpToSave.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            }
            addFileToZip(zipOut, sitePreviewFile, "01_Site_Preview.jpg")

            // 2. Complete Project PDF (10 pages)
            onProgress("Generating Complete Project PDF...")
            val completePdfFile = File(tempDir, "02_Complete_Project.pdf")
            PdfExportService.generateCompleteProjectPdf(project, completePdfFile)
            addFileToZip(zipOut, completePdfFile, "02_Complete_Project.pdf")

            // 3. Estimate PDF
            onProgress("Generating Detailed Estimate PDF...")
            val estimatePdfFile = File(tempDir, "03_Estimate.pdf")
            PdfExportService.generateEstimatePdf(project, estimatePdfFile)
            addFileToZip(zipOut, estimatePdfFile, "03_Estimate.pdf")

            // 4. AutoCAD DXF
            onProgress("Generating AutoCAD DXF...")
            val dxfFile = File(tempDir, "04_Wardrobe_Drawing.dxf")
            DxfExportService.exportDxfFile(project, dxfFile)
            addFileToZip(zipOut, dxfFile, "04_Wardrobe_Drawing.dxf")

            // 5. Front Elevation JPG (2K)
            onProgress("Rendering 2K Technical Elevation Drawings...")
            val frontBmp = TechnicalDrawingEngine.generateDrawingBitmap(project, TechnicalDrawingViewType.FRONT_ELEVATION)
            val frontFile = File(tempDir, "05_Front_Elevation.jpg")
            FileOutputStream(frontFile).use { frontBmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            addFileToZip(zipOut, frontFile, "05_Front_Elevation.jpg")

            // 6. Interior Elevation JPG
            val intBmp = TechnicalDrawingEngine.generateDrawingBitmap(project, TechnicalDrawingViewType.INTERIOR_ELEVATION)
            val intFile = File(tempDir, "06_Interior_Elevation.jpg")
            FileOutputStream(intFile).use { intBmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            addFileToZip(zipOut, intFile, "06_Interior_Elevation.jpg")

            // 7. Plan View JPG
            val planBmp = TechnicalDrawingEngine.generateDrawingBitmap(project, TechnicalDrawingViewType.PLAN_VIEW)
            val planFile = File(tempDir, "07_Plan_View.jpg")
            FileOutputStream(planFile).use { planBmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            addFileToZip(zipOut, planFile, "07_Plan_View.jpg")

            // 8. Side View JPG
            val sideBmp = TechnicalDrawingEngine.generateDrawingBitmap(project, TechnicalDrawingViewType.SIDE_ELEVATION)
            val sideFile = File(tempDir, "08_Side_View.jpg")
            FileOutputStream(sideFile).use { sideBmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            addFileToZip(zipOut, sideFile, "08_Side_View.jpg")

            // 9. BOM CSV
            onProgress("Generating Bill of Materials CSV...")
            val bomCsvFile = File(tempDir, "09_BOM.csv")
            generateBomCsv(project, bomCsvFile)
            addFileToZip(zipOut, bomCsvFile, "09_BOM.csv")

            // 10. Cutting List CSV
            onProgress("Generating Cutting Schedule CSV...")
            val cuttingCsvFile = File(tempDir, "10_Cutting_List.csv")
            generateCuttingCsv(project, cuttingCsvFile)
            addFileToZip(zipOut, cuttingCsvFile, "10_Cutting_List.csv")

            // 11. Project Data JSON
            onProgress("Writing Project Metadata JSON...")
            val jsonFile = File(tempDir, "11_Project_Data.json")
            generateProjectJson(project, jsonFile)
            addFileToZip(zipOut, jsonFile, "11_Project_Data.json")

            onProgress("Packaging Project ZIP...")
        } finally {
            zipOut.close()
            tempDir.deleteRecursively()
        }

        return outputZipFile
    }

    private fun addFileToZip(zipOut: ZipOutputStream, file: File, entryName: String) {
        if (!file.exists()) return
        val entry = ZipEntry(entryName)
        zipOut.putNextEntry(entry)
        file.inputStream().use { input ->
            input.copyTo(zipOut)
        }
        zipOut.closeEntry()
    }

    private fun generateBomCsv(project: WardrobeProject, outputFile: File) {
        val bom = BOMCalculator.calculateBOM(project.wardrobeConfig)
        val sb = StringBuilder()
        sb.append("S.No.,Item,Specification,Quantity,Unit,Unit Rate (INR),Total Amount (INR)\n")
        bom.items.forEachIndexed { idx, item ->
            val cleanName = item.name.replace(",", " ")
            val cleanSpec = item.details.replace(",", " ")
            sb.append("${idx + 1},\"$cleanName\",\"$cleanSpec\",${item.quantity},${item.unit},${item.unitRate},${item.totalCost}\n")
        }
        sb.append("\n,,,Subtotal,,,,${bom.materialsCost + bom.hardwareCost}\n")
        sb.append(",,,Labour Charge,,,,${bom.labourCost}\n")
        sb.append(",,,Total Estimated Cost,,,,${bom.totalCost}\n")
        outputFile.writeText(sb.toString())
    }

    private fun generateCuttingCsv(project: WardrobeProject, outputFile: File) {
        val schedule = CuttingScheduleGenerator.generateCuttingSchedule(project.wardrobeConfig)
        val sb = StringBuilder()
        sb.append("Part Name,Section,Quantity,Length (Inches),Length (Ft In),Width (Inches),Width (Ft In),Thickness (mm),Material,Finish,Edge Banding\n")
        schedule.forEach { p ->
            sb.append("\"${p.partName}\",\"${p.section}\",${p.quantity},${p.lengthInches},\"${p.lengthFtIn}\",${p.widthInches},\"${p.widthFtIn}\",${p.thicknessMm},\"${p.material}\",\"${p.finish}\",\"${p.edgeBanding}\"\n")
        }
        outputFile.writeText(sb.toString())
    }

    private fun generateProjectJson(project: WardrobeProject, outputFile: File) {
        val cfg = project.wardrobeConfig
        val json = """
{
  "application": "VisionSpace",
  "version": "2.0",
  "project": {
    "id": "${project.id}",
    "name": "${project.name}",
    "room": "${project.roomName}",
    "created_at": ${project.createdAt},
    "updated_at": ${project.updatedAt},
    "dimensions": {
      "width_inches": ${project.overallWidthInches},
      "height_inches": ${project.overallHeightInches},
      "depth_inches": ${project.overallDepthInches},
      "width_cm": ${cfg.widthCm},
      "height_cm": ${cfg.heightCm},
      "depth_cm": ${cfg.depthCm},
      "display_ft_in": "${project.formattedOverallDimensionsFtIn}"
    },
    "specification": {
      "sections_count": ${cfg.sectionsCount},
      "door_style": "${cfg.doorStyle.name}",
      "finish": "${cfg.finish.name}",
      "shelves_count": ${cfg.shelvesCount},
      "drawers_count": ${cfg.drawersCount},
      "hanging_rails_count": ${cfg.hangingRailsCount},
      "has_shoe_rack": ${cfg.hasShoeRack},
      "has_mirror_panel": ${cfg.hasMirrorPanel},
      "has_jewelry_tray": ${cfg.hasJewelryTray},
      "has_trouser_rack": ${cfg.hasTrouserRack},
      "lighting": "${cfg.ledLighting.name}",
      "handle_type": "${cfg.handleType.name}"
    },
    "measurement_confidence": "${project.siteCapture.confidence.name}"
  }
}
        """.trimIndent()
        outputFile.writeText(json)
    }
}
