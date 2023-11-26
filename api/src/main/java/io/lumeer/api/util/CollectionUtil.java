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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.CollectionPurpose;
import io.lumeer.api.model.CollectionPurposeType;
import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.Rule;
import io.lumeer.api.model.rule.AutoLinkRule;

import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
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

   public static Attribute getDefaultAttribute(final Collection collection) {
      if(collection == null || collection.getAttributes() == null) {
         return null;
      }

      var defaultAttribute = getAttribute(collection, collection.getDefaultAttributeId());
      if(defaultAttribute != null) {
         return defaultAttribute;
      }

      return collection.getAttributes().stream().min(Comparator.comparing(Attribute::getId)).orElse(null);
   }

   public static Attribute getAttribute(final Collection collection, final String attributeId) {
      return ResourceUtils.findAttribute(collection.getAttributes(), attributeId);
   }

   public static Attribute getAttributeByName(final Collection collection, final String attributeName) {
      return ResourceUtils.findAttributeByName(collection.getAttributes(), attributeName);
   }

   public static boolean isDueDateInUTC(final Collection collection) {
      final CollectionPurpose purpose = collection.getPurpose();

      if (purpose != null && purpose.getType() == CollectionPurposeType.Tasks) {
         if (StringUtils.isNotEmpty(purpose.getDueDateAttributeId())) {
            final Attribute attribute = getAttribute(collection, purpose.getDueDateAttributeId());

            if (attribute != null && attribute.getConstraint() != null && attribute.getConstraint().getType() == ConstraintType.DateTime) {
               return AttributeUtil.isUTC(attribute);
            }
         }
      }

      return false;
   }

   public static boolean hasDueDateFormatTimeOptions(final Collection collection) {
      final CollectionPurpose purpose = collection.getPurpose();

      if (purpose != null && purpose.getType() == CollectionPurposeType.Tasks) {
         if (StringUtils.isNotEmpty(purpose.getDueDateAttributeId())) {
            final Attribute attribute = getAttribute(collection, purpose.getDueDateAttributeId());

            if (attribute != null && attribute.getConstraint() != null && attribute.getConstraint().getType() == ConstraintType.DateTime) {
               return AttributeUtil.formatHasTimeOptions(attribute);
            }
         }
      }

      return false;
   }
}
