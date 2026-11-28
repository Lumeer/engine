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
package io.lumeer.datafilters.date

/**
 * moment.js locale data and the locale functions used for formatting and parsing (moment 2.31 locale definitions).
 * FR is the global locale the filters bundle parsed with; EN, CS and HU are the locales automations format and parse with.
 */
internal abstract class DateLocale(val abbr: String) {
    abstract val dow: Int
    abstract val doy: Int
    protected abstract val longDateFormats: Map<String, String>

    // ---- formatting

    abstract fun months(month: Int, format: String): String
    abstract fun monthsShort(month: Int, format: String): String
    abstract val weekdays: List<String>
    abstract val weekdaysShort: List<String>
    abstract val weekdaysMin: List<String>
    /** Locale#ordinal(number, token): token is the format token without "o" (D, M, DDD, d, w, W, Q, y) */
    abstract fun ordinal(number: Long, token: String): String

    /** Locale#weekdays(m, format) */
    open fun weekdays(day: Int, format: String): String = weekdays[day]

    open val invalidDate: String = "Invalid date"
    open fun meridiem(hours: Int, isLower: Boolean): String = if (hours > 11) (if (isLower) "pm" else "PM") else (if (isLower) "am" else "AM")

    /** Locale#longDateFormat: lowercase keys are derived from the uppercase ones */
    fun longDateFormat(key: String): String? {
        longDateFormats[key]?.let { return it }
        val upper = longDateFormats[key.uppercase()] ?: return null
        return DateFormatter.FORMATTING_TOKENS.findAll(upper).joinToString("") { match ->
            val token = match.value
            if (token == "MMMM" || token == "MM" || token == "DD" || token == "dddd") token.substring(1) else token
        }
    }

    // ---- parsing (not strict)

    abstract val monthsRegex: Regex
    abstract val monthsShortRegex: Regex
    abstract fun monthsParse(input: String, token: String): Int?
    abstract val weekdaysRegex: Regex
    abstract val weekdaysShortRegex: Regex
    abstract val weekdaysMinRegex: Regex
    abstract fun weekdaysParse(input: String, token: String): Int?
    abstract val dayOfMonthOrdinalParseLenient: Regex
    open val meridiemParse: Regex = Regex("[ap]\\.?m?\\.?", RegexOption.IGNORE_CASE)
    open fun isPM(input: String): Boolean = input.lowercase().startsWith("p")

    /** Default locales: localeMonthsParse and localeWeekdaysParse with per-index regexes (not strict) */
    abstract class Default(abbr: String) : DateLocale(abbr) {
        abstract val monthNames: List<String>
        abstract val monthShortNames: List<String>

        private val monthsParseRegexes: List<Regex> by lazy {
            (0 until 12).map { Regex(("^" + monthNames[it] + "|^" + monthShortNames[it]).replaceFirst(".", ""), RegexOption.IGNORE_CASE) }
        }
        private val weekdaysParseRegexes: List<Regex> by lazy {
            (0 until 7).map { Regex(("^" + weekdays[it] + "|^" + weekdaysShort[it] + "|^" + weekdaysMin[it]).replaceFirst(".", ""), RegexOption.IGNORE_CASE) }
        }

        override fun months(month: Int, format: String): String = monthNames[month]
        override fun monthsShort(month: Int, format: String): String = monthShortNames[month]
        override val monthsRegex: Regex get() = MATCH_WORD
        override val monthsShortRegex: Regex get() = MATCH_WORD
        override val weekdaysRegex: Regex get() = MATCH_WORD
        override val weekdaysShortRegex: Regex get() = MATCH_WORD
        override val weekdaysMinRegex: Regex get() = MATCH_WORD
        override fun monthsParse(input: String, token: String): Int? = monthsParseRegexes.indexOfFirst { it.containsMatchIn(input) }.takeIf { it >= 0 }
        override fun weekdaysParse(input: String, token: String): Int? = weekdaysParseRegexes.indexOfFirst { it.containsMatchIn(input) }.takeIf { it >= 0 }
    }

