# data-filters (Kotlin Multiplatform)

Kotlin port of [@lumeer/data-filters](https://github.com/Lumeer/data-filters) used by the engine to filter documents and
link instances by a query (`io.lumeer.core.util.js.DataFilter`). It replaced the GraalVM + `lumeer-data-filters.min.js`
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
