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
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
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
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.marvec.pusher.Pusher;
import org.marvec.pusher.data.Event;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

   private Pusher pusher = null;

   private ObjectMapper mapper;

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
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private OrganizationFacade organizationFacade;

   @PostConstruct
   public void init() {
      PUSHER_APP_ID = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.PUSHER_APP_ID)).orElse("");
      PUSHER_KEY = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.PUSHER_KEY)).orElse("");
      PUSHER_SECRET = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.PUSHER_SECRET)).orElse("");
      PUSHER_CLUSTER = Optional.ofNullable(defaultConfigurationProducer.get(DefaultConfigurationProducer.PUSHER_CLUSTER)).orElse("");

      if (PUSHER_SECRET != null && !"".equals(PUSHER_SECRET)) {
         pusher = new Pusher(PUSHER_APP_ID, PUSHER_KEY, PUSHER_SECRET);
         pusher.setCluster(PUSHER_CLUSTER);
         pusher.setEncrypted(true);

         mapper = new ObjectMapper();
         AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
         AnnotationIntrospector secondary = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
         AnnotationIntrospector pair = AnnotationIntrospector.pair(primary, secondary);
         mapper.setAnnotationIntrospector(pair);

         pusher.setDataMarshaller(o -> {
            StringWriter sw = new StringWriter();
            try {
               mapper.writeValue(sw, o);
               return sw.toString();
            } catch (IOException e) {
               return null;
            }
         });
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
            checkRemovedPermissions(updateResource);
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

   private void checkRemovedPermissions(final UpdateResource updateResource) {
      Set<String> removedUsers = getRemovedUsersPermissions(updateResource.getOriginalResource(), updateResource.getResource());
      if (removedUsers.isEmpty()) {
         return;
      }

      if (updateResource.getResource() instanceof Project) {
         removedUsers.removeAll(getOrganizationManagers());
      } else if (updateResource.getResource() instanceof Collection || updateResource.getResource() instanceof View) {
         removedUsers.removeAll(getWorkspaceManagers());
      }
      removedUsers.remove(authenticatedUser.getCurrentUserId());

      sendNotificationsByUsers(updateResource.getResource(), removedUsers, REMOVE_EVENT_SUFFIX);
   }

   private Set<String> getRemovedUsersPermissions(final Resource originalResource, final Resource updatedResource) {
      if (originalResource == null || updatedResource == null) {
         return Collections.emptySet();
      }
      Set<Permission> actualPermissions = originalResource.getPermissions().getUserPermissions();
      Set<Permission> newPermissions = updatedResource.getPermissions().getUserPermissions();
      // TODO groups

      Set<String> actualUsers = actualPermissions.stream().map(Permission::getId).collect(Collectors.toSet());
      actualUsers.removeAll(newPermissions.stream().filter(permission -> permission.getRoles().size() > 0).map(Permission::getId).collect(Collectors.toSet()));
      return actualUsers;
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
      final Set<String> userIdsExceptCurrent = new HashSet<>(userIds);
      userIdsExceptCurrent.remove(authenticatedUser.getCurrentUserId());

      sendNotificationsBatch(userIdsExceptCurrent.stream()
                                                 .map(userId -> createEvent(object, event, userId))
                                                 .collect(Collectors.toList()));
   }

   private Event createEvent(final Object object, final String event, final String userId) {
      if (object instanceof Document) {
         return createEventForDocument((Document) object, event, userId);
      } else if (object instanceof LinkType) {
         return createEventForLinkType((LinkType) object, event, userId);
      } else if (object instanceof LinkInstance) {
         return createEventForLinkInstance((LinkInstance) object, event, userId);
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

   private Event createEventForDocument(final Document document, final String event, final String userId) {
      Object object = REMOVE_EVENT_SUFFIX.equals(event) ? new ResourceId(document.getId()) : document;
      return createEventForObject(object, event, userId);
   }

   private Event createEventForLinkInstance(final LinkInstance linkInstance, final String event, final String userId) {
      Object object = REMOVE_EVENT_SUFFIX.equals(event) ? new ResourceId(linkInstance.getId()) : linkInstance;
      return createEventForObject(object, event, userId);
   }

   private Event createEventForLinkType(final LinkType linkType, final String event, final String userId) {
      Object object = REMOVE_EVENT_SUFFIX.equals(event) ? new ResourceId(linkType.getId()) : linkType;
      return createEventForObject(object, event, userId);
   }

   private Event createEventForResource(final Resource resource, final String event, final String userId) {
      Object object = REMOVE_EVENT_SUFFIX.equals(event) ? getResourceId(resource) : filterUserRoles(userId, resource);
      return createEventForObject(object, event, userId);
   }

   private Event createEventForObject(final Object object, final String event, final String userId) {
      return new Event(eventChannel(userId), object.getClass().getSimpleName() + event, object);
   }

   private Event createEventForNestedResource(final ObjectWithParent objectWithParent, final String event, final String userId) {
      Resource resource = (Resource) objectWithParent.object;
      if (REMOVE_EVENT_SUFFIX.equals(event)) {
         return createEventForResource(resource, event, userId);
      }

      Resource filteredResource = filterUserRoles(userId, resource);
      ObjectWithParent newObjectWithParent = new ObjectWithParent(filteredResource, objectWithParent.organizationId, objectWithParent.projectId);
      return createEventForObjectWithParent(newObjectWithParent, event, userId);
   }

   private Event createEventForObjectWithParent(final ObjectWithParent objectWithParent, final String event, final String userId) {
      return new Event(eventChannel(userId), objectWithParent.object.getClass().getSimpleName() + event, objectWithParent);
   }

   private String eventChannel(String userId) {
      return PRIVATE_CHANNEL_PREFIX + userId;
   }

   private ResourceId getResourceId(Resource resource) {
      if (resource instanceof View) {
         return new ResourceId(resource.getCode());
      }
      return new ResourceId(resource.getId());
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

      sendViewQueryNotifications(view);
   }

   private void sendViewQueryNotifications(final View view) {
      Set<String> userIds = ResourceUtils.usersAllowedRead(view);
      userIds.removeAll(getWorkspaceManagers());

      List<Event> notifications = new ArrayList<>();
      List<LinkType> linkTypes = linkTypeDao.getLinkTypesByIds(view.getQuery().getLinkTypeIds());

      Set<String> collectionIds = view.getQuery().getCollectionIds();
      collectionIds.addAll(linkTypes.stream().map(LinkType::getCollectionIds)
                                    .flatMap(java.util.Collection::stream).collect(Collectors.toSet()));

      List<Collection> collections = collectionDao.getCollectionsByIds(collectionIds);
      for (String user : userIds) {
         notifications.addAll(linkTypes.stream()
                                       .map(linkType -> createEventForLinkType(linkType, UPDATE_EVENT_SUFFIX, user)).collect(Collectors.toList()));

         notifications.addAll(collections.stream()
                                         .map(collection -> createEventForResource(collection, UPDATE_EVENT_SUFFIX, user)).collect(Collectors.toList()));
      }

      sendNotificationsBatch(notifications);
   }

   private void sendCollectionNotifications(final Collection collection, final String event) {
      Set<String> userIds = collectionFacade.getUsersIdsWithAccess(collection);
      ObjectWithParent object = new ObjectWithParent(collection, getOrganization().getId(), getProject().getId());
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
            .map(collectionFacade::getUsersIdsWithAccess) // now we have several sets of user ids
            .collect(Collectors.toSet());
      return intersection(userIdsMaps);
   }

   private void sendResourceNotificationByLinkType(final LinkType linkType, final String event) {
      Set<String> userIds = getUserIdsForLinkType(linkType);
      sendNotificationsByUsers(linkType, userIds, event);
   }

   private void sendNotification(final String userId, final String event, final Object message) {
      if (isEnabled() && authenticatedUser.getCurrentUserId() != null && !authenticatedUser.getCurrentUserId().equals(userId)) {
         pusher.trigger(PRIVATE_CHANNEL_PREFIX + userId, message.getClass().getSimpleName() + event, message);
      }
   }

   private void sendNotificationsBatch(List<Event> notifications) {
      if (isEnabled() && notifications != null && notifications.size() > 0) {
         pusher.trigger(notifications);
      }
   }

   private boolean isEnabled() {
      return pusher != null;
   }

   private <T extends Resource> T filterUserRoles(final String userId, final T resource) {
      final T copy = resource.copy();

      Set<Role> roles = permissionsChecker.getActualRoles(copy, userId);
      Permission permission = Permission.buildWithRoles(userId, roles);

      copy.getPermissions().clear();
      copy.getPermissions().updateUserPermissions(permission);

      return copy;
   }

   public static final class ResourceId {
      private final String id;

      public ResourceId(final String id) {
         this.id = id;
      }

      public String getId() {
         return id;
      }
   }

   public static Set<String> intersection(Set<Set<String>> sets) {
      return sets.stream().map(HashSet::new).reduce(
            (s1, s2) -> {
               s1.retainAll(s2);
               return s1;
            }).orElse(new HashSet<>());
   }

   public static final class ObjectWithParent {
      private final Object object;
      private final String organizationId;
      private final String projectId;

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
   }

}
