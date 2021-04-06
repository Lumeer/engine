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
package io.lumeer.core.adapter

import io.lumeer.api.model.AuditRecord
import io.lumeer.api.model.Payment
import io.lumeer.api.model.ResourceType
import io.lumeer.api.model.User
import io.lumeer.engine.api.data.DataDocument
import io.lumeer.storage.api.dao.AuditDao
import io.lumeer.storage.api.dao.context.DaoContextSnapshot
import org.apache.commons.lang3.StringUtils
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

private const val FREE_MAX_RECORDS: Int = 3 // number of last records available
private const val BUSINESS_MAX_WEEKS: Long = 2 // number of last weeks of records available
private const val UPDATE_MERGE_WINDOW_MINUTES: Long = 5 // number of minutes to merge record changes by the same originator (user or automation)

class AuditAdapter(private val auditDao: AuditDao) {

   fun getAuditRecords(parentId: String, resourceType: ResourceType, resourceId: String, serviceLevel: Payment.ServiceLevel) =
         if (serviceLevel == Payment.ServiceLevel.FREE)
            auditDao.findAuditRecords(parentId, resourceType, resourceId, FREE_MAX_RECORDS)
         else
            auditDao.findAuditRecords(parentId, resourceType, resourceId, ZonedDateTime.now().minus(BUSINESS_MAX_WEEKS, ChronoUnit.WEEKS))

   fun registerUpdate(parentId: String, resourceType: ResourceType, resourceId: String, user: User?, automation: String?, oldState: DataDocument, oldStateDecoded: DataDocument, newState: DataDocument, newStateDecoded: DataDocument) =
         getChanges(oldStateDecoded, newStateDecoded).takeIf { it.isNotEmpty() }?.let { changes ->
            val lastAuditRecord = auditDao.findLatestAuditRecord(parentId, resourceType, resourceId)

            if (lastAuditRecord != null && changesOverlap(lastAuditRecord, user?.id, automation, changes)) {
               changes.keys.forEach {
                  if (!lastAuditRecord.oldState.containsKey(it) && !lastAuditRecord.newState.containsKey(it))
                     lastAuditRecord.oldState[it] = oldState[it]
               }
               lastAuditRecord.newState.putAll(changes)
               changes.keys.forEach {
                  if (lastAuditRecord.oldState[it] == lastAuditRecord.newState[it]) {
                     lastAuditRecord.oldState.remove(it)
                     lastAuditRecord.newState.remove(it)
                  }
               }
               lastAuditRecord.changeDate = ZonedDateTime.now()

               if (lastAuditRecord.newState.isEmpty()) {
                  auditDao.deleteAuditRecord(lastAuditRecord.id)
                  lastAuditRecord
               } else
                  auditDao.updateAuditRecord(lastAuditRecord)
            } else {
               // we will keep only those values that changed
               val partialOldState = DataDocument(oldState.filterKeys { it != DataDocument.ID })
               val oldStateKeys = HashSet(partialOldState.keys)
               oldStateKeys.forEach {
                  if (!changes.containsKey(it)) partialOldState.remove(it)
               }

               // we need to clean the history only when adding new entries
               // we keep business level history in case the user upgraded
               auditDao.cleanAuditRecords(parentId, resourceType, resourceId, ZonedDateTime.now().minusWeeks(BUSINESS_MAX_WEEKS))

               val auditRecord = AuditRecord(parentId, resourceType, resourceId, ZonedDateTime.now(), user?.id, user?.name, user?.email, automation, partialOldState, changes)
               auditDao.createAuditRecord(auditRecord)
            }
         }

   private fun changesOverlap(lastAuditRecord: AuditRecord, userId: String?, automation: String?, changes: DataDocument): Boolean = when {
      StringUtils.isNotEmpty(lastAuditRecord.user) && lastAuditRecord.user != userId -> false
      StringUtils.isNotEmpty(lastAuditRecord.automation) && lastAuditRecord.automation != automation -> false
      lastAuditRecord.changeDate.isBefore(ZonedDateTime.now().minusMinutes(UPDATE_MERGE_WINDOW_MINUTES)) -> false
      else -> true
   }

   fun getChanges(oldState: DataDocument, newState: DataDocument): DataDocument {
      val result = DataDocument(newState.filterKeys { it != DataDocument.ID })
      oldState.keys.filter { it != DataDocument.ID }.forEach {
         // remove everything that did not change, keeping newly added values
         if (result.containsKey(it) && result[it] == oldState[it])
            result.remove(it)
         else
            // make sure that deleted values are present in the changes
            if (!result.containsKey(it)) result[it] = null
      }

      return result
   }

   fun removeAllAuditRecords(parentId: String, resourceType: ResourceType, resourceId: String) {
      auditDao.deleteAuditRecords(parentId, resourceType, resourceId)
   }

   companion object {

      @JvmStatic
      fun getAuditAdapter(daoContextSnapshot: DaoContextSnapshot) = AuditAdapter(daoContextSnapshot.auditDao)
   }
}
