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
package io.lumeer.engine.rest.dao;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class LinkType {

   /**
    * From which collection is the link type.
    */
   private String fromCollection;

   /**
    * To which collection it leads.
    */
   private String toCollection;

   /**
    * What is the role of this link.
    */
   private String role;

   public LinkType() {
   }

   public LinkType(final DataDocument dataDocument){
      this.fromCollection = dataDocument.getString(LumeerConst.Linking.Type.ATTR_FROM_COLLECTION);
      this.toCollection = dataDocument.getString(LumeerConst.Linking.Type.ATTR_TO_COLLECTION);
      this.role = dataDocument.getString(LumeerConst.Linking.Type.ATTR_ROLE);
   }

   public LinkType(final String fromCollection, final String toCollection, final String role) {
      this.fromCollection = fromCollection;
      this.toCollection = toCollection;
      this.role = role;
   }

   public String getFromCollection() {
      return fromCollection;
   }

   public void setFromCollection(final String fromCollection) {
      this.fromCollection = fromCollection;
   }

   public String getToCollection() {
      return toCollection;
   }

   public void setToCollection(final String toCollection) {
      this.toCollection = toCollection;
   }

   public String getRole() {
      return role;
   }

   public void setRole(final String role) {
      this.role = role;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      final LinkType that = (LinkType) o;

      if (fromCollection != null ? !fromCollection.equals(that.fromCollection) : that.fromCollection != null) {
         return false;
      }
      if (toCollection != null ? !toCollection.equals(that.toCollection) : that.toCollection != null) {
         return false;
      }
      return role != null ? role.equals(that.role) : that.role == null;
   }

   @Override
   public int hashCode() {
      int result = fromCollection != null ? fromCollection.hashCode() : 0;
      result = 31 * result + (toCollection != null ? toCollection.hashCode() : 0);
      result = 31 * result + (role != null ? role.hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return "LinkType{"
            + "fromCollection='" + fromCollection + '\''
            + ", toCollection='" + toCollection + '\''
            + ", role='" + role + '\''
            + '}';
   }
}
