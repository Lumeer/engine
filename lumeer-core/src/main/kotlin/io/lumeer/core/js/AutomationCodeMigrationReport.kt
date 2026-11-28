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
package io.lumeer.core.js

import com.fasterxml.jackson.annotation.JsonUnwrapped

/** What the automation code migration found (dry run) or did, for each rule and attribute function with legacy code. */
class AutomationCodeMigrationReport(val dryRun: Boolean) {

    enum class Status {
        /** dry run: the code would be migrated */
        WOULD_MIGRATE,
        MIGRATED,
        /** the code changed while migrating, nothing was written; run the migration again */
        CONFLICT,
        /** the migrated code does not parse, nothing was written */
        INVALID,
        /** legacy calls the migration does not know, they keep running through the shim */
        NOT_MIGRATED,
    }

    /** Where a rule or function is: the organization, project and collection or link type, by id and code or name. */
    class Location(
        val organizationId: String,
        val organization: String,
        val projectId: String,
        val project: String,
        val collectionId: String? = null,
        val collection: String? = null,
        val linkTypeId: String? = null,
        val linkType: String? = null,
    )

    class Item(
        @get:JsonUnwrapped val location: Location,
        val ruleId: String?,
        val rule: String?,
        val attributeId: String?,
        val attribute: String?,
        val status: Status,
        /** the rewrites, also for INVALID and CONFLICT items, where they were not written */
        val replacements: List<LegacyCodeMigration.Replacement>,
        /** the legacy calls the stored code has after this run */
        val legacyCallsLeft: List<String>,
        val reason: String?,
    )

    /** An organization or project the migration could not process. */
    class Failure(val organizationId: String, val organization: String, val projectId: String?, val project: String?, val message: String)

    var organizations = 0
    var projects = 0
    val items = mutableListOf<Item>()
    val errors = mutableListOf<Failure>()

    /** items whose code was (or in a dry run would be) rewritten; INVALID and CONFLICT items keep their code */
    private val changedItems: List<Item> get() = items.filter { it.status == Status.MIGRATED || it.status == Status.WOULD_MIGRATE }

    val projectsWithChanges: Int get() = changedItems.map { it.location.organizationId to it.location.projectId }.distinct().size
    val changes: Int get() = changedItems.sumOf { it.replacements.size }
    val legacyCallsLeft: Int get() = items.sumOf { it.legacyCallsLeft.size }
}
