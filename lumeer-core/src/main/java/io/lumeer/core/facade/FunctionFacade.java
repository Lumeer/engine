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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.model.function.FunctionParameter;
import io.lumeer.api.model.function.FunctionResourceType;
import io.lumeer.api.model.function.FunctionRow;
import io.lumeer.core.task.ContextualTaskFactory;
import io.lumeer.core.task.FunctionTask;
import io.lumeer.core.util.FunctionOrder;
import io.lumeer.core.util.FunctionXmlParser;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FunctionDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;

import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class FunctionFacade extends AbstractFacade {

   @Inject
   private FunctionDao functionDao;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private ContextualTaskFactory contextualTaskFactory;

   public void onCreateCollectionFunction(Collection collection, Attribute attribute) {
      List<FunctionRow> functionRows = createCollectionRowsFromXml(collection, attribute);
      if (!functionRows.isEmpty()) {
         functionDao.createRows(functionRows);
      }

      Deque<FunctionParameterDocuments> queue = createQueueForCollection(collection, attribute, functionRows);
      convertQueueToTaskAndExecute(queue);
   }

   private List<FunctionRow> createCollectionRowsFromXml(Collection collection, Attribute attribute) {
      return FunctionXmlParser.parseFunctionXml(attribute.getFunction().getXml()).stream()
                              .map(reference -> FunctionRow.createForCollection(collection.getId(), attribute.getId(), reference.getCollectionId(), reference.getLinkTypeId(), reference.getAttributeId()))
                              .collect(Collectors.toList());
   }

   public Deque<FunctionParameterDocuments> createQueueForCollection(Collection collection, Attribute attribute, List<FunctionRow> functionRows) {
      Set<Document> documents = new HashSet<>(documentDao.getDocumentsByCollection(collection.getId()));
      if (documents.isEmpty()) {
         return new LinkedList<>();
      }

      FunctionParameterDocuments parameter = new FunctionParameterDocuments(FunctionResourceType.COLLECTION, collection.getId(), attribute.getId());
      parameter.setDocuments(documents);
      parameter.setCollection(collection);
      parameter.setAttribute(attribute);

      Map<FunctionParameterDocuments, List<FunctionParameterDocuments>> parametersMap = new HashMap<>();
      parametersMap.put(parameter, functionRows.stream().map(this::functionRowToParameter).collect(Collectors.toList()));

      fillParametersMapForCollection(parametersMap, parameter);

      return orderFunctions(parametersMap);
   }

   private Deque<FunctionParameterDocuments> orderFunctions(Map<FunctionParameterDocuments, List<FunctionParameterDocuments>> parametersMap) {
      Deque<FunctionParameterDocuments> queue = FunctionOrder.orderFunctions(parametersMap);

      Deque<FunctionParameterDocuments> mappedQueue = new LinkedList<>();
      for (final FunctionParameterDocuments functionParameterDocuments : queue) {
         Optional<FunctionParameterDocuments> first = parametersMap.keySet().stream().filter(param -> param.equals(functionParameterDocuments)).findFirst();
         first.ifPresent(mappedQueue::add);
      }

      return mappedQueue;
   }

   private void fillParametersMapForCollection(Map<FunctionParameterDocuments, List<FunctionParameterDocuments>> parametersMap, FunctionParameterDocuments parentParameter) {
      List<FunctionRow> functionRows = functionDao.searchByDependentCollection(parentParameter.getResourceId(), parentParameter.getAttributeId());

      functionRows.forEach(row -> {
         List<FunctionRow> rows = functionDao.searchByResource(row.getResourceId(), row.getAttributeId(), row.getType());
         Set<String> documentIds = parentParameter.getDocuments().stream().map(Document::getId).collect(Collectors.toSet());

         FunctionParameterDocuments parameter = new FunctionParameterDocuments(row.getType(), row.getResourceId(), row.getAttributeId());
         if (!parametersMap.containsKey(parameter)) {
            if (row.getType() == FunctionResourceType.COLLECTION) {
               Set<Document> documents = findDocumentsForRow(row, documentIds);
               if (!documents.isEmpty()) {
                  parameter.setDocuments(documents);
                  parametersMap.put(parameter, rows.stream().map(this::functionRowToParameter).collect(Collectors.toList()));
                  fillParametersMapForCollection(parametersMap, parameter);
               }
            } else {
               Set<LinkInstance> linkInstances = new HashSet<>(linkInstanceDao.getLinkInstancesByDocumentIds(documentIds, row.getDependentLinkTypeId()));
               if (!linkInstances.isEmpty()) {
                  parameter.setLinkInstances(linkInstances);
                  parametersMap.put(parameter, rows.stream().map(this::functionRowToParameter).collect(Collectors.toList()));
                  fillParametersMapForLinkType(parametersMap, parameter);
               }
            }
         }
      });

   }

   private void fillParametersMapForLinkType(Map<FunctionParameterDocuments, List<FunctionParameterDocuments>> parametersMap, FunctionParameterDocuments parentParameter) {
      List<FunctionRow> functionRows = functionDao.searchByDependentLinkType(parentParameter.getResourceId(), parentParameter.getAttributeId());

      functionRows.forEach(row -> {
         List<FunctionRow> rows = functionDao.searchByResource(row.getResourceId(), row.getAttributeId(), row.getType());
         Set<String> linkInstanceIds = parentParameter.getLinkInstances().stream().map(LinkInstance::getId).collect(Collectors.toSet());

         FunctionParameterDocuments parameter = new FunctionParameterDocuments(row.getType(), row.getResourceId(), row.getAttributeId());
         if (!parametersMap.containsKey(parameter)) {
            if (row.getType() == FunctionResourceType.COLLECTION) {
               Set<Document> documents = findDocumentsForRowByLinkInstances(row, linkInstanceIds);
               if (!documents.isEmpty()) {
                  parameter.setDocuments(documents);
                  parametersMap.put(parameter, rows.stream().map(this::functionRowToParameter).collect(Collectors.toList()));
                  fillParametersMapForCollection(parametersMap, parameter);
               }
            } else if (row.getDependentLinkTypeId() == null || row.getDependentLinkTypeId().equals(row.getResourceId())) {
               Set<LinkInstance> linkInstances = new HashSet<>(linkInstanceDao.getLinkInstances(linkInstanceIds));
               if (!linkInstances.isEmpty()) {
                  parameter.setLinkInstances(linkInstances);
                  parametersMap.put(parameter, rows.stream().map(this::functionRowToParameter).collect(Collectors.toList()));
                  fillParametersMapForLinkType(parametersMap, parameter);
               }
            }
         }
      });
   }

   private void convertQueueToTaskAndExecute(final Deque<FunctionParameterDocuments> queue) {
      FunctionTask task = convertQueueToTask(queue);

      if (task != null) {
         task.process();
      }
   }

   public FunctionTask convertQueueToTask(final Deque<FunctionParameterDocuments> queue) {
      Set<String> collectionIds = queue.stream().filter(q -> q.getType() == FunctionResourceType.COLLECTION && q.getCollection() == null).map(FunctionParameter::getResourceId).collect(Collectors.toSet());
      Set<String> linkTypeIds = queue.stream().filter(q -> q.getType() == FunctionResourceType.LINK && q.getLinkType() == null).map(FunctionParameter::getResourceId).collect(Collectors.toSet());

      Map<String, Collection> collectionMap = collectionIds.size() > 0 ? collectionDao.getCollectionsByIds(collectionIds).stream().collect(Collectors.toMap(Resource::getId, c -> c)) : new HashMap<>();
      Map<String, LinkType> linkTypeMap = linkTypeIds.size() > 0 ? linkTypeDao.getLinkTypesByIds(linkTypeIds).stream().collect(Collectors.toMap(LinkType::getId, c -> c)) : new HashMap<>();

      FunctionTask task = null;

      final Iterator<FunctionParameterDocuments> iterator = queue.descendingIterator();
      while (iterator.hasNext()) {
         final FunctionParameterDocuments parameter = iterator.next();
         if (parameter.getType() == FunctionResourceType.COLLECTION) {
            Collection collection = parameter.getCollection() != null ? parameter.getCollection() : collectionMap.get(parameter.getResourceId());
            Attribute attribute = parameter.getAttribute() != null ? parameter.getAttribute() : findAttributeInCollection(collection, parameter.getAttributeId());

            if (collection != null && attribute != null) {
               FunctionTask functionTask = contextualTaskFactory.getInstance(FunctionTask.class);
               functionTask.setFunctionTask(attribute, collection, parameter.getDocuments(), createParentTasks(task));
               task = functionTask;
            }
         } else if (parameter.getType() == FunctionResourceType.LINK) {
            LinkType linkType = parameter.getLinkType() != null ? parameter.getLinkType() : linkTypeMap.get(parameter.getResourceId());
            Attribute attribute = parameter.getAttribute() != null ? parameter.getAttribute() : findAttributeInLinkType(linkType, parameter.getAttributeId());

            if (linkType != null && attribute != null) {
               FunctionTask functionTask = contextualTaskFactory.getInstance(FunctionTask.class);
               functionTask.setFunctionTask(attribute, linkType, parameter.getLinkInstances(), createParentTasks(task));
               task = functionTask;
            }
         }

      }

      return task;
   }

   private List<FunctionTask> createParentTasks(FunctionTask task) {
      if (task == null) {
         return null;
      }

      FunctionTask functionTask = contextualTaskFactory.getInstance(FunctionTask.class);
      if (task.getCollection() != null) {
         functionTask.setFunctionTask(task.getAttribute(), task.getCollection(), task.getDocuments(), task.getParents());
         return Collections.singletonList(functionTask);

      } else if (task.getLinkType() != null) {
         functionTask.setFunctionTask(task.getAttribute(), task.getLinkType(), task.getLinkInstances(), task.getParents());
         return Collections.singletonList(functionTask);
      }

      return null;
   }

   private Attribute findAttributeInCollection(Collection collection, String attributeId) {
      if (collection == null || collection.getAttributes() == null) {
         return null;
      }

      return findAttribute(collection.getAttributes(), attributeId);
   }

   private Attribute findAttributeInLinkType(LinkType linkType, String attributeId) {
      if (linkType == null || linkType.getAttributes() == null) {
         return null;
      }

      return findAttribute(linkType.getAttributes(), attributeId);
   }

   private Attribute findAttribute(java.util.Collection<Attribute> attributes, String attributeId) {
      for (Attribute attribute : attributes) {
         if (attribute.getId().equals(attributeId)) {
            return attribute;
         }
      }
      return null;
   }

   private FunctionParameterDocuments functionRowToParameter(FunctionRow row) {
      String resourceId = row.getDependentCollectionId() != null ? row.getDependentCollectionId() : row.getDependentLinkTypeId();
      FunctionResourceType type = row.getDependentCollectionId() != null ? FunctionResourceType.COLLECTION : FunctionResourceType.LINK;
      return new FunctionParameterDocuments(type, resourceId, row.getDependentAttributeId());
   }

   public void onUpdateCollectionFunction(Collection collection, Attribute attribute) {
      onDeleteCollectionFunction(collection.getId(), attribute.getId());
      onCreateCollectionFunction(collection, attribute);
   }

   public void onDeleteCollectionFunction(String collectionId, String attributeId) {
      functionDao.deleteByCollection(collectionId, attributeId);
   }

   public void onCreateLinkTypeFunction(LinkType linkType, Attribute attribute) {
      List<FunctionRow> functionRows = createLinkRowsFromXml(linkType, attribute);
      if (!functionRows.isEmpty()) {
         functionDao.createRows(functionRows);
      }

      Deque<FunctionParameterDocuments> queue = createQueueForLinkType(linkType, attribute, functionRows);
      convertQueueToTaskAndExecute(queue);
   }

   private List<FunctionRow> createLinkRowsFromXml(LinkType linkType, Attribute attribute) {
      return FunctionXmlParser.parseFunctionXml(attribute.getFunction().getXml()).stream()
                              .map(reference -> FunctionRow.createForLink(linkType.getId(), attribute.getId(), reference.getCollectionId(), reference.getLinkTypeId(), reference.getAttributeId()))
                              .collect(Collectors.toList());
   }

   public Deque<FunctionParameterDocuments> createQueueForLinkType(LinkType linkType, Attribute attribute, List<FunctionRow> functionRows) {
      Set<LinkInstance> linkInstances = new HashSet<>(linkInstanceDao.getLinkInstancesByLinkType(linkType.getId()));
      if (linkInstances.isEmpty()) {
         return new LinkedList<>();
      }

      FunctionParameterDocuments parameter = new FunctionParameterDocuments(FunctionResourceType.LINK, linkType.getId(), attribute.getId());
      parameter.setLinkInstances(linkInstances);
      parameter.setLinkType(linkType);
      parameter.setAttribute(attribute);

      Map<FunctionParameterDocuments, List<FunctionParameterDocuments>> parametersMap = new HashMap<>();
      parametersMap.put(parameter, functionRows.stream().map(this::functionRowToParameter).collect(Collectors.toList()));

      fillParametersMapForLinkType(parametersMap, parameter);

      return orderFunctions(parametersMap);
   }

   public void onUpdateLinkTypeFunction(LinkType linkType, Attribute attribute) {
      onDeleteLinkTypeFunction(linkType.getId(), attribute.getId());
      onCreateLinkTypeFunction(linkType, attribute);
   }

   public void onDeleteLinkTypeFunction(String collectionId, String attributeId) {
      functionDao.deleteByLinkType(collectionId, attributeId);
   }

   public void onDocumentCreated(Collection collection, Document document) {
      Deque<FunctionParameterDocuments> queue = createQueueForCreatedDocument(collection, document);
      convertQueueToTaskAndExecute(queue);
   }

   public Deque<FunctionParameterDocuments> createQueueForCreatedDocument(Collection collection, Document document) {
      List<Attribute> attributes = collection.getAttributes().stream().filter(attribute -> attribute.getFunction() != null).collect(Collectors.toList());

      Map<FunctionParameterDocuments, List<FunctionParameterDocuments>> parametersMap = new HashMap<>();

      attributes.forEach(attribute -> {
         FunctionParameterDocuments parameter = new FunctionParameterDocuments(FunctionResourceType.COLLECTION, collection.getId(), attribute.getId());
         if (!parametersMap.containsKey(parameter)) {
            parameter.setDocuments(Collections.singleton(document));
            parameter.setCollection(collection);
            parameter.setAttribute(attribute);

            List<FunctionRow> functionRows = functionDao.searchByResource(collection.getId(), attribute.getId(), FunctionResourceType.COLLECTION);
            if (!functionRows.isEmpty()) {
               parametersMap.put(parameter, functionRows.stream().map(this::functionRowToParameter).collect(Collectors.toList()));

               fillParametersMapForCollection(parametersMap, parameter);
            }
         }
      });

      return orderFunctions(parametersMap);
   }

   public void onDocumentValueChanged(String collectionId, String attributeId, String documentId) {
      Deque<FunctionParameterDocuments> queue = createQueueForDocumentChanged(collectionId, attributeId, documentId);
      convertQueueToTaskAndExecute(queue);
   }

   public Deque<FunctionParameterDocuments> createQueueForDocumentChanged(String collectionId, String attributeId, String documentId) {
      Map<FunctionParameterDocuments, List<FunctionParameterDocuments>> parametersMap = new HashMap<>();
      List<FunctionRow> functionRows = functionDao.searchByDependentCollection(collectionId, attributeId);

      functionRows.forEach(row -> {
         FunctionParameterDocuments parameter = new FunctionParameterDocuments(row.getType(), row.getResourceId(), row.getAttributeId());
         List<FunctionRow> rows = functionDao.searchByResource(row.getResourceId(), row.getAttributeId(), row.getType());
         if (!parametersMap.containsKey(parameter)) {
            if (row.getType() == FunctionResourceType.COLLECTION) {
               Set<Document> documents = findDocumentsForRow(row, Collections.singleton(documentId));
               if (!documents.isEmpty()) {
                  parameter.setDocuments(documents);
                  parametersMap.put(parameter, rows.stream().map(this::functionRowToParameter).collect(Collectors.toList()));
                  fillParametersMapForCollection(parametersMap, parameter);
               }
            } else {
               Set<LinkInstance> linkInstances = new HashSet<>(linkInstanceDao.getLinkInstancesByDocumentIds(Collections.singleton(documentId), row.getDependentLinkTypeId()));
               if (!linkInstances.isEmpty()) {
                  parameter.setLinkInstances(linkInstances);
                  parametersMap.put(parameter, rows.stream().map(this::functionRowToParameter).collect(Collectors.toList()));
                  fillParametersMapForLinkType(parametersMap, parameter);
               }
            }
         }
      });

      return orderFunctions(parametersMap);
   }

   public void onLinkCreated(LinkType linkType, LinkInstance linkInstance) {
      Deque<FunctionParameterDocuments> queue = createQueueForCreatedLink(linkType, linkInstance);
      convertQueueToTaskAndExecute(queue);
   }

   public Deque<FunctionParameterDocuments> createQueueForCreatedLink(LinkType linkType, LinkInstance linkInstance) {
      List<Attribute> attributes = linkType.getAttributes().stream().filter(attribute -> attribute.getFunction() != null).collect(Collectors.toList());

      Map<FunctionParameterDocuments, List<FunctionParameterDocuments>> parametersMap = new HashMap<>();

      attributes.forEach(attribute -> {
         FunctionParameterDocuments parameter = new FunctionParameterDocuments(FunctionResourceType.LINK, linkType.getId(), attribute.getId());
         if (!parametersMap.containsKey(parameter)) {
            parameter.setLinkInstances(Collections.singleton(linkInstance));
            parameter.setLinkType(linkType);
            parameter.setAttribute(attribute);

            List<FunctionRow> functionRows = functionDao.searchByResource(linkType.getId(), attribute.getId(), FunctionResourceType.LINK);
            if (!functionRows.isEmpty()) {
               parametersMap.put(parameter, functionRows.stream().map(this::functionRowToParameter).collect(Collectors.toList()));

               fillParametersMapForLinkType(parametersMap, parameter);
            }
         }
      });

      List<FunctionRow> dependentRows = functionDao.searchByDependentLinkType(linkType.getId(), null);
      dependentRows.forEach(row -> {
         FunctionParameterDocuments parameter = new FunctionParameterDocuments(row.getType(), row.getResourceId(), row.getAttributeId());
         List<FunctionRow> rows = functionDao.searchByResource(row.getResourceId(), row.getAttributeId(), row.getType());

         if (!parametersMap.containsKey(parameter)) {
            if (row.getType() == FunctionResourceType.COLLECTION) {
               Set<Document> documents = findDocumentsForRowByLinkInstances(row, Collections.singleton(linkInstance.getId()));
               if (!documents.isEmpty()) {
                  parameter.setDocuments(documents);
                  parametersMap.put(parameter, rows.stream().map(this::functionRowToParameter).collect(Collectors.toList()));
                  fillParametersMapForCollection(parametersMap, parameter);
               }
            } else if (row.getDependentLinkTypeId() == null || row.getDependentLinkTypeId().equals(row.getResourceId())) {
               parameter.setLinkInstances(Collections.singleton(linkInstance));
               parametersMap.put(parameter, rows.stream().map(this::functionRowToParameter).collect(Collectors.toList()));
               fillParametersMapForLinkType(parametersMap, parameter);
            }

            fillParametersMapForLinkType(parametersMap, parameter);
         }

      });

      return orderFunctions(parametersMap);
   }

   public void onLinkValueChanged(String linkTypeId, String attributeId, String linkInstanceId) {
      Deque<FunctionParameterDocuments> queue = createQueueForLinkChanged(linkTypeId, attributeId, linkInstanceId);
      convertQueueToTaskAndExecute(queue);
   }

   public Deque<FunctionParameterDocuments> createQueueForLinkChanged(String linkTypeId, String attributeId, String linkInstanceId) {
      Map<FunctionParameterDocuments, List<FunctionParameterDocuments>> parametersMap = new HashMap<>();
      List<FunctionRow> functionRows = functionDao.searchByDependentLinkType(linkTypeId, attributeId);

      functionRows.forEach(row -> {
         FunctionParameterDocuments parameter = new FunctionParameterDocuments(row.getType(), row.getResourceId(), row.getAttributeId());
         List<FunctionRow> rows = functionDao.searchByResource(row.getResourceId(), row.getAttributeId(), row.getType());
         if (!parametersMap.containsKey(parameter)) {
            if (row.getType() == FunctionResourceType.COLLECTION) {
               Set<Document> documents = findDocumentsForRowByLinkInstances(row, Collections.singleton(linkInstanceId));
               if (!documents.isEmpty()) {
                  parameter.setDocuments(documents);
                  parametersMap.put(parameter, rows.stream().map(this::functionRowToParameter).collect(Collectors.toList()));
                  fillParametersMapForCollection(parametersMap, parameter);
               }
            } else if (row.getDependentLinkTypeId() == null || row.getDependentLinkTypeId().equals(row.getResourceId())) {
               parameter.setLinkInstances(Collections.singleton(linkInstanceDao.getLinkInstance(linkInstanceId)));
               parametersMap.put(parameter, rows.stream().map(this::functionRowToParameter).collect(Collectors.toList()));
               fillParametersMapForLinkType(parametersMap, parameter);
            }

            fillParametersMapForLinkType(parametersMap, parameter);
         }
      });

      return orderFunctions(parametersMap);
   }

   public void onDeleteCollection(String collectionId) {
      List<FunctionRow> functionRows = functionDao.searchByAnyCollection(collectionId, null);

      deleteByRows(FunctionResourceType.COLLECTION, functionRows);
   }

   public void onDeleteLinkType(String linkTypeId) {
      List<FunctionRow> functionRows = functionDao.searchByAnyLinkType(linkTypeId, null);

      deleteByRows(FunctionResourceType.LINK, functionRows);
   }

   public void onDeleteCollectionAttribute(String collectionId, String attributeId) {
      List<FunctionRow> functionRows = functionDao.searchByAnyCollection(collectionId, attributeId);

      deleteByRows(FunctionResourceType.COLLECTION, functionRows);
   }

   public void onDeleteLinkAttribute(String linkTypeId, String attributeId) {
      List<FunctionRow> functionRows = functionDao.searchByAnyLinkType(linkTypeId, attributeId);

      deleteByRows(FunctionResourceType.LINK, functionRows);
   }

   private void deleteByRows(FunctionResourceType type, List<FunctionRow> functionRows) {
      String[] resourceIdsToDelete = functionRows.stream().map(FunctionRow::getResourceId).toArray(String[]::new);
      functionDao.deleteByResources(type, resourceIdsToDelete);
   }

   private Set<Document> findDocumentsForRow(FunctionRow row, Set<String> documentIds) {
      if (documentIds.isEmpty()) {
         return Collections.emptySet();
      }
      if (row.getResourceId().equals(row.getDependentCollectionId())) {
         return new HashSet<>(documentDao.getDocumentsByIds(documentIds.toArray(new String[0])));
      }
      if (row.getDependentLinkTypeId() != null) {
         List<LinkInstance> linkInstances = linkInstanceDao.getLinkInstancesByDocumentIds(documentIds, row.getDependentLinkTypeId());
         Set<String> documentIdsFromLinks = linkInstances.stream().map(LinkInstance::getDocumentIds).flatMap(java.util.Collection::stream).collect(Collectors.toSet());
         documentIdsFromLinks.removeAll(documentIds);
         if (!documentIdsFromLinks.isEmpty()) {
            return new HashSet<>(documentDao.getDocumentsByIds(documentIdsFromLinks.toArray(new String[0])));
         }
      }
      return Collections.emptySet();
   }

   private Set<Document> findDocumentsForRowByLinkInstances(FunctionRow row, Set<String> linkInstanceIds) {
      if (linkInstanceIds.isEmpty()) {
         return Collections.emptySet();
      }
      List<LinkInstance> linkInstances = linkInstanceDao.getLinkInstances(linkInstanceIds);
      Set<String> documentIdsFromLinks = linkInstances.stream().map(LinkInstance::getDocumentIds).flatMap(java.util.Collection::stream).collect(Collectors.toSet());
      List<Document> documentsFromLinks = documentIdsFromLinks.isEmpty() ? Collections.emptyList() : documentDao.getDocumentsByIds(documentIdsFromLinks.toArray(new String[0]));
      return documentsFromLinks.stream().filter(document -> document.getCollectionId().equals(row.getResourceId())).collect(Collectors.toSet());
   }

   static class FunctionParameterDocuments extends FunctionParameter {

      private Set<Document> documents;
      private Set<LinkInstance> linkInstances;
      private Collection collection;
      private LinkType linkType;
      private Attribute attribute;

      public FunctionParameterDocuments(final FunctionResourceType type, final String resourceId, final String attributeId) {
         super(type, resourceId, attributeId);
      }

      public Set<Document> getDocuments() {
         return documents;
      }

      public void setDocuments(final Set<Document> documents) {
         this.documents = documents;
      }

      public Set<LinkInstance> getLinkInstances() {
         return linkInstances;
      }

      public void setLinkInstances(final Set<LinkInstance> linkInstances) {
         this.linkInstances = linkInstances;
      }

      public Collection getCollection() {
         return collection;
      }

      public void setCollection(final Collection collection) {
         this.collection = collection;
      }

      public LinkType getLinkType() {
         return linkType;
      }

      public void setLinkType(final LinkType linkType) {
         this.linkType = linkType;
      }

      public Attribute getAttribute() {
         return attribute;
      }

      public void setAttribute(final Attribute attribute) {
         this.attribute = attribute;
      }
   }
}