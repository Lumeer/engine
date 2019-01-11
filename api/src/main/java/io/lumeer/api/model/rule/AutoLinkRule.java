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
package io.lumeer.api.model.rule;

import io.lumeer.api.model.Rule;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class AutoLinkRule extends Rule {

   public static final String AUTO_LINK_COLLECTION1 = "collection1";
   public static final String AUTO_LINK_ATTRIBUTE1 = "attribute1";
   public static final String AUTO_LINK_COLLECTION2 = "collection2";
   public static final String AUTO_LINK_ATTRIBUTE2 = "attribute2";
   public static final String AUTO_LINK_LINK_TYPE = "linkType";

   public AutoLinkRule(final Rule rule) {
      super(RuleType.AUTO_LINK, rule.getTiming(), rule.getConfiguration());

      if (rule.getType() != RuleType.BLOCKLY) {
         throw new IllegalArgumentException("Cannot create AutoLink Rule from a rule of type " + rule.getType());
      }
   }

   public String getCollection1() {
      return configuration.getString(AUTO_LINK_COLLECTION1);
   }

   public void setCollection1(final String collection1) {
      configuration.put(AUTO_LINK_COLLECTION1, collection1);
   }

   public String getAttribute1() {
      return configuration.getString(AUTO_LINK_ATTRIBUTE1);
   }

   public void setAttribute1(final String attribute1) {
      configuration.put(AUTO_LINK_ATTRIBUTE1, attribute1);
   }

   public String getCollection2() {
      return configuration.getString(AUTO_LINK_COLLECTION2);
   }

   public void setCollection2(final String collection2) {
      configuration.put(AUTO_LINK_COLLECTION2, collection2);
   }

   public String getAttribute2() {
      return configuration.getString(AUTO_LINK_ATTRIBUTE2);
   }

   public void setAttribute2(final String attribute2) {
      configuration.put(AUTO_LINK_ATTRIBUTE2, attribute2);
   }

   public String getLinkType() {
      return configuration.getString(AUTO_LINK_LINK_TYPE);
   }

   public void setLinkType(final String linkType) {
      configuration.put(AUTO_LINK_LINK_TYPE, linkType);
   }
}
