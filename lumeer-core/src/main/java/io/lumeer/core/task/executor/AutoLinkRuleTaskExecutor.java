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
package io.lumeer.core.task.executor;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.rule.AutoLinkRule;
import io.lumeer.core.facade.FunctionFacade;
import io.lumeer.core.task.RuleTask;
import io.lumeer.core.task.TaskExecutor;
import io.lumeer.core.task.executor.matcher.DocumentMatcher;
import io.lumeer.core.util.Tuple;

import org.apache.commons.lang3.StringUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AutoLinkRuleTaskExecutor {

   private static final Logger log = Logger.getLogger(AutoLinkRuleTaskExecutor.class.getName());

   private final ChangesTracker changesTracker = new ChangesTracker();
   private String ruleName;
   private final AutoLinkRule rule;
   private final RuleTask ruleTask;

   public AutoLinkRuleTaskExecutor(final String ruleName, final RuleTask ruleTask) {
      this.ruleName = ruleName;
      this.rule = new AutoLinkRule(ruleTask.getRule());
      this.ruleTask = ruleTask;
   }

   public ChangesTracker execute(final TaskExecutor taskExecutor) {
      try {
         final DocumentMatcher matcher = new DocumentMatcher(ruleTask.getDaoContextSnapshot(), ruleTask);

         if (matcher.initialize(rule)) {
            // both documents are set
            if (matcher.getOldDocument() != null && matcher.getNewDocument() != null) {
               // the attributes are different
               if ((matcher.getOldValue() != null && !matcher.getOldValue().equals(matcher.getNewValue())) ||
                     (matcher.getNewValue() != null && !matcher.getNewValue().equals(matcher.getOldValue()))) {
                  final List<LinkInstance> links = matcher.getAllLinkInstances();
                  List<LinkInstance> remainingLinks = List.of();

                  // it was not null before
                  if (matcher.getOldValue() != null && StringUtils.isNotEmpty(matcher.getOldValue().toString())) {
                     remainingLinks = removeLinks(taskExecutor, links, matcher);
                  }
                  // and it is not null either
                  if (matcher.getNewValue() != null && StringUtils.isNotEmpty(matcher.getNewValue().toString())) {
                     addLinks(taskExecutor, remainingLinks, matcher);
                  }
               }
            } else {// one of the docs is null (i.e. new document created or old document deleted)
               // when oldDocument is set and the newDocument isn't, the old one was deleted and all links were automatically removed

               // new document was created
               if (matcher.getNewDocument() != null && matcher.getNewDocument().getData().get(matcher.getThisAttribute().getId()) != null) {
                  addLinks(taskExecutor, List.of(), matcher);
               }
            }
         }

         return changesTracker;
      } catch (Exception ex) {
         log.log(Level.SEVERE, "Error executing auto-link rule: ", ex);
         return changesTracker;
      }
   }

   // returns remaining links
   private List<LinkInstance> removeLinks(final TaskExecutor taskExecutor, final List<LinkInstance> links, final DocumentMatcher matcher) {
      final String thisCollection = matcher.getThisCollection().getId();

      // filter links for old attribute value
      // only source can change (old value, new value), target remains the same
      // for multiselects on both sides, we link to values having all source values (i.e. target might have additional values)
      // if source is multiselect (select or user) value and target is single value => compute removed values from source attr. & filter links by removed values on target
      // if source is single value and target is multiselect value => filter links by target values containing the old source value
      // if source is single value and target is single value => filter links by target values containing the old source value
      // if both source and target are multiselect value => compute ADDED values to source attr. & filter links by target values NOT containing the added values

      if (!links.isEmpty()) {
         // find link instances matching the old value
         final Tuple<List<Document>, List<LinkInstance>> oldLinks = matcher.filterForRemoval();
         final List<LinkInstance> linksForRemoval = oldLinks.getSecond();

         if (linksForRemoval.size() > 0) {
            final Set<String> removeIds = linksForRemoval.stream().map(LinkInstance::getId).collect(toSet());
            ruleTask.getDaoContextSnapshot().getLinkInstanceDao().deleteLinkInstances(removeIds);
            ruleTask.getDaoContextSnapshot().getLinkDataDao().deleteData(matcher.getLinkType().getId(), removeIds);
            changesTracker.updateLinkTypesMap(Map.of(matcher.getLinkType().getId(), matcher.getLinkType()));
            changesTracker.addRemovedLinkInstances(linksForRemoval);

            final FunctionFacade functionFacade = ruleTask.getFunctionFacade();
            final List<String> skipCollectionIds = List.of(thisCollection);
            linksForRemoval.forEach(linkInstance -> {
               taskExecutor.submitTask(functionFacade.createTaskForRemovedLinks(matcher.getLinkType(), Collections.singletonList(linkInstance), skipCollectionIds));
            });
         }

         final List<LinkInstance> result = new ArrayList<>(links);
         result.removeAll(linksForRemoval);

         return result;
      }

      return List.of();
   }

   private void addLinks(final TaskExecutor taskExecutor, final List<LinkInstance> links, final DocumentMatcher matcher) {
      final String thisCollection = matcher.getThisCollection().getId();
      if (thisCollection.equals(matcher.getThatCollection().getId())) {
         return;
      }

      final String thisDocumentId = matcher.getThisDocument().getId();
      final Set<String> alreadyLinkedDocumentIds = links.stream().map(link -> {
         final var res = new ArrayList<>(link.getDocumentIds());
         res.remove(thisDocumentId);
         return res.get(0);
      }).collect(toSet());
      final Tuple<List<Document>, List<LinkInstance>> newLinks = matcher.filterForCreation();
      final Set<String> targetDocuments = newLinks.getFirst().stream().map(Document::getId).filter(id -> !alreadyLinkedDocumentIds.contains(id)).collect(toSet());

      // filter target documents for new attribute value
      // do not duplicate existing links
      // only source can change (old value, new value), target remains the same
      // for multiselects on both sides, we link to values having all source values (i.e. target might have additional values)
      // if source is multiselect (select or user) value and target is single value => compute added values to source attr. & filter documents by added values on target
      // if source is single value and target is multiselect value => filter documents by target values containing the new source value
      // if source is single value and target is single value => filter documents by target values containing the new source value
      // if both source and target are multiselect value => filter documents by target values containing all the new values

      if (targetDocuments.size() > 0) {
         final List<LinkInstance> linkInstances =
               targetDocuments.stream()
                              .filter(documentId -> !documentId.equals(matcher.getNewDocument().getId()))
                              .map(documentId -> {
                                 var l = new LinkInstance(rule.getLinkType(), Arrays.asList(matcher.getNewDocument().getId(), documentId));
                                 l.setCreatedBy(ruleTask.getInitiator().getId());
                                 l.setCreationDate(ZonedDateTime.now());
                                 return l;
                              })
                              .collect(toList());

         if (!linkInstances.isEmpty()) {
            ruleTask.getDaoContextSnapshot().getLinkInstanceDao().createLinkInstances(linkInstances);
            changesTracker.updateLinkTypesMap(Map.of(matcher.getLinkType().getId(), matcher.getLinkType()));
            changesTracker.addCreatedLinkInstances(linkInstances);

            final FunctionFacade functionFacade = ruleTask.getFunctionFacade();
            final List<String> skipCollectionIds = List.of(thisCollection);
            linkInstances.forEach(linkInstance -> {
               taskExecutor.submitTask(functionFacade.createTaskForCreatedLinks(matcher.getLinkType(), Collections.singletonList(linkInstance), skipCollectionIds));
            });
         }
      }
   }
}
