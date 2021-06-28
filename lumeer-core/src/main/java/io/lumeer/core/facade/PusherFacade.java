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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Group;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceComment;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.RolesDifference;
import io.lumeer.api.model.Sequence;
import io.lumeer.api.model.User;
import io.lumeer.api.model.UserNotification;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.model.common.WithId;
import io.lumeer.core.action.DelayedActionProcessor;
import io.lumeer.core.adapter.CollectionAdapter;
import io.lumeer.core.adapter.DocumentAdapter;
import io.lumeer.core.adapter.LinkInstanceAdapter;
import io.lumeer.core.adapter.LinkTypeAdapter;
import io.lumeer.core.adapter.PermissionAdapter;
import io.lumeer.core.adapter.PusherAdapter;
import io.lumeer.core.adapter.ResourceAdapter;
import io.lumeer.core.adapter.ViewAdapter;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.DocumentUtils;
import io.lumeer.core.util.PusherClient;
import io.lumeer.engine.api.event.AddFavoriteItem;
import io.lumeer.engine.api.event.CreateDocument;
import io.lumeer.engine.api.event.CreateDocumentsAndLinks;
import io.lumeer.engine.api.event.CreateLinkInstance;
import io.lumeer.engine.api.event.CreateLinkType;
import io.lumeer.engine.api.event.CreateOrUpdateGroup;
import io.lumeer.engine.api.event.CreateOrUpdatePayment;
import io.lumeer.engine.api.event.CreateOrUpdateSequence;
import io.lumeer.engine.api.event.CreateOrUpdateUser;
import io.lumeer.engine.api.event.CreateOrUpdateUserNotification;
import io.lumeer.engine.api.event.CreateResource;
import io.lumeer.engine.api.event.CreateResourceComment;
import io.lumeer.engine.api.event.FavoriteItem;
import io.lumeer.engine.api.event.ImportResource;
import io.lumeer.engine.api.event.ReloadGroups;
import io.lumeer.engine.api.event.ReloadResourceContent;
import io.lumeer.engine.api.event.RemoveDocument;
import io.lumeer.engine.api.event.RemoveFavoriteItem;
import io.lumeer.engine.api.event.RemoveGroup;
import io.lumeer.engine.api.event.RemoveLinkInstance;
import io.lumeer.engine.api.event.RemoveLinkType;
import io.lumeer.engine.api.event.RemoveResource;
import io.lumeer.engine.api.event.RemoveResourceComment;
import io.lumeer.engine.api.event.RemoveSequence;
import io.lumeer.engine.api.event.RemoveUser;
import io.lumeer.engine.api.event.RemoveUserNotification;
import io.lumeer.engine.api.event.SetDocumentLinks;
import io.lumeer.engine.api.event.TemplateCreated;
import io.lumeer.engine.api.event.UpdateCompanyContact;
import io.lumeer.engine.api.event.UpdateDefaultViewConfig;
import io.lumeer.engine.api.event.UpdateDocument;
import io.lumeer.engine.api.event.UpdateLinkInstance;
import io.lumeer.engine.api.event.UpdateLinkType;
import io.lumeer.engine.api.event.UpdateResource;
import io.lumeer.engine.api.event.UpdateResourceComment;
import io.lumeer.engine.api.event.UpdateServiceLimits;
import io.lumeer.engine.api.event.UserEvent;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ResourceCommentDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.ViewDao;

