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
import io.lumeer.api.model.ResourceComment;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.common.Resource;
import io.lumeer.core.adapter.ResourceCommentAdapter;
import io.lumeer.core.exception.AccessForbiddenException;
import io.lumeer.core.exception.NoPermissionException;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.api.event.RemoveDocument;
import io.lumeer.engine.api.event.RemoveResource;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.ResourceCommentDao;
import io.lumeer.storage.api.dao.ViewDao;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@RequestScoped
public class ResourceCommentFacade extends AbstractFacade {

   @Inject
   private ResourceCommentDao resourceCommentDao;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private ViewDao viewDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   private ResourceCommentAdapter adapter;

   @PostConstruct
   public void init() {
      adapter = new ResourceCommentAdapter(resourceCommentDao);
   }

   public ResourceCommentAdapter getAdapter() {
      return adapter;
   }

   public ResourceComment createResourceComment(final ResourceComment comment) {
      checkPermissions(comment.getResourceType(), comment.getResourceId());

      comment.setAuthor(getCurrentUserId());
      comment.setAuthorEmail(authenticatedUser.getCurrentUser().getEmail());
      comment.setAuthorName(authenticatedUser.getCurrentUser().getName());
      comment.setCreationDate(ZonedDateTime.now());
      comment.setParentId(getParentIdByComment(comment));

      return resourceCommentDao.createComment(comment);
   }

   public void storeResourceComments(final List<ResourceComment> comments) {
      // this is called from template creation so we do not need to check permissions
      resourceCommentDao.createComments(comments);
   }

   private String getParentIdByComment(final ResourceComment comment) {
      if (comment.getResourceType() == ResourceType.DOCUMENT) {
         return Utils.computeIfNotNull(documentDao.getDocumentById(comment.getResourceId()), Document::getCollectionId);
      } else if (comment.getResourceType() == ResourceType.LINK) {
         return Utils.computeIfNotNull(linkInstanceDao.getLinkInstance(comment.getResourceId()), LinkInstance::getLinkTypeId);
      }

      return null;
   }

   public ResourceComment updateResourceComment(final ResourceComment comment) {
      checkPermissions(comment.getResourceType(), comment.getResourceId());
      final ResourceComment dbComment = getCommentForUpdate(comment);

      dbComment.setUpdateDate(ZonedDateTime.now());
      dbComment.setComment(comment.getComment());
      dbComment.setMetaData(comment.getMetaData());

      return resourceCommentDao.updateComment(dbComment);
   }

   public void deleteComment(final ResourceComment comment) {
      checkPermissions(comment.getResourceType(), comment.getResourceId());
      final ResourceComment dbComment = getCommentForUpdate(comment);

      resourceCommentDao.deleteComment(dbComment);
   }

   public List<ResourceComment> getComments(final ResourceType resourceType, final String resourceId, final int pageStart, final int pageLenght) {
      checkPermissions(resourceType, resourceId);

      return resourceCommentDao.getResourceComments(resourceType, resourceId, pageStart, pageLenght);
   }

   public long getCommentsCount(final ResourceType resourceType, final String resourceId) {
      return adapter.getCommentsCount(resourceType, resourceId);
   }

   public Map<String, Integer> getCommentsCountByParent(final ResourceType resourceType, final String parentId) {
      return adapter.getCommentsCountsByParent(resourceType, parentId);
   }

   public Map<String, Integer> getCommentsCounts(final ResourceType resourceType, final Set<String> resourceIds) {
      return adapter.getCommentsCounts(resourceType, resourceIds);
   }

   public void removeResource(@Observes final RemoveResource removeResource) {
      final ResourceType type = removeResource.getResource().getType();
      if (type != ResourceType.ORGANIZATION && type != ResourceType.PROJECT) {
         resourceCommentDao.deleteComments(removeResource.getResource().getType(), removeResource.getResource().getId());
      }
   }

   public void removeDocument(@Observes final RemoveDocument removeDocument) {
      resourceCommentDao.deleteComments(ResourceType.DOCUMENT, removeDocument.getDocument().getId());
   }

   private ResourceComment getCommentForUpdate(final ResourceComment comment) {
      final ResourceComment result = resourceCommentDao.getComment(comment.getId());

      if (result != null && result.getAuthor() != null && result.getAuthor().equals(getCurrentUserId())) {
         return result;
      }

      throw new AccessForbiddenException("Only comment author can update it.");
   }

   private void checkPermissions(final ResourceType resourceType, final String resourceId) {
      switch (resourceType) {
         case LINK_TYPE: {
            permissionsChecker.checkRoleInLinkTypeWithView(getLinkType(resourceId), RoleType.CommentContribute);
            break;
         }
         case COLLECTION: {
            permissionsChecker.checkRoleInCollectionWithView(getCollection(resourceId), RoleType.CommentContribute);
            break;
         }
         case LINK: {
            final LinkInstance linkInstance = linkInstanceDao.getLinkInstance(resourceId);
            final LinkType linkType = getLinkType(linkInstance.getLinkTypeId());
            permissionsChecker.checkRoleInLinkTypeWithView(linkType, RoleType.CommentContribute);
            break;
         }
         case DOCUMENT: {
            Document document = documentDao.getDocumentById(resourceId);
            Collection collection = collectionDao.getCollectionById(document.getCollectionId());
            permissionsChecker.checkRoleInCollectionWithView(collection, RoleType.CommentContribute);
            break;
         }
         default: {
            permissionsChecker.checkRole(getResource(resourceType, resourceId), RoleType.CommentContribute);
         }
      }
   }

   private Resource getResource(final ResourceType resourceType, final String resourceId) {
      switch (resourceType) {
         case ORGANIZATION:
            return getOrganization();
         case PROJECT:
            return getProject();
         case VIEW:
            return viewDao.getViewById(resourceId);
         case COLLECTION:
            return collectionDao.getCollectionById(resourceId);
         case DOCUMENT:
            return collectionDao.getCollectionById(documentDao.getDocumentById(resourceId).getCollectionId());
         default:
            return null;
      }
   }

   private LinkType getLinkType(final String resourceId) {
      return linkTypeDao.getLinkType(resourceId);
   }

   private Collection getCollection(final String resourceId) {
      return collectionDao.getCollectionById(resourceId);
   }
}
