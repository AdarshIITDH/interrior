package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.FinishArcticWhite
import com.example.ui.theme.FinishBrushedGold
import com.example.ui.theme.FinishMatteObsidian
import com.example.ui.theme.FinishMidnightTeal
import com.example.ui.theme.FinishNordicOak
import com.example.ui.theme.FinishSmokedGlass
import com.example.ui.theme.FinishTitanium
import com.example.ui.theme.FinishWalnut

enum class FinishType(
    val title: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val highlightColor: Color,
    val grainColor: Color = Color(0xFF28180E).copy(alpha = 0.45f),
    val roughness: Float = 0.5f,
    val isWood: Boolean = true,
    val isGlass: Boolean = false
) {
    // 6 Core Finishes from Design Image Screen 7 with realistic timber hues
    WALNUT(
        title = "Walnut",
        primaryColor = Color(0xFF3E2415),     // Deep American Walnut body
        secondaryColor = Color(0xFF27140B),   // Dark espresso shadow & recessed tone
        highlightColor = Color(0xFF5E3922),   // Warm chestnut bevel highlight
        grainColor = Color(0xFF160B05),       // Rich dark grain veins
        roughness = 0.40f,
        isWood = true
    ),
    OAK(
        title = "Oak",
        primaryColor = Color(0xFFC7A781),     // Warm Honey Nordic Oak body
        secondaryColor = Color(0xFFA17E56),   // Amber shadow & recessed field
        highlightColor = Color(0xFFE4CBAC),   // Sunlit champagne oak bevel highlight
        grainColor = Color(0xFF7A5934),       // Golden tan grain veins
        roughness = 0.45f,
        isWood = true
    ),
    TEAK(
        title = "Teak",
        primaryColor = Color(0xFFA96D3A),     // Royal Burmese Teak body
        secondaryColor = Color(0xFF814C20),   // Warm spiced teak shadow
        highlightColor = Color(0xFFCE8F56),   // Golden amber bevel highlight
        grainColor = Color(0xFF5B300F),       // Deep teak heartwood grain
        roughness = 0.42f,
        isWood = true
    ),
    WHITE(
        title = "White",
        primaryColor = Color(0xFFF3F4F6),
        secondaryColor = Color(0xFFCBD5E1),
        highlightColor = Color(0xFFFFFFFF),
        grainColor = Color(0xFF94A3B8).copy(alpha = 0.3f),
        roughness = 0.85f,
        isWood = false
    ),
    GRAPHITE(
        title = "Graphite",
        primaryColor = Color(0xFF2C2F36),
        secondaryColor = Color(0xFF1B1C22),
        highlightColor = Color(0xFF3E424C),
        grainColor = Color(0xFF111215),
        roughness = 0.75f,
        isWood = false
    ),
    BEIGE(
        title = "Beige",
        primaryColor = Color(0xFFD7C9B5),
        secondaryColor = Color(0xFFB5A692),
        highlightColor = Color(0xFFEFE6D7),
        grainColor = Color(0xFF968774).copy(alpha = 0.35f),
        roughness = 0.65f,
        isWood = false
    ),

    // Backward-compatibility aliases
    MATTE_OBSIDIAN("Matte Obsidian", FinishMatteObsidian, Color(0xFF111726), Color(0xFF2D3748), Color(0xFF0B0E14), 0.8f, isWood = false),
    NORDIC_OAK("Nordic Oak", Color(0xFFC7A781), Color(0xFFA17E56), Color(0xFFE4CBAC), Color(0xFF7A5934), 0.45f, isWood = true),
    SMOKED_GLASS("Smoked Glass", FinishSmokedGlass, Color(0x6600E5FF), Color(0xFF38BDF8), Color(0xFF0284C7), 0.1f, isWood = false, isGlass = true),
    BRUSHED_TITANIUM("Brushed Titanium", FinishTitanium, Color(0xFF64748B), Color(0xFFCBD5E1), Color(0xFF475569), 0.3f, isWood = false),
    ARCTIC_WHITE("Arctic White", FinishArcticWhite, Color(0xFFE2E8F0), Color(0xFFFFFFFF), Color(0xFFCBD5E1), 0.9f, isWood = false),
    MIDNIGHT_TEAL("Midnight Teal", FinishMidnightTeal, Color(0xFF004D40), Color(0xFF0D9488), Color(0xFF042F2E), 0.7f, isWood = false),
    WALNUT_LUXE("Walnut Luxe", Color(0xFF3E2415), Color(0xFF27140B), Color(0xFF5E3922), Color(0xFF160B05), 0.40f, isWood = true),
    BRUSHED_GOLD("Brushed Brass", FinishBrushedGold, Color(0xFFB8860B), Color(0xFFFACC15), Color(0xFF92400E), 0.35f, isWood = false)
}

