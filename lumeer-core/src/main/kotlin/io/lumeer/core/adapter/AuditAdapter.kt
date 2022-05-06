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

import io.lumeer.api.model.*
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

   fun getAuditRecords(userId: String, collectionIds: Set<String>, linkTypeIds: Set<String>, viewIds: Set<String>, serviceLevel: Payment.ServiceLevel) =
      if (serviceLevel == Payment.ServiceLevel.FREE)
         auditDao.findAuditRecords(userId, collectionIds, linkTypeIds, viewIds, FREE_MAX_RECORDS)
      else
         auditDao.findAuditRecords(userId, collectionIds, linkTypeIds, viewIds, ZonedDateTime.now().minus(BUSINESS_MAX_WEEKS, ChronoUnit.WEEKS))

   fun getAuditRecords(collectionIds: Set<String>, linkTypeIds: Set<String>, viewIds: Set<String>, serviceLevel: Payment.ServiceLevel) =
      if (serviceLevel == Payment.ServiceLevel.FREE)
         auditDao.findAuditRecords(collectionIds, linkTypeIds, viewIds, FREE_MAX_RECORDS)
      else
         auditDao.findAuditRecords(collectionIds, linkTypeIds, viewIds, ZonedDateTime.now().minus(BUSINESS_MAX_WEEKS, ChronoUnit.WEEKS))

   fun getAuditRecords(parentId: String, resourceType: ResourceType, serviceLevel: Payment.ServiceLevel) =
      if (serviceLevel == Payment.ServiceLevel.FREE)
         auditDao.findAuditRecords(parentId, resourceType, FREE_MAX_RECORDS)
      else
         auditDao.findAuditRecords(parentId, resourceType, ZonedDateTime.now().minus(BUSINESS_MAX_WEEKS, ChronoUnit.WEEKS))

   fun getAuditRecords(parentId: String, resourceType: ResourceType, resourceId: String, serviceLevel: Payment.ServiceLevel) =
      if (serviceLevel == Payment.ServiceLevel.FREE)
         auditDao.findAuditRecords(parentId, resourceType, resourceId, FREE_MAX_RECORDS)
      else
         auditDao.findAuditRecords(parentId, resourceType, resourceId, ZonedDateTime.now().minus(BUSINESS_MAX_WEEKS, ChronoUnit.WEEKS))

   fun registerEnter(parentId: String, resourceType: ResourceType, resourceId: String, user: User?): AuditRecord {
      val auditRecord = AuditRecord(parentId, resourceType, resourceId, ZonedDateTime.now(), user?.id, user?.name, user?.email, null, null, DataDocument(), DataDocument())
      auditRecord.type = AuditType.Entered
      return auditDao.createAuditRecord(auditRecord)
   }

   fun registerDelete(parentId: String, resourceType: ResourceType, resourceId: String, user: User?, automation: String?, viewId: String?, oldState: DataDocument): AuditRecord {
      val partialOldState = DataDocument(oldState.filterKeys { it != DataDocument.ID })
      val auditRecord = AuditRecord(parentId, resourceType, resourceId, ZonedDateTime.now(), user?.id, user?.name, user?.email, viewId, automation, partialOldState, DataDocument())
      auditRecord.type = AuditType.Deleted
      return auditDao.createAuditRecord(auditRecord)
   }

   fun registerCreate(parentId: String, resourceType: ResourceType, resourceId: String, user: User?, automation: String?, viewId: String?, newState: DataDocument): AuditRecord {
      val partialNewState = DataDocument(newState.filterKeys { it != DataDocument.ID })
      val auditRecord = AuditRecord(parentId, resourceType, resourceId, ZonedDateTime.now(), user?.id, user?.name, user?.email, viewId, automation, DataDocument(), partialNewState)
      auditRecord.type = AuditType.Created
      return auditDao.createAuditRecord(auditRecord)
   }

   fun registerRevert(parentId: String, resourceType: ResourceType, resourceId: String, user: User?, automation: String?, viewId: String?, oldState: DataDocument, newState: DataDocument): AuditRecord {
      val partialNewState = DataDocument(newState.filterKeys { it != DataDocument.ID })
      val partialOldState = DataDocument(oldState.filterKeys { it != DataDocument.ID })
      val auditRecord = AuditRecord(parentId, resourceType, resourceId, ZonedDateTime.now(), user?.id, user?.name, user?.email, viewId, automation, partialOldState, partialNewState)
      auditRecord.type = AuditType.Reverted
      return auditDao.createAuditRecord(auditRecord)
   }

   fun registerDataChange(parentId: String, resourceType: ResourceType, resourceId: String, user: User?, automation: String?, viewId: String?, oldState: DataDocument, oldStateDecoded: DataDocument, newState: DataDocument, newStateDecoded: DataDocument) =
      getChanges(oldStateDecoded, newStateDecoded).takeIf { it.isNotEmpty() }?.let { changes ->
         val lastAuditRecord = auditDao.findLatestAuditRecord(parentId, resourceType, resourceId, AuditType.Updated)

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

            val auditRecord = AuditRecord(parentId, resourceType, resourceId, ZonedDateTime.now(), user?.id, user?.name, user?.email, viewId, automation, partialOldState, changes)
            auditRecord.type = AuditType.Updated
            auditDao.createAuditRecord(auditRecord)
         }
      }

   private fun changesOverlap(lastAuditRecord: AuditRecord, userId: String?, automation: String?, changes: DataDocument): Boolean = when {
      (StringUtils.isNotEmpty(lastAuditRecord.user) || StringUtils.isNotEmpty(userId)) && lastAuditRecord.user != userId -> false
      (StringUtils.isNotEmpty(lastAuditRecord.automation) || StringUtils.isNotEmpty(automation)) && lastAuditRecord.automation != automation -> false
      lastAuditRecord.changeDate.isBefore(ZonedDateTime.now().minusMinutes(UPDATE_MERGE_WINDOW_MINUTES)) -> false
      else -> true
   }

   private fun getChanges(oldState: DataDocument, newState: DataDocument): DataDocument {
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

   companion object {

      @JvmStatic
      fun getAuditAdapter(daoContextSnapshot: DaoContextSnapshot) = AuditAdapter(daoContextSnapshot.auditDao)
   }
}
