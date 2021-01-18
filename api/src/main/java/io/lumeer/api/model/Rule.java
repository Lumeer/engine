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

import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Rule {

   public static final String NAME = "name";
   public static final String TYPE = "type";
   public static final String TIMING = "timing";
   public static final String CONFIGURATION = "configuration";

   public enum RuleType {
      AUTO_LINK, BLOCKLY, ZAPIER
   }

   public enum RuleTiming {
      CREATE, UPDATE, CREATE_UPDATE, DELETE, CREATE_DELETE, UPDATE_DELETE, ALL
   }

   private String name;
   private RuleType type;
   private RuleTiming timing;
   protected DataDocument configuration;

   @JsonCreator
   public Rule(@JsonProperty(NAME) final String name, @JsonProperty(TYPE) final RuleType type, @JsonProperty(TIMING) final RuleTiming timing, @JsonProperty(CONFIGURATION) final DataDocument configuration) {
      this.name = name;
      this.type = type;
      this.timing = timing;
      this.configuration = configuration;
   }

   public Rule(Rule rule) {
      this.name = rule.name;
      this.type = rule.type;
      this.timing = rule.timing;
      this.configuration = new DataDocument(rule.configuration);
   }

   public String getName() {
      return name;
   }

   public void setName(final String name) {
      this.name = name;
   }

   public RuleType getType() {
      return type;
   }

   public void setType(final RuleType type) {
      this.type = type;
   }

   @Override
   public String toString() {
      return "Rule{" +
            "name=" + name +
            ", type=" + type +
            ", timing=" + timing +
            ", configuration=" + configuration +
            '}';
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final Rule rule = (Rule) o;
      return Objects.equals(name, rule.name) && type == rule.type && timing == rule.timing && Objects.equals(configuration, rule.configuration);
   }

   @Override
   public int hashCode() {
      return Objects.hash(name, type, timing, configuration);
   }

   public RuleTiming getTiming() {
      return timing;
   }

   public void setTiming(final RuleTiming timing) {
      this.timing = timing;
   }

   public DataDocument getConfiguration() {
      return configuration;
   }

   public void setConfiguration(final DataDocument configuration) {
      this.configuration = configuration;
   }

}
