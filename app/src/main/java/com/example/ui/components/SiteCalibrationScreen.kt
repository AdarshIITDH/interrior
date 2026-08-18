package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ImagePoint
import com.example.model.MeasurementConfidence
import com.example.model.SiteCalibration
import com.example.model.SiteCapture
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Screen: Mode B - Gallery & Manual Site Calibration.
 * Allows choosing Quick Visual Placement OR Calibrating Space with 4-point wall quadrilateral and known tape measurement.
 */
@Composable
fun SiteCalibrationScreen(
    siteBitmap: Bitmap?,
    initialCapture: SiteCapture,
    onCompleteCalibration: (SiteCapture) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isCalibrateMode by remember { mutableStateOf(false) }

    // 4 Corner Points for Wall Quad Calibration (normalized 0..1)
    var pBottomLeft by remember { mutableStateOf(ImagePoint(0.15f, 0.82f)) }
    var pBottomRight by remember { mutableStateOf(ImagePoint(0.85f, 0.82f)) }
    var pTopLeft by remember { mutableStateOf(ImagePoint(0.15f, 0.18f)) }
    var pTopRight by remember { mutableStateOf(ImagePoint(0.85f, 0.18f)) }

    var wallWidthFeet by remember { mutableIntStateOf(10) }
    var wallWidthInches by remember { mutableIntStateOf(6) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        // Site Image Background
        if (siteBitmap != null) {
            Image(
                bitmap = siteBitmap.asImageBitmap(),
                contentDescription = "Site Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                Text("Site Photo", color = TextSecondary)
            }
        }

        // Overlay Cyan Quad Polygon if in Calibrate Mode
        if (isCalibrateMode) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            val width = size.width.toFloat()
                            val height = size.height.toFloat()
                            val pos = change.position
                            val normX = (pos.x / width).coerceIn(0.05f, 0.95f)
                            val normY = (pos.y / height).coerceIn(0.05f, 0.95f)

                            // Find closest pin to drag
                            val distBL = Math.hypot((normX - pBottomLeft.x).toDouble(), (normY - pBottomLeft.y).toDouble())
                            val distBR = Math.hypot((normX - pBottomRight.x).toDouble(), (normY - pBottomRight.y).toDouble())
                            val distTL = Math.hypot((normX - pTopLeft.x).toDouble(), (normY - pTopLeft.y).toDouble())
                            val distTR = Math.hypot((normX - pTopRight.x).toDouble(), (normY - pTopRight.y).toDouble())

                            val minDist = minOf(distBL, distBR, distTL, distTR)
                            when (minDist) {
                                distBL -> pBottomLeft = ImagePoint(normX, normY)
                                distBR -> pBottomRight = ImagePoint(normX, normY)
                                distTL -> pTopLeft = ImagePoint(normX, normY)
                                distTR -> pTopRight = ImagePoint(normX, normY)
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                val bl = Offset(pBottomLeft.x * w, pBottomLeft.y * h)
                val br = Offset(pBottomRight.x * w, pBottomRight.y * h)
                val tr = Offset(pTopRight.x * w, pTopRight.y * h)
                val tl = Offset(pTopLeft.x * w, pTopLeft.y * h)

                val quadPath = Path().apply {
                    moveTo(bl.x, bl.y)
                    lineTo(br.x, br.y)
                    lineTo(tr.x, tr.y)
                    lineTo(tl.x, tl.y)
                    close()
                }

                // Fill semi-transparent cyan
                drawPath(quadPath, CyanNeon.copy(alpha = 0.15f))
                // Stroke border
                drawPath(quadPath, CyanNeon, style = Stroke(width = 3f))

                // Draw corner handles
                listOf(bl, br, tr, tl).forEach { pt ->
                    drawCircle(CyanNeon, radius = 18f, center = pt)
                    drawCircle(Color.Black, radius = 8f, center = pt)
                }
            }
        }

        // Top Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xAA000000))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isCalibrateMode) "Space Calibration" else "Site Setup Mode",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.size(42.dp))
        }

        // Bottom Action Sheet
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    color = Color(0xFA090D16),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            if (!isCalibrateMode) {
                // Choice 1: Quick Visual Placement
                Card(
                    onClick = {
                        val updated = initialCapture.copy(
                            confidence = MeasurementConfidence.VISUAL_ONLY
                        )
                        onCompleteCalibration(updated)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_placement_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Quick Visual Placement", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("For visual design & styling. No measurements required.", color = TextSecondary, fontSize = 12.sp)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Choice 2: Calibrate Space
                Card(
                    onClick = { isCalibrateMode = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calibrate_space_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CyanNeon.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Straighten,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Calibrate Space", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Mark wall corners + enter 1 real measurement for scale.", color = TextSecondary, fontSize = 12.sp)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = CyanNeon)
                    }
                }
            } else {
                // Calibrate Quad Mode Active
                Text(
                    text = "1. Drag corner pins to align with your wall boundary.",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "2. Enter known real wall width:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Real Dimension Steppers (Feet & Inches)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Feet
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF111827))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("-", color = CyanNeon, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { if (wallWidthFeet > 4) wallWidthFeet-- }.padding(horizontal = 10.dp))
                            Text("$wallWidthFeet ft", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                            Text("+", color = CyanNeon, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { if (wallWidthFeet < 30) wallWidthFeet++ }.padding(horizontal = 10.dp))
                        }
                    }

                    // Inches
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF111827))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("-", color = CyanNeon, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { if (wallWidthInches > 0) wallWidthInches-- }.padding(horizontal = 10.dp))
                            Text("$wallWidthInches in", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                            Text("+", color = CyanNeon, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { if (wallWidthInches < 11) wallWidthInches++ }.padding(horizontal = 10.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Complete Calibration Button
                Button(
                    onClick = {
                        val totalInches = (wallWidthFeet * 12) + wallWidthInches
                        val totalCm = totalInches * 2.54f

                        val manualCal = SiteCalibration(
                            topLeft = pTopLeft,
                            topRight = pTopRight,
                            bottomRight = pBottomRight,
                            bottomLeft = pBottomLeft,
                            referenceWallWidthCm = totalCm,
                            isCalibrated = true
                        )

                        val updated = initialCapture.copy(
                            manualCalibration = manualCal,
                            confidence = MeasurementConfidence.MANUALLY_CALIBRATED
                        )
                        onCompleteCalibration(updated)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("apply_calibration_btn"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Apply Calibration & Design", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
