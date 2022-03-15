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

import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Pagination;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.query.SearchQueryStem;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public interface LinkDataDao {

   void createDataRepository(String linkTypeId);

   void deleteDataRepository(String linkTypeId);

   DataDocument createData(String linkTypeId, String linkInstanceId, DataDocument data);

   List<DataDocument> createData(final String linkTypeId, final List<DataDocument> data);

   DataDocument updateData(String linkTypeId, String linkInstanceId, DataDocument data);

   DataDocument patchData(String linkTypeId, String linkInstanceId, DataDocument data);

   void deleteData(String linkTypeId, String linkInstanceId);

   void deleteData(String linkTypeId, Set<String> linkInstanceIds);

   long deleteAttribute(String linkTypeId, String attributeId);

   DataDocument getData(String linkTypeId, String linkInstanceId);

   List<DataDocument> getData(String linkTypeId);

   List<DataDocument> getData(String linkTypeId, Integer skip, Integer limit);

   Stream<DataDocument> getDataStream(String linkTypeId);

   List<DataDocument> getData(String linkTypeId, Set<String> linkInstanceIds);

   List<DataDocument> getData(String linkTypeId, Set<String> linkInstanceIds, String parameter);

   List<DataDocument> searchData(SearchQueryStem stem, Pagination pagination, LinkType linkType);

   List<DataDocument> searchDataByFulltexts(Set<String> fulltexts, Pagination pagination, List<LinkType> linkTypes);

   List<DataDocument> duplicateData(String linkTypeId, Map<String, String> linkIds);

}
