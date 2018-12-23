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
import io.lumeer.api.model.Role;
import io.lumeer.api.model.View;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.PusherClient;
import io.lumeer.engine.api.event.CreateDocument;
import io.lumeer.engine.api.event.CreateLinkInstance;
import io.lumeer.engine.api.event.CreateLinkType;
import io.lumeer.engine.api.event.CreateOrUpdatePayment;
import io.lumeer.engine.api.event.CreateOrUpdateUserNotification;
import io.lumeer.engine.api.event.CreateResource;
import io.lumeer.engine.api.event.DocumentEvent;
import io.lumeer.engine.api.event.RemoveDocument;
import io.lumeer.engine.api.event.RemoveLinkInstance;
import io.lumeer.engine.api.event.RemoveLinkType;
import io.lumeer.engine.api.event.RemoveResource;
import io.lumeer.engine.api.event.RemoveResourcePermissions;
import io.lumeer.engine.api.event.RemoveUserNotification;
import io.lumeer.engine.api.event.UpdateCompanyContact;
import io.lumeer.engine.api.event.UpdateDocument;
import io.lumeer.engine.api.event.UpdateLinkInstance;
import io.lumeer.engine.api.event.UpdateLinkType;
import io.lumeer.engine.api.event.UpdateResource;
import io.lumeer.engine.api.event.UpdateServiceLimits;

import org.marvec.pusher.data.Event;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@ApplicationScoped
public class PusherFacade {

   public static final String PRIVATE_CHANNEL_PREFIX = "private-";
   public static final String UPDATE_EVENT_SUFFIX = ":update";
   public static final String CREATE_EVENT_SUFFIX = ":create";
   public static final String REMOVE_EVENT_SUFFIX = ":remove";

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