    object En : Default("en") {
        override val dow = 0
        override val doy = 6
        override val longDateFormats = mapOf("LTS" to "h:mm:ss A", "LT" to "h:mm A", "L" to "MM/DD/YYYY", "LL" to "MMMM D, YYYY", "LLL" to "MMMM D, YYYY h:mm A", "LLLL" to "dddd, MMMM D, YYYY h:mm A")
        override val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        override val monthShortNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        override val weekdays = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        override val weekdaysShort = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        override val weekdaysMin = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
        override val dayOfMonthOrdinalParseLenient = Regex("\\d{1,2}(th|st|nd|rd)|\\d{1,2}")

        override fun ordinal(number: Long, token: String): String {
            val b = number % 10
            val suffix = if ((number % 100) / 10 == 1L) "th" else when (b) {
                1L -> "st"
                2L -> "nd"
                3L -> "rd"
                else -> "th"
            }
            return number.toString() + suffix
        }
    }

    object Cs : DateLocale("cs") {
        override val dow = 1
        override val doy = 4
        override val longDateFormats = mapOf("LT" to "H:mm", "LTS" to "H:mm:ss", "L" to "DD.MM.YYYY", "LL" to "D. MMMM YYYY", "LLL" to "D. MMMM YYYY H:mm", "LLLL" to "dddd D. MMMM YYYY H:mm", "l" to "D. M. YYYY")
        private val standalone = "leden_únor_březen_duben_květen_červen_červenec_srpen_září_říjen_listopad_prosinec".split("_")
        private val format = "ledna_února_března_dubna_května_června_července_srpna_září_října_listopadu_prosince".split("_")
        private val isFormat = Regex("DD?[o.]?(\\[[^\\[\\]]*\\]|\\s)+MMMM")
        private val shortNames = "led_úno_bře_dub_kvě_čvn_čvc_srp_zář_říj_lis_pro".split("_")
        private val monthsParseRegexes = listOf("^led", "^úno", "^bře", "^dub", "^kvě", "^(čvn|červen$|června)", "^(čvc|červenec|července)", "^srp", "^zář", "^říj", "^lis", "^pro")
            .map { Regex(it.replace("$", "\\z"), RegexOption.IGNORE_CASE) }
        private val monthsRegexValue = Regex("^(leden|únor|březen|duben|květen|červenec|července|červen|června|srpen|září|říjen|listopad|prosinec|led|úno|bře|dub|kvě|čvn|čvc|srp|zář|říj|lis|pro)", RegexOption.IGNORE_CASE)

        override fun months(month: Int, format: String): String = (if (isFormat.containsMatchIn(format)) this.format else standalone)[month]
        override fun monthsShort(month: Int, format: String): String = shortNames[month]
        override val weekdays = "neděle_pondělí_úterý_středa_čtvrtek_pátek_sobota".split("_")
        override val weekdaysShort = "ne_po_út_st_čt_pá_so".split("_")
        override val weekdaysMin = "ne_po_út_st_čt_pá_so".split("_")
        override fun ordinal(number: Long, token: String): String = "$number."
        override val monthsRegex get() = monthsRegexValue
        override val monthsShortRegex get() = monthsRegexValue
        override fun monthsParse(input: String, token: String): Int? = monthsParseRegexes.indexOfFirst { it.containsMatchIn(input) }.takeIf { it >= 0 }
        override val weekdaysRegex: Regex get() = MATCH_WORD
        override val weekdaysShortRegex: Regex get() = MATCH_WORD
        override val weekdaysMinRegex: Regex get() = MATCH_WORD
        private val weekdaysParseRegexes: List<Regex> by lazy {
            (0 until 7).map { Regex(("^" + weekdays[it] + "|^" + weekdaysShort[it] + "|^" + weekdaysMin[it]).replaceFirst(".", ""), RegexOption.IGNORE_CASE) }
        }
        override fun weekdaysParse(input: String, token: String): Int? = weekdaysParseRegexes.indexOfFirst { it.containsMatchIn(input) }.takeIf { it >= 0 }
        override val dayOfMonthOrdinalParseLenient = Regex("\\d{1,2}\\.|\\d{1,2}")
    }

