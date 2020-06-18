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

import java.util.Collections;
import java.util.List;

public class TemplateMetadata {

   private final String imageUrl;
   private final String defaultView;
   private final List<String> tags;
   private final String allowedDomains;
   private final Long relativeDate;
   private final boolean showTopPanel;
   private final boolean isEditable;
   private String organizationId;

   @JsonCreator
   public TemplateMetadata(@JsonProperty("imageUrl") final String imageUrl,
         @JsonProperty("showTopPanel") final boolean showTopPanel,
         @JsonProperty("editable") final boolean isEditable,
         @JsonProperty("tags") final List<String> tags,
         @JsonProperty("relativeDate") final Long relativeDate,
         @JsonProperty("defaultView") final String defaultView,
         @JsonProperty("allowedDomains") final String allowedDomains) {
      this.imageUrl = imageUrl;
      this.isEditable = isEditable;
      this.tags = tags;
      this.relativeDate = relativeDate;
      this.showTopPanel = showTopPanel;
      this.defaultView = defaultView;
      this.allowedDomains = allowedDomains;
   }

   public TemplateMetadata() {
      this.imageUrl = null;
      this.defaultView = null;
      this.tags = Collections.emptyList();
      this.allowedDomains = null;
      this.relativeDate = null;
      this.showTopPanel = false;
      this.isEditable = false;
   }

   public TemplateMetadata(TemplateMetadata metadata) {
      this.imageUrl = metadata.getImageUrl();
      this.isEditable = metadata.isEditable();
      this.tags = metadata.getTags();
      this.relativeDate = metadata.getRelativeDate();
      this.showTopPanel = metadata.isShowTopPanel();
      this.defaultView = metadata.getDefaultView();
      this.allowedDomains = metadata.getAllowedDomains();
   }

   public String getImageUrl() {
      return imageUrl;
   }

   public String getDefaultView() {
      return defaultView;
   }

   public String getAllowedDomains() {
      return allowedDomains;
   }

   public List<String> getTags() {
      return tags;
   }

   public Long getRelativeDate() {
      return relativeDate;
   }

   public boolean isShowTopPanel() {
      return showTopPanel;
   }

   public boolean isEditable() {
      return isEditable;
   }

   public String getOrganizationId() {
      return organizationId;
   }

   public void setOrganizationId(final String organizationId) {
      this.organizationId = organizationId;
   }
}
