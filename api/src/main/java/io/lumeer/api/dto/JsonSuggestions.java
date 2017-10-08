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
package io.lumeer.api.dto;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.View;

import java.util.Collections;
import java.util.List;

public class JsonSuggestions {

   private final List<JsonCollection> attributes;
   private final List<JsonCollection> collections;
   private final List<JsonView> views;

   public JsonSuggestions(final List<Collection> attributes, final List<Collection> collections, final List<View> views) {
      this.attributes = JsonCollection.convert(attributes);
      this.collections = JsonCollection.convert(collections);
      this.views = JsonView.convert(views);
   }

   public List<JsonCollection> getAttributes() {
      return Collections.unmodifiableList(attributes);
   }

   public List<JsonCollection> getCollections() {
      return Collections.unmodifiableList(collections);
   }

   public List<JsonView> getViews() {
      return Collections.unmodifiableList(views);
   }

   public static JsonSuggestions emptySuggestions() {
      return new JsonSuggestions(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
   }

   public static JsonSuggestions attributeSuggestions(List<Collection> attributes) {
      return new JsonSuggestions(attributes, Collections.emptyList(), Collections.emptyList());
   }

   public static JsonSuggestions collectionSuggestions(List<Collection> collections) {
      return new JsonSuggestions(Collections.emptyList(), collections, Collections.emptyList());
   }

   public static JsonSuggestions viewSuggestions(List<View> views) {
      return new JsonSuggestions(Collections.emptyList(), Collections.emptyList(), views);
   }
}