    object Hu : Default("hu") {
        override val dow = 1
        override val doy = 4
        override val longDateFormats = mapOf("LT" to "H:mm", "LTS" to "H:mm:ss", "L" to "YYYY.MM.DD.", "LL" to "YYYY. MMMM D.", "LLL" to "YYYY. MMMM D. H:mm", "LLLL" to "YYYY. MMMM D., dddd H:mm")
        override val monthNames = "január_február_március_április_május_június_július_augusztus_szeptember_október_november_december".split("_")
        override val monthShortNames = "jan._feb._márc._ápr._máj._jún._júl._aug._szept._okt._nov._dec.".split("_")
        override val weekdays = "vasárnap_hétfő_kedd_szerda_csütörtök_péntek_szombat".split("_")
        override val weekdaysShort = "vas_hét_kedd_sze_csüt_pén_szo".split("_")
        override val weekdaysMin = "v_h_k_sze_cs_p_szo".split("_")
        override fun ordinal(number: Long, token: String): String = "$number."
        override val dayOfMonthOrdinalParseLenient = Regex("\\d{1,2}\\.|\\d{1,2}")
        override val meridiemParse = Regex("de|du", RegexOption.IGNORE_CASE)
        override fun isPM(input: String): Boolean = input.length > 1 && input[1].lowercaseChar() == 'u'
        override fun meridiem(hours: Int, isLower: Boolean): String = if (hours < 12) (if (isLower) "de" else "DE") else (if (isLower) "du" else "DU")

        // monthsParseExact: computeMonthsParse and handleStrictParse
        private val monthsRegexValue: Regex by lazy {
            val mixed = (0 until 12).flatMap { listOf(regexEscape(monthNames[it]), regexEscape(monthShortNames[it])) }.sortedByDescending { it.length }
            Regex("^(" + mixed.joinToString("|") + ")", RegexOption.IGNORE_CASE)
        }
        override val monthsRegex get() = monthsRegexValue
        override val monthsShortRegex get() = monthsRegexValue

        override fun monthsParse(input: String, token: String): Int? {
            val llc = input.lowercase()
            val short = monthShortNames.map { it.lowercase() }
            val long = monthNames.map { it.lowercase() }
            val order = if (token == "MMM") listOf(short, long) else listOf(long, short)
            for (list in order) {
                val index = list.indexOf(llc)
                if (index != -1) return index
            }
            return null
        }
    }

    /** The global locale of the original filters bundle: only parsing is used */
    object Fr : DateLocale("fr") {
        override val dow = 1
        override val doy = 4
        override val longDateFormats = mapOf("LT" to "HH:mm", "LTS" to "HH:mm:ss", "L" to "DD/MM/YYYY", "LL" to "D MMMM YYYY", "LLL" to "D MMMM YYYY HH:mm", "LLLL" to "dddd D MMMM YYYY HH:mm")
        private val monthsRegexValue = Regex("(janv\\.?|févr\\.?|mars|avr\\.?|mai|juin|juil\\.?|août|sept\\.?|oct\\.?|nov\\.?|déc\\.?|janvier|février|mars|avril|mai|juin|juillet|août|septembre|octobre|novembre|décembre)", RegexOption.IGNORE_CASE)
        private val monthsParseRegexes = listOf("^janv", "^févr", "^mars", "^avr", "^mai", "^juin", "^juil", "^août", "^sept", "^oct", "^nov", "^déc").map { Regex(it, RegexOption.IGNORE_CASE) }
        override val weekdays = listOf("dimanche", "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi")
        override val weekdaysShort = listOf("dim.", "lun.", "mar.", "mer.", "jeu.", "ven.", "sam.")
        override val weekdaysMin = listOf("di", "lu", "ma", "me", "je", "ve", "sa")
        private val weekdaysRegexValue: Regex by lazy {
            val pieces = (0 until 7).flatMap { listOf(weekdaysMin[it], weekdaysShort[it], weekdays[it]) }.map { regexEscape(it) }
            Regex("^(" + pieces.sortedByDescending { it.length }.joinToString("|") + ")", RegexOption.IGNORE_CASE)
        }
        override fun months(month: Int, format: String): String = error("fr formatting is not used")
        override fun monthsShort(month: Int, format: String): String = error("fr formatting is not used")
        override fun ordinal(number: Long, token: String): String = error("fr formatting is not used")
        override val monthsRegex get() = monthsRegexValue
        override val monthsShortRegex get() = monthsRegexValue
        override fun monthsParse(input: String, token: String): Int? = monthsParseRegexes.indexOfFirst { it.containsMatchIn(input) }.takeIf { it >= 0 }
        override val weekdaysRegex get() = weekdaysRegexValue
        override val weekdaysShortRegex get() = weekdaysRegexValue
        override val weekdaysMinRegex get() = weekdaysRegexValue

