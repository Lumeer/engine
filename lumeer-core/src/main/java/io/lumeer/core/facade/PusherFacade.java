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
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.ResourceComment;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.Sequence;
import io.lumeer.api.model.User;
import io.lumeer.api.model.UserNotification;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.model.common.WithId;
import io.lumeer.api.util.ResourceUtils;
import io.lumeer.core.action.DelayedActionProcessor;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.PusherClient;
import io.lumeer.core.util.QueryUtils;
import io.lumeer.engine.api.event.AddFavoriteItem;
import io.lumeer.engine.api.event.CreateDocument;
import io.lumeer.engine.api.event.CreateDocumentsAndLinks;
import io.lumeer.engine.api.event.CreateLinkInstance;
import io.lumeer.engine.api.event.CreateLinkType;
import io.lumeer.engine.api.event.CreateOrUpdatePayment;
import io.lumeer.engine.api.event.CreateOrUpdateSequence;
import io.lumeer.engine.api.event.CreateOrUpdateUser;
import io.lumeer.engine.api.event.CreateOrUpdateUserNotification;
import io.lumeer.engine.api.event.CreateResource;
import io.lumeer.engine.api.event.CreateResourceComment;
import io.lumeer.engine.api.event.FavoriteItem;
import io.lumeer.engine.api.event.ImportResource;
import io.lumeer.engine.api.event.ReloadResourceContent;
import io.lumeer.engine.api.event.RemoveDocument;
import io.lumeer.engine.api.event.RemoveFavoriteItem;
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
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import org.marvec.pusher.data.BackupDataEvent;
import org.marvec.pusher.data.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.core.Link;

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
   private CollectionFacade collectionFacade;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private LinkTypeFacade linkTypeFacade;

   @Inject
   private LinkInstanceFacade linkInstanceFacade;

   @Inject
   private ViewDao viewDao;

   @Inject
   private ViewFacade viewFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private RequestDataKeeper requestDataKeeper;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   private ConstraintManager constraintManager;

   @PostConstruct
   public void init() {
      constraintManager = ConstraintManager.getInstance(configurationProducer);
      pusherClient = PusherClient.getInstance(configurationProducer);
      delayedActionProcessor.setPusherClient(pusherClient);
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
         Set<String> userIds = ResourceUtils.usersAllowedRead(templateCreated.getProject());
         userIds.addAll(getOrganizationManagers());
         sendNotificationsByUsers(new ObjectWithParent(templateCreated, getOrganization().getId(), templateCreated.getProject().getId()), userIds, CREATE_EVENT_SUFFIX);
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
      Set<String> removedUsers = ResourceUtils.getRemovedPermissions(originalOrganization, updatedOrganization);
      removedUsers.remove(authenticatedUser.getCurrentUserId());
      if (!removedUsers.isEmpty()) {
         sendNotificationsByUsers(updatedOrganization, removedUsers, REMOVE_EVENT_SUFFIX);
      }
   }

   private void checkProjectPermissionsChange(final Project originalProject, final Project updatedProject) {
      Set<String> removedUsers = ResourceUtils.getRemovedPermissions(originalProject, updatedProject);
      removedUsers.removeAll(getOrganizationManagers());
      removedUsers.remove(authenticatedUser.getCurrentUserId());
      if (!removedUsers.isEmpty()) {
         sendNotificationsByUsers(updatedProject, removedUsers, REMOVE_EVENT_SUFFIX);
      }
   }

   private void checkCollectionsPermissionsChange(final Collection originalCollection, final Collection updatedCollection) {
      Set<String> removedUsers = ResourceUtils.getRemovedPermissions(originalCollection, updatedCollection);
      removedUsers.removeAll(getWorkspaceManagers());
      removedUsers.remove(authenticatedUser.getCurrentUserId());

      Set<String> addedUsers = ResourceUtils.getAddedPermissions(originalCollection, updatedCollection);
      addedUsers.removeAll(getWorkspaceManagers());
      addedUsers.remove(authenticatedUser.getCurrentUserId());

      if (removedUsers.isEmpty() && addedUsers.isEmpty()) {
         return;
      }

      List<LinkType> linkTypes = linkTypeDao.getLinkTypesByCollectionId(updatedCollection.getId());
      List<View> views = viewDao.getAllViews();
      List<Collection> allCollections = collectionDao.getAllCollections();
      List<Event> notifications = new ArrayList<>();

      if (removedUsers.size() > 0) {
         notifications.addAll(createRemoveCollectionNotification(updatedCollection, removedUsers, views, linkTypes));
         if (linkTypes.size() > 0) {
            notifications.addAll(createRemoveCollectionLinkTypesNotification(linkTypes, allCollections, removedUsers, views));
         }
      }

      if (addedUsers.size() > 0 && linkTypes.size() > 0) {
         notifications.addAll(createSendCollectionLinkTypesNotification(linkTypes, allCollections, addedUsers, views));
      }

      if (notifications.size() > 0) {
         sendNotificationsBatch(notifications);
      }
   }

   private List<Event> createRemoveCollectionNotification(final Collection collection, final Set<String> userIds, final List<View> views, final List<LinkType> linkTypes) {
      List<Event> notifications = new ArrayList<>();

      Set<String> usersWithRights = ResourceUtils.usersAllowedRead(collection);
      for (String user : userIds) { // checks if user has collection in some view
         if (usersWithRights.contains(user)) {
            continue;
         }

         List<View> viewsByUser = views.stream().filter(view -> permissionsChecker.hasRole(view, Role.READ, user)).collect(Collectors.toList());
         Set<String> collectionIdsInViews = viewsByUser.stream().map(view -> QueryUtils.getQueryCollectionIds(view.getQuery(), linkTypes))
                                                       .flatMap(java.util.Collection::stream)
                                                       .collect(Collectors.toSet());

         if (collectionIdsInViews.contains(collection.getId())) {
            notifications.add(createEventForResource(collection, UPDATE_EVENT_SUFFIX, user));
         } else {
            notifications.add(createEventForResource(collection, REMOVE_EVENT_SUFFIX, user));
         }
      }

      return notifications;
   }

   private List<Event> createRemoveCollectionLinkTypesNotification(final List<LinkType> linkTypes, final List<Collection> collections, final Set<String> userIds, final List<View> views) {
      List<Event> notifications = new ArrayList<>();

      for (String user : userIds) {
         filterLinkTypesNotInViews(linkTypes, collections, views, user, true).forEach(linkType -> notifications.add(createEventForRemove(linkType.getClass().getSimpleName(),
               new ResourceId(linkType.getId(), getOrganization().getId(), getProject().getId()), user)));
      }

      return notifications;
   }

   private List<LinkType> filterLinkTypesNotInViews(List<LinkType> linkTypes, List<Collection> collections, List<View> views, String user, boolean includeReadableCollections) {
      List<View> viewsByUser = views.stream().filter(view -> permissionsChecker.hasRole(view, Role.READ, user)).collect(Collectors.toList());
      Set<String> linkTypeIdsInViews = viewsByUser.stream().map(view -> view.getQuery().getLinkTypeIds())
                                                  .flatMap(java.util.Collection::stream).collect(Collectors.toSet());
      if (includeReadableCollections) {
         Set<String> readableCollectionsIds = filterViewsReadableCollectionsIds(views, linkTypes, collections, user);
         return linkTypes.stream().filter(linkType -> !linkTypeIdsInViews.contains(linkType.getId()) && !readableCollectionsIds.containsAll(linkType.getCollectionIds())).collect(Collectors.toList());
      }

      return linkTypes.stream().filter(linkType -> !linkTypeIdsInViews.contains(linkType.getId())).collect(Collectors.toList());
   }

   private Set<String> filterViewsReadableCollectionsIds(final List<View> views, final List<LinkType> linkTypes, final List<Collection> collections, final String user) {
      Set<String> readableCollectionsIds = QueryUtils.getViewsCollectionIds(views, linkTypes);
      readableCollectionsIds.addAll(collections.stream().filter(collection -> permissionsChecker.hasRole(collection, Role.READ, user)).map(Collection::getId).collect(Collectors.toSet()));
      return readableCollectionsIds;
   }

   private List<Event> createSendCollectionLinkTypesNotification(final List<LinkType> linkTypes, final List<Collection> collections, final Set<String> userIds, final List<View> views) {
      List<Event> notifications = new ArrayList<>();

      Map<String, Collection> collectionsMap = collections.stream().collect(Collectors.toMap(Collection::getId, Function.identity()));

      for (String user : userIds) {
         List<View> viewsByUser = views.stream().filter(view -> permissionsChecker.hasRole(view, Role.READ, user)).collect(Collectors.toList());
         Set<String> viewsCollectionsIds = QueryUtils.getViewsCollectionIds(viewsByUser, linkTypes);
         filterLinkTypesNotInViews(linkTypes, collections, views, user, false).stream()
                                                                              .filter(linkType -> canUserReadLinkType(user, linkType, collectionsMap, viewsCollectionsIds))
                                                                              .forEach(linkType -> notifications.add(createEventForWorkspaceObject(linkType, linkType.getId(), UPDATE_EVENT_SUFFIX, user)));
      }

      return notifications;
   }

   private boolean canUserReadLinkType(String userId, LinkType linkType, Map<String, Collection> collectionsMap, Set<String> viewsCollectionsIds) {
      return linkType.getCollectionIds().stream().map(collectionsMap::get)
                     .allMatch(collection -> collection != null && (permissionsChecker.hasRole(collection, Role.READ, userId) || viewsCollectionsIds.contains(collection.getId())));
   }

   private void checkViewPermissionsChange(final View originalView, final View updatedView) {
      Set<String> removedUsers = ResourceUtils.getRemovedPermissions(originalView, updatedView);
      removedUsers.removeAll(getWorkspaceManagers());
      removedUsers.remove(authenticatedUser.getCurrentUserId());

      Set<String> addedUsers = ResourceUtils.getAddedPermissions(originalView, updatedView);
      addedUsers.removeAll(getWorkspaceManagers());
      addedUsers.remove(authenticatedUser.getCurrentUserId());

      if (removedUsers.isEmpty() && addedUsers.isEmpty()) {
         return;
      }

      List<View> views = viewDao.getAllViews();
      List<LinkType> linkTypes = linkTypeDao.getLinkTypesByIds(updatedView.getQuery().getLinkTypeIds());
      List<Collection> collections = getCollectionsByQuery(updatedView.getQuery(), linkTypes);
      List<Collection> allCollections = collectionDao.getAllCollections();
      List<LinkType> allLinkTypes = linkTypeDao.getAllLinkTypes();

      List<Event> notifications = new ArrayList<>();

      if (removedUsers.size() > 0) {
         removedUsers.forEach(userId -> notifications.add(createEventForResource(updatedView, REMOVE_EVENT_SUFFIX, userId)));
         if (collections.size() > 0) {
            collections.forEach(collection -> notifications.addAll(createRemoveCollectionNotification(collection, removedUsers, views, linkTypes)));
         }
         if (linkTypes.size() > 0) {
            linkTypes.forEach(linkType -> notifications.addAll(createRemoveCollectionLinkTypesNotification(linkTypes, allCollections, removedUsers, views)));
         }
      }

      for (String user : addedUsers) {
         List<LinkType> linkTypesByUser = new ArrayList<>(linkTypes);
         linkTypesByUser.addAll(linkTypesByViewReadPermission(updatedView, views, allCollections, allLinkTypes, user));
         notifications.addAll(linkTypesByUser.stream()
                                       .map(linkType -> createEventForWorkspaceObject(linkType, linkType.getId(), UPDATE_EVENT_SUFFIX, user)).collect(Collectors.toList()));

         notifications.addAll(collections.stream()
                                         .map(collection -> createEventForObjectWithParent(new ObjectWithParent(collection, getOrganization().getId(), getProject().getId()),
                                               getResourceId(collection),
                                               UPDATE_EVENT_SUFFIX, user)).collect(Collectors.toList()));
      }

      if (notifications.size() > 0) {
         sendNotificationsBatch(notifications);
      }
   }

   private List<LinkType> linkTypesByViewReadPermission(View currentView, List<View> views, List<Collection> collections, List<LinkType> linkTypes, String userId) {
      List<View> viewsByUser = views.stream().filter(view -> permissionsChecker.hasRole(view, Role.READ, userId)).collect(Collectors.toList());
      Set<String> collectionsIdsByViews = filterViewsReadableCollectionsIds(viewsByUser, linkTypes, collections, userId);
      List<LinkType> linkTypesByViews = linkTypes.stream().filter(linkType -> collectionsIdsByViews.containsAll(linkType.getCollectionIds())).collect(Collectors.toList());

      List<View> viewsByUserWithoutCurrent = viewsByUser.stream().filter(view -> !view.getId().equals(currentView.getId())).collect(Collectors.toList());
      Set<String> collectionsIdsByViewsWithoutCurrent = filterViewsReadableCollectionsIds(viewsByUserWithoutCurrent, linkTypes, collections, userId);
      List<LinkType> linkTypesByViewsWithoutCurrent = linkTypes.stream().filter(linkType -> collectionsIdsByViewsWithoutCurrent.containsAll(linkType.getCollectionIds())).collect(Collectors.toList());

      linkTypesByViews.removeAll(linkTypesByViewsWithoutCurrent);
      return linkTypesByViews;
   }

   private List<Collection> getCollectionsByQuery(Query query, List<LinkType> linkTypes) {
      Set<String> collectionIds = QueryUtils.getQueryCollectionIds(query, linkTypes);
      return collectionDao.getCollectionsByIds(collectionIds);
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

   private void processWithId(final WithId resource, final String event) {
      if (resource instanceof Collection) {
         sendCollectionNotifications((Collection) resource, event);
      } else if (resource instanceof LinkType) {
         sendResourceNotificationByLinkType((LinkType) resource, event);
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
      } else if (object instanceof ResourceComment) {
         return createEventForWorkspaceObject(object, ((ResourceComment) object).getId(), event, userId);
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
      final ObjectWithParent normalMessage = new ObjectWithParent(object, getOrganization().getId(), getProject().getId());
      String extraId = null;
      if (object instanceof Document) {
         extraId = ((Document) object).getCollectionId();
      } else if (object instanceof LinkInstance) {
         extraId = ((LinkInstance) object).getLinkTypeId();
      } else if (object instanceof ResourceComment) {
         final ResourceComment comment = ((ResourceComment) object);
         extraId = comment.getResourceType().toString() + '/' + comment.getResourceId();
      }
      final ResourceId alternateMessage = new ResourceId(id, getOrganization().getId(), getProject().getId(), extraId);
      return createEventForObjectWithParent(normalMessage, alternateMessage, event, userId);
   }

   private Event createEventForRemove(final String className, final ResourceId object, final String userId) {
      return new Event(eventChannel(userId), className + REMOVE_EVENT_SUFFIX, object, null);
   }

   private Event createEventForResource(final Resource resource, final String event, final String userId) {
      if (REMOVE_EVENT_SUFFIX.equals(event)) {
         return createEventForRemove(resource.getClass().getSimpleName(), getResourceId(resource), userId);
      }
      return createEventForObject(filterUserRoles(userId, resource), getResourceId(resource), event, userId);
   }

   private Event createEventForObject(final Object object, final String event, final String userId) {
      return new Event(eventChannel(userId), object.getClass().getSimpleName() + event, object);
   }

   private BackupDataEvent createEventForObject(final Object object, final Object backupObject, final String event, final String userId) {
      return new BackupDataEvent(eventChannel(userId), object.getClass().getSimpleName() + event, object, backupObject, null);
   }

   private Event createEventForNestedResource(final ObjectWithParent objectWithParent, final String event, final String userId) {
      Resource resource = (Resource) objectWithParent.object;
      if (REMOVE_EVENT_SUFFIX.equals(event)) {
         return createEventForRemove(resource.getClass().getSimpleName(), getResourceId(resource), userId);
      }

      Resource filteredResource = filterUserRoles(userId, resource);
      ObjectWithParent newObjectWithParent = new ObjectWithParent(filteredResource, objectWithParent.organizationId, objectWithParent.projectId);
      newObjectWithParent.setCorrelationId(objectWithParent.getCorrelationId());
      return createEventForObjectWithParent(newObjectWithParent, getResourceId(resource), event, userId);
   }

   private Event createEventForObjectWithParent(final ObjectWithParent objectWithParent, final String event, final String userId) {
      return new Event(eventChannel(userId), objectWithParent.object.getClass().getSimpleName() + event, objectWithParent);
   }

   private BackupDataEvent createEventForObjectWithParent(final ObjectWithParent objectWithParent, final ResourceId resourceId, final String event, final String userId) {
      return new BackupDataEvent(eventChannel(userId), objectWithParent.object.getClass().getSimpleName() + event, objectWithParent, resourceId, null);
   }

   public static String eventChannel(String userId) {
      return PRIVATE_CHANNEL_PREFIX + userId;
   }

   private ResourceId getResourceId(Resource resource) {
      if (resource instanceof Organization) {
         return new ResourceId(resource.getId(), null, null);
      } else if (resource instanceof Project) {
         return new ResourceId(resource.getId(), getOrganization().getId(), null);
      }
      return new ResourceId(resource.getId(), getOrganization().getId(), getProject().getId());
   }

   private void sendProjectNotifications(final Project project, final String event) {
      Set<String> userIds = ResourceUtils.usersAllowedRead(project);
      userIds.addAll(getOrganizationManagers());
      sendNotificationsByUsers(new ObjectWithParent(project, getOrganization().getId()), userIds, event);
   }

   private Organization getOrganization() {
      if (workspaceKeeper.getOrganization().isEmpty()) {
         throw new ResourceNotFoundException(ResourceType.ORGANIZATION);
      }

      return workspaceKeeper.getOrganization().get();
   }

   private void sendViewNotifications(final View view, final String event) {
      Set<String> userIds = ResourceUtils.usersAllowedRead(view);
      userIds.addAll(getWorkspaceManagers());
      view.setAuthorRights(viewFacade.getViewAuthorRights(view));
      sendNotificationsBatch(userIds.stream()
                                    .map(userId -> createEvent(new ObjectWithParent(createViewForUser(view, userId), getOrganization().getId(), getProject().getId()), event, userId))
                                    .collect(Collectors.toList()));
   }

   private View createViewForUser(final View view, final String userId) {
      return viewFacade.mapViewData(view.copy(), userId);
   }

   private void sendCollectionNotifications(final Collection collection, final String event) {
      Set<String> userIds = collectionFacade.getUsersIdsWithAccess(collection);
      sendNotificationsBatch(userIds.stream()
                                    .map(userId -> createEvent(new ObjectWithParent(createCollectionForUser(collection, userId), getOrganization().getId(), getProject().getId()), event, userId))
                                    .collect(Collectors.toList()));
   }

   private Collection createCollectionForUser(final Collection collection, final String userId) {
      return collectionFacade.mapCollectionData(collection.copy(), userId);
   }

   private Project getProject() {
      if (workspaceKeeper.getProject().isEmpty()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }

      return workspaceKeeper.getProject().get();
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
         sendNotificationsByUsers(comment, collectionFacade.getUsersIdsWithAccess(comment.getResourceId()), eventSuffix);
      } else if (comment.getResourceType() == ResourceType.DOCUMENT) {
         final List<Document> docs = documentFacade.getDocuments(Set.of(comment.getResourceId()));
         if (docs != null) {
            final Set<String> users = new HashSet<>();
            docs.stream().map(Document::getCollectionId).collect(Collectors.toSet()).forEach(collectionId -> {
               users.addAll(collectionFacade.getUsersIdsWithAccess(collectionId));
            });

            sendNotificationsByUsers(comment, users, eventSuffix);
         }
      } else if (comment.getResourceType() == ResourceType.LINK) {
         final LinkInstance linkInstance = linkInstanceDao.getLinkInstance(comment.getResourceId());
         final LinkType linkType = linkTypeDao.getLinkType(linkInstance.getLinkTypeId());
         final Set<String> users = getUserIdsForLinkType(linkType);

         sendNotificationsByUsers(comment, users, eventSuffix);
      } else if (comment.getResourceType() == ResourceType.VIEW) {
         final Set<String> users = ResourceUtils.usersAllowedRead(viewFacade.getViewById(comment.getResourceId()));
         users.addAll(getWorkspaceManagers());

         sendNotificationsByUsers(comment, users, eventSuffix);
      } else if (comment.getResourceType() == ResourceType.PROJECT) {
         final Set<String> users = ResourceUtils.usersAllowedRead(getProject());
         users.addAll(getWorkspaceManagers());

         sendNotificationsByUsers(comment, users, eventSuffix);
      } else if (comment.getResourceType() == ResourceType.ORGANIZATION) {
         final Set<String> users = ResourceUtils.usersAllowedRead(getOrganization());

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
            final Collection collection = collectionFacade.getCollection(document.getCollectionId());
            document.setData(constraintManager.decodeDataTypes(collection, document.getData()));
            document.setCommentsCount(documentFacade.getCommentsCount(document.getId()));
            Set<String> userIds = collectionFacade.getUsersIdsWithAccess(document.getCollectionId());
            sendNotificationsBatch(userIds.stream()
                                          .map(userId -> createEvent(createDocumentForUser(document, userId), eventSuffix, userId))
                                          .collect(Collectors.toList()));
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   private Document createDocumentForUser(final Document document, final String userId) {
      if (document.getCommentsCount() == null) {
         return documentFacade.mapDocumentData(new Document(document), userId);
      } else {
         return documentFacade.mapDocumentFavorite(new Document(document), userId);
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
            sendResourceNotificationByLinkType(linkTypeFacade.assignComputedParameters(createLinkType.getLinkType()), CREATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void updateLinkType(@Observes final UpdateLinkType updateLinkType) {
      if (isEnabled()) {
         try {
            sendResourceNotificationByLinkType(linkTypeFacade.assignComputedParameters(updateLinkType.getLinkType()), UPDATE_EVENT_SUFFIX);
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

   public void updateDefaultViewConfig(@Observes final UpdateDefaultViewConfig updateDefaultViewConfig) {
      if (isEnabled()) {
         try {
            ObjectWithParent object = new ObjectWithParent(updateDefaultViewConfig.getConfig(), getOrganization().getId(), getProject().getId());
            Set<String> userIds = Collections.singleton(authenticatedUser.getCurrentUserId());

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
            Set<String> userIds = permissionsChecker.getWorkspaceManagers();

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
            Set<String> userIds = permissionsChecker.getWorkspaceManagers();

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
      LinkType linkType = linkTypeFacade.getLinkType(linkTypeId);
      linkInstance.setData(constraintManager.decodeDataTypes(linkType, linkInstance.getData()));
      linkInstanceFacade.mapLinkInstanceData(linkInstance);
      Set<String> userIds = getUserIdsForLinkType(linkType);
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

   public void createAddOrUpdateUserNotification(@Observes final CreateOrUpdateUser createOrUpdateUser) {
      if (isEnabled()) {
         try {
            Organization organization = organizationDao.getOrganizationById(createOrUpdateUser.getOrganizationId());
            ObjectWithParent object = new ObjectWithParent(cleanUserFromUserEvent(createOrUpdateUser), organization.getId());
            Set<String> users = ResourceUtils.usersAllowedRead(organization);
            List<Event> events = users.stream().map(userId -> createEventForObjectWithParent(object, UPDATE_EVENT_SUFFIX, userId)).collect(Collectors.toList());
            sendNotificationsBatch(events);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void createChainNotification(@Observes final CreateDocumentsAndLinks createDocumentsAndLinks) {
      if (isEnabled()) {
         try {
            Set<String> users = getWorkspaceManagers();
            Project project = getProject();
            users.addAll(ResourceUtils.usersAllowedRead(project));
            ObjectWithParent object = new ObjectWithParent(createDocumentsAndLinks, getOrganization().getId(), project.getId());
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
            Set<String> users = getWorkspaceManagers();
            Project project = getProject();
            users.addAll(ResourceUtils.usersAllowedRead(project));
            ObjectWithParent object = new ObjectWithParent(setDocumentLinks, getOrganization().getId(), project.getId());
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
      Map<String, Set<String>> groups = new HashMap<>();
      groups.put(organizationId, Objects.requireNonNullElse(user.getGroups().get(organizationId), Collections.emptySet()));
      return new User(user.getId(), user.getName(), user.getEmail(), groups);
   }

   public void createRemoveUserNotification(@Observes final RemoveUser removeUser) {
      if (isEnabled()) {
         try {
            Organization organization = organizationDao.getOrganizationById(removeUser.getOrganizationId());
            ResourceId resourceId = new ResourceId(removeUser.getUser().getId(), organization.getId());
            String className = removeUser.getUser().getClass().getSimpleName();
            Set<String> users = ResourceUtils.usersAllowedRead(organization);
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

   private <T extends Resource> T filterUserRoles(final String userId, final T resource) {
      return this.mapResource(resource.copy(), userId);
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
