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
package io.lumeer.api.util;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Rule;
import io.lumeer.api.model.rule.AutoLinkRule;

import java.util.HashMap;
import java.util.Objects;

public class CollectionUtil {

   private CollectionUtil() {

   }

   public static boolean containsAutoLinkRuleLinkType(final Collection collection, final String linkTypeId) {
      return Objects.requireNonNullElse(collection.getRules(), new HashMap<>())
                    .entrySet().stream()
                    .anyMatch(entry -> CollectionUtil.containsAutoLinkRuleLinkType((Rule) entry.getValue(), linkTypeId));
   }

   public static boolean containsAutoLinkRuleLinkType(final Rule rule, final String linkTypeId) {
      if (!rule.getType().equals(Rule.RuleType.AUTO_LINK)) {
         return false;
      }

      var autoLinkRule = new AutoLinkRule(rule);

      return autoLinkRule.getLinkType().equals(linkTypeId);
   }

   public static boolean containsAutoLinkRuleAttribute(final Collection collection, final String collectionId, final String attributeId) {
      return Objects.requireNonNullElse(collection.getRules(), new HashMap<>())
                    .entrySet().stream()
                    .anyMatch(entry -> CollectionUtil.containsAutoLinkRuleAttribute((Rule) entry.getValue(), collectionId, attributeId));
   }

   public static boolean containsAutoLinkRuleAttribute(final Rule rule, final String collectionId, final String attributeId) {
      if (!rule.getType().equals(Rule.RuleType.AUTO_LINK)) {
         return false;
      }

      var autoLinkRule = new AutoLinkRule(rule);

      return (autoLinkRule.getCollection1().equals(collectionId) && autoLinkRule.getAttribute1().equals(attributeId))
            || (autoLinkRule.getCollection2().equals(collectionId) && autoLinkRule.getAttribute2().equals(attributeId));
   }
}
