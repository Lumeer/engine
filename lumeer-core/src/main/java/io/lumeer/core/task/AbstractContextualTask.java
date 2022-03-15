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
package io.lumeer.core.task;

import io.lumeer.api.model.AppId;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Group;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.Sequence;
import io.lumeer.api.model.User;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.WithId;
import io.lumeer.core.adapter.CollectionAdapter;
import io.lumeer.core.adapter.DocumentAdapter;
import io.lumeer.core.adapter.FacadeAdapter;
import io.lumeer.core.adapter.FileAttachmentAdapter;
import io.lumeer.core.adapter.LinkInstanceAdapter;
import io.lumeer.core.adapter.LinkTypeAdapter;
import io.lumeer.core.adapter.PermissionAdapter;
import io.lumeer.core.adapter.PusherAdapter;
import io.lumeer.core.adapter.ResourceAdapter;
import io.lumeer.core.adapter.ViewAdapter;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.constraint.AbstractConstraintConverter;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.FunctionFacade;
import io.lumeer.core.facade.PusherFacade;
import io.lumeer.core.facade.TaskProcessingFacade;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.facade.detector.PurposeChangeProcessor;
import io.lumeer.core.task.executor.ChangesTracker;
import io.lumeer.core.task.executor.request.GenericPrintRequest;
import io.lumeer.core.task.executor.request.NavigationRequest;
import io.lumeer.core.task.executor.request.SendEmailRequest;
import io.lumeer.core.task.executor.request.UserMessageRequest;
import io.lumeer.core.util.LumeerS3Client;
import io.lumeer.core.util.PusherClient;
import io.lumeer.core.util.Utils;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;

