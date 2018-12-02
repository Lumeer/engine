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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Pagination;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.util.CodeGenerator;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.query.DatabaseQuery;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class CollectionFacade extends AbstractFacade {

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private DataDao dataDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private FavoriteItemDao favoriteItemDao;

   @Inject
   private ViewDao viewDao;

   public Collection createCollection(Collection collection) {
      checkProjectWriteRole();
      long collectionsCount = collectionDao.getCollectionsCount();
      permissionsChecker.checkCreationLimits(collection, collectionsCount);

      Collection storedCollection = createCollectionMetadata(collection);
      dataDao.createDataRepository(storedCollection.getId());

      return storedCollection;
   }

   public Collection updateCollection(String collectionId, Collection collection) {
      Collection storedCollection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(storedCollection, Role.MANAGE);

      keepUnmodifiableFields(collection, storedCollection);
      Collection updatedCollection = collectionDao.updateCollection(storedCollection.getId(), collection);
      return mapResource(updatedCollection);
   }

   private void keepUnmodifiableFields(Collection collection, Collection storedCollection) {
      keepStoredPermissions(collection, storedCollection.getPermissions());

      collection.setAttributes(storedCollection.getAttributes());
      collection.setDocumentsCount(storedCollection.getDocumentsCount());
      collection.setLastTimeUsed(storedCollection.getLastTimeUsed());
   }

   public void deleteCollection(String collectionId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.MANAGE);

      collectionDao.deleteCollection(collectionId);

      deleteCollectionBasedData(collectionId);
   }

   private void deleteCollectionBasedData(final String collectionId) {
      documentDao.deleteDocuments(collectionId);
      dataDao.deleteDataRepository(collectionId);

      List<LinkType> linkTypes = linkTypeDao.getLinkTypesByCollectionId(collectionId);
      if (!linkTypes.isEmpty()) {
         linkTypeDao.deleteLinkTypesByCollectionId(collectionId);
         linkInstanceDao.deleteLinkInstancesByLinkTypesIds(linkTypes.stream().map(LinkType::getId).collect(Collectors.toSet()));
      }

      favoriteItemDao.removeFavoriteCollectionFromUsers(getCurrentProject().getId(), collectionId);
      favoriteItemDao.removeFavoriteDocumentsByCollectionFromUsers(getCurrentProject().getId(), collectionId);
   }

   public Collection getCollection(String collectionId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRoleWithView(collection, Role.READ, Role.READ);

      return mapResource(collection);
   }

   public List<Collection> getCollections(Pagination pagination) {
      DatabaseQuery searchQuery = createSimplePaginationQuery(pagination);

      return collectionDao.getCollections(searchQuery).stream()
                          .map(this::mapResource)
                          .filter(collection -> permissionsChecker.hasRoleWithView(collection, Role.READ, Role.READ))
                          .collect(Collectors.toList());
   }

   public void addFavoriteCollection(String collectionId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.READ);

      String projectId = getCurrentProject().getId();
      String userId = getCurrentUser().getId();
      favoriteItemDao.addFavoriteCollection(userId, projectId, collectionId);
   }

   public void removeFavoriteCollection(String collectionId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.READ);

      String userId = getCurrentUser().getId();
      favoriteItemDao.removeFavoriteCollection(userId, collectionId);
   }

   public boolean isFavorite(String collectionId) {
      return getFavoriteCollectionsIds().contains(collectionId);
   }

   public Set<String> getFavoriteCollectionsIds() {
      String projectId = getCurrentProject().getId();
      String userId = getCurrentUser().getId();

      return favoriteItemDao.getFavoriteCollectionIds(userId, projectId);
   }

   public long getDocumentsCountInAllCollections() {
      final LongAdder la = new LongAdder();
      collectionDao.getAllCollectionIds().forEach(id -> {
         final Collection collection = collectionDao.getCollectionById(id);
         if (permissionsChecker.hasRoleWithView(collection, Role.READ, Role.READ)) {
            la.add(getCollection(id).getDocumentsCount());
         }
      });

      return la.longValue();
   }

   public java.util.Collection<Attribute> createCollectionAttributes(String collectionId, java.util.Collection<Attribute> attributes) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.MANAGE);

      for (Attribute attribute : attributes) {
         final Integer freeNum = getFreeAttributeNum(collection);
         attribute.setId(Collection.ATTRIBUTE_PREFIX + freeNum);
         attribute.setUsageCount(0);
         collection.createAttribute(attribute);
         collection.setLastAttributeNum(freeNum);
      }

      collectionDao.updateCollection(collection.getId(), collection);

      return attributes;
   }

   private Integer getFreeAttributeNum(final Collection collection) {
      final AtomicInteger last = new AtomicInteger(Math.max(1, collection.getLastAttributeNum()));
      while (collection.getAttributes().stream().anyMatch(attribute -> attribute.getId().equals(Collection.ATTRIBUTE_PREFIX + last.get()))) {
         last.incrementAndGet();
      }

      return last.get();
   }

   public Attribute updateCollectionAttribute(String collectionId, String attributeId, Attribute attribute) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.MANAGE);

      collection.updateAttribute(attributeId, attribute);

      collectionDao.updateCollection(collection.getId(), collection);

      return attribute;
   }

   public void deleteCollectionAttribute(String collectionId, String attributeId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.MANAGE);

      collection.deleteAttribute(attributeId);
      if (collection.getDefaultAttributeId() != null && collection.getDefaultAttributeId().equals(attributeId)) {
         collection.setDefaultAttributeId(null);
      }
      collectionDao.updateCollection(collection.getId(), collection);
   }

   public void setDefaultAttribute(String collectionId, String attributeId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.MANAGE);

      boolean containsAttribute = collection.getAttributes().stream()
                                            .anyMatch(attribute -> attribute.getId().equals(attributeId));
      if (containsAttribute) {
         collection.setDefaultAttributeId(attributeId);
         collectionDao.updateCollection(collection.getId(), collection);
      }
   }

   public Permissions getCollectionPermissions(final String collectionId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.MANAGE);

      return collection.getPermissions();
   }

   public Set<Permission> updateUserPermissions(final String collectionId, final Permission... userPermissions) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.MANAGE);
      permissionsChecker.invalidateCache(collection);

      collection.getPermissions().updateUserPermissions(userPermissions);
      Collection updatedCollection = collectionDao.updateCollection(collection.getId(), collection);

      return updatedCollection.getPermissions().getUserPermissions();
   }

   public void removeUserPermission(final String collectionId, final String userId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.MANAGE);
      permissionsChecker.invalidateCache(collection);

      collection.getPermissions().removeUserPermission(userId);
      collectionDao.updateCollection(collection.getId(), collection);
   }

   public Set<Permission> updateGroupPermissions(final String collectionId, final Permission... groupPermissions) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.MANAGE);
      permissionsChecker.invalidateCache(collection);

      collection.getPermissions().updateGroupPermissions(groupPermissions);
      Collection updatedCollection = collectionDao.updateCollection(collection.getId(), collection);

      return updatedCollection.getPermissions().getGroupPermissions();
   }

   public void removeGroupPermission(final String collectionId, final String groupId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.MANAGE);
      permissionsChecker.invalidateCache(collection);

      collection.getPermissions().removeGroupPermission(groupId);
      collectionDao.updateCollection(collection.getId(), collection);
   }

   public Set<String> getUsersIdsWithAccess(final String collectionId) {
      return getUsersIdsWithAccess(collectionDao.getCollectionById(collectionId));
   }

   public Set<String> getUsersIdsWithAccess(final Collection collection) {
      final Set<String> result = new HashSet<>();

      result.addAll(collection.getPermissions().getUserPermissions().stream().map(Permission::getId).collect(Collectors.toSet()));

      viewDao.getViewsByLinkTypeIds(
            linkTypeDao.getLinkTypesByCollectionId(collection.getId())
                       .stream()
                       .map(LinkType::getId)
                       .collect(Collectors.toList()))
             .stream()
             .map(Resource::getPermissions)
             .map(Permissions::getUserPermissions)
             .forEach(permissions -> {
                result.addAll(permissions.stream().map(Permission::getId).collect(Collectors.toList()));
             });

      viewDao.getViewsByCollectionId(collection.getId()).stream()
             .map(Resource::getPermissions)
             .map(Permissions::getUserPermissions)
             .forEach(permissions -> {
                result.addAll(permissions.stream().map(Permission::getId).collect(Collectors.toList()));
             });

      // TODO: Handle user groups as well

      return result;
   }

   private void checkProjectWriteRole() {
      if (!workspaceKeeper.getProject().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }

      Project project = workspaceKeeper.getProject().get();
      permissionsChecker.checkRole(project, Role.WRITE);
   }

   private Project getCurrentProject() {
      if (!workspaceKeeper.getProject().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return workspaceKeeper.getProject().get();
   }

   private User getCurrentUser() {
      return authenticatedUser.getCurrentUser();
   }

   private Collection createCollectionMetadata(Collection collection) {
      if (collection.getCode() == null || collection.getCode().isEmpty()) {
         collection.setCode(generateCollectionCode(collection.getName()));
      }

      collection.setLastAttributeNum(0);

      Permission defaultUserPermission = Permission.buildWithRoles(authenticatedUser.getCurrentUserId(), Collection.ROLES);
      collection.getPermissions().updateUserPermissions(defaultUserPermission);

      return collectionDao.createCollection(collection);
   }

   private String generateCollectionCode(String collectionName) {
      Set<String> existingCodes = collectionDao.getAllCollectionCodes();
      return CodeGenerator.generate(existingCodes, collectionName);
   }

}
