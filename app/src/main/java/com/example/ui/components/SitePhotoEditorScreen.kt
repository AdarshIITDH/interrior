package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DoorSliding
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BOMCalculator
import com.example.model.DoorStyle
import com.example.model.FinishType
import com.example.model.LedLighting
import com.example.model.MeasurementConfidence
import com.example.model.UnitSystem
import com.example.model.WardrobeConfig
import com.example.model.WardrobeProject
import com.example.spatial.ARPlacementState
import com.example.spatial.DeviceOrientationState
import com.example.spatial.SpatialRenderer
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class SiteEditorTab(val title: String) {
    LAYOUT("Layout"),
    FINISH("Finish"),
    INTERIOR("Interior"),
    SIZE("Size"),
    ESTIMATE("Estimate")
}

/**
 * Screen: Primary Static Site Photo Editor.
 * Lightweight, 0 lag, renders 3D parametric wardrobe directly on site photo.
 */
@Composable
fun SitePhotoEditorScreen(
    project: WardrobeProject,
    siteBitmap: Bitmap?,
    onUpdateProject: (WardrobeProject) -> Unit,
    onOpenTechnicalDrawing: () -> Unit,
    onOpenCarpenterShare: () -> Unit,
    onOpenVideoScreen: () -> Unit,
    onPreviewInLiveAR: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = project.wardrobeConfig
    val textMeasurer = rememberTextMeasurer()

    var selectedTab by remember { mutableStateOf(SiteEditorTab.LAYOUT) }
    var isNudgeDpadOpen by remember { mutableStateOf(false) }
    var isLightingAdjustOpen by remember { mutableStateOf(false) }
    var isVerifyModalOpen by remember { mutableStateOf(false) }

    // Position and perspective transforms
    var lateralOffset by remember { mutableFloatStateOf(project.siteOffsetNormalizedX) }
    var elevationOffset by remember { mutableFloatStateOf(project.siteOffsetNormalizedY) }
    var yawRotation by remember { mutableFloatStateOf(project.siteYawRotationDeg) }
    var distanceMeters by remember { mutableFloatStateOf(2.4f) }

    // Lighting parameters
    var brightness by remember { mutableFloatStateOf(project.siteCapture.brightness) }
    var shadowIntensity by remember { mutableFloatStateOf(project.siteCapture.shadowIntensity) }
    var warmth by remember { mutableFloatStateOf(project.siteCapture.warmth) }

    // Verification modal state
    var verifiedChecked by remember { mutableStateOf(project.siteCapture.confidence.isFabricationReady) }

    val bomResult = remember(config) { BOMCalculator.calculateBOM(config) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("site_photo_editor_screen")
    ) {
        // 1. Site Photo Layer
        if (siteBitmap != null) {
            val colorMatrix = remember(brightness, warmth) {
                ColorMatrix().apply {
                    setToScale(brightness * warmth, brightness, brightness * (2f - warmth), 1f)
                }
            }
            Image(
                bitmap = siteBitmap.asImageBitmap(),
                contentDescription = "Site Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(colorMatrix)
            )
        } else {
            // High-tech minimal blueprint grid room background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF090D16)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 60f
                    for (x in 0..size.width.toInt() step step.toInt()) {
                        drawLine(Color(0x1500F0FF), androidx.compose.ui.geometry.Offset(x.toFloat(), 0f), androidx.compose.ui.geometry.Offset(x.toFloat(), size.height), 1f)
                    }
                    for (y in 0..size.height.toInt() step step.toInt()) {
                        drawLine(Color(0x1500F0FF), androidx.compose.ui.geometry.Offset(0f, y.toFloat()), androidx.compose.ui.geometry.Offset(size.width, y.toFloat()), 1f)
                    }
                }
            }
        }

        // 2. Parametric 3D Wardrobe Projection Layer (Lightweight Canvas, 0 AR Core load)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Drag to move laterally & adjust elevation smoothly
                        lateralOffset = (lateralOffset + (dragAmount.x * 0.003f)).coerceIn(-1.5f, 1.5f)
                        elevationOffset = (elevationOffset - (dragAmount.y * 0.003f)).coerceIn(-1.0f, 1.0f)
                    }
                }
        ) {
            val placement = ARPlacementState(
                distanceMeters = distanceMeters,
                lateralOffsetMeters = lateralOffset,
                elevationOffsetMeters = elevationOffset - 0.2f,
                userRotationYDeg = yawRotation,
                showFloorGrid = false,
                showDimensions = true
            )
            val staticOrientation = DeviceOrientationState(yaw = 0f, pitch = 0.08f, roll = 0f)

            SpatialRenderer.renderWardrobeScene(
                drawScope = this,
                config = config,
                placement = placement,
                deviceOrientation = staticOrientation,
                textMeasurer = textMeasurer,
                unitSystem = UnitSystem.FEET_INCHES
            )
        }

        // 3. Top Header Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }

                // Center Title & Confidence Badge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = project.name,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                when (project.siteCapture.confidence) {
                                    MeasurementConfidence.USER_VERIFIED -> Color(0xFF065F46)
                                    MeasurementConfidence.AR_CALIBRATED -> Color(0xFF075985)
                                    MeasurementConfidence.MANUALLY_CALIBRATED -> Color(0xFF1E293B)
                                    MeasurementConfidence.VISUAL_ONLY -> Color(0xFF374151)
                                }
                            )
                            .clickable { isVerifyModalOpen = true }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = project.siteCapture.confidence.badgeLabel,
                            color = when (project.siteCapture.confidence) {
                                MeasurementConfidence.USER_VERIFIED -> Color(0xFF34D399)
                                MeasurementConfidence.AR_CALIBRATED -> CyanNeon
                                else -> TextSecondary
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Quick Action: Live AR preview
                IconButton(
                    onClick = onPreviewInLiveAR,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                ) {
                    Icon(Icons.Default.ViewInAr, contentDescription = "Live AR", tint = CyanNeon)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Floating Action Pills: [ Technical Drawing ] [ Estimate ] [ Video ] [ Share ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Technical Drawing
                EditorActionPill(
                    icon = Icons.Default.Architecture,
                    label = "Drawings",
                    onClick = {
                        if (project.siteCapture.confidence.isFabricationReady) {
                            onOpenTechnicalDrawing()
                        } else {
                            isVerifyModalOpen = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // Video Presentation
                EditorActionPill(
                    icon = Icons.Default.VideoLibrary,
                    label = "Video",
                    onClick = onOpenVideoScreen,
                    modifier = Modifier.weight(1f)
                )

                // Share Project
                EditorActionPill(
                    icon = Icons.Default.Share,
                    label = "Share",
                    onClick = onOpenCarpenterShare,
                    modifier = Modifier.weight(1f)
                )

                // Adjust Lighting
                IconButton(
                    onClick = { isLightingAdjustOpen = !isLightingAdjustOpen },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isLightingAdjustOpen) CyanPrimary else Color(0x99000000))
                ) {
                    Icon(
                        Icons.Default.WbSunny,
                        contentDescription = "Lighting",
                        tint = if (isLightingAdjustOpen) Color.Black else TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Nudge Dpad Toggle
                IconButton(
                    onClick = { isNudgeDpadOpen = !isNudgeDpadOpen },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isNudgeDpadOpen) CyanPrimary else Color(0x99000000))
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Nudge",
                        tint = if (isNudgeDpadOpen) Color.Black else TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Lighting Adjustment Popover
        AnimatedVisibility(
            visible = isLightingAdjustOpen,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 110.dp, end = 16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEE0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier.width(220.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Site Lighting", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Brightness", color = TextSecondary, fontSize = 10.sp)
                    Slider(
                        value = brightness,
                        onValueChange = { brightness = it },
                        valueRange = 0.5f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanNeon)
                    )

                    Text("Warmth", color = TextSecondary, fontSize = 10.sp)
                    Slider(
                        value = warmth,
                        onValueChange = { warmth = it },
                        valueRange = 0.8f..1.2f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD54F), activeTrackColor = Color(0xFFFFD54F))
                    )
                }
            }
        }

        // Floating Nudge D-Pad Controls
        AnimatedVisibility(
            visible = isNudgeDpadOpen,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xEE090D16)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
                modifier = Modifier.width(130.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Nudge Position", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Up
                    IconButton(onClick = { elevationOffset += 0.04f }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = CyanNeon)
                    }

                    // Left & Right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { lateralOffset -= 0.04f }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowLeft, contentDescription = "Left", tint = CyanNeon)
                        }
                        IconButton(onClick = { lateralOffset += 0.04f }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowRight, contentDescription = "Right", tint = CyanNeon)
                        }
                    }

                    // Down
                    IconButton(onClick = { elevationOffset -= 0.04f }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Down", tint = CyanNeon)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Rotation Yaw Steppers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { yawRotation -= 10f }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.RotateLeft, contentDescription = "Rot Left", tint = TextSecondary)
                        }
                        IconButton(onClick = { yawRotation += 10f }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.RotateRight, contentDescription = "Rot Right", tint = TextSecondary)
                        }
                    }
                }
            }
        }

        // Bottom Customization Studio & Dock
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    color = Color(0xF7090D16),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Dimension Pill & Cost Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = project.formattedOverallDimensionsFtIn,
                    color = CyanNeon,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "₹${bomResult.totalCost.toInt()}",
                    color = Color(0xFF34D399),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Studio Tabs: [ Layout ] [ Finish ] [ Interior ] [ Size ] [ Estimate ]
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color.Transparent,
                contentColor = CyanNeon,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        color = CyanNeon,
                        height = 2.dp
                    )
                }
            ) {
                SiteEditorTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.title,
                                color = if (selectedTab == tab) CyanNeon else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content Panes
            when (selectedTab) {
                SiteEditorTab.LAYOUT -> {
                    // Door Style & Sections
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            DoorStyle.SLIDING_DOOR to "Sliding",
                            DoorStyle.HINGED_DOOR to "Hinged",
                            DoorStyle.MIRROR_SLIDING_DOOR to "Mirror",
                            DoorStyle.OPEN_CONCEPT to "Open"
                        ).forEach { (style, label) ->
                            val isSel = config.doorStyle == style
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) CyanPrimary else Color(0xFF1E293B))
                                    .clickable {
                                        onUpdateProject(project.copy(wardrobeConfig = config.copy(doorStyle = style)))
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) Color.Black else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                SiteEditorTab.FINISH -> {
                    // Horizontal Swatches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(FinishType.WALNUT, FinishType.OAK, FinishType.TEAK, FinishType.WHITE, FinishType.GRAPHITE, FinishType.BEIGE).forEach { finish ->
                            val isSel = config.finish == finish
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onUpdateProject(project.copy(wardrobeConfig = config.copy(finish = finish)))
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(finish.primaryColor)
                                        .border(if (isSel) 2.dp else 1.dp, if (isSel) CyanNeon else Color(0x33FFFFFF), CircleShape)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(finish.title, color = if (isSel) CyanNeon else TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }

                SiteEditorTab.INTERIOR -> {
                    // Shelves & Drawers Quick Steppers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Drawers Stepper
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Drawers: ${config.drawersCount}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row {
                                    Text("-", color = CyanNeon, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                                        if (config.drawersCount > 0) onUpdateProject(project.copy(wardrobeConfig = config.copy(drawersCount = config.drawersCount - 1)))
                                    }.padding(horizontal = 6.dp))
                                    Text("+", color = CyanNeon, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                                        if (config.drawersCount < 6) onUpdateProject(project.copy(wardrobeConfig = config.copy(drawersCount = config.drawersCount + 1)))
                                    }.padding(horizontal = 6.dp))
                                }
                            }
                        }

                        // Shelves Stepper
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Shelves: ${config.shelvesCount}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row {
                                    Text("-", color = CyanNeon, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                                        if (config.shelvesCount > 2) onUpdateProject(project.copy(wardrobeConfig = config.copy(shelvesCount = config.shelvesCount - 1)))
                                    }.padding(horizontal = 6.dp))
                                    Text("+", color = CyanNeon, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                                        if (config.shelvesCount < 12) onUpdateProject(project.copy(wardrobeConfig = config.copy(shelvesCount = config.shelvesCount + 1)))
                                    }.padding(horizontal = 6.dp))
                                }
                            }
                        }
                    }
                }

                SiteEditorTab.SIZE -> {
                    // Dimension Steppers (Width, Height, Depth in ft/in)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Width Adjust
                        DimensionAdjustPill(
                            label = "Width",
                            valueCm = config.widthCm,
                            onAdjust = { deltaCm ->
                                val newW = (config.widthCm + deltaCm).coerceIn(100f, 400f)
                                onUpdateProject(project.copy(wardrobeConfig = config.copy(widthCm = newW)))
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Height Adjust
                        DimensionAdjustPill(
                            label = "Height",
                            valueCm = config.heightCm,
                            onAdjust = { deltaCm ->
                                val newH = (config.heightCm + deltaCm).coerceIn(150f, 280f)
                                onUpdateProject(project.copy(wardrobeConfig = config.copy(heightCm = newH)))
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Depth Adjust
                        DimensionAdjustPill(
                            label = "Depth",
                            valueCm = config.depthCm,
                            onAdjust = { deltaCm ->
                                val newD = (config.depthCm + deltaCm).coerceIn(45f, 90f)
                                onUpdateProject(project.copy(wardrobeConfig = config.copy(depthCm = newD)))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                SiteEditorTab.ESTIMATE -> {
                    // Instant Cost Summary Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Plywood + Finishes: ₹${(bomResult.materialsCost).toInt()}", color = TextSecondary, fontSize = 11.sp)
                            Text("Hardware & Labour: ₹${(bomResult.hardwareCost + bomResult.labourCost).toInt()}", color = TextSecondary, fontSize = 11.sp)
                        }
                        Button(
                            onClick = onOpenCarpenterShare,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            Text("View Full BOM & Share", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Critical Measurement Verification Dialog
        if (isVerifyModalOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000))
                    .clickable { isVerifyModalOpen = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Straighten, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Confirm Site Dimensions", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("To generate fabrication-grade technical drawings, confirm actual on-site tape measurements.", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Verified Dimensions Matrix
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Wall Width: ${project.formattedOverallDimensionsFtIn.split("×")[0].trim()}", color = TextPrimary, fontSize = 13.sp)
                                Text("Ceiling Height: ${project.formattedOverallDimensionsFtIn.split("×").getOrNull(1)?.trim() ?: ""}", color = TextPrimary, fontSize = 13.sp)
                                Text("Available Depth: ${project.formattedOverallDimensionsFtIn.split("×").getOrNull(2)?.trim() ?: ""}", color = TextPrimary, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { verifiedChecked = !verifiedChecked }
                        ) {
                            Checkbox(
                                checked = verifiedChecked,
                                onCheckedChange = { verifiedChecked = it },
                                colors = CheckboxDefaults.colors(checkedColor = CyanNeon, checkmarkColor = Color.Black)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("I have verified these dimensions on site", color = TextPrimary, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                val updatedCapture = project.siteCapture.copy(
                                    confidence = if (verifiedChecked) MeasurementConfidence.USER_VERIFIED else project.siteCapture.confidence
                                )
                                onUpdateProject(project.copy(siteCapture = updatedCapture))
                                isVerifyModalOpen = false
                                onOpenTechnicalDrawing()
                            },
                            enabled = verifiedChecked,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            Text("Confirm & Generate Drawing", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditorActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0x990F172A))
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun DimensionAdjustPill(
    label: String,
    valueCm: Float,
    onAdjust: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val inches = valueCm / 2.54f
    val feet = (inches / 12).toInt()
    val remInches = (inches % 12).toInt()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .padding(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = TextSecondary, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text("$feet' $remInches\"", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("-", color = CyanNeon, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onAdjust(-5f) }.padding(horizontal = 6.dp))
                Text("+", color = CyanNeon, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onAdjust(5f) }.padding(horizontal = 6.dp))
            }
        }
    }
}
