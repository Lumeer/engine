/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
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
package io.lumeer.storage.api.dao;

import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.query.SearchQuery;

import java.util.List;

public interface DataDao {

   void createDataRepository(String collectionId);

   void deleteDataRepository(String collectionId);

   DataDocument createData(String collectionId, String documentId, DataDocument data);

   List<DataDocument> createData(String collectionId, List<DataDocument> data);

   DataDocument updateData(String collectionId, String documentId, DataDocument data);

   DataDocument patchData(String collectionId, String documentId, DataDocument data);

   void deleteData(String collectionId, String documentId);

   DataDocument getData(String collectionId, String documentId);

   List<DataDocument> getData(String collectionId, SearchQuery query);

   long getDataCount(String collectionId, SearchQuery query);

}
