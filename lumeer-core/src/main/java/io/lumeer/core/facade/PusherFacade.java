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
import io.lumeer.engine.api.event.CreateDocument;
import io.lumeer.engine.api.event.CreateLinkInstance;
import io.lumeer.engine.api.event.CreateLinkType;
import io.lumeer.engine.api.event.CreateOrUpdatePayment;
import io.lumeer.engine.api.event.CreateResource;
import io.lumeer.engine.api.event.RemoveDocument;
import io.lumeer.engine.api.event.RemoveLinkInstance;
import io.lumeer.engine.api.event.RemoveLinkType;
import io.lumeer.engine.api.event.RemoveResource;
import io.lumeer.engine.api.event.RemoveResourcePermissions;
import io.lumeer.engine.api.event.UpdateCompanyContact;
import io.lumeer.engine.api.event.UpdateDocument;
import io.lumeer.engine.api.event.UpdateLinkInstance;
import io.lumeer.engine.api.event.UpdateLinkType;
import io.lumeer.engine.api.event.UpdateResource;
import io.lumeer.engine.api.event.UpdateServiceLimits;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.marvec.pusher.Pusher;
import org.marvec.pusher.data.Event;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

   public static final String PRIVATE_CHANNEL_PREFIX = "private-";
   public static final String UPDATE_EVENT_SUFFIX = ":update";
   public static final String CREATE_EVENT_SUFFIX = ":create";
   public static final String REMOVE_EVENT_SUFFIX = ":remove";

   private String PUSHER_APP_ID;
   private String PUSHER_KEY;
   private String PUSHER_SECRET;
   private String PUSHER_CLUSTER;

   private Pusher pusher = null;

   private ObjectMapper mapper;

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
         processResource(createResource.getResource(), CREATE_EVENT_SUFFIX);
      }
   }

   public void updateResource(@Observes final UpdateResource updateResource) {
      if (isEnabled()) {
         processResource(updateResource.getResource(), UPDATE_EVENT_SUFFIX);
      }
   }

   public void removeResource(@Observes final RemoveResource removeResource) {
      if (isEnabled()) {
         processResource(removeResource.getResource(), REMOVE_EVENT_SUFFIX);
      }
   }

   public void removeResourcePermissions(@Observes final RemoveResourcePermissions removeResourcePermissions) {
      if (isEnabled()) {
         sendNotificationByUsers(removeResourcePermissions.getResource(),
               removeResourcePermissions.getRemovedUsers(),
               REMOVE_EVENT_SUFFIX);
      }
   }

   public void createDocument(@Observes final CreateDocument createDocument) {
      if (isEnabled()) {
         sendResourceNotificationByUsers(createDocument.getDocument(),
               collectionFacade.getUsersIdsWithAccess(createDocument.getDocument().getCollectionId()),
               CREATE_EVENT_SUFFIX);
      }
   }

   public void updateDocument(@Observes final UpdateDocument updateDocument) {
      if (isEnabled()) {
         sendResourceNotificationByUsers(updateDocument.getDocument(),
               collectionFacade.getUsersIdsWithAccess(updateDocument.getDocument().getCollectionId()),
               UPDATE_EVENT_SUFFIX);
      }
   }

   public void removeDocument(@Observes final RemoveDocument removeDocument) {
      if (isEnabled()) {
         sendResourceNotificationByUsers(removeDocument.getDocument(),
               collectionFacade.getUsersIdsWithAccess(removeDocument.getDocument().getCollectionId()),
               REMOVE_EVENT_SUFFIX);
      }
   }

   public void createLinkInstance(@Observes final CreateLinkInstance createLinkInstance) {
      if (isEnabled()) {
         sendResourceNotificationByLinkType(createLinkInstance.getLinkInstance(),
               createLinkInstance.getLinkInstance().getLinkTypeId(),
               CREATE_EVENT_SUFFIX);
      }
   }

   public void updateLinkInstance(@Observes final UpdateLinkInstance updateLinkInstance) {
      if (isEnabled()) {
         sendResourceNotificationByLinkType(updateLinkInstance.getLinkInstance(),
               updateLinkInstance.getLinkInstance().getLinkTypeId(),
               UPDATE_EVENT_SUFFIX);
      }
   }

   public void removeLinkInstance(@Observes final RemoveLinkInstance removeLinkInstance) {
      if (isEnabled()) {
         sendResourceNotificationByLinkType(removeLinkInstance.getLinkInstance(),
               removeLinkInstance.getLinkInstance().getLinkTypeId(),
               REMOVE_EVENT_SUFFIX);
      }
   }

   public void createLinkType(@Observes final CreateLinkType createLinkType) {
      if (isEnabled()) {
         sendResourceNotificationByLinkType(createLinkType.getLinkType(),
               CREATE_EVENT_SUFFIX);
      }
   }

   public void updateLinkType(@Observes final UpdateLinkType updateLinkType) {
      if (isEnabled()) {
         sendResourceNotificationByLinkType(updateLinkType.getLinkType(),
               UPDATE_EVENT_SUFFIX);
      }
   }

   public void removeLinkType(@Observes final RemoveLinkType removeLinkType) {
      if (isEnabled()) {
         sendResourceNotificationByLinkType(removeLinkType.getLinkType(),
               REMOVE_EVENT_SUFFIX);
      }
   }

   public void updateCompanyContact(@Observes final UpdateCompanyContact updateCompanyContact) {
      if (isEnabled()) {
         sendNotificationByUsers(
               updateCompanyContact.getCompanyContact(),
               organizationFacade.getOrganizationManagers(
                     organizationFacade.getOrganizationById(updateCompanyContact.getCompanyContact().getOrganizationId())),
               UPDATE_EVENT_SUFFIX);
      }
   }

   public void updateServiceLimits(@Observes final UpdateServiceLimits updateServiceLimits) {
      if (isEnabled()) {
         sendNotificationByUsers(
               new EntityWithOrganizationId(updateServiceLimits.getOrganization().getId(), updateServiceLimits.getServiceLimits()),
               organizationFacade.getOrganizationManagers(updateServiceLimits.getOrganization()),
               UPDATE_EVENT_SUFFIX);
      }
   }

   public void createOrUpdatePayment(@Observes final CreateOrUpdatePayment createOrUpdatePayment) {
      if (isEnabled()) {
         sendNotificationByUsers(
               new EntityWithOrganizationId(createOrUpdatePayment.getOrganization().getId(), createOrUpdatePayment.getPayment()),
               organizationFacade.getOrganizationManagers(createOrUpdatePayment.getOrganization()),
               UPDATE_EVENT_SUFFIX);
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

   private void sendResourceNotificationByUsers(final Document resource, final Set<String> userIds, final String event) {
      sendNotificationByUsers(resource, userIds, event);
   }

   private void sendNotificationByUsers(final Object resource, final Set<String> userIds, final String event) {
      sendNotificationsBatch(userIds.stream()
                                    .filter(userId -> authenticatedUser.getCurrentUserId() != null && !authenticatedUser.getCurrentUserId().equals(userId))
                                    .map(userId ->
                                          new Event(
                                                PRIVATE_CHANNEL_PREFIX + userId,
                                                resource.getClass().getSimpleName() + event,
                                                resource))
                                    .collect(Collectors.toList()));
   }

   private void sendResourceNotificationByLinkType(final LinkInstance linkInstance, final String linkTypeId, final String event) {
      linkTypeFacade.getLinkTypeCollections(linkTypeId).stream().map(collectionFacade::getUsersIdsWithAccess)
                    .filter(userId -> authenticatedUser.getCurrentUserId() != null && !authenticatedUser.getCurrentUserId().equals(userId))
                    .forEach(userIds ->
                          sendNotificationsBatch(userIds.stream().map(userId ->
                                new Event(
                                      PRIVATE_CHANNEL_PREFIX + userId,
                                      LinkInstance.class.getSimpleName() + event,
                                      linkInstance))
                                                        .collect(Collectors.toList()))
                    );
   }

   private void sendResourceNotificationByLinkType(final LinkType linkType, final String event) {
      linkType.getCollectionIds().stream().map(collectionFacade::getUsersIdsWithAccess)
              .filter(userId -> authenticatedUser.getCurrentUserId() != null && !authenticatedUser.getCurrentUserId().equals(userId))
              .forEach(userIds ->
                    sendNotificationsBatch(userIds.stream().map(userId ->
                          new Event(
                                PRIVATE_CHANNEL_PREFIX + userId,
                                LinkInstance.class.getSimpleName() + event,
                                linkType))
                                                  .collect(Collectors.toList()))
              );
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