        /** weekdaysParseExact handleStrictParse, not strict */
        override fun weekdaysParse(input: String, token: String): Int? {
            val llc = input.lowercase()
            val order = when (token) {
                "dddd" -> listOf(weekdays, weekdaysShort, weekdaysMin)
                "ddd" -> listOf(weekdaysShort, weekdays, weekdaysMin)
                else -> listOf(weekdaysMin, weekdays, weekdaysShort)
            }
            for (list in order) {
                val index = list.indexOf(llc)
                if (index != -1) return index
            }
            return null
        }
        override val dayOfMonthOrdinalParseLenient = Regex("\\d{1,2}(er|)|\\d{1,2}")
    }

    /**
     * A locale generated from the moment 2.31 locale definitions (DateLocaleData.kt): names (with format/standalone
     * variants chosen by a regex on the whole format, as moment does), parsing regexes and the small locale functions.
     */
    class Data(
        abbr: String,
        override val dow: Int,
        override val doy: Int,
        override val longDateFormats: Map<String, String>,
        /** standalone names, and the names used when the regex matches the format */
        private val monthNames: List<String>,
        private val monthFormats: Pair<Regex, List<String>>? = null,
        private val monthShortNames: List<String>,
        private val monthShortFormats: Pair<Regex, List<String>>? = null,
        override val weekdays: List<String>,
        /** weekday variants tried in order */
        private val weekdayFormats: List<Pair<Regex, List<String>>> = emptyList(),
        override val weekdaysShort: List<String>,
        override val weekdaysMin: List<String>,
        private val ordinalFunction: (Long, String) -> String,
        override val invalidDate: String,
        private val monthsRegexValue: Regex?,
        private val monthsShortRegexValue: Regex?,
        private val monthsParseRegexes: List<Regex>,
        /** weekdaysParseExact: the mixed regex and the lowercase names; otherwise the per-day regexes */
        private val weekdaysExactRegex: Regex?,
        private val weekdaysParseRegexes: List<Regex>,
        override val dayOfMonthOrdinalParseLenient: Regex,
        override val meridiemParse: Regex,
        private val meridiemFunction: ((Int, Boolean) -> String)? = null,
        private val isPMFunction: ((String) -> Boolean)? = null,
    ) : DateLocale(abbr) {
        override fun months(month: Int, format: String): String =
            monthFormats?.takeIf { it.first.containsMatchIn(format) }?.second?.get(month) ?: monthNames[month]

        override fun monthsShort(month: Int, format: String): String =
            monthShortFormats?.takeIf { it.first.containsMatchIn(format) }?.second?.get(month) ?: monthShortNames[month]

        override fun weekdays(day: Int, format: String): String =
            weekdayFormats.firstOrNull { it.first.containsMatchIn(format) }?.second?.get(day) ?: weekdays[day]

        override fun ordinal(number: Long, token: String): String = ordinalFunction(number, token)
        override fun meridiem(hours: Int, isLower: Boolean): String = meridiemFunction?.invoke(hours, isLower) ?: super.meridiem(hours, isLower)
        override fun isPM(input: String): Boolean = isPMFunction?.invoke(input) ?: super.isPM(input)

        override val monthsRegex: Regex get() = monthsRegexValue ?: MATCH_WORD
        override val monthsShortRegex: Regex get() = monthsShortRegexValue ?: MATCH_WORD
        override fun monthsParse(input: String, token: String): Int? = monthsParseRegexes.indexOfFirst { it.containsMatchIn(input) }.takeIf { it >= 0 }
        override val weekdaysRegex: Regex get() = weekdaysExactRegex ?: MATCH_WORD
        override val weekdaysShortRegex: Regex get() = weekdaysExactRegex ?: MATCH_WORD
        override val weekdaysMinRegex: Regex get() = weekdaysExactRegex ?: MATCH_WORD

        private val lowerWeekdays by lazy { listOf(weekdays, weekdaysShort, weekdaysMin).map { list -> list.map { it.lowercase() } } }

        override fun weekdaysParse(input: String, token: String): Int? {
            if (weekdaysExactRegex == null) return weekdaysParseRegexes.indexOfFirst { it.containsMatchIn(input) }.takeIf { it >= 0 }
            // handleStrictParse (not strict)
            val llc = input.lowercase()
            val (long, short, min) = lowerWeekdays
            val order = when (token) {
                "dddd" -> listOf(long, short, min)
                "ddd" -> listOf(short, long, min)
                else -> listOf(min, long, short)
            }
            for (list in order) {
                val index = list.indexOf(llc)
                if (index != -1) return index
            }
            return null
        }
    }

