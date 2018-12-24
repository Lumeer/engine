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

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.PusherClient;
import io.lumeer.core.util.ResourceUtils;
import io.lumeer.engine.api.event.CreateDocument;
import io.lumeer.engine.api.event.CreateLinkInstance;
import io.lumeer.engine.api.event.CreateLinkType;
import io.lumeer.engine.api.event.CreateOrUpdatePayment;
import io.lumeer.engine.api.event.CreateOrUpdateUserNotification;
import io.lumeer.engine.api.event.CreateResource;
import io.lumeer.engine.api.event.RemoveDocument;
import io.lumeer.engine.api.event.RemoveLinkInstance;
import io.lumeer.engine.api.event.RemoveLinkType;
import io.lumeer.engine.api.event.RemoveResource;
import io.lumeer.engine.api.event.RemoveUserNotification;
import io.lumeer.engine.api.event.UpdateCompanyContact;
import io.lumeer.engine.api.event.UpdateDocument;
import io.lumeer.engine.api.event.UpdateLinkInstance;
import io.lumeer.engine.api.event.UpdateLinkType;
import io.lumeer.engine.api.event.UpdateResource;
import io.lumeer.engine.api.event.UpdateServiceLimits;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.marvec.pusher.data.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@ApplicationScoped
public class PusherFacade {

   private static final String PRIVATE_CHANNEL_PREFIX = "private-";
   private static final String UPDATE_EVENT_SUFFIX = ":update";
   private static final String CREATE_EVENT_SUFFIX = ":create";
   private static final String REMOVE_EVENT_SUFFIX = ":remove";

   private String PUSHER_APP_ID;
   private String PUSHER_KEY;
   private String PUSHER_SECRET;
   private String PUSHER_CLUSTER;

   private PusherClient pusherClient = null;

   @Inject
   private Logger log;

   @Inject
   private DefaultConfigurationProducer defaultConfigurationProducer;

   @Inject
   private AuthenticatedUser authenticatedUser;

   @Inject
   private PermissionsChecker permissionsChecker;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private LinkTypeFacade linkTypeFacade;

   @Inject
   private ViewDao viewDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private RequestDataKeeper requestDataKeeper;

