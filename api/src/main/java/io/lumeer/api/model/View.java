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

import io.lumeer.api.adapter.ZonedDateTimeAdapter;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.util.RoleUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class View extends Resource implements Updatable<View> {

   public static Set<Role> ROLES = RoleUtils.viewResourceRoles();

   public static final String QUERY = "query";
   public static final String ADDITIONAL_QUERIES = "additionalQueries";
   public static final String PERSPECTIVE = "perspective";
   public static final String CONFIG = "config";
   public static final String SETTINGS = "settings";
   public static final String AUTHOR_ID = "authorId";
   public static final String FOLDERS = "folders";

   private Query query;
   private List<Query> additionalQueries;
   private Perspective perspective;
   private Object config;
   private Object settings;
   private String authorId;
   private Map<String, Set<RoleType>> authorCollectionsRights;
   private Map<String, Set<RoleType>> authorLinkTypesRights;
   private List<String> folders;

   @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
   private ZonedDateTime lastTimeUsed;

   private boolean favorite;

   public View() {
   }

   @JsonCreator
   public View(@JsonProperty(CODE) final String code,
         @JsonProperty(NAME) final String name,
         @JsonProperty(ICON) final String icon,
         @JsonProperty(COLOR) final String color,
         @JsonProperty(DESCRIPTION) final String description,
         @JsonProperty(PRIORITY) final Long order,
         @JsonProperty(PERMISSIONS) final Permissions permissions,
         @JsonProperty(QUERY) final Query query,
         @JsonProperty(ADDITIONAL_QUERIES) final List<Query> additionalQueries,
         @JsonProperty(PERSPECTIVE) final Perspective perspective,
         @JsonProperty(CONFIG) final Object config,
         @JsonProperty(SETTINGS) final Object settings,
         @JsonProperty(AUTHOR_ID) final String authorId,
         @JsonProperty(FOLDERS) final List<String> folders) {
      super(code, name, icon, color, description, order, permissions);

      this.query = query;
      this.additionalQueries = additionalQueries;
      this.perspective = perspective;
      this.config = config;
      this.settings = settings;
      this.authorId = authorId;
      this.folders = folders;
   }

   public View(final Resource resource,
         final Query query,
         final List<Query> additionalQueries,
         final Perspective perspective,
         final Object config,
         final Object settings,
         final String authorId,
         final List<String> folders) {
      super(resource);

      this.query = query;
      this.additionalQueries = additionalQueries;
      this.perspective = perspective;
      this.config = config;
      this.settings = settings;
      this.authorId = authorId;
      this.folders = folders;
   }

   @Override
   public View copy() {
      final View o = new View();

      o.id = this.id;
      o.code = this.code;
      o.name = this.name;
      o.icon = this.icon;
      o.color = this.color;
      o.description = this.description;
      o.nonRemovable = this.nonRemovable;
      o.permissions = new Permissions(this.getPermissions());
      o.query = this.query;
      o.additionalQueries = this.additionalQueries;
      o.perspective = this.perspective;
      o.config = this.config;
      o.settings = this.settings;
      o.authorId = this.authorId;
      o.authorCollectionsRights = this.authorCollectionsRights;
      o.authorLinkTypesRights = this.authorLinkTypesRights;
      o.version = this.version;
      o.lastTimeUsed = this.lastTimeUsed;
      o.favorite = this.favorite;
      o.priority = this.priority;
      o.folders = this.folders;

      return o;
   }

   public ResourceType getType() {
      return ResourceType.VIEW;
   }

   public Query getQuery() {
      return query;
   }

   public List<Query> getAdditionalQueries() {
      return additionalQueries != null ? additionalQueries : Collections.emptyList();
   }

   @JsonIgnore
   public Set<String> getAllLinkTypeIds() {
      Set<String> linkTypeIds = new HashSet<>(getQuery() != null ? getQuery().getLinkTypeIds() : Collections.emptyList());
      linkTypeIds.addAll(getAdditionalLinkTypeIds());
      return linkTypeIds;
   }

   @JsonIgnore
   public Set<String> getAdditionalLinkTypeIds() {
      Set<String> linkTypeIds = new HashSet<>();
      getAdditionalQueries().forEach(query -> linkTypeIds.addAll(query.getLinkTypeIds()));
      return linkTypeIds;
   }

   public Perspective getPerspective() {
      return perspective;
   }

   public Object getConfig() {
      return config;
   }

   public String getAuthorId() {
      return authorId;
   }

   public void setQuery(final Query query) {
      this.query = query;
   }

   public void setPerspective(final Perspective perspective) {
      this.perspective = perspective;
   }

   public void setConfig(final Object config) {
      this.config = config;
   }

   public void setAuthorId(final String authorId) {
      this.authorId = authorId;
   }

   public void setAdditionalQueries(final List<Query> additionalQueries) {
      this.additionalQueries = additionalQueries;
   }

   public Map<String, Set<RoleType>> getAuthorCollectionsRights() {
      return authorCollectionsRights;
   }

   public void setAuthorCollectionsRights(final Map<String, Set<RoleType>> authorCollectionsRights) {
      this.authorCollectionsRights = authorCollectionsRights;
   }

   public Map<String, Set<RoleType>> getAuthorLinkTypesRights() {
      return authorLinkTypesRights;
   }

   public void setAuthorLinkTypesRights(final Map<String, Set<RoleType>> authorLinkTypesRights) {
      this.authorLinkTypesRights = authorLinkTypesRights;
   }

   public ZonedDateTime getLastTimeUsed() {
      return lastTimeUsed;
   }

   public void setLastTimeUsed(final ZonedDateTime lastTimeUsed) {
      this.lastTimeUsed = lastTimeUsed;
   }

   public boolean isFavorite() {
      return favorite;
   }

   public void setFavorite(final boolean favorite) {
      this.favorite = favorite;
   }

   public Object getSettings() {
      return settings;
   }

   public void setSettings(final Object settings) {
      this.settings = settings;
   }

   public List<String> getFolders() {
      return folders;
   }

   public void setFolders(final List<String> folders) {
      this.folders = folders;
   }

   @Override
   public String toString() {
      return "View{" +
            "id='" + id + '\'' +
            ", code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", icon='" + icon + '\'' +
            ", color='" + color + '\'' +
            ", order='" + priority + '\'' +
            ", permissions=" + permissions +
            ", perspective='" + perspective + '\'' +
            ", config=" + config + '\'' +
            ", settings=" + settings + '\'' +
            ", authorId='" + authorId + '\'' +
            ", authorCollectionsRights=" + authorCollectionsRights +
            ", authorLinkTypesRights=" + authorLinkTypesRights +
            ", lastTimeUsed=" + lastTimeUsed +
            ", query=" + query + '\'' +
            ", folders=" + folders + '\'' +
            '}';
   }

   @Override
   public void patch(final View resource, final Set<RoleType> roles) {
      patchResource(this, resource, roles);

      if (roles.contains(RoleType.Manage)) {
         setFolders(resource.getFolders());
      }
      if (roles.contains(RoleType.QueryConfig)) {
         setQuery(resource.getQuery());
      }
      if (roles.contains(RoleType.PerspectiveConfig)) {
         setConfig(resource.getConfig());
         setPerspective(resource.getPerspective());
         setSettings(resource.getSettings());
         setAdditionalQueries(resource.getAdditionalQueries());
      }
   }
}
