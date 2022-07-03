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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import io.lumeer.api.SelectedWorkspace;
import io.lumeer.api.model.AllowedPermissions;
import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.FileAttachment;
import io.lumeer.api.model.Group;
import io.lumeer.api.model.Language;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.ResourceVariable;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.api.model.User;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.WithId;
import io.lumeer.api.util.CollectionUtil;
import io.lumeer.api.util.LinkTypeUtil;
import io.lumeer.api.util.PermissionUtils;
import io.lumeer.core.adapter.PaymentAdapter;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.pdf.PdfCreator;
import io.lumeer.core.task.ContextualTask;
import io.lumeer.core.task.Task;
import io.lumeer.core.task.TaskExecutor;
import io.lumeer.core.task.executor.ChangesTracker;
import io.lumeer.core.task.executor.operation.AddDocumentFileAttachmentOperation;
import io.lumeer.core.task.executor.operation.AddLinkFileAttachmentOperation;
import io.lumeer.core.task.executor.operation.DocumentCreationOperation;
import io.lumeer.core.task.executor.operation.DocumentOperation;
import io.lumeer.core.task.executor.operation.DocumentRemovalOperation;
import io.lumeer.core.task.executor.operation.DummySequenceOperation;
import io.lumeer.core.task.executor.operation.LinkCreationOperation;
import io.lumeer.core.task.executor.operation.LinkOperation;
import io.lumeer.core.task.executor.operation.NavigationOperation;
import io.lumeer.core.task.executor.operation.Operation;
import io.lumeer.core.task.executor.operation.PrintAttributeOperation;
import io.lumeer.core.task.executor.operation.PrintTextOperation;
import io.lumeer.core.task.executor.operation.ResourceOperation;
import io.lumeer.core.task.executor.operation.SendEmailOperation;
import io.lumeer.core.task.executor.operation.SendSmtpEmailOperation;
import io.lumeer.core.task.executor.operation.UserMessageOperation;
import io.lumeer.core.task.executor.operation.ViewPermissionsOperation;
import io.lumeer.core.task.executor.operation.data.FileAttachmentData;
import io.lumeer.core.task.executor.request.NavigationRequest;
import io.lumeer.core.task.executor.request.PrintRequest;
import io.lumeer.core.task.executor.request.SendEmailRequest;
import io.lumeer.core.task.executor.request.SendSmtpEmailRequest;
import io.lumeer.core.task.executor.request.SmtpConfiguration;
import io.lumeer.core.task.executor.request.SmtpConfigurationBuilder;
import io.lumeer.core.task.executor.request.TextPrintRequest;
import io.lumeer.core.task.executor.request.UserMessageRequest;
import io.lumeer.core.util.DocumentUtils;
import io.lumeer.core.util.EmailSecurityType;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SearchQueryStem;

import com.floreysoft.jmte.Engine;
import org.apache.commons.lang3.StringUtils;
import org.graalvm.polyglot.Value;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

public class LumeerBridge {
   private static final String CREATE_PREFIX = "CREATE_";

   private static final DefaultConfigurationProducer configurationProducer = new DefaultConfigurationProducer();
   private static final ConstraintManager constraintManager = ConstraintManager.getInstance(configurationProducer);
   private final ContextualTask task;
   private final ServiceLimits serviceLimits;
   private final ChangesTracker changesTracker = new ChangesTracker();
   private Set<Operation<?>> operations = new HashSet<>();
   private Exception cause = null;
   private boolean dryRun = false;
   private boolean printed = false;

   private Engine templateEngine = null;

   public LumeerBridge(final ContextualTask task) {
      this.task = task;
      PaymentAdapter paymentAdapter = new PaymentAdapter(task.getDaoContextSnapshot().getPaymentDao(), null);
      serviceLimits = paymentAdapter.computeServiceLimits(task.getDaoContextSnapshot().getOrganization(), false);
   }

   @SuppressWarnings("unused")
   public String getSequenceNumber(final String sequenceName, final int digits) {
      final String format = "%" + (digits <= 1 ? "" : "0" + digits) + "d";

      if (dryRun) {
         return String.format(format, 1);
      } else {
         final int sequenceValue = task.getDaoContextSnapshot().getSequenceDao().getNextSequenceNo(sequenceName);
         operations.add(new DummySequenceOperation(sequenceName));
         changesTracker.addSequence(sequenceName);

         return String.format(format, sequenceValue);
      }
   }

