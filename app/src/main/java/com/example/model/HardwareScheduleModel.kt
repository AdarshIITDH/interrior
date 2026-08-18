package com.example.model

/**
 * Single hardware component in the hardware schedule.
 */
data class HardwareItem(
    val itemName: String,
    val specification: String,
    val quantity: Int,
    val unit: String,
    val unitRateInr: Double,
    val category: String = "Fittings"
) {
    val totalAmountInr: Double get() = quantity * unitRateInr
}

/**
 * Generates the full hardware schedule from the wardrobe configuration.
 */
object HardwareScheduleGenerator {

    fun generateHardwareSchedule(config: WardrobeConfig): List<HardwareItem> {
        val list = mutableListOf<HardwareItem>()
        val sections = config.sectionsCount

        // 1. Hinges or Sliding Channels
        if (config.doorStyle == DoorStyle.SLIDING_BYPASS || config.doorStyle == DoorStyle.SLIDING_DOOR || config.doorStyle == DoorStyle.MIRROR_SLIDING_DOOR) {
            list.add(
                HardwareItem(
                    itemName = "Heavy-Duty Top & Bottom Sliding Track Kit",
                    specification = "Anodized Aluminum Double Track (80kg capacity, anti-jump rollers, soft-close dampers)",
                    quantity = 1,
                    unit = "set",
                    unitRateInr = 3850.0,
                    category = "Door Mechanisms"
                )
            )
        } else if (config.doorStyle != DoorStyle.OPEN_CONCEPT) {
            val totalDoors = sections * 2
            val hingesPerDoor = when {
                config.heightCm >= 240f -> 5
                config.heightCm >= 210f -> 4
                else -> 3
            }
            val totalHinges = totalDoors * hingesPerDoor
            list.add(
                HardwareItem(
                    itemName = "3D Soft-Close Auto-Concealed Hinges (0 Crank)",
                    specification = "SUS304 Stainless Steel with integrated hydraulic damper (80,000 cycles certified)",
                    quantity = totalHinges,
                    unit = "pcs",
                    unitRateInr = 240.0,
                    category = "Hinges"
                )
            )
        }

        // 2. Drawer Telescopic Channels
        if (config.drawersCount > 0) {
            list.add(
                HardwareItem(
                    itemName = "Synchronized Soft-Close Telescopic Drawer Channels",
                    specification = "Heavy-Duty 45mm 3-Fold Ball Bearing Slides with Silent Hydraulic Buffer (45kg load)",
                    quantity = config.drawersCount,
                    unit = "pairs",
                    unitRateInr = 950.0,
                    category = "Drawer Slides"
                )
            )
        }

        // 3. Handles & Pulls
        if (config.doorStyle != DoorStyle.OPEN_CONCEPT) {
            val totalHandles = (if (config.doorStyle == DoorStyle.SLIDING_DOOR || config.doorStyle == DoorStyle.SLIDING_BYPASS) sections else sections * 2) + config.drawersCount
            list.add(
                HardwareItem(
                    itemName = "Architectural Designer Hardware Pulls",
                    specification = "${config.handleType.title} (${config.handleType.description})",
                    quantity = totalHandles,
                    unit = "pcs",
                    unitRateInr = config.handleType.unitPriceInr,
                    category = "Handles"
                )
            )
        }

        // 4. Hanging Rails & Flange Brackets
        if (config.hangingRailsCount > 0) {
            val railLengthRft = (config.hangingRailsCount * (config.widthCm / 100f / sections) * 3.28084f).toInt().coerceAtLeast(config.hangingRailsCount * 2)
            list.add(
                HardwareItem(
                    itemName = "Oval Anodized Aluminum Wardrobe Hanging Rods",
                    specification = "30x15mm Heavy Gauge Chrome Profile with Anti-Noise Rubber Strip",
                    quantity = railLengthRft,
                    unit = "rft",
                    unitRateInr = 160.0,
                    category = "Wardrobe Fittings"
                )
            )
            list.add(
                HardwareItem(
                    itemName = "Zinc Alloy Side & Center Hanging Flange Brackets",
                    specification = "Die-cast Heavy Chrome End Sockets with safety set-screws",
                    quantity = config.hangingRailsCount * 2,
                    unit = "pcs",
                    unitRateInr = 65.0,
                    category = "Wardrobe Fittings"
                )
            )
        }

        // 5. Smart LED Lighting Kit
        if (config.ledLighting != LedLighting.NONE) {
            list.add(
                HardwareItem(
                    itemName = "45° Aluminum Corner Diffuser Profile + COB LED Strip",
                    specification = "${config.ledLighting.title} (${config.ledLighting.tempKelvin}) CRI>90+ Dotless Strip",
                    quantity = (config.widthCm / 100f * 3.28f).toInt().coerceAtLeast(6),
                    unit = "rft",
                    unitRateInr = 280.0,
                    category = "Electrical & Lighting"
                )
            )
            list.add(
                HardwareItem(
                    itemName = "Wardrobe PIR Door Sensor + 60W Slim Power Driver",
                    specification = "Auto ON/OFF Infrared Proximity Switch (12V DC, Overload Protected)",
                    quantity = 1,
                    unit = "set",
                    unitRateInr = 1450.0,
                    category = "Electrical & Lighting"
                )
            )
        }

        // 6. Accessories
        if (config.hasMirrorPanel) {
            list.add(
                HardwareItem(
                    itemName = "Full-Height Beveled Dressing Mirror Panel",
                    specification = "5mm Saint-Gobain Distortion-Free Silver Mirror with safety film backing",
                    quantity = 1,
                    unit = "panel",
                    unitRateInr = 2200.0,
                    category = "Accessories"
                )
            )
        }
        if (config.hasJewelryTray) {
            list.add(
                HardwareItem(
                    itemName = "Modular Velvet Jewelry & Watch Organizer Insert",
                    specification = "Multi-compartment microfiber velvet tray with ring rolls and watch cushions",
                    quantity = 1,
                    unit = "tray",
                    unitRateInr = 1850.0,
                    category = "Accessories"
                )
            )
        }
        if (config.hasTrouserRack) {
            list.add(
                HardwareItem(
                    itemName = "Telescopic Pull-Out Trouser & Tie Organizer Rack",
                    specification = "Anti-slip silicone coated 9-prong soft-close pull-out rail",
                    quantity = 1,
                    unit = "set",
                    unitRateInr = 1450.0,
                    category = "Accessories"
                )
            )
        }
        if (config.hasShoeRack) {
            list.add(
                HardwareItem(
                    itemName = "Slanted Shoe Storage Tier with Heel Catch Rail",
                    specification = "Anodized aluminum bottom tier with moisture-barrier laminate",
                    quantity = 1,
                    unit = "tier",
                    unitRateInr = 1200.0,
                    category = "Accessories"
                )
            )
        }

        // 7. Structural Fasteners & Corner KD Fittings
        list.add(
            HardwareItem(
                itemName = "Cabinet MiniFix Cam-Lock & Dowel Knock-Down Assembly Kit",
                specification = "Zinc Alloy Cam with steel connector bolt and nylon expansion plugs",
                quantity = (sections * 16).coerceAtLeast(32),
                unit = "sets",
                unitRateInr = 28.0,
                category = "Assembly Hardware"
            )
        )
        list.add(
            HardwareItem(
                itemName = "Heavy-Duty Leveling Plinth Legs",
                specification = "100mm Adjustable ABS base legs with plinth clip brackets",
                quantity = (sections + 1) * 2,
                unit = "pcs",
                unitRateInr = 75.0,
                category = "Assembly Hardware"
            )
        )

        return list
    }
}
