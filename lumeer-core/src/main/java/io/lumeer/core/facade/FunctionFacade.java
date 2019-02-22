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
import io.lumeer.api.model.function.FunctionResourceType;
import io.lumeer.api.model.function.FunctionRow;
import io.lumeer.core.task.ContextualTaskFactory;
import io.lumeer.core.task.FunctionTask;
import io.lumeer.core.util.FunctionXmlParser;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FunctionDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
   private DataDao dataDao;

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

      Set<Document> documents = getDocumentsByCollection(collection.getId());
      FunctionTask task = createCollectionTask(collection, attribute, documents);
      if (task != null) {
         task.process();
      }
   }

   private Set<Document> getDocumentsByCollection(String collectionId) {
      final List<Document> documentsByCollection = documentDao.getDocumentsByCollection(collectionId);
      Map<String, DataDocument> data = dataDao.getData(collectionId).stream().collect(Collectors.toMap(DataDocument::getId, d -> d));
      return documentsByCollection.stream().peek(document -> document.setData(data.get(document.getId())))
                                  .collect(Collectors.toSet());
   }

   private List<FunctionRow> createCollectionRowsFromXml(Collection collection, Attribute attribute) {
      return FunctionXmlParser.parseFunctionXml(attribute.getFunction().getXml()).stream()
                              .map(reference -> FunctionRow.createForCollection(collection.getId(), attribute.getId(), reference.getCollectionId(), reference.getLinkTypeId(), reference.getAttributeId()))
                              .collect(Collectors.toList());
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

      Set<LinkInstance> linkInstances = new HashSet<>(linkInstanceDao.getLinkInstancesByLinkType(linkType.getId()));
      // TODO set link instance data when implemented
      FunctionTask task = createLinkTypeTask(linkType, attribute, linkInstances);
      if (task != null) {
         task.process();
      }
   }

   private List<FunctionRow> createLinkRowsFromXml(LinkType linkType, Attribute attribute) {
      return FunctionXmlParser.parseFunctionXml(attribute.getFunction().getXml()).stream()
                              .map(reference -> FunctionRow.createForLink(linkType.getId(), attribute.getId(), reference.getCollectionId(), reference.getLinkTypeId(), reference.getAttributeId()))
                              .collect(Collectors.toList());
   }

   public void onUpdateLinkTypeFunction(LinkType linkType, Attribute attribute) {
      onDeleteLinkTypeFunction(linkType.getId(), attribute.getId());
      onCreateLinkTypeFunction(linkType, attribute);
   }

   public void onDeleteLinkTypeFunction(String collectionId, String attributeId) {
      functionDao.deleteByLinkType(collectionId, attributeId);
   }

   public void onDocumentValueChanged(String collectionId, String attributeId, String documentId) {
      List<FunctionRow> functionRows = functionDao.searchByDependentCollection(collectionId, attributeId);

      List<FunctionTask> tasks = createTasksForDependentCollection(functionRows, Collections.singleton(documentId));
      tasks.forEach(FunctionTask::process);
   }

   public void onLinkValueChanged(String linkTypeId, String attributeId, String linkInstanceId) {
      List<FunctionRow> functionRows = functionDao.searchByDependentLinkType(linkTypeId, attributeId);

      List<FunctionTask> tasks = createTasksForDependentLinkType(functionRows, Collections.singleton(linkInstanceId));
      tasks.forEach(FunctionTask::process);
   }

   public void onDeleteColection(String collectionId) {
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

   private FunctionTask createCollectionTask(Collection collection, Attribute attribute, Set<Document> documents) {
      return createCollectionTaskRecursive(collection, attribute, documents, new HashSet<>());
   }

   private FunctionTask createCollectionTaskRecursive(Collection collection, Attribute attribute, Set<Document> documents, Set<String> functionResourceIds) {
      if (documents.isEmpty()) {
         return null;
      }

      Set<String> documentIds = documents.stream().map(Document::getId).collect(Collectors.toSet());
      List<FunctionRow> functionRows = functionDao.searchByDependentCollection(collection.getId(), attribute.getId());

      functionResourceIds.add(functionResourceId(FunctionResourceType.COLLECTION, collection.getId(), attribute.getId()));

      final FunctionTask functionTask = contextualTaskFactory.getInstance(FunctionTask.class);
      functionTask.setFunctionTask(attribute, collection, documents, createTasksForDependentCollectionRecursive(functionRows, documentIds, functionResourceIds));
      return functionTask;
   }

   public List<FunctionTask> createTasksForDependentCollection(List<FunctionRow> functionRows, Set<String> documentIds) {
      return createTasksForDependentCollectionRecursive(functionRows, documentIds, new HashSet<>());
   }

   private List<FunctionTask> createTasksForDependentCollectionRecursive(List<FunctionRow> functionRows, Set<String> documentIds, Set<String> functionResourceIds) {
      if (functionRows.isEmpty() || documentIds.isEmpty()) {
         return Collections.emptyList();
      }

      return functionRows.stream().filter(row -> !functionResourceIds.contains(functionResourceId(row.getType(), row.getResourceId(), row.getAttributeId())))
                         .map(row -> {
                            if (row.getType() == FunctionResourceType.COLLECTION) {
                               Collection collection = collectionDao.getCollectionById(row.getResourceId());
                               Attribute attribute = collection.getAttributes().stream().filter(attr -> attr.getId().equals(row.getAttributeId())).findFirst().get();
                               Set<Document> documents = setDataForDocuments(collection.getId(), findDocumentsForRow(row, documentIds));

                               return createCollectionTaskRecursive(collection, attribute, documents, functionResourceIds);
                            } else if (row.getType() == FunctionResourceType.LINK) {
                               LinkType linkType = linkTypeDao.getLinkType(row.getResourceId());
                               Attribute attribute = linkType.getAttributes().stream().filter(attr -> attr.getId().equals(row.getAttributeId())).findFirst().get();
                               Set<LinkInstance> linkInstances = new HashSet<>(linkInstanceDao.getLinkInstancesByDocumentIds(documentIds, row.getDependentLinkTypeId()));
                               // TODO set link instance data when implemented

                               return createLinkTypeTaskRecursive(linkType, attribute, linkInstances, functionResourceIds);
                            }
                            return null;
                         }).filter(Objects::nonNull).collect(Collectors.toList());
   }

   private Set<Document> findDocumentsForRow(FunctionRow row, Set<String> documentIds) {
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

   private Set<Document> setDataForDocuments(String collectionId, Set<Document> documents) {
      if (documents.isEmpty()) {
         return Collections.emptySet();
      }
      Map<String, DataDocument> data = dataDao.getData(collectionId).stream().collect(Collectors.toMap(DataDocument::getId, d -> d));
      return new HashSet<>(documents).stream().peek(document -> document.setData(data.get(document.getId())))
                                     .collect(Collectors.toSet());
   }

   private FunctionTask createLinkTypeTask(LinkType linkType, Attribute attribute, Set<LinkInstance> linkInstances) {
      return createLinkTypeTaskRecursive(linkType, attribute, linkInstances, new HashSet<>());
   }

   private FunctionTask createLinkTypeTaskRecursive(LinkType linkType, Attribute attribute, Set<LinkInstance> linkInstances, Set<String> functionResourceIds) {
      if (linkInstances.isEmpty()) {
         return null;
      }

      Set<String> linkInstanceIds = linkInstances.stream().map(LinkInstance::getId).collect(Collectors.toSet());
      List<FunctionRow> functionRows = functionDao.searchByDependentLinkType(linkType.getId(), attribute.getId());

      functionResourceIds.add(functionResourceId(FunctionResourceType.LINK, linkType.getId(), attribute.getId()));

      final FunctionTask functionTask = contextualTaskFactory.getInstance(FunctionTask.class);
      functionTask.setFunctionTask(attribute, linkType, linkInstances, createTasksForDependentLinkTypeRecursive(functionRows, linkInstanceIds, functionResourceIds));
      return functionTask;
   }

   public List<FunctionTask> createTasksForDependentLinkType(List<FunctionRow> functionRows, Set<String> linkInstanceIds) {
      return createTasksForDependentLinkTypeRecursive(functionRows, linkInstanceIds, new HashSet<>());
   }

   private List<FunctionTask> createTasksForDependentLinkTypeRecursive(List<FunctionRow> functionRows, Set<String> linkInstanceIds, Set<String> functionResourceIds) {
      if (functionRows.isEmpty() || linkInstanceIds.isEmpty()) {
         return Collections.emptyList();
      }

      return functionRows.stream().filter(row -> !functionResourceIds.contains(functionResourceId(row.getType(), row.getResourceId(), row.getAttributeId())))
                         .map(row -> {
                            if (row.getType() == FunctionResourceType.COLLECTION) {
                               Collection collection = collectionDao.getCollectionById(row.getResourceId());
                               Attribute attribute = collection.getAttributes().stream().filter(attr -> attr.getId().equals(row.getAttributeId())).findFirst().get();
                               Set<Document> documents = setDataForDocuments(collection.getId(), findDocumentsForRowByLinkInstances(row, linkInstanceIds));

                               return createCollectionTaskRecursive(collection, attribute, documents, functionResourceIds);
                            } else if (row.getType() == FunctionResourceType.LINK) {
                               LinkType linkType = linkTypeDao.getLinkType(row.getResourceId());
                               Attribute attribute = linkType.getAttributes().stream().filter(attr -> attr.getId().equals(row.getAttributeId())).findFirst().get();

                               if (row.getDependentLinkTypeId() == null || row.getDependentLinkTypeId().equals(row.getResourceId())) {
                                  Set<LinkInstance> linkInstances = new HashSet<>(linkInstanceDao.getLinkInstances(linkInstanceIds));
                                  // TODO set link instance data when implemented

                                  return createLinkTypeTaskRecursive(linkType, attribute, linkInstances, functionResourceIds);
                               }
                            }
                            return null;
                         }).filter(Objects::nonNull).collect(Collectors.toList());
   }

   private Set<Document> findDocumentsForRowByLinkInstances(FunctionRow row, Set<String> linkInstanceIds) {
      List<LinkInstance> linkInstances = linkInstanceDao.getLinkInstances(linkInstanceIds);
      Set<String> documentIdsFromLinks = linkInstances.stream().map(LinkInstance::getDocumentIds).flatMap(java.util.Collection::stream).collect(Collectors.toSet());
      List<Document> documentsFromLinks = documentIdsFromLinks.isEmpty() ? Collections.emptyList() : documentDao.getDocumentsByIds(documentIdsFromLinks.toArray(new String[0]));
      return documentsFromLinks.stream().filter(document -> document.getCollectionId().equals(row.getResourceId())).collect(Collectors.toSet());
   }

   private String functionResourceId(FunctionResourceType type, String resourceId, String attributeId) {
      return type.toString() + ":" + resourceId + ":" + attributeId;
   }

}