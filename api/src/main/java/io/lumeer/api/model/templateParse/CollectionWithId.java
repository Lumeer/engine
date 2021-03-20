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
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.CollectionPurpose;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Rule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Set;

public class CollectionWithId extends Collection {

   public static final String _ID = "_id";
   public static final String DEFAULT_ATTRIBUTE_ID = "defaultAttributeId";

   @JsonCreator
   public CollectionWithId(
         @JsonProperty(_ID) final String id,
         @JsonProperty(CODE) final String code,
         @JsonProperty(NAME) final String name,
         @JsonProperty(ICON) final String icon,
         @JsonProperty(COLOR) final String color,
         @JsonProperty(DESCRIPTION) final String description,
         @JsonProperty(PERMISSIONS) final Permissions permissions,
         @JsonProperty(ATTRIBUTES) final Set<Attribute> attributes,
         @JsonProperty(RULES) final Map<String, Rule> rules,
         @JsonProperty(DATA_DESCRIPTION) final String dataDescription,
         @JsonProperty(PURPOSE) final CollectionPurpose purpose,
         @JsonProperty(DEFAULT_ATTRIBUTE_ID) final String defaultAttributeId) {
      super(code, name, icon, color, description, permissions, attributes, rules, dataDescription, purpose);
      setId(id);
      setDefaultAttributeId(defaultAttributeId);
   }

   public CollectionWithId(final Collection collection) {
      super(
            collection.getCode(),
            collection.getName(),
            collection.getIcon(),
            collection.getColor(),
            collection.getDescription(),
            collection.getPermissions(),
            collection.getAttributes(),
            collection.getRules(),
            collection.getDataDescription(),
            collection.getPurpose());
      this.setId(collection.getId());
      this.setDefaultAttributeId(collection.getDefaultAttributeId());
   }

   @Override
   @JsonProperty(_ID)
   public String getId() {
      return super.getId();
   }
}
