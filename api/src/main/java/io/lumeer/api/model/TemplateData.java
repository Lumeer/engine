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
package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TemplateData {

   private TemplateMetadata metadata;
   private ProjectContent content;

   @JsonCreator
   public TemplateData(@JsonProperty("metadata") final TemplateMetadata metadata, @JsonProperty("content") final ProjectContent content) {
      this.metadata = metadata;
      this.content = content;
   }

   public ProjectContent getContent() {
      return content;
   }

   public TemplateMetadata getMetadata() {
      return metadata;
   }
}
