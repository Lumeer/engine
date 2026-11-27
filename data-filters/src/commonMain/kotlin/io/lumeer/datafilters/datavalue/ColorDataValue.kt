/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.lumeer.datafilters.datavalue

import io.lumeer.datafilters.model.ConditionType
import io.lumeer.datafilters.model.ConditionValue
import io.lumeer.datafilters.number.Numbro
import io.lumeer.datafilters.util.JS_S
import io.lumeer.datafilters.util.formatUnknownDataValue
import io.lumeer.datafilters.util.hasJsDisplayValue
import io.lumeer.datafilters.util.jsStringOrEmpty
import io.lumeer.datafilters.util.jsToNumber
import io.lumeer.datafilters.util.jsTrim
import io.lumeer.datafilters.util.valueMeetFulltexts

internal val VALID_DATA_COLORS: Map<String, String> = linkedMapOf(
    "black" to "#000000",
    "silver" to "#c0c0c0",
    "gray" to "#808080",
    "white" to "#ffffff",
    "maroon" to "#800000",
    "red" to "#ff0000",
    "purple" to "#800080",
    "fuchsia" to "#ff00ff",
    "green" to "#008000",
    "lime" to "#00ff00",
    "olive" to "#808000",
    "yellow" to "#ffff00",
    "navy" to "#000080",
    "blue" to "#0000ff",
    "teal" to "#008080",
    "orange" to "#ffa500",
    "aliceblue" to "#f0f8ff",
    "antiquewhite" to "#faebd7",
    "aquamarine" to "#7fffd4",
    "azure" to "#f0ffff",
    "beige" to "#f5f5dc",
    "bisque" to "#ffe4c4",
    "blanchedalmond" to "#ffebcd",
    "blueviolet" to "#8a2be2",
    "brown" to "#a52a2a",
    "burlywood" to "#deb887",
    "cadetblue" to "#5f9ea0",
    "chartreuse" to "#7fff00",
    "chocolate" to "#d2691e",
    "coral" to "#ff7f50",
    "cornflowerblue" to "#6495ed",
    "cornsilk" to "#fff8dc",
    "crimson" to "#dc143c",
    "cyan" to "#00ffff",
    "aqua" to "#00ffff",
    "darkblue" to "#00008b",
    "darkcyan" to "#008b8b",
    "darkgoldenrod" to "#b8860b",
    "darkgray" to "#a9a9a9",
    "darkgreen" to "#006400",
    "darkgrey" to "#a9a9a9",
    "darkkhaki" to "#bdb76b",
    "darkmagenta" to "#8b008b",
    "darkolivegreen" to "#556b2f",
    "darkorange" to "#ff8c00",
    "darkorchid" to "#9932cc",
    "darkred" to "#8b0000",
    "darksalmon" to "#e9967a",
    "darkseagreen" to "#8fbc8f",
    "darkslateblue" to "#483d8b",
    "darkslategray" to "#2f4f4f",
    "darkslategrey" to "#2f4f4f",
    "darkturquoise" to "#00ced1",
    "darkviolet" to "#9400d3",
    "deeppink" to "#ff1493",
    "deepskyblue" to "#00bfff",
    "dimgray" to "#696969",
    "dimgrey" to "#696969",
    "dodgerblue" to "#1e90ff",
    "firebrick" to "#b22222",
    "floralwhite" to "#fffaf0",
    "forestgreen" to "#228b22",
    "gainsboro" to "#dcdcdc",
    "ghostwhite" to "#f8f8ff",
    "gold" to "#ffd700",
    "goldenrod" to "#daa520",
    "greenyellow" to "#adff2f",
    "grey" to "#808080",
    "honeydew" to "#f0fff0",
    "hotpink" to "#ff69b4",
    "indianred" to "#cd5c5c",
    "indigo" to "#4b0082",
    "ivory" to "#fffff0",
    "khaki" to "#f0e68c",
    "lavender" to "#e6e6fa",
    "lavenderblush" to "#fff0f5",
    "lawngreen" to "#7cfc00",
    "lemonchiffon" to "#fffacd",
    "lightblue" to "#add8e6",
    "lightcoral" to "#f08080",
    "lightcyan" to "#e0ffff",
    "lightgoldenrodyellow" to "#fafad2",
    "lightgray" to "#d3d3d3",
    "lightgreen" to "#90ee90",
    "lightgrey" to "#d3d3d3",
    "lightpink" to "#ffb6c1",
    "lightsalmon" to "#ffa07a",
    "lightseagreen" to "#20b2aa",
    "lightskyblue" to "#87cefa",
    "lightslategray" to "#778899",
    "lightslategrey" to "#778899",
    "lightsteelblue" to "#b0c4de",
    "lightyellow" to "#ffffe0",
    "limegreen" to "#32cd32",
    "linen" to "#faf0e6",
    "magenta" to "#ff00ff",
    "mediumaquamarine" to "#66cdaa",
    "mediumblue" to "#0000cd",
    "mediumorchid" to "#ba55d3",
    "mediumpurple" to "#9370db",
    "mediumseagreen" to "#3cb371",
    "mediumslateblue" to "#7b68ee",
    "mediumspringgreen" to "#00fa9a",
    "mediumturquoise" to "#48d1cc",
    "mediumvioletred" to "#c71585",
    "midnightblue" to "#191970",
    "mintcream" to "#f5fffa",
    "mistyrose" to "#ffe4e1",
    "moccasin" to "#ffe4b5",
    "navajowhite" to "#ffdead",
    "oldlace" to "#fdf5e6",
    "olivedrab" to "#6b8e23",
    "orangered" to "#ff4500",
    "orchid" to "#da70d6",
    "palegoldenrod" to "#eee8aa",
    "palegreen" to "#98fb98",
    "paleturquoise" to "#afeeee",
    "palevioletred" to "#db7093",
    "papayawhip" to "#ffefd5",
    "peachpuff" to "#ffdab9",
    "peru" to "#cd853f",
    "pink" to "#ffc0cb",
    "plum" to "#dda0dd",
    "powderblue" to "#b0e0e6",
    "rosybrown" to "#bc8f8f",
    "royalblue" to "#4169e1",
    "saddlebrown" to "#8b4513",
    "salmon" to "#fa8072",
    "sandybrown" to "#f4a460",
    "seagreen" to "#2e8b57",
    "seashell" to "#fff5ee",
    "sienna" to "#a0522d",
    "skyblue" to "#87ceeb",
    "slateblue" to "#6a5acd",
    "slategray" to "#708090",
    "slategrey" to "#708090",
    "snow" to "#fffafa",
    "springgreen" to "#00ff7f",
    "steelblue" to "#4682b4",
    "tan" to "#d2b48c",
    "thistle" to "#d8bfd8",
    "tomato" to "#ff6347",
    "turquoise" to "#40e0d0",
    "violet" to "#ee82ee",
    "wheat" to "#f5deb3",
    "whitesmoke" to "#f5f5f5",
    "yellowgreen" to "#9acd32",
    "rebeccapurple" to "#663399",
)

