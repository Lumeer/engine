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
package io.lumeer.core.util.js

import io.lumeer.api.model.Attribute
import io.lumeer.api.model.AttributeFilter
import io.lumeer.api.model.AttributeFilterEquation
import io.lumeer.api.model.AttributeLock
import io.lumeer.api.model.AttributeLockExceptionGroup
import io.lumeer.api.model.Collection
import io.lumeer.api.model.CollectionAttributeFilter
import io.lumeer.api.model.ConditionType
import io.lumeer.api.model.ConditionValue
import io.lumeer.api.model.Constraint
import io.lumeer.api.model.ConstraintData
import io.lumeer.api.model.ConstraintType
import io.lumeer.api.model.CurrencyData
import io.lumeer.api.model.Document
import io.lumeer.api.model.Group
import io.lumeer.api.model.Language
import io.lumeer.api.model.LinkAttributeFilter
import io.lumeer.api.model.LinkInstance
import io.lumeer.api.model.LinkType
import io.lumeer.api.model.Permissions
import io.lumeer.api.model.Query
import io.lumeer.api.model.QueryStem
import io.lumeer.api.model.SelectOption
import io.lumeer.api.model.SelectionList
import io.lumeer.api.model.User
import io.lumeer.engine.api.data.DataDocument
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.util.TimeZone
import java.util.zip.GZIPInputStream

/**
 * Replays the parity vectors of the data-filters library (generated with the original JavaScript bundle)
 * through the engine models and DataFilter, so the engine adapter is covered too.
 */
@Suppress("UNCHECKED_CAST")
class DataFilterParityTest {

    private fun Any?.asMap(): Map<String, Any?> = this as? Map<String, Any?> ?: emptyMap()
    private fun Any?.asList(): List<Any?> = this as? List<Any?> ?: emptyList()

    private fun user(value: Any?): User? = value?.asMap()?.let { User(it["id"] as String?, it["name"] as String?, it["email"] as String?, setOf()) }

    private fun conditionValues(list: Any?): List<ConditionValue> = list.asList().map { ConditionValue(it.asMap()["type"] as String?, it.asMap()["value"]) }

    private fun equation(value: Any?): AttributeFilterEquation? = value?.asMap()?.let { e ->
        val filter = e["filter"]?.asMap()
        AttributeFilterEquation(
            e["equations"].asList().mapNotNull { equation(it) },
            filter?.let { AttributeFilter(it["attributeId"] as String, ConditionType.fromString(it["condition"] as String?), conditionValues(it["conditionValues"])) },
            e["operator"] as String?,
        )
    }

    private fun attribute(value: Any?): Attribute {
        val map = value.asMap()
        return Attribute(map["id"] as String).apply {
            map["constraint"]?.asMap()?.let { constraint ->
                val type = constraint["type"] as String?
                if (type != null) {
                    setConstraint(Constraint(ConstraintType.valueOf(type), constraint["config"]))
                }
            }
            map["lock"]?.asMap()?.let { lock ->
                setLock(AttributeLock(lock["exceptionGroups"].asList().map { g ->
                    val group = g.asMap()
                    AttributeLockExceptionGroup(equation(group["equation"]), group["typeValue"].asList().map { it as String }, group["type"] as String?)
                }, lock["locked"] == true))
            }
        }
    }

    private fun constraintData(map: Map<String, Any?>): ConstraintData {
        val currency = map["currencyData"]?.asMap()
        return ConstraintData(
            map["users"].asList().mapNotNull { user(it) },
            user(map["currentUser"]),
            map["durationUnitsMap"]?.asMap()?.mapValues { it.value as String },
            CurrencyData(currency?.get("abbreviations").asList().map { it as String }, currency?.get("ordinals").asList().map { it as String }),
            map["timezone"] as String?,
            map["teams"].asList().map { val t = it.asMap(); Group(t["id"] as String?, t["name"] as String?, null, null, null, t["users"].asList().map { u -> u as String }) },
            map["selectionLists"].asList().map {
                val l = it.asMap()
                SelectionList(l["id"] as String?, l["name"] as String?, null, null, null, l["displayValues"] as Boolean?, l["options"].asList().map { o -> SelectOption(o.asMap()["value"], o.asMap()["displayValue"], null) })
            },
        )
    }