   @PostConstruct
   public void init() {
      PUSHER_APP_ID = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.PUSHER_APP_ID)).orElse("");
      PUSHER_KEY = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.PUSHER_KEY)).orElse("");
      PUSHER_SECRET = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.PUSHER_SECRET)).orElse("");
      PUSHER_CLUSTER = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.PUSHER_CLUSTER)).orElse("");

      if (PUSHER_SECRET != null && !"".equals(PUSHER_SECRET)) {
         pusherClient = new PusherClient(PUSHER_APP_ID, PUSHER_KEY, PUSHER_SECRET, PUSHER_CLUSTER);
      }
   }

   public String getPusherAppId() {
      return PUSHER_APP_ID;
   }

   public String getPusherKey() {
      return PUSHER_KEY;
   }

   public String getPusherSecret() {
      return PUSHER_SECRET;
   }

   public String getPusherCluster() {
      return PUSHER_CLUSTER;
   }

   public PusherClient getPusherClient() {
      return pusherClient;
   }

   public void createResource(@Observes final CreateResource createResource) {
      if (isEnabled()) {
         try {
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
      Set<String> removedUsers = getRemovedUsersPermissions(originalOrganization, updatedOrganization);
      removedUsers.remove(authenticatedUser.getCurrentUserId());
      if (!removedUsers.isEmpty()) {
         sendNotificationsByUsers(updatedOrganization, removedUsers, REMOVE_EVENT_SUFFIX);
      }
   }

   private Set<String> getRemovedUsersPermissions(final Resource originalResource, final Resource updatedResource) {
      return getPermissionsDifference(originalResource, updatedResource);
   }

   private Set<String> getPermissionsDifference(final Resource resource1, final Resource resource2) {
      if (resource1 == null || resource2 == null) {
         return Collections.emptySet();
      }
      Set<Permission> permissions1 = resource1.getPermissions().getUserPermissions();
      Set<Permission> permissions2 = resource2.getPermissions().getUserPermissions();
      // TODO groups

      Set<String> users = permissions1.stream().filter(ResourceUtils::canReadByPermission).map(Permission::getId).collect(Collectors.toSet());
      users.removeAll(permissions2.stream().filter(ResourceUtils::canReadByPermission).map(Permission::getId).collect(Collectors.toSet()));
      return users;
   }

   private void checkProjectPermissionsChange(final Project originalProject, final Project updatedProject) {
      Set<String> removedUsers = getRemovedUsersPermissions(originalProject, updatedProject);
      removedUsers.removeAll(getOrganizationManagers());
      removedUsers.remove(authenticatedUser.getCurrentUserId());
      if (!removedUsers.isEmpty()) {
         sendNotificationsByUsers(updatedProject, removedUsers, REMOVE_EVENT_SUFFIX);
      }
   }

   private void checkCollectionsPermissionsChange(final Collection originalCollection, final Collection updatedCollection) {
      List<LinkType> linkTypes = linkTypeDao.getLinkTypesByCollectionId(updatedCollection.getId());
      List<View> views = viewDao.getAllViews();

      List<Event> notifications = new ArrayList<>();

      Set<String> removedUsers = getRemovedUsersPermissions(originalCollection, updatedCollection);
      removedUsers.removeAll(getWorkspaceManagers());
      removedUsers.remove(authenticatedUser.getCurrentUserId());

      if (removedUsers.size() > 0) {
         notifications.addAll(createRemoveCollectionNotification(updatedCollection, removedUsers, views));
         if (linkTypes.size() > 0) {
            notifications.addAll(createRemoveCollectionLinkTypesNotification(linkTypes, removedUsers, views));
         }
      }

      Set<String> addedUsers = getAddedUsersPermissions(originalCollection, updatedCollection);
      addedUsers.removeAll(getWorkspaceManagers());
      addedUsers.remove(authenticatedUser.getCurrentUserId());

      if (addedUsers.size() > 0 && linkTypes.size() > 0) {
         Map<String, Collection> collectionMapByLinkTypes = getCollectionsMapFromLinkTypes(linkTypes);
         notifications.addAll(createSendCollectionLinkTypesNotification(linkTypes, addedUsers, views, collectionMapByLinkTypes));
      }

      if (notifications.size() > 0) {
         sendNotificationsBatch(notifications);
      }
   }

   private Map<String, Collection> getCollectionsMapFromLinkTypes(List<LinkType> linkTypes) {
      final Set<String> collectionIds = linkTypes.stream().map(LinkType::getCollectionIds)
                                                 .flatMap(java.util.Collection::stream).collect(Collectors.toSet());
      return collectionDao.getCollectionsByIds(collectionIds).stream().collect(Collectors.toMap(Collection::getId, Function.identity()));
   }

   private List<Event> createRemoveCollectionNotification(final Collection collection, final Set<String> userIds, final List<View> views) {
      List<Event> notifications = new ArrayList<>();

      for (String user : userIds) { // checks if user has collection in some view
         List<View> viewsByUser = views.stream().filter(view -> permissionsChecker.hasRole(view, Role.READ, user)).collect(Collectors.toList());
         Set<String> collectionIdsInViews = viewsByUser.stream().map(view -> view.getQuery().getCollectionIds())
                                                       .flatMap(java.util.Collection::stream).collect(Collectors.toSet());
         if (!collectionIdsInViews.contains(collection.getId())) {
            notifications.add(createEventForResource(collection, REMOVE_EVENT_SUFFIX, user));
         }
      }

      return notifications;
   }

   private List<Event> createRemoveCollectionLinkTypesNotification(final List<LinkType> linkTypes, final Set<String> userIds, final List<View> views) {
      List<Event> notifications = new ArrayList<>();

      for (String user : userIds) {
         filterLinkTypesNotInViews(linkTypes, views, user).forEach(linkType -> notifications.add(createEventForRemove(linkType.getClass().getSimpleName(),
               new ResourceId(linkType.getId(), getOrganization().getId(), getProject().getId()), user)));
      }

      return notifications;
   }

   private List<LinkType> filterLinkTypesNotInViews(List<LinkType> linkTypes, List<View> views, String user) {
      List<View> viewsByUser = views.stream().filter(view -> permissionsChecker.hasRole(view, Role.READ, user)).collect(Collectors.toList());
      Set<String> linkTypeIdsInView = viewsByUser.stream().map(view -> view.getQuery().getLinkTypeIds())
                                                 .flatMap(java.util.Collection::stream).collect(Collectors.toSet());

      return linkTypes.stream().filter(linkType -> !linkTypeIdsInView.contains(linkType.getId())).collect(Collectors.toList());
   }

   private Set<String> getAddedUsersPermissions(final Resource originalResource, final Resource updatedResource) {
      return getPermissionsDifference(updatedResource, originalResource);
   }

   private List<Event> createSendCollectionLinkTypesNotification(final List<LinkType> linkTypes, final Set<String> userIds, final List<View> views, final Map<String, Collection> collectionsMap) {
      List<Event> notifications = new ArrayList<>();

      for (String user : userIds) {
         filterLinkTypesNotInViews(linkTypes, views, user).stream()
                                                          .filter(linkType -> canUserReadLinkType(user, linkType, collectionsMap))
                                                          .forEach(linkType -> notifications.add(createEventForWorkspaceObject(linkType, linkType.getId(), UPDATE_EVENT_SUFFIX, user)));
      }

      return notifications;
   }

   private boolean canUserReadLinkType(String userId, LinkType linkType, Map<String, Collection> collectionsMap) {
      long count = linkType.getCollectionIds().stream().map(collectionsMap::get)
                           .filter(collection -> collection == null || !permissionsChecker.hasRole(collection, Role.READ, userId)).count();

      return count == 0;
   }

   private void checkViewPermissionsChange(final View originalView, final View updatedView) {
      List<View> views = viewDao.getAllViews();
      List<LinkType> linkTypes = linkTypeDao.getLinkTypesByIds(updatedView.getQuery().getLinkTypeIds());
      List<Collection> collections = new ArrayList<>(getCollectionsMapFromLinkTypes(linkTypes).values());

      List<Event> notifications = new ArrayList<>();

      Set<String> removedUsers = getRemovedUsersPermissions(originalView, updatedView);
      removedUsers.removeAll(getWorkspaceManagers());
      removedUsers.remove(authenticatedUser.getCurrentUserId());

      if (removedUsers.size() > 0) {
         removedUsers.forEach(userId -> notifications.add(createEventForResource(updatedView, REMOVE_EVENT_SUFFIX, userId)));
         if (collections.size() > 0) {
            collections.forEach(collection -> notifications.addAll(createRemoveCollectionNotification(collection, removedUsers, views)));
         }
         if (linkTypes.size() > 0) {
            linkTypes.forEach(linkType -> notifications.addAll(createRemoveCollectionLinkTypesNotification(linkTypes, removedUsers, views)));
         }
      }

      Set<String> addedUsers = getAddedUsersPermissions(originalView, updatedView);
      addedUsers.removeAll(getWorkspaceManagers());
      addedUsers.remove(authenticatedUser.getCurrentUserId());

      for (String user : addedUsers) {
         notifications.addAll(linkTypes.stream()
                                       .map(linkType -> createEventForWorkspaceObject(linkType, linkType.getId(), UPDATE_EVENT_SUFFIX, user)).collect(Collectors.toList()));

         notifications.addAll(collections.stream()
                                         .map(collection -> createEventForObjectWithParent(new ObjectWithParent(collection, getOrganization().getId(), getProject().getId())
                                               , UPDATE_EVENT_SUFFIX, user)).collect(Collectors.toList()));
      }

      if (notifications.size() > 0) {
         sendNotificationsBatch(notifications);
      }
   }

   private Set<String> getOrganizationManagers() {
      return permissionsChecker.getOrganizationManagers();
   }

   private Set<String> getWorkspaceManagers() {
      return permissionsChecker.getWorkspaceManagers();
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

   private void sendOrganizationNotifications(final Organization organization, final String event) {
      sendNotificationsByUsers(organization, ResourceUtils.usersAllowedRead(organization), event);
   }

   private void sendNotificationsByUsers(final Object object, final Set<String> userIds, final String event) {
      sendNotificationsBatch(userIds.stream()
                                    .map(userId -> createEvent(object, event, userId))
                                    .collect(Collectors.toList()));
   }

   private Event createEvent(final Object object, final String event, final String userId) {
      if (object instanceof Document) {
         return createEventForWorkspaceObject(object, ((Document) object).getId(), event, userId);
      } else if (object instanceof LinkType) {
         return createEventForWorkspaceObject(object, ((LinkType) object).getId(), event, userId);
      } else if (object instanceof LinkInstance) {
         return createEventForWorkspaceObject(object, ((LinkInstance) object).getId(), event, userId);
      } else if (object instanceof Resource) {
         return createEventForResource((Resource) object, event, userId);
      } else if (object instanceof ObjectWithParent) {
         ObjectWithParent objectWithParent = ((ObjectWithParent) object);
         if (objectWithParent.object instanceof Resource) {
            return createEventForNestedResource(objectWithParent, event, userId);
         } else {
            return createEventForObjectWithParent(objectWithParent, event, userId);
         }
      } else {
         return createEventForObject(object, event, userId);
      }

   }

   private Event createEventForWorkspaceObject(final Object object, final String id, final String event, final String userId) {
      if (REMOVE_EVENT_SUFFIX.equals(event)) {
         return createEventForRemove(object.getClass().getSimpleName(), new ResourceId(id, getOrganization().getId(), getProject().getId()), userId);
      }
      return createEventForObjectWithParent(new ObjectWithParent(object, getOrganization().getId(), getProject().getId()), event, userId);
   }

   private Event createEventForRemove(final String className, final ResourceId object, final String userId) {
      return new Event(eventChannel(userId), className + REMOVE_EVENT_SUFFIX, object);
   }

   private Event createEventForResource(final Resource resource, final String event, final String userId) {
      if (REMOVE_EVENT_SUFFIX.equals(event)) {
         return createEventForRemove(resource.getClass().getSimpleName(), getResourceId(resource), userId);
      }
      return createEventForObject(filterUserRoles(userId, resource), event, userId);
   }

   private Event createEventForObject(final Object object, final String event, final String userId) {
      return new Event(eventChannel(userId), object.getClass().getSimpleName() + event, object);
   }

   private Event createEventForNestedResource(final ObjectWithParent objectWithParent, final String event, final String userId) {
      Resource resource = (Resource) objectWithParent.object;
      if (REMOVE_EVENT_SUFFIX.equals(event)) {
         return createEventForRemove(resource.getClass().getSimpleName(), getResourceId(resource), userId);
      }

      Resource filteredResource = filterUserRoles(userId, resource);
      ObjectWithParent newObjectWithParent = new ObjectWithParent(filteredResource, objectWithParent.organizationId, objectWithParent.projectId);
      newObjectWithParent.setCorrelationId(objectWithParent.getCorrelationId());
      return createEventForObjectWithParent(newObjectWithParent, event, userId);
   }

   private Event createEventForObjectWithParent(final ObjectWithParent objectWithParent, final String event, final String userId) {
      return new Event(eventChannel(userId), objectWithParent.object.getClass().getSimpleName() + event, objectWithParent);
   }

   private String eventChannel(String userId) {
      return PRIVATE_CHANNEL_PREFIX + userId;
   }

   private ResourceId getResourceId(Resource resource) {
      if (resource instanceof Organization) {
         return new ResourceId(resource.getId());
      } else if (resource instanceof Project) {
         return new ResourceId(resource.getId(), getOrganization().getId());
      } else if (resource instanceof View) {
         return new ResourceId(resource.getCode(), getOrganization().getId(), getProject().getId());
      }
      return new ResourceId(resource.getId(), getOrganization().getId(), getProject().getId());
   }

   private void sendProjectNotifications(final Project project, final String event) {
      Set<String> userIds = ResourceUtils.usersAllowedRead(project);
      userIds.addAll(getOrganizationManagers());
      sendNotificationsByUsers(new ObjectWithParent(project, getOrganization().getId()), userIds, event);
   }

   private Organization getOrganization() {
      if (!workspaceKeeper.getOrganization().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }

      return workspaceKeeper.getOrganization().get();
   }

   private void sendViewNotifications(final View view, final String event) {
      Set<String> userIds = ResourceUtils.usersAllowedRead(view);
      userIds.addAll(getWorkspaceManagers());
      ObjectWithParent object = new ObjectWithParent(view, getOrganization().getId(), getProject().getId());
      sendNotificationsByUsers(object, userIds, event);
   }

   private void sendCollectionNotifications(final Collection collection, final String event) {
      Set<String> userIds = collectionFacade.getUsersIdsWithAccess(collection);
      ObjectWithParent object = new ObjectWithParent(collection, getOrganization().getId(), getProject().getId());
      object.setCorrelationId(requestDataKeeper.getCorrelationId());
      sendNotificationsByUsers(object, userIds, event);
   }

   private Project getProject() {
      if (!workspaceKeeper.getProject().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }

      return workspaceKeeper.getProject().get();
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

   private void documentNotification(final Document document, final String eventSuffix) {
      if (isEnabled()) {
         try {
            sendNotificationsByUsers(document, collectionFacade.getUsersIdsWithAccess(document.getCollectionId()), eventSuffix);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
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
            sendResourceNotificationByLinkType(createLinkType.getLinkType(), CREATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void updateLinkType(@Observes final UpdateLinkType updateLinkType) {
      if (isEnabled()) {
         try {
            sendResourceNotificationByLinkType(updateLinkType.getLinkType(), UPDATE_EVENT_SUFFIX);
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
            Set<String> userIds = ResourceUtils.getManagers(organizationFacade.getOrganizationById(updateCompanyContact.getCompanyContact().getOrganizationId()));
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
            Set<String> userIds = ResourceUtils.getManagers(updateServiceLimits.getOrganization());
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
            Set<String> userIds = ResourceUtils.getManagers(createOrUpdatePayment.getOrganization());
            sendNotificationsByUsers(object, userIds, UPDATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void createUserNotification(@Observes final CreateOrUpdateUserNotification createOrUpdateUserNotification) {
      if (isEnabled()) {
         try {
            sendNotification(createOrUpdateUserNotification.getUserNotification().getUserId(),
                  CREATE_EVENT_SUFFIX, createOrUpdateUserNotification.getUserNotification()
            );
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void removeUserNotification(@Observes final RemoveUserNotification removeUserNotification) {
      if (isEnabled()) {
         try {
            sendNotification(
                  removeUserNotification.getUserNotification().getUserId(),
                  REMOVE_EVENT_SUFFIX, new ResourceId(removeUserNotification.getUserNotification().getId())
            );
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   private void sendNotificationByLinkType(final LinkInstance linkInstance, final String linkTypeId, final String event) {
      Set<String> userIds = getUserIdsForLinkType(linkTypeFacade.getLinkType(linkTypeId));
      sendNotificationsByUsers(linkInstance, userIds, event);
   }

   private Set<String> getUserIdsForLinkType(final LinkType linkType) {
      if (linkType == null) {
         return Collections.emptySet();
      }

      Set<Set<String>> userIdsMaps = linkType
            .getCollectionIds()
            .stream()
            .map(collectionFacade::getUsersIdsWithAccess)
            .collect(Collectors.toSet());
      return intersection(userIdsMaps);
   }

   private void sendResourceNotificationByLinkType(final LinkType linkType, final String event) {
      Set<String> userIds = getUserIdsForLinkType(linkType);
      sendNotificationsByUsers(linkType, userIds, event);
   }

   private void sendNotification(final String userId, final String event, final Object message) {
      if (isEnabled() && authenticatedUser.getCurrentUserId() != null && !authenticatedUser.getCurrentUserId().equals(userId)) {
         pusherClient.trigger(PRIVATE_CHANNEL_PREFIX + userId, message.getClass().getSimpleName() + event, message);
      }
   }

   private void sendNotificationsBatch(List<Event> notifications) {
      if (isEnabled() && notifications != null && notifications.size() > 0) {
         pusherClient.trigger(notifications);
      }
   }

   private boolean isEnabled() {
      return pusherClient != null;
   }

   private <T extends Resource> T filterUserRoles(final String userId, final T resource) {
      final T copy = resource.copy();
      if (permissionsChecker.hasRole(resource, Role.MANAGE, userId)) {
         return copy;
      }

      Set<Role> roles = permissionsChecker.getActualRoles(copy, userId);
      Permission permission = Permission.buildWithRoles(userId, roles);

      copy.getPermissions().clear();
      copy.getPermissions().updateUserPermissions(permission);

      return copy;
   }

   private static Set<String> intersection(Set<Set<String>> sets) {
      return sets.stream().map(HashSet::new).reduce(
            (s1, s2) -> {
               s1.retainAll(s2);
               return s1;
            }).orElse(new HashSet<>());
   }

   public static final class ResourceId {
      private final String id;
      private final String organizationId;
      private final String projectId;

      public ResourceId(final String id) {
         this(id, null, null);
      }

      public ResourceId(final String id, final String organizationId) {
         this(id, organizationId, null);
      }

      public ResourceId(final String id, final String organizationId, final String projectId) {
         this.id = id;
         this.organizationId = organizationId;
         this.projectId = projectId;
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
