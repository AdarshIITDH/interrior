package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.InteriorCategory
import com.example.model.InteriorPreset
import com.example.model.PresetCatalog
import com.example.model.WardrobeConfig
import com.example.spatial.ARPlacementState
import com.example.spatial.DeviceOrientationState
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Screen 6: Interior Configuration Screen
 * Top [← Interior  ⋮], 3D open doors view, Category pills [Hanging] [Shelves] [Drawers] [Mixed],
 * and bottom 3 thumbnail cards.
 */
@Composable
fun InteriorConfigScreen(
    currentConfig: WardrobeConfig,
    placement: ARPlacementState,
    deviceOrientation: DeviceOrientationState,
    selectedCategory: InteriorCategory,
    onSelectCategory: (InteriorCategory) -> Unit,
    onApplyPreset: (InteriorPreset) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = PresetCatalog.INTERIOR_PRESETS.filter {
        selectedCategory == InteriorCategory.ALL || it.category == selectedCategory
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("interior_config_screen")
    ) {
        // 1. 3D AR Wardrobe (Doors open)
        ARViewport(
            config = currentConfig.copy(doorOpenRatio = 1.0f),
            placement = placement.copy(showDimensions = false),
            deviceOrientation = deviceOrientation,
            onRotateYaw = {},
            onTranslate = { _, _ -> },
            onTap = {},
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Header Bar: [←  Interior  ⋮]
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
                        .testTag("interior_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Interior",
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

        // 3. Bottom Controls: Filter Chips + 3 Preset Thumbnails
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Category Filter Pills: [Hanging] [Shelves] [Drawers] [Mixed]
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    InteriorCategory.HANGING to "Hanging",
                    InteriorCategory.SHELVES to "Shelves",
                    InteriorCategory.DRAWERS to "Drawers",
                    InteriorCategory.MIXED to "Mixed"
                ).forEach { (cat, label) ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) CyanPrimary else Color(0xDD111827))
                            .border(1.dp, if (isSelected) CyanNeon else Color(0x33FFFFFF), CircleShape)
                            .clickable { onSelectCategory(cat) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("filter_$label")
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row of 3 Interior Preset Thumbnails
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(presets.take(3)) { preset ->
                    Card(
                        onClick = { onApplyPreset(preset) },
                        modifier = Modifier
                            .width(105.dp)
                            .height(85.dp)
                            .testTag("preset_${preset.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xDD111827)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = preset.name,
                                color = TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
