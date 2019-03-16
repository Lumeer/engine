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
package io.lumeer.api.model;

import java.util.Collections;
import java.util.List;

public class Suggestions {

   private final List<Collection> attributes;
   private final List<Collection> collections;
   private final List<View> views;
   private final List<LinkType> linkTypes;
   private final List<LinkType> linkAttributes;

   public Suggestions(final List<Collection> attributes, final List<Collection> collections, final List<View> views, final List<LinkType> linkTypes, final List<LinkType> linkAttributes) {
      this.attributes = attributes;
      this.collections = collections;
      this.views = views;
      this.linkTypes = linkTypes;
      this.linkAttributes = linkAttributes;
   }

   public List<Collection> getAttributes() {
      return Collections.unmodifiableList(attributes);
   }

   public List<Collection> getCollections() {
      return Collections.unmodifiableList(collections);
   }

   public List<View> getViews() {
      return Collections.unmodifiableList(views);
   }

   public List<LinkType> getLinkTypes() {
      return Collections.unmodifiableList(linkTypes);
   }

   public List<LinkType> getLinkAttributes() {
      return Collections.unmodifiableList(linkAttributes);
   }

   public static Suggestions emptySuggestions() {
      return new Suggestions(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
   }

   public static Suggestions attributeSuggestions(List<Collection> attributes) {
      return new Suggestions(attributes, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
   }

   public static Suggestions collectionSuggestions(List<Collection> collections) {
      return new Suggestions(Collections.emptyList(), collections, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
   }

   public static Suggestions viewSuggestions(List<View> views) {
      return new Suggestions(Collections.emptyList(), Collections.emptyList(), views, Collections.emptyList(), Collections.emptyList());
   }

   public static Suggestions linkSuggestions(List<LinkType> linkTypes)  {
      return new Suggestions(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), linkTypes, Collections.emptyList());
   }

   public static Suggestions linkAttributesSuggestions(List<LinkType> linkAttributes)  {
      return new Suggestions(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), linkAttributes);
   }
}
