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
package io.lumeer.api.model.template;

import io.lumeer.api.model.View;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ViewWithId extends View {

   public ViewWithId(final View view) {
      super(view.getCode(), view.getName(), view.getIcon(), view.getColor(),
            view.getDescription(), view.getPermissions(), view.getQuery(), view.getPerspective(),
            view.getConfig(), view.getAuthorId());
      this.setId(view.getId());
   }

   @JsonProperty("_id")
   @Override
   public String getId() {
      return super.getId();
   }
}
