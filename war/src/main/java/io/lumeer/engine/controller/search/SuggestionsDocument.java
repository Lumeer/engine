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
package io.lumeer.engine.controller.search;

/**
 * Describes the structure of returned document when getting search suggestions.
 */
public class SuggestionsDocument {

   public static final String ATTRIBUTES = "attributes";
   public static final String ATTRIBUTES_COLLECTION = "collection";
   public static final String ATTRIBUTES_NAME = "name";
   public static final String ATTRIBUTES_CONSTRAINTS = "constraints";

   public static final String COLLECTIONS = "collections";
   public static final String COLLECTIONS_NAME = "name";
   public static final String COLLECTIONS_ICON = "icon";

   public static final String LINKS = "links";
   public static final String LINKS_FROM = "from";
   public static final String LINKS_TO = "to";
   public static final String LINKS_ROLE = "role";

   public static final String VIEWS = "views";
   public static final String VIEWS_NAME = "name";
   public static final String VIEWS_ICON = "icon";

   private SuggestionsDocument() {

   }

}
