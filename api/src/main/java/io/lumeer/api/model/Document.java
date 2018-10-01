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
package io.lumeer.api.model;

import io.lumeer.engine.api.data.DataDocument;

import java.time.ZonedDateTime;

public interface Document {

   String META_PARENT_ID = "parentId";

   String getId();

   void setId(String id);

   String getCollectionId();

   void setCollectionId(String collectionId);

   ZonedDateTime getCreationDate();

   void setCreationDate(ZonedDateTime creationDate);

   ZonedDateTime getUpdateDate();

   void setUpdateDate(ZonedDateTime updateDate);

   String getCreatedBy();

   void setCreatedBy(String createdBy);

   String getUpdatedBy();

   void setUpdatedBy(String updatedBy);

   Integer getDataVersion();

   void setDataVersion(Integer dataVersion);

   DataDocument getData();

   void setData(DataDocument data);

   DataDocument getMetaData();

   void setMetaData(final DataDocument metaData);

}