import org.marvec.pusher.data.Event;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class PusherFacade extends AbstractFacade {

   public static final String PRIVATE_CHANNEL_PREFIX = "private-";
   public static final String UPDATE_EVENT_SUFFIX = ":update";
   public static final String CREATE_EVENT_SUFFIX = ":create";
   public static final String REMOVE_EVENT_SUFFIX = ":remove";
   public static final String IMPORT_EVENT_SUFFIX = ":import";
   public static final String RELOAD_EVENT_SUFFIX = ":reload";

   private PusherClient pusherClient = null;

   @Inject
   private DelayedActionProcessor delayedActionProcessor;

   @Inject
   private Logger log;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private FavoriteItemDao favoriteItemDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private ViewDao viewDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private UserDao userDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private DataDao dataDao;

   @Inject
   private ResourceCommentDao resourceCommentDao;

   @Inject
   private RequestDataKeeper requestDataKeeper;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   private ConstraintManager constraintManager;
   private CollectionAdapter collectionAdapter;
   private LinkTypeAdapter linkTypeAdapter;
   private ViewAdapter viewAdapter;
   private ResourceAdapter resourceAdapter;
   private DocumentAdapter documentAdapter;
   private PusherAdapter pusherAdapter;
   private LinkInstanceAdapter linkInstanceAdapter;
   private PermissionAdapter permissionAdapter;

   @PostConstruct
   public void init() {
      constraintManager = ConstraintManager.getInstance(configurationProducer);
      pusherClient = PusherClient.getInstance(configurationProducer);
      delayedActionProcessor.setPusherClient(pusherClient);
      permissionAdapter = permissionsChecker.getPermissionAdapter();
      collectionAdapter = new CollectionAdapter(collectionDao, favoriteItemDao, documentDao);
      resourceAdapter = new ResourceAdapter(permissionAdapter, collectionDao, linkTypeDao, viewDao, userDao);
      linkTypeAdapter = new LinkTypeAdapter(linkInstanceDao);
      viewAdapter = new ViewAdapter(resourceAdapter, favoriteItemDao);

      documentAdapter = new DocumentAdapter(resourceCommentDao, favoriteItemDao);
      linkInstanceAdapter = new LinkInstanceAdapter(resourceCommentDao);
      pusherAdapter = new PusherAdapter(getFacadeAdapter(), resourceAdapter, permissionAdapter, viewDao, linkTypeDao, collectionDao);
   }

   public String getPusherKey() {
      return pusherClient != null ? pusherClient.getKey() : null;
   }

   public String getPusherSecret() {
      return pusherClient != null ? pusherClient.getSecret() : null;
   }

   public PusherClient getPusherClient() {
      return pusherClient;
   }

   public void createResource(@Observes final CreateResource createResource) {
      if (isEnabled()) {
         try {
            checkPermissionsChange(createResource);
            processResource(createResource.getResource(), CREATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void updateResource(@Observes final UpdateResource updateResource) {
      if (isEnabled()) {
         try {
            checkPermissionsChange(updateResource);
            processResource(updateResource.getResource(), UPDATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void removeResource(@Observes final RemoveResource removeResource) {
      if (isEnabled()) {
         try {
            processResource(removeResource.getResource(), REMOVE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void importResource(@Observes final ImportResource importResource) {
      if (isEnabled()) {
         try {
            processWithId(importResource.getResource(), IMPORT_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void reloadResource(@Observes final ReloadResourceContent reloadResourceContent) {
      if (isEnabled()) {
         try {
            processWithId(reloadResourceContent.getResource(), RELOAD_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void templateCreated(@Observes final TemplateCreated templateCreated) {
      if (isEnabled()) {
         Set<String> userIds = resourceAdapter.getProjectReaders(getOrganization(), templateCreated.getProject());
         sendNotificationsByUsers(new ObjectWithParent(templateCreated, getOrganization().getId(), templateCreated.getProject().getId()), userIds, CREATE_EVENT_SUFFIX);
      }
   }

   /*
    * There is possibility to create resource with permissions inside via API.
    */
   private void checkPermissionsChange(final CreateResource createResource) {
      var originalResource = createResource.getResource().copy();
      originalResource.setPermissions(new Permissions());

      if (createResource.getResource() instanceof Organization) {
         checkOrganizationPermissionsChange((Organization) originalResource, (Organization) createResource.getResource());
      } else if (createResource.getResource() instanceof Project) {
         checkProjectPermissionsChange((Project)originalResource, (Project) createResource.getResource());
      } else if (createResource.getResource() instanceof Collection) {
         checkCollectionsPermissionsChange((Collection) originalResource, (Collection) createResource.getResource());
      } else if (createResource.getResource() instanceof View) {
         checkViewPermissionsChange((View) originalResource, (View) createResource.getResource());
      }
   }

   private void checkPermissionsChange(final UpdateResource updateResource) {
      if (updateResource.getResource() instanceof Organization) {
         checkOrganizationPermissionsChange((Organization) updateResource.getOriginalResource(), (Organization) updateResource.getResource());
      } else if (updateResource.getResource() instanceof Project) {
         checkProjectPermissionsChange((Project) updateResource.getOriginalResource(), (Project) updateResource.getResource());
      } else if (updateResource.getResource() instanceof Collection) {
         checkCollectionsPermissionsChange((Collection) updateResource.getOriginalResource(), (Collection) updateResource.getResource());
      } else if (updateResource.getResource() instanceof View) {
         checkViewPermissionsChange((View) updateResource.getOriginalResource(), (View) updateResource.getResource());
      }
   }

   private void checkOrganizationPermissionsChange(final Organization originalOrganization, final Organization updatedOrganization) {
      RolesDifference rolesDifference = permissionAdapter.getOrganizationReadersDifference(originalOrganization, updatedOrganization);
      Set<String> removedUsers = new HashSet<>(rolesDifference.getRemovedUsers());
      removedUsers.remove(getCurrentUserId());
      if (!removedUsers.isEmpty()) {
         sendNotificationsByUsers(updatedOrganization, removedUsers, REMOVE_EVENT_SUFFIX);
      }
   }

   private void checkProjectPermissionsChange(final Project originalProject, final Project updatedProject) {
      RolesDifference rolesDifference = permissionAdapter.getProjectReadersDifference(getOrganization(), originalProject, updatedProject);
      Set<String> removedUsers = new HashSet<>(rolesDifference.getRemovedUsers());
      removedUsers.remove(getCurrentUserId());
      if (!removedUsers.isEmpty()) {
         sendNotificationsByUsers(updatedProject, removedUsers, REMOVE_EVENT_SUFFIX);
      }
   }

   private void checkLinkTypesPermissionsChange(final LinkType originalLinkType, final LinkType updatedLinkType) {
      List<Event> notifications = pusherAdapter.checkLinkTypePermissionsChange(getOrganization(), getProject(), authenticatedUser.getCurrentUser(), originalLinkType, updatedLinkType);
      if (notifications.size() > 0) {
         sendNotificationsBatch(notifications);
      }
   }

   private void checkCollectionsPermissionsChange(final Collection originalCollection, final Collection updatedCollection) {
      List<Event> notifications = pusherAdapter.checkCollectionsPermissionsChange(getOrganization(), getProject(), authenticatedUser.getCurrentUser(), originalCollection, updatedCollection);
      if (notifications.size() > 0) {
         sendNotificationsBatch(notifications);
      }
   }

   private void checkViewPermissionsChange(final View originalView, final View updatedView) {
      List<Event> notifications = pusherAdapter.checkViewPermissionsChange(getOrganization(), getProject(), authenticatedUser.getCurrentUser(), originalView, updatedView);
      if (notifications.size() > 0) {
         sendNotificationsBatch(notifications);
      }

   }

   private void processResource(final Resource resource, final String event) {
      if (resource instanceof Organization) {
         sendOrganizationNotifications((Organization) resource, event);
      } else if (resource instanceof View) {
         sendViewNotifications((View) resource, event);
      } else if (resource instanceof Project) {
         sendProjectNotifications((Project) resource, event);
      } else if (resource instanceof Collection) {
         sendCollectionNotifications((Collection) resource, event);
      }
   }

   private void processWithId(final WithId resource, final String event) {
      if (resource instanceof Collection) {
         sendCollectionNotifications((Collection) resource, event);
      } else if (resource instanceof LinkType) {
         sendResourceNotificationByLinkType((LinkType) resource, event);
      }
   }

   private void sendOrganizationNotifications(final Organization organization, final String event) {
      sendNotificationsByUsers(organization, resourceAdapter.getOrganizationReaders(organization), event);
   }

   private void sendNotificationsByUsers(final Object object, final Set<String> userIds, final String event) {
      sendNotificationsBatch(userIds.stream()
                                    .map(userId -> createEvent(object, event, userId))
                                    .collect(Collectors.toList()));
   }

   private Event createEvent(final Object object, final String event, final String userId) {
      return pusherAdapter.createEvent(
            getOrganization(),
            getProject(),
            object,
            event,
            userId
      );
   }

   private Event createEventForRemove(final String className, final ResourceId object, final String userId) {
      return pusherAdapter.createEventForRemove(className, object, userId);
   }

   private Event createEventForObject(final Object object, final String event, final String userId) {
      return pusherAdapter.createEventForObject(object, event, userId);
   }

   private Event createEventForObjectWithParent(final ObjectWithParent objectWithParent, final String event, final String userId) {
      return pusherAdapter.createEventForObjectWithParent(objectWithParent, event, userId);
   }

   public static String eventChannel(String userId) {
      return PusherAdapter.eventChannel(userId);
   }

   private void sendProjectNotifications(final Project project, final String event) {
      Set<String> userIds = resourceAdapter.getProjectReaders(getOrganization(), project);
      sendNotificationsByUsers(new ObjectWithParent(project, getOrganization().getId()), userIds, event);
   }

   private void sendViewNotifications(final View view, final String event) {
      Set<String> userIds = resourceAdapter.getViewReaders(getOrganization(), getProject(), view);
      sendNotificationsBatch(userIds.stream()
                                    .map(userId -> createEvent(new ObjectWithParent(createViewForUser(view, userId), getOrganization().getId(), getProject().getId()), event, userId))
                                    .collect(Collectors.toList()));
   }

   private View createViewForUser(final View view, final String userId) {
      return viewAdapter.mapViewData(getOrganization(), getProject(), view.copy(), userId, workspaceKeeper.getProjectId());
   }

   private void sendCollectionNotifications(final Collection collection, final String event) {
      Set<String> userIds = resourceAdapter.getCollectionReaders(getOrganization(), getProject(), collection);
      sendNotificationsBatch(userIds.stream()
                                    .map(userId -> createEvent(new ObjectWithParent(createCollectionForUser(collection, userId), getOrganization().getId(), getProject().getId()), event, userId))
                                    .collect(Collectors.toList()));
   }

   private Collection createCollectionForUser(final Collection collection, final String userId) {
      return collectionAdapter.mapCollectionComputedProperties(collection.copy(), userId, workspaceKeeper.getProjectId());
   }

   public void createResourceComment(@Observes final CreateResourceComment commentEvent) {
      resourceCommentNotification(commentEvent.getResourceComment(), CREATE_EVENT_SUFFIX);
   }

   public void updateResourceComment(@Observes final UpdateResourceComment commentEvent) {
      resourceCommentNotification(commentEvent.getResourceComment(), UPDATE_EVENT_SUFFIX);
   }

   public void removeResourceComment(@Observes final RemoveResourceComment commentEvent) {
      resourceCommentNotification(commentEvent.getResourceComment(), REMOVE_EVENT_SUFFIX);
   }

   private void resourceCommentNotification(final ResourceComment comment, final String eventSuffix) {
      if (comment.getResourceType() == ResourceType.COLLECTION) {
         final Set<String> users = resourceAdapter.getCollectionReaders(getOrganization(), getProject(), comment.getResourceId());

         sendNotificationsByUsers(comment, users, eventSuffix);
      } else if (comment.getResourceType() == ResourceType.DOCUMENT) {
         final Document document = DocumentUtils.loadDocumentWithData(documentDao, dataDao, comment.getResourceId());
         final Collection collection = collectionDao.getCollectionById(document.getCollectionId());
         document.setData(constraintManager.decodeDataTypes(collection, document.getData()));
         Set<String> userIds = resourceAdapter.getDocumentReaders(getOrganization(), getProject(), collection, document);

         sendNotificationsByUsers(comment, userIds, eventSuffix);
      } else if (comment.getResourceType() == ResourceType.LINK) {
         final LinkInstance linkInstance = linkInstanceDao.getLinkInstance(comment.getResourceId());
         final LinkType linkType = linkTypeDao.getLinkType(linkInstance.getLinkTypeId());
         Set<String> users = resourceAdapter.getLinkTypeReaders(getOrganization(), getProject(), linkType);

         sendNotificationsByUsers(comment, users, eventSuffix);
      } else if (comment.getResourceType() == ResourceType.VIEW) {
         final Set<String> users = resourceAdapter.getViewReaders(getOrganization(), getProject(), comment.getResourceId());

         sendNotificationsByUsers(comment, users, eventSuffix);
      } else if (comment.getResourceType() == ResourceType.PROJECT) {
         final Set<String> users = resourceAdapter.getProjectReaders(getOrganization(), getProject());

         sendNotificationsByUsers(comment, users, eventSuffix);
      } else if (comment.getResourceType() == ResourceType.ORGANIZATION) {
         final Set<String> users = resourceAdapter.getOrganizationReaders(getOrganization());

         sendNotificationsByUsers(comment, users, eventSuffix);
      }
   }

   public void createDocument(@Observes final CreateDocument createDocument) {
      documentNotification(createDocument.getDocument(), CREATE_EVENT_SUFFIX);
   }

   public void updateDocument(@Observes final UpdateDocument updateDocument) {
      documentNotification(updateDocument.getDocument(), UPDATE_EVENT_SUFFIX);
   }

   public void removeDocument(@Observes final RemoveDocument removeDocument) {
      documentNotification(removeDocument.getDocument(), REMOVE_EVENT_SUFFIX);
   }

   private void documentNotification(final Document updatedDocument, final String eventSuffix) {
      if (isEnabled()) {
         try {
            final Document document = new Document(updatedDocument);
            final Collection collection = collectionDao.getCollectionById(document.getCollectionId());
            document.setData(constraintManager.decodeDataTypes(collection, document.getData()));
            Set<String> userIds = resourceAdapter.getDocumentReaders(getOrganization(), getProject(), collection, document);

            sendNotificationsBatch(userIds.stream()
                                          .map(userId -> createEvent(createDocumentForUser(document, userId), eventSuffix, userId))
                                          .collect(Collectors.toList()));
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   private Document createDocumentForUser(final Document document, final String userId) {
      return documentAdapter.mapDocumentData(document, userId, workspaceKeeper.getProjectId());
   }

   public void createLinkInstance(@Observes final CreateLinkInstance createLinkInstance) {
      if (isEnabled()) {
         try {
            sendNotificationByLinkType(createLinkInstance.getLinkInstance(), createLinkInstance.getLinkInstance().getLinkTypeId(), CREATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void updateLinkInstance(@Observes final UpdateLinkInstance updateLinkInstance) {
      if (isEnabled()) {
         try {
            sendNotificationByLinkType(updateLinkInstance.getLinkInstance(), updateLinkInstance.getLinkInstance().getLinkTypeId(), UPDATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void removeLinkInstance(@Observes final RemoveLinkInstance removeLinkInstance) {
      if (isEnabled()) {
         try {
            sendNotificationByLinkType(removeLinkInstance.getLinkInstance(), removeLinkInstance.getLinkInstance().getLinkTypeId(), REMOVE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void createLinkType(@Observes final CreateLinkType createLinkType) {
      if (isEnabled()) {
         try {
            var originalLinkType = new LinkType(createLinkType.getLinkType());
            originalLinkType.setPermissions(new Permissions());

            checkLinkTypesPermissionsChange(originalLinkType, createLinkType.getLinkType());
            sendResourceNotificationByLinkType(linkTypeAdapter.mapLinkTypeComputedProperties(createLinkType.getLinkType()), CREATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void updateLinkType(@Observes final UpdateLinkType updateLinkType) {
      if (isEnabled()) {
         try {
            checkLinkTypesPermissionsChange(updateLinkType.getOriginalLinkType(), updateLinkType.getLinkType());
            sendResourceNotificationByLinkType(linkTypeAdapter.mapLinkTypeComputedProperties(updateLinkType.getLinkType()), UPDATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void removeLinkType(@Observes final RemoveLinkType removeLinkType) {
      if (isEnabled()) {
         try {
            sendResourceNotificationByLinkType(removeLinkType.getLinkType(), REMOVE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void updateCompanyContact(@Observes final UpdateCompanyContact updateCompanyContact) {
      if (isEnabled()) {
         try {
            Organization organization = organizationDao.getOrganizationById(updateCompanyContact.getCompanyContact().getOrganizationId());
            Set<String> userIds =  permissionAdapter.getOrganizationUsersByRole(organization, RoleType.Manage);
            sendNotificationsByUsers(updateCompanyContact.getCompanyContact(), userIds, UPDATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void updateServiceLimits(@Observes final UpdateServiceLimits updateServiceLimits) {
      if (isEnabled()) {
         try {
            ObjectWithParent object = new ObjectWithParent(updateServiceLimits.getServiceLimits(), updateServiceLimits.getOrganization().getId());
            Set<String> userIds = permissionAdapter.getOrganizationUsersByRole(updateServiceLimits.getOrganization(), RoleType.Manage);
            sendNotificationsByUsers(object, userIds, UPDATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void createOrUpdatePayment(@Observes final CreateOrUpdatePayment createOrUpdatePayment) {
      if (isEnabled()) {
         try {
            ObjectWithParent object = new ObjectWithParent(createOrUpdatePayment.getPayment(), createOrUpdatePayment.getOrganization().getId());
            Set<String> userIds = permissionAdapter.getOrganizationUsersByRole(createOrUpdatePayment.getOrganization(), RoleType.Manage);
            sendNotificationsByUsers(object, userIds, UPDATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void updateDefaultViewConfig(@Observes final UpdateDefaultViewConfig updateDefaultViewConfig) {
      if (isEnabled()) {
         try {
            ObjectWithParent object = new ObjectWithParent(updateDefaultViewConfig.getConfig(), getOrganization().getId(), getProject().getId());
            Set<String> userIds = Collections.singleton(getCurrentUserId());

            sendNotificationsByUsers(object, userIds, UPDATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void createOrUpdateSequence(@Observes final CreateOrUpdateSequence createOrUpdateSequence) {
      if (isEnabled()) {
         try {
            ObjectWithParent object = new ObjectWithParent(createOrUpdateSequence.getSequence(), getOrganization().getId(), getProject().getId());
            Set<String> userIds = permissionAdapter.getProjectUsersByRole(getOrganization(), getProject(), RoleType.TechConfig);

            sendNotificationsByUsers(object, userIds, UPDATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void removeSequenceNotification(@Observes final RemoveSequence removeSequence) {
      if (isEnabled()) {
         try {
            Sequence sequence = removeSequence.getSequence();
            Set<String> userIds = permissionAdapter.getProjectUsersByRole(getOrganization(), getProject(), RoleType.TechConfig);

            ResourceId message = new ResourceId(sequence.getId());

            sendNotificationsByUsers(message, userIds, REMOVE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void createUserNotification(@Observes final CreateOrUpdateUserNotification createOrUpdateUserNotification) {
      if (isEnabled()) {
         try {
            UserNotification notification = createOrUpdateUserNotification.getUserNotification();
            Event event = createEventForObject(notification, CREATE_EVENT_SUFFIX, notification.getUserId());
            sendNotificationsBatch(Collections.singletonList(event));
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void removeUserNotification(@Observes final RemoveUserNotification removeUserNotification) {
      if (isEnabled()) {
         try {
            UserNotification notification = removeUserNotification.getUserNotification();
            Event event = createEventForRemove(notification.getClass().getSimpleName(), new ResourceId(notification.getId()), notification.getUserId());
            sendNotificationsBatch(Collections.singletonList(event));
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void createAddFavoriteItemNotification(@Observes final AddFavoriteItem addFavoriteItem) {
      if (isEnabled()) {
         try {
            createFavoriteItemNotification(addFavoriteItem, CREATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   private void createFavoriteItemNotification(final FavoriteItem favoriteItem, final String suffix) {
      ResourceId resourceId = new ResourceId(favoriteItem.getItemId(), getOrganization().getId(), getProject().getId());
      String resource = favoriteItem.getResourceType().toString();
      String favoriteItemPrefix = "Favorite" + resource.substring(0, 1).toUpperCase() + resource.substring(1);
      Event event = new Event(eventChannel(favoriteItem.getUserId()), favoriteItemPrefix + suffix, resourceId);
      sendNotificationsBatch(Collections.singletonList(event));
   }

   public void createRemoveFavoriteItemNotification(@Observes final RemoveFavoriteItem removeFavoriteItem) {
      if (isEnabled()) {
         try {
            createFavoriteItemNotification(removeFavoriteItem, REMOVE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   private void sendNotificationByLinkType(final LinkInstance originalLinkInstance, final String linkTypeId, final String event) {
      final LinkInstance linkInstance = new LinkInstance(originalLinkInstance);
      LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      linkInstance.setData(constraintManager.decodeDataTypes(linkType, linkInstance.getData()));
      linkInstanceAdapter.mapLinkInstanceData(linkInstance);
      Set<String> userIds = resourceAdapter.getLinkTypeReaders(getOrganization(), getProject(), linkType);
      sendNotificationsByUsers(linkInstance, userIds, event);
   }

   private void sendResourceNotificationByLinkType(final LinkType linkType, final String event) {
      Set<String> userIds = resourceAdapter.getLinkTypeReaders(getOrganization(), getProject(), linkType);
      sendNotificationsByUsers(linkType, userIds, event);
   }

   public void createAddOrUpdateUserNotification(@Observes final CreateOrUpdateUser createOrUpdateUser) {
      if (isEnabled()) {
         try {
            Organization organization = organizationDao.getOrganizationById(createOrUpdateUser.getOrganizationId());
            ObjectWithParent object = new ObjectWithParent(cleanUserFromUserEvent(createOrUpdateUser), organization.getId());
            Set<String> users = resourceAdapter.getOrganizationReaders(organization);
            List<Event> events = users.stream().map(userId -> createEventForObjectWithParent(object, UPDATE_EVENT_SUFFIX, userId)).collect(Collectors.toList());
            sendNotificationsBatch(events);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void createAddOrUpdateGroupNotification(@Observes final CreateOrUpdateGroup createOrUpdateGroup) {
      if (isEnabled()) {
         try {
            Organization organization = organizationDao.getOrganizationById(createOrUpdateGroup.getOrganizationId());
            ObjectWithParent object = new ObjectWithParent(createOrUpdateGroup.getGroup(), organization.getId());
            Set<String> users = permissionAdapter.getOrganizationUsersByRole(organization, RoleType.UserConfig);
            List<Event> events = users.stream().map(userId -> createEventForObjectWithParent(object, UPDATE_EVENT_SUFFIX, userId)).collect(Collectors.toList());
            sendNotificationsBatch(events);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void reloadGroups(@Observes final ReloadGroups reloadGroups) {
      if (isEnabled()) {
         try {
            ObjectWithParent object = new ObjectWithParent(reloadGroups.getOrganizationId(), reloadGroups.getOrganizationId());
            Organization organization = organizationDao.getOrganizationById(reloadGroups.getOrganizationId());
            Set<String> users = permissionAdapter.getOrganizationUsersByRole(organization, RoleType.UserConfig);
            List<Event> events = users.stream().map(userId -> new Event(eventChannel(userId), Group.class.getSimpleName() + RELOAD_EVENT_SUFFIX, object)).collect(Collectors.toList());
            sendNotificationsBatch(events);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void createChainNotification(@Observes final CreateDocumentsAndLinks createDocumentsAndLinks) {
      if (isEnabled()) {
         try {
            Set<String> users = resourceAdapter.getProjectReaders(getOrganization(), getProject());
            ObjectWithParent object = new ObjectWithParent(createDocumentsAndLinks, getOrganization().getId(), getProject().getId());
            List<Event> events = users.stream().map(userId -> new Event(eventChannel(userId), "DocumentsAndLinks" + CREATE_EVENT_SUFFIX, object)).collect(Collectors.toList());
            sendNotificationsBatch(events);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void setDocumentLinksNotification(@Observes final SetDocumentLinks setDocumentLinks) {
      if (isEnabled()) {
         try {
            Set<String> users = resourceAdapter.getProjectReaders(getOrganization(), getProject());
            ObjectWithParent object = new ObjectWithParent(setDocumentLinks, getOrganization().getId(), getProject().getId());
            List<Event> events = users.stream().map(userId -> new Event(eventChannel(userId), "SetDocumentLinks" + CREATE_EVENT_SUFFIX, object)).collect(Collectors.toList());
            sendNotificationsBatch(events);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   private User cleanUserFromUserEvent(UserEvent event) {
      String organizationId = event.getOrganizationId();
      User user = event.getUser();
      return new User(user.getId(), user.getName(), user.getEmail(), Collections.singleton(organizationId));
   }

   public void createRemoveUserNotification(@Observes final RemoveUser removeUser) {
      if (isEnabled()) {
         try {
            Organization organization = organizationDao.getOrganizationById(removeUser.getOrganizationId());
            ResourceId resourceId = new ResourceId(removeUser.getUser().getId(), organization.getId());
            String className = removeUser.getUser().getClass().getSimpleName();
            Set<String> users = resourceAdapter.getOrganizationReaders(organization);
            List<Event> events = users.stream().map(userId -> createEventForRemove(className, resourceId, userId)).collect(Collectors.toList());
            sendNotificationsBatch(events);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void createRemoveGroupNotification(@Observes final RemoveGroup removeGroup) {
      if (isEnabled()) {
         try {
            Organization organization = organizationDao.getOrganizationById(removeGroup.getOrganizationId());
            ResourceId resourceId = new ResourceId(removeGroup.getGroup().getId(), organization.getId());
            String className = removeGroup.getGroup().getClass().getSimpleName();
            Set<String> users = permissionAdapter.getOrganizationUsersByRole(organization, RoleType.UserConfig);
            List<Event> events = users.stream().map(userId -> createEventForRemove(className, resourceId, userId)).collect(Collectors.toList());
            sendNotificationsBatch(events);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   private void sendNotificationsBatch(List<Event> notifications) {
      if (isEnabled() && notifications != null && notifications.size() > 0) {
         notifications.forEach(event -> {
            if (event.getData() instanceof ObjectWithParent) {
               ((ObjectWithParent) event.getData()).setCorrelationId(requestDataKeeper.getCorrelationId());
            }
         });

         pusherClient.trigger(notifications);
      }
   }

   private boolean isEnabled() {
      return pusherClient != null;
   }

   public static final class ResourceId {
      private final String id;
      private final String organizationId;
      private final String projectId;
      private final String extraId; // used to carry collectionId or linkTypeId

      public ResourceId(final String id) {
         this(id, null, null);
      }

      public ResourceId(final String id, final String organizationId) {
         this(id, organizationId, null);
      }

      public ResourceId(final String id, final String organizationId, final String projectId) {
         this(id, organizationId, projectId, null);
      }

      public ResourceId(final String id, final String organizationId, final String projectId, final String extraId) {
         this.id = id;
         this.organizationId = organizationId;
         this.projectId = projectId;
         this.extraId = extraId;
      }

      public String getId() {
         return id;
      }

      public String getOrganizationId() {
         return organizationId;
      }

      public String getProjectId() {
         return projectId;
      }

      public String getExtraId() {
         return extraId;
      }
   }

   public static final class ObjectWithParent {
      private final Object object;
      private final String organizationId;
      private final String projectId;
      private String correlationId;

      public ObjectWithParent(final Object object, final String organizationId) {
         this(object, organizationId, null);
      }

      public ObjectWithParent(final Object object, final String organizationId, final String projectId) {
         this.object = object;
         this.organizationId = organizationId;
         this.projectId = projectId;
      }

      public Object getObject() {
         return object;
      }

      public String getOrganizationId() {
         return organizationId;
      }

      public String getProjectId() {
         return projectId;
      }

      public String getCorrelationId() {
         return correlationId;
      }

      public void setCorrelationId(final String correlationId) {
         this.correlationId = correlationId;
      }

   }

}
