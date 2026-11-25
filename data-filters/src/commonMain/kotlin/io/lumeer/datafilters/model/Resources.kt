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
package io.lumeer.datafilters.model

import io.lumeer.datafilters.constraint.Constraint

/**
 * Document or link instance. Implemented by thin adapters around the platform objects
 * (engine Document/LinkInstance on JVM, plain store objects in the browser), so the filter never copies data.
 */
interface DataResource {
    val id: String

    /** Raw value of the attribute, or null when missing. */
    fun value(attributeId: String): Any?
}

interface DocumentModel : DataResource {
    val collectionId: String
    val parentId: String?
}

interface LinkInstanceModel : DataResource {
    val linkTypeId: String
    val documentIds: List<String>
}

class Attribute(val id: String, val constraint: Constraint?, val lock: AttributeLock? = null)

sealed interface AttributesResource {
    val id: String
    val attributes: List<Attribute>
}

class Collection(override val id: String, override val attributes: List<Attribute>) : AttributesResource

class LinkType(override val id: String, val collectionIds: List<String>, override val attributes: List<Attribute>) : AttributesResource
