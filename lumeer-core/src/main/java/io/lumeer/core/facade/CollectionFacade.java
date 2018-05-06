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
import io.lumeer.core.AuthenticatedUserGroups;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.core.util.CodeGenerator;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.query.SearchQuery;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
   private AuthenticatedUserGroups authenticatedUserGroups;

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

      SearchQuery queryLinkTypes = createQueryForLinkTypes(collectionId);
      List<LinkType> linkTypes = linkTypeDao.getLinkTypes(queryLinkTypes);
      if (!linkTypes.isEmpty()) {
         linkTypeDao.deleteLinkTypes(queryLinkTypes);
         linkInstanceDao.deleteLinkInstances(createQueryForLinkInstances(linkTypes));
      }

      favoriteItemDao.removeFavoriteCollection(collectionId);
      favoriteItemDao.removeFavoriteDocumentsByCollection(collectionId);
   }

   public Collection getCollection(String collectionId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.READ);

      return mapResource(collection);
   }

   public List<Collection> getCollections(Pagination pagination) {
      SearchQuery searchQuery = createPaginationQuery(pagination);

      return collectionDao.getCollections(searchQuery).stream()
                          .map(this::mapResource)
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

   public Set<String> getCollectionNames() {
      return collectionDao.getAllCollectionNames();
   }

   public Set<Attribute> createCollectionAttributes(String collectionId, java.util.Collection<? extends Attribute> attributes) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.MANAGE);

      int i = 0;
      for (Attribute attribute : attributes) {
         attribute.setId(Collection.ATTRIBUTE_PREFIX + (collection.getLastAttributeNum() + i + 1));
         attribute.setUsageCount(0);
         collection.createAttribute(attribute);
         i++;
      }

      collection.setLastAttributeNum(collection.getLastAttributeNum() + attributes.size());

      collectionDao.updateCollection(collection.getId(), collection);

      return new HashSet<>(attributes);
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

      Optional<Attribute> toDelete = collection.getAttributes().stream().filter(attribute -> attribute.getId().equals(attributeId)).findFirst();
      if (toDelete.isPresent()) {
         collection.deleteAttribute(toDelete.get().getName());
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

      collection.getPermissions().updateUserPermissions(userPermissions);
      Collection updatedCollection = collectionDao.updateCollection(collection.getId(), collection);

      return updatedCollection.getPermissions().getUserPermissions();
   }

   public void removeUserPermission(final String collectionId, final String userId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.MANAGE);

      collection.getPermissions().removeUserPermission(userId);
      collectionDao.updateCollection(collection.getId(), collection);
   }

   public Set<Permission> updateGroupPermissions(final String collectionId, final Permission... groupPermissions) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.MANAGE);

      collection.getPermissions().updateGroupPermissions(groupPermissions);
      Collection updatedCollection = collectionDao.updateCollection(collection.getId(), collection);

      return updatedCollection.getPermissions().getGroupPermissions();
   }

   public void removeGroupPermission(final String collectionId, final String groupId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, Role.MANAGE);

      collection.getPermissions().removeGroupPermission(groupId);
      collectionDao.updateCollection(collection.getId(), collection);
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

      Permission defaultUserPermission = new SimplePermission(authenticatedUser.getCurrentUserId(), Collection.ROLES);
      collection.getPermissions().updateUserPermissions(defaultUserPermission);

      return collectionDao.createCollection(collection);
   }

   private String generateCollectionCode(String collectionName) {
      Set<String> existingCodes = collectionDao.getAllCollectionCodes();
      return CodeGenerator.generate(existingCodes, collectionName);
   }

   private SearchQuery createQueryForLinkTypes(String collectionId) {
      String user = authenticatedUser.getCurrentUserId();
      Set<String> groups = authenticatedUserGroups.getCurrentUserGroups();

      return SearchQuery.createBuilder(user).groups(groups)
                        .collectionIds(Collections.singleton(collectionId))
                        .build();
   }

   private SearchQuery createQueryForLinkInstances(List<LinkType> linkTypes) {
      String user = authenticatedUser.getCurrentUserId();
      Set<String> groups = authenticatedUserGroups.getCurrentUserGroups();

      return SearchQuery.createBuilder(user).groups(groups)
                        .linkTypeIds(linkTypes.stream().map(LinkType::getId).collect(Collectors.toSet()))
                        .build();
   }

}
