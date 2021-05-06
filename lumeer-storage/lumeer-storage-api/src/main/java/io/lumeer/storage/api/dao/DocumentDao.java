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

import io.lumeer.api.model.Document;
import io.lumeer.engine.api.data.DataDocument;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DocumentDao extends ProjectScopedDao {

   Document createDocument(Document document);

   List<Document> createDocuments(List<Document> documents);

   Document updateDocument(String id, Document document);

   void deleteDocument(String id, DataDocument data);

   void deleteDocuments(String collectionId);

   Document getDocumentById(String id);

   Long getDocumentsCountByCollection(String collectionId);

   Map<String, Long> getDocumentsCounts();

   List<Document> getDocumentsByIds(String... ids);

   List<Document> getDocumentsByIds(Set<String> ids);

   List<Document> getDocumentsByParentId(final String parentId);

   List<Document> getDocumentsByCollection(String collectionId);

   List<Document> getDocumentsWithTemplateId();

   List<Document> getRecentDocuments(final String collectionId, boolean byUpdate);

   List<Document> getDocumentsByCollectionIds(Collection<String> collectionIds);

   List<Document> duplicateDocuments(List<Document> documents);

}
