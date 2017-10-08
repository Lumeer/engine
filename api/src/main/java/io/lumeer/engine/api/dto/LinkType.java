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

import javax.annotation.concurrent.Immutable;

@Immutable
public class LinkType {

   private final String fromCollection;

   private final String toCollection;

   private final String role;

   public LinkType() {
      this(null, null, null);
   }

   public LinkType(final DataDocument dataDocument) {
      this.fromCollection = dataDocument.getString(LumeerConst.Linking.Type.ATTR_FROM_COLLECTION_ID);
      this.toCollection = dataDocument.getString(LumeerConst.Linking.Type.ATTR_TO_COLLECTION_ID);
      this.role = dataDocument.getString(LumeerConst.Linking.Type.ATTR_ROLE);
   }

   @JsonCreator
   public LinkType(final @JsonProperty("fromCollection") String fromCollection,
         final @JsonProperty("toCollection") String toCollection,
         final @JsonProperty("role") String role) {
      this.fromCollection = fromCollection;
      this.toCollection = toCollection;
      this.role = role;
   }

   public String getFromCollection() {
      return fromCollection;
   }

   public String getToCollection() {
      return toCollection;
   }

   public String getRole() {
      return role;
   }

}
