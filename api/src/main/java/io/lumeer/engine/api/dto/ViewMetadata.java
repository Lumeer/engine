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
      return createDate != null ? new Date(createDate.getTime()) : null;
   }

   public String getCreateUser() {
      return createUser;
   }

   public Date getUpdateDate() {
      return updateDate != null ? new Date(updateDate.getTime()) : null;
   }

   public String getUpdateUser() {
      return updateUser;
   }

   public DataDocument getConfiguration() {
      return configuration != null ? new DataDocument(configuration) : null;
   }

   public DataDocument toDataDocument() {
      return new DataDocument(View.ID_KEY, id)
            .append(View.NAME_KEY, name)
            .append(View.TYPE_KEY, type)
            .append(View.DESCRIPTION_KEY, description)
            .append(View.CREATE_DATE_KEY, createDate)
            .append(View.CREATE_USER_KEY, createUser)
            .append(View.UPDATE_DATE_KEY, updateDate)
            .append(View.UPDATE_USER_KEY, updateUser);
   }
}
