/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.core.facade;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Pagination;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.core.model.SimplePermission;
import io.lumeer.core.util.CodeGenerator;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.query.SearchQuery;

import java.util.List;
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

   public Collection createCollection(Collection collection) {
      checkProjectWriteRole();

      Collection storedCollection = createCollectionMetadata(collection);
      dataDao.createDataRepository(storedCollection.getId());

      return storedCollection;
   }

   public Collection updateCollection(String collectionCode, Collection collection) {
      Collection storedCollection = collectionDao.getCollectionByCode(collectionCode);
      permissionsChecker.checkRole(storedCollection, Role.MANAGE);

      keepUnmodifiableFields(collection, storedCollection);
      Collection updatedCollection = collectionDao.updateCollection(storedCollection.getId(), collection);
      return keepOnlyActualUserRoles(updatedCollection);
   }

   private void keepUnmodifiableFields(Collection collection, Collection storedCollection) {
      keepStoredPermissions(collection, storedCollection.getPermissions());

      collection.setAttributes(storedCollection.getAttributes());
      collection.setDocumentsCount(storedCollection.getDocumentsCount());
      collection.setLastTimeUsed(storedCollection.getLastTimeUsed());
   }

   public void deleteCollection(String collectionCode) {
      Collection collection = collectionDao.getCollectionByCode(collectionCode);
      permissionsChecker.checkRole(collection, Role.MANAGE);

      String collectionId = collection.getId();
      collectionDao.deleteCollection(collectionId);
      documentDao.deleteDocuments(collectionId);
      dataDao.deleteDataRepository(collectionId);
   }

   public Collection getCollection(String collectionCode) {
      Collection collection = collectionDao.getCollectionByCode(collectionCode);
      permissionsChecker.checkRole(collection, Role.READ);

      return keepOnlyActualUserRoles(collection);
   }

   public List<Collection> getCollections(Pagination pagination) {
      SearchQuery searchQuery = createPaginationQuery(pagination);
      return collectionDao.getCollections(searchQuery).stream()
                          .map(this::keepOnlyActualUserRoles)
                          .collect(Collectors.toList());
   }

   public Attribute updateCollectionAttribute(String collectionCode, String attributeFullName, Attribute attribute) {
      Collection collection = collectionDao.getCollectionByCode(collectionCode);
      permissionsChecker.checkRole(collection, Role.MANAGE);

      collection.updateAttribute(attributeFullName, attribute);
      collectionDao.updateCollection(collection.getId(), collection);

      return attribute;
   }

   public void deleteCollectionAttribute(String collectionCode, String attributeFullName) {
      Collection collection = collectionDao.getCollectionByCode(collectionCode);
      permissionsChecker.checkRole(collection, Role.MANAGE);

      collection.deleteAttribute(attributeFullName);
      collectionDao.updateCollection(collection.getId(), collection);
   }

   public Permissions getCollectionPermissions(final String code) {
      Collection collection = collectionDao.getCollectionByCode(code);
      permissionsChecker.checkRole(collection, Role.MANAGE);

      return collection.getPermissions();
   }

   public Set<Permission> updateUserPermissions(final String code, final Permission... userPermissions) {
      Collection collection = collectionDao.getCollectionByCode(code);
      permissionsChecker.checkRole(collection, Role.MANAGE);

      collection.getPermissions().updateUserPermissions(userPermissions);
      Collection updatedCollection = collectionDao.updateCollection(collection.getId(), collection);

      return updatedCollection.getPermissions().getUserPermissions();
   }

   public void removeUserPermission(final String code, final String user) {
      Collection collection = collectionDao.getCollectionByCode(code);
      permissionsChecker.checkRole(collection, Role.MANAGE);

      collection.getPermissions().removeUserPermission(user);
      collectionDao.updateCollection(collection.getId(), collection);
   }

   public Set<Permission> updateGroupPermissions(final String code, final Permission... groupPermissions) {
      Collection collection = collectionDao.getCollectionByCode(code);
      permissionsChecker.checkRole(collection, Role.MANAGE);

      collection.getPermissions().updateGroupPermissions(groupPermissions);
      Collection updatedCollection = collectionDao.updateCollection(collection.getId(), collection);

      return updatedCollection.getPermissions().getGroupPermissions();
   }

   public void removeGroupPermission(final String code, final String group) {
      Collection collection = collectionDao.getCollectionByCode(code);
      permissionsChecker.checkRole(collection, Role.MANAGE);

      collection.getPermissions().removeGroupPermission(group);
      collectionDao.updateCollection(collection.getId(), collection);
   }

   private void checkProjectWriteRole() {
      if (!workspaceKeeper.getProject().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }

      Project project = workspaceKeeper.getProject().get();
      permissionsChecker.checkRole(project, Role.WRITE);
   }

   private Collection createCollectionMetadata(Collection collection) {
      if (collection.getCode() == null || collection.getCode().isEmpty()) {
         collection.setCode(generateCollectionCode(collection.getName()));
      }

      Permission defaultUserPermission = new SimplePermission(authenticatedUser.getCurrentUsername(), Collection.ROLES);
      collection.getPermissions().updateUserPermissions(defaultUserPermission);

      return collectionDao.createCollection(collection);
   }

   private String generateCollectionCode(String collectionName) {
      Set<String> existingCodes = collectionDao.getAllCollectionCodes();
      return CodeGenerator.generate(existingCodes, collectionName);
   }

}
