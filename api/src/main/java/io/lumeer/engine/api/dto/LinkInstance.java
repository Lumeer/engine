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

import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.concurrent.Immutable;

@Immutable
public class LinkInstance extends LinkType {

   private final String fromId;

   private final String toId;

   private final DataDocument attributes;

   public LinkInstance() {
      this(null, null, null, null, null, new DataDocument());
   }

   public LinkInstance(final LinkType linkType, final String fromId, final String toId, final DataDocument attributes) {
      super(linkType.getFromCollection(), linkType.getToCollection(), linkType.getRole());
      this.fromId = fromId;
      this.toId = toId;
      this.attributes = attributes;
   }

   @JsonCreator
   public LinkInstance(final @JsonProperty("fromCollection") String fromCollection,
         final @JsonProperty("toCollection") String toCollection,
         final @JsonProperty("role") String role,
         final @JsonProperty("fromId") String fromId,
         final @JsonProperty("toId") String toId,
         final @JsonProperty("attributes") DataDocument attributes) {
      super(fromCollection, toCollection, role);
      this.fromId = fromId;
      this.toId = toId;
      this.attributes = attributes;
   }

   public String getFromId() {
      return fromId;
   }

   public String getToId() {
      return toId;
   }

   public DataDocument getAttributes() {
      return attributes != null ? new DataDocument(attributes) : null;
   }

}
