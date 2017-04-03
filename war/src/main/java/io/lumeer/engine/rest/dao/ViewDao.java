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
import io.lumeer.engine.controller.UserFacade;

import java.util.Date;
import javax.inject.Inject;

/**
 * Describes view information.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ViewDao {

   @Inject
   private UserFacade userFacade;

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
    * View description from user
    */
   private String description;

   /**
    * Complete view configuration.
    */
   private DataDocument configuration;

   private Date createDate;
   private String createUser;
   private Date updateDate;
   private String updateUser;

   public ViewDao() {
   }

   public ViewDao(final int id, final String name, final String type, final String description, final DataDocument configuration) {
      this.id = id;
      this.name = name;
      this.type = type;
      this.description = description;
      this.configuration = configuration;
      this.createUser = userFacade.getUserEmail();
      this.createDate = new Date();
   }

   public ViewDao(DataDocument viewMetadata) {
      this.id = viewMetadata.getInteger(LumeerConst.View.ID_KEY);
      this.name = viewMetadata.getString(LumeerConst.View.NAME_KEY);
      this.type = viewMetadata.getString(LumeerConst.View.TYPE_KEY);
      this.description = viewMetadata.getString(LumeerConst.View.DESCRIPTION_KEY);
      this.configuration = viewMetadata.getDataDocument(LumeerConst.View.CONFIGURATION_KEY);
      this.createDate = viewMetadata.getDate(LumeerConst.View.CREATE_DATE_KEY);
      this.createUser = viewMetadata.getString(LumeerConst.View.CREATE_USER_KEY);
      this.updateDate = viewMetadata.getDate(LumeerConst.View.UPDATE_DATE_KEY);
      this.updateUser = viewMetadata.getString(LumeerConst.View.UPDATE_USER_KEY);
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

   public String getDescription() {
      return description;
   }

   public void setDescription(final String description) {
      this.description = description;
   }

   public Date getCreateDate() {
      return createDate;
   }

   public void setCreateDate(final Date createDate) {
      this.createDate = createDate;
   }

   public String getCreateUser() {
      return createUser;
   }

   public void setCreateUser(final String createUser) {
      this.createUser = createUser;
   }

   public Date getUpdateDate() {
      return updateDate;
   }

   public void setUpdateDate(final Date updateDate) {
      this.updateDate = updateDate;
   }

   public String getUpdateUser() {
      return updateUser;
   }

   public void setUpdateUser(final String updateUser) {
      this.updateUser = updateUser;
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
