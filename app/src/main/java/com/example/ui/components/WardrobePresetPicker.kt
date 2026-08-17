package com.example.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DoorStyle
import com.example.model.FinishType
import com.example.model.LedLighting
import com.example.model.PresetCatalog
import com.example.model.WardrobePreset
import com.example.ui.theme.CyanBorderActive
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun WardrobePresetPicker(
    selectedPresetId: String,
    onSelectPreset: (WardrobePreset) -> Unit,
    onOpenCustomizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // Minimalist Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WARDROBE MODELS",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "Edit Specs ↗",
                color = CyanNeon,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable(onClick = onOpenCustomizer)
                    .testTag("btn_header_customize")
            )
        }

        // Compact Horizontal Carousel of Visual Rendered Preset Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetCatalog.PRESETS.forEach { preset ->
                val isSelected = preset.id == selectedPresetId

                CompactWardrobePresetCard(
                    preset = preset,
                    isSelected = isSelected,
                    onClick = { onSelectPreset(preset) }
                )
            }
        }
    }
}

@Composable
fun CompactWardrobePresetCard(
    preset: WardrobePreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val config = preset.config
    val finish = config.finish

    val (primaryColor, accentColor, glowColor) = getPresetRenderColors(finish, config.ledLighting)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) Color(0xFF1E242B) else ObsidianSurfaceVariant)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) CyanBorderActive else GlassBorderSubtle,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("preset_${preset.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Mini 3D Rendered Wardrobe Visual
        Box(
            modifier = Modifier
                .size(width = 38.dp, height = 48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0F1115)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(width = 34.dp, height = 44.dp)) {
                val w = size.width
                val h = size.height

                // Carcass shadow & body
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(2f, 2f),
                    size = Size(w - 4f, h - 4f),
                    cornerRadius = CornerRadius(3f, 3f)
                )

                // Outer frame border
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(2f, 2f),
                    size = Size(w - 4f, h - 4f),
                    cornerRadius = CornerRadius(3f, 3f),
                    style = Stroke(width = 1.2f)
                )

                // Internal details based on Door Style
                when (config.doorStyle) {
                    DoorStyle.OPEN_CONCEPT -> {
                        // Show internal shelves and hangers
                        val shelfY1 = h * 0.35f
                        val shelfY2 = h * 0.7f
                        drawLine(accentColor, Offset(4f, shelfY1), Offset(w - 4f, shelfY1), strokeWidth = 1f)
                        drawLine(accentColor, Offset(4f, shelfY2), Offset(w - 4f, shelfY2), strokeWidth = 1f)
                        // Hanging rail
                        drawLine(Color(0xFFCBD5E1), Offset(w * 0.25f, shelfY1 + 4f), Offset(w * 0.75f, shelfY1 + 4f), strokeWidth = 1.5f)
                    }
                    DoorStyle.SLIDING_BYPASS, DoorStyle.SLIDING_DOOR, DoorStyle.MIRROR_SLIDING_DOOR -> {
                        // Two overlapping sliding doors with subtle seam
                        val midX = w * 0.52f
                        drawLine(Color(0x80000000), Offset(midX, 2f), Offset(midX, h - 2f), strokeWidth = 1.2f)
                        // Recessed handle lines
                        drawLine(CyanNeon, Offset(midX - 4f, h * 0.4f), Offset(midX - 4f, h * 0.6f), strokeWidth = 1f)
                        drawLine(CyanNeon, Offset(midX + 4f, h * 0.4f), Offset(midX + 4f, h * 0.6f), strokeWidth = 1f)
                    }
                    DoorStyle.DUAL_HINGED, DoorStyle.HINGED_DOOR -> {
                        // Center split line
                        val midX = w * 0.5f
                        drawLine(Color(0x99000000), Offset(midX, 2f), Offset(midX, h - 2f), strokeWidth = 1.2f)
                        // Handles
                        drawCircle(Color(0xFFE2E8F0), radius = 1.2f, center = Offset(midX - 3f, h * 0.5f))
                        drawCircle(Color(0xFFE2E8F0), radius = 1.2f, center = Offset(midX + 3f, h * 0.5f))
                    }
                    DoorStyle.ACCORDION_BI_FOLD -> {
                        val seg = w / 3f
                        drawLine(Color(0x66000000), Offset(seg, 2f), Offset(seg, h - 2f), strokeWidth = 1f)
                        drawLine(Color(0x66000000), Offset(seg * 2f, 2f), Offset(seg * 2f, h - 2f), strokeWidth = 1f)
                    }
                }

                // Mirrored panel highlight
                if (config.hasMirrorPanel) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0x40E0F2FE), Color(0x1038BDF8), Color(0x40E0F2FE))
                        ),
                        topLeft = Offset(w * 0.5f, 4f),
                        size = Size(w * 0.4f, h - 8f)
                    )
                }

                // LED top glow
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(glowColor.copy(alpha = 0.8f), Color.Transparent)
                    ),
                    topLeft = Offset(3f, 3f),
                    size = Size(w - 6f, 6f)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 2. Info Block (Name, Dimensions, and Swatch)
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Finish Color Swatch Dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                        .border(0.8.dp, Color(0x66FFFFFF), CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = preset.name,
                    color = if (isSelected) CyanNeon else TextPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${config.widthCm.toInt()}×${config.heightCm.toInt()} cm • ${finish.title.split(" ").first()}",
                color = TextSecondary,
                fontSize = 9.5.sp,
                maxLines = 1
            )
        }
    }
}

private fun getPresetRenderColors(finish: FinishType, led: LedLighting): Triple<Color, Color, Color> {
    val primary = finish.primaryColor
    val accent = finish.secondaryColor
    val glow = when (led) {
        LedLighting.WARM_AMBIENT -> Color(0xFFFBBF24)
        LedLighting.CYAN_HOLOGRAPHIC -> Color(0xFF22D3EE)
        LedLighting.NATURAL_DAYLIGHT -> Color(0xFFF8FAFC)
        LedLighting.NONE -> Color.Transparent
    }
    return Triple(primary, accent, glow)
}
