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

import java.util.HashSet;
import java.util.List;
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
      registerDocumentUpdate(updateDocument.getOriginalDocument(), updateDocument.getDocument(), null);
   }

   public void documentRemoved(@Observes final RemoveDocument removeDocument) {
      auditAdapter.removeAllAuditRecords(removeDocument.getDocument().getCollectionId(), ResourceType.DOCUMENT, removeDocument.getDocument().getId());
   }

   public void linkInstanceUpdated(@Observes final UpdateLinkInstance updateLinkInstance) {
      registerLinkUpdate(updateLinkInstance.getOriginalLinkInstance(), updateLinkInstance.getLinkInstance(), null);
   }

   public void linkInstanceRemoved(@Observes final RemoveLinkInstance removeLinkInstance) {
      auditAdapter.removeAllAuditRecords(removeLinkInstance.getLinkInstance().getLinkTypeId(), ResourceType.LINK, removeLinkInstance.getLinkInstance().getId());
   }

   public List<AuditRecord> getAuditRecordsForDocument(final String collectionId, final String documentId) {
      final Collection collection = collectionDao.getCollectionById(collectionId);
      final Document document = DocumentUtils.loadDocumentWithData(documentDao, dataDao, collection, documentId);
      permissionsChecker.checkEditDocument(collection, document);

      if (selectedWorkspace.getOrganization().isPresent()) {
         final ServiceLimits limits = paymentFacade.getCurrentServiceLimits(selectedWorkspace.getOrganization().get());

         return decode(collection, auditAdapter.getAuditRecords(collectionId, ResourceType.DOCUMENT, documentId, limits.getServiceLevel()));
      }

      return List.of();
   }

   public List<AuditRecord> getAuditRecordsForLink(final String linkTypeId, final String linkInstanceId) {
      final LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      final LinkInstance linkInstance = LinkInstanceUtils.loadLinkInstanceWithData(linkInstanceDao, linkDataDao, linkInstanceId);
      permissionsChecker.checkEditLinkInstance(linkType, linkInstance);

      if (selectedWorkspace.getOrganization().isPresent()) {
         final ServiceLimits limits = paymentFacade.getCurrentServiceLimits(selectedWorkspace.getOrganization().get());

         return decode(linkType, auditAdapter.getAuditRecords(linkTypeId, ResourceType.LINK, linkInstanceId, limits.getServiceLevel()));
      }

      return List.of();
   }

   public Document revertLastDocumentAuditOperation(final String collectionId, final String documentId, final String auditRecordId) {
      final Collection collection = collectionDao.getCollectionById(collectionId);
      final Document document = DocumentUtils.loadDocumentWithData(documentDao, dataDao, collection, documentId);
      permissionsChecker.checkEditDocument(collection, document);

      if (selectedWorkspace.getOrganization().isPresent()) {
         final ServiceLimits limits = paymentFacade.getCurrentServiceLimits(selectedWorkspace.getOrganization().get());

         if (limits.getServiceLevel().equals(Payment.ServiceLevel.FREE)) {
            throw new UnsupportedOperationException("Reverting audit log entries is not available on the free plan.");
         }

         final AuditRecord auditRecord = auditDao.findLatestAuditRecord(collectionId, ResourceType.DOCUMENT, documentId);

         if (auditRecord != null && auditRecord.getId().equals(auditRecordId) && auditRecord.getOldState() != null) {
            var keysToBeRemoved = new HashSet<>(auditRecord.getNewState().keySet());
            keysToBeRemoved.removeAll(auditRecord.getOldState().keySet());

            document.getData().putAll(auditRecord.getOldState());
            keysToBeRemoved.forEach(key -> document.getData().remove(key));

            dataDao.patchData(collectionId, documentId, auditRecord.getOldState());
            auditDao.deleteAuditRecord(auditRecordId);
         }

         document.setData(constraintManager.decodeDataTypes(collection, document.getData()));

         return documentAdapter.mapDocumentData(document, getCurrentUserId(), selectedWorkspace.getProjectId());
      }

      throw new UnsupportedOperationException("No organization specified.");
   }

   public LinkInstance revertLastLinkAuditOperation(final String linkTypeId, final String linkInstanceId, final String auditRecordId) {
      final LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      final LinkInstance linkInstance = LinkInstanceUtils.loadLinkInstanceWithData(linkInstanceDao, linkDataDao, linkInstanceId);
      permissionsChecker.checkEditLinkInstance(linkType, linkInstance);

      if (selectedWorkspace.getOrganization().isPresent()) {
         final ServiceLimits limits = paymentFacade.getCurrentServiceLimits(selectedWorkspace.getOrganization().get());

         if (limits.getServiceLevel().equals(Payment.ServiceLevel.FREE)) {
            throw new UnsupportedOperationException("Reverting audit log entries is not available on the free plan.");
         }

         final AuditRecord auditRecord = auditDao.findLatestAuditRecord(linkTypeId, ResourceType.LINK, linkInstanceId);

         if (auditRecord != null && auditRecord.getId().equals(auditRecordId) && auditRecord.getOldState() != null) {
            var keysToBeRemoved = new HashSet<>(auditRecord.getNewState().keySet());
            keysToBeRemoved.removeAll(auditRecord.getOldState().keySet());

            linkInstance.getData().putAll(auditRecord.getOldState());
            keysToBeRemoved.forEach(key -> linkInstance.getData().remove(key));

            dataDao.patchData(linkTypeId, linkInstanceId, auditRecord.getOldState());
            auditDao.deleteAuditRecord(auditRecordId);
         }

         linkInstance.setData(constraintManager.decodeDataTypes(linkType, linkInstance.getData()));

         return linkInstanceAdapter.mapLinkInstanceData(linkInstance);
      }

      throw new UnsupportedOperationException("No organization specified.");
   }

   public AuditRecord registerDocumentUpdate(final Document oldDocument, final Document newDocument, final String automation) {
      if (oldDocument == null || newDocument == null || oldDocument.getData() == null || newDocument.getData() == null ||
            !oldDocument.getId().equals(newDocument.getId())) {
         throw new UnsupportedOperationException("Cannot create audit record from different or incomplete documents.");
      }

      final User user = authenticatedUser.getCurrentUser();
      final Collection collection = collectionDao.getCollectionById(oldDocument.getCollectionId());
      final DataDocument oldDataDecoded = constraintManager.decodeDataTypes(collection, oldDocument.getData());
      final DataDocument newDataDecoded = constraintManager.decodeDataTypes(collection, newDocument.getData());

      return auditAdapter.registerUpdate(collection.getId(), ResourceType.DOCUMENT, oldDocument.getId(), user, automation, oldDocument.getData(), oldDataDecoded, newDocument.getData(), newDataDecoded);
   }

   public AuditRecord registerLinkUpdate(final LinkInstance oldLink, final LinkInstance newLink, final String automation) {
      if (oldLink == null || newLink == null || oldLink.getData() == null || newLink.getData() == null ||
            !oldLink.getId().equals(newLink.getId())) {
         throw new UnsupportedOperationException("Cannot create audit record from different or incomplete link instances.");
      }

      final User user = authenticatedUser.getCurrentUser();
      final LinkType linkType = linkTypeDao.getLinkType(oldLink.getLinkTypeId());
      final DataDocument oldDataDecoded = constraintManager.decodeDataTypes(linkType, oldLink.getData());
      final DataDocument newDataDecoded = constraintManager.decodeDataTypes(linkType, newLink.getData());

      return auditAdapter.registerUpdate(linkType.getId(), ResourceType.LINK, oldLink.getId(), user, automation, oldLink.getData(), oldDataDecoded, newLink.getData(), newDataDecoded);
   }

   private List<AuditRecord> decode(final Collection collection, final List<AuditRecord> auditRecords) {
      if (auditRecords != null) {
         auditRecords.forEach(record -> {
            record.setOldState(constraintManager.decodeDataTypes(collection, record.getOldState()));
            record.setNewState(constraintManager.decodeDataTypes(collection, record.getNewState()));
         });
      }

      return auditRecords;
   }

   private List<AuditRecord> decode(final LinkType linkType, final List<AuditRecord> auditRecords) {
      if (auditRecords != null) {
         auditRecords.forEach(record -> {
            record.setOldState(constraintManager.decodeDataTypes(linkType, record.getOldState()));
            record.setNewState(constraintManager.decodeDataTypes(linkType, record.getNewState()));
         });
      }

      return auditRecords;
   }
}
