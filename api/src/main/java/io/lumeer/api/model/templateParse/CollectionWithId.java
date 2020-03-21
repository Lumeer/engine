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

import io.lumeer.api.model.Collection;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CollectionWithId extends Collection {

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
            collection.getDataDescription());
      this.setId(collection.getId());
   }

   @Override
   @JsonProperty("_id")
   public String getId() {
      return super.getId();
   }
}
