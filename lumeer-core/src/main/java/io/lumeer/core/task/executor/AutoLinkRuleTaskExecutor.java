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
package io.lumeer.core.task.executor;

import io.lumeer.api.model.ConditionType;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.rule.AutoLinkRule;
import io.lumeer.core.facade.PusherFacade;
import io.lumeer.core.task.RuleTask;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.filter.CollectionSearchAttributeFilter;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SearchQueryStem;

import org.marvec.pusher.data.Event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class AutoLinkRuleTaskExecutor {

   private static Logger log = Logger.getLogger(AutoLinkRuleTaskExecutor.class.getName());

   private String ruleName;
   private AutoLinkRule rule;
   private RuleTask ruleTask;

   public AutoLinkRuleTaskExecutor(final String ruleName, final RuleTask ruleTask) {
      this.ruleName = ruleName;
      this.rule = new AutoLinkRule(ruleTask.getRule());
      this.ruleTask = ruleTask;
   }

   public void execute() {
      final LinkType linkType = ruleTask.getDaoContextSnapshot().getLinkTypeDao().getLinkType(rule.getLinkType());

      if (linkType != null) {
         final Document thisDocument = ruleTask.getOldDocument() != null ? ruleTask.getOldDocument() : ruleTask.getNewDocument();
         final String thisCollectionId = thisDocument.getCollectionId();
         final String thatCollectionId = linkType.getCollectionIds().get(0).equals(thisCollectionId) ? linkType.getCollectionIds().get(1) : linkType.getCollectionIds().get(0);
         final String thisAttribute, thatAttribute;

         if (rule.getCollection1().equals(thisCollectionId)) {
            thisAttribute = rule.getAttribute1();
            thatAttribute = rule.getAttribute2();
         } else {
            thisAttribute = rule.getAttribute2();
            thatAttribute = rule.getAttribute1();
         }

         // both documents are set
         if (ruleTask.getOldDocument() != null && ruleTask.getNewDocument() != null) {
            Object o1 = ruleTask.getOldDocument().getData().get(thisAttribute);
            Object o2 = ruleTask.getNewDocument().getData().get(thisAttribute);

            // the attributes are different
            if ((o1 != null && !o1.equals(o2)) || (o2 != null && !o2.equals(o1))) {
               // it was not null before
               if (o1 != null) {
                  removeLinks(ruleTask.getOldDocument(), linkType, thatCollectionId, thisAttribute, thatAttribute);
               }
               // and it is not null either
               if (o2 != null) {
                  addLinks(ruleTask.getNewDocument(), linkType, thatCollectionId, thisAttribute, thatAttribute);
               }
            }
         } else { // one of the docs is null (i.e. new document created or old document deleted
            // when oldDocument is set and the newDocument isn't, the old one was deleted and all links were automatically removed

            // new document was created
            if (ruleTask.getNewDocument() != null && ruleTask.getNewDocument().getData().get(thisAttribute) != null) {
               addLinks(ruleTask.getNewDocument(), linkType, thatCollectionId, thisAttribute, thatAttribute);
            }
         }
      }
   }

   private void removeLinks(final Document oldDocument, final LinkType linkType, final String thatCollection, final String thisAttribute, final String thatAttribute) {
      final String thisCollection = oldDocument.getCollectionId();

      final SearchQuery query = SearchQuery
            .createBuilder()
            .stems(Collections.singletonList(
                  SearchQueryStem
                        .createBuilder("")
                        .linkTypeIds(Collections.singletonList(linkType.getId()))
                        .documentIds(Set.of(oldDocument.getId()))
                        .build()))
            .build();

      final List<LinkInstance> links = ruleTask.getDaoContextSnapshot().getLinkInstanceDao().searchLinkInstances(query);
      ruleTask.getDaoContextSnapshot().getLinkInstanceDao().deleteLinkInstances(query);
      ruleTask.getDaoContextSnapshot().getLinkDataDao().deleteData(linkType.getId(), links.stream().map(LinkInstance::getId).collect(Collectors.toSet()));

      sendPushNotifications(thisCollection, thatCollection, links, true);
   }

   private void addLinks(final Document newDocument, final LinkType linkType, final String thatCollection, final String thisAttribute, final String thatAttribute) {
      final String thisCollection = newDocument.getCollectionId();
      final Object value = newDocument.getData().get(thisAttribute);
      final SearchQueryStem queryStem = SearchQueryStem
            .createBuilder(thatCollection)
            .filters(Set.of(new CollectionSearchAttributeFilter(thatCollection, ConditionType.EQUALS, thatAttribute, value)))
            .build();

      final List<String> targetDocuments = ruleTask.getDaoContextSnapshot().getDataDao()
                                                   .searchData(queryStem, null, ruleTask.getDaoContextSnapshot().getCollectionDao().getCollectionById(thatCollection))
                                                   .stream()
                                                   .map(DataDocument::getId)
                                                   .collect(Collectors.toList());

      if (targetDocuments.size() > 0) {
         final List<LinkInstance> linkInstances =
               targetDocuments.stream()
                              .filter(documentId -> !thisCollection.equals(thatCollection) || !documentId.equals(newDocument.getId()))
                              .map(documentId ->
                                    new LinkInstance(rule.getLinkType(), Arrays.asList(newDocument.getId(), documentId)))
                              .collect(Collectors.toList());

         ruleTask.getDaoContextSnapshot().getLinkInstanceDao().createLinkInstances(linkInstances);
         sendPushNotifications(thisCollection, thatCollection, linkInstances, false);
      }
   }

   private void sendPushNotifications(final String thisCollection, final String thatCollection, final List<LinkInstance> links, final boolean removeOperation) {
      if (ruleTask.getPusherClient() != null) {
         final Set<String> users1 = ruleTask.getDaoContextSnapshot().getCollectionReaders(thisCollection);
         final Set<String> users2 = ruleTask.getDaoContextSnapshot().getCollectionReaders(thatCollection);
         final Set<String> users = users1.stream().filter(users2::contains).collect(Collectors.toSet());

         final List<Event> events = new ArrayList<>();
         users.forEach(user -> {
            links.forEach(link -> {
               if (removeOperation) {
                  events.add(
                        new Event(
                              PusherFacade.PRIVATE_CHANNEL_PREFIX + user,
                              LinkInstance.class.getSimpleName() + PusherFacade.REMOVE_EVENT_SUFFIX,
                              new PusherFacade.ResourceId(link.getId(), ruleTask.getDaoContextSnapshot().getOrganizationId(), ruleTask.getDaoContextSnapshot().getProjectId())));
               } else {
                  events.add(
                        new Event(
                              PusherFacade.PRIVATE_CHANNEL_PREFIX + user,
                              LinkInstance.class.getSimpleName() + PusherFacade.CREATE_EVENT_SUFFIX,
                              new PusherFacade.ObjectWithParent(link, ruleTask.getDaoContextSnapshot().getOrganizationId(), ruleTask.getDaoContextSnapshot().getProjectId())));
               }
            });
         });

         ruleTask.getPusherClient().trigger(events);
      }
   }
}