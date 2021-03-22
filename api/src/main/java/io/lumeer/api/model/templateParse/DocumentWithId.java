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

import io.lumeer.api.model.Document;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DocumentWithId extends Document {

   public static final String _ID = "_id";

   public DocumentWithId(final Document document) {
      super(document);
   }

   @JsonCreator
   public DocumentWithId(@JsonProperty(_ID) final String id, @JsonProperty(DATA) final DataDocument data) {
      super(data);
      setId(id);
   }

   @JsonProperty(_ID)
   @Override
   public String getId() {
      return super.getId();
   }
}
