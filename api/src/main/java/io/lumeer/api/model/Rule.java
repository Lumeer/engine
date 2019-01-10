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

import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class Rule {

   public static final String TYPE = "type";
   public static final String CONFIGURATION = "configuration";

   public static final String BLOCKLY_XML = "blocklyXml";
   public static final String BLOCKLY_JS = "blocklyJs";
   public static final String AUTO_LINK_COLLECTION1 = "collection1";
   public static final String AUTO_LINK_ATTRIBUTE1 = "attribute1";
   public static final String AUTO_LINK_COLLECTION2 = "collection2";
   public static final String AUTO_LINK_ATTRIBUTE2 = "attribute2";
   public static final String AUTO_LINK_LINK_TYPE = "linkType";

   public enum RuleType {
      AUTO_LINK, BLOCKLY
   }

   private RuleType type;
   private DataDocument configuration;

   @JsonCreator
   public Rule(@JsonProperty(TYPE) final RuleType type, @JsonProperty(CONFIGURATION) final DataDocument configuration) {
      this.type = type;
      this.configuration = configuration;
   }

   public RuleType getType() {
      return type;
   }

   public void setType(final RuleType type) {
      this.type = type;
   }

   public DataDocument getConfiguration() {
      return configuration;
   }

   public void setConfiguration(final DataDocument configuration) {
      this.configuration = configuration;
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
      return type == rule.type &&
            Objects.equals(configuration, rule.configuration);
   }

   @Override
   public int hashCode() {
      return Objects.hash(type, configuration);
   }

   @Override
   public String toString() {
      return "Rule{" +
            "type=" + type +
            ", configuration=" + configuration +
            '}';
   }
}
