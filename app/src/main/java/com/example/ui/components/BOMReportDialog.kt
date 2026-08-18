package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BOMCalculator
import com.example.model.BOMCategory
import com.example.model.BOMItem
import com.example.model.DetailedWardrobeBOM
import com.example.model.DimensionFormatter
import com.example.model.HandleType
import com.example.model.LedLighting
import com.example.model.UnitSystem
import com.example.model.WardrobeConfig
import com.example.model.WardrobeCostRates
import com.example.ui.theme.AmberWarning
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
import java.util.Locale

enum class ActiveDimension(
    val label: String,
    val minCm: Float,
    val maxCm: Float,
    val tag: String
) {
    WIDTH("Width", 80f, 360f, "width"),
    HEIGHT("Height", 150f, 280f, "height"),
    DEPTH("Depth", 40f, 90f, "depth")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BOMReportDialog(
    config: WardrobeConfig,
    initialUnitSystem: UnitSystem = UnitSystem.FEET_INCHES,
    onDimensionsChange: (w: Float, h: Float, d: Float) -> Unit = { _, _, _ -> },
    onConfigChange: (WardrobeConfig) -> Unit = {},
    onSaveToVault: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Active dimension selection for the slider (Width, Height, or Depth)
    var activeDimension by remember { mutableStateOf(ActiveDimension.WIDTH) }

    // Live dimension parameters
    var widthVal by remember(config.widthCm) { mutableFloatStateOf(config.widthCm) }
    var heightVal by remember(config.heightCm) { mutableFloatStateOf(config.heightCm) }
    var depthVal by remember(config.depthCm) { mutableFloatStateOf(config.depthCm) }

    // Live add-ons & customizations
    var drawersVal by remember(config.drawersCount) { mutableIntStateOf(config.drawersCount) }
    var ledVal by remember(config.ledLighting) { mutableStateOf(config.ledLighting) }
    var handleVal by remember(config.handleStyle) { mutableStateOf(config.handleStyle) }
    var mirrorVal by remember(config.hasMirrorPanel) { mutableStateOf(config.hasMirrorPanel) }
    var shoeRackVal by remember(config.hasShoeRack) { mutableStateOf(config.hasShoeRack) }
    var jewelryTrayVal by remember(config.hasJewelryTray) { mutableStateOf(config.hasJewelryTray) }
    var trouserRackVal by remember(config.hasTrouserRack) { mutableStateOf(config.hasTrouserRack) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Costing Summary & Add-ons, 1: Itemized Cut List
    var selectedCategoryFilter by remember { mutableStateOf<BOMCategory?>(null) }
    var unitSystem by remember { mutableStateOf(initialUnitSystem) }
    var showFormulaBreakdown by remember { mutableStateOf(false) }

    // Live dynamic wardrobe configuration
    val liveConfig = remember(
        config, widthVal, heightVal, depthVal, drawersVal,
        ledVal, handleVal, mirrorVal, shoeRackVal, jewelryTrayVal, trouserRackVal
    ) {
        config.copy(
            widthCm = widthVal,
            heightCm = heightVal,
            depthCm = depthVal,
            drawersCount = drawersVal,
            ledLighting = ledVal,
            handleStyle = handleVal,
            hasMirrorPanel = mirrorVal,
            hasShoeRack = shoeRackVal,
            hasJewelryTray = jewelryTrayVal,
            hasTrouserRack = trouserRackVal
        )
    }

    val bomSummary = remember(liveConfig) {
        BOMCalculator.calculateBOM(liveConfig)
    }

    val detailed = bomSummary.detailedBOM

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
                .padding(horizontal = 16.dp)
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
                            .background(CyanNeon.copy(alpha = 0.15f))
                            .border(1.dp, CyanBorderActive, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Bespoke Wardrobe Costing",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${liveConfig.name} • ${DimensionFormatter.formatDimensions(widthVal, heightVal, depthVal, unitSystem, compact = false)}",
                            color = CyanNeon,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Wardrobe Costing Sheet", detailed.toFormattedMarkdown(unitSystem))
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Full Costing & BOM copied to clipboard!", Toast.LENGTH_SHORT).show()
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

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Tabs: [ Costing Summary & Add-ons | Itemized Cut List ]
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = ObsidianSurfaceVariant,
                contentColor = CyanNeon,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CyanNeon,
                        height = 3.dp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Costing Summary & Add-ons (₹)",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) CyanNeon else TextSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Itemized BOM (${filteredItems.size})",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) CyanNeon else TextSecondary
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // =========================================================================
                // 1. Interactive Dimension Studio (Tap Width/Height/Depth to vary slider)
                // =========================================================================
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ObsidianSurface)
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "WARDROBE DIMENSIONS",
                                    color = CyanNeon,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• Tap to select",
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }

                            // 3-way Unit Switcher [ FT | IN | CM ]
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ObsidianSurfaceVariant)
                                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 3.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UnitSystem.values().forEach { u ->
                                    val isSelected = unitSystem == u
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) CyanNeon else Color.Transparent)
                                            .clickable { unitSystem = u }
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
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

                        // Width, Height, Depth selectable input boxes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DimensionInputBox(
                                label = "WIDTH",
                                valueCm = widthVal,
                                minCm = 80f,
                                maxCm = 360f,
                                isSelected = activeDimension == ActiveDimension.WIDTH,
                                unitSystem = unitSystem,
                                onClick = { activeDimension = ActiveDimension.WIDTH },
                                onValueChange = {
                                    widthVal = it
                                    onDimensionsChange(widthVal, heightVal, depthVal)
                                    onConfigChange(liveConfig.copy(widthCm = widthVal))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            DimensionInputBox(
                                label = "HEIGHT",
                                valueCm = heightVal,
                                minCm = 150f,
                                maxCm = 280f,
                                isSelected = activeDimension == ActiveDimension.HEIGHT,
                                unitSystem = unitSystem,
                                onClick = { activeDimension = ActiveDimension.HEIGHT },
                                onValueChange = {
                                    heightVal = it
                                    onDimensionsChange(widthVal, heightVal, depthVal)
                                    onConfigChange(liveConfig.copy(heightCm = heightVal))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            DimensionInputBox(
                                label = "DEPTH",
                                valueCm = depthVal,
                                minCm = 40f,
                                maxCm = 90f,
                                isSelected = activeDimension == ActiveDimension.DEPTH,
                                unitSystem = unitSystem,
                                onClick = { activeDimension = ActiveDimension.DEPTH },
                                onValueChange = {
                                    depthVal = it
                                    onDimensionsChange(widthVal, heightVal, depthVal)
                                    onConfigChange(liveConfig.copy(depthCm = depthVal))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dynamic Slider bound to selected dimension (Width, Height, or Depth)
                        val currentVal = when (activeDimension) {
                            ActiveDimension.WIDTH -> widthVal
                            ActiveDimension.HEIGHT -> heightVal
                            ActiveDimension.DEPTH -> depthVal
                        }
                        val minRange = activeDimension.minCm
                        val maxRange = activeDimension.maxCm

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(CyanNeon)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SLIDER ADJUSTING ${activeDimension.label.uppercase()}",
                                    color = CyanNeon,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = "${DimensionFormatter.formatLength(currentVal, unitSystem, compact = false)} (${currentVal.toInt()} cm)",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = currentVal.coerceIn(minRange, maxRange),
                            onValueChange = { newVal ->
                                when (activeDimension) {
                                    ActiveDimension.WIDTH -> widthVal = newVal
                                    ActiveDimension.HEIGHT -> heightVal = newVal
                                    ActiveDimension.DEPTH -> depthVal = newVal
                                }
                                onDimensionsChange(widthVal, heightVal, depthVal)
                                onConfigChange(
                                    liveConfig.copy(
                                        widthCm = widthVal,
                                        heightCm = heightVal,
                                        depthCm = depthVal
                                    )
                                )
                            },
                            valueRange = minRange..maxRange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .testTag("bom_slider_${activeDimension.tag}"),
                            colors = SliderDefaults.colors(
                                thumbColor = CyanNeon,
                                activeTrackColor = CyanNeon,
                                inactiveTrackColor = ObsidianSurfaceVariant
                            )
                        )
                    }
                }

                if (selectedTab == 0) {
                    // ==========================================
                    // TAB 0: COSTING SUMMARY & ADD-ONS CATALOG
                    // ==========================================

                    // Total Cost Big Banner
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
                                        text = "ESTIMATED TOTAL WARDROBE COST",
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = DimensionFormatter.formatCurrencyINR(detailed.finalCost),
                                        color = EmeraldLaser,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${detailed.shuttersCount} Shutters • ${detailed.shelvesCount} Shelves",
                                        color = CyanNeon,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${detailed.verticalPartitionsCount} Partition(s) • ${detailed.drawersCount} Drawers",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // 4 Core Cost Pillars
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CostPillarCard(
                                    title = "MATERIAL",
                                    amount = detailed.materialTotal,
                                    subtitle = "Ply, Laminates, Glue",
                                    color = CyanNeon,
                                    modifier = Modifier.weight(1f)
                                )
                                CostPillarCard(
                                    title = "HARDWARE",
                                    amount = detailed.hardwareTotal,
                                    subtitle = "${detailed.totalClamps} Clamps + Add-ons",
                                    color = AmberWarning,
                                    modifier = Modifier.weight(1f)
                                )
                                CostPillarCard(
                                    title = "LABOUR",
                                    amount = detailed.labourCost,
                                    subtitle = "₹${WardrobeCostRates.LABOUR_RATE_PER_SQFT.toInt()}/sq.ft",
                                    color = ElectricBlue,
                                    modifier = Modifier.weight(1f)
                                )
                                CostPillarCard(
                                    title = "MISC (5%)",
                                    amount = detailed.miscellaneousCost,
                                    subtitle = "Logistics & Fasteners",
                                    color = TextSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // =========================================================================
                    // 2. WARDROBE ADD-ONS & CUSTOMIZATIONS SECTION (Visual Sample Catalog)
                    // =========================================================================
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(ObsidianSurface)
                                .border(1.dp, CyanBorderActive, RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Extension,
                                        contentDescription = null,
                                        tint = CyanNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "WARDROBE ADD-ONS & UPGRADES",
                                            color = CyanNeon,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "Visual samples • Tap '+ Add' to include in wardrobe & BOM",
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // ----------------- ADD-ON 1: LED LIGHTING -----------------
                            AddonLightingCard(
                                currentLighting = ledVal,
                                onSelectLighting = { newLed ->
                                    ledVal = newLed
                                    onConfigChange(liveConfig.copy(ledLighting = newLed))
                                }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // ----------------- ADD-ON 2: MODULAR DRAWERS -----------------
                            AddonDrawersCard(
                                drawersCount = drawersVal,
                                onAddDrawer = {
                                    if (drawersVal < 6) {
                                        drawersVal++
                                        onConfigChange(liveConfig.copy(drawersCount = drawersVal))
                                    }
                                },
                                onRemoveDrawer = {
                                    if (drawersVal > 0) {
                                        drawersVal--
                                        onConfigChange(liveConfig.copy(drawersCount = drawersVal))
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // ----------------- ADD-ON 3: DESIGNER HANDLES -----------------
                            AddonHandlesCard(
                                currentHandle = handleVal,
                                shuttersCount = detailed.shuttersCount,
                                onSelectHandle = { newHandle ->
                                    handleVal = newHandle
                                    onConfigChange(liveConfig.copy(handleStyle = newHandle))
                                }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // ----------------- ADD-ON 4: ACCESSORIES & ORGANIZERS -----------------
                            Text(
                                text = "INTERIOR ORGANIZERS & PANELS",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // Dressing Mirror
                            AddonToggleItemCard(
                                title = "Full-Height Dressing Mirror Shutter",
                                specs = "5mm Beveled Float Glass with safety vinyl backing",
                                price = 2200.0,
                                isEnabled = mirrorVal,
                                previewType = PreviewType.MIRROR,
                                onToggle = {
                                    mirrorVal = !mirrorVal
                                    onConfigChange(liveConfig.copy(hasMirrorPanel = mirrorVal))
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Velvet Jewelry & Watch Tray
                            AddonToggleItemCard(
                                title = "Modular Velvet Jewelry & Watch Tray",
                                specs = "Plush beige velvet divider with watch pillows & ring rolls",
                                price = 1850.0,
                                isEnabled = jewelryTrayVal,
                                previewType = PreviewType.JEWELRY_TRAY,
                                onToggle = {
                                    jewelryTrayVal = !jewelryTrayVal
                                    onConfigChange(liveConfig.copy(hasJewelryTray = jewelryTrayVal))
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Telescopic Trouser & Tie Pull-out
                            AddonToggleItemCard(
                                title = "Telescopic Trouser & Tie Pull-out Rack",
                                specs = "9-arm anti-slip rubber-padded chrome hanging bars",
                                price = 1450.0,
                                isEnabled = trouserRackVal,
                                previewType = PreviewType.TROUSER_RACK,
                                onToggle = {
                                    trouserRackVal = !trouserRackVal
                                    onConfigChange(liveConfig.copy(hasTrouserRack = trouserRackVal))
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Slanted Shoe Rack
                            AddonToggleItemCard(
                                title = "Slanted Metallic Shoe Organizer Tier",
                                specs = "Dual-tier ventilated anthracite wire mesh racks",
                                price = 1200.0,
                                isEnabled = shoeRackVal,
                                previewType = PreviewType.SHOE_RACK,
                                onToggle = {
                                    shoeRackVal = !shoeRackVal
                                    onConfigChange(liveConfig.copy(hasShoeRack = shoeRackVal))
                                }
                            )
                        }
                    }

                    // 1. Board Details Section
                    item {
                        CostSectionCard(
                            title = "1. BOARD DETAILS (8×4 FT / 32 SQ.FT)",
                            icon = Icons.Default.Dashboard
                        ) {
                            CostRow(
                                label = "Carcass Plywood (${detailed.carcassPlywoodSheets} sheets @ ₹${WardrobeCostRates.PLYWOOD_RATE_PER_SQFT.toInt()}/sq.ft)",
                                sub = "Area: ${String.format(Locale.US, "%.1f", detailed.carcassBoardAreaSqFt)} sq.ft (Purchased: ${detailed.carcassPlywoodPurchasedSqFt.toInt()} sq.ft)",
                                amount = detailed.carcassPlywoodCost
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CostRow(
                                label = "Shutter Blockboard (${detailed.shutterBlockboardSheets} sheets @ ₹${WardrobeCostRates.BLOCKBOARD_RATE_PER_SQFT.toInt()}/sq.ft)",
                                sub = "Area: ${String.format(Locale.US, "%.1f", detailed.shutterAreaSqFt)} sq.ft (Purchased: ${detailed.shutterPurchasedSqFt.toInt()} sq.ft)",
                                amount = detailed.shutterBlockboardCost
                            )
                        }
                    }

                    // 2. Laminate Details Section
                    item {
                        CostSectionCard(
                            title = "2. LAMINATE DETAILS",
                            icon = Icons.Default.Layers
                        ) {
                            CostRow(
                                label = "Internal Liner Laminate (${detailed.internalLaminateSheets} sheets @ ₹${WardrobeCostRates.INTERNAL_LAMINATE_RATE_PER_SHEET.toInt()}/sheet)",
                                sub = "Area: ${String.format(Locale.US, "%.1f", detailed.internalLaminateAreaSqFt)} sq.ft (0.8mm off-white)",
                                amount = detailed.internalLaminateCost
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CostRow(
                                label = "Exterior Texture Laminate (${detailed.exteriorLaminateSheets} sheets @ ₹${WardrobeCostRates.EXTERNAL_LAMINATE_RATE_PER_SHEET.toInt()}/sheet)",
                                sub = "Area: ${String.format(Locale.US, "%.1f", detailed.exteriorLaminateAreaSqFt)} sq.ft (1.0mm ${liveConfig.finish.title})",
                                amount = detailed.exteriorLaminateCost
                            )
                        }
                    }

                    // 3. Consumables Section
                    item {
                        CostSectionCard(
                            title = "3. CONSUMABLES & JOINERY",
                            icon = Icons.Default.Science
                        ) {
                            CostRow(
                                label = "Fevicol Marine Adhesive (${String.format(Locale.US, "%.2f", detailed.marineFevicolKg)} kg)",
                                sub = "Yield: 0.75 kg per laminated sheet (@ ₹${WardrobeCostRates.MARINE_FEVI_RATE_PER_KG.toInt()}/kg)",
                                amount = detailed.marineFevicolCost
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CostRow(
                                label = "2mm PVC Edge Bending Tape (${detailed.pvcEdgeRolls} Rolls)",
                                sub = "Length: ${String.format(Locale.US, "%.1f", detailed.shutterEdgeLengthM)} m total (@ ₹${WardrobeCostRates.PVC_EDGE_ROLL_RATE.toInt()}/roll)",
                                amount = detailed.pvcEdgeTapeCost
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CostRow(
                                label = "Fevicol Probond Adhesive (${String.format(Locale.US, "%.1f", detailed.probondKg)} kg)",
                                sub = "1 kg per PVC Edge roll (@ ₹${WardrobeCostRates.PROBOND_RATE_PER_KG.toInt()}/kg)",
                                amount = detailed.probondCost
                            )
                        }
                    }

                    // 4. Hardware & Upgrades Section
                    item {
                        CostSectionCard(
                            title = "4. HARDWARE, CLAMPS & UPGRADES",
                            icon = Icons.Default.Build
                        ) {
                            CostRow(
                                label = "Soft-Close Hinges / Clamps (${detailed.totalClamps} pcs)",
                                sub = "${detailed.clampsPerDoor} clamps/door based on height (@ ₹${WardrobeCostRates.CLAMP_RATE_PER_PC.toInt()}/pc)",
                                amount = detailed.clampCost
                            )
                            if (detailed.drawersCount > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                CostRow(
                                    label = "Modular Drawers & Telescopic Channels (${detailed.drawersCount} sets)",
                                    sub = "45kg soft-close ball-bearing runners (@ ₹950/set)",
                                    amount = detailed.drawerChannelsCost
                                )
                            }
                            if (detailed.handlesCount > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                CostRow(
                                    label = "Designer Shutter Handles (${detailed.handlesCount} pcs)",
                                    sub = liveConfig.handleStyle,
                                    amount = detailed.handleCost
                                )
                            }
                            if (detailed.ledLightingCost > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                CostRow(
                                    label = "Smart LED Cabinet Strip & Sensor Driver",
                                    sub = "45° aluminum profile with COB 24V strip (${liveConfig.ledLighting.title})",
                                    amount = detailed.ledLightingCost
                                )
                            }
                            if (detailed.accessoriesCost > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                CostRow(
                                    label = "Interior Upgrades (Mirror/Trays/Racks)",
                                    sub = listOfNotNull(
                                        if (mirrorVal) "Mirror" else null,
                                        if (jewelryTrayVal) "Jewelry Tray" else null,
                                        if (trouserRackVal) "Trouser Rack" else null,
                                        if (shoeRackVal) "Shoe Rack" else null
                                    ).joinToString(" + "),
                                    amount = detailed.accessoriesCost
                                )
                            }
                        }
                    }

                    // 5. Fabrication & Skilled Labour Section
                    item {
                        CostSectionCard(
                            title = "5. FABRICATION & SKILLED CARPENTRY",
                            icon = Icons.Default.Engineering
                        ) {
                            CostRow(
                                label = "Skilled Carpentry Labor (${String.format(Locale.US, "%.1f", detailed.shutterAreaSqFt)} sq.ft)",
                                sub = "Standard rate @ ₹${WardrobeCostRates.LABOUR_RATE_PER_SQFT.toInt()}/sq.ft based on shutter face area",
                                amount = detailed.labourCost
                            )
                        }
                    }

                    // Collapsible Mathematical Formula Breakdown
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianSurface)
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
                                .clickable { showFormulaBreakdown = !showFormulaBreakdown }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Science,
                                        contentDescription = null,
                                        tint = CyanNeon,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Carpentry Mathematical Formulas",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(
                                    imageVector = if (showFormulaBreakdown) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = showFormulaBreakdown,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                ) {
                                    HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FormulaRow(
                                        label = "Carcass Area Formula:",
                                        value = "Back + 2×Sides + 2×Top/Bottom + Shelves + Partitions"
                                    )
                                    FormulaRow(
                                        label = "Plywood Sheets Yield:",
                                        value = "ceil(${String.format(Locale.US, "%.1f", detailed.carcassBoardAreaSqFt)} / 32 sq.ft) = ${detailed.carcassPlywoodSheets} sheets"
                                    )
                                    FormulaRow(
                                        label = "Shutter Sheets Yield:",
                                        value = "ceil(${String.format(Locale.US, "%.1f", detailed.shutterAreaSqFt)} / 32 sq.ft) = ${detailed.shutterBlockboardSheets} sheets"
                                    )
                                    FormulaRow(
                                        label = "Fevicol Marine Glue Yield:",
                                        value = "${detailed.totalLaminateSheets} sheets × 0.75 kg = ${String.format(Locale.US, "%.2f", detailed.marineFevicolKg)} kg"
                                    )
                                    FormulaRow(
                                        label = "Clamp Allocation by Height:",
                                        value = "Height ${String.format(Locale.US, "%.2f", detailed.heightFt)} ft → ${detailed.clampsPerDoor} clamps/door"
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // TAB 1: ITEMIZED BOM & CUT LIST
                    // ==========================================

                    // Category Filter Chips
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedCategoryFilter == null,
                                onClick = { selectedCategoryFilter = null },
                                label = { Text("All Items (${bomSummary.items.size})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanNeon,
                                    selectedLabelColor = Color.Black,
                                    containerColor = ObsidianSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )

                            BOMCategory.values().forEach { category ->
                                val count = bomSummary.items.count { it.category == category }
                                if (count > 0) {
                                    FilterChip(
                                        selected = selectedCategoryFilter == category,
                                        onClick = { selectedCategoryFilter = category },
                                        label = { Text("${category.title} ($count)", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CyanNeon,
                                            selectedLabelColor = Color.Black,
                                            containerColor = ObsidianSurfaceVariant,
                                            labelColor = TextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Itemized BOM items
                    items(filteredItems) { item ->
                        BOMItemCard(item = item)
                    }
                }

                // ==========================================
                // Bottom Action Buttons
                // ==========================================
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onDimensionsChange(widthVal, heightVal, depthVal)
                                onConfigChange(liveConfig)
                                Toast.makeText(context, "Dimensions & Add-ons applied to 3D View", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_apply_bom_3d"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ObsidianSurfaceVariant,
                                contentColor = CyanNeon
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanBorderActive)
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

                        Button(
                            onClick = {
                                onSaveToVault(liveConfig.name)
                                onConfigChange(liveConfig)
                                Toast.makeText(context, "Saved '${liveConfig.name}' with Costing to Vault", Toast.LENGTH_SHORT).show()
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

// =========================================================================
// Visual Previews & Add-on Cards
// =========================================================================

enum class PreviewType {
    LIGHTING,
    DRAWERS,
    HANDLE,
    MIRROR,
    JEWELRY_TRAY,
    TROUSER_RACK,
    SHOE_RACK
}

@Composable
private fun AddonLightingCard(
    currentLighting: LedLighting,
    onSelectLighting: (LedLighting) -> Unit
) {
    val isEnabled = currentLighting != LedLighting.NONE

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurfaceVariant)
            .border(1.dp, if (isEnabled) CyanNeon.copy(alpha = 0.5f) else GlassBorderSubtle, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual Sample Image Canvas
            VisualPreviewCanvas(
                previewType = PreviewType.LIGHTING,
                lighting = currentLighting,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0D1117))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Smart Interior LED Lighting",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEnabled) "+₹2,800" else "Off",
                        color = if (isEnabled) EmeraldLaser else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "45° Aluminum profile + 24V COB Strip with auto PIR motion sensor",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    lineHeight = 12.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Light tone selection pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    LedLighting.values().forEach { led ->
                        val selected = currentLighting == led
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) CyanNeon else ObsidianSurface)
                                .border(1.dp, if (selected) CyanNeon else GlassBorderSubtle, RoundedCornerShape(6.dp))
                                .clickable { onSelectLighting(led) }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = led.title,
                                color = if (selected) Color.Black else TextDisabled,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddonDrawersCard(
    drawersCount: Int,
    onAddDrawer: () -> Unit,
    onRemoveDrawer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurfaceVariant)
            .border(1.dp, if (drawersCount > 0) CyanNeon.copy(alpha = 0.5f) else GlassBorderSubtle, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual Sample Image Canvas
            VisualPreviewCanvas(
                previewType = PreviewType.DRAWERS,
                drawersCount = drawersCount,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0D1117))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Modular Soft-Close Drawers",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (drawersCount > 0) "+₹${drawersCount * 950}" else "₹0",
                        color = if (drawersCount > 0) EmeraldLaser else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "15mm Solid ply box with 45kg telescopic ball-bearing sliders (@ ₹950/drawer)",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    lineHeight = 12.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$drawersCount Drawer(s) Configured",
                        color = CyanNeon,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Stepper + Add Drawer Button
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
                                .clickable(enabled = drawersCount > 0) { onRemoveDrawer() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Remove drawer",
                                tint = if (drawersCount > 0) CyanNeon else TextDisabled,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        Button(
                            onClick = onAddDrawer,
                            enabled = drawersCount < 6,
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("btn_add_drawer"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanNeon,
                                contentColor = Color.Black,
                                disabledContainerColor = ObsidianSurface,
                                disabledContentColor = TextDisabled
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Add Drawer (+₹950)", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddonHandlesCard(
    currentHandle: String,
    shuttersCount: Int,
    onSelectHandle: (String) -> Unit
) {
    val handleOptions = listOf(
        Triple("Matte Black Bar", 350.0, Color(0xFF1E2026)),
        Triple("Brushed Brass Profile", 550.0, Color(0xFFD4AF37)),
        Triple("Rose Gold Knob", 280.0, Color(0xFFB76E79)),
        Triple("Concealed J-Pull", 400.0, Color(0xFF475569)),
        Triple("Brushed Chrome Edge", 480.0, Color(0xFFCBD5E1))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurfaceVariant)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual Sample Image Canvas
            VisualPreviewCanvas(
                previewType = PreviewType.HANDLE,
                handleStyle = currentHandle,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0D1117))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Designer Shutter Handles",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$shuttersCount pcs @ ₹${handleOptions.firstOrNull { it.first == currentHandle }?.second?.toInt() ?: 350}/pc",
                        color = EmeraldLaser,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Architectural grade solid brass & anodized aluminum hardware",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    lineHeight = 12.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Handle choices
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    handleOptions.forEach { (name, price, color) ->
                        val selected = currentHandle == name
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) CyanNeon else ObsidianSurface)
                                .border(1.dp, if (selected) CyanNeon else GlassBorderSubtle, RoundedCornerShape(6.dp))
                                .clickable { onSelectHandle(name) }
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(0.5.dp, if (selected) Color.Black else Color.White.copy(alpha = 0.5f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = name,
                                color = if (selected) Color.Black else TextDisabled,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddonToggleItemCard(
    title: String,
    specs: String,
    price: Double,
    isEnabled: Boolean,
    previewType: PreviewType,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianSurfaceVariant)
            .border(1.dp, if (isEnabled) CyanNeon.copy(alpha = 0.4f) else GlassBorderSubtle, RoundedCornerShape(10.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Visual Sample Image Canvas
        VisualPreviewCanvas(
            previewType = previewType,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0D1117))
                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(6.dp))
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = specs,
                color = TextSecondary,
                fontSize = 8.5.sp,
                lineHeight = 11.sp
            )
            Text(
                text = "+${DimensionFormatter.formatCurrencyINR(price)}",
                color = EmeraldLaser,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // "+ Add" or "✓ Added" button
        Button(
            onClick = onToggle,
            modifier = Modifier
                .height(30.dp)
                .testTag("btn_addon_${previewType.name.lowercase()}"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isEnabled) EmeraldLaser else CyanNeon,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(6.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
        ) {
            Icon(
                imageVector = if (isEnabled) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isEnabled) "Added" else "Add",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Procedural CAD-style Visual Sample Graphics drawn with Jetpack Compose Canvas
 */
@Composable
private fun VisualPreviewCanvas(
    previewType: PreviewType,
    modifier: Modifier = Modifier,
    lighting: LedLighting = LedLighting.WARM_AMBIENT,
    drawersCount: Int = 3,
    handleStyle: String = "Matte Black Bar"
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (previewType) {
            PreviewType.LIGHTING -> {
                // Cabinet interior box
                drawRoundRect(
                    color = Color(0xFF161B22),
                    topLeft = Offset(4f, 4f),
                    size = Size(w - 8f, h - 8f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // LED light source at top
                val glowColor = if (lighting != LedLighting.NONE) lighting.color else Color.DarkGray
                drawRoundRect(
                    color = Color(0xFFC0C0C0),
                    topLeft = Offset(8f, 6f),
                    size = Size(w - 16f, 5f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                if (lighting != LedLighting.NONE) {
                    // Light cone gradient
                    val path = Path().apply {
                        moveTo(8f, 11f)
                        lineTo(w - 8f, 11f)
                        lineTo(w - 4f, h - 6f)
                        lineTo(4f, h - 6f)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(glowColor.copy(alpha = 0.7f), glowColor.copy(alpha = 0.05f))
                        )
                    )
                    // Hanger bar illuminated
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(10f, 24f),
                        end = Offset(w - 10f, 24f),
                        strokeWidth = 3f
                    )
                }
            }

            PreviewType.DRAWERS -> {
                // Wooden Drawer box in 3D perspective
                val boxColor = Color(0xFF8B5A2B)
                val frontColor = Color(0xFFA06D3B)
                val slideRailColor = Color(0xFFCBD5E1)

                // Back cabinet wall
                drawRoundRect(
                    color = Color(0xFF1E232A),
                    topLeft = Offset(4f, 4f),
                    size = Size(w - 8f, h - 8f),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Telescopic slide rails on sides
                drawLine(
                    color = slideRailColor,
                    start = Offset(6f, h * 0.45f),
                    end = Offset(w * 0.35f, h * 0.45f),
                    strokeWidth = 3f
                )
                drawLine(
                    color = slideRailColor,
                    start = Offset(w - 6f, h * 0.45f),
                    end = Offset(w * 0.65f, h * 0.45f),
                    strokeWidth = 3f
                )

                // Open drawer body
                drawRoundRect(
                    color = boxColor,
                    topLeft = Offset(10f, 12f),
                    size = Size(w - 20f, h * 0.45f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                // Drawer front panel
                drawRoundRect(
                    color = frontColor,
                    topLeft = Offset(8f, h * 0.42f),
                    size = Size(w - 16f, h * 0.42f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                // Handle pull
                drawRoundRect(
                    color = Color(0xFF1A1A1A),
                    topLeft = Offset(w * 0.38f, h * 0.58f),
                    size = Size(w * 0.24f, 4f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }

            PreviewType.HANDLE -> {
                // Cabinet Shutter background
                drawRoundRect(
                    color = Color(0xFF271B13),
                    topLeft = Offset(4f, 4f),
                    size = Size(w - 8f, h - 8f),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                val handleColor = when (handleStyle) {
                    "Brushed Brass Profile" -> Color(0xFFE5C158)
                    "Rose Gold Knob" -> Color(0xFFE08D9B)
                    "Concealed J-Pull" -> Color(0xFF475569)
                    "Brushed Chrome Edge" -> Color(0xFFE2E8F0)
                    else -> Color(0xFF111111) // Matte Black
                }

                if (handleStyle == "Rose Gold Knob") {
                    // Knurled round knob
                    drawCircle(
                        color = handleColor,
                        radius = 12f,
                        center = Offset(w * 0.5f, h * 0.5f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f),
                        radius = 6f,
                        center = Offset(w * 0.46f, h * 0.46f)
                    )
                } else if (handleStyle == "Concealed J-Pull") {
                    // Integrated edge groove
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.6f),
                        topLeft = Offset(w * 0.6f, 10f),
                        size = Size(6f, h - 20f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                } else {
                    // Bar pull with standoffs
                    drawRoundRect(
                        color = handleColor,
                        topLeft = Offset(w * 0.45f, 10f),
                        size = Size(6f, h - 20f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                    // Top & bottom mounting standoffs
                    drawRect(
                        color = handleColor.copy(alpha = 0.8f),
                        topLeft = Offset(w * 0.35f, 14f),
                        size = Size(8f, 4f)
                    )
                    drawRect(
                        color = handleColor.copy(alpha = 0.8f),
                        topLeft = Offset(w * 0.35f, h - 18f),
                        size = Size(8f, 4f)
                    )
                }
            }

            PreviewType.MIRROR -> {
                // Outer wooden door frame
                drawRoundRect(
                    color = Color(0xFF1E232A),
                    topLeft = Offset(4f, 4f),
                    size = Size(w - 8f, h - 8f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // Mirror Glass with reflective sheen
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFE2F1F8), Color(0xFFB0D5E5), Color(0xFF86B9D0))
                    ),
                    topLeft = Offset(8f, 8f),
                    size = Size(w - 16f, h - 16f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                // Diagonal reflection streaks
                drawLine(
                    color = Color.White.copy(alpha = 0.6f),
                    start = Offset(14f, 12f),
                    end = Offset(w - 20f, h - 12f),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.3f),
                    start = Offset(24f, 12f),
                    end = Offset(w - 12f, h - 20f),
                    strokeWidth = 1.5f
                )
            }

            PreviewType.JEWELRY_TRAY -> {
                // Velvet tray frame (Beige/Gold velvet)
                drawRoundRect(
                    color = Color(0xFFCBB89D),
                    topLeft = Offset(4f, 4f),
                    size = Size(w - 8f, h - 8f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // 4 velvet compartment cells
                val cellW = (w - 16f) / 2f
                val cellH = (h - 16f) / 2f
                for (row in 0..1) {
                    for (col in 0..1) {
                        drawRoundRect(
                            color = Color(0xFFB59F82),
                            topLeft = Offset(6f + (col * (cellW + 4f)), 6f + (row * (cellH + 4f))),
                            size = Size(cellW, cellH),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                }
                // Watch pillow icon in top-left
                drawCircle(
                    color = Color(0xFF8C7355),
                    radius = 5f,
                    center = Offset(6f + (cellW / 2f), 6f + (cellH / 2f))
                )
                // Ring rolls in bottom-right
                drawLine(
                    color = Color(0xFF8C7355),
                    start = Offset(10f + cellW + 4f, 10f + cellH + 4f),
                    end = Offset(w - 10f, 10f + cellH + 4f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color(0xFF8C7355),
                    start = Offset(10f + cellW + 4f, 16f + cellH + 4f),
                    end = Offset(w - 10f, 16f + cellH + 4f),
                    strokeWidth = 2f
                )
            }

            PreviewType.TROUSER_RACK -> {
                // Chrome pull out frame
                drawRoundRect(
                    color = Color(0xFF1E232A),
                    topLeft = Offset(4f, 4f),
                    size = Size(w - 8f, h - 8f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // Top telescopic rail
                drawRoundRect(
                    color = Color(0xFFCBD5E1),
                    topLeft = Offset(8f, 8f),
                    size = Size(w - 16f, 4f),
                    cornerRadius = CornerRadius(1f, 1f)
                )
                // Vertical hanging rungs
                val rungs = 4
                val spacing = (w - 20f) / (rungs + 1)
                for (i in 1..rungs) {
                    val x = 10f + (i * spacing)
                    drawLine(
                        color = Color(0xFF94A3B8),
                        start = Offset(x, 12f),
                        end = Offset(x, h - 10f),
                        strokeWidth = 2.5f
                    )
                }
            }

            PreviewType.SHOE_RACK -> {
                // Anthracite wire mesh frame
                drawRoundRect(
                    color = Color(0xFF1E232A),
                    topLeft = Offset(4f, 4f),
                    size = Size(w - 8f, h - 8f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // Slanted dual tiers
                drawLine(
                    color = Color(0xFF64748B),
                    start = Offset(8f, 14f),
                    end = Offset(w - 8f, 22f),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color(0xFF64748B),
                    start = Offset(8f, 30f),
                    end = Offset(w - 8f, 38f),
                    strokeWidth = 3f
                )
            }
        }
    }
}

// =========================================================================
// Helper Composables
// =========================================================================

@Composable
private fun CostPillarCard(
    title: String,
    amount: Double,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianSurface)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Text(text = title, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = DimensionFormatter.formatCurrencyINR(amount), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = subtitle, color = TextMuted, fontSize = 8.sp, maxLines = 1)
    }
}

@Composable
private fun CostSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianSurface)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyanNeon,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = CyanNeon,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun CostRow(
    label: String,
    sub: String,
    amount: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(text = sub, color = TextSecondary, fontSize = 10.sp)
        }
        Text(
            text = DimensionFormatter.formatCurrencyINR(amount),
            color = EmeraldLaser,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FormulaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMuted, fontSize = 10.sp)
        Text(text = value, color = CyanNeon, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DimensionInputBox(
    label: String,
    valueCm: Float,
    minCm: Float,
    maxCm: Float,
    isSelected: Boolean,
    unitSystem: UnitSystem,
    onClick: () -> Unit,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayFormatted = DimensionFormatter.formatLength(valueCm, unitSystem, compact = true)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CyanNeon.copy(alpha = 0.12f) else ObsidianSurfaceVariant)
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) CyanNeon else GlassBorderSubtle, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(CyanNeon)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = label,
                color = if (isSelected) CyanNeon else TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = displayFormatted,
            color = if (isSelected) CyanNeon else TextPrimary,
            fontSize = 13.sp,
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
                        onClick()
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
                        onClick()
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
private fun BOMItemCard(item: BOMItem) {
    val categoryIcon: ImageVector = when (item.category) {
        BOMCategory.CARCASS_PANELS, BOMCategory.BOARDS -> Icons.Default.Dashboard
        BOMCategory.DOORS_FACADES -> Icons.Default.DoorFront
        BOMCategory.LAMINATES -> Icons.Default.Layers
        BOMCategory.CONSUMABLES, BOMCategory.ADHESIVES -> Icons.Default.Science
        BOMCategory.HARDWARE_FASTENERS, BOMCategory.HARDWARE, BOMCategory.HANDLES -> Icons.Default.Build
        BOMCategory.INTERIOR_MODULES, BOMCategory.ACCESSORIES -> Icons.Default.Inventory2
        BOMCategory.LABOUR_SERVICES, BOMCategory.LABOUR -> Icons.Default.Engineering
        BOMCategory.LIGHTING -> Icons.Default.Lightbulb
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
