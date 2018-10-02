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
package io.lumeer.storage.mongodb.model;

import io.lumeer.api.model.Query;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.View;
import io.lumeer.storage.mongodb.model.common.MorphiaResource;
import io.lumeer.storage.mongodb.model.embedded.MorphiaQuery;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.utils.IndexType;

import java.util.Map;
import java.util.Set;

@Entity(noClassnameStored = true)
@Indexes({
      @Index(fields = { @Field(MorphiaView.CODE) }, options = @IndexOptions(unique = true)),
      @Index(fields = { @Field(MorphiaView.NAME) }, options = @IndexOptions(unique = true)),
      @Index(fields = {
            @Field(value = MorphiaView.CODE, type = IndexType.TEXT),
            @Field(value = MorphiaView.NAME, type = IndexType.TEXT)
      })
})
public class MorphiaView extends MorphiaResource implements View {

   public static final String QUERY = "query";
   public static final String PERSPECTIVE = "perspective";
   public static final String CONFIG = "config";
   public static final String AUTHOR_ID = "authorId";

   @Embedded(QUERY)
   private MorphiaQuery query;

   @Property(PERSPECTIVE)
   private String perspective;

   @Property(CONFIG)
   private Object config;

   @Property(AUTHOR_ID)
   private String authorId;

   @Transient
   private Map<String, Set<Role>> authorRights;

   public MorphiaView() {
   }

   public MorphiaView(final View view) {
      super(view);

      this.query = new MorphiaQuery(view.getQuery());
      this.perspective = view.getPerspective();
      this.config = view.getConfig();
      this.authorId = view.getAuthorId();
      this.authorRights = view.getAuthorRights();
   }

   @Override
   public Query getQuery() {
      return query;
   }

   @Override
   public String getPerspective() {
      return perspective;
   }

   @Override
   public Object getConfig() {
      return config;
   }

   @Override
   public String getAuthorId() {
      return authorId;
   }

   public void setConfig(final Object config) {
      this.config = config;
   }

   public void setQuery(final MorphiaQuery query) {
      this.query = query;
   }

   public void setPerspective(final String perspective) {
      this.perspective = perspective;
   }

   public void setAuthorId(final String authorId) {
      this.authorId = authorId;
   }

   @Override
   public Map<String, Set<Role>> getAuthorRights() {
      return authorRights;
   }

   @Override
   public void setAuthorRights(final Map<String, Set<Role>> authorRights) {
      this.authorRights = authorRights;
   }

   @Override
   public String toString() {
      return "MongoView{" +
            "id='" + getId() + '\'' +
            ", code='" + code + '\'' +
            ", name='" + name + '\'' +
            ", icon='" + icon + '\'' +
            ", color='" + color + '\'' +
            ", permissions=" + permissions +
            ", query=" + query +
            ", perspective='" + perspective + '\'' +
            ", authorId='" + authorId + '\'' +
            ", authorRights='" + authorRights + '\'' +
            '}';
   }
}