    /** Ids of the filtered documents and link instances */
    private fun run(case: Map<String, Any?>): Pair<List<String>, List<String>> {
        val input = case["input"].asMap()
        val collections = input["collections"].asList().map {
            val c = it.asMap()
            val id = c["id"] as String
            Collection(id, id, "", "", "", null, Permissions(), LinkedHashSet(c["attributes"].asList().map { a -> attribute(a) }), mapOf(), "", null).apply { this.id = id }
        }
        val documents = input["documents"].asList().map {
            val d = it.asMap()
            Document(DataDocument(d["data"].asMap())).apply {
                id = d["id"] as String
                collectionId = d["collectionId"] as String
                (d["metaData"]?.asMap()?.get("parentId") as String?)?.let { metaData = DataDocument(Document.META_PARENT_ID, it) }
            }
        }
        val linkTypes = input["linkTypes"].asList().map {
            val l = it.asMap()
            val id = l["id"] as String
            LinkType(id, l["collectionIds"].asList().map { c -> c as String }, l["attributes"].asList().map { a -> attribute(a) }, mapOf(), Permissions(), null).apply { this.id = id }
        }
        val linkInstances = input["linkInstances"].asList().map {
            val l = it.asMap()
            LinkInstance(l["linkTypeId"] as String, l["documentIds"].asList().map { c -> c as String }).apply {
                id = l["id"] as String
                data = DataDocument(l["data"].asMap())
            }
        }
        val query = input["query"].asMap()
        val stems = query["stems"].asList().map {
            val s = it.asMap()
            val filters = s["filters"].asList().map { f ->
                val filter = f.asMap()
                CollectionAttributeFilter(filter["collectionId"] as String, filter["attributeId"] as String, ConditionType.fromString(filter["condition"] as String?), conditionValues(filter["conditionValues"]))
            }
            val linkFilters = s["linkFilters"].asList().map { f ->
                val filter = f.asMap()
                LinkAttributeFilter(filter["linkTypeId"] as String, filter["attributeId"] as String, ConditionType.fromString(filter["condition"] as String?), conditionValues(filter["conditionValues"]))
            }
            QueryStem(null, s["collectionId"] as String, s["linkTypeIds"].asList().map { id -> id as String }, s["documentIds"].asList().map { id -> id as String }.toSet(), filters, linkFilters)
        }
        val engineQuery = Query(stems, LinkedHashSet(query["fulltexts"].asList().map { it as String }), (query["page"] as Number?)?.toInt(), (query["pageSize"] as Number?)?.toInt())
        val now = (case["now"] as Number).toLong()
        DataFilterAdapter.clock = { now }
        val result = DataFilter.filterDocumentsAndLinksByQueryFromJson(documents, collections, linkTypes, linkInstances, engineQuery, mapOf(), mapOf(),
            constraintData(input["constraintData"].asMap()), input["includeChildren"] == true, input["includeNonLinkedDocuments"] == true, Language.EN)
        return result.first.map { it.id } to result.second.map { it.id }
    }

    @Test
    fun replayVectors() {
        val file = File(System.getProperty("parity.vectors") ?: "../data-filters/src/jvmTest/resources/parity-vectors.json.gz")
        val root = ObjectMapper().readValue(GZIPInputStream(file.inputStream()), Map::class.java) as Map<String, Any?>
        val originalZone = TimeZone.getDefault()
        val originalClock = DataFilterAdapter.clock
        (root["systemTimezone"] as String?)?.let { TimeZone.setDefault(TimeZone.getTimeZone(it)) }
        try {
            val failures = mutableListOf<String>()
            var total = 0
            val engineTypes = ConstraintType.values().map { it.name }.toSet()
            for (case in root["cases"].asList().map { it.asMap() }) {
                val expected = case["expected"] as? List<*> ?: continue
                // the engine does not model every constraint type of the library (e.g. Team)
                val types = (case["input"].asMap()["collections"].asList() + case["input"].asMap()["linkTypes"].asList()).flatMap { it.asMap()["attributes"].asList() }
                    .mapNotNull { it.asMap()["constraint"]?.asMap()?.get("type") as String? }
                if (!engineTypes.containsAll(types)) continue
                total++
                // link instances are compared for the cases generated with them
                val expectedLinks = case["expectedLinks"] as? List<*>
                val (actualDocuments, actualLinks) = run(case)
                if (actualDocuments != expected || (expectedLinks != null && actualLinks != expectedLinks)) {
                    failures.add("case ${case["index"]} (${case["type"]}): expected=$expected ${expectedLinks ?: ""} actual=$actualDocuments $actualLinks")
                }
            }
            println("Engine parity: $total cases, ${failures.size} failures")
            assertThat(failures).describedAs(failures.take(10).joinToString("\n")).isEmpty()
        } finally {
            TimeZone.setDefault(originalZone)
            DataFilterAdapter.clock = originalClock
        }
    }
}
