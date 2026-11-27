# data-filters (Kotlin Multiplatform)

Kotlin port of [@lumeer/data-filters](https://github.com/Lumeer/data-filters) used by the engine to filter documents and
link instances by a query (`io.lumeer.core.util.js.DataFilter`), and of the libraries the automation (Blockly) blocks
used (`automation/`, see below). It replaced the GraalVM + `lumeer-data-filters.min.js`
bundle. moment, moment-timezone, numbro, big.js and coordinate-parser are not used; the parts the filters need are
reimplemented (`date/`, `number/`).

The same code builds a JS module for web-ui later; the JS time zone support (`TimeZones.js.kt`) is not finished yet.

## Build

The engine builds it through Maven (`pom.xml` here runs the Gradle wrapper and attaches the jar), so `mvn install` in the
engine root is enough. Directly:

```sh
./gradlew jvmTest                               # unit + parity tests
./gradlew jvmJar                                # build/libs/data-filters-jvm-1.0-SNAPSHOT.jar
./gradlew jsNodeProductionLibraryDistribution   # ES module + .d.ts in build/dist/js/productionLibrary
```

Gradle 9.8 with Kotlin 2.4.21 needs Java 17+; the jar targets Java 21. `lumeer-core` must use the same or newer Kotlin.

## What is covered

Everything the backend filter touches: `filterDocumentsAndLinksByQuery` (stems, link chains, filters, fulltexts,
document ids, children, non-linked documents, pagination), `meetCondition` and `meetFullTexts` of all constraint types,
and the Action button enabled/disabled conditions evaluated from attribute locks.

Not ported (not used by the filter): aggregation, serialization, validation, increment/decrement, valueByCondition and
input parsing.

## Behaviour kept from the JavaScript bundle on purpose

The port reproduces the old bundle exactly, including its quirks:

- **Dates are parsed with moment's `fr` locale.** The bundle imports several moment locales and the last one (`fr`)
  stays global. French month and weekday names, French `L` formats and ISO weeks (Monday first) are used for parsing,
  `week()` and "this week" conditions; dates are formatted (fulltext) with `en`.
- **Two moment copies.** Values parsed by format go through the copy in @lumeer/utils, which ignores the timezone from
  the constraint data and uses the server's time zone. ISO values and "now" use the configured timezone.
- **numbro keeps a global language.** Every Number value switches it (to its currency or en-US), and later formatting
  (also of Duration values) uses whatever was set last. `NumbroState` reproduces this per filter run.
- **Link instances can repeat.** Within one stem, a link instance reached from several documents is returned once per
  document; only items already returned by earlier stems are dropped. The engine maps the ids back to objects the same way.
- JavaScript coercion rules (`String(x)`, `Number(x)`, truthiness, `trim`, `toFixed`) are reproduced in `util/JsSemantics.kt`.

One intended difference: the engine now passes attribute locks to the filter, so the Action button
enabled/disabled conditions work. The old Gson serialization dropped locks, so "enabled" never matched before.

## Parity tests

`src/jvmTest/resources/parity-vectors.json.gz` holds 12,000 random single-collection cases (all constraint types, timezones,
languages, currencies, relative dates), 5 fixed Address cases and 1,500 cases with link chains (link types, link
instances, link filters, document ids, children, non-linked documents, pagination, several stems), with the results of
the old bundle (documents, and link instances for the link cases). `ParityVectorsTest` replays them here and
`DataFilterParityTest` in lumeer-core replays them through the engine models.

The expected results were produced by the old bundle as of commit `797255de`
(`lumeer-core/src/main/resources/lumeer-data-filters.min.js`), run in Node with `TZ=Europe/Prague`.

## Automation functions

`automation/` replaces the JavaScript libraries the automation blocks used (moment, moment-business, numbro, he); the
engine does not ship them any more. The behaviour follows the newest versions: moment 2.31, numbro 2.5 (all its
languages) and he 2.0. The engine exposes the functions to scripts through the lumeer bridge (`lumeer.changeDate`,
`shiftDate`, `formatDate`, `parseDate`, `formatCurrency`, `escapeHtml`, `unescapeHtml`, see
`io.lumeer.core.js.AutomationFunctions`), and the Java code formats and parses dates with them (`DateFormats`).

- `AutomationDates`: blocks "date change" (with the business days of the plugin the engine bundled), "shift date",
  "format date" and "parse date". Inputs are JS values (`JsUndefined`, `JsDate`, `JsObject`, numbers, strings...) as the
  original code received them; results are the time values of the JS Dates the code returned, in the server zone.
- `AutomationNumbers.formatCurrency`: numbro.setLanguage + formatCurrency; the language data is in `NumbroLanguages.kt`.
- `AutomationHtml`: he.escape and he.unescape (`HtmlEntityData.kt` is generated from he's data.mjs).
- Dates are formatted and parsed by `date/DateFormatter` and `date/DateParser` with a `DateLocale`. The filters use them in
  the mode of the moment version their bundle had; automations with `latest = true` (moment 2.31).

**Locales.** Format and parse date support en, cs, hu, sk, pl, de, de-at, de-ch, es, fr, it, pt, pt-br, nl, ru, uk and
en-gb. The data of the locales other than en, cs and hu is generated from moment's locale definitions
(`DateLocaleData.kt`). Other locale keys use their base language when it is supported (es-mx: es), otherwise en. This is
the one intended change: before, moment formatted all its 140 locales; a block with for example "ja" now gives English.

**Code generated before.** Rules saved before call moment, numbro and he directly. The engine adds a small script
(`io.lumeer.core.js.LegacyScriptShim`) that provides `moment(date)` (it records the calls, and the bridge evaluates them
with `AutomationDates.dateChain`), `formatMomentJsDate`, `parseMomentJsDate`, `lumeer_numbro` and `he`, so these rules
keep working; web-ui generates the `lumeer.*` calls.

"Subtract" with sticky months/quarters/years subtracts; the block generated an add for it before, and rules saved then
keep adding (the shim runs their code as it was). Quirks of the original block code that are kept: a fractional count
of business days fails (the plugin looped forever), `Math.pow` of the script engine (integer powers by squaring) is used
for currency rounding.

The parity vectors (`lumeer-core/src/test/resources/automation-parity-vectors.json.gz`, replayed by
`AutomationParityTest`) hold the results of the original libraries run in GraalVM in several time zones.
