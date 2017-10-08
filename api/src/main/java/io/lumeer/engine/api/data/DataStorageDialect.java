/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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
package io.lumeer.engine.api.data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public interface DataStorageDialect extends Serializable {

   // CollectionMetadataFacade
   DataDocument renameAttributeQuery(final String metadataCollection, final String collection, final String oldName, final String newName);

   DataDocument addRecentlyUsedDocumentQuery(final String metadataCollection, final String collection, final String id, final int listSize);

   DataDocument[] usersOfGroupAggregate(final String organization, final String group);

   DataFilter fieldValueFilter(final String fieldName, final Object value);

   DataFilter fieldValueWildcardFilter(final String fieldName, final Object valuePart);

   DataFilter fieldValueWildcardFilterOneSided(final String fieldName, final Object valuePart);

   DataFilter documentFilter(final String documentFilter);

   DataFilter documentNestedIdFilter(final String documentId);

   DataFilter documentNestedIdFilterWithVersion(final String documentId, final int version);

   DataFilter documentIdFilter(final String documentId);

   DataFilter multipleFieldsValueFilter(final Map<String, Object> fields);

   DataFilter combineFilters(DataFilter... filters);

   DataFilter collectionPermissionsRoleFilter(final String role, final String user, final List<String> groups);

   DataSort documentSort(final String documentSort);

   DataSort documentFieldSort(final String fieldName, final int sortOrder);

   String concatFields(String... fields);
}
