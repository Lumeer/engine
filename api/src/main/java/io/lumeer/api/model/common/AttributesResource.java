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
package io.lumeer.api.model.common;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.Rule;
import io.lumeer.api.util.AttributeUtil;
import io.lumeer.api.util.ResourceUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public interface AttributesResource {

   public static final String ATTRIBUTE_PREFIX = "a";

   Map<String, Rule> getRules();

   void setRules(final Map<String, Rule> rules);

   Collection<Attribute> getAttributes();

   @JsonIgnore
   Collection<Attribute> getMutableAttributes();

   void setAttributes(final Collection<Attribute> attributes);

   Integer getLastAttributeNum();

   void setLastAttributeNum(final Integer lastAttributeNum);

   default void createAttribute(final Attribute attribute) {
      var hasCustomId = attribute.getId() != null;
      if (attribute.getId() == null) {
         final Integer freeNum = getFreeAttributeNum();
         attribute.setId(ATTRIBUTE_PREFIX + freeNum);
         setLastAttributeNum(freeNum);
      }
      getMutableAttributes().add(attribute);

      // we should check this after attribute was added because custom id can be any number (i.e. we have a1, a2, a3 and next we try to add a200)
      if (hasCustomId) {
         final Integer freeNum = getFreeAttributeNum();
         setLastAttributeNum(freeNum - 1);
      }
   }

   default void updateAttribute(final String attributeId, final Attribute attribute) {
      Optional<Attribute> oldAttribute = getMutableAttributes().stream().filter(attr -> attr.getId().equals(attributeId)).findFirst();
      getMutableAttributes().removeIf(a -> a.getId().equals(attributeId));

      oldAttribute.ifPresent((a) -> attribute.setUsageCount(a.getUsageCount()));
      getMutableAttributes().add(attribute);

      if (oldAttribute.isPresent() && !oldAttribute.get().getName().equals(attribute.getName())) {
         AttributeUtil.renameChildAttributes(getMutableAttributes(), oldAttribute.get().getName(), attribute.getName());
      }
   }

   default void deleteAttribute(final String attributeId) {
      Optional<Attribute> toDelete = getMutableAttributes().stream().filter(attribute -> attribute.getId().equals(attributeId)).findFirst();
      toDelete.ifPresent(jsonAttribute -> getMutableAttributes().removeIf(attribute -> AttributeUtil.isEqualOrChild(attribute, jsonAttribute.getName())));
   }

   default void patchRules(final Map<String, Rule> rules) {
      // update and delete rules
      final Map<String, Rule> updatingRules = new HashMap<>();
      getRules().keySet().forEach(ruleId -> {
         var oldRule = getRules().get(ruleId);
         var newRule = rules.get(ruleId);
         if (newRule != null) {
            newRule.checkConfiguration(oldRule);
            updatingRules.put(ruleId, newRule);
         }
      });

      // create new rules
      Set<String> ruleIds = getRules().keySet();
      rules.keySet().stream()
           .filter(ruleId -> !ruleIds.contains(ruleId))
           .forEach(ruleId -> {
              var newRule = rules.get(ruleId);
              newRule.checkConfiguration(null);
              updatingRules.put(ruleId, newRule);
           });

      setRules(updatingRules);
   }

   default void patchAttributes(final java.util.Collection<Attribute> attributes, final Set<RoleType> roles) {
      // update and delete attributes
      new ArrayList<>(getAttributes()).forEach(oldAttribute -> {
         var newAttribute = ResourceUtils.findAttribute(attributes, oldAttribute.getId());
         if (newAttribute != null) { // update
            var updatingAttribute = oldAttribute.copy();
            updatingAttribute.patch(newAttribute, roles);
            updateAttribute(oldAttribute.getId(), updatingAttribute);
         } else if (roles.contains(RoleType.AttributeEdit)) { // delete
            deleteAttribute(oldAttribute.getId());
         }
      });

      // create new attributes
      if (roles.contains(RoleType.AttributeEdit)) {
         Set<String> attributesIds = getAttributes().stream().map(Attribute::getId).collect(Collectors.toSet());
         attributes.stream()
                   .filter(attribute -> attribute.getId() == null || !attributesIds.contains(attribute.getId()))
                   .forEach(newAttribute -> {
                      newAttribute.patchCreation(roles);
                      createAttribute(newAttribute);
                   });
      }
   }

   private Integer getFreeAttributeNum() {
      final AtomicInteger last = new AtomicInteger(Math.max(1, getLastAttributeNum() + 1));
      while (getAttributes().stream().anyMatch(attribute -> attribute.getId().equals(ATTRIBUTE_PREFIX + last.get()))) {
         last.incrementAndGet();
      }

      return last.get();
   }
}
