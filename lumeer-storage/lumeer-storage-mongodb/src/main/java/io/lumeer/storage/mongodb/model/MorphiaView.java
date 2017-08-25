/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.storage.mongodb.model;

import io.lumeer.api.model.Perspective;
import io.lumeer.api.model.Query;
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
import org.mongodb.morphia.utils.IndexType;

@Entity
@Indexes({
      @Index(fields = { @Field(MorphiaView.CODE) }, options = @IndexOptions(unique = true)),
      @Index(fields = { @Field(MorphiaView.NAME) }),
      @Index(fields = {
            @Field(value = MorphiaView.CODE, type = IndexType.TEXT),
            @Field(value = MorphiaView.NAME, type = IndexType.TEXT)
      })
})
public class MorphiaView extends MorphiaResource implements View {

   public static final String QUERY = "query";
   public static final String PERSPECTIVE = "perspective";

   @Embedded(QUERY)
   private MorphiaQuery query;

   @Property(PERSPECTIVE)
   private String perspective;

   public MorphiaView() {
   }

   public MorphiaView(final View view) {
      super(view);

      this.query = new MorphiaQuery(view.getQuery());
      this.perspective = view.getPerspective().toString();
   }

   @Override
   public Query getQuery() {
      return query;
   }

   @Override
   public Perspective getPerspective() {
      return Perspective.fromString(perspective);
   }

   public void setQuery(final MorphiaQuery query) {
      this.query = query;
   }

   public void setPerspective(final String perspective) {
      this.perspective = perspective;
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
            '}';
   }
}
