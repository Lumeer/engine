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
package io.lumeer.storage.api.dao;

import io.lumeer.api.model.AuditRecord;
import io.lumeer.api.model.AuditType;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

public interface AuditDao extends ProjectScopedDao, Serializable {

   AuditRecord findLatestAuditRecord(final String parentId, final ResourceType resourceType, final String resourceId);
   AuditRecord findLatestAuditRecord(final String parentId, final ResourceType resourceType, final String resourceId, final AuditType type);

   List<AuditRecord> findAuditRecords(final Set<String> collectionIds, final Set<String> linkTypeIds, final Set<String> viewIds, final ZonedDateTime noOlderThan);
   List<AuditRecord> findAuditRecords(final Set<String> collectionIds, final Set<String> linkTypeIds, final Set<String> viewIds, final ZonedDateTime noOlderThan, final int countLimit);
   List<AuditRecord> findAuditRecords(final String userId, final Set<String> collectionIds, final Set<String> linkTypeIds, final Set<String> viewIds, final ZonedDateTime noOlderThan);
   List<AuditRecord> findAuditRecords(final String userId, final Set<String> collectionIds, final Set<String> linkTypeIds, final Set<String> viewIds, final ZonedDateTime noOlderThan, final int countLimit);

   List<AuditRecord> findAuditRecords(final String parentId, final ResourceType resourceType, final ZonedDateTime noOlderThan);
   List<AuditRecord> findAuditRecords(final String parentId, final ResourceType resourceType, final ZonedDateTime noOlderThan, final int countLimit);

   List<AuditRecord> findAuditRecords(final String parentId, final ResourceType resourceType, final String resourceId, final ZonedDateTime noOlderThan);
   List<AuditRecord> findAuditRecords(final String parentId, final ResourceType resourceType, final String resourceId, final ZonedDateTime noOlderThan, final int countLimit);

   List<AuditRecord> findAuditRecords(final ZonedDateTime olderThan, final Set<AuditType> types);
   void cleanAuditRecords(final ZonedDateTime olderThan);


   AuditRecord createAuditRecord(final AuditRecord record);
   AuditRecord updateAuditRecord(final AuditRecord record);

   AuditRecord getAuditRecord(final String id);

   void deleteAuditRecord(final String id);

   void ensureIndexes(final Project project);
}
