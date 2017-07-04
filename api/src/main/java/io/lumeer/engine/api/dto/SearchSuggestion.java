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
package io.lumeer.engine.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import javax.annotation.concurrent.Immutable;

@Immutable
public class SearchSuggestion implements Serializable {

   public static final String TYPE_ALL = "all";
   public static final String TYPE_ATTRIBUTE = "attribute";
   public static final String TYPE_COLLECTION = "collection";
   public static final String TYPE_LINK = "link";
   public static final String TYPE_VIEW = "view";

   private final String type;
   private final String text;
   private final List<String> constraints;
   private final String icon;

   public SearchSuggestion(final String type, final String text) {
      this(type, text, "");
   }

   public SearchSuggestion(final String type, final String text, final String icon) {
      this(type, text, Collections.emptyList(), icon);
   }

   @JsonCreator
   public SearchSuggestion(final @JsonProperty("type") String type, final @JsonProperty("text") String text,
         final @JsonProperty("constraints") List<String> constraints, final @JsonProperty("icon") String icon) {
      this.type = type;
      this.text = text;
      this.constraints = constraints;
      this.icon = icon;
   }

   public String getType() {
      return type;
   }

   public String getText() {
      return text;
   }

   public List<String> getConstraints() {
      return Collections.unmodifiableList(constraints);
   }

   public String getIcon() {
      return icon;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      final SearchSuggestion that = (SearchSuggestion) o;

      if (type != null ? !type.equals(that.type) : that.type != null) {
         return false;
      }
      return text != null ? text.equals(that.text) : that.text == null;
   }

   @Override
   public int hashCode() {
      int result = type != null ? type.hashCode() : 0;
      result = 31 * result + (text != null ? text.hashCode() : 0);
      return result;
   }
}
