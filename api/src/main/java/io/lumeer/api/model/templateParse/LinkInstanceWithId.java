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

import io.lumeer.api.model.LinkInstance;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class LinkInstanceWithId extends LinkInstance {

   public static final String _ID = "_id";
   public static final String DATA = "data";

   @JsonCreator
   public LinkInstanceWithId(
         @JsonProperty(_ID) final String id,
         @JsonProperty(LINK_TYPE_ID) final String linkTypeId,
         @JsonProperty(DOCUMENTS_IDS) final List<String> documentIds) {
      super(linkTypeId, documentIds);
      setId(id);
   }

   public LinkInstanceWithId(final LinkInstance linkInstance) {
      super(linkInstance);
   }

   @JsonProperty(_ID)
   @Override
   public String getId() {
      return super.getId();
   }
}
