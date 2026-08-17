package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DoorStyle
import com.example.model.FinishType
import com.example.model.LedLighting
import com.example.model.WardrobeConfig
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldLaser
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModularConfigSheet(
    config: WardrobeConfig,
    onDimensionsChange: (w: Float, h: Float, d: Float) -> Unit,
    onFinishChange: (FinishType) -> Unit,
    onDoorStyleChange: (DoorStyle) -> Unit,
    onModulesChange: (shelves: Int?, rails: Int?, drawers: Int?, mirror: Boolean?, led: LedLighting?) -> Unit,
    onOpenBOM: () -> Unit,
    onSaveDesign: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    var customWidth by remember(config.widthCm) { mutableFloatStateOf(config.widthCm) }
    var customHeight by remember(config.heightCm) { mutableFloatStateOf(config.heightCm) }
    var customDepth by remember(config.depthCm) { mutableFloatStateOf(config.depthCm) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(TextMuted.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Modular Wardrobe Architect",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time parametric adjustments & spatial validation",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Dimensions Section
            SectionHeader(title = "SPATIAL DIMENSIONS (CM)")

            DimensionSlider(
                label = "Width",
                value = customWidth,
                range = 80f..320f,
                unit = "cm",
                testTag = "slider_width",
                onValueChange = {
                    customWidth = it
                    onDimensionsChange(customWidth, customHeight, customDepth)
                }
            )

            DimensionSlider(
                label = "Height",
                value = customHeight,
                range = 160f..270f,
                unit = "cm",
                testTag = "slider_height",
                onValueChange = {
                    customHeight = it
                    onDimensionsChange(customWidth, customHeight, customDepth)
                }
            )

            DimensionSlider(
                label = "Depth",
                value = customDepth,
                range = 40f..80f,
                unit = "cm",
                testTag = "slider_depth",
                onValueChange = {
                    customDepth = it
                    onDimensionsChange(customWidth, customHeight, customDepth)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Material Finishes Section
            SectionHeader(title = "MATERIAL & TEXTURE FINISH")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FinishType.values().forEach { finish ->
                    val isSelected = config.finish == finish
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) ObsidianSurfaceVariant else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) CyanNeon else GlassBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onFinishChange(finish) }
                            .padding(8.dp)
                            .testTag("finish_${finish.name}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            finish.primaryColor,
                                            finish.secondaryColor
                                        )
                                    )
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) CyanNeon else Color.White.copy(alpha = 0.3f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (finish == FinishType.ARCTIC_WHITE) Color.Black else CyanNeon,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = finish.title,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Door Mechanism Section
            SectionHeader(title = "DOOR ARCHITECTURE")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DoorStyle.values().forEach { style ->
                    val isSelected = config.doorStyle == style
                    FilterChip(
                        selected = isSelected,
                        onClick = { onDoorStyleChange(style) },
                        label = { Text(style.title, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanNeon,
                            selectedLabelColor = Color.Black,
                            containerColor = ObsidianSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) CyanNeon else GlassBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Interior Modular Elements
            SectionHeader(title = "INTERIOR MODULES & DRAWERS")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CounterControl(
                    label = "Shelves",
                    count = config.shelvesCount,
                    onIncrement = { onModulesChange(config.shelvesCount + 1, null, null, null, null) },
                    onDecrement = { if (config.shelvesCount > 1) onModulesChange(config.shelvesCount - 1, null, null, null, null) }
                )

                CounterControl(
                    label = "Drawers",
                    count = config.drawersCount,
                    onIncrement = { onModulesChange(null, null, config.drawersCount + 1, null, null) },
                    onDecrement = { if (config.drawersCount > 0) onModulesChange(null, null, config.drawersCount - 1, null, null) }
                )

                CounterControl(
                    label = "Hanging Rails",
                    count = config.hangingRailsCount,
                    onIncrement = { onModulesChange(null, config.hangingRailsCount + 1, null, null, null) },
                    onDecrement = { if (config.hangingRailsCount > 0) onModulesChange(null, config.hangingRailsCount - 1, null, null, null) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. LED Lighting Selection
            SectionHeader(title = "INTEGRATED LED ILLUMINATION")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LedLighting.values().forEach { led ->
                    val isSelected = config.ledLighting == led
                    FilterChip(
                        selected = isSelected,
                        onClick = { onModulesChange(null, null, null, null, led) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = if (led == LedLighting.NONE) TextMuted else led.color,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("${led.title} (${led.tempKelvin})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (led == LedLighting.CYAN_HOLOGRAPHIC) CyanNeon else ObsidianSurfaceVariant,
                            selectedLabelColor = if (led == LedLighting.CYAN_HOLOGRAPHIC) Color.Black else TextPrimary,
                            containerColor = ObsidianSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) CyanNeon else GlassBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Calculate Bill of Materials (BOM) CTA
            Button(
                onClick = {
                    onDismiss()
                    onOpenBOM()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_sheet_open_bom"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ObsidianSurfaceVariant,
                    contentColor = CyanNeon
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanNeon),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Calculate Bill of Materials (BOM)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save Design CTA (Local Room DB - 0 Login required)
            Button(
                onClick = {
                    onSaveDesign(config.name)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_config_db"),
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Design to Local Vault",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = CyanNeon,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun DimensionSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    testTag: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = TextSecondary, fontSize = 12.sp)

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Stepper -
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(ObsidianSurfaceVariant)
                        .clickable { onValueChange((value - 5f).coerceIn(range.start, range.endInclusive)) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Minus",
                        tint = CyanNeon,
                        modifier = Modifier.size(12.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "${value.toInt()} $unit",
                    color = CyanNeon,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Stepper +
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(ObsidianSurfaceVariant)
                        .clickable { onValueChange((value + 5f).coerceIn(range.start, range.endInclusive)) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Plus",
                        tint = CyanNeon,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.testTag(testTag),
            colors = SliderDefaults.colors(
                thumbColor = CyanNeon,
                activeTrackColor = CyanNeon,
                inactiveTrackColor = ObsidianSurfaceVariant
            )
        )
    }
}

@Composable
private fun CounterControl(
    label: String,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Text(text = label, color = TextSecondary, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onDecrement, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    tint = CyanNeon,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = count.toString(),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(onClick = onIncrement, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = CyanNeon,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