enum class DoorStyle(val title: String) {
    DUAL_HINGED("Hinged"),
    SLIDING_BYPASS("Sliding"),
    ACCORDION_BI_FOLD("Mirror Sliding"),
    OPEN_CONCEPT("Open"),

    // Aliases
    HINGED_DOOR("Hinged"),
    SLIDING_DOOR("Sliding"),
    MIRROR_SLIDING_DOOR("Mirror Sliding")
}

enum class InteriorCategory(val title: String) {
    ALL("All"),
    HANGING("Hanging"),
    SHELVES("Shelves"),
    DRAWERS("Drawers"),
    MIXED("Mixed")
}

enum class LedLighting(val title: String, val color: Color, val tempKelvin: String) {
    NONE("Off", Color.Transparent, "0K"),
    WARM_AMBIENT("Warm Amber", Color(0xFFFFD54F), "2700K"),
    NATURAL_DAYLIGHT("Neutral Day", Color(0xFFFFFFFF), "4000K"),
    CYAN_HOLOGRAPHIC("Cyber Cyan", Color(0xFF00F0FF), "6500K")
}

data class InteriorPreset(
    val id: String,
    val name: String,
    val category: InteriorCategory,
    val shelvesCount: Int,
    val hangingRailsCount: Int,
    val drawersCount: Int,
    val description: String
)

data class RoomMeasurement(
    val detectedWallWidthM: Float = 3.86f,
    val detectedHeightM: Float = 2.60f,
    val detectedDepthM: Float = 1.20f,
    val topClearanceCm: Float = 18f,
    val sideClearanceCm: Float = 14f,
    val isWallDetected: Boolean = true,
    val isFloorDetected: Boolean = true,
    val scanProgress: Float = 1.0f
) {
    val recommendedWidthCm: Float
        get() = ((detectedWallWidthM * 100f) - (sideClearanceCm * 2f)).coerceIn(120f, 320f)

    val recommendedHeightCm: Float
        get() = ((detectedHeightM * 100f) - topClearanceCm).coerceIn(180f, 250f)

    val recommendedDepthCm: Float = 60f
}

data class WardrobeConfig(
    val id: String = "custom_${System.currentTimeMillis()}",
    val name: String = "Bedroom Wardrobe",
    val widthCm: Float = 240f,      // 80cm - 320cm
    val heightCm: Float = 240f,     // 160cm - 270cm
    val depthCm: Float = 60f,       // 40cm - 80cm
    val finish: FinishType = FinishType.WALNUT,
    val doorStyle: DoorStyle = DoorStyle.DUAL_HINGED,
    val doorOpenRatio: Float = 0.0f, // 0.0 (closed) to 1.0 (fully open)
    val shelvesCount: Int = 4,
    val hangingRailsCount: Int = 2,
    val drawersCount: Int = 3,
    val hasShoeRack: Boolean = true,
    val hasMirrorPanel: Boolean = false,
    val ledLighting: LedLighting = LedLighting.WARM_AMBIENT,
    val handleStyle: String = "Matte Black Bar",
    val wallAnchorMode: Boolean = true
) {
    val volumeCubicMeters: Float
        get() = (widthCm / 100f) * (heightCm / 100f) * (depthCm / 100f)

    val requiredFloorClearanceCm: Float
        get() = if (doorStyle == DoorStyle.OPEN_CONCEPT || doorStyle == DoorStyle.SLIDING_BYPASS) {
            depthCm + 40f
        } else {
            depthCm + (widthCm / 2f) + 30f
        }
}

data class WardrobePreset(
    val id: String,
    val name: String,
    val subtitle: String,
    val category: String = "Modern",
    val config: WardrobeConfig,
    val iconName: String = "ic_wardrobe"
)

object PresetCatalog {
    val INTERIOR_PRESETS = listOf(
        InteriorPreset(
            id = "int_hanging",
            name = "Dual Hanging + Loft",
            category = InteriorCategory.HANGING,
            shelvesCount = 2,
            hangingRailsCount = 3,
            drawersCount = 2,
            description = "Maximized vertical hanging for coats, dresses & suits"
        ),
        InteriorPreset(
            id = "int_shelves",
            name = "Tower Shelves Organizer",
            category = InteriorCategory.SHELVES,
            shelvesCount = 6,
            hangingRailsCount = 1,
            drawersCount = 2,
            description = "Multi-tier folded garment & accessories storage"
        ),
        InteriorPreset(
            id = "int_drawers",
            name = "Triple Soft-Close Drawers",
            category = InteriorCategory.DRAWERS,
            shelvesCount = 3,
            hangingRailsCount = 2,
            drawersCount = 5,
            description = "Deep soft-close modular wooden pull-out drawers"
        ),
        InteriorPreset(
            id = "int_mixed",
            name = "Executive Balanced Suite",
            category = InteriorCategory.MIXED,
            shelvesCount = 4,
            hangingRailsCount = 2,
            drawersCount = 3,
            description = "Harmonious combination of rails, shelves & drawers"
        )
    )

