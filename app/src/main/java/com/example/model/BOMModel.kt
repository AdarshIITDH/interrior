package com.example.model

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

enum class BOMCategory(val title: String, val iconName: String) {
    CARCASS_PANELS("Structural Panels", "dashboard"),
    DOORS_FACADES("Doors & Facades", "door_front"),
    LAMINATES("Laminates & Veneer", "layers"),
    CONSUMABLES("Glues & Edge Bending", "science"),
    HARDWARE_FASTENERS("Hardware & Clamps", "build"),
    INTERIOR_MODULES("Interior Storage", "inventory_2"),
    LABOUR_SERVICES("Carpentry & Labour", "engineering"),

    // Category aliases for export engines
    BOARDS("Structural Boards", "dashboard"),
    ADHESIVES("Glues & Tapes", "science"),
    HARDWARE("Hardware & Hinges", "build"),
    HANDLES("Handles & Knobs", "build"),
    LIGHTING("Lighting & Sensors", "lightbulb"),
    ACCESSORIES("Interior Accessories", "inventory_2"),
    LABOUR("Labour & Services", "engineering")
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
    val details: String get() = dimensionSpec
    val unitRate: Double get() = unitCostInr
    val totalCost: Double get() = totalCostInr
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
    val estimatedAssemblyHours: Float,
    val detailedBOM: DetailedWardrobeBOM
) {
    val totalCost: Double get() = totalEstimatedCostInr
    val materialsCost: Double get() = detailedBOM.materialTotal
    val hardwareCost: Double get() = detailedBOM.hardwareTotal
    val labourCost: Double get() = detailedBOM.labourCost
    val totalEstimatedCostUsd: Double get() = totalEstimatedCostInr

    fun toFormattedMarkdown(unitSystem: UnitSystem = UnitSystem.FEET_INCHES): String {
        return detailedBOM.toFormattedMarkdown(unitSystem)
    }
}

typealias BOMResult = BOMSummary

object BOMCalculator {

    fun sheetsRequired(areaSqFt: Double): Int {
        return ceil(areaSqFt / WardrobeCostRates.SHEET_AREA_SQFT).toInt()
    }

    /**
     * Calculates default shelf count based on height in feet.
     */
    fun getShelfCount(heightFt: Double): Int {
        return when {
            heightFt <= 3.0 -> 1
            heightFt <= 5.0 -> 2
            heightFt <= 7.0 -> 3
            else -> 4
        }
    }

    /**
     * Calculates default door/shutter count based on width in feet.
     */
    fun getDoorCount(widthFt: Double): Int {
        return when {
            widthFt <= 3.0 -> 2
            widthFt <= 5.0 -> 3
            widthFt <= 8.0 -> 4
            else -> 5
        }
    }

    /**
     * Calculates vertical partition mullions based on shutter count:
     * Upto 2 doors = 0, 3-4 = 1, 5-6 = 2, 7-8 = 3
     */
    fun getVerticalPartitions(doors: Int): Int {
        return when {
            doors <= 2 -> 0
            doors <= 4 -> 1
            doors <= 6 -> 2
            else -> 3
        }
    }

    /**
     * Calculates soft-close hinges/clamps per door based on height in feet.
     */
    fun getClampsPerDoor(heightFt: Double): Int {
        return when {
            heightFt <= 3.0 -> 2
            heightFt <= 4.5 -> 3
            heightFt <= 6.0 -> 4
            heightFt <= 8.0 -> 5
            else -> 6
        }
    }

