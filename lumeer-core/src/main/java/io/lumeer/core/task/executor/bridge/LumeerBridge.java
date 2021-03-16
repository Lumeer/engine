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
package io.lumeer.core.task.executor.bridge;

import static java.util.stream.Collectors.*;

import io.lumeer.api.SelectedWorkspace;
import io.lumeer.api.model.AllowedPermissions;
import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Language;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.WithId;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.task.ContextualTask;
import io.lumeer.core.task.executor.operation.NavigationOperation;
import io.lumeer.core.task.executor.operation.SendEmailOperation;
import io.lumeer.core.task.executor.request.NavigationRequest;
import io.lumeer.core.task.executor.request.PrintRequest;
import io.lumeer.core.task.Task;
import io.lumeer.core.task.TaskExecutor;
import io.lumeer.core.task.executor.request.SendEmailRequest;
import io.lumeer.core.task.executor.request.UserMessageRequest;
import io.lumeer.core.task.executor.ChangesTracker;
import io.lumeer.core.task.executor.ResourceOperation;
import io.lumeer.core.task.executor.operation.DocumentCreationOperation;
import io.lumeer.core.task.executor.operation.DocumentOperation;
import io.lumeer.core.task.executor.operation.DocumentRemovalOperation;
import io.lumeer.core.task.executor.operation.LinkCreationOperation;
import io.lumeer.core.task.executor.operation.LinkOperation;
import io.lumeer.core.task.executor.operation.Operation;
import io.lumeer.core.task.executor.operation.PrintAttributeOperation;
import io.lumeer.core.task.executor.operation.UserMessageOperation;
import io.lumeer.core.util.DocumentUtils;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SearchQueryStem;

