/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public class AttributeFormattingGroup {

   private final AttributeFilterEquation equation;
   private final List<String> styles;
   private final String color;
   private final String background;

   @JsonCreator
   public AttributeFormattingGroup(@JsonProperty("equation") final AttributeFilterEquation equation,
         @JsonProperty("styles") final List<String> styles,
         @JsonProperty("color") final String color,
         @JsonProperty("background") final String background) {
      this.equation = equation;
      this.styles = styles;
      this.color = color;
      this.background = background;
   }

   public AttributeFilterEquation getEquation() {
      return equation;
   }

   public List<String> getStyles() {
      return styles;
   }

   public String getBackground() {
      return background;
   }

   public String getColor() {
      return color;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof AttributeFormattingGroup)) {
         return false;
      }
      final AttributeFormattingGroup that = (AttributeFormattingGroup) o;
      return Objects.equals(equation, that.equation) && Objects.equals(styles, that.styles) && Objects.equals(color, that.color) && Objects.equals(background, that.background);
   }

   @Override
   public int hashCode() {
      return Objects.hash(equation, styles, color, background);
   }

   @Override
   public String toString() {
      return "AttributeFormattingGroup{" +
            "equation=" + equation +
            ", styles=" + styles +
            ", color='" + color + '\'' +
            ", background='" + background + '\'' +
            '}';
   }
}