    val PRESETS = listOf(
        WardrobePreset(
            id = "preset_bedroom_walnut",
            name = "Bedroom Wardrobe",
            subtitle = "Rich Walnut 3-Door with Brushed Handles",
            iconName = "view_agenda",
            config = WardrobeConfig(
                name = "Bedroom Wardrobe",
                widthCm = 240f,
                heightCm = 240f,
                depthCm = 60f,
                finish = FinishType.WALNUT,
                doorStyle = DoorStyle.DUAL_HINGED,
                shelvesCount = 4,
                hangingRailsCount = 2,
                drawersCount = 3,
                hasMirrorPanel = false,
                ledLighting = LedLighting.WARM_AMBIENT
            )
        ),
        WardrobePreset(
            id = "preset_guest_oak",
            name = "Guest Room Wardrobe",
            subtitle = "Natural Scandi Oak with Sliding Doors",
            iconName = "forest",
            config = WardrobeConfig(
                name = "Guest Room Wardrobe",
                widthCm = 200f,
                heightCm = 230f,
                depthCm = 60f,
                finish = FinishType.OAK,
                doorStyle = DoorStyle.SLIDING_BYPASS,
                shelvesCount = 4,
                hangingRailsCount = 2,
                drawersCount = 2,
                hasMirrorPanel = true,
                ledLighting = LedLighting.WARM_AMBIENT
            )
        ),
        WardrobePreset(
            id = "preset_walkin_teak",
            name = "Walk-in Wardrobe",
            subtitle = "Amber Teak open concept luxury dressing room",
            iconName = "grid_view",
            config = WardrobeConfig(
                name = "Walk-in Wardrobe",
                widthCm = 280f,
                heightCm = 240f,
                depthCm = 65f,
                finish = FinishType.TEAK,
                doorStyle = DoorStyle.OPEN_CONCEPT,
                shelvesCount = 6,
                hangingRailsCount = 3,
                drawersCount = 4,
                hasMirrorPanel = true,
                ledLighting = LedLighting.NATURAL_DAYLIGHT
            )
        ),
        WardrobePreset(
            id = "preset_minimal_white",
            name = "Minimal Wardrobe",
            subtitle = "Satin Pure White with flush bi-fold design",
            iconName = "view_compact",
            config = WardrobeConfig(
                name = "Minimal Wardrobe",
                widthCm = 180f,
                heightCm = 220f,
                depthCm = 58f,
                finish = FinishType.WHITE,
                doorStyle = DoorStyle.ACCORDION_BI_FOLD,
                shelvesCount = 4,
                hangingRailsCount = 2,
                drawersCount = 2,
                hasMirrorPanel = false,
                ledLighting = LedLighting.NATURAL_DAYLIGHT
            )
        ),
        WardrobePreset(
            id = "preset_graphite_sliding",
            name = "Graphite Sliding Suite",
            subtitle = "Modern Matte Charcoal with integrated recessed pulls",
            iconName = "door_sliding",
            config = WardrobeConfig(
                name = "Graphite Sliding Suite",
                widthCm = 260f,
                heightCm = 240f,
                depthCm = 62f,
                finish = FinishType.GRAPHITE,
                doorStyle = DoorStyle.SLIDING_BYPASS,
                shelvesCount = 5,
                hangingRailsCount = 3,
                drawersCount = 4,
                hasMirrorPanel = true,
                ledLighting = LedLighting.WARM_AMBIENT
            )
        ),
        WardrobePreset(
            id = "preset_warm_beige",
            name = "Warm Beige Alcove",
            subtitle = "Gentle architectural beige for calm bedrooms",
            iconName = "hotel",
            config = WardrobeConfig(
                name = "Warm Beige Alcove",
                widthCm = 190f,
                heightCm = 225f,
                depthCm = 60f,
                finish = FinishType.BEIGE,
                doorStyle = DoorStyle.DUAL_HINGED,
                shelvesCount = 4,
                hangingRailsCount = 2,
                drawersCount = 3,
                hasMirrorPanel = false,
                ledLighting = LedLighting.WARM_AMBIENT
            )
        )
    )
}
