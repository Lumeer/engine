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
import io.lumeer.api.model.Query;
import io.lumeer.api.model.Role;
import io.lumeer.core.PermissionsChecker;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkTypeDao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

public class LinkTypeFacade {

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   protected PermissionsChecker permissionsChecker;

   public LinkType createLinkType(LinkType linkType) {
      checkLinkTypePermission(new HashSet<>(linkType.getCollectionIds()));

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
      checkLinkTypePermission(new HashSet<>(linkType.getCollectionIds()));

      linkTypeDao.deleteLinkType(id);
   }

   public List<LinkType> getLinkTypes(Query query) {
      return linkTypeDao.getLinkTypes(query);
   }

   private void checkLinkTypePermission(Set<String> collectionIds) {
      List<Collection> collections = collectionDao.getCollectionByIds(new ArrayList<>(collectionIds));
      for (Collection collection : collections) {
         permissionsChecker.checkRole(collection, Role.WRITE);
      }
   }

}
