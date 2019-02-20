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
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Role;
import io.lumeer.core.auth.AuthenticatedUserGroups;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.query.DatabaseQuery;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class LinkTypeFacade extends AbstractFacade {

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private FunctionFacade functionFacade;

   @Inject
   private AuthenticatedUserGroups authenticatedUserGroups;

   public LinkType createLinkType(LinkType linkType) {
      checkLinkTypePermission(linkType.getCollectionIds());

      return linkTypeDao.createLinkType(linkType);
   }

   public LinkType updateLinkType(String id, LinkType linkType) {
      Set<String> collectionIds = new HashSet<>(linkType.getCollectionIds());
      collectionIds.addAll(linkTypeDao.getLinkType(id).getCollectionIds());

      checkLinkTypePermission(collectionIds);

      return linkTypeDao.updateLinkType(id, linkType);
   }

   public void deleteLinkType(String id) {
      LinkType linkType = linkTypeDao.getLinkType(id);
      checkLinkTypePermission(linkType.getCollectionIds());

      linkTypeDao.deleteLinkType(id);
      functionFacade.onDeleteLinkType(id);
   }

   public LinkType getLinkType(final String linkTypeId) {
      return linkTypeDao.getLinkType(linkTypeId);
   }

   public List<LinkType> getLinkTypes() {
      List<LinkType> allLinkTypes = linkTypeDao.getAllLinkTypes();
      if (isManager()) {
         return allLinkTypes;
      }

      List<String> allowedCollectionIds = collectionDao.getCollections(createCollectionsQuery()).stream()
                                                       .map(Collection::getId).collect(Collectors.toList());
      return allLinkTypes.stream()
                         .filter(linkType -> allowedCollectionIds.containsAll(linkType.getCollectionIds()))
                         .collect(Collectors.toList());
   }

   private void checkLinkTypePermission(java.util.Collection<String> collectionIds) {
      List<Collection> collections = collectionDao.getCollectionsByIds(collectionIds);
      for (Collection collection : collections) {
         permissionsChecker.checkRoleWithView(collection, Role.WRITE, Role.WRITE);
      }
   }

   private DatabaseQuery createCollectionsQuery() {
      String user = authenticatedUser.getCurrentUserId();
      Set<String> groups = authenticatedUserGroups.getCurrentUserGroups();

      return DatabaseQuery.createBuilder(user).groups(groups)
                          .build();
   }

}
