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
package io.lumeer.core.facade;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Rule;
import io.lumeer.core.task.ContextualTaskFactory;
import io.lumeer.core.task.FunctionTask;
import io.lumeer.core.task.RuleTask;
import io.lumeer.core.task.Task;
import io.lumeer.core.task.TaskExecutor;
import io.lumeer.engine.api.event.CreateDocumentsAndLinks;
import io.lumeer.engine.api.event.CreateDocument;
import io.lumeer.engine.api.event.CreateLinkInstance;
import io.lumeer.engine.api.event.DocumentEvent;
import io.lumeer.engine.api.event.LinkInstanceEvent;
import io.lumeer.engine.api.event.RemoveDocument;
import io.lumeer.engine.api.event.RemoveLinkInstance;
import io.lumeer.engine.api.event.RemoveLinkType;
import io.lumeer.engine.api.event.RemoveResource;
import io.lumeer.engine.api.event.UpdateDocument;
import io.lumeer.engine.api.event.UpdateLinkInstance;
import io.lumeer.engine.api.event.UpdateLinkType;
import io.lumeer.engine.api.event.UpdateResource;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkTypeDao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

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

   public void runRule(final Collection collection, final String ruleName, final Document document) {
      if (collection != null && document != null) {
         Optional<RuleTask> task = createRuleTask(collection, ruleName, null, document);
         task.ifPresent(this::processTasks);
      }
   }

   public void onCreateChain(@Observes final CreateDocumentsAndLinks chain) {
      List<Task> allTasks = new ArrayList<>();

      var linkOffset = 0;
      // it means that first link was created before first document
      if (chain.getDocuments().size() == chain.getLinkInstances().size() && chain.getDocuments().size() > 0) {
         allTasks.addAll(linkCreatedTasks(chain.getLinkInstances().get(0)));
         linkOffset = 1;
      }

      for (int i = 0; i < chain.getDocuments().size(); i++) {
         allTasks.addAll(documentCreatedTasks(chain.getDocuments().get(i)));

         var linkIndex = i + linkOffset;
         if (linkIndex < chain.getLinkInstances().size()) {
            allTasks.addAll(linkCreatedTasks(chain.getLinkInstances().get(linkIndex)));
         }
      }

      processTasks(allTasks.toArray(new Task[0]));
   }

   public void onCreateDocument(@Observes final CreateDocument createDocument) {
      List<Task> tasks = documentCreatedTasks(createDocument.getDocument());
      processTasks(tasks.toArray(new Task[0]));
   }

   private List<Task> documentCreatedTasks(Document document) {
      final Collection collection = collectionDao.getCollectionById(document.getCollectionId());
      if (collection == null) {
         return Collections.emptyList();
      }
      FunctionTask functionTask = functionFacade.createTaskForCreatedDocument(collection, document);
      List<RuleTask> tasks = createDocumentCreateRuleTasks(collection, document);
      RuleTask ruleTask = createOrderedRuleTask(tasks);
      return Arrays.asList(functionTask, ruleTask);
   }

   private Collection getCollectionForEvent(final DocumentEvent event) {
      if (event.getDocument() != null) {
         return collectionDao.getCollectionById(event.getDocument().getCollectionId());
      }
      return null;
   }

   private List<RuleTask> createDocumentCreateRuleTasks(final Collection collection, final Document document) {
      if (document != null) {
         return createRuleTasks(collection, null, document, Arrays.asList(Rule.RuleTiming.CREATE, Rule.RuleTiming.CREATE_UPDATE, Rule.RuleTiming.CREATE_DELETE, Rule.RuleTiming.ALL));
      }
      return Collections.emptyList();
   }

   private List<RuleTask> createLinkInstanceCreateRuleTasks(final LinkType linkType, final LinkInstance linkInstance) {
      if (linkInstance != null) {
         return createRuleTasks(linkType, null, linkInstance, Arrays.asList(Rule.RuleTiming.CREATE, Rule.RuleTiming.CREATE_UPDATE, Rule.RuleTiming.CREATE_DELETE, Rule.RuleTiming.ALL));
      }
      return Collections.emptyList();
   }


   private Optional<RuleTask> createRuleTask(final Collection collection, final String ruleName, final Document originalDocument, final Document document) {
      if (collection.getRules() == null) {
         return Optional.empty();
      }
      return collection.getRules().entrySet().stream()
                       .filter(entry -> entry.getKey().equals(ruleName))
                       .map(entry -> {
                          final RuleTask ruleTask = contextualTaskFactory.getInstance(RuleTask.class);
                          ruleTask.setRule(entry.getKey(), entry.getValue(), collection, originalDocument, document);
                          return ruleTask;
                       }).findFirst();
   }

   private List<RuleTask> createRuleTasks(final Collection collection, final Document originalDocument, final Document document, final List<Rule.RuleTiming> timings) {
      if (collection.getRules() == null) {
         return Collections.emptyList();
      }
      return collection.getRules().entrySet().stream()
                       .filter(entry -> timings == null || timings.contains(entry.getValue().getTiming()))
                       .map(entry -> {
                          final RuleTask ruleTask = contextualTaskFactory.getInstance(RuleTask.class);
                          ruleTask.setRule(entry.getKey(), entry.getValue(), collection, originalDocument, document);
                          return ruleTask;
                       }).collect(Collectors.toList());
   }

   private List<RuleTask> createRuleTasks(final LinkType linkType, final LinkInstance originalLinkInstance, final LinkInstance linkInstance, final List<Rule.RuleTiming> timings) {
      if (linkType.getRules() == null) {
         return Collections.emptyList();
      }
      return linkType.getRules().entrySet().stream()
                       .filter(entry -> timings.contains(entry.getValue().getTiming()))
                       .map(entry -> {
                          final RuleTask ruleTask = contextualTaskFactory.getInstance(RuleTask.class);
                          ruleTask.setRule(entry.getKey(), entry.getValue(), linkType, originalLinkInstance, linkInstance);
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

   private void processTasks(Task... tasks) {
      List<Task> filteredTasks = Arrays.stream(tasks).filter(Objects::nonNull).collect(Collectors.toList());
      if (filteredTasks.isEmpty()) {
         return;
      }

      if (filteredTasks.size() > 1) {
         for (int i = 1; i < filteredTasks.size(); i++) {
            setParentForLatestTask(filteredTasks.get(i - 1), filteredTasks.get(i));
         }
      }

      taskExecutor.submitTask(filteredTasks.get(0));
   }

   private void setParentForLatestTask(Task task, Task newParent) {
      Task currentTask = task;
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

      processTasks(functionTask, ruleTask);
   }

   private List<RuleTask> createDocumentUpdateRuleTasks(final Collection collection, final UpdateDocument updateDocument) {
      if (updateDocument.getOriginalDocument() != null && updateDocument.getDocument() != null) {
         return createRuleTasks(collection, updateDocument.getOriginalDocument(), updateDocument.getDocument(), Arrays.asList(Rule.RuleTiming.UPDATE, Rule.RuleTiming.CREATE_UPDATE, Rule.RuleTiming.UPDATE_DELETE, Rule.RuleTiming.ALL));
      }
      return Collections.emptyList();
   }

   private List<RuleTask> createLinkInstanceUpdateRuleTasks(final LinkType linkType, final UpdateLinkInstance updateLinkInstance) {
      if (updateLinkInstance.getOriginalLinkInstance() != null && updateLinkInstance.getLinkInstance() != null) {
         return createRuleTasks(linkType, updateLinkInstance.getOriginalLinkInstance(), updateLinkInstance.getLinkInstance(), Arrays.asList(Rule.RuleTiming.UPDATE, Rule.RuleTiming.CREATE_UPDATE, Rule.RuleTiming.UPDATE_DELETE, Rule.RuleTiming.ALL));
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

      processTasks(functionTask, ruleTask);
   }

   private List<RuleTask> createDocumentRemoveRuleTasks(final Collection collection, final RemoveDocument removeDocument) {
      if (removeDocument.getDocument() != null) {
         return createRuleTasks(collection, removeDocument.getDocument(), null, Arrays.asList(Rule.RuleTiming.DELETE, Rule.RuleTiming.CREATE_DELETE, Rule.RuleTiming.UPDATE_DELETE, Rule.RuleTiming.ALL));
      }
      return Collections.emptyList();
   }

   private List<RuleTask> createLinkInstanceRemoveRuleTasks(final LinkType linkType, final RemoveLinkInstance removeLinkInstance) {
      if (removeLinkInstance.getLinkInstance() != null) {
         return createRuleTasks(linkType, removeLinkInstance.getLinkInstance(), null, Arrays.asList(Rule.RuleTiming.DELETE, Rule.RuleTiming.CREATE_DELETE, Rule.RuleTiming.UPDATE_DELETE, Rule.RuleTiming.ALL));
      }
      return Collections.emptyList();
   }

   public void onCreateLink(@Observes final CreateLinkInstance createLinkEvent) {
      List<Task> tasks = linkCreatedTasks(createLinkEvent.getLinkInstance());
      processTasks(tasks.toArray(new Task[0]));
   }

   private List<Task> linkCreatedTasks(LinkInstance linkInstance) {
      LinkType linkType = linkTypeDao.getLinkType(linkInstance.getLinkTypeId());
      if (linkType == null) {
         return Collections.emptyList();
      }

      FunctionTask functionTask = functionFacade.createTaskForCreatedLink(linkType, linkInstance);
      List<RuleTask> tasks = createLinkInstanceCreateRuleTasks(linkType, linkInstance);
      RuleTask ruleTask = createOrderedRuleTask(tasks);

      return Arrays.asList(functionTask, ruleTask);
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
      List<RuleTask> tasks = createLinkInstanceUpdateRuleTasks(linkType, updateLinkEvent);
      RuleTask ruleTask = createOrderedRuleTask(tasks);

      processTasks(functionTask, ruleTask);
   }

   public void onRemoveLink(@Observes final RemoveLinkInstance removeLinkInstanceEvent) {
      LinkType linkType = getLinkTypeForEvent(removeLinkInstanceEvent);
      if (linkType == null) {
         return;
      }

      FunctionTask functionTask = functionFacade.createTaskForRemovedLink(linkType, removeLinkInstanceEvent.getLinkInstance());
      List<RuleTask> tasks = createLinkInstanceRemoveRuleTasks(linkType, removeLinkInstanceEvent);
      RuleTask ruleTask = createOrderedRuleTask(tasks);

      processTasks(functionTask, ruleTask);
   }

   public void onUpdateCollection(@Observes final UpdateResource updateResource) {
      if (updateResource.getResource() == null || updateResource.getOriginalResource() == null || !(updateResource.getResource() instanceof Collection)) {
         return;
      }

      List<Task> tasks = new ArrayList<>();
      Collection original = (Collection) updateResource.getOriginalResource();
      Collection current = (Collection) updateResource.getResource();
      AttributesDiff attributesDiff = checkAttributesDiff(original.getAttributes(), current.getAttributes());

      attributesDiff.getRemovedIds().forEach(removedAttributeId -> functionFacade.onDeleteCollectionAttribute(original.getId(), removedAttributeId));
      attributesDiff.getRemovedFunction().forEach(removedAttributeId -> functionFacade.onDeleteCollectionFunction(original.getId(), removedAttributeId));

      attributesDiff.getCreatedFunction().forEach(attribute -> tasks.add(functionFacade.createTaskForCreatedFunction(current, attribute)));
      attributesDiff.getUpdatedFunction().forEach(attribute -> tasks.add(functionFacade.createTaskForUpdatedFunction(current, attribute)));

      processTasks(tasks.toArray(new Task[0]));
   }

   public void onRemoveCollection(@Observes final RemoveResource removeResource) {
      if (removeResource.getResource() == null || !(removeResource.getResource() instanceof Collection)) {
         return;
      }

      functionFacade.onDeleteCollection(removeResource.getResource().getId());
   }

   public void onUpdateLinkType(@Observes final UpdateLinkType updateLinkType) {
      if (updateLinkType.getOriginalLinkType() == null || updateLinkType.getLinkType() == null) {
         return;
      }

      List<Task> tasks = new ArrayList<>();
      AttributesDiff attributesDiff = checkAttributesDiff(updateLinkType.getOriginalLinkType().getAttributes(), updateLinkType.getLinkType().getAttributes());
      String linkTypeId = updateLinkType.getOriginalLinkType().getId();

      attributesDiff.getRemovedIds().forEach(removedAttributeId -> functionFacade.onDeleteLinkAttribute(linkTypeId, removedAttributeId));
      attributesDiff.getRemovedFunction().forEach(removedAttributeId -> functionFacade.onDeleteLinkTypeFunction(linkTypeId, removedAttributeId));

      attributesDiff.getCreatedFunction().forEach(attribute -> tasks.add(functionFacade.createTaskForCreatedLinkFunction(updateLinkType.getLinkType(), attribute)));
      attributesDiff.getUpdatedFunction().forEach(attribute -> tasks.add(functionFacade.createTaskForUpdatedLinkFunction(updateLinkType.getLinkType(), attribute)));

      processTasks(tasks.toArray(new Task[0]));
   }

   public void onRemoveLinkType(@Observes final RemoveLinkType removeLinkType) {
      if (removeLinkType.getLinkType() == null) {
         return;
      }

      functionFacade.onDeleteLinkType(removeLinkType.getLinkType().getId());
   }

   private AttributesDiff checkAttributesDiff(java.util.Collection<Attribute> originalAttributes, java.util.Collection<Attribute> currentAttributes) {
      Map<String, Attribute> originalAttributesMap = convertAttributesToMap(originalAttributes);
      Map<String, Attribute> currentAttributesMap = convertAttributesToMap(currentAttributes);
      Set<String> allAttributeIds = new HashSet<>(originalAttributesMap.keySet());
      allAttributeIds.addAll(currentAttributesMap.keySet());

      List<Attribute> createdFunction = new ArrayList<>();
      List<Attribute> updatedFunction = new ArrayList<>();
      List<String> removedFunction = new ArrayList<>();
      List<String> removedIds = new ArrayList<>();

      for (String attributeId : allAttributeIds) {
         if (originalAttributesMap.containsKey(attributeId) && !currentAttributesMap.containsKey(attributeId)) {
            removedIds.add(attributeId);
         } else if (!originalAttributesMap.containsKey(attributeId) && currentAttributesMap.containsKey(attributeId)) {
            Attribute attribute = currentAttributesMap.get(attributeId);
            if (attribute.getFunction() != null && attribute.getFunction().getJs() != null) {
               createdFunction.add(attribute);
            }
         } else {
            Attribute originalAttribute = originalAttributesMap.get(attributeId);
            Attribute currentAttribute = currentAttributesMap.get(attributeId);

            if (!functionIsDefined(originalAttribute) && functionIsDefined(currentAttribute)) {
               createdFunction.add(currentAttribute);
            } else if (functionIsDefined(originalAttribute) && !functionIsDefined(currentAttribute)) {
               removedFunction.add(attributeId);
            } else if (functionIsDefined(originalAttribute) && functionIsDefined(currentAttribute)) {
               if (!originalAttribute.getFunction().getXml().equals(currentAttribute.getFunction().getXml())) {
                  updatedFunction.add(currentAttribute);
               }
            }
         }

      }

      return new AttributesDiff(createdFunction, updatedFunction, removedFunction, removedIds);
   }

   private boolean functionIsDefined(Attribute attribute) {
      return attribute.getFunction() != null && attribute.getFunction().getJs() != null && !attribute.getFunction().getJs().isEmpty();
   }

   private Map<String, Attribute> convertAttributesToMap(java.util.Collection<Attribute> attributes) {
      if (attributes == null) {
         return new HashMap<>();
      }
      return new HashMap<>(attributes.stream().collect(Collectors.toMap(Attribute::getId, a -> a)));
   }

   private static class AttributesDiff {
      private final List<Attribute> createdFunction;
      private final List<Attribute> updatedFunction;
      private final List<String> removedFunction;
      private final List<String> removedIds;

      public AttributesDiff(final List<Attribute> createdFunction, final List<Attribute> updatedFunction, final List<String> removedFunction, final List<String> removedIds) {
         this.createdFunction = createdFunction;
         this.updatedFunction = updatedFunction;
         this.removedFunction = removedFunction;
         this.removedIds = removedIds;
      }

      public List<Attribute> getCreatedFunction() {
         return createdFunction;
      }

      public List<Attribute> getUpdatedFunction() {
         return updatedFunction;
      }

      public List<String> getRemovedFunction() {
         return removedFunction;
      }

      public List<String> getRemovedIds() {
         return removedIds;
      }
   }
}
