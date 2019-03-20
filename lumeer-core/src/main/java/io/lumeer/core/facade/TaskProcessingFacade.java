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
package io.lumeer.core.facade;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Rule;
import io.lumeer.core.task.AbstractContextualTask;
import io.lumeer.core.task.ContextualTaskFactory;
import io.lumeer.core.task.FunctionTask;
import io.lumeer.core.task.RuleTask;
import io.lumeer.core.task.TaskExecutor;
import io.lumeer.engine.api.event.CreateDocument;
import io.lumeer.engine.api.event.CreateLinkInstance;
import io.lumeer.engine.api.event.DocumentEvent;
import io.lumeer.engine.api.event.LinkInstanceEvent;
import io.lumeer.engine.api.event.RemoveDocument;
import io.lumeer.engine.api.event.RemoveLinkInstance;
import io.lumeer.engine.api.event.UpdateDocument;
import io.lumeer.engine.api.event.UpdateLinkInstance;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkTypeDao;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@RequestScoped
public class TaskProcessingFacade {

   @Inject
   private TaskExecutor taskExecutor;

   @Inject
   private ContextualTaskFactory contextualTaskFactory;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private FunctionFacade functionFacade;

   public void onCreateDocument(@Observes final CreateDocument createDocument) {
      final Collection collection = getCollectionForEvent(createDocument);
      if (collection == null) {
         return;
      }

      FunctionTask functionTask = functionFacade.createTaskForCreatedDocument(collection, createDocument.getDocument());
      List<RuleTask> tasks = createDocumentCreateRuleTasks(collection, createDocument);
      RuleTask ruleTask = createOrderedRuleTask(tasks);

      processFunctionAndRuleTasks(functionTask, ruleTask);
   }

   private Collection getCollectionForEvent(final DocumentEvent event) {
      if (event.getDocument() != null) {
         return collectionDao.getCollectionById(event.getDocument().getCollectionId());
      }
      return null;
   }

   private List<RuleTask> createDocumentCreateRuleTasks(final Collection collection, final CreateDocument createDocument) {
      if (createDocument.getDocument() != null) {
         return createRuleTasks(collection, null, createDocument.getDocument(), Arrays.asList(Rule.RuleTiming.CREATE, Rule.RuleTiming.CREATE_UPDATE, Rule.RuleTiming.CREATE_DELETE, Rule.RuleTiming.ALL));
      }
      return Collections.emptyList();
   }

   private List<RuleTask> createRuleTasks(final Collection collection, final Document originalDocument, final Document document, final List<Rule.RuleTiming> timings) {
      if (collection.getRules() == null) {
         return Collections.emptyList();
      }
      return collection.getRules().entrySet().stream()
                       .filter(entry -> timings.contains(entry.getValue().getTiming()))
                       .map(entry -> {
                          final RuleTask ruleTask = contextualTaskFactory.getInstance(RuleTask.class);
                          ruleTask.setRule(entry.getKey(), entry.getValue(), collection, originalDocument, document);
                          return ruleTask;
                       }).collect(Collectors.toList());
   }

   private RuleTask createOrderedRuleTask(List<RuleTask> tasks) {
      if (tasks.isEmpty()) {
         return null;
      }

      for (int i = 1; i < tasks.size(); i++) {
         tasks.get(i - 1).setParent(tasks.get(i));
      }

      return tasks.get(0);
   }

   private void processFunctionAndRuleTasks(FunctionTask functionTask, RuleTask ruleTask) {
      if (functionTask != null) {
         setParentForLatestTask(functionTask, ruleTask);
         functionTask.process();
      } else if (ruleTask != null) {
         ruleTask.process();
      }
   }

   private void setParentForLatestTask(AbstractContextualTask task, AbstractContextualTask newParent) {
      AbstractContextualTask currentTask = task;
      while (currentTask.getParent() != null) {
         currentTask = currentTask.getParent();
      }
      currentTask.setParent(newParent);
   }

   public void onDocumentUpdate(@Observes final UpdateDocument updateDocument) {
      final Collection collection = getCollectionForEvent(updateDocument);
      if (collection == null) {
         return;
      }

      FunctionTask functionTask = functionFacade.createTaskForUpdateDocument(collection, updateDocument.getOriginalDocument(), updateDocument.getDocument());
      List<RuleTask> tasks = createDocumentUpdateRuleTasks(collection, updateDocument);
      RuleTask ruleTask = createOrderedRuleTask(tasks);

      processFunctionAndRuleTasks(functionTask, ruleTask);
   }

   private List<RuleTask> createDocumentUpdateRuleTasks(final Collection collection, final UpdateDocument updateDocument) {
      if (updateDocument.getOriginalDocument() != null && updateDocument.getDocument() != null) {
         return createRuleTasks(collection, updateDocument.getOriginalDocument(), updateDocument.getDocument(), Arrays.asList(Rule.RuleTiming.UPDATE, Rule.RuleTiming.CREATE_UPDATE, Rule.RuleTiming.UPDATE_DELETE, Rule.RuleTiming.ALL));
      }
      return Collections.emptyList();
   }

   public void onRemoveDocument(@Observes final RemoveDocument removeDocument) {
      final Collection collection = getCollectionForEvent(removeDocument);
      if (collection == null) {
         return;
      }

      FunctionTask functionTask = functionFacade.createTaskForRemovedDocument(collection, removeDocument.getDocument());
      List<RuleTask> tasks = createDocumentRemoveRuleTasks(collection, removeDocument);
      RuleTask ruleTask = createOrderedRuleTask(tasks);

      processFunctionAndRuleTasks(functionTask, ruleTask);
   }

   private List<RuleTask> createDocumentRemoveRuleTasks(final Collection collection, final RemoveDocument removeDocument) {
      if (removeDocument.getDocument() != null) {
         return createRuleTasks(collection, removeDocument.getDocument(), null, Arrays.asList(Rule.RuleTiming.DELETE, Rule.RuleTiming.CREATE_DELETE, Rule.RuleTiming.UPDATE_DELETE, Rule.RuleTiming.ALL));
      }
      return Collections.emptyList();
   }

   public void onCreateLink(@Observes final CreateLinkInstance createLinkEvent) {
      LinkType linkType = getLinkTypeForEvent(createLinkEvent);
      if (linkType == null) {
         return;
      }

      FunctionTask functionTask = functionFacade.createTaskForCreatedLink(linkType, createLinkEvent.getLinkInstance());
      if (functionTask != null) {
         functionTask.process();
      }
   }

   private LinkType getLinkTypeForEvent(LinkInstanceEvent event) {
      if (event.getLinkInstance() != null) {
         return linkTypeDao.getLinkType(event.getLinkInstance().getLinkTypeId());
      }

      return null;
   }

   public void onUpdateLink(@Observes final UpdateLinkInstance updateLinkEvent) {
      LinkType linkType = getLinkTypeForEvent(updateLinkEvent);
      if (linkType == null) {
         return;
      }

      FunctionTask functionTask = functionFacade.creatTaskForChangedLink(linkType, updateLinkEvent.getOriginalLinkInstance(), updateLinkEvent.getLinkInstance());
      if (functionTask != null) {
         functionTask.process();
      }
   }

   public void onRemoveLink(@Observes final RemoveLinkInstance removeLinkInstanceEvent) {
      LinkType linkType = getLinkTypeForEvent(removeLinkInstanceEvent);
      if (linkType == null) {
         return;
      }

      FunctionTask functionTask = functionFacade.createTaskForRemovedLink(linkType, removeLinkInstanceEvent.getLinkInstance());
      if (functionTask != null) {
         functionTask.process();
      }
   }
}
