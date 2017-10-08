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

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.concurrent.Immutable;

@Immutable
public class Collection {

   private final String code;
   private final String name;
   private final String icon;
   private final String color;
   private final int documentCount;
   private final List<String> userRoles;

   public Collection(String name) {
      this(null, name, "", "", 0, Collections.emptyList());
   }

   /**
    * Constructor used when inserting a document to a collection that does not exist
    *
    * @param code
    * @param name
    */
   public Collection(String code, String name) {
      this(code, name, "", "", 0, Collections.emptyList());
   }

   public Collection(DataDocument document, String user, List<String> userGroups) {
      this(document.getString(LumeerConst.Collection.CODE),
            document.getString(LumeerConst.Collection.REAL_NAME),
            document.getString(LumeerConst.Collection.ICON),
            document.getString(LumeerConst.Collection.COLOR),
            document.getInteger(LumeerConst.Collection.DOCUMENT_COUNT),
            new ArrayList<>());

      Set<String> roles = new HashSet<>();
      List<DataDocument> usersList = document.getDataDocument(LumeerConst.Security.PERMISSIONS_KEY).getArrayList(LumeerConst.Security.USERS_KEY, DataDocument.class);
      List<DataDocument> groupsList = document.getDataDocument(LumeerConst.Security.PERMISSIONS_KEY).getArrayList(LumeerConst.Security.GROUP_KEY, DataDocument.class);
      usersList.stream().filter(u -> u.getString(LumeerConst.Security.USERGROUP_NAME_KEY).equals(user)).forEach(d -> roles.addAll(d.getArrayList(LumeerConst.Security.USERGROUP_ROLES_KEY, String.class)));
      groupsList.stream().filter(u -> userGroups.contains(u.getString(LumeerConst.Security.USERGROUP_NAME_KEY))).forEach(d -> roles.addAll(d.getArrayList(LumeerConst.Security.USERGROUP_ROLES_KEY, String.class)));
      this.userRoles.addAll(roles);
   }

   @JsonCreator
   public Collection(final @JsonProperty("code") String code,
         final @JsonProperty("name") String name,
         final @JsonProperty("icon") String icon,
         final @JsonProperty("color") String color,
         final @JsonProperty("documentCount") int documentCount,
         final @JsonProperty("userRoles") List<String> userRoles) {
      this.code = code;
      this.name = name;
      this.icon = icon;
      this.color = color;
      this.documentCount = documentCount;
      this.userRoles = userRoles;
   }

   public String getCode() {
      return code;
   }

   public String getName() {
      return name;
   }

   public String getIcon() {
      return icon;
   }

   public String getColor() {
      return color;
   }

   public int getDocumentCount() {
      return documentCount;
   }

   public List<String> getUserRoles() {
      return userRoles != null ? Collections.unmodifiableList(userRoles) : Collections.emptyList();
   }

   public DataDocument toDataDocument() {
      return new DataDocument(LumeerConst.Collection.CODE, code)
            .append(LumeerConst.Collection.REAL_NAME, name)
            .append(LumeerConst.Collection.COLOR, color)
            .append(LumeerConst.Collection.ICON, icon)
            .append(LumeerConst.Collection.DOCUMENT_COUNT, documentCount);
   }

}