import org.apache.commons.lang3.StringUtils;
import org.marvec.pusher.data.BackupDataEvent;
import org.marvec.pusher.data.Event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class AbstractContextualTask implements ContextualTask {

   private static final int RELOAD_EVENT_THRESHOLD = 50;

   private static final Logger log = Logger.getLogger(AbstractConstraintConverter.class.getName());

   private int recursionDepth = 0;

   protected User initiator;
   protected DaoContextSnapshot daoContextSnapshot;
   protected PusherClient pusherClient;
   protected LumeerS3Client lumeerS3Client;
   protected Task parent;
   protected RequestDataKeeper requestDataKeeper;
   protected ConstraintManager constraintManager;
   protected DefaultConfigurationProducer.DeployEnvironment environment;
   protected String timeZone;

   protected DocumentAdapter documentAdapter;
   protected CollectionAdapter collectionAdapter;
   protected ResourceAdapter resourceAdapter;
   protected ViewAdapter viewAdapter;
   protected LinkTypeAdapter linkTypeAdapter;
   protected LinkInstanceAdapter linkInstanceAdapter;
   protected PermissionAdapter permissionAdapter;
   protected PusherAdapter pusherAdapter;
   protected FileAttachmentAdapter fileAttachmentAdapter;

   @Override
   public ContextualTask initialize(final User initiator, final DaoContextSnapshot daoContextSnapshot, final PusherClient pusherClient, final LumeerS3Client lumeerS3Client, final RequestDataKeeper requestDataKeeper, final ConstraintManager constraintManager, DefaultConfigurationProducer.DeployEnvironment environment, final int recursionDepth) {
      this.initiator = initiator;
      this.daoContextSnapshot = daoContextSnapshot;
      this.pusherClient = pusherClient;
      this.lumeerS3Client = lumeerS3Client;
      this.requestDataKeeper = requestDataKeeper;
      this.constraintManager = constraintManager;
      this.environment = environment;
      this.timeZone = requestDataKeeper.getTimezone();
      this.recursionDepth = recursionDepth;

      collectionAdapter = new CollectionAdapter(daoContextSnapshot.getCollectionDao(), daoContextSnapshot.getFavoriteItemDao(), daoContextSnapshot.getDocumentDao());
      permissionAdapter = new PermissionAdapter(daoContextSnapshot.getUserDao(), daoContextSnapshot.getGroupDao(), daoContextSnapshot.getViewDao(), daoContextSnapshot.getLinkTypeDao(), daoContextSnapshot.getCollectionDao());
      resourceAdapter = new ResourceAdapter(permissionAdapter, daoContextSnapshot.getCollectionDao(), daoContextSnapshot.getLinkTypeDao(), daoContextSnapshot.getViewDao(), daoContextSnapshot.getUserDao());
      viewAdapter = new ViewAdapter(resourceAdapter, daoContextSnapshot.getFavoriteItemDao());
      documentAdapter = new DocumentAdapter(daoContextSnapshot.getResourceCommentDao(), daoContextSnapshot.getFavoriteItemDao());
      linkTypeAdapter = new LinkTypeAdapter(daoContextSnapshot.getLinkTypeDao(), daoContextSnapshot.getLinkInstanceDao());
      linkInstanceAdapter = new LinkInstanceAdapter(daoContextSnapshot.getResourceCommentDao());
      pusherAdapter = new PusherAdapter(getAppId(), new FacadeAdapter(permissionAdapter), resourceAdapter, permissionAdapter, daoContextSnapshot.getViewDao(), daoContextSnapshot.getLinkTypeDao(), daoContextSnapshot.getCollectionDao());
      fileAttachmentAdapter = new FileAttachmentAdapter(getLumeerS3Client(), daoContextSnapshot.getFileAttachmentDao(), environment.name());

      return this;
   }

   @Override
   public DaoContextSnapshot getDaoContextSnapshot() {
      return daoContextSnapshot;
   }

   @Override
   public PusherClient getPusherClient() {
      return pusherClient;
   }

   @Override
   public LumeerS3Client getLumeerS3Client() {
      return lumeerS3Client;
   }

   @Override
   public ConstraintManager getConstraintManager() {
      return constraintManager;
   }

   @Override
   public User getInitiator() {
      return initiator;
   }

   @Override
   public List<Group> getGroups() {
      return permissionAdapter.getGroups(daoContextSnapshot.getOrganizationId());
   }

   @Override
   public Task getParent() {
      return parent;
   }

   @Override
   public void setParent(final Task parent) {
      this.parent = parent;
   }

   @Override
   public String getTimeZone() {
      return timeZone;
   }

   @Override
   public FileAttachmentAdapter getFileAttachmentAdapter() {
      return fileAttachmentAdapter;
   }

   private Set<String> getLinkTypeReaders(final LinkType linkType) {
      return resourceAdapter.getLinkTypeReaders(getDaoContextSnapshot().getOrganization(), getDaoContextSnapshot().getProject(), linkType);
   }

   private Set<String> getViewReaders(final View view) {
      return resourceAdapter.getViewReaders(getDaoContextSnapshot().getOrganization(), getDaoContextSnapshot().getProject(), view);
   }

   private Set<String> getCollectionReaders(final Collection collection) {
      return resourceAdapter.getCollectionReaders(getDaoContextSnapshot().getOrganization(), getDaoContextSnapshot().getProject(), collection);
   }

   public void sendPushNotifications(final Collection collection) {
      sendPushNotifications(collection, PusherFacade.UPDATE_EVENT_SUFFIX);
   }

   public void sendPushNotifications(final Collection collection, final String suffix) {
      if (getPusherClient() != null) {
         final Set<String> users = getCollectionReaders(collection);
         final List<Event> events = users.stream().map(user -> createEventForCollection(collection, user, suffix)).collect(Collectors.toList());

         getPusherClient().trigger(events);
      }
   }

   public void sendPushNotifications(final View originalView, final View view, final String suffix) {
      if (getPusherClient() != null) {
         final Set<String> users = getViewReaders(view);
         final List<Event> events = users.stream().map(user ->
               createEventForView(view, user, suffix)
         ).collect(Collectors.toList());

         users.forEach(userId -> {
            final User user = permissionAdapter.getUser(userId);

            events.addAll(
                  pusherAdapter.checkViewPermissionsChange(
                        daoContextSnapshot.getOrganization(),
                        getDaoContextSnapshot().getProject(),
                        user,
                        originalView,
                        view
                  )
            );
         });

         getPusherClient().trigger(events);
      }
   }

   public void sendPushNotifications(final LinkType linkType) {
      sendPushNotifications(linkType, PusherFacade.UPDATE_EVENT_SUFFIX);
   }

   public void sendPushNotifications(final LinkType linkType, final String suffix) {
      if (getPusherClient() != null) {
         linkTypeAdapter.mapLinkTypeComputedProperties(linkType);
         final Set<String> users = getLinkTypeReaders(linkType);
         final List<Event> events = users.stream().map(user -> createEventForLinkType(linkType, user, suffix)).collect(Collectors.toList());

         getPusherClient().trigger(events);
      }
   }

   private Event createEventForCollection(final Collection collection, final String userId) {
      return createEventForCollection(collection, userId, PusherFacade.UPDATE_EVENT_SUFFIX);
   }

   private Event createEventForCollection(final Collection collection, final String userId, final String suffix) {
      final String projectId = daoContextSnapshot.getSelectedWorkspace().getProject().map(Project::getId).orElse("");

      final Collection mappedCollection = collectionAdapter.mapCollectionComputedProperties(collection.copy(), userId, projectId);

      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(getAppId(), mappedCollection, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      injectCorrelationId(message);
      return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, Collection.class.getSimpleName() + suffix, message, getResourceId(mappedCollection, null), null);
   }

   private Event createEventForView(final View view, final String userId, final String suffix) {
      final String projectId = daoContextSnapshot.getSelectedWorkspace().getProject().map(Project::getId).orElse("");

      final View mappedView = viewAdapter.mapViewData(getDaoContextSnapshot().getOrganization(), getDaoContextSnapshot().getProject(), view.copy(), userId, projectId);

      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(getAppId(), mappedView, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      injectCorrelationId(message);
      return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, View.class.getSimpleName() + suffix, message, getResourceId(mappedView, null), null);
   }

   private Event createEventForDocument(final Document document, final String userId, final String suffix) {
      final String projectId = daoContextSnapshot.getSelectedWorkspace().getProject().map(Project::getId).orElse("");

      final Document mappedDocument = documentAdapter.mapDocumentData(new Document(document), userId, projectId);

      if (PusherFacade.REMOVE_EVENT_SUFFIX.equals(suffix)) {
         return createEventForRemove(Document.class.getSimpleName(), getResourceId(mappedDocument, mappedDocument.getCollectionId()), userId);
      }

      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(getAppId(), mappedDocument, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      injectCorrelationId(message);
      return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, Document.class.getSimpleName() + suffix, message, getResourceId(mappedDocument, mappedDocument.getCollectionId()), null);
   }

   private Event createEventForLinkType(final LinkType linkType, final String userId) {
      return createEventForLinkType(linkType, userId, PusherFacade.UPDATE_EVENT_SUFFIX);
   }

   private Event createEventForLinkType(final LinkType linkType, final String userId, final String suffix) {
      linkTypeAdapter.mapLinkTypeComputedProperties(linkType);

      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(getAppId(), linkType, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      injectCorrelationId(message);
      return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, LinkType.class.getSimpleName() + suffix, message, getResourceId(linkType, null), null);
   }

   private Event createEventForLinkInstance(final LinkInstance linkInstance, final String userId, final String suffix) {
      linkInstanceAdapter.mapLinkInstanceData(linkInstance);

      if (PusherFacade.REMOVE_EVENT_SUFFIX.equals(suffix)) {
         return createEventForRemove(LinkInstance.class.getSimpleName(), getResourceId(linkInstance, linkInstance.getLinkTypeId()), userId);
      } else {
         final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(getAppId(), linkInstance, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
         injectCorrelationId(message);
         return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, LinkInstance.class.getSimpleName() + suffix, message, getResourceId(linkInstance, linkInstance.getLinkTypeId()), null);
      }
   }

   private Event createEventForSequence(final Sequence sequence, final String userId) {
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(getAppId(), sequence, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      injectCorrelationId(message);
      return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, Sequence.class.getSimpleName() + PusherFacade.UPDATE_EVENT_SUFFIX, message, getResourceId(sequence, null), null);
   }

   private Event createEventForUserMessage(final UserMessageRequest userMessageRequest, final String userId) {
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(getAppId(), userMessageRequest, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      injectCorrelationId(message);
      return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, UserMessageRequest.class.getSimpleName() + PusherFacade.CREATE_EVENT_SUFFIX, message, getResourceId(), null);
   }

   private Event createEventForPrintRequest(final GenericPrintRequest printRequest, final String userId) {
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(getAppId(), printRequest, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      injectCorrelationId(message);
      return new Event(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, printRequest.getClass().getSimpleName(), message, null);
   }

   private Event createEventForNavigationRequest(final NavigationRequest navigationRequest, final String userId) {
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(getAppId(), navigationRequest, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      injectCorrelationId(message);
      return new Event(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, NavigationRequest.class.getSimpleName(), message, null);
   }

   private Event createEventForSendEmailRequest(final SendEmailRequest sendEmailRequest, final String userId) {
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(getAppId(), sendEmailRequest, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      injectCorrelationId(message);
      return new Event(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, SendEmailRequest.class.getSimpleName(), message, null);
   }

   private PusherFacade.ResourceId getResourceId(final WithId idObject, final String extraId) {
      return new PusherFacade.ResourceId(getAppId(), idObject.getId(), getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId(), extraId);
   }

   private PusherFacade.ResourceId getResourceId() {
      return new PusherFacade.ResourceId(getAppId(), null, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId(), null);
   }

   private Event createEventForRemove(final String className, final PusherFacade.ResourceId object, final String userId) {
      return new Event(eventChannel(userId), className + PusherFacade.REMOVE_EVENT_SUFFIX, object, null);
   }

   public static String eventChannel(String userId) {
      return PusherFacade.PRIVATE_CHANNEL_PREFIX + userId;
   }

   public void sendPushNotifications(final Collection collection, final List<Document> documents, final String suffix, final boolean collectionChanged) {
      final Set<String> users = getCollectionReaders(collection);
      final List<Event> events = new ArrayList<>();
      final List<Event> collectionEvents = new ArrayList<>();

      users.forEach(userId -> {
         documents.forEach(doc -> events.add(createEventForDocument(doc, userId, suffix)));
         if (collectionChanged) {
            collectionEvents.add(createEventForCollection(collection, userId));
         }
      });

      getPusherClient().trigger(events);
      getPusherClient().trigger(collectionEvents);
   }

   public void sendPushNotifications(final LinkType linkType, final List<LinkInstance> linkInstances, final String suffix, final boolean linkTypeChanged) {
      if (linkType.getCollectionIds().size() == 2) {
         linkTypeAdapter.mapLinkTypeComputedProperties(linkType);
         final Set<String> users = getLinkTypeReaders(linkType);

         final List<Event> events = new ArrayList<>();
         final List<Event> linkInstanceEvents = new ArrayList<>();

         users.forEach(userId -> {
            linkInstances.forEach(link -> events.add(createEventForLinkInstance(link, userId, suffix)));
            if (linkTypeChanged) {
               linkInstanceEvents.add(createEventForLinkType(linkType, userId));
            }
         });

         getPusherClient().trigger(events);
         getPusherClient().trigger(linkInstanceEvents);
      }
   }

   public void sendPushNotifications(final String sequenceName) {
      final Sequence sequence = getDaoContextSnapshot().getSequenceDao().getSequence(sequenceName);

      final Set<String> techManagers = permissionAdapter.getProjectUsersByRole(daoContextSnapshot.getOrganization(), daoContextSnapshot.getProject(), RoleType.TechConfig);

      final List<Event> events = new ArrayList<>();
      techManagers.forEach(manager -> events.add(createEventForSequence(sequence, manager)));

      getPusherClient().trigger(events);
   }

   public void sendPushNotifications(final List<UserMessageRequest> userMessageRequests) {
      final List<Event> events = new ArrayList<>();

      userMessageRequests.stream().filter(m -> StringUtils.isNotEmpty(m.getMessage())).forEach(m ->
            events.add(createEventForUserMessage(m, initiator.getId()))
      );

      getPusherClient().trigger(events);
   }

   public void sendPrintRequestPushNotifications(final List<GenericPrintRequest> printRequests) {
      final List<Event> events = new ArrayList<>();

      printRequests.forEach(m ->
            events.add(createEventForPrintRequest(m, initiator.getId()))
      );

      getPusherClient().trigger(events);
   }

   public void sendNavigationRequestPushNotifications(final List<NavigationRequest> navigationRequests) {
      final List<Event> events = new ArrayList<>();

      navigationRequests.forEach(m ->
            events.add(createEventForNavigationRequest(m, initiator.getId()))
      );

      getPusherClient().trigger(events);
   }

   public void sendSendEmailRequestPushNotifications(final List<SendEmailRequest> sendEmailRequests) {
      final List<Event> events = new ArrayList<>();

      sendEmailRequests.forEach(m ->
            events.add(createEventForSendEmailRequest(m, initiator.getId()))
      );

      getPusherClient().trigger(events);
   }

   private void sendPushNotificationsForDocuments(final ChangesTracker changesTracker) {
      // keep track of collections without updated documents
      final Set<String> collectionIds = changesTracker.getCollections().stream().map(Collection::getId).collect(Collectors.toSet());
      final Set<String> updatedIds = new HashSet<>();

      if (changesTracker.getCreatedDocuments().size() > 0) {
         final Map<String, List<Document>> documentsByCollectionId =
               Utils.categorize(changesTracker.getCreatedDocuments().stream(), Document::getCollectionId);

         documentsByCollectionId.forEach((collectionId, documents) -> {
            if (changesTracker.getCollectionsMap().containsKey(collectionId)) {
               if (documents.size() > RELOAD_EVENT_THRESHOLD) {
                  sendPushNotifications(changesTracker.getCollectionsMap().get(collectionId), PusherFacade.RELOAD_EVENT_SUFFIX);
               } else {
                  sendPushNotifications(changesTracker.getCollectionsMap().get(collectionId), documents, PusherFacade.CREATE_EVENT_SUFFIX, collectionIds.contains(collectionId));
               }
               updatedIds.add(collectionId);
            }
         });
      }
      collectionIds.removeAll(updatedIds);

      if (changesTracker.getUpdatedDocuments().size() > 0) {
         final Map<String, List<Document>> documentsByCollectionId =
               Utils.categorize(changesTracker.getUpdatedDocuments().stream(), Document::getCollectionId);

         documentsByCollectionId.forEach((collectionId, documents) -> {
            if (changesTracker.getCollectionsMap().containsKey(collectionId)) {
               if (documents.size() > RELOAD_EVENT_THRESHOLD) {
                  sendPushNotifications(changesTracker.getCollectionsMap().get(collectionId), PusherFacade.RELOAD_EVENT_SUFFIX);
               } else {
                  sendPushNotifications(changesTracker.getCollectionsMap().get(collectionId), documents, PusherFacade.UPDATE_EVENT_SUFFIX, collectionIds.contains(collectionId));
               }
               updatedIds.add(collectionId);
            }
         });
      }
      collectionIds.removeAll(updatedIds);

      if (changesTracker.getRemovedDocuments().size() > 0) {
         final Map<String, List<Document>> documentsByCollectionId =
               Utils.categorize(changesTracker.getRemovedDocuments().stream(), Document::getCollectionId);

         documentsByCollectionId.forEach((collectionId, documents) -> {
            if (changesTracker.getCollectionsMap().containsKey(collectionId)) {
               if (documents.size() > RELOAD_EVENT_THRESHOLD) {
                  sendPushNotifications(changesTracker.getCollectionsMap().get(collectionId), PusherFacade.RELOAD_EVENT_SUFFIX);
               } else {
                  sendPushNotifications(changesTracker.getCollectionsMap().get(collectionId), documents, PusherFacade.REMOVE_EVENT_SUFFIX, collectionIds.contains(collectionId));
               }
               updatedIds.add(collectionId);
            }
         });
      }
      collectionIds.removeAll(updatedIds);

      if (collectionIds.size() > 0) {
         collectionIds.forEach(id -> {
            if (changesTracker.getCollectionsMap().containsKey(id)) {
               sendPushNotifications(changesTracker.getCollectionsMap().get(id));
            }
         });
      }
   }

   private void sendPushNotificationsForLinks(final ChangesTracker changesTracker) {
      // keep track of collections without updated documents
      final Set<String> linkTypeIds = changesTracker.getLinkTypes().stream().map(LinkType::getId).collect(Collectors.toSet());
      final Set<String> updatedIds = changesTracker.getLinkTypes().stream().map(LinkType::getId).collect(Collectors.toSet());

      if (changesTracker.getCreatedLinkInstances().size() > 0) {
         final Map<String, List<LinkInstance>> linksByLinkTypeId =
               Utils.categorize(changesTracker.getCreatedLinkInstances().stream(), LinkInstance::getLinkTypeId);

         linksByLinkTypeId.forEach((linkTypeId, links) -> {
            if (changesTracker.getLinkTypesMap().containsKey(linkTypeId)) {
               if (links.size() > RELOAD_EVENT_THRESHOLD) {
                  sendPushNotifications(changesTracker.getLinkTypesMap().get(linkTypeId), PusherFacade.RELOAD_EVENT_SUFFIX);
               } else {
                  sendPushNotifications(changesTracker.getLinkTypesMap().get(linkTypeId), links, PusherFacade.CREATE_EVENT_SUFFIX, linkTypeIds.contains(linkTypeId));
               }
               updatedIds.add(linkTypeId);
            }
         });
      }
      linkTypeIds.removeAll(updatedIds);

      if (changesTracker.getUpdatedLinkInstances().size() > 0) {
         final Map<String, List<LinkInstance>> linksByLinkTypeId =
               Utils.categorize(changesTracker.getUpdatedLinkInstances().stream(), LinkInstance::getLinkTypeId);

         linksByLinkTypeId.forEach((linkTypeId, links) -> {
            if (changesTracker.getLinkTypesMap().containsKey(linkTypeId)) {
               if (links.size() > RELOAD_EVENT_THRESHOLD) {
                  sendPushNotifications(changesTracker.getLinkTypesMap().get(linkTypeId), PusherFacade.RELOAD_EVENT_SUFFIX);
               } else {
                  sendPushNotifications(changesTracker.getLinkTypesMap().get(linkTypeId), links, PusherFacade.UPDATE_EVENT_SUFFIX, linkTypeIds.contains(linkTypeId));
               }
               updatedIds.add(linkTypeId);
            }
         });
      }
      linkTypeIds.removeAll(updatedIds);

      if (changesTracker.getRemovedLinkInstances().size() > 0) {
         final Map<String, List<LinkInstance>> linksByLinkTypeId =
               Utils.categorize(changesTracker.getRemovedLinkInstances().stream(), LinkInstance::getLinkTypeId);

         linksByLinkTypeId.forEach((linkTypeId, links) -> {
            if (changesTracker.getLinkTypesMap().containsKey(linkTypeId)) {
               if (links.size() > RELOAD_EVENT_THRESHOLD) {
                  sendPushNotifications(changesTracker.getLinkTypesMap().get(linkTypeId), PusherFacade.RELOAD_EVENT_SUFFIX);
               } else {
                  sendPushNotifications(changesTracker.getLinkTypesMap().get(linkTypeId), links, PusherFacade.REMOVE_EVENT_SUFFIX, linkTypeIds.contains(linkTypeId));
               }
               updatedIds.add(linkTypeId);
            }
         });
      }
      linkTypeIds.removeAll(updatedIds);

      if (linkTypeIds.size() > 0) {
         linkTypeIds.forEach(id -> {
            if (changesTracker.getLinkTypesMap().containsKey(id)) {
               sendPushNotifications(changesTracker.getLinkTypesMap().get(id));
            }
         });
      }
   }

   @Override
   public void processChanges(final ChangesTracker changesTracker) {
      if (getPusherClient() != null) {
         sendPushNotificationsForDocuments(changesTracker);
         sendPushNotificationsForLinks(changesTracker);

         if (changesTracker.getSequences().size() > 0) {
            changesTracker.getSequences().forEach(this::sendPushNotifications);
         }

         if (changesTracker.getUserMessages().size() > 0) {
            sendPushNotifications(changesTracker.getUserMessages());
         }

         if (changesTracker.getPrintRequests().size() > 0) {
            sendPrintRequestPushNotifications(changesTracker.getPrintRequests());
         }

         if (changesTracker.getNavigationRequests().size() > 0) {
            sendNavigationRequestPushNotifications(changesTracker.getNavigationRequests());
         }

         if (changesTracker.getSendEmailRequests().size() > 0) {
            sendSendEmailRequestPushNotifications(changesTracker.getSendEmailRequests());
         }

         if (changesTracker.getUpdatedViews().size() > 0) {
            changesTracker.getUpdatedViews().forEach(viewTuple -> sendPushNotifications(viewTuple.getFirst(), viewTuple.getSecond(), PusherFacade.UPDATE_EVENT_SUFFIX));
         }
      }
   }

   @Override
   public void propagateChanges(final List<Document> documents, final List<LinkInstance> links) {
      if (parent != null) {
         parent.propagateChanges(documents, links);
      }
   }

   private void injectCorrelationId(final PusherFacade.ObjectWithParent obj) {
      if (requestDataKeeper != null) {
         obj.setCorrelationId(getCorrelationId());
      }
   }

   @Override
   public FunctionFacade getFunctionFacade() {
      return FunctionFacade.getInstance(
            getDaoContextSnapshot().getFunctionDao(),
            getDaoContextSnapshot().getCollectionDao(),
            getDaoContextSnapshot().getDocumentDao(),
            getDaoContextSnapshot().getLinkInstanceDao(),
            getDaoContextSnapshot().getLinkTypeDao(),
            new LocalContextualTaskFactory()
      );
   }

   @Override
   public TaskProcessingFacade getTaskProcessingFacade(final TaskExecutor taskExecutor, final FunctionFacade functionFacade) {
      return TaskProcessingFacade.getInstance(
            taskExecutor,
            new LocalContextualTaskFactory(),
            getDaoContextSnapshot().getCollectionDao(),
            getDaoContextSnapshot().getLinkTypeDao(),
            functionFacade
      );
   }

   @Override
   public PurposeChangeProcessor getPurposeChangeProcessor() {
      return new PurposeChangeProcessor(
            getDaoContextSnapshot().getDelayedActionDao(), getDaoContextSnapshot().getUserDao(), getDaoContextSnapshot().getGroupDao(), getDaoContextSnapshot().getSelectedWorkspace(),
            initiator, new RequestDataKeeper(requestDataKeeper), constraintManager, environment);
   }

   @Override
   public String getCurrentLocale() {
      if (StringUtils.isEmpty(requestDataKeeper.getUserLocale())) {
         return initiator.getNotificationsLanguage();
      }

      return requestDataKeeper.getUserLocale();
   }

   @Override
   public String getCorrelationId() {
      return requestDataKeeper.getCorrelationId();
   }

   @Override
   public AppId getAppId() {
      return requestDataKeeper.getAppId();
   }

   public int getRecursionDepth() {
      return recursionDepth;
   }

   class LocalContextualTaskFactory extends ContextualTaskFactory {

      public <T extends ContextualTask> T getInstance(final Class<T> clazz) {
         try {
            T t = clazz.getConstructor().newInstance();
            t.initialize(getInitiator(), getDaoContextSnapshot(), getPusherClient(), getLumeerS3Client(), new RequestDataKeeper(requestDataKeeper), constraintManager, environment, recursionDepth + 1);

            return t;
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to instantiate a task: ", e);
         }

         return null;
      }
   }

   public static class SyntheticContextualTaskFactory extends ContextualTaskFactory {
      private final ConstraintManager constraintManager;
      private final User initiator;
      private final DaoContextSnapshot contextSnapshot;
      private final PusherClient pusherClient;
      private final LumeerS3Client lumeerS3Client;
      private final DefaultConfigurationProducer.DeployEnvironment environment;

      public SyntheticContextualTaskFactory(final DefaultConfigurationProducer configurationProducer, final DaoContextSnapshot daoContextSnapshot) {
         this.contextSnapshot = daoContextSnapshot;
         constraintManager = ConstraintManager.getInstance(configurationProducer);
         pusherClient = PusherClient.getInstance(configurationProducer);
         lumeerS3Client = new LumeerS3Client(configurationProducer);
         initiator = AuthenticatedUser.getMachineUser();
         environment = configurationProducer.getEnvironment();
      }

      public <T extends ContextualTask> T getInstance(final Class<T> clazz) {
         try {
            T t = clazz.getConstructor().newInstance();
            t.initialize(initiator, contextSnapshot.shallowCopy(), pusherClient, lumeerS3Client, new RequestDataKeeper(), constraintManager, environment, 0);

            return t;
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to instantiate a task: ", e);
         }

         return null;
      }

   }
}
