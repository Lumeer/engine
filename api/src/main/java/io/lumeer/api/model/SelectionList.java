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

import io.lumeer.api.exception.InsaneObjectException;
import io.lumeer.api.model.common.WithId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public class SelectionList implements WithId, HealthChecking {

   public static final String ID = "id";
   public static final String NAME = "name";
   public static final String DESCRIPTION = "description";
   public static final String ORGANIZATION_ID = "organizationId";
   public static final String PROJECT_ID = "projectId";
   public static final String DISPLAY_VALUES = "displayValues";
   public static final String OPTIONS = "options";

   private String id;
   private String name;
   private String description;
   private String organizationId;
   private String projectId;
   private Boolean displayValues;
   private List<SelectOption> options;

   @JsonCreator
   public SelectionList(@JsonProperty(ID) final String id,
         @JsonProperty(NAME) final String name,
         @JsonProperty(DESCRIPTION) final String description,
         @JsonProperty(ORGANIZATION_ID) final String organizationId,
         @JsonProperty(PROJECT_ID) final String projectId,
         @JsonProperty(DISPLAY_VALUES) final Boolean displayValues,
         @JsonProperty(OPTIONS) final List<SelectOption> options) {
      this.id = id;
      this.name = name;
      this.description = description;
      this.organizationId = organizationId;
      this.projectId = projectId;
      this.displayValues = displayValues;
      this.options = options;
   }

   public String getId() {
      return id;
   }

   public String getName() {
      return name;
   }

   public String getDescription() {
      return description;
   }

   public String getOrganizationId() {
      return organizationId;
   }

   public String getProjectId() {
      return projectId;
   }

   public Boolean getDisplayValues() {
      return displayValues;
   }

   public List<SelectOption> getOptions() {
      return options;
   }

   public void setOrganizationId(final String organizationId) {
      this.organizationId = organizationId;
   }

   public void setId(final String id) {
      this.id = id;
   }

   public void setName(final String name) {
      this.name = name;
   }

   public void setDescription(final String description) {
      this.description = description;
   }

   public void setProjectId(final String projectId) {
      this.projectId = projectId;
   }

   public void setDisplayValues(final Boolean displayValues) {
      this.displayValues = displayValues;
   }

   public void setOptions(final List<SelectOption> options) {
      this.options = options;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof SelectionList)) {
         return false;
      }
      final SelectionList that = (SelectionList) o;
      return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(organizationId, that.organizationId) && Objects.equals(projectId, that.projectId) && Objects.equals(displayValues, that.displayValues) && Objects.equals(options, that.options);
   }

   @Override
   public String toString() {
      return "SelectionList{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", organizationId='" + organizationId + '\'' +
            ", projectId='" + projectId + '\'' +
            ", displayValues=" + displayValues +
            ", options=" + options +
            '}';
   }

   @Override
   public int hashCode() {
      return Objects.hash(id, name, description, organizationId, projectId, displayValues, options);
   }

   @Override
   public void checkHealth() throws InsaneObjectException {
      checkStringLength("name", name, MAX_STRING_LENGTH);
      checkStringLength("description", description, MAX_LONG_STRING_LENGTH);
   }

   public void patch(SelectionList list) {
      setName(list.getName());
      setDescription(list.getDescription());
      setDisplayValues(list.getDisplayValues());
      setOptions(list.getOptions());
      setProjectId(list.getProjectId());
   }
}
