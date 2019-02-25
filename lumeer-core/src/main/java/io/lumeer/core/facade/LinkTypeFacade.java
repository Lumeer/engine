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
import io.lumeer.api.model.Role;
import io.lumeer.core.auth.AuthenticatedUserGroups;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.query.DatabaseQuery;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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

   @Inject
   private LinkDataDao linkDataDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   public LinkType createLinkType(LinkType linkType) {
      checkLinkTypePermission(linkType.getCollectionIds());

      return linkTypeDao.createLinkType(linkType);
   }

   public LinkType updateLinkType(String id, LinkType linkType) {
      LinkType storedLinkType = linkTypeDao.getLinkType(id);
      Set<String> collectionIds = new HashSet<>(linkType.getCollectionIds());
      collectionIds.addAll(storedLinkType.getCollectionIds());

      checkLinkTypePermission(collectionIds);
      keepUnmodifiableFields(linkType, storedLinkType);

      return linkTypeDao.updateLinkType(id, linkType);
   }

   private void keepUnmodifiableFields(LinkType linkType, LinkType storedLinkType) {
      linkType.setAttributes(storedLinkType.getAttributes());
      linkType.setLastAttributeNum(storedLinkType.getLastAttributeNum());
   }

   public void deleteLinkType(String id) {
      LinkType linkType = linkTypeDao.getLinkType(id);
      checkLinkTypePermission(linkType.getCollectionIds());

      linkTypeDao.deleteLinkType(id);
      deleteLinkTypeBasedData(linkType.getId());
   }

   private void deleteLinkTypeBasedData(final String linkTypeId) {
      functionFacade.onDeleteLinkType(linkTypeId);
      linkInstanceDao.deleteLinkInstancesByLinkTypesIds(Collections.singleton(linkTypeId));
      linkDataDao.deleteDataRepository(linkTypeId);
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

   public java.util.Collection<Attribute> createLinkTypeAttributes(final String linkTypeId, final java.util.Collection<Attribute> attributes) {
      LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      checkLinkTypePermission(linkType.getCollectionIds());

      for (Attribute attribute : attributes) {
         final Integer freeNum = getFreeAttributeNum(linkType);
         attribute.setId(LinkType.ATTRIBUTE_PREFIX + freeNum);
         attribute.setUsageCount(0);
         linkType.createAttribute(attribute);
         linkType.setLastAttributeNum(freeNum);
      }

      linkTypeDao.updateLinkType(linkTypeId, linkType);

      return attributes;
   }

   private Integer getFreeAttributeNum(final LinkType linkType) {
      final AtomicInteger last = new AtomicInteger(Math.max(1, linkType.getLastAttributeNum() + 1));
      while (linkType.getAttributes().stream().anyMatch(attribute -> attribute.getId().equals(LinkType.ATTRIBUTE_PREFIX + last.get()))) {
         last.incrementAndGet();
      }

      return last.get();
   }

   public Attribute updateLinkTypeAttribute(final String linkTypeId, final String attributeId, final Attribute attribute) {
      LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      checkLinkTypePermission(linkType.getCollectionIds());

      LinkType originalLinkType = new LinkType(linkType);

      linkType.updateAttribute(attributeId, attribute);

      linkTypeDao.updateLinkType(linkTypeId, linkType);

      Attribute attributeCopy = new Attribute(attributeId, attribute.getName(), attribute.getConstraint(), attribute.getFunction(), attribute.getUsageCount());
      checkAttributeFunctionChange(originalLinkType, attributeCopy);

      return attribute;
   }

   private void checkAttributeFunctionChange(LinkType linkType, Attribute newAttribute) {
      Attribute originalAttribute = linkType.getAttributes().stream()
                                            .filter(attr -> attr.getId().equals(newAttribute.getId()))
                                            .findFirst().orElse(null);
      if (originalAttribute == null) {
         return;
      }

      if (originalAttribute.getFunction() == null && newAttribute.getFunction() != null) {
         functionFacade.onCreateLinkTypeFunction(linkType, newAttribute);
      } else if (originalAttribute.getFunction() != null && newAttribute.getFunction() == null) {
         functionFacade.onDeleteLinkTypeFunction(linkType.getId(), newAttribute.getId());
      } else if (originalAttribute.getFunction() != null && newAttribute.getFunction() != null) {
         if (!originalAttribute.getFunction().getXml().equals(newAttribute.getFunction().getXml())) {
            functionFacade.onUpdateLinkTypeFunction(linkType, newAttribute);
         }
      }

   }

   public void deleteLinkTypeAttribute(final String linkTypeId, final String attributeId) {
      LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      checkLinkTypePermission(linkType.getCollectionIds());

      linkDataDao.deleteAttribute(linkTypeId, attributeId);

      linkType.deleteAttribute(attributeId);
      linkTypeDao.updateLinkType(linkTypeId, linkType);

      functionFacade.onDeleteLinkAttribute(linkTypeId, attributeId);
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
