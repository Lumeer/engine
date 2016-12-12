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

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class LinkDao extends LinkTypeDao {

   /**
    * Id of the source document.
    */
   private String fromId;

   /**
    * Id of the target document.
    */
   private String toId;

   public LinkDao() {
   }

   public LinkDao(final String fromCollection, final String toCollection, final String role, final String fromId, final String toId) {
      super(fromCollection, toCollection, role);
      this.fromId = fromId;
      this.toId = toId;
   }

   public String getFromId() {
      return fromId;
   }

   public void setFromId(final String fromId) {
      this.fromId = fromId;
   }

   public String getToId() {
      return toId;
   }

   public void setToId(final String toId) {
      this.toId = toId;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      if (!super.equals(o)) {
         return false;
      }

      final LinkDao linkDao = (LinkDao) o;

      if (fromId != null ? !fromId.equals(linkDao.fromId) : linkDao.fromId != null) {
         return false;
      }
      return toId != null ? toId.equals(linkDao.toId) : linkDao.toId == null;
   }

   @Override
   public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + (fromId != null ? fromId.hashCode() : 0);
      result = 31 * result + (toId != null ? toId.hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return "LinkDao{"
            + "fromId='" + fromId + '\''
            + ", toId='" + toId + '\''
            + '}';
   }
}
