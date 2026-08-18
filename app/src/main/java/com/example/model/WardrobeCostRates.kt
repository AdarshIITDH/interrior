package com.example.model

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Standard industry rates & material yield parameters for custom bespoke wardrobes.
 * Rates in INR (₹) calibrated against authentic Indian carpentry and modular interior standards.
 */
object WardrobeCostRates {
    const val SHEET_AREA_SQFT = 32.0 // Standard 8x4 ft board (2440 x 1220 mm)
    const val PLYWOOD_RATE_PER_SQFT = 110.0 // 18mm BWP/MR Plywood per sq.ft
    const val BLOCKBOARD_RATE_PER_SQFT = 110.0 // 18mm/25mm Blockboard for shutters per sq.ft

    const val INTERNAL_LAMINATE_RATE_PER_SHEET = 550.0 // 0.8mm Liner / Off-white laminate
    const val EXTERNAL_LAMINATE_RATE_PER_SHEET = 1200.0 // 1.0mm Decorative / Texture laminate

    const val MARINE_FEVI_RATE_PER_KG = 320.0 // Fevicol Marine waterproof glue
    const val MARINE_FEVI_PER_SHEET_KG = 0.75 // 0.75 kg per sheet laminated

    const val PVC_EDGE_ROLL_RATE = 400.0 // 2mm PVC Edge tape roll
    const val PVC_EDGE_ROLL_LENGTH_M = 50.0 // 50 meters per roll

    const val PROBOND_RATE_PER_KG = 430.0 // Fevicol Probond / Edge banding adhesive per roll
    const val LABOUR_RATE_PER_SQFT = 300.0 // Skilled carpentry fabrication rate based on shutter face area
    const val CLAMP_RATE_PER_PC = 150.0 // Soft-close auto-hinge / clamp rate per unit
    const val MISC_PERCENT = 0.05 // 5% miscellaneous hardware & logistics buffer
}

/**
 * Detailed Bill of Materials (BOM) Cost Breakdown based on actual carpentry estimation logic.
 */
