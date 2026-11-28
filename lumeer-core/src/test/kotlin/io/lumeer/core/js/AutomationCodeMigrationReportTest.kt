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

import io.lumeer.core.js.AutomationCodeMigrationReport.Item
import io.lumeer.core.js.AutomationCodeMigrationReport.Location
import io.lumeer.core.js.AutomationCodeMigrationReport.Status
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AutomationCodeMigrationReportTest {

    private fun item(project: String, status: Status, replacements: Int, legacyCallsLeft: List<String> = emptyList()) = Item(
        Location("o1", "ORG", project, project), "r", "rule", null, null, status,
        List(replacements) { LegacyCodeMigration.Replacement("before", "after") }, legacyCallsLeft, null,
    )

    @Test
    fun countsOnlyWrittenChanges() {
        val report = AutomationCodeMigrationReport(false).apply {
            items.add(item("p1", Status.MIGRATED, 2))
            items.add(item("p2", Status.CONFLICT, 3, listOf("moment", "he.escape", "formatMomentJsDate")))
            items.add(item("p3", Status.INVALID, 1, listOf("moment")))
            items.add(item("p3", Status.NOT_MIGRATED, 0, listOf("moment")))
        }
        assertThat(report.changes).isEqualTo(2)
        assertThat(report.projectsWithChanges).isEqualTo(1)
        assertThat(report.legacyCallsLeft).isEqualTo(5)
    }

    @Test
    fun countsDryRunChanges() {
        val report = AutomationCodeMigrationReport(true).apply {
            items.add(item("p1", Status.WOULD_MIGRATE, 2))
            items.add(item("p2", Status.WOULD_MIGRATE, 1))
        }
        assertThat(report.changes).isEqualTo(3)
        assertThat(report.projectsWithChanges).isEqualTo(2)
        assertThat(report.legacyCallsLeft).isEqualTo(0)
    }
}
