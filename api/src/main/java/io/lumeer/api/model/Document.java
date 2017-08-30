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
package io.lumeer.api.model;

import io.lumeer.engine.api.data.DataDocument;

import java.time.LocalDateTime;

public interface Document {

   String getId();

   void setId(String id);

   String getCollectionId();

   void setCollectionId(String collectionId);

   String getCollectionCode();

   void setCollectionCode(String collectionCode);

   LocalDateTime getCreationDate();

   void setCreationDate(LocalDateTime creationDate);

   LocalDateTime getUpdateDate();

   void setUpdateDate(LocalDateTime updateDate);

   String getCreatedBy();

   void setCreatedBy(String createdBy);

   String getUpdatedBy();

   void setUpdatedBy(String updatedBy);

   Integer getDataVersion();

   void setDataVersion(Integer dataVersion);

   DataDocument getData();

   void setData(DataDocument data);

}
