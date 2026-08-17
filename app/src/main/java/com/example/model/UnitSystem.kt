package com.example.model

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Supported measurement units across the application.
 * Default is FEET_INCHES as requested.
 */
enum class UnitSystem(val title: String, val shortLabel: String, val unitSymbol: String) {
    FEET_INCHES("Feet & Inches", "FT", "ft"),
    INCHES("Inches", "IN", "in"),
    CENTIMETERS("Centimeters", "CM", "cm")
}

object DimensionFormatter {

    fun format(cm: Float, unitSystem: UnitSystem, compact: Boolean = false): String {
        return formatLength(cm, unitSystem, compact)
    }

    /**
     * Formats a centimeter value into the selected unit system.
     * E.g. 240cm in FEET_INCHES -> 7′ 10″ (or 7 ft 10 in)
     * 240cm in INCHES -> 94″ (or 94.5 in)
     * 240cm in CENTIMETERS -> 240 cm
     */
    fun formatLength(cm: Float, unitSystem: UnitSystem, compact: Boolean = false): String {
        return when (unitSystem) {
            UnitSystem.FEET_INCHES -> {
                val totalInches = cm / 2.54f
                val feet = (totalInches / 12f).toInt()
                val remainingInches = (totalInches % 12f).roundToInt()
                val adjustedFeet = if (remainingInches >= 12) feet + 1 else feet
                val finalInches = if (remainingInches >= 12) 0 else remainingInches
                if (compact) {
                    "${adjustedFeet}′ ${finalInches}″"
                } else {
                    "${adjustedFeet} ft ${finalInches} in"
                }
            }
            UnitSystem.INCHES -> {
                val totalInches = (cm / 2.54f).roundToInt()
                if (compact) "${totalInches}″" else "$totalInches in"
            }
            UnitSystem.CENTIMETERS -> {
                "${cm.roundToInt()} cm"
            }
        }
    }

    /**
     * Formats 3D bounding dimensions (Width x Height x Depth)
     */
    fun formatDimensions(
        wCm: Float,
        hCm: Float,
        dCm: Float,
        unitSystem: UnitSystem,
        compact: Boolean = false
    ): String {
        val wStr = formatLength(wCm, unitSystem, compact)
        val hStr = formatLength(hCm, unitSystem, compact)
        val dStr = formatLength(dCm, unitSystem, compact)
        return "$wStr × $hStr × $dStr"
    }

    /**
     * Formats room measurement in meters (e.g. 3.86m) into the selected unit system.
     */
    fun formatRoomMeters(
        meters: Float,
        unitSystem: UnitSystem,
        compact: Boolean = false
    ): String {
        val cm = meters * 100f
        return formatLength(cm, unitSystem, compact)
    }

    /**
     * Formats surface area in sq.ft or sq.m
     */
    fun formatArea(sqMeters: Float, unitSystem: UnitSystem): String {
        return when (unitSystem) {
            UnitSystem.FEET_INCHES, UnitSystem.INCHES -> {
                val sqFeet = sqMeters * 10.7639f
                String.format(Locale.US, "%.1f sq.ft", sqFeet)
            }
            UnitSystem.CENTIMETERS -> {
                String.format(Locale.US, "%.2f m²", sqMeters)
            }
        }
    }

    /**
     * Converts feet and inches components to centimeters.
     */
    fun feetInchesToCm(feet: Int, inches: Float): Float {
        return (feet * 12f + inches) * 2.54f
    }

    /**
     * Converts inches to centimeters.
     */
    fun inchesToCm(inches: Float): Float {
        return inches * 2.54f
    }

    /**
     * Formats an amount into Indian Rupees (INR / ₹) with standard Indian comma grouping:
     * e.g. ₹ 78,500, ₹ 1,24,000, ₹ 4,200
     */
    fun formatCurrencyINR(amount: Double): String {
        val longVal = amount.roundToInt()
        val formatted = formatIndianNumber(longVal.toLong())
        return "₹$formatted"
    }

    private fun formatIndianNumber(number: Long): String {
        if (number < 0) return "-₹" + formatIndianNumber(-number)
        if (number < 1000) return number.toString()
        val numStr = number.toString()
        val lastThree = numStr.substring(numStr.length - 3)
        val otherDigits = numStr.substring(0, numStr.length - 3)
        val sb = StringBuilder()
        var count = 0
        for (i in otherDigits.length - 1 downTo 0) {
            sb.append(otherDigits[i])
            count++
            if (count % 2 == 0 && i != 0) {
                sb.append(',')
            }
        }
        return sb.reverse().toString() + "," + lastThree
    }
}
