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

import static java.util.stream.Collectors.*;

import io.lumeer.api.SelectedWorkspace;
import io.lumeer.api.model.AllowedPermissions;
import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.CollectionPurposeType;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Language;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.WithId;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.FunctionFacade;
import io.lumeer.core.facade.TaskProcessingFacade;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.facade.detector.PurposeChangeProcessor;
import io.lumeer.core.js.JsEngineFactory;
import io.lumeer.core.task.ContextualTask;
import io.lumeer.core.task.FunctionTask;
import io.lumeer.core.task.PrintRequest;
import io.lumeer.core.task.RuleTask;
import io.lumeer.core.task.Task;
import io.lumeer.core.task.TaskExecutor;
import io.lumeer.core.task.UserMessage;
import io.lumeer.core.util.DocumentUtils;
import io.lumeer.core.util.LinkTypeUtils;
import io.lumeer.core.util.MomentJsParser;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.CreateDocument;
import io.lumeer.engine.api.event.CreateLinkInstance;
import io.lumeer.engine.api.event.UpdateDocument;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SearchQueryStem;

import org.apache.commons.lang3.StringUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JsExecutor {

   private static final String CREATE_PREFIX = "CREATE_";
   private static final String MOMENT_JS_SIGNATURE = "/** MomentJs **/";

   private LumeerBridge lumeerBridge;
   private boolean dryRun = false;
   private static final Engine engine = JsEngineFactory.getEngine();
   private static final String momentJsCode = MomentJsParser.getMomentJsCode();

   public static class LumeerBridge {

      private static final DefaultConfigurationProducer configurationProducer = new DefaultConfigurationProducer();
      private static final ConstraintManager constraintManager = ConstraintManager.getInstance(configurationProducer);
      private final ContextualTask task;
      private final Collection collection;
      private final ChangesTracker changesTracker = new ChangesTracker();
      @SuppressWarnings("rawtypes")
      private Set<Change> changes = new HashSet<>();
      private Exception cause = null;
      private boolean dryRun = false;
      private boolean printed = false;

      private LumeerBridge(final ContextualTask task, final Collection collection) {
         this.task = task;
         this.collection = collection;
      }

      @SuppressWarnings("unused")
      public String getSequenceNumber(final String sequenceName, final int digits) {
         final String format = "%" + (digits <= 1 ? "" : "0" + digits) + "d";

         if (dryRun) {
            return String.format(format, 1);
         } else {
            final int sequenceValue = task.getDaoContextSnapshot().getSequenceDao().getNextSequenceNo(sequenceName);
            changesTracker.addSequence(sequenceName);

            return String.format(format, sequenceValue);
         }
      }

      public String getCurrentUser() {
         final String email = task.getInitiator().getEmail();
         return email == null ? "" : email;
      }

      @SuppressWarnings("unused")
      public String getCurrentLocale() {
         return task.getCurrentLocale();
      }

      @SuppressWarnings("unused")
      public void showMessage(final String type, final String message) {
         if (task.getDaoContextSnapshot().increaseMessageCounter() <= Task.MAX_MESSAGES) {
            changes.add(new UserMessageChange(new UserMessage(type, message)));
         }
      }

      public DocumentBridge createDocument(final String collectionId) {
         if (task.getDaoContextSnapshot().increaseCreationCounter() <= Task.MAX_CREATED_AND_DELETED_DOCUMENTS_AND_LINKS) {
            try {
               final Document d = new Document(collectionId, ZonedDateTime.now(), null, task.getInitiator().getId(), null, 0, null);
               d.createIfAbsentMetaData().put(Document.META_CORRELATION_ID, CREATE_PREFIX + UUID.randomUUID().toString());
               d.setData(new DataDocument());

               changes.add(new DocumentCreation(d));

               return new DocumentBridge(d);
            } catch (Exception e) {
               cause = e;
               throw e;
            }
         } else {
            return null;
         }
      }

      @SuppressWarnings("unused")
      public List<DocumentBridge> readView(final String viewId) {
         try {
            final View view = task.getDaoContextSnapshot().getViewDao().getViewById(viewId);
            final Query query = view.getQuery().getFirstStem(0, Task.MAX_VIEW_DOCUMENTS);
            final Language language = Language.fromString(task.getCurrentLocale());
            final AllowedPermissions permissions = AllowedPermissions.getAllowedPermissions(task.getInitiator().getId(), view.getPermissions());
            final List<Document> documents = DocumentUtils.getDocuments(task.getDaoContextSnapshot(), query, task.getInitiator(), language, permissions);

            return documents.stream().map(DocumentBridge::new).collect(toList());
         } catch (Exception e) {
            cause = e;
            throw e;
         }
      }

      @SuppressWarnings("unused")
      public void setLinkAttribute(final LinkBridge l, final String attrId, final Value value) {
         try {
            changes.add(new LinkChange(l.link, attrId, convertValue(value)));
         } catch (Exception e) {
            cause = e;
            throw e;
         }
      }

      public void setDocumentAttribute(final DocumentBridge d, final String attrId, final Value value) {
         try {
            changes.add(new DocumentChange(d.document, attrId, convertValue(value)));
            if (d.document != null) {
               if (d.document.getData() != null) {
                  d.document.getData().append(attrId, convertValue(value));
               } else {
                  d.document.setData(new DataDocument().append(attrId, convertValue(value)));
               }
            }
         } catch (Exception e) {
            cause = e;
            throw e;
         }
      }

      @SuppressWarnings("unused")
      public void removeDocument(final DocumentBridge d) {
         if (task.getDaoContextSnapshot().increaseDeletionCounter() <= Task.MAX_CREATED_AND_DELETED_DOCUMENTS_AND_LINKS) {
            try {
               changes.add(new DocumentRemoval(d.document));
            } catch (Exception e) {
               cause = e;
               throw e;
            }
         }
      }

      @SuppressWarnings("unused")
      public LinkBridge linkDocuments(final DocumentBridge d1, final DocumentBridge d2, final String linkTypeId) {
         if (task.getDaoContextSnapshot().increaseCreationCounter() <= Task.MAX_CREATED_AND_DELETED_DOCUMENTS_AND_LINKS) {
            try {
               final LinkInstance link = new LinkInstance(linkTypeId, List.of(
                     d1.document.getId() != null ? d1.document.getId() : d1.document.createIfAbsentMetaData().getString(Document.META_CORRELATION_ID),
                     d2.document.getId() != null ? d2.document.getId() : d2.document.createIfAbsentMetaData().getString(Document.META_CORRELATION_ID)
               ));
               link.setTemplateId(CREATE_PREFIX + UUID.randomUUID().toString());
               link.setData(new DataDocument());

               changes.add(new LinkCreation(link));

               return new LinkBridge(link);
            } catch (Exception e) {
               cause = e;
               throw e;
            }
         } else {
            return null;
         }
      }

      @SuppressWarnings("unused")
      public void printAttribute(final DocumentBridge d, final String attrId) {
         final SelectedWorkspace workspace = task.getDaoContextSnapshot().getSelectedWorkspace();
         if (!printed && workspace.getOrganization().isPresent() && workspace.getProject().isPresent()) {
            final PrintRequest pq = new PrintRequest(
                  workspace.getOrganization().get().getCode(),
                  workspace.getProject().get().getCode(),
                  d.document.getCollectionId(),
                  d.document.getId(),
                  attrId,
                  ResourceType.COLLECTION
            );
            changes.add(new PrintAttributeChange(pq));
            printed = true; // we can trigger this only once per rule/function
         }
      }

      @SuppressWarnings("unused")
      public void printAttribute(final LinkBridge l, final String attrId) {
         final SelectedWorkspace workspace = task.getDaoContextSnapshot().getSelectedWorkspace();
         if (!printed && workspace.getOrganization().isPresent() && workspace.getProject().isPresent()) {
            final PrintRequest pq = new PrintRequest(
                  workspace.getOrganization().get().getCode(),
                  workspace.getProject().get().getCode(),
                  l.link.getLinkTypeId(),
                  l.link.getId(),
                  attrId,
                  ResourceType.LINK
            );
            changes.add(new PrintAttributeChange(pq));
            printed = true; // we can trigger this only once per rule/function
         }
      }

      @SuppressWarnings({ "rawtypes", "unchecked" })
      Object convertValue(final Value value) {
         if (value.isNumber()) {
            return value.fitsInLong() ? value.asLong() : value.asDouble();
         } else if (value.isBoolean()) {
            return value.asBoolean();
         } else if (value.isHostObject() && value.asHostObject() instanceof Date) {
            return value.asHostObject();
         } else if (value.isNull()) {
            return null;
         } else if (value.hasArrayElements()) {
            final List list = new ArrayList();
            for (long i = 0; i < value.getArraySize(); i = i + 1) {
               list.add(convertValue(value.getArrayElement(i)));
            }
            return list;
         } else {
            return value.asString();
         }
      }

      private List<LinkInstance> getLinkInstances(final String documentId, final String linkTypeId) {
         final SearchQuery query = SearchQuery
               .createBuilder()
               .stems(Collections.singletonList(
                     SearchQueryStem
                           .createBuilder("")
                           .linkTypeIds(Collections.singletonList(linkTypeId))
                           .documentIds(Set.of(documentId))
                           .build()))
               .build();

         final List<LinkInstance> result = task.getDaoContextSnapshot().getLinkInstanceDao()
                                               .searchLinkInstances(query);
         result.forEach(linkInstance ->
               linkInstance.setData(constraintManager.encodeDataTypesForFce(task.getDaoContextSnapshot().getLinkTypeDao().getLinkType(linkTypeId), linkInstance.getData()))
         );

         return result;
      }

      public List<LinkBridge> getLinks(DocumentBridge d, String linkTypeId) {
         try {
            final List<LinkInstance> links = getLinkInstances(d.document.getId(), linkTypeId);

            // load link data
            if (links.size() > 0) {
               final Map<String, DataDocument> linkData = task.getDaoContextSnapshot().getLinkDataDao().getData(linkTypeId, links.stream().map(LinkInstance::getId).collect(toSet())).stream()
                                                              .collect(Collectors.toMap(DataDocument::getId, data -> data));

               // match link instances with their data and convert to bridge
               return links.stream().map(linkInstance -> {
                  linkInstance.setData(linkData.get(linkInstance.getId()));
                  return new LinkBridge(linkInstance);
               }).collect(toList());
            } else {
               return Collections.emptyList();
            }
         } catch (Exception e) {
            cause = e;
            throw e;
         }
      }

      @SuppressWarnings("unused")
      public DocumentBridge getLinkDocument(final LinkBridge l, final String collectionId) {
         try {
            List<Document> documents = task.getDaoContextSnapshot().getDocumentDao().getDocumentsByIds(l.link.getDocumentIds().toArray(new String[0]));
            if (documents.size() == 2) {
               final Document doc = documents.get(0).getCollectionId().equals(collectionId) ? documents.get(0) : documents.get(1);

               DataDocument data = task.getDaoContextSnapshot().getDataDao().getData(doc.getCollectionId(), doc.getId());
               data = constraintManager.encodeDataTypesForFce(task.getDaoContextSnapshot().getCollectionDao().getCollectionById(collectionId), data);
               doc.setData(data);

               return new DocumentBridge(doc);
            }

            return null;
         } catch (Exception e) {
            cause = e;
            throw e;
         }
      }

      public List<DocumentBridge> getLinkedDocuments(DocumentBridge d, String linkTypeId) {
         try {
            final LinkType linkType = task.getDaoContextSnapshot().getLinkTypeDao().getLinkType(linkTypeId);
            final List<LinkInstance> links = getLinkInstances(d.document.getId(), linkTypeId);
            final String otherCollectionId = linkType.getCollectionIds().get(0).equals(collection.getId()) ?
                  linkType.getCollectionIds().get(1) : linkType.getCollectionIds().get(0);

            // load linked document ids
            if (links.size() > 0) {
               final Set<String> documentIds = links.stream()
                                                    .map(LinkInstance::getDocumentIds)
                                                    .flatMap(java.util.Collection::stream)
                                                    .collect(Collectors.toSet());
               documentIds.remove(d.document.getId());

               // load document data
               final Map<String, DataDocument> data = new HashMap<>();
               task.getDaoContextSnapshot().getDataDao()
                   .getData(otherCollectionId, documentIds)
                   .forEach(dd -> data.put(dd.getId(), dd));

               // load document meta data and match them with user data
               return task.getDaoContextSnapshot().getDocumentDao()
                          .getDocumentsByIds(documentIds.toArray(new String[0]))
                          .stream().map(document -> {
                        DataDocument dd = data.get(document.getId());
                        dd = constraintManager.encodeDataTypesForFce(task.getDaoContextSnapshot().getCollectionDao().getCollectionById(otherCollectionId), dd);
                        document.setData(dd);

                        return new DocumentBridge(document);
                     }).collect(toList());
            } else {
               return Collections.emptyList();
            }
         } catch (Exception e) {
            cause = e;
            throw e;
         }
      }

      private Value filterValue(final Object o) {
         if (o instanceof BigDecimal) {
            return Value.asValue(((BigDecimal) o).doubleValue());
         }

         return Value.asValue(o);
      }

      @SuppressWarnings("unused")
      public Value getLinkAttribute(final LinkBridge l, final String attrId) {
         try {
            if (l.link == null || l.link.getData() == null) {
               return Value.asValue(null);
            }

            return filterValue(l.link.getData().get(attrId));
         } catch (Exception e) {
            cause = e;
            throw e;
         }
      }

      public Value getDocumentAttribute(final DocumentBridge d, final String attrId) {
         try {
            if (d.document == null || d.document.getData() == null) {
               return Value.asValue(null);
            }

            return filterValue(d.document.getData().get(attrId));
         } catch (Exception e) {
            cause = e;
            throw e;
         }
      }

      @SuppressWarnings("unused")
      public List<Value> getLinkAttribute(final List<LinkBridge> links, final String attrId) {
         try {
            final List<Value> result = new ArrayList<>();
            links.forEach(link -> result.add(filterValue(link.link != null && link.link.getData() != null ? link.link.getData().get(attrId) : Value.asValue(null))));

            return result;
         } catch (Exception e) {
            cause = e;
            throw e;
         }
      }

      public List<Value> getDocumentAttribute(List<DocumentBridge> docs, String attrId) {
         try {
            final List<Value> result = new ArrayList<>();
            docs.forEach(doc -> result.add(filterValue(doc.document != null && doc.document.getData() != null ? doc.document.getData().get(attrId) : Value.asValue(null))));

            return result;
         } catch (Exception e) {
            cause = e;
            throw e;
         }
      }

      private List<Document> createDocuments(final List<DocumentCreation> changes) {
         if (changes.isEmpty()) {
            return List.of();
         }

         final List<Document> documents = changes.stream().map(DocumentCreation::getEntity).collect(toList());

         return task.getDaoContextSnapshot().getDocumentDao().createDocuments(documents);
      }

      private List<Document> commitDocumentChanges(final TaskExecutor taskExecutor, final List<DocumentChange> changes,
            final List<Document> createdDocuments, final Map<String, List<Document>> createdDocumentsByCollectionId,
            final Map<String, Collection> collectionsMapForCreatedDocuments) {
         if (changes.isEmpty()) {
            return List.of();
         }

         final FunctionFacade functionFacade = task.getFunctionFacade();
         final TaskProcessingFacade taskProcessingFacade = task.getTaskProcessingFacade(taskExecutor, functionFacade);
         final PurposeChangeProcessor purposeChangeProcessor = task.getPurposeChangeProcessor();

         final Map<String, List<Document>> updatedDocuments = new HashMap<>(); // Collection -> [Document]
         Map<String, Set<String>> documentIdsByCollection = changes.stream().map(Change::getEntity)
                                                                   .collect(Collectors.groupingBy(Document::getCollectionId, mapping(Document::getId, toSet())));
         final Map<String, Collection> collectionsMap = task.getDaoContextSnapshot().getCollectionDao().getCollectionsByIds(documentIdsByCollection.keySet())
                                                            .stream().collect(Collectors.toMap(Collection::getId, coll -> coll));
         final Set<String> collectionsChanged = new HashSet<>();

         Map<String, Document> documentsByCorrelationId = createdDocuments.stream().collect(Collectors.toMap(doc -> doc.createIfAbsentMetaData().getString(Document.META_CORRELATION_ID), Function.identity()));

         // aggregate all changes to individual documents
         final Map<String, List<DocumentChange>> changesByDocumentId = Utils.categorize(changes.stream(), change -> change.getEntity().getId());

         changesByDocumentId.forEach((id, changeList) -> {
            final Document document = changeList.get(0).getEntity();
            final Document originalDocument =
                  (task instanceof RuleTask) ? ((RuleTask) task).getOldDocument() :
                        ((task instanceof FunctionTask) ? ((FunctionTask) task).getOriginalDocumentOrDefault(id, changeList.get(0).getOriginalDocument()) :
                              changeList.get(0).getOriginalDocument());
            final Collection collection = collectionsMap.get(document.getCollectionId());
            final DataDocument aggregatedUpdate = new DataDocument();
            changeList.forEach(change -> aggregatedUpdate.put(change.getAttrId(), change.getValue()));
            final DataDocument newData = constraintManager.encodeDataTypes(collection, aggregatedUpdate);
            final DataDocument oldData = originalDocument != null ? new DataDocument(originalDocument.getData()) : new DataDocument();

            Set<String> attributesIdsToAdd = new HashSet<>(newData.keySet());
            attributesIdsToAdd.removeAll(oldData.keySet());

            if (attributesIdsToAdd.size() > 0) {
               collection.getAttributes().stream().filter(attr -> attributesIdsToAdd.contains(attr.getId())).forEach(attr -> {
                  attr.setUsageCount(attr.getUsageCount() + 1);
                  collection.setLastTimeUsed(ZonedDateTime.now());
                  collectionsChanged.add(collection.getId());
               });
            }

            document.setUpdatedBy(task.getInitiator().getId());
            document.setUpdateDate(ZonedDateTime.now());

            DataDocument patchedData = task.getDaoContextSnapshot().getDataDao()
                                           .patchData(document.getCollectionId(), document.getId(), newData);

            Document updatedDocument = task.getDaoContextSnapshot().getDocumentDao()
                                           .updateDocument(document.getId(), document);

            updatedDocument.setData(patchedData);

            // notify delayed actions about data change
            if (collection.getPurposeType() == CollectionPurposeType.Tasks) {
               purposeChangeProcessor.processChanges(new UpdateDocument(updatedDocument, originalDocument), collection);
            }

            // add patched data to new documents
            boolean created = false;
            if (StringUtils.isNotEmpty(document.createIfAbsentMetaData().getString(Document.META_CORRELATION_ID))) {
               final Document doc = documentsByCorrelationId.get(document.getMetaData().getString(Document.META_CORRELATION_ID));

               if (doc != null) {
                  doc.setData(patchedData);
                  created = true;
               }
            }

            if (task instanceof RuleTask) {
               if (created) {
                  taskProcessingFacade.onCreateDocument(new CreateDocument(updatedDocument));
               } else {
                  taskExecutor.submitTask(functionFacade.createTaskForUpdateDocument(collection, originalDocument, updatedDocument, aggregatedUpdate.keySet()));
               }
            }

            patchedData = constraintManager.decodeDataTypes(collection, patchedData);
            updatedDocument.setData(patchedData);

            updatedDocuments.computeIfAbsent(document.getCollectionId(), key -> new ArrayList<>())
                            .add(updatedDocument);
         });

         // update collections with the new document counts
         collectionsMapForCreatedDocuments.forEach((id, col) -> {
            collectionsChanged.add(id);
            final Collection collection = collectionsMap.computeIfAbsent(id, i -> col);
            collection.setDocumentsCount(collection.getDocumentsCount() + (createdDocumentsByCollectionId.get(id) != null ? createdDocumentsByCollectionId.get(id).size() : 0));
         });

         changesTracker.addCollections(collectionsChanged.stream().map(collectionsMap::get).collect(toSet()));
         changesTracker.addUpdatedDocuments(updatedDocuments.values().stream().flatMap(java.util.Collection::stream).collect(toSet()));
         changesTracker.updateCollectionsMap(collectionsMapForCreatedDocuments);
         changesTracker.updateCollectionsMap(collectionsMap);

         collectionsChanged.forEach(collectionId -> task.getDaoContextSnapshot()
                                                        .getCollectionDao().updateCollection(collectionId, collectionsMap.get(collectionId), null, false));

         return updatedDocuments.values().stream().flatMap(java.util.Collection::stream).collect(toList());
      }

      private List<Document> removeDocuments(final List<DocumentRemoval> changes) {
         if (!changes.isEmpty()) {
            final List<Document> documents = changes.stream().map(DocumentRemoval::getEntity).collect(toList());
            final Map<String, Collection> updatedCollections = new HashMap<>();

            final Map<String, Collection> allCollections = task.getDaoContextSnapshot().getCollectionDao().getCollectionsByIds(
                  documents.stream().map(Document::getCollectionId).collect(toList())
               ).stream().collect(Collectors.toMap(Collection::getId, Function.identity()));
            final Map<String, LinkType> allLinkTypes = task.getDaoContextSnapshot().getLinkTypeDao().getAllLinkTypes().stream().collect(Collectors.toMap(LinkType::getId, Function.identity()));

            documents.forEach(document -> {
               // decrease documents count in collections map
               if (changesTracker.getCollectionsMap().containsKey(document.getCollectionId())) { // present in collections map
                  final Collection collection = changesTracker.getCollectionsMap().get(document.getCollectionId());
                  collection.setDocumentsCount(collection.getDocumentsCount() - 1);
                  updatedCollections.put(collection.getId(), collection);
               } else { // not yet tracked
                  final Collection collection = allCollections.get(document.getCollectionId());
                  collection.setDocumentsCount(collection.getDocumentsCount() - 1);
                  updatedCollections.put(document.getCollectionId(), collection);
                  changesTracker.updateCollectionsMap(Map.of(collection.getId(), collection));
               }

               // decrease documents count in updated collections in changes tracker
               final Optional<Collection> changedCollection = changesTracker.getCollections().stream().filter(c -> c.getId().equals(document.getCollectionId())).findFirst();
               if (changedCollection.isPresent()) { // present in changes tracker
                  final Collection collection = allCollections.get(document.getCollectionId());
                  changedCollection.get().setDocumentsCount(collection.getDocumentsCount());
               } else { // not yet tracked
                  final Collection collection = allCollections.get(document.getCollectionId());
                  changesTracker.getCollections().add(collection);
               }

               final List<LinkInstance> removedLinks = task.getDaoContextSnapshot().getLinkInstanceDao().getLinkInstancesByDocumentIds(Set.of(document.getId()));
               final Set<String> removedFromLinkTypes = removedLinks.stream().map(LinkInstance::getLinkTypeId).collect(toSet());
               changesTracker.addRemovedLinkInstances(removedLinks);
               task.getDaoContextSnapshot().getLinkInstanceDao().deleteLinkInstancesByDocumentsIds(Set.of(document.getId()));

               removedFromLinkTypes.forEach(linkTypeId -> {
                  // decrease link instances count in link types map
                  if (changesTracker.getLinkTypesMap().containsKey(linkTypeId)) { // present in link types map
                     final LinkType linkType = changesTracker.getLinkTypesMap().get(linkTypeId);
                     linkType.setLinksCount(task.getDaoContextSnapshot().getLinkInstanceDao().getLinkInstancesCountByLinkType(linkType.getId()));
                  } else { // not yet in the map
                     final LinkType linkType = allLinkTypes.get(linkTypeId);
                     linkType.setLinksCount(task.getDaoContextSnapshot().getLinkInstanceDao().getLinkInstancesCountByLinkType(linkType.getId()));
                     changesTracker.updateLinkTypesMap(Map.of(linkType.getId(), linkType));
                  }

                  // decrease link instances count in updated link types in changes tracker
                  final Optional<LinkType> changedLinkType = changesTracker.getLinkTypes().stream().filter(l -> l.getId().equals(linkTypeId)).findFirst();
                  if (changedLinkType.isPresent()) { // tracked in changes tracker
                     final LinkType linkType = allLinkTypes.get(linkTypeId);
                     changedLinkType.get().setLinksCount(linkType.getLinksCount());
                  } else { // not yet tracked
                     final LinkType linkType = allLinkTypes.get(linkTypeId);
                     changesTracker.getLinkTypes().add(linkType);
                  }
               });

               task.getDaoContextSnapshot().getDocumentDao().deleteDocument(document.getId(), document.getData());
               task.getDaoContextSnapshot().getDataDao().deleteData(document.getCollectionId(), document.getId());
            });

            updatedCollections.forEach((id, col) -> {
               col.setDocumentsCount(col.getDocumentsCount() - documents.size());
               task.getDaoContextSnapshot().getCollectionDao().updateCollection(id, col, null, false);
            });

            return documents;
         }

         return List.of();
      }

      private List<LinkInstance> createLinks(final List<LinkCreation> changes) {
         if (changes.isEmpty()) {
            return List.of();
         }

         final List<LinkInstance> linkInstances = changes.stream().map(LinkCreation::getEntity).collect(toList());

         return task.getDaoContextSnapshot().getLinkInstanceDao().createLinkInstances(linkInstances, false);
      }

      private List<LinkInstance> commitLinkChanges(final TaskExecutor taskExecutor, final List<LinkChange> changes,
            final List<LinkInstance> createdLinks, final Map<String, List<LinkInstance>> createdLinksByLinkTypeId,
            final Map<String, LinkType> linkTypeMapForCreatedLinks) {
         if (changes.isEmpty()) {
            return List.of();
         }

         final FunctionFacade functionFacade = task.getFunctionFacade();
         final TaskProcessingFacade taskProcessingFacade = task.getTaskProcessingFacade(taskExecutor, functionFacade);

         final Map<String, List<LinkInstance>> updatedLinks = new HashMap<>(); // LinkType -> [LinkInstance]
         final Map<String, LinkType> linkTypesMap = task.getDaoContextSnapshot().getLinkTypeDao().getAllLinkTypes()
                                                        .stream().collect(Collectors.toMap(LinkType::getId, linkType -> linkType));
         Set<String> linkTypesChanged = new HashSet<>();

         Map<String, LinkInstance> linksByCorrelationId = createdLinks.stream().collect(Collectors.toMap(LinkInstance::getTemplateId, Function.identity()));

         // aggregate all changes to individual link instances
         final Map<String, List<LinkChange>> changesByLinkTypeId = Utils.categorize(changes.stream(), change -> change.getEntity().getId());

         changesByLinkTypeId.forEach((id, changeList) -> {
            final LinkInstance linkInstance = changeList.get(0).getEntity();
            final LinkInstance originalLinkInstance = (task instanceof RuleTask) ? ((RuleTask) task).getOldLinkInstance() :
                  ((task instanceof FunctionTask) ? ((FunctionTask) task).getOriginalLinkInstanceOrDefault(id, changeList.get(0).getOriginalLinkInstance()) :
                        changeList.get(0).getOriginalLinkInstance());
            final LinkType linkType = linkTypesMap.get(linkInstance.getLinkTypeId());
            final DataDocument aggregatedUpdate = new DataDocument();
            changeList.forEach(change -> aggregatedUpdate.put(change.getAttrId(), change.getValue()));
            final DataDocument newData = constraintManager.encodeDataTypes(linkType, aggregatedUpdate);
            final DataDocument oldData = originalLinkInstance != null ? new DataDocument(originalLinkInstance.getData()) : new DataDocument();

            Set<String> attributesIdsToAdd = new HashSet<>(newData.keySet());
            attributesIdsToAdd.removeAll(oldData.keySet());

            if (attributesIdsToAdd.size() > 0) {
               linkType.getAttributes().stream().filter(attr -> attributesIdsToAdd.contains(attr.getId())).forEach(attr -> {
                  attr.setUsageCount(attr.getUsageCount() + 1);
                  linkTypesChanged.add(linkType.getId());
               });
            }

            linkInstance.setUpdatedBy(task.getInitiator().getId());
            linkInstance.setUpdateDate(ZonedDateTime.now());

            DataDocument patchedData = task.getDaoContextSnapshot().getLinkDataDao()
                                           .patchData(linkInstance.getLinkTypeId(), linkInstance.getId(), newData);

            LinkInstance updatedLink = task.getDaoContextSnapshot().getLinkInstanceDao()
                                           .updateLinkInstance(linkInstance.getId(), linkInstance);

            updatedLink.setData(patchedData);

            // add patched data to new links
            boolean created = false;
            if (StringUtils.isNotEmpty(linkInstance.getTemplateId())) {
               final LinkInstance link = linksByCorrelationId.get(linkInstance.getTemplateId());

               if (link != null) {
                  link.setData(patchedData);
                  created = true;
               }
            }

            if (task instanceof RuleTask) {
               if (created) {
                  taskProcessingFacade.onCreateLink(new CreateLinkInstance(updatedLink));
               } else {
                  taskExecutor.submitTask(functionFacade.creatTaskForChangedLink(linkType, originalLinkInstance, updatedLink, aggregatedUpdate.keySet()));
               }
            }

            patchedData = constraintManager.decodeDataTypes(collection, patchedData);
            updatedLink.setData(patchedData);

            updatedLinks.computeIfAbsent(linkInstance.getLinkTypeId(), key -> new ArrayList<>())
                        .add(updatedLink);
         });

         linkTypeMapForCreatedLinks.forEach((id, linkType) -> {
            linkTypesChanged.add(id);
         });

         changesTracker.addLinkTypes(linkTypesChanged.stream().map(linkTypesMap::get).collect(toSet()));
         changesTracker.addUpdatedLinkInstances(updatedLinks.values().stream().flatMap(java.util.Collection::stream).collect(toSet()));
         changesTracker.updateLinkTypesMap(linkTypeMapForCreatedLinks);
         changesTracker.updateLinkTypesMap(linkTypesMap);

         linkTypesChanged.forEach(linkTypeId -> task.getDaoContextSnapshot()
                                                    .getLinkTypeDao().updateLinkType(linkTypeId, linkTypesMap.get(linkTypeId), null));

         return updatedLinks.values().stream().flatMap(java.util.Collection::stream).collect(toList());
      }

      ChangesTracker commitChanges(final TaskExecutor taskExecutor) {
         if (changes.isEmpty()) {
            return null;
         }

         @SuppressWarnings("rawtypes")
         final List<Change> invalidChanges = changes.stream().filter(change -> !change.isComplete()).collect(toList());
         if (invalidChanges.size() > 0) {
            final StringBuilder sb = new StringBuilder();
            invalidChanges.forEach(change -> sb.append("Invalid update request: ").append(change.toString()).append("\n"));
            throw new IllegalArgumentException(sb.toString());
         }

         // first create all new documents
         final List<Document> createdDocuments = createDocuments(changes.stream().filter(change -> change instanceof DocumentCreation && change.isComplete()).map(change -> (DocumentCreation) change).collect(toList()));
         final Map<String, List<Document>> toBeRemovedDocumentsByCollection = Utils.categorize(changes.stream().filter(change -> change instanceof DocumentRemoval && change.isComplete()).map(change -> ((DocumentRemoval) change).getEntity()), Document::getCollectionId);
         // get data structures for efficient manipulation with the new documents
         final Map<String, List<Document>> documentsByCollection = DocumentUtils.getDocumentsByCollection(createdDocuments);
         final List<String> usedCollections = new ArrayList<>(documentsByCollection.keySet());
         usedCollections.addAll(toBeRemovedDocumentsByCollection.keySet());
         final Map<String, Collection> collectionsMap = task.getDaoContextSnapshot().getCollectionDao().getCollectionsByIds(usedCollections).stream().collect(Collectors.toMap(Collection::getId, Function.identity()));
         final Map<String, String> correlationIdsToIds = createdDocuments.stream().collect(Collectors.toMap(doc -> doc.createIfAbsentMetaData().getString(Document.META_CORRELATION_ID), Document::getId));

         // report new empty documents, later updates are sent separately
         changesTracker.addCreatedDocuments(createdDocuments);
         changesTracker.addCollections(collectionsMap.values().stream().filter(c -> documentsByCollection.containsKey(c.getId())).collect(toSet()));

         // map the newly create document IDs to all other changes so that we use the correct document in updates etc.
         changes.stream().filter(change -> change instanceof DocumentChange).forEach(change -> {
            final Document doc = (Document) change.getEntity();
            if (StringUtils.isEmpty(doc.getId()) && StringUtils.isNotEmpty(doc.createIfAbsentMetaData().getString(Document.META_CORRELATION_ID))) {
               doc.setId(correlationIdsToIds.get(doc.getMetaData().getString(Document.META_CORRELATION_ID)));
            }
         });
         changes.stream().filter(change -> change instanceof LinkCreation).forEach(change -> {
            final LinkInstance link = ((LinkCreation) change).getEntity();
            if (StringUtils.isEmpty(link.getId()) && StringUtils.isNotEmpty(link.getTemplateId())) {
               link.setDocumentIds(List.of(
                     correlationIdsToIds.containsKey(link.getDocumentIds().get(0)) ? correlationIdsToIds.get(link.getDocumentIds().get(0)) : link.getDocumentIds().get(0),
                     correlationIdsToIds.containsKey(link.getDocumentIds().get(1)) ? correlationIdsToIds.get(link.getDocumentIds().get(1)) : link.getDocumentIds().get(1)
               ));
            }
         });


         // commit document changes
         final List<Document> changedDocuments = commitDocumentChanges(
               taskExecutor,
               changes.stream().filter(change -> change instanceof DocumentChange && change.isComplete()).map(change -> (DocumentChange) change).collect(toList()),
               createdDocuments,
               documentsByCollection,
               collectionsMap
         );

         // remove documents
         final List<Document> removedDocuments = removeDocuments(changes.stream().filter(change -> change instanceof DocumentRemoval).map(change -> (DocumentRemoval) change).collect(toList()));
         changesTracker.addRemovedDocuments(removedDocuments);

         // remove created documents that were deleted later
         final Set<Document> unusedCreatedDocuments = new HashSet<>(changesTracker.getRemovedDocuments());
         unusedCreatedDocuments.retainAll(changesTracker.getCreatedDocuments());
         changesTracker.getCreatedDocuments().removeAll(unusedCreatedDocuments);

         // create new links
         final List<LinkInstance> createdLinks = createLinks(changes.stream().filter(change -> change instanceof LinkCreation && change.isComplete()).map(change -> (LinkCreation) change).collect(toList()));
         final Map<String, List<LinkInstance>> linksByType = LinkTypeUtils.getLinksByType(createdLinks);
         final Map<String, LinkType> linkTypesMap = task.getDaoContextSnapshot().getLinkTypeDao().getLinkTypesByIds(linksByType.keySet()).stream().collect(Collectors.toMap(LinkType::getId, Function.identity()));
         final Map<String, String> linkCorrelationIdsToIds = createdLinks.stream().collect(Collectors.toMap(LinkInstance::getTemplateId, LinkInstance::getId));

         // report new empty links, later updates are sent separately
         changesTracker.addCreatedLinkInstances(createdLinks);
         changesTracker.addLinkTypes(linkTypesMap.values().stream().filter(c -> linksByType.containsKey(c.getId())).collect(toSet()));

         // map the newly create link IDs to all other changes so that we use the correct document in updates etc.
         changes.stream().filter(change -> change instanceof LinkChange).forEach(change -> {
            final LinkInstance link = (LinkInstance) change.getEntity();
            if (StringUtils.isEmpty(link.getId()) && StringUtils.isNotEmpty(link.getTemplateId())) {
               link.setId(linkCorrelationIdsToIds.get(link.getTemplateId()));
            }
         });

         // commit link changes
         final List<LinkInstance> changedLinkInstances = commitLinkChanges(
               taskExecutor,
               changes.stream().filter(change -> change instanceof LinkChange && change.isComplete()).map(change -> (LinkChange) change).collect(toList()),
               createdLinks,
               linksByType,
               linkTypesMap
         );

         // report user messages and print requests for rules triggered via an Action button
         final String correlationId = task.getCorrelationId();
         if (StringUtils.isNotEmpty(correlationId)) {
            final List<UserMessage> userMessages = changes.stream().filter(change -> change instanceof UserMessageChange).map(change -> ((UserMessageChange) change).getEntity()).collect(toList());
            changesTracker.addUserMessages(userMessages);

            final List<PrintRequest> printRequests = changes.stream().filter(change -> change instanceof PrintAttributeChange).map(change -> ((PrintAttributeChange) change).getEntity()).collect(toList());
            changesTracker.addPrintRequests(printRequests);
         }

         // propagate changes in existing documents and links that has been loaded prior to calling this rule
         task.propagateChanges(changedDocuments, changedLinkInstances);

         return changesTracker;
      }

      ChangesTracker commitDryRunChanges(final TaskExecutor taskExecutor) {
         if (changes.isEmpty()) {
            return null;
         }

         final String correlationId = task.getCorrelationId();
         if (StringUtils.isNotEmpty(correlationId)) {
            final List<UserMessage> userMessages = changes.stream().filter(change -> change instanceof UserMessageChange).map(change -> ((UserMessageChange) change).getEntity()).collect(toList());
            changesTracker.addUserMessages(userMessages);
         }

         return changesTracker;
      }

      String getChanges() {
         final Map<String, Collection> collections = new HashMap<>();
         final Map<String, LinkType> linkTypes = new HashMap<>();
         final StringBuilder sb = new StringBuilder();

         final LongAdder i = new LongAdder();
         changes.forEach(change -> {
            if (change instanceof DocumentCreation ) {
               final DocumentCreation documentCreation = (DocumentCreation) change;
               if (StringUtils.isEmpty(documentCreation.getEntity().getId())) {
                  i.increment();
                  documentCreation.getEntity().setId("NEW" + i.intValue());
               }
            }
         });

         changes.forEach(change -> {
            if (change instanceof DocumentCreation) {
               final DocumentCreation documentCreation = (DocumentCreation) change;
               final Collection collection = collections.computeIfAbsent(documentCreation.getEntity().getCollectionId(), id -> task.getDaoContextSnapshot().getCollectionDao().getCollectionById(id));
               sb.append("new Document(").append(collection.getName()).append(")\n");
            } else if (change instanceof DocumentChange) {
               final DocumentChange documentChange = (DocumentChange) change;
               final Collection collection = collections.computeIfAbsent(documentChange.getEntity().getCollectionId(), id -> task.getDaoContextSnapshot().getCollectionDao().getCollectionById(id));
               appendChange(sb, collection.getName(), collection.getAttributes(), documentChange);
            } else if (change instanceof LinkChange) {
               final LinkChange linkChange = (LinkChange) change;
               final LinkType linkType = linkTypes.computeIfAbsent(linkChange.getEntity().getId(), id -> task.getDaoContextSnapshot().getLinkTypeDao().getLinkType(id));
               appendChange(sb, linkType.getName(), linkType.getAttributes(), linkChange);
            } else if (change instanceof UserMessageChange) {
               sb.append(change.toString());
            } else if (change instanceof PrintAttributeChange) {
               sb.append(change.toString());
            } else if (change instanceof LinkCreation) {
               final LinkCreation linkCreation = (LinkCreation) change;
               final LinkType linkType = linkTypes.computeIfAbsent(linkCreation.getEntity().getLinkTypeId(), id -> task.getDaoContextSnapshot().getLinkTypeDao().getLinkType(id));
               sb.append("new Link(").append(linkType.getName()).append(")\n");
            }
         });

         return sb.toString();
      }

      @SuppressWarnings("rawtypes")
      private void appendChange(final StringBuilder sb, final String name, final java.util.Collection<Attribute> attributes, final Change change) {
         if (change instanceof ResourceChange) {
            final ResourceChange resourceChange = (ResourceChange) change;
            sb.append(name).append("(").append(last4(((WithId) resourceChange.getEntity()).getId())).append("): ");
            sb.append(attributes.stream().filter(a -> a.getId().equals(resourceChange.getAttrId())).map(Attribute::getName).findFirst().orElse(""));
            sb.append(" = ");
            sb.append(resourceChange.getValue());
            sb.append("\n");
         }
      }

      private String last4(final String str) {
         if (str.length() <= 4) {
            return str;
         }
         return str.substring(str.length() - 4);
      }
   }

   public static abstract class Change<T> {
      protected final T entity;

      public Change(final T entity) {
         this.entity = entity;
      }

      public boolean isComplete() {
         return entity != null;
      }

      public T getEntity() {
         return entity;
      }

      @Override
      public String toString() {
         return "Change{" +
               "entity=" + entity +
               '}';
      }
   }

   public static class UserMessageChange extends Change<UserMessage> {
      public UserMessageChange(final UserMessage entity) {
         super(entity);
      }

      @Override
      public String toString() {
         return "UserMessageChange{" +
               "message=" + entity +
               '}';
      }
   }

   public static class PrintAttributeChange extends Change<PrintRequest> {
      public PrintAttributeChange(final PrintRequest entity) {
         super(entity);
      }

      @Override
      public String toString() {
         return "PrintAttributeChange{" +
               "entity=" + entity +
               '}';
      }
   }

   public static abstract class ResourceChange<T extends WithId> extends Change<T> {

      private final String attrId;
      private final Object value;

      public ResourceChange(final T entity, final String attrId, final Object value) {
         super(entity);
         this.attrId = attrId;
         this.value = value;
      }

      public boolean isComplete() {
         return entity != null && StringUtils.isNotEmpty(attrId);
      }

      public String getAttrId() {
         return attrId;
      }

      public Object getValue() {
         return value;
      }

      @Override
      public String toString() {
         return getClass().getSimpleName() + "{" +
               "entity=" + getEntity().getId() +
               ", attrId='" + getAttrId() + '\'' +
               ", value=" + getValue() +
               '}';
      }
   }

   public static class DocumentCreation extends Change<Document> {

      public DocumentCreation(final Document entity) {
         super(entity);
      }
   }

   public static class DocumentRemoval extends Change<Document> {

      public DocumentRemoval(final Document entity) {
         super(entity);
      }
   }

   public static class DocumentChange extends ResourceChange<Document> {

      private final Document originalDocument;

      public DocumentChange(final Document entity, final String attrId, final Object value) {
         super(entity, attrId, value);
         originalDocument = new Document(entity);
      }

      public Document getOriginalDocument() {
         return originalDocument;
      }

      @Override
      public String toString() {
         return "DocumentChange{" +
               "entity=" + entity +
               ", attrId='" + getAttrId() + '\'' +
               ", value=" + getValue() +
               ", originalDocument=" + originalDocument +
               '}';
      }
   }

   public static class LinkChange extends ResourceChange<LinkInstance> {

      private final LinkInstance originalLinkInstance;

      public LinkChange(final LinkInstance entity, final String attrId, final Object value) {
         super(entity, attrId, value);
         originalLinkInstance = new LinkInstance(entity);
      }

      public LinkInstance getOriginalLinkInstance() {
         return originalLinkInstance;
      }
   }

   public static class LinkCreation extends Change<LinkInstance> {

      public LinkCreation(final LinkInstance entity) {
         super(entity);
      }
   }

   public static class DocumentBridge {
      private final Document document;

      DocumentBridge(final Document document) {
         this.document = document;
      }

      @Override
      public String toString() {
         return "DocumentBridge{" +
               "document=" + document +
               '}';
      }
   }

   public static class LinkBridge {
      private final LinkInstance link;

      LinkBridge(final LinkInstance link) {
         this.link = link;
      }

      @Override
      public String toString() {
         return "LinkBridge{" +
               "link=" + link +
               '}';
      }
   }

   private String getJsLib() {
      return "function lumeer_isEmpty(v) {\n"
            + "  return (v === null || v === undefined || v === '' || (Array.isArray(v) && (v.length === 0 || (v.length === 1 && lumeer_isEmpty(v[0])))) || (typeof v === 'object' && !!v && Object.keys(v).length === 0 && v.constructor === Object));\n"
            + "}\n";
   }

   public void execute(final Map<String, Object> bindings, final ContextualTask task, final Collection collection, final String js) {
      lumeerBridge = new LumeerBridge(task, collection);
      lumeerBridge.dryRun = dryRun;

      Context context = Context
            .newBuilder("js")
            .engine(engine)
            .allowAllAccess(true)
            .build();
      context.initialize("js");
      context.getPolyglotBindings().putMember("lumeer", lumeerBridge);

      bindings.forEach((k, v) -> context.getBindings("js").putMember(k, v));

      Timer timer = new Timer(true);
      timer.schedule(new TimerTask() {
         @Override
         public void run() {
            context.close(true);
         }
      }, 3000);

      final String jsCode = getJsLib() +
            (js.contains(MomentJsParser.FORMAT_JS_DATE) || js.contains(MomentJsParser.PARSE_JS_DATE) || js.contains(MOMENT_JS_SIGNATURE) ? momentJsCode + ";\n" : "") + js;

      context.eval("js", jsCode);
   }

   public ChangesTracker commitChanges(final TaskExecutor taskExecutor) {
      return lumeerBridge.commitChanges(taskExecutor);
   }

   public ChangesTracker commitDryRunChanges(final TaskExecutor taskExecutor) {
      return lumeerBridge.commitDryRunChanges(taskExecutor);
   }

   public String getChanges() {
      return lumeerBridge.getChanges();
   }

   public void setErrorInAttribute(final Document document, final String attributeId, final TaskExecutor taskExecutor) {
      lumeerBridge.changes = Set.of(new DocumentChange(document, attributeId, "ERR!"));
      lumeerBridge.commitChanges(taskExecutor);
   }

   public Exception getCause() {
      return lumeerBridge.cause;
   }

   public void setDryRun(final boolean dryRun) {
      this.dryRun = dryRun;
   }
}
