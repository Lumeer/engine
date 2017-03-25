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

import io.lumeer.engine.api.LumeerConst;

import java.io.Serializable;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public interface DataStorageDialect extends Serializable {

   // CollectionMetadataFacade
   DataDocument updateCollectionAttributeCountQuery(final String metadataCollectionName, final String attributeName);

   // LinkingFacade
   String linkingFromTablesColNameFilter(final String collectionName, final String role);

   String linkingFromTablesFilter(final String firstCollectionName, final String role, final LumeerConst.Linking.LinkDirection linkDirection);

   String linkingFromToTablesFilter(final String firstCollectionName, final String secondCollectionName, final String role, final LumeerConst.Linking.LinkDirection linkDirection);

   String linkingFromToDocumentFilter(final String fromId, final String toId, final LumeerConst.Linking.LinkDirection linkDirection);

   String linkingFromDocumentFilter(final String fromId, final LumeerConst.Linking.LinkDirection linkDirection);

   String fieldValueFilter(final String fieldName, final Object value);

   String documentIdFilter(final String documentId);

}
