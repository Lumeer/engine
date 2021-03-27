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
import io.lumeer.api.model.Role;
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.core.adapter.AuditAdapter;
import io.lumeer.core.exception.BadFormatException;
import io.lumeer.core.exception.NoPermissionException;
import io.lumeer.core.util.DocumentUtils;
import io.lumeer.core.exception.UnsupportedOperationException;
import io.lumeer.storage.api.dao.AuditDao;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.LinkTypeDao;

import java.util.HashSet;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
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
   private PaymentFacade paymentFacade;

   private AuditAdapter auditAdapter;

   @PostConstruct
   public void init() {
      auditAdapter = new AuditAdapter(auditDao);
   }

   public List<AuditRecord> getAuditRecordsForDocument(final String collectionId, final String documentId) {
      final Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRoleWithView(collection, Role.WRITE, Role.WRITE);

      if (workspaceKeeper.getOrganization().isPresent()) {
         final ServiceLimits limits = paymentFacade.getCurrentServiceLimits(workspaceKeeper.getOrganization().get());

         return auditAdapter.getAuditRecords(collectionId, ResourceType.DOCUMENT, documentId, limits.getServiceLevel());
      }

      return List.of();
   }

   public List<AuditRecord> getAuditRecordsForLink(final String linkTypeId, final String linkInstanceId) {
      final LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      permissionsChecker.checkLinkTypePermissions(linkType, Role.WRITE, false);

      if (workspaceKeeper.getOrganization().isPresent()) {
         final ServiceLimits limits = paymentFacade.getCurrentServiceLimits(workspaceKeeper.getOrganization().get());

         return auditAdapter.getAuditRecords(linkTypeId, ResourceType.LINK, linkInstanceId, limits.getServiceLevel());
      }

      return List.of();
   }

   public Document revertLastDocumentAuditOperation(final String collectionId, final String documentId, final String auditRecordId) {
      final Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRoleWithView(collection, Role.WRITE, Role.WRITE);

      if (workspaceKeeper.getOrganization().isPresent()) {
         final ServiceLimits limits = paymentFacade.getCurrentServiceLimits(workspaceKeeper.getOrganization().get());

         if (limits.getServiceLevel().equals(Payment.ServiceLevel.FREE)) {
            throw new UnsupportedOperationException("Reverting audit log entries is not available on the free plan.");
         }

         final AuditRecord auditRecord = auditDao.findLatestAuditRecord(collectionId, ResourceType.DOCUMENT, documentId);
         final Document document = DocumentUtils.loadDocumentWithData(documentDao, dataDao, collection, documentId);

         if (auditRecord != null && auditRecord.getId().equals(auditRecordId) && auditRecord.getOldState() != null) {
            // toto není tak jednoduché - musí se smazat přidané
            document.getData().putAll(auditRecord.getOldState());
            dataDao.patchData(collectionId, documentId, auditRecord.getOldState());
            auditDao.deleteAuditRecord(auditRecordId);
         }

         return document;
      }

      throw new UnsupportedOperationException("No organization specified.");
   }

   public LinkInstance revertLastLinkAuditOperation(final String linkTypeId, final String linkInstanceId, final String auditRecordId) {
      final LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      permissionsChecker.checkLinkTypePermissions(linkType, Role.WRITE, false);

      if (workspaceKeeper.getOrganization().isPresent()) {
         final ServiceLimits limits = paymentFacade.getCurrentServiceLimits(workspaceKeeper.getOrganization().get());

         if (limits.getServiceLevel().equals(Payment.ServiceLevel.FREE)) {
            throw new UnsupportedOperationException("Reverting audit log entries is not available on the free plan.");
         }

         final AuditRecord auditRecord = auditDao.findLatestAuditRecord(linkTypeId, ResourceType.LINK, linkInstanceId);

         final LinkInstance linkInstance = null;// load link with data - DocumentUtils.loadDocumentWithData(documentDao, dataDao, linkType, linkInstanceId);

         if (auditRecord != null && auditRecord.getId().equals(auditRecordId) && auditRecord.getOldState() != null) {
            // toto není tak jednoduché - musí se smazat přidané
            var keysToBeRemoved = new HashSet(auditRecord.getNewState().keySet());
            keysToBeRemoved.removeAll(auditRecord.getOldState().keySet());

            linkInstance.getData().putAll(auditRecord.getOldState());
            keysToBeRemoved.forEach(key -> linkInstance.getData().remove(key));

            dataDao.patchData(linkTypeId, linkInstanceId, auditRecord.getOldState());
            auditDao.deleteAuditRecord(auditRecordId);
         }

         return linkInstance;
      }

      throw new UnsupportedOperationException("No organization specified.");
   }

   public AuditRecord registerDocumentUpdate(final Document oldDocument, final Document newDocument, final String automation) {
      if (oldDocument == null || newDocument == null || oldDocument.getData() == null || newDocument.getData() == null ||
            !oldDocument.getId().equals(newDocument.getId())) {
         throw new UnsupportedOperationException("Cannot create audit record from different or incomplete documents.");
      }

      final String user = authenticatedUser.getCurrentUserId();
      final String parentId = oldDocument.getCollectionId();

      return auditAdapter.registerUpdate(parentId, ResourceType.DOCUMENT, oldDocument.getId(), user, automation, oldDocument.getData(), newDocument.getData());
   }

   public AuditRecord registerLinkUpdate(final LinkInstance oldLink, final LinkInstance newLink, final String automation) {
      if (oldLink == null || newLink == null || oldLink.getData() == null || newLink.getData() == null ||
            !oldLink.getId().equals(newLink.getId())) {
         throw new UnsupportedOperationException("Cannot create audit record from different or incomplete link instances.");
      }

      final String user = authenticatedUser.getCurrentUserId();
      final String parentId = oldLink.getLinkTypeId();

      return auditAdapter.registerUpdate(parentId, ResourceType.DOCUMENT, oldLink.getId(), user, automation, oldLink.getData(), newLink.getData());
   }
}
