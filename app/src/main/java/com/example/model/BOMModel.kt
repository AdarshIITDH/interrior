package com.example.model

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

enum class BOMCategory(val title: String, val iconName: String) {
    CARCASS_PANELS("Structural Panels", "dashboard"),
    DOORS_FACADES("Doors & Facades", "door_front"),
    INTERIOR_MODULES("Interior Storage", "inventory_2"),
    HARDWARE_FASTENERS("Hardware & Joinery", "build"),
    LIGHTING_ELECTRICAL("Lighting & Sensors", "lightbulb")
}

data class BOMItem(
    val id: String,
    val category: BOMCategory,
    val name: String,
    val dimensionSpec: String,
    val quantity: Int,
    val unit: String,
    val material: String,
    val unitCostInr: Double,
    val totalCostInr: Double = unitCostInr * quantity
) {
    // Backward-compatibility properties
    val unitCostUsd: Double get() = unitCostInr
    val totalCostUsd: Double get() = totalCostInr
}

data class BOMSummary(
    val wardrobeName: String,
    val overallDimensionsFeetInches: String,
    val overallDimensionsInches: String,
    val overallDimensionsCm: String,
    val finishName: String,
    val doorStyleName: String,
    val totalVolumeCubicMeters: Float,
    val totalWeightKg: Float,
    val totalSheetBoardsRequired: Int,
    val totalEdgeBandingMeters: Float,
    val items: List<BOMItem>,
    val totalEstimatedCostInr: Double,
    val estimatedAssemblyHours: Float
) {
    // Backward-compatibility property
    val totalEstimatedCostUsd: Double get() = totalEstimatedCostInr

    fun toFormattedMarkdown(unitSystem: UnitSystem = UnitSystem.FEET_INCHES): String {
        val sb = StringBuilder()
        sb.appendLine("## Bill of Materials (BOM) — $wardrobeName")
        sb.appendLine("**Primary Dimensions:** $overallDimensionsFeetInches ($overallDimensionsCm / $overallDimensionsInches)")
        sb.appendLine("**Material Finish:** $finishName | **Door Style:** $doorStyleName")
        sb.appendLine("**Volume:** ${String.format(Locale.US, "%.2f", totalVolumeCubicMeters)} m³ | **Estimated Weight:** ${totalWeightKg.toInt()} kg")
        sb.appendLine("**Sheet Goods Required (4×8 ft):** $totalSheetBoardsRequired boards | **Edge Banding:** ${totalEdgeBandingMeters.toInt()} m")
        sb.appendLine("**Estimated Total Cost:** ${DimensionFormatter.formatCurrencyINR(totalEstimatedCostInr)} | **Assembly Time:** ${estimatedAssemblyHours} hrs")
        sb.appendLine()
        sb.appendLine("| Category | Item | Specifications | Qty | Unit Rate (INR) | Total (INR) |")
        sb.appendLine("|---|---|---|---|---|---|")
        items.forEach { item ->
            sb.appendLine("| ${item.category.title} | ${item.name} | ${item.dimensionSpec} (${item.material}) | ${item.quantity} ${item.unit} | ${DimensionFormatter.formatCurrencyINR(item.unitCostInr)} | ${DimensionFormatter.formatCurrencyINR(item.totalCostInr)} |")
        }
        return sb.toString()
    }
}

object BOMCalculator {