    companion object {
        /** "%d." style ordinals */
        fun pattern(ordinal: String): (Long, String) -> String = { number, _ -> ordinal.replace("%d", number.toString()) }

        /** en, en-gb: 1st 2nd 3rd 4th, 11th 12th 13th */
        val englishOrdinal: (Long, String) -> String = { number, _ ->
            val b = number % 10
            number.toString() + if ((number % 100) / 10 == 1L) "th" else when (b) {
                1L -> "st"
                2L -> "nd"
                3L -> "rd"
                else -> "th"
            }
        }

        /** fr: 1er (day), 1er/2e, 1re/2e (weeks) */
        val frenchOrdinal: (Long, String) -> String = { number, token ->
            number.toString() + when (token) {
                "D" -> if (number == 1L) "er" else ""
                "w", "W" -> if (number == 1L) "re" else "e"
                else -> if (number == 1L) "er" else "e"
            }
        }

        /** nl: 1ste 2de 8ste 20ste */
        val dutchOrdinal: (Long, String) -> String = { number, _ -> number.toString() + if (number == 1L || number == 8L || number >= 20) "ste" else "de" }

        /** ru, uk: a suffix by token, the number alone for the other tokens */
        fun suffixOrdinal(suffixes: Map<String, String>): (Long, String) -> String = { number, token -> number.toString() + suffixes[token].orEmpty() }

        /** ru, uk: night, morning, day, evening */
        fun dayPartMeridiem(parts: List<String>): (Int, Boolean) -> String = { hours, _ ->
            when {
                hours < 4 -> parts[0]
                hours < 12 -> parts[1]
                hours < 17 -> parts[2]
                else -> parts[3]
            }
        }

        private fun range(from: Int, to: Int): String = "\\x{" + from.toString(16) + "}-\\x{" + to.toString(16) + "}"

        /** matchWord of moment */
        val MATCH_WORD = Regex(
            "[0-9]{0,256}['a-z" + range(0xA0, 0x5FF) + range(0x700, 0xD7FF) + range(0xF900, 0xFDCF) + range(0xFDF0, 0xFF07) + range(0xFF10, 0xFFEF) + "]{1,256}|" +
                "[" + range(0x600, 0x6FF) + "/]{1,256}(\\s*?[" + range(0x600, 0x6FF) + "]{1,256}){1,2}",
            RegexOption.IGNORE_CASE,
        )

        private val REGEX_SPECIAL = Regex("[-/\\\\^$*+?.()|\\[\\]{}]")
        fun regexEscape(s: String): String = s.replace(REGEX_SPECIAL) { "\\" + it.value }

        /** all locales of moment-with-locales 2.31 */
        private val MOMENT_LOCALES: Set<String> = setOf("en", "af", "am-et", "ar-dz", "ar-kw", "ar-ly", "ar-ma", "ar-ps", "ar-sa", "ar-tn", "ar", "az", "be", "bg", "bm", "bn-bd", "bn", "bo", "br", "bs", "ca", "cs", "cv", "cy", "da", "de-at", "de-ch", "de", "dv", "el", "en-au", "en-ca", "en-gb", "en-ie", "en-il", "en-in", "en-nz", "en-sg", "eo", "es-do", "es-mx", "es-us", "es", "et", "eu", "fa", "fi", "fil", "fo", "fr-ca", "fr-ch", "fr", "fy", "ga", "gd", "gl", "gom-deva", "gom-latn", "gu", "he", "hi", "hr", "hu", "hy-am", "id", "is", "it-ch", "it", "ja", "jv", "ka", "kk", "km", "kn", "ko", "ku-kmr", "ku", "ky", "lb", "lo", "lt", "lv", "me", "mi", "mk", "ml", "mn", "mr", "ms-my", "ms", "mt", "my", "nb", "ne", "nl-be", "nl", "nn", "oc-lnc", "pa-in", "pl", "ps", "pt-br", "pt", "ro", "ru", "sd", "se", "si", "sk", "sl", "sq", "sr-cyrl", "sr", "ss", "sv", "sw", "ta", "te", "tet", "tg", "th", "tk", "tl-ph", "tlh", "tr", "tzl", "tzm-latn", "tzm", "ug-cn", "uk", "ur", "uz-latn", "uz", "vi", "x-pseudo", "yo", "zh-cn", "zh-hk", "zh-mo", "zh-tw")

        private val PORTED: Map<String, DateLocale> by lazy { mapOf("en" to En, "cs" to Cs, "hu" to Hu) + DateLocaleData.LOCALES }

        /** Language codes users write that moment does not know, mapped to the moment ones ("cz" is the country code of "cs"). */
        private val LANGUAGE_ALIASES: Map<String, String> = mapOf("cz" to "cs")

        /**
         * The locale name moment getLocale resolves a string key to (normalized, tried from the longest prefix),
         * "en" (the global locale) when none matches. Unlike moment, a language in [LANGUAGE_ALIASES] resolves as
         * its alias ("cz" and "cz-CZ" as "cs").
         */
        fun resolveName(key: String): String {
            if (key.isEmpty()) return "en"
            if (key in MOMENT_LOCALES) return key
            val normalized = withLanguageAlias(key.lowercase().replaceFirst("_", "-"))
            if (normalized in MOMENT_LOCALES) return normalized
            val split = normalized.split("-")
            for (j in split.size downTo 1) {
                val name = split.subList(0, j).joinToString("-")
                if (name in MOMENT_LOCALES) return name
            }
            return "en"
        }

        /** [key] (normalized, "cz-cz") with its language replaced by the alias ("cs-cz") */
        private fun withLanguageAlias(key: String): String {
            val language = key.substringBefore("-")
            val alias = LANGUAGE_ALIASES[language] ?: return key
            return alias + key.substring(language.length)
        }

        /**
         * The locale for a moment locale key: the supported locale moment would resolve it to; for other locales the
         * supported base language (es-mx -> es) or en.
         */
        fun supported(key: String): DateLocale {
            val name = resolveName(key)
            return PORTED[name] ?: PORTED[name.substringBefore("-")] ?: En
        }

        /** The names of the supported locales */
        val SUPPORTED: Set<String> get() = PORTED.keys
    }
}
