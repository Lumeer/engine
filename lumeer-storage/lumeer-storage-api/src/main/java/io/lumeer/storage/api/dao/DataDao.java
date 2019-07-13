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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Pagination;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.query.SearchQueryStem;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public interface DataDao {

   void createDataRepository(String collectionId);

   void deleteDataRepository(String collectionId);

   DataDocument createData(String collectionId, String documentId, DataDocument data);

   List<DataDocument> createData(String collectionId, List<DataDocument> data);

   DataDocument updateData(String collectionId, String documentId, DataDocument data);

   DataDocument patchData(String collectionId, String documentId, DataDocument data);

   void deleteData(String collectionId, String documentId);

   long deleteAttribute(String collectionId, String attributeId);

   DataDocument getData(String collectionId, String documentId);

   List<DataDocument> getData(String collectionId);

   Stream<DataDocument> getDataStream(String collectionId);

   List<DataDocument> getData(String collectionId, Set<String> documentIds);

   List<DataDocument> searchData(SearchQueryStem stem, Pagination pagination, Collection collection);

   List<DataDocument> searchDataByFulltexts(Set<String> fulltexts, Pagination pagination, List<Collection> projectCollections);

}
