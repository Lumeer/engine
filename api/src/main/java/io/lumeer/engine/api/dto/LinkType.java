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
