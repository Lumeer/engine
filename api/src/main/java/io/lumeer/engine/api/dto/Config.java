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

/**
 * DTO object for storing configuration
 */
@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class Config {

   private final String key;
   private final Object value;
   private final String description;
   private final boolean restricted;

   public Config(final DataDocument dataDocument) {
      this(dataDocument.getString(LumeerConst.Configuration.CONFIGS_CONFIG_KEY),
            dataDocument.get(LumeerConst.Configuration.CONFIGS_CONFIG_VALUE),
            dataDocument.getString(LumeerConst.Configuration.CONFIGS_CONFIG_DESCRIPTION),
            dataDocument.getBoolean(LumeerConst.Configuration.CONFIGS_CONFIG_FLAG_RESTRICTED));
   }

   public Config(final String key, final Object value) {
      this(key, value, null, false);
   }

   @JsonCreator
   public Config(final @JsonProperty("key") String key,
         final @JsonProperty("value") Object value,
         final @JsonProperty("description") String description,
         final @JsonProperty("restricted") boolean restricted) {
      this.key = key;
      this.value = value;
      this.description = description;
      this.restricted = restricted;
   }

   public String getKey() {
      return key;
   }

   public Object getValue() {
      return value;
   }

   public String getDescription() {
      return description;
   }

   public boolean isRestricted() {
      return restricted;
   }

   public DataDocument toDataDocument() {
      return new DataDocument(LumeerConst.Configuration.CONFIGS_CONFIG_KEY, key)
            .append(LumeerConst.Configuration.CONFIGS_CONFIG_VALUE, value)
            .append(LumeerConst.Configuration.CONFIGS_CONFIG_DESCRIPTION, description)
            .append(LumeerConst.Configuration.CONFIGS_CONFIG_FLAG_RESTRICTED, restricted);
   }
}
