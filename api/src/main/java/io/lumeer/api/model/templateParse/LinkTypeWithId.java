/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
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
package io.lumeer.api.model.templateParse;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Rule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;
import java.util.Map;

public class LinkTypeWithId extends LinkType {

   public static final String _ID = "_id";
   public static final String LAST_ATTRIBUTE_NUM = "lastAttributeNum";
   public static final String VERSION = "version";

   @JsonCreator
   public LinkTypeWithId(
         @JsonProperty(_ID) final String id,
         @JsonProperty(NAME) final String name,
         @JsonProperty(VERSION) final long version,
         @JsonProperty(COLLECTION_IDS) final List<String> collectionIds,
         @JsonProperty(ATTRIBUTES) final List<Attribute> attributes,
         @JsonProperty(LAST_ATTRIBUTE_NUM) final Integer lastAttributeNum,
         @JsonProperty(RULES) final Map<String, Rule> rules) {
      super(name, collectionIds, attributes, rules);
      setId(id);
      setVersion(version);
      setLastAttributeNum(lastAttributeNum);
   }

   public LinkTypeWithId(final LinkType linkType) {
      super(linkType);
   }

   @JsonProperty(_ID)
   @Override
   public String getId() {
      return super.getId();
   }
}
