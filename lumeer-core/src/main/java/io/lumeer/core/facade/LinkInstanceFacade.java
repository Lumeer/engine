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
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Role;
import io.lumeer.api.util.ResourceUtils;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.CreateLinkInstance;
import io.lumeer.engine.api.event.UpdateLinkInstance;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
public class LinkInstanceFacade extends AbstractFacade {

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private LinkDataDao linkDataDao;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   @Inject
   private Event<CreateLinkInstance> createLinkInstanceEvent;

   @Inject
   private Event<UpdateLinkInstance> updateLinkInstanceEvent;

   private ConstraintManager constraintManager;

   @PostConstruct
   public void init() {
      constraintManager = ConstraintManager.getInstance(configurationProducer);
   }

   public LinkInstance createLinkInstance(final LinkInstance linkInstance) {
      checkLinkTypeWritePermissions(linkInstance.getLinkTypeId());

      linkInstance.setCreatedBy(authenticatedUser.getCurrentUserId());
      linkInstance.setCreationDate(ZonedDateTime.now());
      LinkInstance createdLinkInstance = linkInstanceDao.createLinkInstance(linkInstance);

      final DataDocument data = linkDataDao.createData(linkInstance.getLinkTypeId(), createdLinkInstance.getId(), new DataDocument());
      createdLinkInstance.setData(data);

      if (createLinkInstanceEvent != null) {
         createLinkInstanceEvent.fire(new CreateLinkInstance(createdLinkInstance));
      }

      return createdLinkInstance;
   }

   public LinkInstance updateLinkInstanceData(final String linkInstanceId, final DataDocument data) {
      final LinkInstance stored = linkInstanceDao.getLinkInstance(linkInstanceId);
      final LinkInstance originalLinkInstance = copyLinkInstance(stored);
      final LinkType linkType = checkLinkTypeWritePermissions(stored.getLinkTypeId());

      constraintManager.encodeDataTypes(linkType, data);

      final DataDocument oldData = linkDataDao.getData(linkType.getId(), linkInstanceId);
      final Set<String> attributesIdsToAdd = new HashSet<>(data.keySet());
      attributesIdsToAdd.removeAll(oldData.keySet());

      final Set<String> attributesIdsToDec = new HashSet<>(oldData.keySet());
      attributesIdsToDec.removeAll(data.keySet());
      updateLinkTypeMetadata(linkType, attributesIdsToAdd, attributesIdsToDec);

      final DataDocument updatedData = linkDataDao.updateData(linkType.getId(), linkInstanceId, data);

      final LinkInstance updatedLinkInstance = updateLinkInstance(stored, updatedData, originalLinkInstance);

      constraintManager.decodeDataTypes(linkType, updatedData);

      return updatedLinkInstance;
   }

   private LinkInstance copyLinkInstance(final LinkInstance linkInstance) {
      final LinkInstance originalLinkInstance = new LinkInstance(linkInstance);

      if (originalLinkInstance.getData() != null) {
         originalLinkInstance.setData(new DataDocument(originalLinkInstance.getData())); // deep copy of data
      }

      return originalLinkInstance;
   }

   private LinkInstance updateLinkInstance(LinkInstance linkInstance, DataDocument newData, final LinkInstance originalLinkInstance){
      linkInstance.setData(newData);
      linkInstance.setUpdateDate(ZonedDateTime.now());
      linkInstance.setUpdatedBy(authenticatedUser.getCurrentUserId());

      LinkInstance updatedLinkInstance = linkInstanceDao.updateLinkInstance(linkInstance.getId(), linkInstance);
      updatedLinkInstance.setData(newData);

      fireLinkInstanceUpdate(linkInstance, updatedLinkInstance, originalLinkInstance);

      return updatedLinkInstance;
   }

   private void fireLinkInstanceUpdate(final LinkInstance toBeStored, final LinkInstance updatedLinkInstance, final LinkInstance originalLinkInstance) {
      if (updateLinkInstanceEvent != null) {
         LinkInstance updatedLinkInstanceWithData = new LinkInstance(updatedLinkInstance);
         updatedLinkInstanceWithData.setDataVersion(toBeStored.getDataVersion());
         updateLinkInstanceEvent.fire(new UpdateLinkInstance(updatedLinkInstanceWithData, originalLinkInstance));
      }
   }

   private void updateLinkTypeMetadata(LinkType linkType, Set<String> attributesIdsToInc, Set<String> attributesIdsToDec) {
      linkType.setAttributes(new ArrayList<>(ResourceUtils.incOrDecAttributes(linkType.getAttributes(), attributesIdsToInc, attributesIdsToDec)));
      linkTypeDao.updateLinkType(linkType.getId(), linkType);
   }

   public LinkInstance patchLinkInstanceData(final String linkInstanceId, final DataDocument data) {
      final LinkInstance stored = linkInstanceDao.getLinkInstance(linkInstanceId);
      final LinkInstance originalLinkInstance = copyLinkInstance(stored);
      final LinkType linkType = checkLinkTypeWritePermissions(stored.getLinkTypeId());

      constraintManager.encodeDataTypes(linkType, data);

      final DataDocument oldData = linkDataDao.getData(linkType.getId(), linkInstanceId);
      final Set<String> attributesIdsToAdd = new HashSet<>(data.keySet());
      attributesIdsToAdd.removeAll(oldData.keySet());

      updateLinkTypeMetadata(linkType, attributesIdsToAdd, Collections.emptySet());

      final DataDocument updatedData = linkDataDao.patchData(linkType.getId(), linkInstanceId, data);

      final LinkInstance updatedLinkInstance = updateLinkInstance(stored, updatedData, originalLinkInstance);

      constraintManager.decodeDataTypes(linkType, updatedData);

      return updatedLinkInstance;
   }

   public void deleteLinkInstance(String id) {
      LinkInstance stored = linkInstanceDao.getLinkInstance(id);
      checkLinkTypeWritePermissions(stored.getLinkTypeId());

      linkInstanceDao.deleteLinkInstance(id);
      linkDataDao.deleteData(stored.getLinkTypeId(), id);
   }

   public LinkInstance getLinkInstance(String linkTypeId, String linkInstanceId) {
      LinkInstance stored = linkInstanceDao.getLinkInstance(linkInstanceId);
      LinkType linkType = checkLinkTypeReadPermissions(stored.getLinkTypeId());

      DataDocument data = linkDataDao.getData(linkTypeId, linkInstanceId);
      constraintManager.decodeDataTypes(linkType, data);
      stored.setData(data);

      return stored;
   }

   private LinkType checkLinkTypeWritePermissions(String linkTypeId) {
      return checkLinkTypePermissions(linkTypeId, Role.WRITE);
   }

   private LinkType checkLinkTypeReadPermissions(String linkTypeId) {
      return checkLinkTypePermissions(linkTypeId, Role.READ);
   }

   private LinkType checkLinkTypePermissions(String linkTypeId, Role role) {
      LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      List<Collection> collections = collectionDao.getCollectionsByIds(linkType.getCollectionIds());
      for (Collection collection : collections) {
         permissionsChecker.checkRoleWithView(collection, role, role);
      }
      return linkType;
   }

}