   public String getCurrentUser() {
      final String email = task.getInitiator().getEmail();
      return email == null ? "" : email;
   }

   @SuppressWarnings("unused")
   public String[] getUserTeams() {
      return task.getDaoContextSnapshot().getGroupDao().getAllGroups().stream().filter(group ->
            group.getUsers().stream().anyMatch(user -> user.equals(task.getInitiator().getId()))
      ).map(Group::getName).toArray(String[]::new);
   }

   @SuppressWarnings("unused")
   public String[] getUserTeams(final String userEmail) {
      final User user = task.getDaoContextSnapshot().getUserDao().getUserByEmail(userEmail);

      return user == null ? new String[0] : task.getDaoContextSnapshot().getGroupDao().getAllGroups().stream().filter(group ->
         group.getUsers().stream().anyMatch(u -> u.equals(user.getId()))
      ).map(Group::getName).toArray(String[]::new);
   }

   @SuppressWarnings("unused")
   public String[] getUserTeamIds(final String userEmail) {
      final User user = task.getDaoContextSnapshot().getUserDao().getUserByEmail(userEmail);

      return user == null ? new String[0] : task.getDaoContextSnapshot().getGroupDao().getAllGroups().stream().filter(group ->
              group.getUsers().stream().anyMatch(u -> u.equals(user.getId()))
      ).map(g -> "@" + g.getId()).toArray(String[]::new);
   }

