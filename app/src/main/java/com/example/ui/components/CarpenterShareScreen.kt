package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.export.DxfExportService
import com.example.export.PdfExportService
import com.example.export.VideoExportManager
import com.example.export.ZipExportService
import com.example.model.BOMCalculator
import com.example.model.WardrobeProject
import com.example.spatial.TechnicalDrawingEngine
import com.example.spatial.TechnicalDrawingViewType
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Screen: Share Project Package with Carpenter / Contractor.
 * Direct one-tap export for Complete PDF, AutoCAD DXF, 2K Drawings, Project ZIP, and Presentation Video.
 */
@Composable
fun CarpenterShareScreen(
    project: WardrobeProject,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isExporting by remember { mutableStateOf(false) }
    var exportStatusText by remember { mutableStateOf("") }

    val bom = remember(project) { BOMCalculator.calculateBOM(project.wardrobeConfig) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("carpenter_share_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = "Share with Carpenter",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.size(42.dp))
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Project Hero Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(project.name, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Room: ${project.roomName}", color = TextSecondary, fontSize = 13.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(CyanPrimary)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(project.siteCapture.confidence.badgeLabel, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Dimensions: ${project.formattedOverallDimensionsFtIn}", color = CyanNeon, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Configuration: ${project.wardrobeConfig.sectionsCount} Sections • ${project.wardrobeConfig.doorStyle.title} • ${project.wardrobeConfig.finish.title}", color = TextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Included Package Assets Checklist
                    Text("INCLUDED FABRICATION DELIVERABLES:", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val deliverables = listOf(
                        "Site Photo & 3D Visualization Preview",
                        "Architectural Technical Drawing (Front, Interior, Plan, Side)",
                        "Detailed Bill of Materials (BOM) & Hardware Schedule",
                        "Factory Panel Cutting List (Inches & Ft-In)",
                        "Cost Estimate Recap (₹${bom.totalCost.toInt()})"
                    )

                    deliverables.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(item, color = TextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Share All Action: ZIP Package
            Card(
                onClick = {
                    if (!isExporting) {
                        isExporting = true
                        exportStatusText = "Preparing Project ZIP Package..."
                        coroutineScope.launch {
                            try {
                                val zipFile = withContext(Dispatchers.IO) {
                                    ZipExportService.createProjectZipPackage(context, project) { stage ->
                                        exportStatusText = stage
                                    }
                                }
                                shareFile(context, zipFile, "application/zip", "VisionSpace Project: ${project.name}")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            } finally {
                                isExporting = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("share_zip_package_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyanPrimary)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FolderZip, contentDescription = null, tint = Color.Black, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Share Complete Project Package (ZIP)", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Includes PDFs, DXF CAD, 2K Drawings, CSVs, and JSON", color = Color(0xCC000000), fontSize = 12.sp)
                    }
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("INDIVIDUAL EXPORT FORMATS", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            // Grid of individual export tiles:
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 1. Complete Project PDF
                ShareExportTile(
                    icon = Icons.Default.PictureAsPdf,
                    title = "Complete Specification PDF",
                    subtitle = "10-page drawing & estimate contractor booklet",
                    iconColor = Color(0xFFEF4444),
                    onClick = {
                        coroutineScope.launch {
                            isExporting = true
                            exportStatusText = "Generating Specification PDF..."
                            val file = File(context.cacheDir, "VisionSpace_${project.name}_Specification.pdf")
                            withContext(Dispatchers.IO) {
                                PdfExportService.generateCompleteProjectPdf(project, file)
                            }
                            isExporting = false
                            shareFile(context, file, "application/pdf", "VisionSpace PDF: ${project.name}")
                        }
                    }
                )

                // 2. AutoCAD DXF
                ShareExportTile(
                    icon = Icons.Default.Architecture,
                    title = "AutoCAD DXF Vector Drawing",
                    subtitle = "Precision CAD file in inches with layered elevations",
                    iconColor = Color(0xFF06B6D4),
                    onClick = {
                        coroutineScope.launch {
                            isExporting = true
                            exportStatusText = "Generating AutoCAD DXF..."
                            val file = File(context.cacheDir, "VisionSpace_${project.name}_CAD.dxf")
                            withContext(Dispatchers.IO) {
                                DxfExportService.exportDxfFile(project, file)
                            }
                            isExporting = false
                            shareFile(context, file, "application/dxf", "VisionSpace CAD: ${project.name}")
                        }
                    }
                )

                // 3. 2K Drawing Image (JPG)
                ShareExportTile(
                    icon = Icons.Default.Image,
                    title = "2K Technical Drawing Blueprint",
                    subtitle = "High-resolution architectural drawing image",
                    iconColor = Color(0xFF3B82F6),
                    onClick = {
                        coroutineScope.launch {
                            isExporting = true
                            exportStatusText = "Rendering 2K Blueprint..."
                            val bmp = withContext(Dispatchers.IO) {
                                TechnicalDrawingEngine.generateDrawingBitmap(project, TechnicalDrawingViewType.ALL_IN_ONE)
                            }
                            val file = File(context.cacheDir, "VisionSpace_${project.name}_Drawing.jpg")
                            withContext(Dispatchers.IO) {
                                FileOutputStream(file).use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, it) }
                            }
                            isExporting = false
                            shareFile(context, file, "image/jpeg", "VisionSpace Drawing: ${project.name}")
                        }
                    }
                )

                // 4. Presentation Video
                ShareExportTile(
                    icon = Icons.Default.VideoLibrary,
                    title = "10s Presentation Video (MP4)",
                    subtitle = "Animated zoom, interior reveal & estimate title card",
                    iconColor = Color(0xFF8B5CF6),
                    onClick = {
                        coroutineScope.launch {
                            isExporting = true
                            exportStatusText = "Encoding MP4 Video..."
                            try {
                                val file = VideoExportManager.generatePresentationVideo(context, project) { progress, status ->
                                    exportStatusText = status
                                }
                                shareFile(context, file, "video/mp4", "VisionSpace Video: ${project.name}")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Video error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            } finally {
                                isExporting = false
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Loading Overlay
        if (isExporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDD000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CyanNeon)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(exportStatusText, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun ShareExportTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = TextSecondary, fontSize = 11.sp)
            }
            Icon(Icons.Default.Share, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
    try {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        // Fallback generic send
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_TEXT, "VisionSpace Project: $title")
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }
}
