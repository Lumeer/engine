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

import io.lumeer.api.model.AuditRecord;
import io.lumeer.api.model.AuditType;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Payment;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.api.model.User;
import io.lumeer.core.adapter.AuditAdapter;
import io.lumeer.core.adapter.DocumentAdapter;
import io.lumeer.core.adapter.LinkInstanceAdapter;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.exception.UnsupportedOperationException;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.DocumentUtils;
import io.lumeer.core.util.LinkInstanceUtils;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.RemoveDocument;
import io.lumeer.engine.api.event.RemoveLinkInstance;
import io.lumeer.engine.api.event.UpdateDocument;
import io.lumeer.engine.api.event.UpdateLinkInstance;
import io.lumeer.storage.api.dao.AuditDao;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.ResourceCommentDao;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@RequestScoped
public class AuditFacade extends AbstractFacade {

   @Inject
   private AuditDao auditDao;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private DocumentDao documentDao;

   @Inject
   private DataDao dataDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private LinkDataDao linkDataDao;

   @Inject
   private PaymentFacade paymentFacade;

   @Inject
   private ResourceCommentDao resourceCommentDao;

   @Inject
   private FavoriteItemDao favoriteItemDao;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   private AuditAdapter auditAdapter;
   private DocumentAdapter documentAdapter;
   private ConstraintManager constraintManager;
   private LinkInstanceAdapter linkInstanceAdapter;

   @PostConstruct
   public void init() {
      constraintManager = ConstraintManager.getInstance(configurationProducer);
      auditAdapter = new AuditAdapter(auditDao);

      documentAdapter = new DocumentAdapter(resourceCommentDao, favoriteItemDao);
      linkInstanceAdapter = new LinkInstanceAdapter(resourceCommentDao);
   }

   public void documentUpdated(@Observes final UpdateDocument updateDocument) {
      registerDocumentUpdate(updateDocument.getOriginalDocument(), updateDocument.getDocument());
   }

   public void documentRemoved(@Observes final RemoveDocument removeDocument) {
      // auditAdapter.removeAllAuditRecords(removeDocument.getDocument().getCollectionId(), ResourceType.DOCUMENT, removeDocument.getDocument().getId());
      registerDocumentDelete(removeDocument.getDocument());
   }

   public void linkInstanceUpdated(@Observes final UpdateLinkInstance updateLinkInstance) {
      registerLinkUpdate(updateLinkInstance.getOriginalLinkInstance(), updateLinkInstance.getLinkInstance());
   }

   public void linkInstanceRemoved(@Observes final RemoveLinkInstance removeLinkInstance) {
      // auditAdapter.removeAllAuditRecords(removeLinkInstance.getLinkInstance().getLinkTypeId(), ResourceType.LINK, removeLinkInstance.getLinkInstance().getId());
      registerLinkDelete(removeLinkInstance.getLinkInstance());
   }

   public List<AuditRecord> getAuditRecordsForProject() {
      // TODO permissions ???

      return decode(auditAdapter.getAuditRecords(getServiceLevel()));
   }

   public List<AuditRecord> getAuditRecordsForCollection(final String collectionId) {
      // TODO permissions ???

      return decode(auditAdapter.getAuditRecords(collectionId, ResourceType.DOCUMENT, getServiceLevel()));
   }

   public List<AuditRecord> getAuditRecordsForLinkType(final String linkTypeId) {
      // TODO permissions ???

      return decode(auditAdapter.getAuditRecords(linkTypeId, ResourceType.LINK, getServiceLevel()));
   }

   public List<AuditRecord> getAuditRecordsForDocument(final String collectionId, final String documentId) {
      final Collection collection = collectionDao.getCollectionById(collectionId);
      final Document document = DocumentUtils.loadDocumentWithData(documentDao, dataDao, collection, documentId);
      permissionsChecker.checkEditDocument(collection, document);

      return decode(collection, auditAdapter.getAuditRecords(collectionId, ResourceType.DOCUMENT, documentId, getServiceLevel()));
   }

   public List<AuditRecord> getAuditRecordsForLink(final String linkTypeId, final String linkInstanceId) {
      final LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      final LinkInstance linkInstance = LinkInstanceUtils.loadLinkInstanceWithData(linkInstanceDao, linkDataDao, linkInstanceId);
      permissionsChecker.checkEditLinkInstance(linkType, linkInstance);

      return decode(linkType, auditAdapter.getAuditRecords(linkTypeId, ResourceType.LINK, linkInstanceId, getServiceLevel()));
   }

   public void revertAudit(final String auditRecordId) {
      final Payment.ServiceLevel level = getServiceLevel();
      if (level.equals(Payment.ServiceLevel.FREE)) {
         throw new UnsupportedOperationException("Reverting audit log entries is not available on the free plan.");
      }

      final AuditRecord record = auditDao.getAuditRecord(auditRecordId);

      switch (record.getResourceType()) {
         case DOCUMENT:
            checkRevertDocumentAudit(record);
            return;
         case LINK:
            checkRevertLinkAudit(record);
            return;
         default:
            throw new UnsupportedOperationException("Could not revert selected record");
      }

   }

