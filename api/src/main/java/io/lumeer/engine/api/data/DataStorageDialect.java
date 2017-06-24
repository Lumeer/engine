/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.engine.api.data;

import java.io.Serializable;
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

   public DataFilter fieldValueWildcardFilterOneSided(final String fieldName, final Object valuePart);

   DataFilter documentFilter(final String documentFilter);

   DataFilter documentNestedIdFilter(final String documentId);

   DataFilter documentNestedIdFilterWithVersion(final String documentId, final int version);

   DataFilter documentIdFilter(final String documentId);

   DataFilter multipleFieldsValueFilter(final Map<String, Object> fields);

   DataFilter combineFilters(DataFilter... filters);

   DataSort documentSort(final String documentSort);

   DataSort documentFieldSort(final String fieldName, final int sortOrder);

   String concatFields(String... fields);
}