    fun calculateBOM(config: WardrobeConfig): BOMSummary {
        val widthCm = config.widthCm
        val heightCm = config.heightCm
        val depthCm = config.depthCm

        // Convert cm to feet (1 foot = 30.48 cm)
        val widthFt = widthCm / 30.48
        val heightFt = heightCm / 30.48
        val depthFt = depthCm / 30.48

        // Shutters based on width or config override
        val defaultDoors = getDoorCount(widthFt)
        val doors = if (config.doorStyle == DoorStyle.OPEN_CONCEPT) 0 else defaultDoors

        // Shelves based on height or config
        val calculatedShelves = getShelfCount(heightFt)
        val shelves = if (config.shelvesCount > 0) config.shelvesCount else calculatedShelves

        // Vertical partitions based on door count
        val verticalPartitions = if (config.doorStyle == DoorStyle.OPEN_CONCEPT) {
            if (widthFt > 7.0) 2 else if (widthFt > 4.5) 1 else 0
        } else {
            getVerticalPartitions(doors)
        }

        // ---------------- CARCASS BOARD AREAS ----------------
        val backArea = widthFt * heightFt
        val sidesArea = 2.0 * depthFt * heightFt
        val topBottomArea = 2.0 * depthFt * widthFt
        val shelvesArea = shelves * depthFt * widthFt
        val verticalPartitionArea = verticalPartitions * depthFt * heightFt

        val carcassBoardArea = backArea + sidesArea + topBottomArea + shelvesArea + verticalPartitionArea

        // ---------------- SHUTTER AREA ----------------
        val shutterArea = widthFt * heightFt

        // ---------------- BOARD SHEETS ----------------
        val plywoodSheets = sheetsRequired(carcassBoardArea)
        val shutterBlockboardSheets = if (doors > 0) sheetsRequired(shutterArea) else 0

        val plywoodPurchasedArea = plywoodSheets * WardrobeCostRates.SHEET_AREA_SQFT
        val shutterPurchasedArea = shutterBlockboardSheets * WardrobeCostRates.SHEET_AREA_SQFT

        val plywoodCost = plywoodPurchasedArea * WardrobeCostRates.PLYWOOD_RATE_PER_SQFT
        val shutterBlockboardCost = shutterPurchasedArea * WardrobeCostRates.BLOCKBOARD_RATE_PER_SQFT

        // ---------------- INTERNAL LAMINATE ----------------
        // Back + sides + top/bottom: one visible side
        val singleSideCarcassLaminateArea = backArea + sidesArea + topBottomArea
        // Shelves visible on both sides
        val shelfLaminateArea = shelvesArea * 2.0
        // Vertical partitions visible on both sides
        val verticalPartitionLaminateArea = verticalPartitionArea * 2.0

        val internalLaminateArea = singleSideCarcassLaminateArea + shelfLaminateArea + verticalPartitionLaminateArea
        val internalLaminateSheets = sheetsRequired(internalLaminateArea)
        val internalLaminateCost = internalLaminateSheets * WardrobeCostRates.INTERNAL_LAMINATE_RATE_PER_SHEET

        // ---------------- EXTERIOR LAMINATE ----------------
        // Shutters laminated on both sides
        val exteriorLaminateArea = if (doors > 0) shutterArea * 2.0 else 0.0
        val exteriorLaminateSheets = if (doors > 0) sheetsRequired(exteriorLaminateArea) else 0
        val exteriorLaminateCost = exteriorLaminateSheets * WardrobeCostRates.EXTERNAL_LAMINATE_RATE_PER_SHEET

        // ---------------- FEVICOL MARINE ----------------
        val totalLaminateSheets = internalLaminateSheets + exteriorLaminateSheets
        val marineFevicolKg = totalLaminateSheets * WardrobeCostRates.MARINE_FEVI_PER_SHEET_KG
        val marineFevicolCost = marineFevicolKg * WardrobeCostRates.MARINE_FEVI_RATE_PER_KG

        // ---------------- EDGE BENDING ----------------
        val shutterWidth = if (doors > 0) widthFt / doors else 0.0
        // Perimeter of shutters in feet converted to meters (1 ft = 0.3048 m)
        val shutterEdgeLengthFt = if (doors > 0) doors * 2.0 * (shutterWidth + heightFt) else 0.0
        val carcassEdgeLengthFt = (2.0 * widthFt) + (4.0 * heightFt) + (shelves * widthFt) + (verticalPartitions * heightFt)
        val totalEdgeLengthMeters = (shutterEdgeLengthFt + carcassEdgeLengthFt) * 0.3048
        val edgeRolls = max(1, ceil(totalEdgeLengthMeters / WardrobeCostRates.PVC_EDGE_ROLL_LENGTH_M).toInt())
        val edgeTapeCost = edgeRolls * WardrobeCostRates.PVC_EDGE_ROLL_RATE

        // ---------------- PROBOND ----------------
        val probondKg = edgeRolls * 1.0
        val probondCost = probondKg * WardrobeCostRates.PROBOND_RATE_PER_KG

        // ---------------- CLAMPS / HINGES ----------------
        val effectiveDoorsForClamps = if (doors > 0) doors else 0
        val clampsPerDoor = getClampsPerDoor(heightFt)
        val totalClamps = effectiveDoorsForClamps * clampsPerDoor
        val clampCost = totalClamps * WardrobeCostRates.CLAMP_RATE_PER_PC

        // Additional Hardware for Drawers, Rails, Lighting, Handles & Organizers
        val drawerChannelsCost = config.drawersCount * 950.0 // Telescopic soft-close pair per drawer
        val hangingRodCost = config.hangingRailsCount * 450.0 // Oval chrome rail + brackets
        val ledLightingCost = when (config.ledLighting) {
            LedLighting.NONE -> 0.0
            LedLighting.WARM_AMBIENT -> 2800.0
            LedLighting.NATURAL_DAYLIGHT -> 2800.0
            LedLighting.CYAN_HOLOGRAPHIC -> 3200.0
        }

        // Handles
        val handleRate = when (config.handleStyle) {
            "Brushed Brass Profile" -> 550.0
            "Rose Gold Knob" -> 280.0
            "Concealed J-Pull" -> 400.0
            "Brushed Chrome Edge" -> 480.0
            else -> 350.0
        }
        val handlesCount = if (doors > 0) doors else 0
        val handleCost = handlesCount * handleRate

        // Accessories
        val mirrorCost = if (config.hasMirrorPanel) 2200.0 else 0.0
        val shoeRackCost = if (config.hasShoeRack) 1200.0 else 0.0
        val jewelryTrayCost = if (config.hasJewelryTray) 1850.0 else 0.0
        val trouserRackCost = if (config.hasTrouserRack) 1450.0 else 0.0
        val accessoriesCost = mirrorCost + shoeRackCost + jewelryTrayCost + trouserRackCost

        // ---------------- LABOUR & TOTALS ----------------
        val materialTotal = plywoodCost +
                shutterBlockboardCost +
                internalLaminateCost +
                exteriorLaminateCost +
                marineFevicolCost +
                edgeTapeCost +
                probondCost

        val hardwareTotal = clampCost + drawerChannelsCost + hangingRodCost + ledLightingCost + handleCost + accessoriesCost

        val labourCost = shutterArea * WardrobeCostRates.LABOUR_RATE_PER_SQFT

        // 5% miscellaneous on Material + Hardware + Labour
        val miscellaneous = (materialTotal + hardwareTotal + labourCost) * WardrobeCostRates.MISC_PERCENT

        val finalCost = materialTotal + hardwareTotal + labourCost + miscellaneous

        // ---------------- ITEMIZED BOM ITEMS ----------------
        val items = mutableListOf<BOMItem>()

        // 1. Carcass Plywood
        items.add(
            BOMItem(
                id = "carcass_plywood",
                category = BOMCategory.CARCASS_PANELS,
                name = "18mm BWP/MR Plywood (Carcass & Shelves)",
                dimensionSpec = "$plywoodSheets Sheets (8×4 ft) • ${String.format(Locale.US, "%.1f", carcassBoardArea)} sq.ft",
                quantity = plywoodSheets,
                unit = "sheets",
                material = "18mm Commercial / BWP Plywood",
                unitCostInr = WardrobeCostRates.SHEET_AREA_SQFT * WardrobeCostRates.PLYWOOD_RATE_PER_SQFT
            )
        )

        // 2. Shutter Blockboard
        if (doors > 0) {
            items.add(
                BOMItem(
                    id = "shutter_blockboard",
                    category = BOMCategory.DOORS_FACADES,
                    name = "18mm Pinewood Blockboard (Shutter Leaves)",
                    dimensionSpec = "$shutterBlockboardSheets Sheets (8×4 ft) • $doors Shutters (${String.format(Locale.US, "%.1f", shutterArea)} sq.ft)",
                    quantity = shutterBlockboardSheets,
                    unit = "sheets",
                    material = "18mm Grade-I Blockboard (Anti-Warping)",
                    unitCostInr = WardrobeCostRates.SHEET_AREA_SQFT * WardrobeCostRates.BLOCKBOARD_RATE_PER_SQFT
                )
            )
        }

        // 3. Laminates
        items.add(
            BOMItem(
                id = "internal_laminate",
                category = BOMCategory.LAMINATES,
                name = "0.8mm Internal Liner Off-White Laminate",
                dimensionSpec = "$internalLaminateSheets Sheets (8×4 ft) • ${String.format(Locale.US, "%.1f", internalLaminateArea)} sq.ft",
                quantity = internalLaminateSheets,
                unit = "sheets",
                material = "0.8mm Matte Liner Sheet",
                unitCostInr = WardrobeCostRates.INTERNAL_LAMINATE_RATE_PER_SHEET
            )
        )

        if (doors > 0) {
            items.add(
                BOMItem(
                    id = "external_laminate",
                    category = BOMCategory.LAMINATES,
                    name = "1.0mm Exterior Decorative Texture Laminate",
                    dimensionSpec = "$exteriorLaminateSheets Sheets (8×4 ft) • ${config.finish.title}",
                    quantity = exteriorLaminateSheets,
                    unit = "sheets",
                    material = "1.0mm Premium Suede/Woodgrain Finish",
                    unitCostInr = WardrobeCostRates.EXTERNAL_LAMINATE_RATE_PER_SHEET
                )
            )
        }

        // 4. Consumables
        items.add(
            BOMItem(
                id = "marine_fevicol",
                category = BOMCategory.CONSUMABLES,
                name = "Fevicol Marine Waterproof Adhesive",
                dimensionSpec = "${String.format(Locale.US, "%.2f", marineFevicolKg)} kg (${WardrobeCostRates.MARINE_FEVI_PER_SHEET_KG} kg/sheet)",
                quantity = ceil(marineFevicolKg).toInt(),
                unit = "kg",
                material = "Waterproof Polyvinyl Acetate Emulsion",
                unitCostInr = WardrobeCostRates.MARINE_FEVI_RATE_PER_KG
            )
        )

        items.add(
            BOMItem(
                id = "pvc_edge_band",
                category = BOMCategory.CONSUMABLES,
                name = "2mm PVC Edge Bending Tape",
                dimensionSpec = "$edgeRolls Rolls (${String.format(Locale.US, "%.1f", totalEdgeLengthMeters)} m total)",
                quantity = edgeRolls,
                unit = "rolls",
                material = "2mm High-Impact PVC Profile",
                unitCostInr = WardrobeCostRates.PVC_EDGE_ROLL_RATE
            )
        )

        items.add(
            BOMItem(
                id = "probond_adhesive",
                category = BOMCategory.CONSUMABLES,
                name = "Fevicol Probond (PVC Edge Adhesive)",
                dimensionSpec = "${String.format(Locale.US, "%.1f", probondKg)} kg (1 kg per roll)",
                quantity = ceil(probondKg).toInt(),
                unit = "kg",
                material = "Synthetic Rubber Adhesive",
                unitCostInr = WardrobeCostRates.PROBOND_RATE_PER_KG
            )
        )

        // 5. Hardware
        if (totalClamps > 0) {
            items.add(
                BOMItem(
                    id = "hardware_clamps",
                    category = BOMCategory.HARDWARE_FASTENERS,
                    name = "Soft-Close Concealed Hinges (Clamps)",
                    dimensionSpec = "$totalClamps pcs ($clampsPerDoor clamps per shutter)",
                    quantity = totalClamps,
                    unit = "pcs",
                    material = "Nickel Plated Hydraulic 3D Hinge",
                    unitCostInr = WardrobeCostRates.CLAMP_RATE_PER_PC
                )
            )
        }

        if (config.drawersCount > 0) {
            items.add(
                BOMItem(
                    id = "hardware_drawers",
                    category = BOMCategory.INTERIOR_MODULES,
                    name = "Modular Soft-Close Drawer Boxes & Telescopic Channels",
                    dimensionSpec = "${config.drawersCount} sets with 45kg load-rated ball bearing sliders",
                    quantity = config.drawersCount,
                    unit = "sets",
                    material = "15mm Ply Box + Zinc Plated Telescopic Slides",
                    unitCostInr = 950.0
                )
            )
        }

        if (config.hangingRailsCount > 0) {
            items.add(
                BOMItem(
                    id = "hardware_hanging_rails",
                    category = BOMCategory.INTERIOR_MODULES,
                    name = "Heavy-Duty Oval Wardrobe Hanging Rods",
                    dimensionSpec = "${config.hangingRailsCount} rods with die-cast end sockets",
                    quantity = config.hangingRailsCount,
                    unit = "rods",
                    material = "Chrome Plated Steel (30×15mm)",
                    unitCostInr = 450.0
                )
            )
        }

        if (config.ledLighting != LedLighting.NONE) {
            items.add(
                BOMItem(
                    id = "lighting_led",
                    category = BOMCategory.HARDWARE_FASTENERS,
                    name = "Integrated LED Strip & Smart Sensor Driver",
                    dimensionSpec = "45° Aluminum profile + 24V COB Strip (${config.ledLighting.title})",
                    quantity = 1,
                    unit = "set",
                    material = "Opal Diffuser + ISI Power Supply",
                    unitCostInr = 2800.0
                )
            )
        }

        if (handlesCount > 0) {
            items.add(
                BOMItem(
                    id = "hardware_handles",
                    category = BOMCategory.HARDWARE_FASTENERS,
                    name = "Designer Handles (${config.handleStyle})",
                    dimensionSpec = "$handlesCount pcs @ ₹${handleRate.toInt()}/pc",
                    quantity = handlesCount,
                    unit = "pcs",
                    material = "Solid Brass / Anodized Aluminum Profile",
                    unitCostInr = handleRate
                )
            )
        }

        if (config.hasMirrorPanel) {
            items.add(
                BOMItem(
                    id = "accessory_mirror",
                    category = BOMCategory.INTERIOR_MODULES,
                    name = "Full-Height Dressing Mirror Shutter Panel",
                    dimensionSpec = "5mm Beveled Float Mirror with safety vinyl backing",
                    quantity = 1,
                    unit = "panel",
                    material = "Saint-Gobain / Modiguard 5mm Extra-Clear Mirror",
                    unitCostInr = 2200.0
                )
            )
        }

        if (config.hasJewelryTray) {
            items.add(
                BOMItem(
                    id = "accessory_jewelry_tray",
                    category = BOMCategory.INTERIOR_MODULES,
                    name = "Modular Velvet Jewelry & Watch Organizer Tray",
                    dimensionSpec = "Multi-compartment velvet lined divider with ring rolls",
                    quantity = 1,
                    unit = "tray",
                    material = "Handcrafted MDF with Plush Beige/Grey Velvet",
                    unitCostInr = 1850.0
                )
            )
        }

        if (config.hasTrouserRack) {
            items.add(
                BOMItem(
                    id = "accessory_trouser_rack",
                    category = BOMCategory.INTERIOR_MODULES,
                    name = "Telescopic Pull-Out Trouser & Tie Rack",
                    dimensionSpec = "9-arm anti-slip rubber padded hanging bars",
                    quantity = 1,
                    unit = "rack",
                    material = "Chrome Steel Rails + Soft-Close Damper",
                    unitCostInr = 1450.0
                )
            )
        }

        if (config.hasShoeRack) {
            items.add(
                BOMItem(
                    id = "accessory_shoe_rack",
                    category = BOMCategory.INTERIOR_MODULES,
                    name = "Slanted Metallic Shoe Organizer Tier",
                    dimensionSpec = "Dual-tier ventilated mesh wire racks",
                    quantity = 1,
                    unit = "tier",
                    material = "Powder-Coated Anthracite Steel Wire",
                    unitCostInr = 1200.0
                )
            )
        }

        // 6. Skilled Labour
        items.add(
            BOMItem(
                id = "labour_carpentry",
                category = BOMCategory.LABOUR_SERVICES,
                name = "Skilled Carpentry & On-Site Installation",
                dimensionSpec = "${String.format(Locale.US, "%.1f", shutterArea)} sq.ft @ ₹${WardrobeCostRates.LABOUR_RATE_PER_SQFT.toInt()}/sq.ft",
                quantity = ceil(shutterArea).toInt(),
                unit = "sq.ft",
                material = "Master Carpentry, Levelling & Finishing",
                unitCostInr = WardrobeCostRates.LABOUR_RATE_PER_SQFT
            )
        )

        val totalWeightKg = (plywoodSheets * 28f) + (shutterBlockboardSheets * 24f) + (config.drawersCount * 6f)
        val assemblyHours = 3.0f + (doors * 0.75f) + (config.drawersCount * 0.5f) + (shelves * 0.2f)

        val detailedBOM = DetailedWardrobeBOM(
            widthFt = widthFt,
            heightFt = heightFt,
            depthFt = depthFt,
            widthCm = widthCm,
            heightCm = heightCm,
            depthCm = depthCm,
            shuttersCount = doors,
            shelvesCount = shelves,
            verticalPartitionsCount = verticalPartitions,
            drawersCount = config.drawersCount,
            hangingRailsCount = config.hangingRailsCount,
            backAreaSqFt = backArea,
            sidesAreaSqFt = sidesArea,
            topBottomAreaSqFt = topBottomArea,
            shelvesAreaSqFt = shelvesArea,
            verticalPartitionAreaSqFt = verticalPartitionArea,
            carcassBoardAreaSqFt = carcassBoardArea,
            shutterAreaSqFt = shutterArea,
            carcassPlywoodSheets = plywoodSheets,
            carcassPlywoodPurchasedSqFt = plywoodPurchasedArea,
            carcassPlywoodCost = plywoodCost,
            shutterBlockboardSheets = shutterBlockboardSheets,
            shutterPurchasedSqFt = shutterPurchasedArea,
            shutterBlockboardCost = shutterBlockboardCost,
            internalLaminateAreaSqFt = internalLaminateArea,
            internalLaminateSheets = internalLaminateSheets,
            internalLaminateCost = internalLaminateCost,
            exteriorLaminateAreaSqFt = exteriorLaminateArea,
            exteriorLaminateSheets = exteriorLaminateSheets,
            exteriorLaminateCost = exteriorLaminateCost,
            totalLaminateSheets = totalLaminateSheets,
            marineFevicolKg = marineFevicolKg,
            marineFevicolCost = marineFevicolCost,
            shutterEdgeLengthM = totalEdgeLengthMeters,
            pvcEdgeRolls = edgeRolls,
            pvcEdgeTapeCost = edgeTapeCost,
            probondKg = probondKg,
            probondCost = probondCost,
            clampsPerDoor = clampsPerDoor,
            totalClamps = totalClamps,
            clampCost = clampCost,
            handlesCount = handlesCount,
            handleCost = handleCost,
            drawerChannelsCost = drawerChannelsCost,
            hangingRodCost = hangingRodCost,
            ledLightingCost = ledLightingCost,
            accessoriesCost = accessoriesCost,
            materialTotal = materialTotal,
            hardwareTotal = hardwareTotal,
            labourCost = labourCost,
            miscellaneousCost = miscellaneous,
            finalCost = finalCost,
            totalWeightKg = totalWeightKg,
            estimatedAssemblyHours = assemblyHours,
            items = items
        )

        val ftInDimensions = DimensionFormatter.formatDimensions(widthCm, heightCm, depthCm, UnitSystem.FEET_INCHES, compact = false)
        val inchDimensions = DimensionFormatter.formatDimensions(widthCm, heightCm, depthCm, UnitSystem.INCHES, compact = false)
        val cmDimensions = "${widthCm.toInt()} W × ${heightCm.toInt()} H × ${depthCm.toInt()} D cm"

        return BOMSummary(
            wardrobeName = config.name,
            overallDimensionsFeetInches = ftInDimensions,
            overallDimensionsInches = inchDimensions,
            overallDimensionsCm = cmDimensions,
            finishName = config.finish.title,
            doorStyleName = config.doorStyle.title,
            totalVolumeCubicMeters = config.volumeCubicMeters,
            totalWeightKg = totalWeightKg,
            totalSheetBoardsRequired = plywoodSheets + shutterBlockboardSheets,
            totalEdgeBandingMeters = totalEdgeLengthMeters.toFloat(),
            items = items,
            totalEstimatedCostInr = finalCost,
            estimatedAssemblyHours = assemblyHours,
            detailedBOM = detailedBOM
        )
    }
}
