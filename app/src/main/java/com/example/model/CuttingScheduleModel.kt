package com.example.model

/**
 * Represents a single cut panel in the wardrobe cutting list.
 */
data class CutPanel(
    val partName: String,
    val section: String,
    val quantity: Int,
    val lengthInches: Float,
    val widthInches: Float,
    val thicknessMm: Int,
    val material: String,
    val finish: String,
    val edgeBanding: String,
    val grainDirection: String = "Lengthwise"
) {
    val lengthFtIn: String get() = formatInchesToFtIn(lengthInches)
    val widthFtIn: String get() = formatInchesToFtIn(widthInches)
    val areaSqFt: Double get() = (lengthInches * widthInches / 144.0) * quantity

    companion object {
        fun formatInchesToFtIn(inches: Float): String {
            val totalInches = Math.round(inches * 2f) / 2f
            val feet = (totalInches / 12).toInt()
            val remInches = totalInches % 12
            val remInt = remInches.toInt()
            val fraction = remInches - remInt
            val fracStr = when {
                fraction >= 0.75f -> "¾"
                fraction >= 0.5f -> "½"
                fraction >= 0.25f -> "¼"
                else -> ""
            }

            return when {
                feet > 0 && remInt > 0 -> "$feet' $remInt$fracStr\""
                feet > 0 && fracStr.isNotEmpty() -> "$feet' $fracStr\""
                feet > 0 -> "$feet' 0\""
                remInt > 0 -> "$remInt$fracStr\""
                fracStr.isNotEmpty() -> "$fracStr\""
                else -> "0\""
            }
        }
    }
}

/**
 * Generator for the full parametric wardrobe panel cutting schedule.
 */
object CuttingScheduleGenerator {

