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

/**
 * DTO object for storing configuration
 */
@Immutable
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
