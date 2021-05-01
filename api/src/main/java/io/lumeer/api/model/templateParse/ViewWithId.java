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

import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.View;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ViewWithId extends View {

   public static final String _ID = "_id";

   @JsonCreator
   public ViewWithId(
         @JsonProperty(_ID) final String id,
         @JsonProperty(CODE) final String code,
         @JsonProperty(NAME) final String name,
         @JsonProperty(ICON) final String icon,
         @JsonProperty(COLOR) final String color,
         @JsonProperty(PRIORITY) final Long order,
         @JsonProperty(DESCRIPTION) final String description,
         @JsonProperty(PERMISSIONS) final Permissions permissions,
         @JsonProperty(QUERY) final Query query,
         @JsonProperty(PERSPECTIVE) final String perspective,
         @JsonProperty(CONFIG) final Object config,
         @JsonProperty(SETTINGS) final Object settings,
         @JsonProperty(AUTHOR_ID) final String authorId,
         @JsonProperty(FOLDERS) final List<String> folders) {
      super(code, name, icon, color, description, order, permissions, query, perspective, config, settings, authorId, folders);
      setId(id);
   }

   public ViewWithId(final View view) {
      super(view.getCode(), view.getName(), view.getIcon(), view.getColor(),
            view.getDescription(), view.getPriority(), view.getPermissions(), view.getQuery(), view.getPerspective(),
            view.getConfig(), view.getSettings(), view.getAuthorId(), view.getFolders());
      this.setId(view.getId());
   }

   @JsonProperty(_ID)
   @Override
   public String getId() {
      return super.getId();
   }
}
