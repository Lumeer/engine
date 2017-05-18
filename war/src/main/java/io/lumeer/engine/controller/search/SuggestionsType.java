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
 * Specifies which part of the query is user typing
 */
public enum SuggestionsType {

   ALL,
   ATTRIBUTE,
   COLLECTION,
   LINK,
   VIEW;

   /**
    * Converts the type from the request to internal representation
    *
    * @param type
    *       suggestion type from the request
    * @return SuggestionsType instance
    */
   public static SuggestionsType getType(String type) {
      try {
         return valueOf(type.toUpperCase());
      } catch (NullPointerException | IllegalArgumentException ex) {
         return ALL;
      }
   }

}