   private void checkRevertDocumentAudit(final AuditRecord record) {
      switch (record.getType()) {
         case Updated:
            revertDocumentChange(record);
            return;
         case Deleted:
            revertDocumentDelete(record);
            return;
         default:
            throw new UnsupportedOperationException("Could not revert selected record");
      }
   }

   private void checkRevertLinkAudit(final AuditRecord record) {
      switch (record.getType()) {
         case Updated:
            revertLinkChange(record);
            return;
         default:
            throw new UnsupportedOperationException("Could not revert selected record");
      }
   }

   private void revertDocumentChange(final AuditRecord requestedRecord) {
      final AuditRecord auditRecord = auditDao.findLatestAuditRecord(requestedRecord.getParentId(), ResourceType.DOCUMENT, requestedRecord.getResourceId(), AuditType.Updated);
      if (auditRecord == null || !auditRecord.getId().equals(requestedRecord.getId())) {
         throw new UnsupportedOperationException("Cannot revert audit record that is not the last.");
      }

      final Collection collection = collectionDao.getCollectionById(auditRecord.getParentId());
      final Document document = DocumentUtils.loadDocumentWithData(documentDao, dataDao, collection, auditRecord.getResourceId());
      permissionsChecker.checkEditDocument(collection, document);

      if (auditRecord.getOldState() != null) {
         var keysToBeRemoved = new HashSet<>(auditRecord.getNewState().keySet());
         keysToBeRemoved.removeAll(auditRecord.getOldState().keySet());

         document.getData().putAll(auditRecord.getOldState());
         keysToBeRemoved.forEach(key -> document.getData().remove(key));

         dataDao.updateData(auditRecord.getParentId(), auditRecord.getResourceId(), document.getData());
         auditDao.deleteAuditRecord(auditRecord.getId());
      }

      document.setData(constraintManager.decodeDataTypes(collection, document.getData()));

      documentAdapter.mapDocumentData(document, getCurrentUserId(), workspaceKeeper.getProjectId());

      // TODO push
   }

   private Document revertDocumentDelete(final AuditRecord requestedRecord) {
      final AuditRecord auditRecord = auditDao.findLatestAuditRecord(requestedRecord.getParentId(), ResourceType.DOCUMENT, requestedRecord.getResourceId(), AuditType.Deleted);
      if (auditRecord == null || !auditRecord.getId().equals(requestedRecord.getId())) {
         throw new UnsupportedOperationException("Cannot revert audit record that is not the last.");
      }

      final Collection collection = collectionDao.getCollectionById(auditRecord.getParentId());
      permissionsChecker.checkCreateDocuments(collection);

      final Document document = new Document(collection.getId(), ZonedDateTime.now(), getCurrentUserId());
      document.setData(auditRecord.getOldState());

      final Document storedDocument = documentDao.createDocument(document);
      DataDocument storedData = dataDao.createData(collection.getId(), storedDocument.getId(), document.getData());

      // TODO update collection metadata

      storedDocument.setData(constraintManager.decodeDataTypes(collection, storedData));

      return documentAdapter.mapDocumentData(storedDocument, getCurrentUserId(), workspaceKeeper.getProjectId());
   }

   public void revertLinkChange(final AuditRecord requestedRecord) {
      final AuditRecord auditRecord = auditDao.findLatestAuditRecord(requestedRecord.getParentId(), ResourceType.LINK, requestedRecord.getResourceId(), AuditType.Updated);
      if (auditRecord == null || !auditRecord.getId().equals(requestedRecord.getId())) {
         throw new UnsupportedOperationException("Cannot revert audit record that is not the last.");
      }

      final LinkType linkType = linkTypeDao.getLinkType(auditRecord.getParentId());
      final LinkInstance linkInstance = LinkInstanceUtils.loadLinkInstanceWithData(linkInstanceDao, linkDataDao, auditRecord.getResourceId());
      permissionsChecker.checkEditLinkInstance(linkType, linkInstance);

      if (auditRecord.getOldState() != null) {
         var keysToBeRemoved = new HashSet<>(auditRecord.getNewState().keySet());
         keysToBeRemoved.removeAll(auditRecord.getOldState().keySet());

         linkInstance.getData().putAll(auditRecord.getOldState());
         keysToBeRemoved.forEach(key -> linkInstance.getData().remove(key));

         linkDataDao.updateData(auditRecord.getParentId(), auditRecord.getResourceId(), linkInstance.getData());
         auditDao.deleteAuditRecord(auditRecord.getId());
      }

      linkInstance.setData(constraintManager.decodeDataTypes(linkType, linkInstance.getData()));

      linkInstanceAdapter.mapLinkInstanceData(linkInstance);

      // TODO push
   }

