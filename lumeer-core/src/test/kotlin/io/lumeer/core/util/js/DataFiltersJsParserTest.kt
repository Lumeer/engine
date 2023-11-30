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

import io.lumeer.api.model.AllowedPermissions
import io.lumeer.api.model.Attribute
import io.lumeer.api.model.Collection
import io.lumeer.api.model.CollectionAttributeFilter
import io.lumeer.api.model.ConditionType
import io.lumeer.api.model.ConstraintData
import io.lumeer.api.model.CurrencyData
import io.lumeer.api.model.Document
import io.lumeer.api.model.LinkInstance
import io.lumeer.api.model.LinkPermissionsType
import io.lumeer.api.model.LinkType
import io.lumeer.api.model.Permissions
import io.lumeer.api.model.Query
import io.lumeer.api.model.QueryStem
import io.lumeer.engine.api.data.DataDocument
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class DataFiltersJsParserTest {

    private val collection1 = Collection("c1", "c1", "", "", "", null, Permissions(), setOf(Attribute("a1")), mapOf(), "", null).apply {
        id = "c1"
    }
    private val collection2 = Collection("c2", "c2", "", "", "", null, Permissions(), setOf(Attribute("a1")), mapOf(), "", null).apply {
        id = "c2"
    }
    private val linkType = LinkType("lt1", listOf(collection1.id, collection2.id), listOf(), mapOf(), Permissions(), LinkPermissionsType.Custom).apply { id = "lt1" }

    @Test
    fun test() {
        val c1AttributeId = collection1.attributes.first().id
        val document1 = Document(DataDocument(c1AttributeId, "abc")).apply {
            id = "d1"
            collectionId = collection1.id
        }
        val document2 = Document(DataDocument(c1AttributeId, "abcd")).apply {
            id = "d2"
            collectionId = collection1.id
        }
        val c2AttributeId = collection2.attributes.first().id
        val document3 = Document(DataDocument(c2AttributeId, "lumeer")).apply {
            id = "d3"
            collectionId = collection2.id
        }
        val document4 = Document(DataDocument(c2AttributeId, "nolumeer")).apply {
            id = "d4"
            collectionId = collection2.id
        }

        val permissions = AllowedPermissions.allAllowed()
        val collectionsPermissions = mapOf(collection1.id to permissions, collection2.id to permissions)
        val linkTypPermissions = mapOf(linkType.id to permissions)
        val constraintData = ConstraintData(listOf(), null, mapOf(), CurrencyData(listOf(), listOf()), "Europe/Bratislava", listOf(), listOf())

        val link1 = LinkInstance(linkType.id, listOf(document1.id, document3.id)).apply { id = "li1" }
        val link2 = LinkInstance(linkType.id, listOf(document2.id, document4.id)).apply { id = "li2" }

        val simpleQuery = Query(listOf(QueryStem(null, collection1.id, listOf(), setOf(), listOf(), listOf())), setOf(), 0, 10)
        val simpleResult = DataFilter.filterDocumentsAndLinksByQuery(
                listOf(document1, document2),
                listOf(collection1),
                listOf(),
                listOf(),
                simpleQuery, collectionsPermissions, linkTypPermissions, constraintData, true, false
        )
        val simpleResult2 = DataFilter.filterDocumentsAndLinksByQueryFromJson(
                listOf(document1, document2),
                listOf(collection1),
                listOf(),
                listOf(),
                simpleQuery, collectionsPermissions, linkTypPermissions, constraintData, true, false
        )

        Assertions.assertThat(simpleResult.first).containsOnly(document1, document2)
        Assertions.assertThat(simpleResult2.first).containsOnly(document1, document2)

        val throughLinkFilter = CollectionAttributeFilter.createFromValues(collection2.id, c2AttributeId, ConditionType.EQUALS, "lumeer")
        val linkQuery = Query(listOf(QueryStem(null, collection1.id, listOf(linkType.id), setOf(), listOf(throughLinkFilter), listOf())), setOf(), 0, 10)

        val linkResult = DataFilter.filterDocumentsAndLinksByQuery(
                listOf(document1, document2, document3, document4),
                listOf(collection1, collection2),
                listOf(linkType),
                listOf(link1, link2),
                linkQuery, collectionsPermissions, linkTypPermissions, constraintData, true, false
        )
        val linkResult2 = DataFilter.filterDocumentsAndLinksByQueryFromJson(
                listOf(document1, document2, document3, document4),
                listOf(collection1, collection2),
                listOf(linkType),
                listOf(link1, link2),
                linkQuery, collectionsPermissions, linkTypPermissions, constraintData, true, false
        )

        Assertions.assertThat(linkResult.first).containsOnly(document1, document3)
        Assertions.assertThat(linkResult.second).containsOnly(link1)
        Assertions.assertThat(linkResult2.first).containsOnly(document1, document3)
        Assertions.assertThat(linkResult2.second).containsOnly(link1)
    }

    @Test
    fun performanceTest() {
        Task(1, 1000).run()
    }

    @Test
    fun multiThreadingTest() {
        val executor = Executors.newFixedThreadPool(4) as ThreadPoolExecutor

        for (i in 1..16) {
            executor.submit(Task(i, 10))
        }

        executor.shutdown()
        executor.awaitTermination(30, TimeUnit.SECONDS)
    }

    class Task(val id: Int, val tasks: Int) : Runnable {
        private val collection1 = Collection("c1", "c1", "", "", "", null, Permissions(), setOf(Attribute("a1")), mapOf(), "", null).apply {
            id = "c1"
        }
        private val collection2 = Collection("c2", "c2", "", "", "", null, Permissions(), setOf(Attribute("a1")), mapOf(), "", null).apply {
            id = "c2"
        }
        private val linkType = LinkType("lt1", listOf(collection1.id, collection2.id), listOf(), mapOf(), Permissions(), LinkPermissionsType.Custom).apply { id = "lt1" }

        override fun run() {
            val c1AttributeId = collection1.attributes.first().id
            val document1 = Document(DataDocument(c1AttributeId, "abc")).apply {
                id = "d1"
                collectionId = collection1.id
            }
            val document2 = Document(DataDocument(c1AttributeId, "abcd")).apply {
                id = "d2"
                collectionId = collection1.id
            }
            val c2AttributeId = collection2.attributes.first().id
            val document3 = Document(DataDocument(c2AttributeId, "lumeer")).apply {
                id = "d3"
                collectionId = collection2.id
            }
            val document4 = Document(DataDocument(c2AttributeId, "nolumeer")).apply {
                id = "d4"
                collectionId = collection2.id
            }

            val permissions = AllowedPermissions.allAllowed()
            val collectionsPermissions = mapOf(collection1.id to permissions, collection2.id to permissions)
            val linkTypPermissions = mapOf(linkType.id to permissions)
            val constraintData = ConstraintData(listOf(), null, mapOf(), CurrencyData(listOf(), listOf()), "Europe/Bratislava", listOf(), listOf())

            val documents = mutableListOf<Document>()
            val links = mutableListOf<LinkInstance>()

            for (i in 1..tasks) {
                val d = Document(document1).apply { id = "doc${i}" }
                documents.add(d)
                val l = LinkInstance(linkType.id, listOf(d.id, document3.id)).apply { id = "li${i}" }
                links.add(l)
            }

            val throughLinkFilter = CollectionAttributeFilter.createFromValues(collection2.id, c2AttributeId, ConditionType.EQUALS, "lumeer")
            val linkQuery = Query(listOf(QueryStem(null, collection1.id, listOf(linkType.id), setOf(), listOf(throughLinkFilter), listOf())), setOf(), 0, 10)

            val linkResult = DataFilter.filterDocumentsAndLinksByQueryFromJson(
                    documents,
                    listOf(collection1, collection2),
                    listOf(linkType),
                    links,
                    linkQuery, collectionsPermissions, linkTypPermissions, constraintData, true, false
            )
        }

    }
}
