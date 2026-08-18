package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

/**
 * Screen 3: Interactive AR Area Selection & Space Measurement Screen
 * Lets the user select/drag wall corner pins, dynamically calculate room width/height/depth,
 * and immediately generate a tailored custom wardrobe for that exact space!
 */
@Composable
fun ARScanScreen(
    roomMeasurement: RoomMeasurement,
    isTorchOn: Boolean,
    onToggleTorch: () -> Unit,
    onCloseScan: () -> Unit,
    onScanReady: () -> Unit,
    onGenerateCustomWardrobe: (widthM: Float, heightM: Float, depthM: Float) -> Unit,
    unitSystem: UnitSystem = UnitSystem.FEET_INCHES,
    onToggleUnit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var wallWidthM by remember { mutableFloatStateOf(roomMeasurement.detectedWallWidthM) }
    var wallHeightM by remember { mutableFloatStateOf(roomMeasurement.detectedHeightM) }
    var wallDepthM by remember { mutableFloatStateOf(roomMeasurement.detectedDepthM) }

    val infiniteTransition = rememberInfiniteTransition(label = "laser_scan")
    val laserPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pulse"
    )

    val usableAreaSqm = wallWidthM * wallHeightM
    val recommendedWardrobeWidthCm = ((wallWidthM * 100f) - 20f).coerceIn(80f, 320f).roundToInt()
    val recommendedWardrobeHeightCm = ((wallHeightM * 100f) - 15f).coerceIn(180f, 260f).roundToInt()

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("ar_scan_screen")
    ) {
        // 1. Top Controls Bar: [Close] [Interactive Laser Measurement] [Unit Switcher] [Torch]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCloseScan,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x88000000))
                    .testTag("close_scan_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xCC0F172A))
                    .border(1.dp, Color(0x3338BDF8), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = "Laser Measure",
                        tint = CyanNeon,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Area Selection & Sizing",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Unit Switcher Button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CyanPrimary.copy(alpha = 0.2f))
                        .border(1.dp, CyanNeon, CircleShape)
                        .clickable { onToggleUnit() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("scan_unit_toggle_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📐 ${unitSystem.shortLabel}",
                        color = CyanNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onToggleTorch,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x88000000))
                        .testTag("torch_button")
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flashlight",
                        tint = if (isTorchOn) CyanNeon else TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 2. Interactive Drag Canvas for Area Selection
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaWidth = dragAmount.x / 300f
                        val deltaHeight = -dragAmount.y / 300f
                        wallWidthM = (wallWidthM + deltaWidth).coerceIn(1.20f, 4.50f)
                        wallHeightM = (wallHeightM + deltaHeight).coerceIn(1.80f, 3.20f)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height

                // Selected Wall Bounding Box
                val boxWidthPx = (canvasW * 0.76f) * (wallWidthM / 2.6f).coerceIn(0.6f, 1.15f)
                val boxHeightPx = (canvasH * 0.44f) * (wallHeightM / 2.5f).coerceIn(0.6f, 1.15f)

                val left = (canvasW - boxWidthPx) / 2f
                val right = left + boxWidthPx
                val top = canvasH * 0.16f
                val bottom = top + boxHeightPx

                // Semi-transparent wall area overlay
                drawRect(
                    color = CyanNeon.copy(alpha = 0.08f * laserPulse),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(boxWidthPx, boxHeightPx)
                )

                // Neon laser boundary box with dash effect
                drawRect(
                    color = CyanNeon.copy(alpha = 0.85f),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(boxWidthPx, boxHeightPx),
                    style = Stroke(
                        width = 2.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                    )
                )

                // 4 Interactive Corner Pins
                val pinRadius = 7f
                val cornerPins = listOf(
                    Offset(left, top),
                    Offset(right, top),
                    Offset(right, bottom),
                    Offset(left, bottom)
                )

                for (pin in cornerPins) {
                    drawCircle(CyanNeon, radius = pinRadius, center = pin)
                    drawCircle(Color.White, radius = pinRadius * 0.5f, center = pin)
                }

                // Laser Dimension Callout Lines: Width (Top) & Height (Right)
                drawLine(
                    color = EmeraldLaser,
                    start = Offset(left, top - 18f),
                    end = Offset(right, top - 18f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = EmeraldLaser,
                    start = Offset(left, top - 26f),
                    end = Offset(left, top - 10f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = EmeraldLaser,
                    start = Offset(right, top - 26f),
                    end = Offset(right, top - 10f),
                    strokeWidth = 2f
                )

                drawLine(
                    color = EmeraldLaser,
                    start = Offset(right + 18f, top),
                    end = Offset(right + 18f, bottom),
                    strokeWidth = 2f
                )
                drawLine(
                    color = EmeraldLaser,
                    start = Offset(right + 10f, top),
                    end = Offset(right + 26f, top),
                    strokeWidth = 2f
                )
                drawLine(
                    color = EmeraldLaser,
                    start = Offset(right + 10f, bottom),
                    end = Offset(right + 26f, bottom),
                    strokeWidth = 2f
                )
            }
        }

        // 3. Dynamic Dimension Badges over Canvas
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val widthFormatted = DimensionFormatter.formatLength(wallWidthM * 100f, unitSystem, compact = true)
            val heightFormatted = DimensionFormatter.formatLength(wallHeightM * 100f, unitSystem, compact = true)
            val secondaryDims = if (unitSystem != UnitSystem.CENTIMETERS) {
                "(${(wallWidthM * 100).roundToInt()} × ${(wallHeightM * 100).roundToInt()} cm)"
            } else ""

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xDD0F172A))
                    .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "↔ Width: $widthFormatted  •  ↕ Height: $heightFormatted $secondaryDims",
                    color = TextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Drag area on screen to adjust wall bounds",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        // 4. Bottom Measurement Suite & Auto-Generate Wardrobe Panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Quick Space Presets Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Compact" to 1.60f,
                    "2-Door" to 2.00f,
                    "Standard" to 2.40f,
                    "Master" to 2.80f,
                    "Wide Wall" to 3.20f
                ).forEach { (label, presetW) ->
                    val isSelected = (wallWidthM * 100).roundToInt() == (presetW * 100).roundToInt()
                    val presetDimStr = DimensionFormatter.formatLength(presetW * 100f, unitSystem, compact = true)
                    val fullPresetLabel = "$label ($presetDimStr)"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) CyanPrimary.copy(alpha = 0.35f) else Color(0xCC1E293B))
                            .border(
                                1.dp,
                                if (isSelected) CyanNeon else Color(0x33FFFFFF),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { wallWidthM = presetW }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = fullPresetLabel,
                            color = if (isSelected) CyanNeon else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Calculated Space & Recommended Wardrobe Fit Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("measurement_summary_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEE090D16)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3338BDF8))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SquareFoot,
                                contentDescription = "Space Area",
                                tint = CyanNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Selected Wall Space",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "${DimensionFormatter.formatArea(usableAreaSqm, unitSystem)} area",
                            color = CyanNeon,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dimension Stepper Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Width Stepper
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Width", color = TextMuted, fontSize = 11.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { wallWidthM = (wallWidthM - 0.10f).coerceIn(1.0f, 4.5f) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Minus", tint = TextPrimary, modifier = Modifier.size(16.dp))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = DimensionFormatter.formatLength(wallWidthM * 100f, unitSystem, compact = true),
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (unitSystem != UnitSystem.CENTIMETERS) {
                                        Text(
                                            text = "${(wallWidthM * 100).roundToInt()} cm",
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { wallWidthM = (wallWidthM + 0.10f).coerceIn(1.0f, 4.5f) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Plus", tint = TextPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Height Stepper
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Height", color = TextMuted, fontSize = 11.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { wallHeightM = (wallHeightM - 0.05f).coerceIn(1.8f, 3.2f) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Minus", tint = TextPrimary, modifier = Modifier.size(16.dp))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = DimensionFormatter.formatLength(wallHeightM * 100f, unitSystem, compact = true),
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (unitSystem != UnitSystem.CENTIMETERS) {
                                        Text(
                                            text = "${(wallHeightM * 100).roundToInt()} cm",
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { wallHeightM = (wallHeightM + 0.05f).coerceIn(1.8f, 3.2f) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Plus", tint = TextPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Depth Stepper
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Depth", color = TextMuted, fontSize = 11.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { wallDepthM = (wallDepthM - 0.05f).coerceIn(0.4f, 1.0f) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Minus", tint = TextPrimary, modifier = Modifier.size(16.dp))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = DimensionFormatter.formatLength(wallDepthM * 100f, unitSystem, compact = true),
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (unitSystem != UnitSystem.CENTIMETERS) {
                                        Text(
                                            text = "${(wallDepthM * 100).roundToInt()} cm",
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { wallDepthM = (wallDepthM + 0.05f).coerceIn(0.4f, 1.0f) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Plus", tint = TextPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tailored Fit Recommendation Row
                    val fitWStr = DimensionFormatter.formatLength(recommendedWardrobeWidthCm.toFloat(), unitSystem, compact = true)
                    val fitHStr = DimensionFormatter.formatLength(recommendedWardrobeHeightCm.toFloat(), unitSystem, compact = true)
                    val fitDStr = DimensionFormatter.formatLength(60f, unitSystem, compact = true)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "✨ Calculated Wardrobe: $fitWStr W × $fitHStr H × $fitDStr D (${recommendedWardrobeWidthCm}×${recommendedWardrobeHeightCm}×60 cm with margins)",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary CTA: Generate & Fit Custom Wardrobe
                    Button(
                        onClick = {
                            onGenerateCustomWardrobe(wallWidthM, wallHeightM, wallDepthM)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_custom_wardrobe_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Generate",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate Custom Wardrobe For This Space",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen 4: Place Wardrobe Screen
 * "Wall detected ✓", "12′ 8″ (3.86 m) available", "Tap to place"
 */
@Composable
fun PlaceWardrobeScreen(
    roomMeasurement: RoomMeasurement,
    onClose: () -> Unit,
    onPlaceWardrobe: () -> Unit,
    unitSystem: UnitSystem = UnitSystem.FEET_INCHES,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("place_wardrobe_screen")
    ) {
        // 1. Top Close Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x88000000))
                    .testTag("close_place_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 2. Cyan Detected Wall Bounding Frame & Wall Info Box
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            val wallLeft = canvasW * 0.12f
            val wallRight = canvasW * 0.88f
            val wallTop = canvasH * 0.18f
            val wallBottom = canvasH * 0.72f

            // Wall Neon Boundary Box
            drawRect(
                color = CyanNeon.copy(alpha = 0.85f),
                topLeft = Offset(wallLeft, wallTop),
                size = androidx.compose.ui.geometry.Size(wallRight - wallLeft, wallBottom - wallTop),
                style = Stroke(width = 2.5f)
            )
        }

        // Wall Info Badge inside wall top
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Wall detected ✓",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            val wallAvailFormatted = DimensionFormatter.formatRoomMeters(roomMeasurement.detectedWallWidthM, unitSystem)
            val wallSecondary = if (unitSystem != UnitSystem.CENTIMETERS) "(${roomMeasurement.detectedWallWidthM} m)" else ""
            Text(
                text = "$wallAvailFormatted $wallSecondary available",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // 3. Center/Bottom Tap to Place Target
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(CyanPrimary.copy(alpha = 0.25f))
                    .border(2.dp, CyanNeon, CircleShape)
                    .clickable { onPlaceWardrobe() }
                    .testTag("tap_to_place_target"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = "Place",
                    tint = CyanNeon,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Tap to place",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
