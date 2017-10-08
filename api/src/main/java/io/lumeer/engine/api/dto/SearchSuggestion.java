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