import org.apache.commons.lang3.StringUtils;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LumeerBridge {
   private static final String CREATE_PREFIX = "CREATE_";

   private static final DefaultConfigurationProducer configurationProducer = new DefaultConfigurationProducer();
   private static final ConstraintManager constraintManager = ConstraintManager.getInstance(configurationProducer);
   private final ContextualTask task;
   private final Collection collection;
   private final ChangesTracker changesTracker = new ChangesTracker();
   private Set<Operation<?>> operations = new HashSet<>();
   private Exception cause = null;
   private boolean dryRun = false;
   private boolean printed = false;
   private Boolean isManager = null;

   public LumeerBridge(final ContextualTask task, final Collection collection) {
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
         operations.add(new UserMessageOperation(new UserMessageRequest(type, message)));
      }
   }

   public DocumentBridge createDocument(final String collectionId) {
      if (task.getDaoContextSnapshot().increaseCreationCounter() <= Task.MAX_CREATED_AND_DELETED_DOCUMENTS_AND_LINKS) {
         try {
            final Document d = new Document(collectionId, ZonedDateTime.now(), null, task.getInitiator().getId(), null, 0, null);
            d.createIfAbsentMetaData().put(Document.META_CORRELATION_ID, CREATE_PREFIX + UUID.randomUUID().toString());
            d.setData(new DataDocument());

            operations.add(new DocumentCreationOperation(d));

            return new DocumentBridge(d);
         } catch (Exception e) {
            cause = e;
            throw e;
         }
      } else {
         return null;
      }
   }

   private void initIsManager() {
      if (isManager == null) {
         isManager = PermissionsChecker.isManager(
               task.getInitiator(),
               task.getDaoContextSnapshot().getSelectedWorkspace().getOrganization().orElse(null),
               task.getDaoContextSnapshot().getSelectedWorkspace().getProject().orElse(null));
      }
   }

   public List<DocumentBridge> getSiblings(final String linkTypeId, final DocumentBridge sourceDocument) {
      try {
         final String sourceDocumentId = sourceDocument.getDocument().getId();
         final List<LinkInstance> counterparts = getLinkInstances(sourceDocumentId, linkTypeId);
         final Set<String> counterpartIds = counterparts
               .stream()
               .map(l -> l.getDocumentIds().get(0).equals(sourceDocumentId) ? l.getDocumentIds().get(1) : l.getDocumentIds().get(0))
               .collect(toSet());
         final List<LinkInstance> instances = getLinkInstances(counterpartIds, linkTypeId);
         final Set<String> documentIds = instances.stream().flatMap(l -> l.getDocumentIds().stream()).collect(toSet());
         documentIds.removeAll(counterpartIds);
         documentIds.remove(sourceDocumentId);

         final Collection collection = task.getDaoContextSnapshot().getCollectionDao().getCollectionById(sourceDocument.getDocument().getCollectionId());
         final List<Document> documents = DocumentUtils.loadDocumentsWithData(task.getDaoContextSnapshot(), collection, documentIds, constraintManager, true);

         return documents.stream().map(DocumentBridge::new).collect(toList());
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   @SuppressWarnings("unused")
   public List<DocumentBridge> readView(final String viewId) {
      try {
         final View view = task.getDaoContextSnapshot().getViewDao().getViewById(viewId);
         final Query query = view.getQuery().getFirstStem(0, Task.MAX_VIEW_DOCUMENTS);
         final Language language = Language.fromString(task.getCurrentLocale());

         initIsManager();
         final AllowedPermissions permissions;
         if (isManager) {
            permissions = AllowedPermissions.getAllAllowed();
         } else {
            permissions = AllowedPermissions.getAllowedPermissions(task.getInitiator().getId(), view.getPermissions());
         }

         final List<Document> documents = DocumentUtils.getDocuments(task.getDaoContextSnapshot(), query, task.getInitiator(), language, permissions, task.getTimeZone());

         return documents.stream().map(DocumentBridge::new).collect(toList());
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   @SuppressWarnings("unused")
   public void setLinkAttribute(final LinkBridge l, final String attrId, final Value value) {
      try {
         operations.add(new LinkOperation(l.getLink(), attrId, convertValue(value)));
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   public void setDocumentAttribute(final DocumentBridge d, final String attrId, final Value value) {
      try {
         operations.add(new DocumentOperation(d.getDocument(), attrId, convertValue(value)));
         if (d.getDocument() != null) {
            if (d.getDocument().getData() != null) {
               d.getDocument().getData().append(attrId, convertValue(value));
            } else {
               d.getDocument().setData(new DataDocument().append(attrId, convertValue(value)));
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
            operations.add(new DocumentRemovalOperation(d.getDocument()));
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
                  d1.getDocument().getId() != null ? d1.getDocument().getId() : d1.getDocument().createIfAbsentMetaData().getString(Document.META_CORRELATION_ID),
                  d2.getDocument().getId() != null ? d2.getDocument().getId() : d2.getDocument().createIfAbsentMetaData().getString(Document.META_CORRELATION_ID)
            ));
            link.setTemplateId(CREATE_PREFIX + UUID.randomUUID().toString());
            link.setData(new DataDocument());

            operations.add(new LinkCreationOperation(link));

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
               d.getDocument().getCollectionId(),
               d.getDocument().getId(),
               attrId,
               ResourceType.COLLECTION
         );
         operations.add(new PrintAttributeOperation(pq));
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
               l.getLink().getLinkTypeId(),
               l.getLink().getId(),
               attrId,
               ResourceType.LINK
         );
         operations.add(new PrintAttributeOperation(pq));
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
      return getLinkInstances(Set.of(documentId), linkTypeId);
   }

   private List<LinkInstance> getLinkInstances(final Set<String> documentIds, final String linkTypeId) {
      final SearchQuery query = SearchQuery
            .createBuilder()
            .stems(Collections.singletonList(
                  SearchQueryStem
                        .createBuilder("")
                        .linkTypeIds(Collections.singletonList(linkTypeId))
                        .documentIds(documentIds)
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
         final List<LinkInstance> links = getLinkInstances(d.getDocument().getId(), linkTypeId);

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
         List<Document> documents = task.getDaoContextSnapshot().getDocumentDao().getDocumentsByIds(l.getLink().getDocumentIds().toArray(new String[0]));
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
         final List<LinkInstance> links = getLinkInstances(d.getDocument().getId(), linkTypeId);
         final String otherCollectionId = linkType.getCollectionIds().get(0).equals(collection.getId()) ?
               linkType.getCollectionIds().get(1) : linkType.getCollectionIds().get(0);

         // load linked document ids
         if (links.size() > 0) {
            final Set<String> documentIds = links.stream()
                                                 .map(LinkInstance::getDocumentIds)
                                                 .flatMap(java.util.Collection::stream)
                                                 .collect(Collectors.toSet());
            documentIds.remove(d.getDocument().getId());

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
         if (l.getLink() == null || l.getLink().getData() == null) {
            return Value.asValue(null);
         }

         return filterValue(l.getLink().getData().get(attrId));
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   public Value getDocumentAttribute(final DocumentBridge d, final String attrId) {
      try {
         if (d.getDocument() == null || d.getDocument().getData() == null) {
            return Value.asValue(null);
         }

         return filterValue(d.getDocument().getData().get(attrId));
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   @SuppressWarnings("unused")
   public List<Value> getLinkAttribute(final List<LinkBridge> links, final String attrId) {
      try {
         final List<Value> result = new ArrayList<>();
         links.forEach(link -> result.add(filterValue(link.getLink() != null && link.getLink().getData() != null ? link.getLink().getData().get(attrId) : Value.asValue(null))));

         return result;
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   public List<Value> getDocumentAttribute(List<DocumentBridge> docs, String attrId) {
      try {
         final List<Value> result = new ArrayList<>();
         docs.forEach(doc -> result.add(filterValue(doc.getDocument() != null && doc.getDocument().getData() != null ? doc.getDocument().getData().get(attrId) : Value.asValue(null))));

         return result;
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   @SuppressWarnings("unused")
   public void navigate(final String viewId, final DocumentBridge documentBridge, final boolean sidebar, final boolean newWindow) {
      try {
         final SelectedWorkspace workspace = task.getDaoContextSnapshot().getSelectedWorkspace();
         if (workspace.getOrganization().isPresent() && workspace.getProject().isPresent()) {

            final View view = task.getDaoContextSnapshot().getViewDao().getViewById(viewId);
            if (view != null && view.getQuery().getStems().size() > 0 && StringUtils.isNotEmpty(view.getQuery().getStems().get(0).getCollectionId())) {
               final String collectionId = view.getQuery().getStems().get(0).getCollectionId();
               final String documentId = documentBridge.getDocument().getId();
               final Collection collection = task.getDaoContextSnapshot().getCollectionDao().getCollectionById(collectionId);
               final String attributeId = StringUtils.isNotEmpty(collection.getDefaultAttributeId()) ? collection.getDefaultAttributeId() :
                     (collection.getAttributes() != null && !collection.getAttributes().isEmpty() ? collection.getAttributes().iterator().next().getId() : "");
               final NavigationRequest navigationRequest = new NavigationRequest(
                     workspace.getOrganization().get().getCode(), workspace.getProject().get().getCode(),
                     viewId, collectionId, documentId, attributeId, sidebar, newWindow
               );

               operations.add(new NavigationOperation(navigationRequest));
            }
         }
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   @SuppressWarnings("unused")
   public void sendEmail(final String email, final String subject, final String body) {
      try {
         operations.add(new SendEmailOperation(new SendEmailRequest(subject, email, body)));
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   public ChangesTracker commitDryRunOperations(final TaskExecutor taskExecutor) {
      if (operations.isEmpty()) {
         return null;
      }

      final String correlationId = task.getCorrelationId();
      if (StringUtils.isNotEmpty(correlationId)) {
         final List<UserMessageRequest> userMessageRequests = operations.stream().filter(operation -> operation instanceof UserMessageOperation).map(operation -> ((UserMessageOperation) operation).getEntity()).collect(toList());
         changesTracker.addUserMessageRequests(userMessageRequests);
      }

      return changesTracker;
   }

   public String getOperationsDescription() {
      final Map<String, Collection> collections = new HashMap<>();
      final Map<String, LinkType> linkTypes = new HashMap<>();
      final StringBuilder sb = new StringBuilder();

      final LongAdder i = new LongAdder();
      operations.forEach(operation -> {
         if (operation instanceof DocumentCreationOperation) {
            final DocumentCreationOperation documentCreationOperation = (DocumentCreationOperation) operation;
            if (StringUtils.isEmpty(documentCreationOperation.getEntity().getId())) {
               i.increment();
               documentCreationOperation.getEntity().setId("NEW" + i.intValue());
            }
         }
      });

      operations.forEach(operation -> {
         if (operation instanceof DocumentCreationOperation) {
            final DocumentCreationOperation documentCreationOperation = (DocumentCreationOperation) operation;
            final Collection collection = collections.computeIfAbsent(documentCreationOperation.getEntity().getCollectionId(), id -> task.getDaoContextSnapshot().getCollectionDao().getCollectionById(id));
            sb.append("new Document(").append(collection.getName()).append(")\n");
         } else if (operation instanceof DocumentOperation) {
            final DocumentOperation documentChange = (DocumentOperation) operation;
            final Collection collection = collections.computeIfAbsent(documentChange.getEntity().getCollectionId(), id -> task.getDaoContextSnapshot().getCollectionDao().getCollectionById(id));
            appendOperation(sb, collection.getName(), collection.getAttributes(), documentChange);
         } else if (operation instanceof LinkOperation) {
            final LinkOperation linkChange = (LinkOperation) operation;
            final LinkType linkType = linkTypes.computeIfAbsent(linkChange.getEntity().getId(), id -> task.getDaoContextSnapshot().getLinkTypeDao().getLinkType(id));
            appendOperation(sb, linkType.getName(), linkType.getAttributes(), linkChange);
         } else if (operation instanceof UserMessageOperation || operation instanceof  PrintAttributeOperation || operation instanceof NavigationOperation || operation instanceof SendEmailOperation) {
            sb.append(operation.toString());
         } else if (operation instanceof LinkCreationOperation) {
            final LinkCreationOperation linkCreationOperation = (LinkCreationOperation) operation;
            final LinkType linkType = linkTypes.computeIfAbsent(linkCreationOperation.getEntity().getLinkTypeId(), id -> task.getDaoContextSnapshot().getLinkTypeDao().getLinkType(id));
            sb.append("new Link(").append(linkType.getName()).append(")\n");
         }
      });

      return sb.toString();
   }

   @SuppressWarnings("rawtypes")
   private void appendOperation(final StringBuilder sb, final String name, final java.util.Collection<Attribute> attributes, final Operation operation) {
      if (operation instanceof ResourceOperation) {
         final ResourceOperation resourceChange = (ResourceOperation) operation;
         sb.append(name).append("(").append(StringUtils.right(((WithId) resourceChange.getEntity()).getId(), 4)).append("): ");
         sb.append(attributes.stream().filter(a -> a.getId().equals(resourceChange.getAttrId())).map(Attribute::getName).findFirst().orElse(""));
         sb.append(" = ");
         sb.append(resourceChange.getValue());
         sb.append("\n");
      }
   }

   public Exception getCause() {
      return cause;
   }

   public void setCause(final Exception cause) {
      this.cause = cause;
   }

   public boolean isDryRun() {
      return dryRun;
   }

   public void setDryRun(final boolean dryRun) {
      this.dryRun = dryRun;
   }

   public Set<Operation<?>> getOperations() {
      return operations;
   }

   public void setOperations(final Set<Operation<?>> operations) {
      this.operations = operations;
   }
}