    fun generateCuttingSchedule(config: WardrobeConfig): List<CutPanel> {
        val list = mutableListOf<CutPanel>()

        val widthIn = config.widthCm / 2.54f
        val heightIn = config.heightCm / 2.54f
        val depthIn = config.depthCm / 2.54f
        val finishName = config.finish.title

        val sections = config.sectionsCount
        val sectionWidthIn = (widthIn - (18f / 25.4f * (sections + 1))) / sections

        // 1. Carcass Side Panels (Left & Right)
        list.add(
            CutPanel(
                partName = "Carcass Side (Left)",
                section = "Outer Frame",
                quantity = 1,
                lengthInches = heightIn,
                widthInches = depthIn,
                thicknessMm = 18,
                material = "BWP Marine Plywood",
                finish = "$finishName (Ext) / White (Int)",
                edgeBanding = "2.0 mm PVC (Front edge)"
            )
        )
        list.add(
            CutPanel(
                partName = "Carcass Side (Right)",
                section = "Outer Frame",
                quantity = 1,
                lengthInches = heightIn,
                widthInches = depthIn,
                thicknessMm = 18,
                material = "BWP Marine Plywood",
                finish = "$finishName (Ext) / White (Int)",
                edgeBanding = "2.0 mm PVC (Front edge)"
            )
        )

        // 2. Top & Bottom Panels
        val carcassInnerWidthIn = widthIn - (2 * 18f / 25.4f)
        list.add(
            CutPanel(
                partName = "Carcass Top Panel",
                section = "Outer Frame",
                quantity = 1,
                lengthInches = carcassInnerWidthIn,
                widthInches = depthIn,
                thicknessMm = 18,
                material = "BWP Marine Plywood",
                finish = "Balancing White 0.8mm",
                edgeBanding = "2.0 mm PVC (Front edge)"
            )
        )
        list.add(
            CutPanel(
                partName = "Carcass Bottom Panel",
                section = "Outer Frame",
                quantity = 1,
                lengthInches = carcassInnerWidthIn,
                widthInches = depthIn,
                thicknessMm = 18,
                material = "BWP Marine Plywood",
                finish = "Balancing White 0.8mm",
                edgeBanding = "2.0 mm PVC (Front edge)"
            )
        )

        // 3. Vertical Partitions
        val innerHeightIn = heightIn - (2 * 18f / 25.4f) - 3.0f // deducting 75mm skirting
        if (sections > 1) {
            list.add(
                CutPanel(
                    partName = "Vertical Partition",
                    section = "Divider",
                    quantity = sections - 1,
                    lengthInches = innerHeightIn,
                    widthInches = depthIn - (18f / 25.4f),
                    thicknessMm = 18,
                    material = "BWP Marine Plywood",
                    finish = "Inner White Laminate 0.8mm (Both sides)",
                    edgeBanding = "0.8 mm PVC (Front edge)"
                )
            )
        }

        // 4. Horizontal Shelves
        if (config.shelvesCount > 0) {
            list.add(
                CutPanel(
                    partName = "Interior Shelf",
                    section = "Storage",
                    quantity = config.shelvesCount,
                    lengthInches = sectionWidthIn,
                    widthInches = depthIn - 1.5f,
                    thicknessMm = 18,
                    material = "BWP Marine Plywood",
                    finish = "Inner White Laminate 0.8mm (Both sides)",
                    edgeBanding = "0.8 mm PVC (Front edge)"
                )
            )
        }

        // 5. Drawer Box Components (Front, Sides, Back, Bottom)
        if (config.drawersCount > 0) {
            val drawerHeightIn = 7.0f
            val drawerDepthIn = depthIn - 2.0f
            val drawerWidthIn = sectionWidthIn - 1.0f // clearance for telescopic channels

            list.add(
                CutPanel(
                    partName = "Drawer Fascia Front",
                    section = "Drawers",
                    quantity = config.drawersCount,
                    lengthInches = sectionWidthIn - 0.25f,
                    widthInches = drawerHeightIn + 0.5f,
                    thicknessMm = 18,
                    material = "BWP Marine Plywood",
                    finish = "$finishName Laminate 1.0mm",
                    edgeBanding = "2.0 mm PVC (All 4 edges)"
                )
            )
            list.add(
                CutPanel(
                    partName = "Drawer Internal Box (Sides & Back)",
                    section = "Drawers",
                    quantity = config.drawersCount * 3,
                    lengthInches = drawerDepthIn,
                    widthInches = drawerHeightIn,
                    thicknessMm = 12,
                    material = "MR Commercial Plywood",
                    finish = "Inner White 0.8mm",
                    edgeBanding = "0.8 mm PVC (Top edge)"
                )
            )
            list.add(
                CutPanel(
                    partName = "Drawer Bottom Board",
                    section = "Drawers",
                    quantity = config.drawersCount,
                    lengthInches = drawerWidthIn,
                    widthInches = drawerDepthIn,
                    thicknessMm = 9,
                    material = "Hardwood Core Plywood",
                    finish = "Inner White 0.8mm",
                    edgeBanding = "None (Grooved into sides)"
                )
            )
        }

        // 6. Shutter / Door Panels
        if (config.doorStyle != DoorStyle.OPEN_CONCEPT) {
            val shutterCount = when (config.doorStyle) {
                DoorStyle.SLIDING_BYPASS, DoorStyle.SLIDING_DOOR, DoorStyle.MIRROR_SLIDING_DOOR, DoorStyle.ACCORDION_BI_FOLD -> sections
                else -> sections * 2
            }
            val shutterWidthIn = when (config.doorStyle) {
                DoorStyle.SLIDING_BYPASS, DoorStyle.SLIDING_DOOR, DoorStyle.MIRROR_SLIDING_DOOR -> (widthIn / shutterCount) + 1.0f // 25mm overlap
                else -> (widthIn / shutterCount) - 0.15f // 3mm expansion gap
            }

            list.add(
                CutPanel(
                    partName = if (config.doorStyle == DoorStyle.MIRROR_SLIDING_DOOR) "Mirror Cladded Shutter" else "Main Shutter Door",
                    section = "Doors",
                    quantity = shutterCount,
                    lengthInches = heightIn - 3.0f,
                    widthInches = shutterWidthIn,
                    thicknessMm = 18,
                    material = "BWP Blockboard (Pinewood core)",
                    finish = "$finishName 1.0mm (Front) / White 0.8mm (Back)",
                    edgeBanding = "2.0 mm Seamless PVC Edge (All 4 edges)"
                )
            )
        }

        // 7. Back Panel (6 mm)
        list.add(
            CutPanel(
                partName = "Rear Backing Panel",
                section = "Back",
                quantity = 1,
                lengthInches = heightIn - 1.0f,
                widthInches = widthIn - 1.0f,
                thicknessMm = 6,
                material = "Hardwood Backing Sheet",
                finish = "One-Side White Laminate",
                edgeBanding = "None (Recessed into 8mm rebate)"
            )
        )

        // 8. Skirting Plinth (Base)
        list.add(
            CutPanel(
                partName = "Plinth Skirting Base",
                section = "Foundation",
                quantity = 2,
                lengthInches = widthIn,
                widthInches = 3.0f, // 75mm
                thicknessMm = 18,
                material = "Calibrated Marine Plywood",
                finish = "Black Water-Resistant Sealant",
                edgeBanding = "None"
            )
        )

        return list
    }
}
