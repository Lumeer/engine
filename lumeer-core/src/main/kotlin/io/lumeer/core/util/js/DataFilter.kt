package io.lumeer.core.util.js

import io.lumeer.api.model.*
import io.lumeer.api.model.Collection
import io.lumeer.core.util.Tuple
import org.graalvm.polyglot.Value
import java.util.concurrent.ExecutorCompletionService
import java.util.logging.Level
import java.util.logging.Logger
import java.util.concurrent.Executors

import java.util.concurrent.ThreadPoolExecutor
import kotlin.math.max
import kotlin.math.min


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
class DataFilter : AutoCloseable {

    companion object {
        private val logger: Logger = Logger.getLogger(DataFilter::class.simpleName)
        private val threads = min(max((Runtime.getRuntime().availableProcessors() / 2), 8), 1)
        private val executor = Executors.newFixedThreadPool(threads) as ThreadPoolExecutor
        private val completionService = ExecutorCompletionService<Tuple<List<Document>, List<LinkInstance>>>(executor)

        @JvmStatic
        fun filterDocumentsAndLinksByQuery(documents: List<Document>,
                                           collections: List<Collection>, linkTypes: List<LinkType>, linkInstances: List<LinkInstance>,
                                           query: Query, collectionsPermissions: Map<String, AllowedPermissions>, linkTypesPermissions: Map<String, AllowedPermissions>,
                                           constraintData: ConstraintData, includeChildren: Boolean, language: Language = Language.EN): Tuple<List<Document>, List<LinkInstance>> {
            val task = DataFilterTask(documents, collections, linkTypes, linkInstances, query, collectionsPermissions, linkTypesPermissions, constraintData, includeChildren, language)
            val future = completionService.submit(task)
            return future.get()
        }
    }

    override fun close() {
        DataFilterTask.close()
    }
}
