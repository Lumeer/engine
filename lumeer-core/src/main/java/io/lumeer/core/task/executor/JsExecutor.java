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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.common.WithId;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.FunctionFacade;
import io.lumeer.core.facade.PusherFacade;
import io.lumeer.core.facade.TaskProcessingFacade;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.task.ContextualTask;
import io.lumeer.core.task.Task;
import io.lumeer.core.task.TaskExecutor;
import io.lumeer.core.task.UserMessage;
import io.lumeer.core.util.DocumentUtils;
import io.lumeer.core.util.MomentJsParser;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.CreateDocument;
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
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class JsExecutor {

   private static final String CREATE_PREFIX = "CREATE_";

   private static Logger log = Logger.getLogger(JsExecutor.class.getName());

   private LumeerBridge lumeerBridge;
   private boolean dryRun = false;
   private static Engine engine = Engine
         .newBuilder()
         .allowExperimentalOptions(true)
         .option("js.experimental-foreign-object-prototype", "true")
         .option("js.foreign-object-prototype", "true")
         .build();
   private static String momentJsCode = MomentJsParser.getMomentJsCode();

   public static class LumeerBridge {

      private static DefaultConfigurationProducer configurationProducer = new DefaultConfigurationProducer();
      private static ConstraintManager constraintManager = ConstraintManager.getInstance(configurationProducer);
      private ContextualTask ruleTask;
      private Collection collection;
      private Set<Change> changes = new HashSet<>();
      private Exception cause = null;
      private boolean dryRun = false;

      private LumeerBridge(final ContextualTask task, final Collection collection) {
         this.ruleTask = task;
         this.collection = collection;
      }

      public String getSequenceNumber(final String sequenceName, final int digits) {
         final String format = "%" + (digits <= 1 ? "" : "0" + digits) + "d";

         if (dryRun) {
            return String.format(format, 1);
         } else {
            final int sequenceValue = ruleTask.getDaoContextSnapshot().getSequenceDao().getNextSequenceNo(sequenceName);
            ruleTask.sendPushNotifications(sequenceName);

            return String.format(format, sequenceValue);
         }
      }

      public String getCurrentUser() {
         final String email = ruleTask.getInitiator().getEmail();
         return email == null ? "" : email;
      }

      public String getCurrentLocale() {
         return ruleTask.getCurrentLocale();
      }

      public void showMessage(final String type, final String message) {
         if (ruleTask.getDaoContextSnapshot().increaseCreationCounter() <= Task.MAX_MESSAGES) {
            changes.add(new UserMessageChange(new UserMessage(type, message)));
         }
      }

      public DocumentBridge createDocument(final String collectionId) {
         if (ruleTask.getDaoContextSnapshot().increaseCreationCounter() <= Task.MAX_CREATED_DOCUMENTS) {
            try {
               final Document d = new Document(collectionId, ZonedDateTime.now(), null, ruleTask.getInitiator().getId(), null, 0, null);
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

         final List<LinkInstance> result = ruleTask.getDaoContextSnapshot().getLinkInstanceDao()
                                                   .searchLinkInstances(query);
         result.stream().forEach(linkInstance ->
               linkInstance.setData(constraintManager.encodeDataTypesForFce(ruleTask.getDaoContextSnapshot().getLinkTypeDao().getLinkType(linkTypeId), linkInstance.getData()))
         );

         return result;
      }

      public List<LinkBridge> getLinks(DocumentBridge d, String linkTypeId) {
         try {
            final LinkType linkType = ruleTask.getDaoContextSnapshot().getLinkTypeDao().getLinkType(linkTypeId);
            final List<LinkInstance> links = getLinkInstances(d.document.getId(), linkTypeId);

            // load link data
            if (links.size() > 0) {
               final Map<String, DataDocument> linkData = ruleTask.getDaoContextSnapshot().getLinkDataDao().getData(linkTypeId, links.stream().map(LinkInstance::getId).collect(toSet())).stream()
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

      public DocumentBridge getLinkDocument(final LinkBridge l, final String collectionId) {
         try {
            List<Document> documents = ruleTask.getDaoContextSnapshot().getDocumentDao().getDocumentsByIds(l.link.getDocumentIds().toArray(new String[0]));
            if (documents.size() == 2) {
               final Document doc = documents.get(0).getCollectionId().equals(collectionId) ? documents.get(0) : documents.get(1);

               DataDocument data = ruleTask.getDaoContextSnapshot().getDataDao().getData(doc.getCollectionId(), doc.getId());
               data = constraintManager.encodeDataTypesForFce(ruleTask.getDaoContextSnapshot().getCollectionDao().getCollectionById(collectionId), data);
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
            final LinkType linkType = ruleTask.getDaoContextSnapshot().getLinkTypeDao().getLinkType(linkTypeId);
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
               ruleTask.getDaoContextSnapshot().getDataDao()
                       .getData(otherCollectionId, documentIds)
                       .forEach(dd -> data.put(dd.getId(), dd));

               // load document meta data and match them with user data
               return ruleTask.getDaoContextSnapshot().getDocumentDao()
                              .getDocumentsByIds(documentIds.toArray(new String[0]))
                              .stream().map(document -> {
                        DataDocument dd = data.get(document.getId());
                        dd = constraintManager.encodeDataTypesForFce(ruleTask.getDaoContextSnapshot().getCollectionDao().getCollectionById(otherCollectionId), dd);
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

      public Value getLinkAttribute(final LinkBridge l, final String attrId) {
         try {
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

      public List<Value> getLinkAttribute(final List<LinkBridge> links, final String attrId) {
         try {
            final List<Value> result = new ArrayList<>();
            links.forEach(link -> result.add(filterValue(link.link.getData().get(attrId))));

            return result;
         } catch (Exception e) {
            cause = e;
            throw e;
         }
      }

      public List<Value> getDocumentAttribute(List<DocumentBridge> docs, String attrId) {
         try {
            final List<Value> result = new ArrayList<>();
            docs.forEach(doc -> result.add(filterValue(doc.document.getData().get(attrId))));

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

         return ruleTask.getDaoContextSnapshot().getDocumentDao().createDocuments(documents);
      }

      private void sendNotificationsForCreatedDocuments(final Map<String, List<Document>> documentsByCollection, final Map<String, Collection> collectionsMap) {
         // send push notification
         if (ruleTask.getPusherClient() != null) {
            documentsByCollection.forEach((key, value) ->
                  ruleTask.sendPushNotifications(collectionsMap.get(key), value, PusherFacade.CREATE_EVENT_SUFFIX)
            );
         }
      }

      private void callFunctionsAndRulesOnNewDocuments(final TaskExecutor taskExecutor, final List<Document> documents, final Map<String, Collection> collectionsMap) {
         if (documents != null && documents.size() > 0) {
            final FunctionFacade functionFacade = ruleTask.getFunctionFacade();
            final TaskProcessingFacade taskProcessingFacade = ruleTask.getTaskProcessingFacade(taskExecutor, functionFacade);
            documents.forEach(doc -> taskProcessingFacade.onCreateDocument(new CreateDocument(doc)));
         }
      }

      private List<Document> commitDocumentChanges(final List<DocumentChange> changes, final List<Document> createdDocuments) {
         if (changes.isEmpty()) {
            return List.of();
         }

         final Map<String, List<Document>> updatedDocuments = new HashMap<>(); // Collection -> [Document]
         Map<String, Set<String>> documentIdsByCollection = changes.stream().map(change -> change.getEntity())
                                                                   .collect(Collectors.groupingBy(Document::getCollectionId, mapping(Document::getId, toSet())));
         final Map<String, Collection> collectionsMap = ruleTask.getDaoContextSnapshot().getCollectionDao().getCollectionsByIds(documentIdsByCollection.keySet())
                                                                .stream().collect(Collectors.toMap(Collection::getId, coll -> coll));
         Set<String> collectionsChanged = new HashSet<>();

         Map<String, Document> documentsByCorrelationId = createdDocuments.stream().collect(Collectors.toMap(doc -> doc.createIfAbsentMetaData().getString(Document.META_CORRELATION_ID), Function.identity()));

         changes.forEach(documentChange -> {
            final Document document = documentChange.getEntity();
            final Collection collection = collectionsMap.get(document.getCollectionId());
            final DataDocument newData = constraintManager.encodeDataTypes(collection, new DataDocument(documentChange.getAttrId(), documentChange.getValue()));
            final DataDocument oldData = new DataDocument(document.getData());

            Set<String> attributesIdsToAdd = new HashSet<>(newData.keySet());
            attributesIdsToAdd.removeAll(oldData.keySet());

            if (attributesIdsToAdd.size() > 0) {
               Optional<Attribute> attribute = collection.getAttributes().stream().filter(attr -> attr.getId().equals(documentChange.getAttrId())).findFirst();
               attribute.ifPresent(attr -> attr.setUsageCount(attr.getUsageCount() + 1));
               collection.setLastTimeUsed(ZonedDateTime.now());
               collectionsChanged.add(collection.getId());
            }

            document.setUpdatedBy(ruleTask.getInitiator().getId());
            document.setUpdateDate(ZonedDateTime.now());

            DataDocument patchedData = ruleTask.getDaoContextSnapshot().getDataDao()
                                               .patchData(documentChange.getEntity().getCollectionId(), documentChange.getEntity().getId(), newData);

            Document updatedDocument = ruleTask.getDaoContextSnapshot().getDocumentDao()
                                               .updateDocument(document.getId(), document);

            // add patched data to new documents
            if (StringUtils.isNotEmpty(document.createIfAbsentMetaData().getString(Document.META_CORRELATION_ID))) {
               final Document doc = documentsByCorrelationId.get(document.getMetaData().getString(Document.META_CORRELATION_ID));

               if (doc != null) {
                  doc.setData(patchedData);
               }
            }

            patchedData = constraintManager.decodeDataTypes(collection, patchedData);
            updatedDocument.setData(patchedData);

            updatedDocuments.computeIfAbsent(documentChange.getEntity().getCollectionId(), key -> new ArrayList<>())
                            .add(updatedDocument);
         });

         collectionsChanged.forEach(collectionId -> ruleTask.getDaoContextSnapshot()
                                                            .getCollectionDao().updateCollection(collectionId, collectionsMap.get(collectionId), null));

         // send push notification
         if (ruleTask.getPusherClient() != null) {
            updatedDocuments.keySet().forEach(collectionId ->
                  ruleTask.sendPushNotifications(collectionsMap.get(collectionId), updatedDocuments.get(collectionId))
            );
         }

         return updatedDocuments.values().stream().flatMap(java.util.Collection::stream).collect(toList());
      }

      private List<LinkInstance> commitLinkChanges(final List<LinkChange> changes) {
         if (changes.isEmpty()) {
            return List.of();
         }

         final Map<String, List<LinkInstance>> updatedLinks = new HashMap<>(); // LinkType -> [LinkInstance]
         final Map<String, LinkType> linkTypesMap = ruleTask.getDaoContextSnapshot().getLinkTypeDao().getAllLinkTypes()
                                                            .stream().collect(Collectors.toMap(LinkType::getId, linkType -> linkType));
         Set<String> linkTypesChanged = new HashSet<>();

         changes.forEach(linkChange -> {
            final LinkInstance linkInstance = linkChange.getEntity();
            final LinkType linkType = linkTypesMap.get(linkInstance.getLinkTypeId());
            final DataDocument newData = constraintManager.encodeDataTypes(linkType, new DataDocument(linkChange.getAttrId(), linkChange.getValue()));
            final DataDocument oldData = new DataDocument(linkInstance.getData() == null ? new DataDocument() : linkInstance.getData());

            Set<String> attributesIdsToAdd = new HashSet<>(newData.keySet());
            attributesIdsToAdd.removeAll(oldData.keySet());

            if (attributesIdsToAdd.size() > 0) {
               Optional<Attribute> attribute = linkType.getAttributes().stream().filter(attr -> attr.getId().equals(linkChange.getAttrId())).findFirst();
               attribute.ifPresent(attr -> attr.setUsageCount(attr.getUsageCount() + 1));
               linkTypesChanged.add(linkType.getId());
            }

            linkInstance.setUpdatedBy(ruleTask.getInitiator().getId());
            linkInstance.setUpdateDate(ZonedDateTime.now());

            DataDocument patchedData = ruleTask.getDaoContextSnapshot().getLinkDataDao()
                                               .patchData(linkChange.getEntity().getLinkTypeId(), linkChange.getEntity().getId(), newData);

            LinkInstance updatedLink = ruleTask.getDaoContextSnapshot().getLinkInstanceDao()
                                               .updateLinkInstance(linkInstance.getId(), linkInstance);

            updatedLink.setData(constraintManager.decodeDataTypes(linkType, patchedData));

            updatedLinks.computeIfAbsent(linkChange.getEntity().getLinkTypeId(), key -> new ArrayList<>())
                        .add(updatedLink);
         });

         linkTypesChanged.forEach(linkTypeId -> ruleTask.getDaoContextSnapshot()
                                                        .getLinkTypeDao().updateLinkType(linkTypeId, linkTypesMap.get(linkTypeId), null));

         // send push notification
         if (ruleTask.getPusherClient() != null) {
            updatedLinks.keySet().forEach(linkTypeId ->
                  ruleTask.sendPushNotifications(linkTypesMap.get(linkTypeId), updatedLinks.get(linkTypeId))
            );
         }

         return updatedLinks.values().stream().flatMap(java.util.Collection::stream).collect(toList());
      }

      void commitChanges(final TaskExecutor taskExecutor) {
         if (changes.isEmpty()) {
            return;
         }

         final List<Change> invalidChanges = changes.stream().filter(change -> !change.isComplete()).collect(toList());
         if (invalidChanges.size() > 0) {
            final StringBuilder sb = new StringBuilder();
            invalidChanges.forEach(change -> sb.append("Invalid update request: " + change.toString() + "\n"));
            throw new IllegalArgumentException(sb.toString());
         }

         // first create all new documents
         final List<Document> createdDocuments = createDocuments(changes.stream().filter(change -> change instanceof DocumentCreation && change.isComplete()).map(change -> (DocumentCreation) change).collect(toList()));
         // get data structures for efficient manipulation with the new documents
         final Map<String, List<Document>> documentsByCollection = DocumentUtils.getDocumentsByCollection(createdDocuments);
         final Map<String, Collection> collectionsMap = DocumentUtils.getCollectionsMap(ruleTask.getDaoContextSnapshot().getCollectionDao(), documentsByCollection);
         final Map<String, String> correlationIdsToIds = createdDocuments.stream().collect(Collectors.toMap(doc -> doc.createIfAbsentMetaData().getString(Document.META_CORRELATION_ID), Document::getId));

         // send notifications for new empty documents, later updates are sent separately
         sendNotificationsForCreatedDocuments(documentsByCollection, collectionsMap);

         // map the newly create document IDs to all other changes so that we use the correct document in updates etc.
         changes.stream().filter(change -> change instanceof DocumentChange).forEach(change -> {
            final Document doc = (Document) change.getEntity();
            if (StringUtils.isEmpty(doc.getId()) && StringUtils.isNotEmpty(doc.createIfAbsentMetaData().getString(Document.META_CORRELATION_ID))) {
               doc.setId(correlationIdsToIds.get(doc.getMetaData().getString(Document.META_CORRELATION_ID)));
            }
         });

         // commit document and link changes
         final List<Document> changedDocuments = commitDocumentChanges(
               changes.stream().filter(change -> change instanceof DocumentChange && change.isComplete()).map(change -> (DocumentChange) change).collect(toList()),
               createdDocuments
         );
         final List<LinkInstance> changedLinkInstances = commitLinkChanges(changes.stream().filter(change -> change instanceof LinkChange && change.isComplete()).map(change -> (LinkChange) change).collect(toList()));

         // call functions and rules on new documents once they are filled with data from document changes
         callFunctionsAndRulesOnNewDocuments(taskExecutor, createdDocuments, collectionsMap);

         // send push notifications with user messages for rules triggered via an Action button
         final String correlationId = ruleTask.getCorrelationId();
         if (StringUtils.isNotEmpty(correlationId)) {
            final List<UserMessage> userMessages = changes.stream().filter(change -> change instanceof UserMessageChange).map(change -> ((UserMessageChange) change).getEntity()).collect(toList());
            ruleTask.sendPushNotifications(userMessages);
         }

         // propagate changes in existing documents and links that has been loaded prior to calling this rule
         ruleTask.propagateChanges(changedDocuments, changedLinkInstances);
      }

      String getChanges() {
         final Map<String, Collection> collections = new HashMap<>();
         final Map<String, LinkType> linkTypes = new HashMap<>();
         final StringBuilder sb = new StringBuilder("");

         changes.forEach(change -> {
            if (change instanceof DocumentCreation) {
               final DocumentCreation documentCreation = (DocumentCreation) change;
               final Collection collection = collections.computeIfAbsent(documentCreation.getEntity().getCollectionId(), id -> ruleTask.getDaoContextSnapshot().getCollectionDao().getCollectionById(id));
               sb.append("new Document(" + collection.getName() + ")\n");
            } else if (change instanceof DocumentChange) {
               final DocumentChange documentChange = (DocumentChange) change;
               final Collection collection = collections.computeIfAbsent(documentChange.getEntity().getCollectionId(), id -> ruleTask.getDaoContextSnapshot().getCollectionDao().getCollectionById(id));
               appendChange(sb, collection.getName(), collection.getAttributes(), documentChange);
            } else if (change instanceof LinkChange) {
               final LinkChange linkChange = (LinkChange) change;
               final LinkType linkType = linkTypes.computeIfAbsent(linkChange.getEntity().getId(), id -> ruleTask.getDaoContextSnapshot().getLinkTypeDao().getLinkType(id));
               appendChange(sb, linkType.getName(), linkType.getAttributes(), linkChange);
            } else if (change instanceof  UserMessageChange) {
               sb.append(change.toString());
            }
         });

         return sb.toString();
      }

      private void appendChange(final StringBuilder sb, final String name, final java.util.Collection<Attribute> attributes, final Change change) {
         if (change instanceof ResourceChange) {
            final ResourceChange resourceChange = (ResourceChange) change;
            sb.append(name + "(" + last4(((WithId) resourceChange.getEntity()).getId()) + "): ");
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

   public static class DocumentChange extends ResourceChange<Document> {

      public DocumentChange(final Document entity, final String attrId, final Object value) {
         super(entity, attrId, value);
      }
   }

   public static class LinkChange extends ResourceChange<LinkInstance> {

      public LinkChange(final LinkInstance entity, final String attrId, final Object value) {
         super(entity, attrId, value);
      }
   }

   public static class DocumentBridge {
      private Document document;

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
      private LinkInstance link;

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

      final String jsCode = (js.contains(MomentJsParser.FORMAT_JS_DATE) || js.contains(MomentJsParser.PARSE_JS_DATE) ? momentJsCode + ";\n" : "") + js;

      context.eval("js", jsCode);
   }

   public void commitChanges(final TaskExecutor taskExecutor) {
      lumeerBridge.commitChanges(taskExecutor);
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

   public boolean isDryRun() {
      return dryRun;
   }

   public void setDryRun(final boolean dryRun) {
      this.dryRun = dryRun;
   }
}
