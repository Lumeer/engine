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

import io.lumeer.api.model.Document;
import io.lumeer.api.model.Project;

import java.util.List;

public interface DocumentDao {

   void createDocumentsRepository(Project project);

   void deleteDocumentsRepository(Project project);

   Document createDocument(Document document);

   List<Document> createDocuments(List<Document> documents);

   Document updateDocument(String id, Document document);

   void deleteDocument(String id);

   void deleteDocuments(String collectionId);

   Document getDocumentById(String id);

   List<Document> getDocumentsByIds(String... ids);

   void setProject(Project project);

}
