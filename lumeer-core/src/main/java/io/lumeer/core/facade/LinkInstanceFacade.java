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
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Role;
import io.lumeer.api.util.ResourceUtils;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
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
   private FunctionFacade functionFacade;

   private ConstraintManager constraintManager;

   @PostConstruct
   public void init() {
      constraintManager = new ConstraintManager();
      final String locale = configurationProducer.get(DefaultConfigurationProducer.LOCALE);

      if (locale != null && !"".equals(locale)) {
         constraintManager.setLocale(Locale.forLanguageTag(locale));
      } else {
         constraintManager.setLocale(Locale.getDefault());
      }
   }

   public LinkInstance createLinkInstance(LinkInstance linkInstance) {
      checkLinkTypePermissions(linkInstance.getLinkTypeId());

      linkInstance.setCreatedBy(authenticatedUser.getCurrentUserId());
      linkInstance.setCreationDate(ZonedDateTime.now());
      return linkInstanceDao.createLinkInstance(linkInstance);
   }

   public LinkInstance updateLinkInstanceData(String linkInstanceId, DataDocument data) {
      LinkInstance stored = linkInstanceDao.getLinkInstance(linkInstanceId);
      LinkType linkType = checkLinkTypePermissions(stored.getLinkTypeId());

      constraintManager.encodeDataTypes(linkType, data);

      DataDocument oldData = linkDataDao.getData(linkType.getId(), linkInstanceId);
      Set<String> attributesIdsToAdd = new HashSet<>(data.keySet());
      attributesIdsToAdd.removeAll(oldData.keySet());

      Set<String> attributesIdsToDec = new HashSet<>(oldData.keySet());
      attributesIdsToDec.removeAll(data.keySet());
      updateLinkTypeMetadata(linkType, attributesIdsToAdd, attributesIdsToDec);

      DataDocument updatedData = linkDataDao.updateData(linkType.getId(), linkInstanceId, data);
      checkAttributesValueChanges(linkType, linkInstanceId, oldData, updatedData);

      constraintManager.decodeDataTypes(linkType, updatedData);
      return updateLinkInstance(stored, updatedData);
   }

   private LinkInstance updateLinkInstance(LinkInstance linkInstance, DataDocument newData){
      linkInstance.setData(newData);
      linkInstance.setUpdateDate(ZonedDateTime.now());
      linkInstance.setUpdatedBy(authenticatedUser.getCurrentUserId());

      LinkInstance updatedLinkInstance = linkInstanceDao.updateLinkInstance(linkInstance.getId(), linkInstance);
      updatedLinkInstance.setData(newData);
      return updatedLinkInstance;
   }

   private void updateLinkTypeMetadata(LinkType linkType, Set<String> attributesIdsToInc, Set<String> attributesIdsToDec) {
      linkType.setAttributes(new ArrayList<>(ResourceUtils.incOrDecAttributes(linkType.getAttributes(), attributesIdsToInc, attributesIdsToDec)));
      linkTypeDao.updateLinkType(linkType.getId(), linkType);
   }

   private void checkAttributesValueChanges(LinkType linkType, String linkInstanceId, DataDocument oldData, DataDocument newData) {
      java.util.Collection<Attribute> attributes = linkType.getAttributes();
      for (Attribute attribute : attributes) {
         Object oldValue = oldData.get(attribute.getId());
         Object newValue = newData.get(attribute.getId());
         if (!Objects.deepEquals(oldValue, newValue)) {
            functionFacade.onLinkValueChanged(linkType.getId(), attribute.getId(), linkInstanceId);
         }
      }
   }

   public LinkInstance patchLinkInstanceData(String linkInstanceId, DataDocument data) {
      LinkInstance stored = linkInstanceDao.getLinkInstance(linkInstanceId);
      LinkType linkType = checkLinkTypePermissions(stored.getLinkTypeId());

      constraintManager.encodeDataTypes(linkType, data);

      DataDocument oldData = linkDataDao.getData(linkType.getId(), linkInstanceId);
      Set<String> attributesIdsToAdd = new HashSet<>(data.keySet());
      attributesIdsToAdd.removeAll(oldData.keySet());

      updateLinkTypeMetadata(linkType, attributesIdsToAdd, Collections.emptySet());

      DataDocument updatedData = linkDataDao.patchData(linkType.getId(), linkInstanceId, data);
      checkAttributesValueChanges(linkType, linkInstanceId, oldData, updatedData);

      constraintManager.decodeDataTypes(linkType, updatedData);
      return updateLinkInstance(stored, updatedData);
   }

   public void deleteLinkInstance(String id) {
      LinkInstance stored = linkInstanceDao.getLinkInstance(id);
      checkLinkTypePermissions(stored.getLinkTypeId());

      linkInstanceDao.deleteLinkInstance(id);
      linkDataDao.deleteData(stored.getLinkTypeId(), id);
   }

   private LinkType checkLinkTypePermissions(String linkTypeId) {
      LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      List<Collection> collections = collectionDao.getCollectionsByIds(linkType.getCollectionIds());
      for (Collection collection : collections) {
         permissionsChecker.checkRoleWithView(collection, Role.WRITE, Role.WRITE);
      }
      return linkType;
   }

}
