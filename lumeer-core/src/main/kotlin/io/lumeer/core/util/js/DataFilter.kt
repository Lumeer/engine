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
import io.lumeer.api.model.Collection
import io.lumeer.api.model.ConstraintData
import io.lumeer.api.model.Document
import io.lumeer.api.model.Language
import io.lumeer.api.model.LinkInstance
import io.lumeer.api.model.LinkType
import io.lumeer.api.model.Query
import io.lumeer.core.util.Tuple

class DataFilter {

    companion object {

        @JvmStatic
        fun filterDocumentsAndLinksByQuery(documents: List<Document>,
                                           collections: List<Collection>, linkTypes: List<LinkType>, linkInstances: List<LinkInstance>,
                                           query: Query, collectionsPermissions: Map<String, AllowedPermissions>, linkTypesPermissions: Map<String, AllowedPermissions>,
                                           constraintData: ConstraintData, includeChildren: Boolean, includeNonLinkedDocuments: Boolean, language: Language = Language.EN): Tuple<List<Document>, List<LinkInstance>> {
            val task = DataFilterTask(documents, collections, linkTypes, linkInstances, query, collectionsPermissions, linkTypesPermissions, constraintData, includeChildren, includeNonLinkedDocuments, language)
            return task.call()
        }

        @JvmStatic
        fun filterDocumentsAndLinksByQueryFromJson(documents: List<Document>,
                                                   collections: List<Collection>, linkTypes: List<LinkType>, linkInstances: List<LinkInstance>,
                                                   query: Query, collectionsPermissions: Map<String, AllowedPermissions>, linkTypesPermissions: Map<String, AllowedPermissions>,
                                                   constraintData: ConstraintData, includeChildren: Boolean, includeNonLinkedDocuments: Boolean, language: Language = Language.EN): Tuple<List<Document>, List<LinkInstance>> {
            val task = DataFilterJsonTask(documents, collections, linkTypes, linkInstances, query, collectionsPermissions, linkTypesPermissions, constraintData, includeChildren, includeNonLinkedDocuments, language)
            return task.call()
        }

        @JvmStatic
        fun filterDocumentsAndLinksByQueryDecodingFromJson(documents: List<Document>,
                                                   collections: List<Collection>, linkTypes: List<LinkType>, linkInstances: List<LinkInstance>,
                                                   query: Query, collectionsPermissions: Map<String, AllowedPermissions>, linkTypesPermissions: Map<String, AllowedPermissions>,
                                                   constraintData: ConstraintData, includeChildren: Boolean, language: Language = Language.EN): Tuple<List<Document>, List<LinkInstance>> {
            val task = DataFilterDecodingJsonTask(documents, collections, linkTypes, linkInstances, query, collectionsPermissions, linkTypesPermissions, constraintData, includeChildren, false, language)
            return task.call()
        }
    }
}
