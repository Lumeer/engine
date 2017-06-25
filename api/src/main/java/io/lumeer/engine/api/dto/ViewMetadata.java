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
package io.lumeer.engine.api.dto;

import io.lumeer.engine.api.LumeerConst.View;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import javax.annotation.concurrent.Immutable;

/**
 * Describes view information.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Immutable
public class ViewMetadata {

   /**
    * Internal view ID.
    */
   private final int id;

   /**
    * User name of the view.
    */
   private final String name;

   /**
    * View type name.
    */
   private final String type;

   /**
    * View description from user
    */
   private final String description;

   /**
    * Complete view configuration.
    */
   private final DataDocument configuration;

   /**
    * Date when view was created
    */
   private final Date createDate;

   /**
    * Name of user who created the view
    */
   private final String createUser;

   /**
    * Date when view was last updated
    */
   private final Date updateDate;

   /**
    * Name of user who last updated the view
    */
   private final String updateUser;

   public ViewMetadata(DataDocument viewMetadata) {
      this.id = viewMetadata.getInteger(View.ID_KEY);
      this.name = viewMetadata.getString(View.NAME_KEY);
      this.type = viewMetadata.getString(View.TYPE_KEY);
      this.description = viewMetadata.getString(View.DESCRIPTION_KEY);
      this.configuration = viewMetadata.getDataDocument(View.CONFIGURATION_KEY);
      this.createDate = viewMetadata.getDate(View.CREATE_DATE_KEY);
      this.createUser = viewMetadata.getString(View.CREATE_USER_KEY);
      this.updateDate = viewMetadata.getDate(View.UPDATE_DATE_KEY);
      this.updateUser = viewMetadata.getString(View.UPDATE_USER_KEY);
   }

   @JsonCreator
   public ViewMetadata(final @JsonProperty(View.ID_KEY) int id,
         final @JsonProperty(View.NAME_KEY) String name,
         final @JsonProperty(View.TYPE_KEY) String type,
         final @JsonProperty(View.DESCRIPTION_KEY) String description,
         final @JsonProperty(View.CONFIGURATION_KEY) DataDocument configuration,
         final @JsonProperty(View.CREATE_DATE_KEY) Date createDate,
         final @JsonProperty(View.CREATE_USER_KEY) String createUser,
         final @JsonProperty(View.UPDATE_DATE_KEY) Date updateDate,
         final @JsonProperty(View.UPDATE_USER_KEY) String updateUser) {
      this.id = id;
      this.name = name;
      this.type = type;
      this.description = description;
      this.configuration = configuration;
      this.createDate = createDate;
      this.createUser = createUser;
      this.updateDate = updateDate;
      this.updateUser = updateUser;
   }

   public int getId() {
      return id;
   }

   public String getName() {
      return name;
   }

   public String getType() {
      return type;
   }

   public String getDescription() {
      return description;
   }

   public Date getCreateDate() {
      return new Date(createDate.getTime());
   }

   public String getCreateUser() {
      return createUser;
   }

   public Date getUpdateDate() {
      return new Date(updateDate.getTime());
   }

   public String getUpdateUser() {
      return updateUser;
   }

   public DataDocument getConfiguration() {
      return new DataDocument(configuration);
   }

   public DataDocument toDataDocument() {
      return new DataDocument(View.ID_KEY, id)
            .append(View.NAME_KEY, name)
            .append(View.TYPE_KEY, name)
            .append(View.DESCRIPTION_KEY, name)
            .append(View.CREATE_DATE_KEY, name)
            .append(View.CREATE_USER_KEY, name)
            .append(View.UPDATE_DATE_KEY, name)
            .append(View.UPDATE_USER_KEY, name);
   }
}