   public void removeResourcePermissions(@Observes final RemoveResourcePermissions removeResourcePermissions) {
      if (isEnabled()) {
         try {
            if (removeResourcePermissions.getResource() instanceof View) {
               sendNotificationOfViewByUsers(removeResourcePermissions.getResource(),
                     removeResourcePermissions.getRemovedUsers(),
                     REMOVE_EVENT_SUFFIX);
            } else {
               sendNotificationByUsers(removeResourcePermissions.getResource(),
                     removeResourcePermissions.getRemovedUsers(),
                     REMOVE_EVENT_SUFFIX);
            }
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void createDocument(@Observes final CreateDocument createDocument) {
      documentNotification(createDocument.getDocument(), CREATE_EVENT_SUFFIX, createDocument);
   }

   public void updateDocument(@Observes final UpdateDocument updateDocument) {
      documentNotification(updateDocument.getDocument(), UPDATE_EVENT_SUFFIX, updateDocument);
   }

   public void removeDocument(@Observes final RemoveDocument removeDocument) {
      documentNotification(removeDocument.getDocument(), REMOVE_EVENT_SUFFIX, removeDocument);
   }

   private void documentNotification(final Document document, final String eventSuffix, final DocumentEvent documentEvent) {
      if (isEnabled()) {
         try {
            sendResourceNotificationByUsers(document,
                  collectionFacade.getUsersIdsWithAccess(document.getCollectionId()),
                  eventSuffix);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void createLinkInstance(@Observes final CreateLinkInstance createLinkInstance) {
      if (isEnabled()) {
         try {
            sendResourceNotificationByLinkType(createLinkInstance.getLinkInstance(),
                  createLinkInstance.getLinkInstance().getLinkTypeId(),
                  CREATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void updateLinkInstance(@Observes final UpdateLinkInstance updateLinkInstance) {
      if (isEnabled()) {
         try {
            sendResourceNotificationByLinkType(updateLinkInstance.getLinkInstance(),
                  updateLinkInstance.getLinkInstance().getLinkTypeId(),
                  UPDATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void removeLinkInstance(@Observes final RemoveLinkInstance removeLinkInstance) {
      if (isEnabled()) {
         try {
            sendResourceNotificationByLinkType(removeLinkInstance.getLinkInstance(),
                  removeLinkInstance.getLinkInstance().getLinkTypeId(),
                  REMOVE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void createLinkType(@Observes final CreateLinkType createLinkType) {
      if (isEnabled()) {
         try {
            sendResourceNotificationByLinkType(createLinkType.getLinkType(),
                  CREATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void updateLinkType(@Observes final UpdateLinkType updateLinkType) {
      if (isEnabled()) {
         try {
            sendResourceNotificationByLinkType(updateLinkType.getLinkType(),
                  UPDATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void removeLinkType(@Observes final RemoveLinkType removeLinkType) {
      if (isEnabled()) {
         try {
            sendResourceNotificationByLinkType(removeLinkType.getLinkType(),
                  REMOVE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void updateCompanyContact(@Observes final UpdateCompanyContact updateCompanyContact) {
      if (isEnabled()) {
         try {
            sendNotificationByUsers(
                  updateCompanyContact.getCompanyContact(),
                  organizationFacade.getOrganizationManagers(
                        organizationFacade.getOrganizationById(updateCompanyContact.getCompanyContact().getOrganizationId())),
                  UPDATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void updateServiceLimits(@Observes final UpdateServiceLimits updateServiceLimits) {
      if (isEnabled()) {
         try {
            sendNotificationByUsers(
                  new EntityWithOrganizationId(updateServiceLimits.getOrganization().getId(), updateServiceLimits.getServiceLimits()),
                  organizationFacade.getOrganizationManagers(updateServiceLimits.getOrganization()),
                  UPDATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void createOrUpdatePayment(@Observes final CreateOrUpdatePayment createOrUpdatePayment) {
      if (isEnabled()) {
         try {
            sendNotificationByUsers(
                  new EntityWithOrganizationId(createOrUpdatePayment.getOrganization().getId(), createOrUpdatePayment.getPayment()),
                  organizationFacade.getOrganizationManagers(createOrUpdatePayment.getOrganization()),
                  UPDATE_EVENT_SUFFIX);
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   public void createUserNotification(@Observes final CreateOrUpdateUserNotification createOrUpdateUserNotification) {
      if (isEnabled()) {
         try {
            sendNotification(
                  createOrUpdateUserNotification.getUserNotification().getUserId(),
                  CREATE_EVENT_SUFFIX,
                  createOrUpdateUserNotification.getUserNotification()
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
                  REMOVE_EVENT_SUFFIX,
                  new ResourceId(removeUserNotification.getUserNotification().getId())
            );
         } catch (Exception e) {
            log.log(Level.WARNING, "Unable to send push notification: ", e);
         }
      }
   }

   private void processResource(final Resource resource, final String event) {
      if (resource instanceof Organization
            || resource instanceof View) {
         sendResourceNotification(
               resource,
               REMOVE_EVENT_SUFFIX.equals(event) ?
                     new ResourceId(resource instanceof View ? resource.getCode() : resource.getId()) : resource,
               event);
      } else if (resource instanceof Project) {
         sendResourceNotification(
               resource,
               REMOVE_EVENT_SUFFIX.equals(event) ?
                     new ResourceId(resource.getId()) : new ResourceWithParent(resource, workspaceKeeper.getOrganization().get().getId()),
               event);
      } else if (resource instanceof Collection) {
         sendResourceNotificationByUsers(
               resource,
               collectionFacade.getUsersIdsWithAccess((Collection) resource),
               event);
      }
   }

   private void sendResourceNotification(final Resource resource, final Object message, final String event) {
      sendNotificationsBatch(resource.getPermissions().getUserPermissions().stream()
                                     .filter(permission -> authenticatedUser.getCurrentUserId() != null
                                           && !authenticatedUser.getCurrentUserId().equals(permission.getId())
                                           && permission.getRoles().size() > 0)
                                     .map(permission ->
                                           new Event(
                                                 PRIVATE_CHANNEL_PREFIX + permission.getId(),
                                                 resource.getClass().getSimpleName() + event,
                                                 message instanceof Resource ? filterUserRoles(permission.getId(), (Resource) message) : message))
                                     .collect(Collectors.toList()));
   }

   private void sendResourceNotificationByUsers(final Resource resource, final Set<String> userIds, final String event) {
      sendNotificationsBatch(userIds.stream()
                                    .filter(userId -> authenticatedUser.getCurrentUserId() != null && !authenticatedUser.getCurrentUserId().equals(userId))
                                    .map(userId ->
                                          new Event(
                                                PRIVATE_CHANNEL_PREFIX + userId,
                                                resource.getClass().getSimpleName() + event,
                                                filterUserRoles(userId, resource)))
                                    .collect(Collectors.toList()));
   }

   private void sendNotificationOfViewByUsers(final Resource resource, final Set<String> userIds, final String event) {
      sendNotificationsBatch(userIds.stream()
                                    .filter(userId -> authenticatedUser.getCurrentUserId() != null && !authenticatedUser.getCurrentUserId().equals(userId))
                                    .map(userId ->
                                          new Event(
                                                PRIVATE_CHANNEL_PREFIX + userId,
                                                resource.getClass().getSimpleName() + event,
                                                resource instanceof View ? new ResourceId(resource.getCode()) : resource))
                                    .collect(Collectors.toList()));
   }

   private void sendResourceNotificationByUsers(final Document resource, final Set<String> userIds, final String event) {
      sendNotificationByUsers(resource, userIds, event);
   }

   private void sendNotificationByUsers(final Object resource, final Set<String> userIds, final String event) {
      sendNotificationsBatch(userIds.stream()
                                    .filter(userId -> authenticatedUser.getCurrentUserId() != null && !authenticatedUser.getCurrentUserId().equals(userId))
                                    .map(userId ->
                                          new Event(
                                                PRIVATE_CHANNEL_PREFIX + userId,
                                                (resource instanceof EntityWithOrganizationId ? ((EntityWithOrganizationId) resource).entity.getClass().getSimpleName() : resource.getClass().getSimpleName()) + event,
                                                resource))
                                    .collect(Collectors.toList()));
   }

   private void sendResourceNotificationByLinkType(final LinkInstance linkInstance, final String linkTypeId, final String event) {
      sendNotificationsBatch(
            linkTypeFacade
                  .getLinkTypeCollections(linkTypeId)
                  .stream()
                  .map(collectionFacade::getUsersIdsWithAccess) // now we have several sets of user ids
                  .flatMap(userIds -> Stream.of(userIds.toArray())) // map them to a single set to remove duplicates
                  .collect(Collectors.toSet())
                  .stream()
                  .filter(userId -> authenticatedUser.getCurrentUserId() != null && !authenticatedUser.getCurrentUserId().equals(userId))
                  .map(userId ->
                        new Event(
                              PRIVATE_CHANNEL_PREFIX + userId,
                              LinkInstance.class.getSimpleName() + event,
                              linkInstance))
                  .collect(Collectors.toList()));
   }

   private void sendResourceNotificationByLinkType(final LinkType linkType, final String event) {
      sendNotificationsBatch(
            linkType
                  .getCollectionIds()
                  .stream()
                  .map(collectionFacade::getUsersIdsWithAccess) // now we have several sets of user ids
                  .flatMap(userIds -> Stream.of(userIds.toArray())) // map them to a single set to remove duplicates
                  .collect(Collectors.toSet())
                  .stream()
                  .filter(userId -> authenticatedUser.getCurrentUserId() != null && !authenticatedUser.getCurrentUserId().equals(userId))
                  .map(userId ->
                        new Event(
                              PRIVATE_CHANNEL_PREFIX + userId,
                              LinkType.class.getSimpleName() + event,
                              linkType))
                  .collect(Collectors.toList()));
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

   public static final class ResourceWithParent {
      private final Resource resource;
      private final String parentId;

      public ResourceWithParent(final Resource resource, final String parentId) {
         this.resource = resource;
         this.parentId = parentId;
      }

      public Resource getResource() {
         return resource;
      }

      public String getParentId() {
         return parentId;
      }
   }

   public static final class EntityWithOrganizationId {
      private final String organizationId;
      private final Object entity;

      public EntityWithOrganizationId(final String organizationId, final Object entity) {
         this.organizationId = organizationId;
         this.entity = entity;
      }

      public String getOrganizationId() {
         return organizationId;
      }

      public Object getEntity() {
         return entity;
      }
   }
}