data class DetailedWardrobeBOM(
    // Dimensions in Feet & Centimeters
    val widthFt: Double,
    val heightFt: Double,
    val depthFt: Double,
    val widthCm: Float,
    val heightCm: Float,
    val depthCm: Float,

    // Component counts
    val shuttersCount: Int,
    val shelvesCount: Int,
    val verticalPartitionsCount: Int,
    val drawersCount: Int,
    val hangingRailsCount: Int,

    // Surface Areas (sq.ft)
    val backAreaSqFt: Double,
    val sidesAreaSqFt: Double,
    val topBottomAreaSqFt: Double,
    val shelvesAreaSqFt: Double,
    val verticalPartitionAreaSqFt: Double,
    val carcassBoardAreaSqFt: Double,
    val shutterAreaSqFt: Double,

    // Boards & Laminates
    val carcassPlywoodSheets: Int,
    val carcassPlywoodPurchasedSqFt: Double,
    val carcassPlywoodCost: Double,

    val shutterBlockboardSheets: Int,
    val shutterPurchasedSqFt: Double,
    val shutterBlockboardCost: Double,

    val internalLaminateAreaSqFt: Double,
    val internalLaminateSheets: Int,
    val internalLaminateCost: Double,

    val exteriorLaminateAreaSqFt: Double,
    val exteriorLaminateSheets: Int,
    val exteriorLaminateCost: Double,

    // Consumables
    val totalLaminateSheets: Int,
    val marineFevicolKg: Double,
    val marineFevicolCost: Double,

    val shutterEdgeLengthM: Double,
    val pvcEdgeRolls: Int,
    val pvcEdgeTapeCost: Double,

    val probondKg: Double,
    val probondCost: Double,

    // Hardware & Add-ons
    val clampsPerDoor: Int,
    val totalClamps: Int,
    val clampCost: Double,
    val handlesCount: Int = 0,
    val handleCost: Double = 0.0,
    val drawerChannelsCost: Double,
    val hangingRodCost: Double,
    val ledLightingCost: Double,
    val accessoriesCost: Double = 0.0,

    // Labor & Subtotals
    val materialTotal: Double,
    val hardwareTotal: Double,
    val labourCost: Double,
    val miscellaneousCost: Double,
    val finalCost: Double,

    // Physical mass & time
    val totalWeightKg: Float,
    val estimatedAssemblyHours: Float,
    val items: List<BOMItem>
) {
    fun toFormattedMarkdown(unitSystem: UnitSystem = UnitSystem.FEET_INCHES): String {
        val sb = StringBuilder()
        val formattedDims = DimensionFormatter.formatDimensions(widthCm, heightCm, depthCm, unitSystem, compact = false)
        sb.appendLine("## Detailed Wardrobe Costing & Bill of Materials (BOM)")
        sb.appendLine("**Size:** $formattedDims (${String.format(Locale.US, "%.2f × %.2f × %.2f ft", widthFt, heightFt, depthFt)})")
        sb.appendLine("**Configuration:** $shuttersCount Shutters • $shelvesCount Shelves • $verticalPartitionsCount Partition(s) • $drawersCount Drawers")
        sb.appendLine()
        sb.appendLine("### 1. Board & Panel Details")
        sb.appendLine("- **Carcass Board Area:** ${String.format(Locale.US, "%.2f sq.ft", carcassBoardAreaSqFt)} → **$carcassPlywoodSheets Sheets (8×4 ft)** = ${DimensionFormatter.formatCurrencyINR(carcassPlywoodCost)}")
        sb.appendLine("- **Shutter Board Area:** ${String.format(Locale.US, "%.2f sq.ft", shutterAreaSqFt)} → **$shutterBlockboardSheets Sheets (8×4 ft)** = ${DimensionFormatter.formatCurrencyINR(shutterBlockboardCost)}")
        sb.appendLine()
        sb.appendLine("### 2. Laminate Details")
        sb.appendLine("- **Internal Liner Laminate:** ${String.format(Locale.US, "%.2f sq.ft", internalLaminateAreaSqFt)} → **$internalLaminateSheets Sheets** = ${DimensionFormatter.formatCurrencyINR(internalLaminateCost)}")
        sb.appendLine("- **Exterior Decorative Laminate:** ${String.format(Locale.US, "%.2f sq.ft", exteriorLaminateAreaSqFt)} → **$exteriorLaminateSheets Sheets** = ${DimensionFormatter.formatCurrencyINR(exteriorLaminateCost)}")
        sb.appendLine()
        sb.appendLine("### 3. Consumables & Joinery")
        sb.appendLine("- **Fevicol Marine Adhesive:** ${String.format(Locale.US, "%.2f kg", marineFevicolKg)} = ${DimensionFormatter.formatCurrencyINR(marineFevicolCost)}")
        sb.appendLine("- **2mm PVC Edge Bending Tape:** ${String.format(Locale.US, "%.2f m (%d Rolls)", shutterEdgeLengthM, pvcEdgeRolls)} = ${DimensionFormatter.formatCurrencyINR(pvcEdgeTapeCost)}")
        sb.appendLine("- **Fevicol Probond:** ${String.format(Locale.US, "%.2f kg", probondKg)} = ${DimensionFormatter.formatCurrencyINR(probondCost)}")
        sb.appendLine()
        sb.appendLine("### 4. Hardware & Fittings")
        sb.appendLine("- **Soft-Close Hinges / Clamps:** $totalClamps pcs ($clampsPerDoor/door) = ${DimensionFormatter.formatCurrencyINR(clampCost)}")
        if (drawersCount > 0) sb.appendLine("- **Drawer Telescopic Channels:** $drawersCount sets = ${DimensionFormatter.formatCurrencyINR(drawerChannelsCost)}")
        if (hangingRailsCount > 0) sb.appendLine("- **Hanging Rods:** $hangingRailsCount rods = ${DimensionFormatter.formatCurrencyINR(hangingRodCost)}")
        if (ledLightingCost > 0) sb.appendLine("- **LED Lighting & Driver:** = ${DimensionFormatter.formatCurrencyINR(ledLightingCost)}")
        sb.appendLine()
        sb.appendLine("### 5. Fabrication & Labor")
        sb.appendLine("- **Skilled Carpentry Labor:** ${String.format(Locale.US, "%.2f sq.ft shutter face", shutterAreaSqFt)} @ ₹${WardrobeCostRates.LABOUR_RATE_PER_SQFT.toInt()}/sq.ft = ${DimensionFormatter.formatCurrencyINR(labourCost)}")
        sb.appendLine()
        sb.appendLine("### 6. Final Financial Summary")
        sb.appendLine("- **Total Material:** ${DimensionFormatter.formatCurrencyINR(materialTotal)}")
        sb.appendLine("- **Total Hardware:** ${DimensionFormatter.formatCurrencyINR(hardwareTotal)}")
        sb.appendLine("- **Total Labor:** ${DimensionFormatter.formatCurrencyINR(labourCost)}")
        sb.appendLine("- **Miscellaneous & Logistics (5%):** ${DimensionFormatter.formatCurrencyINR(miscellaneousCost)}")
        sb.appendLine("**FINAL ESTIMATED COST:** ${DimensionFormatter.formatCurrencyINR(finalCost)}")
        return sb.toString()
    }
}
