package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ViewInAr
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BOMCalculator
import com.example.model.BOMCategory
import com.example.model.BOMItem
import com.example.model.DimensionFormatter
import com.example.model.UnitSystem
import com.example.model.WardrobeConfig
import com.example.ui.theme.CyanBorderActive
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldLaser
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextDisabled
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BOMReportDialog(
    config: WardrobeConfig,
    initialUnitSystem: UnitSystem = UnitSystem.FEET_INCHES,
    onDimensionsChange: (w: Float, h: Float, d: Float) -> Unit,
    onSaveToVault: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var widthVal by remember(config.widthCm) { mutableFloatStateOf(config.widthCm) }
    var heightVal by remember(config.heightCm) { mutableFloatStateOf(config.heightCm) }
    var depthVal by remember(config.depthCm) { mutableFloatStateOf(config.depthCm) }

    var selectedCategoryFilter by remember { mutableStateOf<BOMCategory?>(null) }
    var unitSystem by remember { mutableStateOf(initialUnitSystem) }

    // Live dynamic BOM calculation derived from current input dimensions
    val liveConfig = remember(config, widthVal, heightVal, depthVal) {
        config.copy(
            widthCm = widthVal,
            heightCm = heightVal,
            depthCm = depthVal
        )
    }

    val bomSummary = remember(liveConfig) {
        BOMCalculator.calculateBOM(liveConfig)
    }

    val filteredItems = remember(bomSummary, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) {
            bomSummary.items
        } else {
            bomSummary.items.filter { it.category == selectedCategoryFilter }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(TextMuted.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(CyanNeon, ElectricBlue)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Bill of Materials (BOM) & Specs",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${config.name} • Live Cost (INR ₹) & Specs",
                            color = CyanNeon,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Copy BOM button
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Wardrobe BOM", bomSummary.toFormattedMarkdown(unitSystem))
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "BOM copied with INR (₹) rates to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ObsidianSurfaceVariant)
                            .border(1.dp, GlassBorderSubtle, CircleShape)
                            .testTag("btn_copy_bom")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy BOM",
                            tint = CyanNeon,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ObsidianSurfaceVariant)
                            .border(1.dp, GlassBorderSubtle, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Interactive Dimension Studio Input Card with 3-way Unit Switcher
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ObsidianSurface)
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CUSTOM DIMENSIONS",
                                color = CyanNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            // 3-way Unit Switcher [ FT | IN | CM ]
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ObsidianSurfaceVariant)
                                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UnitSystem.values().forEach { u ->
                                    val isSelected = unitSystem == u
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) CyanNeon else Color.Transparent)
                                            .clickable { unitSystem = u }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = u.shortLabel,
                                            color = if (isSelected) Color.Black else TextDisabled,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Width, Height, Depth numeric & stepper controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DimensionInputBox(
                                label = "WIDTH",
                                valueCm = widthVal,
                                minCm = 80f,
                                maxCm = 320f,
                                unitSystem = unitSystem,
                                onValueChange = {
                                    widthVal = it
                                    onDimensionsChange(widthVal, heightVal, depthVal)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            DimensionInputBox(
                                label = "HEIGHT",
                                valueCm = heightVal,
                                minCm = 160f,
                                maxCm = 270f,
                                unitSystem = unitSystem,
                                onValueChange = {
                                    heightVal = it
                                    onDimensionsChange(widthVal, heightVal, depthVal)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            DimensionInputBox(
                                label = "DEPTH",
                                valueCm = depthVal,
                                minCm = 40f,
                                maxCm = 80f,
                                unitSystem = unitSystem,
                                onValueChange = {
                                    depthVal = it
                                    onDimensionsChange(widthVal, heightVal, depthVal)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Quick Slider for Width
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val wFormatted = DimensionFormatter.formatLength(widthVal, unitSystem)
                            val wCm = "${widthVal.toInt()} cm"
                            Text(
                                text = "Width Slider ($wFormatted / $wCm)",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Slider(
                            value = widthVal,
                            onValueChange = {
                                widthVal = it
                                onDimensionsChange(widthVal, heightVal, depthVal)
                            },
                            valueRange = 80f..320f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bom_slider_width"),
                            colors = SliderDefaults.colors(
                                thumbColor = CyanNeon,
                                activeTrackColor = CyanNeon,
                                inactiveTrackColor = ObsidianSurfaceVariant
                            )
                        )
                    }
                }

                // 2. High-Level Telemetry Summary Cards in Indian Rupees (INR)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(ObsidianSurfaceVariant, ObsidianSurface)
                                )
                            )
                            .border(1.dp, CyanBorderActive, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ESTIMATED TOTAL MATERIAL & HARDWARE COST",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = DimensionFormatter.formatCurrencyINR(bomSummary.totalEstimatedCostInr),
                                    color = EmeraldLaser,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(EmeraldLaser.copy(alpha = 0.15f))
                                    .border(1.dp, EmeraldLaser.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "READY FOR CUT LIST",
                                    color = EmeraldLaser,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Metric Pills Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SummaryStatPill(
                                label = "4×8 FT SHEETS",
                                value = "${bomSummary.totalSheetBoardsRequired} Boards",
                                sub = "Standard BWP / HDHMR",
                                modifier = Modifier.weight(1f)
                            )
                            SummaryStatPill(
                                label = "EDGE BANDING",
                                value = if (unitSystem == UnitSystem.CENTIMETERS) {
                                    "${bomSummary.totalEdgeBandingMeters.toInt()} Meters"
                                } else {
                                    "${(bomSummary.totalEdgeBandingMeters * 3.28f).toInt()} Feet"
                                },
                                sub = "2mm Impact PVC Profile",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SummaryStatPill(
                                label = "TOTAL WEIGHT",
                                value = "${bomSummary.totalWeightKg.toInt()} kg",
                                sub = "Structural Mass",
                                modifier = Modifier.weight(1f)
                            )
                            SummaryStatPill(
                                label = "ASSEMBLY TIME",
                                value = "${bomSummary.estimatedAssemblyHours} Hours",
                                sub = "Modular Flatpack",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3. Category Filter Chips
                item {
                    Column {
                        Text(
                            text = "ITEMIZED BILL OF MATERIALS (${filteredItems.size} ITEMS)",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // "All" chip
                            FilterChip(
                                selected = selectedCategoryFilter == null,
                                onClick = { selectedCategoryFilter = null },
                                label = { Text("All (${bomSummary.items.size})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanNeon,
                                    selectedLabelColor = Color.Black,
                                    containerColor = ObsidianSurfaceVariant,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedCategoryFilter == null,
                                    borderColor = if (selectedCategoryFilter == null) CyanNeon else GlassBorderSubtle
                                )
                            )

                            BOMCategory.values().forEach { cat ->
                                val isSelected = selectedCategoryFilter == cat
                                val count = bomSummary.items.count { it.category == cat }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategoryFilter = if (isSelected) null else cat },
                                    label = { Text("${cat.title} ($count)", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyanNeon,
                                        selectedLabelColor = Color.Black,
                                        containerColor = ObsidianSurfaceVariant,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) CyanNeon else GlassBorderSubtle
                                    )
                                )
                            }
                        }
                    }
                }

                // 4. Itemized BOM List in INR (₹)
                items(filteredItems) { item ->
                    BOMItemCard(item = item)
                }

                // 5. Action Buttons
                item {
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Apply & Return to 3D View
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_apply_bom_3d"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ObsidianSurfaceVariant,
                                contentColor = CyanNeon
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanBorderActive),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Apply to 3D AR",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // Save to Vault
                        Button(
                            onClick = {
                                onSaveToVault(liveConfig.name)
                                Toast.makeText(context, "Saved '${liveConfig.name}' with BOM to Vault", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_save_bom_vault"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanNeon,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Save to Vault",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DimensionInputBox(
    label: String,
    valueCm: Float,
    minCm: Float,
    maxCm: Float,
    unitSystem: UnitSystem,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayFormatted = DimensionFormatter.formatLength(valueCm, unitSystem, compact = true)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurfaceVariant)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = displayFormatted,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Stepper buttons (- / +)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(ObsidianSurface)
                    .border(1.dp, GlassBorderSubtle, CircleShape)
                    .clickable {
                        val step = when (unitSystem) {
                            UnitSystem.FEET_INCHES -> 2.54f * 2f // 2 inches
                            UnitSystem.INCHES -> 2.54f * 2f // 2 inches
                            UnitSystem.CENTIMETERS -> 5f
                        }
                        onValueChange((valueCm - step).coerceIn(minCm, maxCm))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    tint = CyanNeon,
                    modifier = Modifier.size(12.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(ObsidianSurface)
                    .border(1.dp, GlassBorderSubtle, CircleShape)
                    .clickable {
                        val step = when (unitSystem) {
                            UnitSystem.FEET_INCHES -> 2.54f * 2f // 2 inches
                            UnitSystem.INCHES -> 2.54f * 2f // 2 inches
                            UnitSystem.CENTIMETERS -> 5f
                        }
                        onValueChange((valueCm + step).coerceIn(minCm, maxCm))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = CyanNeon,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryStatPill(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianSurface)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = label, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(text = sub, color = TextSecondary, fontSize = 8.5.sp)
    }
}

@Composable
private fun BOMItemCard(item: BOMItem) {
    val categoryIcon: ImageVector = when (item.category) {
        BOMCategory.CARCASS_PANELS -> Icons.Default.Dashboard
        BOMCategory.DOORS_FACADES -> Icons.Default.DoorFront
        BOMCategory.INTERIOR_MODULES -> Icons.Default.Inventory2
        BOMCategory.HARDWARE_FASTENERS -> Icons.Default.Build
        BOMCategory.LIGHTING_ELECTRICAL -> Icons.Default.Lightbulb
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianSurfaceVariant)
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = item.name,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.dimensionSpec,
                    color = CyanNeon,
                    fontSize = 11.sp
                )
                Text(
                    text = item.material,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Price in INR & Qty
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${item.quantity} ${item.unit}",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = DimensionFormatter.formatCurrencyINR(item.totalCostInr),
                color = EmeraldLaser,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "@ ${DimensionFormatter.formatCurrencyINR(item.unitCostInr)}",
                color = TextMuted,
                fontSize = 9.sp
            )
        }
    }
}
