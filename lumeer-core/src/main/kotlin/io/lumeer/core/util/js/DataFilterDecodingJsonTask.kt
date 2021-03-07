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

import io.lumeer.api.model.*
import io.lumeer.api.model.Collection
import io.lumeer.core.constraint.ConstraintManager
import io.lumeer.core.util.Tuple
import java.util.concurrent.Callable
import java.util.logging.Level
import java.util.logging.Logger

data class DataFilterDecodingJsonTask(val documents: List<Document>,
                              val collections: List<Collection>,
                              val linkTypes: List<LinkType>,
                              val linkInstances: List<LinkInstance>,
                              val query: Query,
                              val collectionsPermissions: Map<String, AllowedPermissions>,
                              val linkTypesPermissions: Map<String, AllowedPermissions>,
                              val constraintData: ConstraintData,
                              val includeChildren: Boolean,
                              val language: Language = Language.EN) : Callable<Tuple<List<Document>, List<LinkInstance>>> {

    override fun call(): Tuple<List<Document>, List<LinkInstance>> {
        val constraintManager = ConstraintManager()
        constraintManager.locale = language.toLocale()

        val collectionsById = collections.associateBy { it.id }
        val decodedDocuments = ArrayList<Document>()
        documents.forEach { doc ->
            collectionsById[doc.collectionId]?.let {
                val collection = collectionsById[doc.collectionId]
                val decodedDoc = Document(doc)
                decodedDoc.data = constraintManager.decodeDataTypes(collection, doc.data)
                decodedDocuments.add(decodedDoc)
            }
        }

        val linkTypesById = linkTypes.associateBy { it.id }
        val decodedLinks = ArrayList<LinkInstance>()
        linkInstances.forEach { link ->
            linkTypesById[link.linkTypeId]?.let {
                val linkType = linkTypesById[link.linkTypeId]
                val decodedLink = LinkInstance(link)
                decodedLink.data = constraintManager.decodeDataTypes(linkType, link.data)
                decodedLinks.add(decodedLink)
            }
        }

        val emptyTuple = Tuple<List<Document>, List<LinkInstance>>(emptyList(), emptyList())
        val context = DataFilterJsonTask.getContext()

        return try {
            val filterJsValue = DataFilterJsonTask.getFunction(context)

            val json = DataFilterJsonTask.convertToJson(DataFilterJson(decodedDocuments, collections, linkTypes, decodedLinks, query, collectionsPermissions, linkTypesPermissions, constraintData, includeChildren, language.toLanguageTag()))

            val result = filterJsValue.execute(json)

            if (result != null) {
                val documentsMap = documents.groupBy { it.id }
                val resultDocumentsList = mutableListOf<Document>()
                val resultDocuments = result.getMember("documentsIds")
                for (i in 0 until resultDocuments.arraySize) resultDocumentsList.addAll(documentsMap[resultDocuments.getArrayElement(i).asString()].orEmpty())

                val linkInstancesMap = linkInstances.groupBy { it.id }
                val resultLinksList = mutableListOf<LinkInstance>()
                val resultLinks = result.getMember("linkInstancesIds")
                for (i in 0 until resultLinks.arraySize) resultLinksList.addAll(linkInstancesMap[resultLinks.getArrayElement(i).asString()].orEmpty())

                Tuple(resultDocumentsList, resultLinksList)
            } else {
                logger.log(Level.SEVERE, "Error filtering data - null result.")
                emptyTuple
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error filtering data: ", e)
            emptyTuple
        } finally {
            context.close()
        }
    }

    companion object {
        private val logger: Logger = Logger.getLogger(DataFilterDecodingJsonTask::class.simpleName)
    }
}