    fun calculateBOM(config: WardrobeConfig): BOMSummary {
        val w = config.widthCm
        val h = config.heightCm
        val d = config.depthCm
        val finish = config.finish
        val doorStyle = config.doorStyle

        val items = mutableListOf<BOMItem>()

        // 1. Carcass Structural Panels (18mm thickness standard)
        val panelMaterial = if (finish.isWood) {
            "18mm BWP HDHMR Board (${finish.title} Natural Wood Grain)"
        } else {
            "18mm Pre-Laminated HDHMR Board (${finish.title} Matte Finish)"
        }
        val backboardMaterial = "6mm Moisture-Resistant BWP Backing Sheet"

        // Side Vertical Gable Panels (2 pcs)
        val sidePanelAreaM2 = 2 * (h / 100f) * (d / 100f)
        val sidePanelInr = ((h * d / 10000f) * 2250.0).coerceAtLeast(1800.0)
        val hFtIn = DimensionFormatter.formatLength(h, UnitSystem.FEET_INCHES, compact = true)
        val dFtIn = DimensionFormatter.formatLength(d, UnitSystem.FEET_INCHES, compact = true)
        val wFtIn = DimensionFormatter.formatLength(w, UnitSystem.FEET_INCHES, compact = true)

        items.add(
            BOMItem(
                id = "carcass_sides",
                category = BOMCategory.CARCASS_PANELS,
                name = "Vertical Side Gables (Left/Right)",
                dimensionSpec = "2 pcs @ $hFtIn × $dFtIn (${h.toInt()} × ${d.toInt()} cm)",
                quantity = 2,
                unit = "pcs",
                material = panelMaterial,
                unitCostInr = sidePanelInr
            )
        )

        // Top & Bottom Deck Panels (2 pcs)
        val internalWidth = max(20f, w - 3.6f)
        val deckInr = ((internalWidth * d / 10000f) * 2100.0).coerceAtLeast(1650.0)
        val internalWFtIn = DimensionFormatter.formatLength(internalWidth, UnitSystem.FEET_INCHES, compact = true)

        items.add(
            BOMItem(
                id = "carcass_top_bottom",
                category = BOMCategory.CARCASS_PANELS,
                name = "Top Crown & Bottom Base Decks",
                dimensionSpec = "2 pcs @ $internalWFtIn × $dFtIn (${internalWidth.toInt()} × ${d.toInt()} cm)",
                quantity = 2,
                unit = "pcs",
                material = panelMaterial,
                unitCostInr = deckInr
            )
        )

        // Internal Dividers (if w > 130cm -> 1 divider, if w > 230cm -> 2 dividers)
        val dividerCount = when {
            w > 230f -> 2
            w > 130f -> 1
            else -> 0
        }
        if (dividerCount > 0) {
            val dividerHeight = max(20f, h - 3.6f)
            val dividerDepth = max(20f, d - 2f)
            val divInr = ((dividerHeight * dividerDepth / 10000f) * 1950.0).coerceAtLeast(1450.0)
            val divHFtIn = DimensionFormatter.formatLength(dividerHeight, UnitSystem.FEET_INCHES, compact = true)
            val divDFtIn = DimensionFormatter.formatLength(dividerDepth, UnitSystem.FEET_INCHES, compact = true)

            items.add(
                BOMItem(
                    id = "carcass_dividers",
                    category = BOMCategory.CARCASS_PANELS,
                    name = "Vertical Internal Partition Mullions",
                    dimensionSpec = "$dividerCount pcs @ $divHFtIn × $divDFtIn (${dividerHeight.toInt()} × ${dividerDepth.toInt()} cm)",
                    quantity = dividerCount,
                    unit = "pcs",
                    material = panelMaterial,
                    unitCostInr = divInr
                )
            )
        }

        // Backing Board (6mm HDF / BWP Ply)
        val backInr = ((h * w / 10000f) * 1150.0).coerceAtLeast(950.0)
        items.add(
            BOMItem(
                id = "carcass_back",
                category = BOMCategory.CARCASS_PANELS,
                name = "Grooved Rear Backing Board",
                dimensionSpec = "1 pc @ $hFtIn × $wFtIn (${(h - 1f).toInt()} × ${(w - 1f).toInt()} cm)",
                quantity = 1,
                unit = "pc",
                material = backboardMaterial,
                unitCostInr = backInr
            )
        )

        // Base Plinth / Toe Kick
        items.add(
            BOMItem(
                id = "carcass_plinth",
                category = BOMCategory.CARCASS_PANELS,
                name = "Recessed Toe-Kick Plinth Base",
                dimensionSpec = "1 set @ $wFtIn × 3″ (${w.toInt()} × 8 cm)",
                quantity = 1,
                unit = "set",
                material = "18mm Waterproof Moisture-Barrier Plinth",
                unitCostInr = 1450.0
            )
        )

        // 2. Doors & Facades
        when (doorStyle) {
            DoorStyle.DUAL_HINGED, DoorStyle.HINGED_DOOR -> {
                val doorCount = if (w > 200f) 4 else 2
                val doorLeafWidth = (w - 0.4f * (doorCount + 1)) / doorCount
                val doorLeafHeight = h - 8f
                val doorMat = if (finish == FinishType.SMOKED_GLASS) {
                    "4mm Smoked Tempered Safety Glass with Anodized Frame"
                } else {
                    "18mm ${finish.title} Custom Shaker Facade Leaf"
                }
                val doorUnitCostInr = if (finish == FinishType.SMOKED_GLASS) 7200.0 else 3850.0
                val dLeafWFtIn = DimensionFormatter.formatLength(doorLeafWidth, UnitSystem.FEET_INCHES, compact = true)
                val dLeafHFtIn = DimensionFormatter.formatLength(doorLeafHeight, UnitSystem.FEET_INCHES, compact = true)

                items.add(
                    BOMItem(
                        id = "facade_doors",
                        category = BOMCategory.DOORS_FACADES,
                        name = "Hinged Door Leaf Panels",
                        dimensionSpec = "$doorCount pcs @ $dLeafWFtIn × $dLeafHFtIn (${doorLeafWidth.toInt()} × ${doorLeafHeight.toInt()} cm)",
                        quantity = doorCount,
                        unit = "pcs",
                        material = doorMat,
                        unitCostInr = doorUnitCostInr
                    )
                )
            }
            DoorStyle.SLIDING_BYPASS, DoorStyle.SLIDING_DOOR, DoorStyle.MIRROR_SLIDING_DOOR -> {
                val slidingCount = if (w > 220f) 3 else 2
                val doorLeafWidth = (w / slidingCount) + 4f
                val doorLeafHeight = h - 10f
                val slidingMat = if (finish == FinishType.SMOKED_GLASS || doorStyle == DoorStyle.MIRROR_SLIDING_DOOR) {
                    "4mm Smoked/Mirrored Glass with Anodized Aluminum Sash"
                } else {
                    "18mm ${finish.title} Sliding Panel with Aluminum Integrated Profile"
                }
                val sLeafWFtIn = DimensionFormatter.formatLength(doorLeafWidth, UnitSystem.FEET_INCHES, compact = true)
                val sLeafHFtIn = DimensionFormatter.formatLength(doorLeafHeight, UnitSystem.FEET_INCHES, compact = true)

                items.add(
                    BOMItem(
                        id = "facade_sliding",
                        category = BOMCategory.DOORS_FACADES,
                        name = "Heavy-Duty Bypass Sliding Door Sashes",
                        dimensionSpec = "$slidingCount pcs @ $sLeafWFtIn × $sLeafHFtIn (${doorLeafWidth.toInt()} × ${doorLeafHeight.toInt()} cm)",
                        quantity = slidingCount,
                        unit = "pcs",
                        material = slidingMat,
                        unitCostInr = 6800.0
                    )
                )
                items.add(
                    BOMItem(
                        id = "facade_track",
                        category = BOMCategory.DOORS_FACADES,
                        name = "Top & Bottom Aluminum Sliding Track Set",
                        dimensionSpec = "1 set @ $wFtIn (${w.toInt()} cm) with silent damper rollers",
                        quantity = 1,
                        unit = "set",
                        material = "Extruded Aircraft Aluminum (Anodized)",
                        unitCostInr = 5200.0
                    )
                )
            }
            DoorStyle.ACCORDION_BI_FOLD -> {
                val leafW = w / 4f
                val leafH = h - 8f
                val lWFtIn = DimensionFormatter.formatLength(leafW, UnitSystem.FEET_INCHES, compact = true)
                val lHFtIn = DimensionFormatter.formatLength(leafH, UnitSystem.FEET_INCHES, compact = true)
                items.add(
                    BOMItem(
                        id = "facade_bifold",
                        category = BOMCategory.DOORS_FACADES,
                        name = "Flush Bi-Fold Accordion Leaves & Overhead Rail",
                        dimensionSpec = "4 leaves @ $lWFtIn × $lHFtIn (${leafW.toInt()} × ${leafH.toInt()} cm)",
                        quantity = 4,
                        unit = "leaves",
                        material = "18mm ${finish.title} with Flush Center Hinges",
                        unitCostInr = 3950.0
                    )
                )
            }
            DoorStyle.OPEN_CONCEPT -> {
                items.add(
                    BOMItem(
                        id = "facade_open",
                        category = BOMCategory.DOORS_FACADES,
                        name = "Architectural Open Face Finishing Trim",
                        dimensionSpec = "Perimeter bevel @ $wFtIn × $hFtIn (${w.toInt()} × ${h.toInt()} cm)",
                        quantity = 1,
                        unit = "set",
                        material = "2mm Impact-Resistant ABS Edge Profile",
                        unitCostInr = 1800.0
                    )
                )
            }
        }

        // Full-Length Mirror Option
        if (config.hasMirrorPanel) {
            val mirHFtIn = DimensionFormatter.formatLength(h - 45f, UnitSystem.FEET_INCHES, compact = true)
            items.add(
                BOMItem(
                    id = "facade_mirror",
                    category = BOMCategory.DOORS_FACADES,
                    name = "Full-Length Safety-Backed Float Mirror",
                    dimensionSpec = "1 pc @ 1′ 10″ × $mirHFtIn (55 × ${(h - 45).toInt()} cm, 4mm)",
                    quantity = 1,
                    unit = "pc",
                    material = "Silver Float Mirror with Polished Beveled Edges",
                    unitCostInr = 3800.0
                )
            )
        }

        // 3. Interior Storage Modules
        val compartmentWidth = if (dividerCount > 0) internalWidth / (dividerCount + 1) else internalWidth
        val compWFtIn = DimensionFormatter.formatLength(compartmentWidth, UnitSystem.FEET_INCHES, compact = true)
        val compDFtIn = DimensionFormatter.formatLength(d - 4f, UnitSystem.FEET_INCHES, compact = true)

        if (config.shelvesCount > 0) {
            items.add(
                BOMItem(
                    id = "interior_shelves",
                    category = BOMCategory.INTERIOR_MODULES,
                    name = "Adjustable Internal Shelf Boards",
                    dimensionSpec = "${config.shelvesCount} pcs @ $compWFtIn × $compDFtIn (${compartmentWidth.toInt()} × ${(d - 4).toInt()} cm)",
                    quantity = config.shelvesCount,
                    unit = "pcs",
                    material = panelMaterial,
                    unitCostInr = 1250.0
                )
            )
        }

        if (config.hangingRailsCount > 0) {
            items.add(
                BOMItem(
                    id = "interior_rails",
                    category = BOMCategory.INTERIOR_MODULES,
                    name = "Heavy-Duty Oval Clothes Hanging Rails",
                    dimensionSpec = "${config.hangingRailsCount} rods @ $compWFtIn (${compartmentWidth.toInt()} cm) with end-brackets",
                    quantity = config.hangingRailsCount,
                    unit = "rods",
                    material = "Brushed Chrome Steel (30×15mm Oval)",
                    unitCostInr = 950.0
                )
            )
        }

        if (config.drawersCount > 0) {
            items.add(
                BOMItem(
                    id = "interior_drawers",
                    category = BOMCategory.INTERIOR_MODULES,
                    name = "Modular Soft-Close Drawer Boxes",
                    dimensionSpec = "${config.drawersCount} boxes @ $compWFtIn × ${(d - 10).toInt()} cm (7″ depth)",
                    quantity = config.drawersCount,
                    unit = "boxes",
                    material = "15mm Marine Plywood with Telescopic Soft-Close Channels",
                    unitCostInr = 2950.0
                )
            )
        }

        // 4. Hardware & Fasteners
        val hingeCount = when (doorStyle) {
            DoorStyle.DUAL_HINGED -> if (w > 200f) 16 else 8
            DoorStyle.ACCORDION_BI_FOLD -> 12
            else -> 0
        }
        if (hingeCount > 0) {
            items.add(
                BOMItem(
                    id = "hardware_hinges",
                    category = BOMCategory.HARDWARE_FASTENERS,
                    name = "110° Clip-On Soft-Close Concealed Hinges (Hafele/Ebco)",
                    dimensionSpec = "$hingeCount pcs (35mm cup depth with mounting plates)",
                    quantity = hingeCount,
                    unit = "pcs",
                    material = "Cold-Rolled Nickel Plated Steel",
                    unitCostInr = 340.0
                )
            )
        }

        // Cam locks & Dowels
        val connectorSets = 16 + (dividerCount * 8) + (config.shelvesCount * 2)
        items.add(
            BOMItem(
                id = "hardware_camlocks",
                category = BOMCategory.HARDWARE_FASTENERS,
                name = "Minifix Cam & Expansion Dowel Joinery Sets",
                dimensionSpec = "$connectorSets sets (15mm zinc cam + case-hardened dowel)",
                quantity = connectorSets,
                unit = "sets",
                material = "Zinc Alloy & High-Tensile Steel",
                unitCostInr = 65.0
            )
        )

        // Shelf Pins
        val pinCount = config.shelvesCount * 4
        if (pinCount > 0) {
            items.add(
                BOMItem(
                    id = "hardware_pins",
                    category = BOMCategory.HARDWARE_FASTENERS,
                    name = "Anti-Tip Nickel Shelf Support Pins",
                    dimensionSpec = "$pinCount pcs (5mm shaft with silicone sleeve)",
                    quantity = pinCount,
                    unit = "pcs",
                    material = "Nickel-Plated Steel with Rubber Grip",
                    unitCostInr = 28.0
                )
            )
        }

        // Wall Anchoring Anti-Tip Safety Kit
        items.add(
            BOMItem(
                id = "hardware_safety_anchor",
                category = BOMCategory.HARDWARE_FASTENERS,
                name = "Heavy-Duty Wall Anchor Anti-Tip Safety Brackets",
                dimensionSpec = "2 heavy brackets with expansion fastener anchors",
                quantity = 2,
                unit = "sets",
                material = "3mm Galvanized Carbon Steel",
                unitCostInr = 650.0
            )
        )

        // 5. Lighting & Sensors
        if (config.ledLighting != LedLighting.NONE) {
            val stripMeters = ceil((h * 2 + w) / 100f).toInt()
            val stripFt = (stripMeters * 3.28f).roundToInt()
            items.add(
                BOMItem(
                    id = "elec_led_strip",
                    category = BOMCategory.LIGHTING_ELECTRICAL,
                    name = "Recessed Aluminum 45° LED Profile & COB Strip",
                    dimensionSpec = "$stripMeters meters ($stripFt ft) — ${config.ledLighting.title} (${config.ledLighting.tempKelvin})",
                    quantity = stripMeters,
                    unit = "m",
                    material = "High-CRI 90+ 24V COB Dotless LED + Opal Diffuser",
                    unitCostInr = 1150.0
                )
            )
            items.add(
                BOMItem(
                    id = "elec_driver",
                    category = BOMCategory.LIGHTING_ELECTRICAL,
                    name = "24V 60W Slim Driver & Proximity Door Sensor",
                    dimensionSpec = "1 unit auto-on when door opens",
                    quantity = 1,
                    unit = "set",
                    material = "ISI Certified Constant Voltage Power Supply",
                    unitCostInr = 2650.0
                )
            )
        }

        // Calculations for Total Summary in INR
        val totalCostInr = items.sumOf { it.totalCostInr }
        val volumeM3 = config.volumeCubicMeters
        val totalWeightKg = (volumeM3 * 220f) + (config.drawersCount * 6f) + (if (config.hasMirrorPanel) 14f else 0f)

        // Total sheet boards calculation (standard 2440 x 1220 mm / 8x4 ft = 2.976 m²)
        val totalCarcassAreaM2 = sidePanelAreaM2 + (2 * internalWidth * d / 10000f) +
                (dividerCount * (h - 3.6f) * (d - 2f) / 10000f) +
                (config.shelvesCount * compartmentWidth * (d - 4) / 10000f)
        val sheetBoardsNeeded = max(2, ceil((totalCarcassAreaM2 * 1.15f) / 2.97f).toInt())
        val edgeBandingMeters = ((h * 4 + w * 4 + config.shelvesCount * compartmentWidth * 2) / 100f) * 1.1f
        val assemblyHours = 2.5f + (dividerCount * 0.8f) + (config.drawersCount * 0.5f) + (config.shelvesCount * 0.2f)

        val ftInDimensions = DimensionFormatter.formatDimensions(w, h, d, UnitSystem.FEET_INCHES, compact = false)
        val inchDimensions = DimensionFormatter.formatDimensions(w, h, d, UnitSystem.INCHES, compact = false)
        val cmDimensions = "${w.toInt()} W × ${h.toInt()} H × ${d.toInt()} D cm"

        return BOMSummary(
            wardrobeName = config.name,
            overallDimensionsFeetInches = ftInDimensions,
            overallDimensionsInches = inchDimensions,
            overallDimensionsCm = cmDimensions,
            finishName = finish.title,
            doorStyleName = doorStyle.title,
            totalVolumeCubicMeters = volumeM3,
            totalWeightKg = totalWeightKg,
            totalSheetBoardsRequired = sheetBoardsNeeded,
            totalEdgeBandingMeters = edgeBandingMeters,
            items = items,
            totalEstimatedCostInr = totalCostInr,
            estimatedAssemblyHours = assemblyHours
        )
    }
}
