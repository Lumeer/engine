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
package io.lumeer.engine.rest.dao;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="alica.kacengova@gmail.com">Alica Kačengová</a>
 */
public class CollectionMetadata {

   private String name;
   private String internalName;
   private String projectId;
   private Map<String, Attribute> attributes = new HashMap<>();
   private Date lastTimeUsed;
   private List<String> recentlyUsedDocumentIds = new ArrayList<>();
   private DataDocument customMetadata = new DataDocument();
   private String creator;
   private Date createDate;

   public CollectionMetadata() {
   }

   public CollectionMetadata(final DataDocument metadata) {
      name = metadata.getString(LumeerConst.Collection.REAL_NAME_KEY);
      internalName = metadata.getString(LumeerConst.Collection.INTERNAL_NAME_KEY);
      projectId = metadata.getString(LumeerConst.Collection.PROJECT_ID_KEY);

      DataDocument attributesDocument = metadata.getDataDocument(LumeerConst.Collection.ATTRIBUTES_KEY);
      for (String attributeName : attributesDocument.keySet()) {
         attributes.put(
               attributeName,
               new Attribute(
                     attributesDocument
                           .getDataDocument(attributeName)
                           .append(LumeerConst.Collection.ATTRIBUTE_NAME_KEY, attributeName)));
      }

      lastTimeUsed = metadata.getDate(LumeerConst.Collection.LAST_TIME_USED_KEY);
      recentlyUsedDocumentIds = metadata.getArrayList(LumeerConst.Collection.RECENTLY_USED_DOCUMENTS_KEY, String.class);
      customMetadata = metadata.getDataDocument(LumeerConst.Collection.CUSTOM_META_KEY);
      creator = metadata.getString(LumeerConst.Collection.CREATE_USER_KEY);
      createDate = metadata.getDate(LumeerConst.Collection.CREATE_DATE_KEY);
   }

   public String getName() {
      return name;
   }

   public String getInternalName() {
      return internalName;
   }

   public String getProjectId() {
      return projectId;
   }

   public Map<String, Attribute> getAttributes() {
      return attributes;
   }

   public Date getLastTimeUsed() {
      return lastTimeUsed;
   }

   public List<String> getRecentlyUsedDocumentIds() {
      return recentlyUsedDocumentIds;
   }

   public DataDocument getCustomMetadata() {
      return customMetadata;
   }

   public String getCreator() {
      return creator;
   }

   public Date getCreateDate() {
      return createDate;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      final CollectionMetadata that = (CollectionMetadata) o;

      if (name != null ? !name.equals(that.name) : that.name != null) {
         return false;
      }
      return internalName != null ? internalName.equals(that.internalName) : that.internalName == null;

   }

   @Override
   public int hashCode() {
      int result = name != null ? name.hashCode() : 0;
      result = 31 * result + (internalName != null ? internalName.hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return "CollectionMetadata{"
            + "projectId=" + projectId
            + ", name='" + name + '\''
            + ", internalName='" + internalName + '\''
            + ", lastTimeUsed=" + lastTimeUsed + '\''
            + ", creator='" + creator + '\''
            + ", createDate=" + createDate
            + ", attributes=" + attributes
            + ", recentlyUsedDocumentIds=" + recentlyUsedDocumentIds
            + ", customMetadata=" + customMetadata
            + '}';
   }
}
