/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
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