   private AuditRecord registerDocumentUpdate(final Document oldDocument, final Document newDocument) {
      if (oldDocument == null || newDocument == null || oldDocument.getData() == null || newDocument.getData() == null ||
            !oldDocument.getId().equals(newDocument.getId())) {
         throw new UnsupportedOperationException("Cannot create audit record from different or incomplete documents.");
      }

      final User user = authenticatedUser.getCurrentUser();
      final Collection collection = collectionDao.getCollectionById(oldDocument.getCollectionId());
      final DataDocument oldDataDecoded = constraintManager.decodeDataTypes(collection, oldDocument.getData());
      final DataDocument newDataDecoded = constraintManager.decodeDataTypes(collection, newDocument.getData());

      return auditAdapter.registerDataChange(collection.getId(), ResourceType.DOCUMENT, oldDocument.getId(), user, null, oldDocument.getData(), oldDataDecoded, newDocument.getData(), newDataDecoded);
   }

   private AuditRecord registerDocumentDelete(final Document oldDocument) {
      if (oldDocument == null) {
         throw new UnsupportedOperationException("Cannot create audit record from different or incomplete documents.");
      }

      final User user = authenticatedUser.getCurrentUser();
      final Collection collection = collectionDao.getCollectionById(oldDocument.getCollectionId());

      return auditAdapter.registerDelete(collection.getId(), ResourceType.DOCUMENT, oldDocument.getId(), user, null, oldDocument.getData());
   }

   private AuditRecord registerLinkUpdate(final LinkInstance oldLink, final LinkInstance newLink) {
      if (oldLink == null || newLink == null || oldLink.getData() == null || newLink.getData() == null ||
            !oldLink.getId().equals(newLink.getId())) {
         throw new UnsupportedOperationException("Cannot create audit record from different or incomplete link instances.");
      }

      final User user = authenticatedUser.getCurrentUser();
      final LinkType linkType = linkTypeDao.getLinkType(oldLink.getLinkTypeId());
      final DataDocument oldDataDecoded = constraintManager.decodeDataTypes(linkType, oldLink.getData());
      final DataDocument newDataDecoded = constraintManager.decodeDataTypes(linkType, newLink.getData());

      return auditAdapter.registerDataChange(linkType.getId(), ResourceType.LINK, oldLink.getId(), user, null, oldLink.getData(), oldDataDecoded, newLink.getData(), newDataDecoded);
   }

   private AuditRecord registerLinkDelete(final LinkInstance oldLink) {
      if (oldLink == null) {
         throw new UnsupportedOperationException("Cannot create audit record from different or incomplete link instances.");
      }

      final User user = authenticatedUser.getCurrentUser();
      final LinkType linkType = linkTypeDao.getLinkType(oldLink.getLinkTypeId());

      return auditAdapter.registerDelete(linkType.getId(), ResourceType.LINK, oldLink.getId(), user, null, oldLink.getData());
   }

   private List<AuditRecord> decode(final List<AuditRecord> auditRecords) {
      final List<AuditRecord> decodedRecords = new ArrayList<>();
      final Map<String, Collection> collectionsMap = new HashMap<>();
      final Map<String, LinkType> linkTypesMap = new HashMap<>();

      for (AuditRecord record : auditRecords) {
         if (ResourceType.DOCUMENT.equals(record.getResourceType())) {
            final Collection collection = collectionsMap.computeIfAbsent(record.getParentId(), parentId -> collectionDao.getCollectionById(parentId));
            decode(collection, record);
         } else if (ResourceType.LINK.equals(record.getResourceType())) {
            final LinkType linkType = linkTypesMap.computeIfAbsent(record.getParentId(), parentId -> linkTypeDao.getLinkType(parentId));
            decode(linkType, record);
         }

         decodedRecords.add(record);
      }

      return decodedRecords;
   }

   private List<AuditRecord> decode(final Collection collection, final List<AuditRecord> auditRecords) {
      return auditRecords.stream().peek(record -> decode(collection, record)).collect(Collectors.toList());
   }

   private void decode(final Collection collection, final AuditRecord record) {
      if (AuditType.Updated.equals(record.getType()) || AuditType.Deleted.equals(record.getType())) {
         record.setOldState(constraintManager.decodeDataTypes(collection, record.getOldState()));

      }
      if (AuditType.Updated.equals(record.getType())) {
         record.setNewState(constraintManager.decodeDataTypes(collection, record.getNewState()));
      }
   }

   private List<AuditRecord> decode(final LinkType linkType, final List<AuditRecord> auditRecords) {
      return auditRecords.stream().peek(record -> decode(linkType, record)).collect(Collectors.toList());
   }

   private void decode(final LinkType linkType, final AuditRecord record) {
      if (AuditType.Updated.equals(record.getType()) || AuditType.Deleted.equals(record.getType())) {
         record.setOldState(constraintManager.decodeDataTypes(linkType, record.getOldState()));

      }
      if (AuditType.Updated.equals(record.getType())) {
         record.setNewState(constraintManager.decodeDataTypes(linkType, record.getNewState()));
      }
   }

   private Payment.ServiceLevel getServiceLevel() {
      if (workspaceKeeper.getOrganization().isPresent()) {
         final ServiceLimits limits = paymentFacade.getCurrentServiceLimits(workspaceKeeper.getOrganization().get());
         return limits.getServiceLevel();
      }
      return Payment.ServiceLevel.FREE;
   }
}
