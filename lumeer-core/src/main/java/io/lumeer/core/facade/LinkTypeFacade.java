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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.FileAttachment;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.Role;
import io.lumeer.api.util.CollectionUtil;
import io.lumeer.core.adapter.LinkTypeAdapter;
import io.lumeer.core.auth.AuthenticatedUserGroups;
import io.lumeer.core.exception.NoResourcePermissionException;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.ResourceCommentDao;
import io.lumeer.storage.api.query.DatabaseQuery;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class LinkTypeFacade extends AbstractFacade {

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private AuthenticatedUserGroups authenticatedUserGroups;

   @Inject
   private LinkDataDao linkDataDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private ViewFacade viewFacade;

   @Inject
   private FileAttachmentFacade fileAttachmentFacade;

   @Inject
   private ResourceCommentDao resourceCommentDao;

   private LinkTypeAdapter adapter;

   @PostConstruct
   public void init() {
      adapter = new LinkTypeAdapter(linkInstanceDao);
   }

   public LinkTypeAdapter getAdapter() {
      return adapter;
   }

   public LinkType createLinkType(LinkType linkType) {
      permissionsChecker.checkFunctionsLimit(linkType);
      permissionsChecker.checkRulesLimit(linkType);
      checkLinkTypePermission(linkType);

      linkType.setLastAttributeNum(0);
      linkType.setLinksCount(0L);
      return linkTypeDao.createLinkType(linkType);
   }

   public LinkType updateLinkType(String id, LinkType linkType) {
      return updateLinkType(id, linkType, false);
   }

   public LinkType updateLinkType(String id, LinkType linkType, final boolean skipFceLimits) {
      LinkType storedLinkType = checkLinkTypePermission(id);
      LinkType originalLinkType = new LinkType(storedLinkType);
      if (!storedLinkType.getCollectionIds().containsAll(storedLinkType.getCollectionIds())) {
         checkLinkTypePermission(linkType);
      }

      if (!skipFceLimits) {
         permissionsChecker.checkFunctionsLimit(linkType);
         permissionsChecker.checkRulesLimit(linkType);
      }
      keepUnmodifiableFields(linkType, storedLinkType);

      assignComputedParameters(originalLinkType);
      return linkTypeDao.updateLinkType(id, linkType, originalLinkType);
   }

   private void keepUnmodifiableFields(LinkType linkType, LinkType storedLinkType) {
      linkType.setAttributes(storedLinkType.getAttributes());
      linkType.setLastAttributeNum(storedLinkType.getLastAttributeNum());
      linkType.setCollectionIds(storedLinkType.getCollectionIds());
   }

   public void deleteLinkType(String id) {
      LinkType linkType = checkLinkTypePermission(id);
      linkTypeDao.deleteLinkType(id);
      deleteLinkTypeBasedData(linkType.getId());
   }

   private void deleteLinkTypeBasedData(final String linkTypeId) {
      linkInstanceDao.getLinkInstancesByLinkType(linkTypeId).forEach(linkInstance -> {
         resourceCommentDao.deleteComments(ResourceType.LINK, linkInstance.getId());
      });

      linkInstanceDao.deleteLinkInstancesByLinkTypesIds(Collections.singleton(linkTypeId));
      linkDataDao.deleteDataRepository(linkTypeId);
      deleteAutoLinkRulesByLinkType(linkTypeId);
      fileAttachmentFacade.removeAllFileAttachments(linkTypeId, FileAttachment.AttachmentType.LINK);
   }

   private void deleteAutoLinkRulesByLinkType(final String linkTypeId) {
      collectionDao.getAllCollections().stream()
                   .filter(collection -> CollectionUtil.containsAutoLinkRuleLinkType(collection, linkTypeId))
                   .collect(Collectors.toList())
                   .forEach(collection -> {
                      final Collection originalCollection = collection.copy();
                      filterAutoLinkRulesByLinkType(collection, linkTypeId);
                      collectionDao.updateCollection(collection.getId(), collection, originalCollection);
                   });
   }

   private void filterAutoLinkRulesByLinkType(final Collection collection, final String linkTypeId) {
      collection.setRules(collection.getRules().entrySet()
                                    .stream().filter(entry -> !CollectionUtil.containsAutoLinkRuleLinkType(entry.getValue(), linkTypeId))
                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
   }

   public LinkType getLinkType(final String linkTypeId) {
      var linkType = linkTypeDao.getLinkType(linkTypeId);
      var collections = collectionDao.getCollectionsByIds(linkType.getCollectionIds());
      if (collections.size() == 2 && collections.stream().allMatch(collection -> permissionsChecker.hasRoleWithView(collection, Role.READ, Role.READ))) {
         return assignComputedParameters(linkType);
      }

      var viewsLinkTypes = viewFacade.getViewsLinkTypes();
      if (viewsLinkTypes.stream().anyMatch(lt -> lt.getId().equals(linkTypeId))) {
         return assignComputedParameters(linkType);
      }

      throw new NoResourcePermissionException(collections.get(0));
   }

   public List<LinkType> getLinkTypes() {
      final List<LinkType> allLinkTypes = linkTypeDao.getAllLinkTypes();
      if (isManager()) {
         return assignComputedParameters(allLinkTypes);
      }

      final List<String> allowedCollectionIds = collectionDao.getCollections(createCollectionsQuery()).stream()
                                                             .map(Collection::getId).collect(Collectors.toList());

      final List<LinkType> linkTypes = allLinkTypes.stream()
                                                   .filter(linkType -> allowedCollectionIds.containsAll(linkType.getCollectionIds()))
                                                   .collect(Collectors.toList());
      return assignComputedParameters(linkTypes);
   }

   public List<LinkType> getLinkTypesPublic() {
      if (permissionsChecker.isPublic()) {
         return assignComputedParameters(linkTypeDao.getAllLinkTypes());
      }

      return List.of();
   }

   public List<LinkType> getLinkTypesByIds(Set<String> ids) {
      return assignComputedParameters(linkTypeDao.getLinkTypesByIds(ids));
   }

   public LinkType assignComputedParameters(LinkType linkType) {
      return adapter.mapLinkTypeData(linkType);
   }

   public List<LinkType> assignComputedParameters(List<LinkType> linkTypes) {
      var countsMap = adapter.getLinkInstancesCounts();

      return linkTypes.stream()
                      .peek(linkType -> linkType.setLinksCount(countsMap.getOrDefault(linkType.getId(), 0L)))
                      .collect(Collectors.toList());
   }

   public java.util.Collection<Attribute> createLinkTypeAttributes(final String linkTypeId, final java.util.Collection<Attribute> attributes) {
      LinkType linkType = checkLinkTypePermission(linkTypeId);
      LinkType originalLinkType = new LinkType(linkType);

      for (Attribute attribute : attributes) {
         final Integer freeNum = getFreeAttributeNum(linkType);
         attribute.setId(LinkType.ATTRIBUTE_PREFIX + freeNum);
         attribute.setUsageCount(0);
         linkType.createAttribute(attribute);
         linkType.setLastAttributeNum(freeNum);
      }

      permissionsChecker.checkFunctionsLimit(linkType);
      linkTypeDao.updateLinkType(linkTypeId, linkType, originalLinkType);

      return attributes;
   }

   private Integer getFreeAttributeNum(final LinkType linkType) {
      int lastAttributeNum = Objects.requireNonNullElse(linkType.getLastAttributeNum(), 0);
      final AtomicInteger last = new AtomicInteger(Math.max(1, lastAttributeNum + 1));
      while (linkType.getAttributes().stream().anyMatch(attribute -> attribute.getId().equals(LinkType.ATTRIBUTE_PREFIX + last.get()))) {
         last.incrementAndGet();
      }

      return last.get();
   }

   public Attribute updateLinkTypeAttribute(final String linkTypeId, final String attributeId, final Attribute attribute) {
      return updateLinkTypeAttribute(linkTypeId, attributeId, attribute, false);
   }

   public Attribute updateLinkTypeAttribute(final String linkTypeId, final String attributeId, final Attribute attribute, final boolean skipFceLimits) {
      LinkType linkType = checkLinkTypePermission(linkTypeId);
      LinkType originalLinkType = new LinkType(linkType);
      final Optional<Attribute> originalAttribute = linkType.getAttributes().stream().filter(attr -> attr.getId().equals(attributeId)).findFirst();

      linkType.updateAttribute(attributeId, attribute);
      if (attribute.getFunction() != null && attribute.getFunction().getJs() != null && attribute.getFunction().getJs().isEmpty()) {
         attribute.setFunction(null);
      }

      if (!skipFceLimits) {
         if (originalAttribute.isPresent() && originalAttribute.get().getFunction() == null && attribute.getFunction() != null) {
            permissionsChecker.checkFunctionsLimit(linkType);
         }
      }

      linkTypeDao.updateLinkType(linkTypeId, linkType, originalLinkType);

      return attribute;
   }

   public void deleteLinkTypeAttribute(final String linkTypeId, final String attributeId) {
      LinkType linkType = checkLinkTypePermission(linkTypeId);
      LinkType originalLinkType = new LinkType(linkType);

      linkDataDao.deleteAttribute(linkTypeId, attributeId);

      linkType.deleteAttribute(attributeId);
      linkTypeDao.updateLinkType(linkTypeId, linkType, originalLinkType);

      fileAttachmentFacade.removeAllFileAttachments(linkTypeId, attributeId, FileAttachment.AttachmentType.LINK);
   }

   private void checkLinkTypePermission(LinkType linkType) {
      permissionsChecker.checkLinkTypePermissions(linkType, Role.WRITE, true);
   }

   private LinkType checkLinkTypePermission(String linkTypeId) {
      LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      permissionsChecker.checkLinkTypePermissions(linkType, Role.WRITE, true);
      return linkType;
   }

   private DatabaseQuery createCollectionsQuery() {
      String user = authenticatedUser.getCurrentUserId();
      Set<String> groups = authenticatedUserGroups.getCurrentUserGroups();

      return DatabaseQuery.createBuilder(user).groups(groups)
                          .build();
   }

}
