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
