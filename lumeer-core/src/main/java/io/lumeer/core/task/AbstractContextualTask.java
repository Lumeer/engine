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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Sequence;
import io.lumeer.api.model.User;
import io.lumeer.api.model.common.WithId;
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

   private static Logger log = Logger.getLogger(AbstractConstraintConverter.class.getName());

   protected User initiator;
   protected DaoContextSnapshot daoContextSnapshot;
   protected PusherClient pusherClient;
   protected Task parent;
   protected RequestDataKeeper requestDataKeeper;
   protected ConstraintManager constraintManager;
   protected DefaultConfigurationProducer.DeployEnvironment environment;

   @Override
   public ContextualTask initialize(final User initiator, final DaoContextSnapshot daoContextSnapshot, final PusherClient pusherClient, final RequestDataKeeper requestDataKeeper, final ConstraintManager constraintManager, DefaultConfigurationProducer.DeployEnvironment environment) {
      this.initiator = initiator;
      this.daoContextSnapshot = daoContextSnapshot;
      this.pusherClient = pusherClient;
      this.requestDataKeeper = requestDataKeeper;
      this.constraintManager = constraintManager;
      this.environment = environment;

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
   public ConstraintManager getConstraintManager() {
      return constraintManager;
   }

   @Override
   public User getInitiator() {
      return initiator;
   }

   @Override
   public Task getParent() {
      return parent;
   }

   @Override
   public void setParent(final Task parent) {
      this.parent = parent;
   }

   public void sendPushNotifications(final Collection collection) {
      if (getPusherClient() != null) {
         final Set<String> users = getDaoContextSnapshot().getCollectionManagers(collection.getId());
         final List<Event> events = users.stream().map(user -> createEventForCollection(collection, user)).collect(Collectors.toList());

         getPusherClient().trigger(events);
      }
   }

   public void sendPushNotifications(final Collection collection, final String suffix) {
      if (getPusherClient() != null) {
         final Set<String> users = getDaoContextSnapshot().getCollectionManagers(collection.getId());
         final List<Event> events = users.stream().map(user -> createEventForCollection(collection, user, suffix)).collect(Collectors.toList());

         getPusherClient().trigger(events);
      }
   }

   public void sendPushNotifications(final LinkType linkType) {
      if (getPusherClient() != null) {
         linkType.setLinksCount(getDaoContextSnapshot().getLinkInstanceDao().getLinkInstancesCountByLinkType(linkType.getId()));
         final Set<String> users1 = getDaoContextSnapshot().getCollectionReaders(linkType.getCollectionIds().get(0));
         final Set<String> users2 = getDaoContextSnapshot().getCollectionReaders(linkType.getCollectionIds().get(1));
         final Set<String> users = users1.stream().filter(users2::contains).collect(Collectors.toSet());
         final List<Event> events = users.stream().map(user -> createEventForLinkType(linkType, user)).collect(Collectors.toList());

         getPusherClient().trigger(events);
      }
   }

   public void sendPushNotifications(final LinkType linkType, final String suffix) {
      if (getPusherClient() != null) {
         linkType.setLinksCount(getDaoContextSnapshot().getLinkInstanceDao().getLinkInstancesCountByLinkType(linkType.getId()));
         final Set<String> users1 = getDaoContextSnapshot().getCollectionReaders(linkType.getCollectionIds().get(0));
         final Set<String> users2 = getDaoContextSnapshot().getCollectionReaders(linkType.getCollectionIds().get(1));
         final Set<String> users = users1.stream().filter(users2::contains).collect(Collectors.toSet());
         final List<Event> events = users.stream().map(user -> createEventForLinkType(linkType, user, suffix)).collect(Collectors.toList());

         getPusherClient().trigger(events);
      }
   }

   private Event createEventForCollection(final Collection collection, final String userId) {
      return createEventForCollection(collection, userId, PusherFacade.UPDATE_EVENT_SUFFIX);
   }

   private Event createEventForCollection(final Collection collection, final String userId, final String suffix) {
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(collection, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      injectCorrelationId(message);
      return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, Collection.class.getSimpleName() + suffix, message, getResourceId(collection, null), null);
   }

   private Event createEventForDocument(final Document document, final String userId, final String suffix) {
      document.setCommentsCount(daoContextSnapshot.getResourceCommentDao().getCommentsCount(ResourceType.DOCUMENT, document.getId()));
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(document, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      injectCorrelationId(message);
      return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, Document.class.getSimpleName() + suffix, message, getResourceId(document, document.getCollectionId()), null);
   }

   private Event createEventForLinkType(final LinkType linkType, final String userId) {
      return createEventForLinkType(linkType, userId, PusherFacade.UPDATE_EVENT_SUFFIX);
   }

   private Event createEventForLinkType(final LinkType linkType, final String userId, final String suffix) {
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(linkType, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      injectCorrelationId(message);
      return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, LinkType.class.getSimpleName() + suffix, message, getResourceId(linkType, null), null);
   }

   private Event createEventForLinkInstance(final LinkInstance linkInstance, final String userId, final String suffix) {
      linkInstance.setCommentsCount(daoContextSnapshot.getResourceCommentDao().getCommentsCount(ResourceType.LINK, linkInstance.getId()));

      if (PusherFacade.REMOVE_EVENT_SUFFIX.equals(suffix)) {
         final PusherFacade.ResourceId message = new PusherFacade.ResourceId(linkInstance.getId(), getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
         return new Event(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, LinkInstance.class.getSimpleName() + suffix, message);
      } else {
         final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(linkInstance, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
         injectCorrelationId(message);
         return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, LinkInstance.class.getSimpleName() + suffix, message, getResourceId(linkInstance, linkInstance.getLinkTypeId()), null);
      }
   }

   private Event createEventForSequence(final Sequence sequence, final String userId) {
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(sequence, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      injectCorrelationId(message);
      return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, Sequence.class.getSimpleName() + PusherFacade.UPDATE_EVENT_SUFFIX, message, getResourceId(sequence, null), null);
   }

   private Event createEventForUserMessage(final UserMessage userMessage, final String userId) {
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(userMessage, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      injectCorrelationId(message);
      return new BackupDataEvent(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, UserMessage.class.getSimpleName() + PusherFacade.CREATE_EVENT_SUFFIX, message, getResourceId(), null);
   }

   private Event createEventForPrintRequest(final PrintRequest printRequest, final String userId) {
      final PusherFacade.ObjectWithParent message = new PusherFacade.ObjectWithParent(printRequest, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId());
      injectCorrelationId(message);
      return new Event(PusherFacade.PRIVATE_CHANNEL_PREFIX + userId, PrintRequest.class.getSimpleName(), message, null);
   }

   private PusherFacade.ResourceId getResourceId(final WithId idObject, final String extraId) {
      return new PusherFacade.ResourceId(idObject.getId(), getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId(), extraId);
   }

   private PusherFacade.ResourceId getResourceId() {
      return new PusherFacade.ResourceId(null, getDaoContextSnapshot().getOrganizationId(), getDaoContextSnapshot().getProjectId(), null);
   }

   public void sendPushNotifications(final Collection collection, final List<Document> documents, final String suffix, final boolean collectionChanged) {
      final Set<String> users = getDaoContextSnapshot().getCollectionReaders(collection);
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
         linkType.setLinksCount(getDaoContextSnapshot().getLinkInstanceDao().getLinkInstancesCountByLinkType(linkType.getId()));
         final Set<String> users1 = getDaoContextSnapshot().getCollectionReaders(linkType.getCollectionIds().get(0));
         final Set<String> users2 = getDaoContextSnapshot().getCollectionReaders(linkType.getCollectionIds().get(1));
         final Set<String> users = users1.stream().filter(users2::contains).collect(Collectors.toSet());

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

      final Set<String> managers = getDaoContextSnapshot().getProjectManagers();

      final List<Event> events = new ArrayList<>();
      managers.forEach(manager -> {
         events.add(createEventForSequence(sequence, manager));
      });

      getPusherClient().trigger(events);
   }

   public void sendPushNotifications(final List<UserMessage> userMessages) {
      final List<Event> events = new ArrayList<>();

      userMessages.stream().filter(m -> StringUtils.isNotEmpty(m.getMessage())).forEach(m ->
         events.add(createEventForUserMessage(m, initiator.getId()))
      );

      getPusherClient().trigger(events);
   }

   public void sendPrintRequestPushNotifications(final List<PrintRequest> printRequests) {
      final List<Event> events = new ArrayList<>();

      printRequests.forEach(m ->
            events.add(createEventForPrintRequest(m, initiator.getId()))
      );

      getPusherClient().trigger(events);
   }

   private void sendPushNotificationsForDocuments(final ChangesTracker changesTracker) {
      // keep track of collections without updated documents
      final Set<String> collectionIds = new HashSet<>(changesTracker.getCollections().stream().map(Collection::getId).collect(Collectors.toSet()));
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
      final Set<String> linkTypeIds = new HashSet<>(changesTracker.getLinkTypes().stream().map(LinkType::getId).collect(Collectors.toSet()));
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
         obj.setCorrelationId(requestDataKeeper.getCorrelationId());
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
            getDaoContextSnapshot().getDelayedActionDao(), getDaoContextSnapshot().getUserDao(), getDaoContextSnapshot().getSelectedWorkspace(),
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

   class LocalContextualTaskFactory extends ContextualTaskFactory {

      public <T extends ContextualTask> T getInstance(final Class<T> clazz) {
         try {
            T t = clazz.getConstructor().newInstance();
            t.initialize(getInitiator(), getDaoContextSnapshot(), getPusherClient(), new RequestDataKeeper(requestDataKeeper), constraintManager, environment);

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
      private final DefaultConfigurationProducer.DeployEnvironment environment;

      public SyntheticContextualTaskFactory(final DefaultConfigurationProducer configurationProducer, final DaoContextSnapshot daoContextSnapshot) {
         this.contextSnapshot = daoContextSnapshot;
         constraintManager = ConstraintManager.getInstance(configurationProducer);
         pusherClient = PusherClient.getInstance(configurationProducer);
         initiator = AuthenticatedUser.getMachineUser();
         environment = configurationProducer.getEnvironment();
      }

      public <T extends ContextualTask> T getInstance(final Class<T> clazz) {
         try {
            T t = clazz.getConstructor().newInstance();
            t.initialize(initiator, contextSnapshot, pusherClient, new RequestDataKeeper(), constraintManager, environment);

            return t;
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to instantiate a task: ", e);
         }

         return null;
      }

   }
}
