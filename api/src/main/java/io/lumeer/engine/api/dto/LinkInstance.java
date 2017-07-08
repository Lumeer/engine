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
