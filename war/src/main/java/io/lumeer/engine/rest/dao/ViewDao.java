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

import io.lumeer.engine.api.data.DataDocument;

/**
 * Describes view information.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ViewDao {

   /**
    * Internal view ID.
    */
   private int id;

   /**
    * User name of the view.
    */
   private String name;

   /**
    * View type name.
    */
   private String type;

   /**
    * Complete view configuration.
    */
   private DataDocument configuration;

   public ViewDao() {
   }

   public ViewDao(final int id, final String name, final String type, final DataDocument configuration) {
      this.id = id;
      this.name = name;
      this.type = type;
      this.configuration = configuration;
   }

   public int getId() {
      return id;
   }

   public void setId(final int id) {
      this.id = id;
   }

   public String getName() {
      return name;
   }

   public void setName(final String name) {
      this.name = name;
   }

   public String getType() {
      return type;
   }

   public void setType(final String type) {
      this.type = type;
   }

   public DataDocument getConfiguration() {
      return configuration;
   }

   public void setConfiguration(final DataDocument configuration) {
      this.configuration = configuration;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      final ViewDao viewDao = (ViewDao) o;

      if (id != viewDao.id) {
         return false;
      }
      if (name != null ? !name.equals(viewDao.name) : viewDao.name != null) {
         return false;
      }
      if (type != null ? !type.equals(viewDao.type) : viewDao.type != null) {
         return false;
      }
      return configuration != null ? configuration.equals(viewDao.configuration) : viewDao.configuration == null;
   }

   @Override
   public int hashCode() {
      int result = id;
      result = 31 * result + (name != null ? name.hashCode() : 0);
      result = 31 * result + (type != null ? type.hashCode() : 0);
      result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return "ViewDao{"
            + "id=" + id
            + ", name='" + name + '\''
            + ", type='" + type + '\''
            + ", configuration=" + configuration
            + '}';
   }
}
