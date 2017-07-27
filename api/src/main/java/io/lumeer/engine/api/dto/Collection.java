/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.concurrent.Immutable;

@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class Collection {

   private final String code;
   private final String name;
   private final String icon;
   private final String color;
   private final int documentCount;

   public Collection(String name) {
      this(null, name, "", "", 0);
   }

   public Collection(DataDocument document) {
      this(document.getString(LumeerConst.Collection.CODE),
            document.getString(LumeerConst.Collection.REAL_NAME),
            document.getString(LumeerConst.Collection.ICON),
            document.getString(LumeerConst.Collection.COLOR),
            document.getInteger(LumeerConst.Collection.DOCUMENT_COUNT));
   }

   @JsonCreator
   public Collection(final @JsonProperty("code") String code,
         final @JsonProperty("name") String name,
         final @JsonProperty("icon") String icon,
         final @JsonProperty("color") String color,
         final @JsonProperty("documentCount") int documentCount) {
      this.code = code;
      this.name = name;
      this.icon = icon;
      this.color = color;
      this.documentCount = documentCount;
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

   public DataDocument toDataDocument() {
      return new DataDocument(LumeerConst.Collection.CODE, code)
            .append(LumeerConst.Collection.REAL_NAME, name)
            .append(LumeerConst.Collection.COLOR, color)
            .append(LumeerConst.Collection.ICON, icon)
            .append(LumeerConst.Collection.DOCUMENT_COUNT, documentCount);
   }

}
