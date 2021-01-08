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
package io.lumeer.core.task;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Constraint;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AutoLinkBatchTask extends AbstractContextualTask {

   private LinkType linkType;
   private Collection collection;
   private String attributeId;
   private Constraint constraint;
   private Collection otherCollection;
   private String otherAttributeId;
   private Constraint otherConstraint;

   public void setupBatch(final LinkType linkType,
         final Collection collection, final String attributeId, final Constraint constraint,
         final Collection otherCollection, final String otherAttributeId, final Constraint otherConstraint) {
      this.linkType = linkType;
      this.collection = collection;
      this.attributeId = attributeId;
      this.constraint = constraint;
      this.otherCollection = otherCollection;
      this.otherAttributeId = otherAttributeId;
      this.otherConstraint = otherConstraint;
   }

   @Override
   public void process(final TaskExecutor executor) {
      List<LinkInstance> links = new ArrayList<>();

      Map<String, Set<String>> source = new HashMap<>();
      daoContextSnapshot.getDataDao().getDataStream(collection.getId()).forEach(dd -> {
         final Object o = getConstraintManager().decode(dd.getObject(attributeId), constraint);

         if (o != null) {
            source.computeIfAbsent(o.toString(), key -> new HashSet<>()).add(dd.getId());
         }
      });

      if (source.size() > 0) {
         Map<String, Set<String>> target = new HashMap<>();
         daoContextSnapshot.getDataDao().getDataStream(otherCollection.getId()).forEach(dd -> {
            final Object o = constraintManager.decode(dd.getObject(otherAttributeId), otherConstraint);

            if (o != null && source.containsKey(o.toString())) {
               target.computeIfAbsent(o.toString(), key -> new HashSet<>()).add(dd.getId());
            }
         });

         if (target.size() > 0) {
            source.keySet().forEach(key -> {
               if (target.containsKey(key)) {
                  final Set<String> sourceIds = source.get(key);
                  final Set<String> targetIds = target.get(key);

                  sourceIds.forEach(sourceId -> {
                     targetIds.forEach(targetId -> {
                        links.add(new LinkInstance(linkType.getId(), List.of(sourceId, targetId)));
                     });
                  });
               }
            });

            if (links.size() > 0) {
               final List<LinkInstance> newLinks = daoContextSnapshot.getLinkInstanceDao().createLinkInstances(links, false);
               sendPushNotifications(linkType, newLinks, true);
            }
         }
      }
   }

}