package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DimensionFormatter
import com.example.model.RoomMeasurement
import com.example.model.UnitSystem
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldLaser
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Screen 8: AutoFit Screen
 * Wall dimension calibration matching screenshot:
 * Top clearance, Side clearances, Wardrobe width,
 * Stats card: Available wall | Recommended, and [ Apply ] button.
 */
@Composable
fun AutoFitScreen(
    roomMeasurement: RoomMeasurement,
    onBack: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
    unitSystem: UnitSystem = UnitSystem.FEET_INCHES
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("autofit_screen")
    ) {
        // 1. Top Header: [←  AutoFit  ⋮]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x88000000))
                        .testTag("autofit_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "AutoFit",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x88000000))
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = TextPrimary
                )
            }
        }

        // 2. Wall Calibration Schematic with Dimension Labels
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 70.dp, bottom = 180.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val wallLeft = w * 0.12f
                val wallRight = w * 0.88f
                val wallTop = h * 0.16f
                val wallBottom = h * 0.78f

                // Detected Wall Outline (Cyan dashed boundary)
                drawRect(
                    color = Color(0xFF38BDF8).copy(alpha = 0.5f),
                    topLeft = Offset(wallLeft, wallTop),
                    size = androidx.compose.ui.geometry.Size(wallRight - wallLeft, wallBottom - wallTop),
                    style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
                )

                // Wardrobe Inset Box inside wall
                val marginSide = (wallRight - wallLeft) * 0.10f
                val marginTop = (wallBottom - wallTop) * 0.14f

                val wardLeft = wallLeft + marginSide
                val wardRight = wallRight - marginSide
                val wardTop = wallTop + marginTop
                val wardBottom = wallBottom

                // Solid wardrobe carcass representation
                drawRect(
                    color = Color(0xFF5C4033),
                    topLeft = Offset(wardLeft, wardTop),
                    size = androidx.compose.ui.geometry.Size(wardRight - wardLeft, wardBottom - wardTop)
                )

                // Wardrobe Cyan bounding line
                drawRect(
                    color = CyanNeon,
                    topLeft = Offset(wardLeft, wardTop),
                    size = androidx.compose.ui.geometry.Size(wardRight - wardLeft, wardBottom - wardTop),
                    style = Stroke(width = 2.5f)
                )

                // Green/Cyan Dimension Lines
                val laserColor = Color(0xFF34D399)

                // Top clearance line
                val midX = (wardLeft + wardRight) / 2f
                drawLine(
                    color = laserColor,
                    start = Offset(midX, wallTop + 6f),
                    end = Offset(midX, wardTop - 6f),
                    strokeWidth = 2f
                )

                // Left margin line
                val midY = (wardTop + wardBottom) / 2f
                drawLine(
                    color = laserColor,
                    start = Offset(wallLeft + 6f, midY),
                    end = Offset(wardLeft - 6f, midY),
                    strokeWidth = 2f
                )

                // Right margin line
                drawLine(
                    color = laserColor,
                    start = Offset(wardRight + 6f, midY),
                    end = Offset(wallRight - 6f, midY),
                    strokeWidth = 2f
                )

                // Bottom width line
                drawLine(
                    color = laserColor,
                    start = Offset(wardLeft, wardBottom + 20f),
                    end = Offset(wardRight, wardBottom + 20f),
                    strokeWidth = 2f
                )
            }

            // Dimension Overlay Badges
            val topClrText = DimensionFormatter.format(roomMeasurement.topClearanceCm, unitSystem, compact = true)
            val sideClrText = DimensionFormatter.format(roomMeasurement.sideClearanceCm, unitSystem, compact = true)
            val recWText = DimensionFormatter.format(roomMeasurement.recommendedWidthCm, unitSystem, compact = true)

            // Top clearance
            Text(
                text = topClrText,
                color = Color(0xFF34D399),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
            )

            // Left margin
            Text(
                text = sideClrText,
                color = Color(0xFF34D399),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 18.dp)
            )

            // Right margin
            Text(
                text = sideClrText,
                color = Color(0xFF34D399),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 18.dp)
            )

            // Bottom width
            Text(
                text = recWText,
                color = Color(0xFF34D399),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 70.dp)
            )
        }

        // 3. Bottom Stats Card + [ Apply ] Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val wallFormatted = DimensionFormatter.formatRoomMeters(roomMeasurement.detectedWallWidthM, unitSystem)
            val recDimensionsFormatted = DimensionFormatter.formatDimensions(
                wCm = roomMeasurement.recommendedWidthCm,
                hCm = roomMeasurement.recommendedHeightCm,
                dCm = roomMeasurement.recommendedDepthCm,
                unitSystem = unitSystem,
                compact = true
            )

            // Stats Card: Available wall | Recommended
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xDD111827)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Available wall",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Straighten,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = wallFormatted,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Recommended",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = recDimensionsFormatted,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Apply Button
            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("apply_autofit_button"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text(
                    text = "Apply",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