   @SuppressWarnings("unused")
   public boolean isUserInTeam(final String team) {
      final Optional<Group> group = task.getDaoContextSnapshot().getGroupDao().getAllGroups().stream().filter(g -> g.getName().equals(team)).findFirst();

      return group.map(value -> value.getUsers().stream().anyMatch(user -> user.equals(task.getInitiator().getId()))).orElse(false);
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

   @SuppressWarnings("unused")
   public void copyValues(final DocumentBridge from, final DocumentBridge to, final List<String> attributes) {
      if (from != null && to != null) {
         if (from.getDocument().getId() != null) {
            to.getDocument().createIfAbsentMetaData().put(Document.META_ORIGINAL_DOCUMENT_ID, from.getDocument().getId());

            if (from.getDocument().createIfAbsentMetaData().get(Document.META_PARENT_ID) != null) {
               to.getDocument().getMetaData().put(Document.META_ORIGINAL_PARENT_ID, from.getDocument().getMetaData().getString(Document.META_PARENT_ID));
            }
         }

         final Collection fromCollection = task.getDaoContextSnapshot().getCollectionDao().getCollectionById(from.getDocument().getCollectionId());
         final Collection toCollection = from.getDocument().getCollectionId().equals(to.getDocument().getCollectionId()) ? fromCollection : task.getDaoContextSnapshot().getCollectionDao().getCollectionById(to.getDocument().getCollectionId());
         final Map<String, String> attributesToCopy = new HashMap<>();

         fromCollection.getAttributes()
                       .stream()
                       .filter(attr -> attributes == null || attributes.size() == 0 || attributes.contains(attr.getName()))
                       .forEach(attr -> {
            var a = CollectionUtil.getAttributeByName(toCollection, attr.getName());

            if (a != null) {
               attributesToCopy.put(attr.getId(), a.getId());
            }
         });

         if (to.getDocument().getData() == null) {
            to.getDocument().setData(new DataDocument());
         }

         attributesToCopy.forEach((fromId, toId) ->
            setDocumentAttributeInternal(to, toId, from.getDocument().getData().get(fromId))
         );
      }
   }

   public DocumentBridge createDocument(final String collectionId) {
      if (task.getDaoContextSnapshot().increaseCreationCounter() <= getMaxCreatedRecords()) {
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

   @SuppressWarnings("unused")
   public DocumentBridge getParentDocument(final DocumentBridge sourceDocument) {
      try {
         final String parentId = sourceDocument.getDocument().createIfAbsentMetaData().getString(Document.META_PARENT_ID);

         if (StringUtils.isNotEmpty(parentId)) {
            final Collection collection = task.getDaoContextSnapshot().getCollectionDao().getCollectionById(sourceDocument.getDocument().getCollectionId());
            final List<Document> documents = DocumentUtils.loadDocumentsWithData(task.getDaoContextSnapshot(), collection, Set.of(parentId), constraintManager, true);

            if (documents.size() == 1) {
               return new DocumentBridge(documents.get(0));
            }
         }

         return null;
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   @SuppressWarnings("unused")
   public List<DocumentBridge> getChildDocuments(final DocumentBridge sourceDocument) {
      try {
         final String parentId = sourceDocument.getDocument().getId();
         final List<Document> documents = task.getDaoContextSnapshot().getDocumentDao().getDocumentsByParentId(parentId);

         if (!documents.isEmpty()) {
            final Collection collection = task.getDaoContextSnapshot().getCollectionDao().getCollectionById(sourceDocument.getDocument().getCollectionId());
            final List<Document> documentsWithData = DocumentUtils.loadDocumentsData(task.getDaoContextSnapshot(), collection, documents, constraintManager, true);

            return documentsWithData.stream().map(DocumentBridge::new).collect(toList());
         }

         return List.of();
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   @SuppressWarnings("unused")
   public List<DocumentBridge> getHierarchySiblings(final DocumentBridge sourceDocument) {
      try {
         final DocumentBridge parent = getParentDocument(sourceDocument);

         if (parent != null) {
            final List<DocumentBridge> documents = getChildDocuments(parent);

            return documents.stream().filter(db -> !db.getDocument().getId().equals(sourceDocument.getDocument().getId())).collect(toList());
         }

         return List.of();
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   @SuppressWarnings("unused")
   public List<DocumentBridge> getSiblings(final String linkTypeId, final DocumentBridge sourceDocument) {
      try {
         final String sourceDocumentId = sourceDocument.getDocument().getId();
         final List<LinkInstance> counterparts = getLinkInstances(sourceDocumentId, linkTypeId);

         if (!counterparts.isEmpty()) {
            final Set<String> counterpartIds = counterparts
                  .stream()
                  .map(l -> l.getDocumentIds().get(0).equals(sourceDocumentId) ? l.getDocumentIds().get(1) : l.getDocumentIds().get(0))
                  .collect(toSet());
            final List<LinkInstance> instances = getLinkInstances(counterpartIds, linkTypeId);
            final Set<String> documentIds = instances.stream().flatMap(l -> l.getDocumentIds().stream()).collect(toSet());
            documentIds.removeAll(counterpartIds);
            documentIds.remove(sourceDocumentId);

            if (!documentIds.isEmpty()) {
               final Collection collection = task.getDaoContextSnapshot().getCollectionDao().getCollectionById(sourceDocument.getDocument().getCollectionId());
               final List<Document> documents = DocumentUtils.loadDocumentsWithData(task.getDaoContextSnapshot(), collection, documentIds, constraintManager, true);

               return documents.stream().map(DocumentBridge::new).collect(toList());
            }
         }

         return List.of();
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   @SuppressWarnings("unused")
   public String getViewName(final String viewId) {
      try {
         if (StringUtils.isNotEmpty(viewId)) {
            final View view = task.getDaoContextSnapshot().getViewDao().getViewById(viewId);

            return view.getName();
         }
      } catch (Exception e) {
         cause = e;
         throw e;
      }

      return "";
   }

   @SuppressWarnings("unused")
   public String getViewName(final List<String> viewIds) {
      try {
         if (viewIds != null) {
            return viewIds.stream().filter(StringUtils::isNotEmpty).map(viewId ->
               task.getDaoContextSnapshot().getViewDao().getViewById(viewId).getName()
            ).collect(Collectors.joining(", "));
         }
      } catch (Exception e) {
         cause = e;
         throw e;
      }

      return "";
   }

   @SuppressWarnings("unused")
   public List<DocumentBridge> readView(final String viewId) {
      try {
         final View view = task.getDaoContextSnapshot().getViewDao().getViewById(viewId);
         final Query query = view.getQuery().getFirstStem(0, Task.MAX_VIEW_DOCUMENTS);
         final Language language = Language.fromString(task.getCurrentLocale());

         final Set<RoleType> roles = PermissionUtils.getUserRolesInResource(task.getDaoContextSnapshot().getOrganization(), task.getDaoContextSnapshot().getProject(), view, task.getInitiator(), task.getGroups());
         final AllowedPermissions permissions = new AllowedPermissions(roles);

         final List<Document> documents = DocumentUtils.getDocuments(task.getDaoContextSnapshot(), query, task.getInitiator(), language, permissions, task.getTimeZone());

         return documents.stream().map(DocumentBridge::new).collect(toList());
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   @SuppressWarnings("unused")
   public LinkOperation setLinkAttribute(final LinkBridge l, final String attrId, final Value value) {
      try {
         final LinkOperation operation = new LinkOperation(l.getLink(), attrId, convertValue(value));
         operations.add(operation);

         return operation;
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   @SuppressWarnings("unused")
   public void writePdf(final DocumentBridge d, final String attrId, final String fileName, final boolean overwrite, final String html) throws IOException {
      try {
         if (html.length() > 5L*1024*1024) {
            throw new IllegalArgumentException("Input HTML too large.");
         }
         final ByteArrayOutputStream baos = new ByteArrayOutputStream();
         PdfCreator.createPdf(new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)), baos);

         var relatedOperation = setDocumentAttribute(d, attrId, Value.asValue(getUpdateFileAttachmentsList(d, attrId, fileName, overwrite)));

         operations.add(new AddDocumentFileAttachmentOperation(d.getDocument(), attrId, new FileAttachmentData(baos.toByteArray(), fileName, overwrite), relatedOperation));
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   @SuppressWarnings("unused")
   public void writePdf(final LinkBridge l, final String attrId, final String fileName, final boolean overwrite, final String html) throws IOException {
      try {
         if (html.length() > 5L*1024*1024) {
            throw new IllegalArgumentException("Input HTML too large.");
         }
         final ByteArrayOutputStream baos = new ByteArrayOutputStream();
         PdfCreator.createPdf(new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)), baos);

         var relatedOperation = setLinkAttribute(l, attrId, Value.asValue(getUpdateFileAttachmentsList(l, attrId, fileName, overwrite)));

         operations.add(new AddLinkFileAttachmentOperation(l.getLink(), attrId, new FileAttachmentData(baos.toByteArray(), fileName, overwrite), relatedOperation));
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   private String getUpdateFileAttachmentsList(final DocumentBridge d, final String attrId, final String fileName, final boolean overwrite) {
      var fileNames = task.getDaoContextSnapshot().getFileAttachmentDao().findAllFileAttachments(
            task.getDaoContextSnapshot().getOrganization(),
            task.getDaoContextSnapshot().getProject(),
            d.getDocument().getCollectionId(),
            d.getDocument().getId(),
            attrId,
            FileAttachment.AttachmentType.DOCUMENT
      ).stream().filter(fa -> !fa.getFileName().equals(fileName)).map(fa -> "'" + (fa.getId() + ":" + fa.getFileName()).replaceAll("([\\'\\\\])", "\\\\$1") + "'").collect(toList());

      fileNames.add("'TEMP_" + UUID.randomUUID() + ":" + fileName + "'");

      return "[" + String.join(",", fileNames) + "]";
   }

   private String getUpdateFileAttachmentsList(final LinkBridge l, final String attrId, final String fileName, final boolean overwrite) {
      var fileNames = task.getDaoContextSnapshot().getFileAttachmentDao().findAllFileAttachments(
            task.getDaoContextSnapshot().getOrganization(),
            task.getDaoContextSnapshot().getProject(),
            l.getLink().getLinkTypeId(),
            l.getLink().getId(),
            attrId,
            FileAttachment.AttachmentType.LINK
      ).stream().filter(fa -> !fa.getFileName().equals(fileName)).map(fa -> "'" + (fa.getId() + ":" + fa.getFileName()).replaceAll("([\\'\\\\])", "\\\\$1") + "'").collect(toList());

      fileNames.add(fileName);

      return "[" + String.join(",", fileNames) + "]";
   }

   @SuppressWarnings("unused")
   public String getVariable(final String variableName) {
      try {
         final ResourceVariable variable = task.getDaoContextSnapshot().getResourceVariableDao().getVariableByName(task.getDaoContextSnapshot().getOrganizationId(), task.getDaoContextSnapshot().getProjectId(), variableName);

         if (variable != null && variable.getValue() != null) {
            return variable.getValue().toString();
         }

         return "";
      }  catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   @SuppressWarnings("unused")
   public void sendEmail(final String to, final String fromName, final String subject, final String body, final Value smtpConfig) {
      if (task.getDaoContextSnapshot().increaseEmailCounter() <= Task.MAX_EMAILS) {
         final SmtpConfiguration smtpConfiguration = getSmtpConfiguration(smtpConfig);

         if (smtpConfiguration != null) {
            final SendSmtpEmailRequest sendSmtpEmailRequest = new SendSmtpEmailRequest(subject, to, body, fromName, smtpConfiguration);

            operations.add(new SendSmtpEmailOperation(sendSmtpEmailRequest));
         }
      }
   }

   private void registerAttachment(final SendSmtpEmailRequest req, final DocumentBridge d, final String attrId) throws IllegalStateException {
      if (d != null) {
         if (getAttachmentsSize(d.getDocument(), attrId) > getMaxFileAttachmentSize()) {
            throw new IllegalStateException("Attachments size is larger than " + getMaxFileAttachmentSizeMb() + "MB.");
         }

         req.setAttachment(d.getDocument(), attrId);
      }
   }

   private void registerAttachment(final SendSmtpEmailRequest req, final LinkBridge l, final String attrId) throws IllegalStateException {
      if (l != null) {
         if (getAttachmentsSize(l.getLink(), attrId) > getMaxFileAttachmentSize()) {
            throw new IllegalStateException("Attachments size is larger than " + getMaxFileAttachmentSizeMb() + "MB.");
         }

         req.setAttachment(l.getLink(), attrId);
      }
   }

   @SuppressWarnings("unused")
   public void sendEmail(final String to, final String fromName, final String subject, final String body, final DocumentBridge d, final String attrId, final Value smtpConfig) throws Exception {
      if (task.getDaoContextSnapshot().increaseEmailCounter() <= Task.MAX_EMAILS) {
         if (StringUtils.isNotEmpty(to)) {
            if (to.split(",").length > Task.MAX_EMAIL_RECIPIENTS) {
               cause = new IllegalStateException("Too many email recipients (more than 100).");
               throw cause;
            }

            final SmtpConfiguration smtpConfiguration = getSmtpConfiguration(smtpConfig);

            if (smtpConfiguration != null) {
               final SendSmtpEmailRequest sendSmtpEmailRequest = new SendSmtpEmailRequest(subject, to, body, fromName, smtpConfiguration);
               registerAttachment(sendSmtpEmailRequest, d, attrId);

               operations.add(new SendSmtpEmailOperation(sendSmtpEmailRequest));
            }
         } else {
            cause = new IllegalStateException("Recipients list is empty.");
            throw cause;
         }
      } else {
         cause = new IllegalStateException("Too many requests to send email in a single rule (more than 3).");
         throw cause;
      }
   }

   @SuppressWarnings("unused")
   public void sendEmail(final String to, final String fromName, final String subject, final String body, final LinkBridge l, final String attrId, final Value smtpConfig) throws Exception {
      if (task.getDaoContextSnapshot().increaseEmailCounter() <= Task.MAX_EMAILS) {
         if (StringUtils.isNotEmpty(to) && to.split(",").length > Task.MAX_EMAIL_RECIPIENTS) {
            cause = new IllegalStateException("Too many email recipients (more than 100).");
            throw cause;
         }

         final SmtpConfiguration smtpConfiguration = getSmtpConfiguration(smtpConfig);

         if (smtpConfiguration != null) {
            final SendSmtpEmailRequest sendSmtpEmailRequest = new SendSmtpEmailRequest(subject, to, body, fromName, smtpConfiguration);
            registerAttachment(sendSmtpEmailRequest, l, attrId);

            operations.add(new SendSmtpEmailOperation(sendSmtpEmailRequest));
         }
      } else {
         cause = new IllegalStateException("Too many requests to send email in a single rule (more than 3).");
         throw cause;
      }
   }

   private SmtpConfiguration getSmtpConfiguration(final Value smtpConfig) {
      if (smtpConfig != null && smtpConfig.hasMembers()) {
         final SmtpConfigurationBuilder smtpConfigurationBuilder = new SmtpConfigurationBuilder();
         smtpConfigurationBuilder.setHost(smtpConfig.hasMember("host") ? smtpConfig.getMember("host").asString() : "");
         smtpConfigurationBuilder.setPort(smtpConfig.hasMember("port") ? smtpConfig.getMember("port").asInt() : 0);
         smtpConfigurationBuilder.setUser(smtpConfig.hasMember("user") ? smtpConfig.getMember("user").asString() : "");
         smtpConfigurationBuilder.setPassword(smtpConfig.hasMember("password") ? smtpConfig.getMember("password").asString() : "");
         smtpConfigurationBuilder.setFrom(smtpConfig.hasMember("from") ? smtpConfig.getMember("from").asString() : "");
         smtpConfigurationBuilder.setEmailSecurityType(smtpConfig.hasMember("security") ? EmailSecurityType.valueOf(smtpConfig.getMember("security").asString()) : EmailSecurityType.NONE);

         return smtpConfigurationBuilder.build();
      }

      return null;
   }

   private long getAttachmentsSize(final Document d, final String attrId) {
      return task.getDaoContextSnapshot().getFileAttachmentDao().findAllFileAttachments(
            task.getDaoContextSnapshot().getOrganization(), task.getDaoContextSnapshot().getProject(),
            d.getCollectionId(), d.getId(), attrId, FileAttachment.AttachmentType.DOCUMENT
      ).stream().mapToLong(FileAttachment::getSize).sum();
   }

   private long getAttachmentsSize(final LinkInstance l, final String attrId) {
      return task.getDaoContextSnapshot().getFileAttachmentDao().findAllFileAttachments(
            task.getDaoContextSnapshot().getOrganization(), task.getDaoContextSnapshot().getProject(),
            l.getLinkTypeId(), l.getId(), attrId, FileAttachment.AttachmentType.LINK
      ).stream().mapToLong(FileAttachment::getSize).sum();
   }

   @SuppressWarnings("unused")
   public synchronized String formatTemplate(final String template, final Value replacements, final String splitter) {
      if (replacements == null) {
         return "";
      }

      if (templateEngine == null) {
         templateEngine = Engine.createEngine();
      }

      final Map<String, Object> patterns = new HashMap<>();

      if (replacements.hasArrayElements()) {
         for (int i = 0; i < replacements.getArraySize(); i++) {
            addTemplatePart(patterns, splitter, replacements.getArrayElement(i).toString());
         }
      } else if (replacements.hasMembers()) {
         replacements.getMemberKeys().stream().filter(key -> replacements.getMember(key) != null).forEach(key -> addTemplatePart(patterns, splitter, replacements.getMember(key).asString()));
      } else {
         addTemplatePart(patterns, splitter, replacements.toString());
      }

      try {
         return templateEngine.transform(template, patterns);
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   private void addTemplatePart(final Map<String, Object> patterns, final String splitter, final String item) {
      var parts = item.split(splitter);
      if (parts.length == 2) {
         patterns.put(parts[0], parts[1]);
      } else {
         throw new IllegalStateException(String.format("Illegal number of parts in a replacement pattern. Splitting with %s and found %d parts.", splitter, parts.length));
      }
   }

   public DocumentOperation setDocumentAttribute(final DocumentBridge d, final String attrId, final Value value) {
      try {
         final DocumentOperation operation = setDocumentAttributeInternal(d, attrId, convertValue(value));

         return operation;
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   private DocumentOperation setDocumentAttributeInternal(final DocumentBridge d, final String attrId, final Object value) {
      final DocumentOperation operation = new DocumentOperation(d.getDocument(), attrId, value);
      operations.add(operation);

      if (d.getDocument() != null) {
         if (d.getDocument().getData() != null) {
            d.getDocument().getData().append(attrId, value);
         } else {
            d.getDocument().setData(new DataDocument().append(attrId, value));
         }
      }

      return operation;
   }

   public void copyDocumentAttributes(final DocumentBridge source, final DocumentBridge target) {
      try {
         source.getDocument().getData().forEach((key, val) -> {
            if (StringUtils.isNotEmpty(key) && !"_id".equals(key))
            operations.add(new DocumentOperation(target.getDocument(), key, val));

            if (target.getDocument() != null) {
               if (target.getDocument().getData() != null) {
                  target.getDocument().getData().append(key, val);
               } else {
                  target.getDocument().setData(new DataDocument().append(key, val));
               }
            }
         });
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   @SuppressWarnings("unused")
   public void removeDocument(final DocumentBridge d) {
      if (task.getDaoContextSnapshot().increaseDeletionCounter() <= getMaxCreatedRecords()) {
         try {
            operations.add(new DocumentRemovalOperation(d.getDocument()));
         } catch (Exception e) {
            cause = e;
            throw e;
         }
      }
   }

   @SuppressWarnings("unused")
   public void removeDocumentsInView(final String viewId) {
      try {
         final View view = task.getDaoContextSnapshot().getViewDao().getViewById(viewId);
         final Query query = view.getQuery().getFirstStem(0, Task.MAX_VIEW_DOCUMENTS);
         final Language language = Language.fromString(task.getCurrentLocale());

         final Set<RoleType> roles = PermissionUtils.getUserRolesInResource(task.getDaoContextSnapshot().getOrganization(), task.getDaoContextSnapshot().getProject(), view, task.getInitiator(), task.getGroups());
         final AllowedPermissions permissions = new AllowedPermissions(roles);

         final List<Document> documents = DocumentUtils.getDocuments(task.getDaoContextSnapshot(), query, task.getInitiator(), language, permissions, task.getTimeZone());

         documents.stream()
                 .filter(d -> task.getDaoContextSnapshot().increaseDeletionCounter() <= getMaxCreatedRecords())
                 .forEach(d -> operations.add(new DocumentRemovalOperation(d)));
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   @SuppressWarnings("unused")
   public LinkBridge linkDocuments(final DocumentBridge d1, final DocumentBridge d2, final String linkTypeId) {
      if (task.getDaoContextSnapshot().increaseCreationCounter() <= getMaxCreatedRecords()) {
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
   public void printText(final String text) {
      final SelectedWorkspace workspace = task.getDaoContextSnapshot().getSelectedWorkspace();
      if (!printed && workspace.getOrganization().isPresent() && workspace.getProject().isPresent()) {
         final TextPrintRequest pq = new TextPrintRequest(
               workspace.getOrganization().get().getCode(),
               workspace.getProject().get().getCode(),
               text
         );

         operations.add(new PrintTextOperation(pq));
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

   @SuppressWarnings({ "unused", "rawtypes" })
   public int getListSize(final List list) {
      return list == null ? 0 : list.size();
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

   @SuppressWarnings("unused")
   public Value getDocumentCreationDate(final DocumentBridge d) {
      try {
         return Value.asValue(d.getDocument().getCreationDate());
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   private String getUser(final String userId) {
      if (task.getInitiator().getId().equals(userId)) {
         return task.getInitiator().getEmail();
      } else {
         final User user = task.getDaoContextSnapshot().getUserDao().getUserById(userId);
         if (user != null) {
            return user.getEmail();
         }
      }

      return "";
   }

   @SuppressWarnings("unused")
   public String getDocumentCreatedBy(final DocumentBridge d) {
      try {
         return getUser(d.getDocument().getCreatedBy());
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   @SuppressWarnings("unused")
   public Value getDocumentUpdateDate(final DocumentBridge d) {
      try {
         return Value.asValue(d.getDocument().getUpdateDate());
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   @SuppressWarnings("unused")
   public String getDocumentUpdatedBy(final DocumentBridge d) {
      try {
         return getUser(d.getDocument().getUpdatedBy());
      } catch (Exception e) {
         cause = e;
         throw e;
      }
   }

   public List<DocumentBridge> getLinkedDocuments(DocumentBridge d, String linkTypeId) {
      try {
         final LinkType linkType = task.getDaoContextSnapshot().getLinkTypeDao().getLinkType(linkTypeId);
         final List<LinkInstance> links = getLinkInstances(d.getDocument().getId(), linkTypeId);
         final String otherCollectionId = linkType.getCollectionIds().get(0).equals(d.getDocument().getCollectionId()) ?
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
      navigate(viewId, documentBridge, "", sidebar, newWindow);
   }

   @SuppressWarnings("unused")
   public void navigate(final String viewId, final String search, final boolean sidebar, final boolean newWindow) {
      navigate(viewId, null, search, sidebar, newWindow);
   }

   @SuppressWarnings("unused")
   private void navigate(final String viewId, final DocumentBridge documentBridge, final String search, final boolean sidebar, final boolean newWindow) {
      try {
         final SelectedWorkspace workspace = task.getDaoContextSnapshot().getSelectedWorkspace();
         if (workspace.getOrganization().isPresent() && workspace.getProject().isPresent()) {

            final View view = task.getDaoContextSnapshot().getViewDao().getViewById(viewId);
            if (view != null && view.getQuery().getStems().size() > 0 && StringUtils.isNotEmpty(view.getQuery().getStems().get(0).getCollectionId())) {
               final String collectionId = view.getQuery().getStems().get(0).getCollectionId();
               final String documentId = documentBridge != null ? documentBridge.getDocument().getId() : null;
               final Collection collection = task.getDaoContextSnapshot().getCollectionDao().getCollectionById(collectionId);
               final String attributeId = StringUtils.isNotEmpty(collection.getDefaultAttributeId()) ? collection.getDefaultAttributeId() :
                       (collection.getAttributes() != null && !collection.getAttributes().isEmpty() ? collection.getAttributes().iterator().next().getId() : "");
               final NavigationRequest navigationRequest = new NavigationRequest(
                       workspace.getOrganization().get().getCode(), workspace.getProject().get().getCode(),
                       viewId, collectionId, documentId, attributeId, sidebar, newWindow, search
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
   public void shareView(final String viewId, final String userEmail, final String roles) {
      try {
         final SelectedWorkspace workspace = task.getDaoContextSnapshot().getSelectedWorkspace();
         if (workspace.getOrganization().isPresent() && workspace.getProject().isPresent()) {

            final View view = task.getDaoContextSnapshot().getViewDao().getViewById(viewId);
            if (view != null) {
               // can the initiator share the view?
               if (PermissionUtils.hasRole(workspace.getOrganization().get(), workspace.getProject().get(), view, RoleType.UserConfig, task.getInitiator(), task.getGroups())) {
                  final User newUser = task.getDaoContextSnapshot().getUserDao().getUserByEmail(userEmail);
                     final Set<RoleType> userRoles = StringUtils.isNotEmpty(roles) && !"none".equals(roles) ? Arrays.stream(roles.split(",")).map(RoleType::fromString).collect(toSet()) : Set.of();

                     operations.add(new ViewPermissionsOperation(view, newUser.getId(), userRoles));
               }
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
         } else if (operation instanceof AddDocumentFileAttachmentOperation) {
            final AddDocumentFileAttachmentOperation addDocumentFileAttachmentOperation = (AddDocumentFileAttachmentOperation) operation;
            final Collection collection = collections.computeIfAbsent(addDocumentFileAttachmentOperation.getEntity().getCollectionId(), id -> task.getDaoContextSnapshot().getCollectionDao().getCollectionById(id));
            sb.append("new record file attachment (")
                  .append(collection.getName())
                  .append(".")
                  .append(CollectionUtil.getAttribute(collection, addDocumentFileAttachmentOperation.getAttrId()))
                  .append(": ")
                  .append(addDocumentFileAttachmentOperation.getFileAttachmentData().getFileName())
                  .append(")\n");
         } else if (operation instanceof AddLinkFileAttachmentOperation) {
            final AddLinkFileAttachmentOperation addLinkFileAttachmentOperation = (AddLinkFileAttachmentOperation) operation;
            final LinkType linkType = linkTypes.computeIfAbsent(addLinkFileAttachmentOperation.getEntity().getLinkTypeId(), id -> task.getDaoContextSnapshot().getLinkTypeDao().getLinkType(id));
            sb.append("new link file attachment (")
              .append(linkType.getName())
              .append(".")
              .append(LinkTypeUtil.getAttribute(linkType, addLinkFileAttachmentOperation.getAttrId()))
              .append(": ")
              .append(addLinkFileAttachmentOperation.getFileAttachmentData().getFileName())
              .append(")\n");
         } else if (operation instanceof SendSmtpEmailOperation) {
            final SendSmtpEmailOperation sendSmtpEmailOperation = (SendSmtpEmailOperation) operation;
            sb.append("send email (to: ")
                  .append(sendSmtpEmailOperation.getEntity().getEmail())
                  .append(", subject: ")
                  .append(sendSmtpEmailOperation.getEntity().getSubject())
                  .append(")\n");
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

   private int getMaxCreatedRecords() {
      return serviceLimits.getMaxCreatedRecords();
   }

   private int getMaxFileAttachmentSizeMb() {
      return serviceLimits.getFileSizeMb();
   }

   private int getMaxFileAttachmentSize() {
      return getMaxFileAttachmentSizeMb() * 1024 * 1024;
   }
}
