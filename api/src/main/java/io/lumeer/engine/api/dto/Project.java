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
 * DTO object for Project
 */
@Immutable
public class Project {

   private final String name;
   private final String code;
   private final String icon;
   private final String color;

   public Project(final DataDocument dataDocument) {
      this(dataDocument.getString(LumeerConst.Project.ATTR_PROJECT_CODE),
            dataDocument.getString(LumeerConst.Project.ATTR_PROJECT_NAME),
            dataDocument.getString(LumeerConst.Project.ATTR_META_ICON),
            dataDocument.getString(LumeerConst.Project.ATTR_META_COLOR));
   }

   public Project(final String code, final String name) {
      this(code, name, null, null);
   }

   @JsonCreator
   public Project(final @JsonProperty("code") String code,
         final @JsonProperty("name") String name,
         final @JsonProperty("icon") String icon,
         final @JsonProperty("color") String color) {
      this.code = code;
      this.name = name;
      this.icon = icon;
      this.color = color;
   }

   public String getName() {
      return name;
   }

   public String getCode() {
      return code;
   }

   public String getIcon() {
      return icon;
   }

   public String getColor() {
      return color;
   }

   public DataDocument toDataDocument() {
      return new DataDocument(LumeerConst.Project.ATTR_PROJECT_CODE, code)
            .append(LumeerConst.Project.ATTR_PROJECT_NAME, name)
            .append(LumeerConst.Project.ATTR_META_COLOR, color)
            .append(LumeerConst.Project.ATTR_META_ICON, icon);
   }
}