/** hex code to the first color name with it */
private val COLOR_NAMES: Map<String, String> = LinkedHashMap<String, String>().also { names ->
    VALID_DATA_COLORS.forEach { (name, hex) -> if (!names.containsKey(hex)) names[hex] = name }
}

private const val S = JS_S
private val HEX6 = Regex("^#?[0-9a-f]{6}$")
private val HEX3 = Regex("^#?[0-9a-f]{3}$")
private val RGB = Regex("^rgb\\(${S}*[0-9]{1,3}${S}*,${S}*[0-9]{1,3}${S}*,${S}*[0-9]{1,3}${S}*\\)$")
private val HSL = Regex("^hsl\\(${S}*[0-9]{1,3}${S}*,${S}*[0-9]{1,3}${S}%*,${S}*[0-9]{1,3}%${S}*\\)$")

class ColorDataValue(val value: Any?) : DataValue {

    val hexCode: String? = if (hasJsDisplayValue(value)) parseColorHexCode(value) else null

    override fun format(): String = hexCode?.takeIf { it.isNotEmpty() } ?: formatUnknownDataValue(value)

    override fun meetCondition(condition: ConditionType?, values: List<ConditionValue>): Boolean =
        meetTextCondition(condition, values, ::ColorDataValue) { jsTrim(it.format()).lowercase() }

    override fun meetFullTexts(fulltexts: List<String>): Boolean {
        val formattedFulltexts = fulltexts.map { ColorDataValue(it).format() }
        return valueMeetFulltexts(format(), formattedFulltexts) || valueMeetFulltexts(readableColor(), fulltexts)
    }

    private fun readableColor(): String = COLOR_NAMES[hexCode] ?: format()

    private fun parseColorHexCode(rawValue: Any?): String {
        val value = jsTrim(jsStringOrEmpty(rawValue)).lowercase()
        VALID_DATA_COLORS[value]?.let { return it }
        return when {
            HEX6.matches(value) -> if (value.startsWith("#")) value else "#$value"
            HEX3.matches(value) -> "#" + value.removePrefix("#").map { "$it$it" }.joinToString("")
            RGB.matches(value) -> convertRgbColorToHex(value)
            HSL.matches(value) -> convertHslColorToHex(value)
            else -> ""
        }
    }

    private fun convertRgbColorToHex(rgb: String): String {
        val result = "#" + rgb.substring(4, rgb.length - 1).split(",").joinToString("") {
            jsToNumber(jsTrim(it)).toLong().toString(16).padStart(2, '0')
        }
        return if (HEX6.matches(result)) result else ""
    }

    private fun convertHslColorToHex(hsl: String): String {
        val parts = jsTrim(hsl).let { it.substring(4, it.length - 1) }.split(",")
            .map { jsTrim(it) }
            .map { if (it.endsWith("%")) it.dropLast(1) else it }
            .map { jsToNumber(it) }
        val hue = parts[0] / 360
        val saturation = parts[1] / 100
        val lightness = parts[2] / 100
        val red: Double
        val green: Double
        val blue: Double
        if (saturation == 0.0) {
            red = lightness
            green = lightness
            blue = lightness
        } else {
            val q = if (lightness < 0.5) lightness * (1 + saturation) else lightness + saturation - lightness * saturation
            val p = 2 * lightness - q
            red = hueToRgb(p, q, hue + 1.0 / 3)
            green = hueToRgb(p, q, hue)
            blue = hueToRgb(p, q, hue - 1.0 / 3)
        }
        return "#" + colorToHex(red) + colorToHex(green) + colorToHex(blue)
    }

    private fun hueToRgb(p: Double, q: Double, value: Double): Double {
        var t = value
        if (t < 0) t += 1
        if (t > 1) t -= 1
        if (t < 1.0 / 6) return p + (q - p) * 6 * t
        if (t < 1.0 / 2) return q
        if (t < 2.0 / 3) return p + (q - p) * (2.0 / 3 - t) * 6
        return p
    }

    private fun colorToHex(x: Double): String = Numbro.jsRound(x * 255).toLong().toString(16).padStart(2, '0')
}
