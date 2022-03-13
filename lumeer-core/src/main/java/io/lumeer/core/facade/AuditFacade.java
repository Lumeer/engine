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

import static java.util.stream.Collectors.*;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.AuditRecord;
import io.lumeer.api.model.AuditType;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Constraint;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Payment;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.ServiceLimits;
import io.lumeer.api.model.User;
import io.lumeer.api.model.common.Resource;
import io.lumeer.api.util.CollectionUtil;
import io.lumeer.core.adapter.AuditAdapter;
import io.lumeer.core.adapter.CollectionAdapter;
import io.lumeer.core.adapter.DocumentAdapter;
import io.lumeer.core.adapter.LinkInstanceAdapter;
import io.lumeer.core.adapter.LinkTypeAdapter;
import io.lumeer.core.adapter.PermissionAdapter;
import io.lumeer.core.adapter.PusherAdapter;
import io.lumeer.core.adapter.ResourceAdapter;
import io.lumeer.core.auth.RequestDataKeeper;
import io.lumeer.core.constraint.ConstraintManager;
import io.lumeer.core.exception.UnsupportedOperationException;
import io.lumeer.core.facade.configuration.DefaultConfigurationProducer;
import io.lumeer.core.util.DocumentUtils;
import io.lumeer.core.util.LinkInstanceUtils;
import io.lumeer.core.util.PusherClient;
import io.lumeer.core.util.Utils;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.event.CreateDocument;
import io.lumeer.engine.api.event.CreateLinkInstance;
import io.lumeer.engine.api.event.RemoveDocument;
import io.lumeer.engine.api.event.RemoveLinkInstance;
import io.lumeer.engine.api.event.UpdateDefaultWorkspace;
import io.lumeer.engine.api.event.UpdateDocument;
import io.lumeer.engine.api.event.UpdateLinkInstance;
import io.lumeer.storage.api.dao.AuditDao;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.ResourceCommentDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.ViewDao;

import org.marvec.pusher.data.Event;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
   private ViewDao viewDao;

   @Inject
   private UserDao userDao;

   @Inject
   private GroupDao groupDao;

   @Inject
   private DefaultConfigurationProducer configurationProducer;

   @Inject
   private RequestDataKeeper requestDataKeeper;

   @Inject
   private PusherHelperFacade pusherHelperFacade;

   private AuditAdapter auditAdapter;
   private DocumentAdapter documentAdapter;
   private ConstraintManager constraintManager;
   private LinkInstanceAdapter linkInstanceAdapter;
   private ResourceAdapter resourceAdapter;
   private CollectionAdapter collectionAdapter;
   private LinkTypeAdapter linkTypeAdapter;
   private PusherAdapter pusherAdapter;
   private PusherClient pusherClient = null;

   @PostConstruct
   public void init() {
      constraintManager = ConstraintManager.getInstance(configurationProducer);
      pusherClient = pusherHelperFacade.getPusherClient();
      auditAdapter = new AuditAdapter(auditDao);

      documentAdapter = new DocumentAdapter(resourceCommentDao, favoriteItemDao);
      linkInstanceAdapter = new LinkInstanceAdapter(resourceCommentDao);

      PermissionAdapter permissionAdapter = new PermissionAdapter(userDao, groupDao, viewDao, linkTypeDao, collectionDao);
      resourceAdapter = new ResourceAdapter(permissionAdapter, collectionDao, linkTypeDao, viewDao, userDao);

      collectionAdapter = new CollectionAdapter(collectionDao, favoriteItemDao, documentDao);
      linkTypeAdapter = new LinkTypeAdapter(linkTypeDao, linkInstanceDao);

      pusherAdapter = new PusherAdapter(requestDataKeeper.getAppId(), getFacadeAdapter(), resourceAdapter, permissionAdapter, viewDao, linkTypeDao, collectionDao);
   }

   public void workspaceUpdated(@Observes final UpdateDefaultWorkspace updateDefaultWorkspace) {
      registerProjectEnter(updateDefaultWorkspace.getOrganization(), updateDefaultWorkspace.getProject());
   }

   public void documentCreated(@Observes final CreateDocument createDocument) {
      registerDocumentCreate(createDocument.getDocument());
   }

   public void documentUpdated(@Observes final UpdateDocument updateDocument) {
      registerDocumentUpdate(updateDocument.getOriginalDocument(), updateDocument.getDocument());
   }

   public void documentRemoved(@Observes final RemoveDocument removeDocument) {
      registerDocumentDelete(removeDocument.getDocument());
   }

   public void linkInstanceCreated(@Observes final CreateLinkInstance createLinkInstance) {
      registerLinkCreate(createLinkInstance.getLinkInstance());
   }

   public void linkInstanceUpdated(@Observes final UpdateLinkInstance updateLinkInstance) {
      registerLinkUpdate(updateLinkInstance.getOriginalLinkInstance(), updateLinkInstance.getLinkInstance());
   }

   public void linkInstanceRemoved(@Observes final RemoveLinkInstance removeLinkInstance) {
      registerLinkDelete(removeLinkInstance.getLinkInstance());
   }

   public List<AuditRecord> getAuditRecordsForProject() {
      checkProjectRole(RoleType.Manage);

      Map<String, Collection> collectionsMap = resourceAdapter.getCollections(getOrganization(), getProject(), getCurrentUserId())
                                                              .stream().collect(Collectors.toMap(Resource::getId, c -> c));
      Map<String, LinkType> linkTypesMap = resourceAdapter.getLinkTypes(getOrganization(), getProject(), getCurrentUserId())
                                                          .stream().collect(Collectors.toMap(LinkType::getId, c -> c));
      // currently not supported
      Set<String> viewIds = Collections.emptySet();

      List<AuditRecord> auditRecords = auditAdapter.getAuditRecords(collectionsMap.keySet(), linkTypesMap.keySet(), viewIds, getServiceLevel());

      collectionsMap.values().forEach(collection -> {
         var collectionAuditRecords = auditRecords.stream()
                                                  .filter(record -> ResourceType.DOCUMENT.equals(record.getResourceType()) && collection.getId().equals(record.getParentId()))
                                                  .collect(toList());
         decodeWithTitle(collection, collectionAuditRecords);
      });

      linkTypesMap.values().forEach(linkType -> {
         var linkAuditRecords = auditRecords.stream()
                                            .filter(record -> ResourceType.LINK.equals(record.getResourceType()) && linkType.getId().equals(record.getParentId()))
                                            .collect(toList());
         decodeWithTitle(linkType, linkAuditRecords);
      });

      return auditRecords;
   }

   public List<AuditRecord> getAuditRecordsForCollection(final String collectionId) {
      Collection collection = collectionDao.getCollectionById(collectionId);
      permissionsChecker.checkRole(collection, RoleType.Manage);

      return decodeWithTitle(collection, auditAdapter.getAuditRecords(collectionId, ResourceType.DOCUMENT, getServiceLevel()));
   }

   public List<AuditRecord> getAuditRecordsForLinkType(final String linkTypeId) {
      LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      permissionsChecker.checkRoleInLinkType(linkType, RoleType.Manage);

      return decodeWithTitle(linkType, auditAdapter.getAuditRecords(linkTypeId, ResourceType.LINK, getServiceLevel()));
   }

   public List<AuditRecord> getAuditRecordsForDocument(final String collectionId, final String documentId) {
      final Collection collection = collectionDao.getCollectionById(collectionId);
      final Document document = DocumentUtils.loadDocumentWithData(documentDao, dataDao, collection, documentId);
      permissionsChecker.checkEditDocument(collection, document);

      return auditAdapter.getAuditRecords(collectionId, ResourceType.DOCUMENT, documentId, getServiceLevel())
                         .stream().peek(log -> decode(collection, log))
                         .collect(toList());
   }

   public List<AuditRecord> getAuditRecordsForLink(final String linkTypeId, final String linkInstanceId) {
      final LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      final LinkInstance linkInstance = LinkInstanceUtils.loadLinkInstanceWithData(linkInstanceDao, linkDataDao, linkInstanceId);
      permissionsChecker.checkEditLinkInstance(linkType, linkInstance);

      return auditAdapter.getAuditRecords(linkTypeId, ResourceType.LINK, linkInstanceId, getServiceLevel())
                         .stream().peek(link -> decode(linkType, link))
                         .collect(toList());
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
         var keysToAdd = new HashSet<>(auditRecord.getOldState().keySet());
         keysToAdd.removeAll(auditRecord.getNewState().keySet());

         var keysToRemove = new HashSet<>(auditRecord.getNewState().keySet());
         keysToRemove.removeAll(auditRecord.getOldState().keySet());

         document.getData().putAll(auditRecord.getOldState());
         keysToRemove.forEach(key -> document.getData().remove(key));

         DataDocument storedData = dataDao.updateData(auditRecord.getParentId(), auditRecord.getResourceId(), document.getData());
         checkAuditOnObjectRevert(auditRecord);

         document.setUpdatedBy(getCurrentUserId());
         document.setUpdateDate(ZonedDateTime.now());

         Document storedDocument = documentDao.updateDocument(document.getId(), document);
         storedDocument.setData(storedData);

         collectionAdapter.updateCollectionMetadata(collection, keysToAdd, keysToRemove);

         sendDocumentPushNotifications(collection, storedDocument, PusherFacade.UPDATE_EVENT_SUFFIX);
      }
   }

   private void checkAuditOnObjectRevert(final AuditRecord auditRecord) {
      auditDao.deleteAuditRecord(auditRecord.getId());

      var currentUser = authenticatedUser.getCurrentUser();
      if (!currentUser.getId().equals(auditRecord.getUser())) {
         auditAdapter.registerRevert(auditRecord.getParentId(), auditRecord.getResourceType(), auditRecord.getResourceId(), currentUser, null, null, auditRecord.getNewState(), auditRecord.getOldState());
      }
   }

   private void sendDocumentPushNotifications(final Collection collection, final Document document, final String event) {
      document.setData(constraintManager.decodeDataTypes(collection, document.getData()));

      List<Event> events = resourceAdapter.getDocumentReaders(getOrganization(), getProject(), collection, document)
                                          .stream().map(userId -> {
               var userDocument = documentAdapter.mapDocumentData(document, getCurrentUserId(), workspaceKeeper.getProjectId());
               return pusherAdapter.createEvent(getOrganization(), getProject(), userDocument, event, userId);
            }).collect(toList());

      sendPushNotification(events);
   }

   private void sendPushNotification(final List<Event> events) {
      if (pusherClient != null && events.size() > 0) {
         pusherClient.trigger(events);
      }
   }

   private void revertDocumentDelete(final AuditRecord requestedRecord) {
      final AuditRecord auditRecord = auditDao.findLatestAuditRecord(requestedRecord.getParentId(), ResourceType.DOCUMENT, requestedRecord.getResourceId(), AuditType.Deleted);
      if (auditRecord == null || !auditRecord.getId().equals(requestedRecord.getId())) {
         throw new UnsupportedOperationException("Cannot revert audit record that is not the last.");
      }

      final Collection collection = collectionDao.getCollectionById(auditRecord.getParentId());
      permissionsChecker.checkCreateDocuments(collection);

      final Document document = new Document(collection.getId(), ZonedDateTime.now(), getCurrentUserId());
      document.setData(auditRecord.getOldState());
      permissionsChecker.checkDocumentLimits(document);

      final Document storedDocument = documentDao.createDocument(document);
      DataDocument storedData = dataDao.createData(collection.getId(), storedDocument.getId(), document.getData());
      storedDocument.setData(storedData);

      collectionAdapter.updateCollectionMetadata(collection, document.getData().keySet(), Collections.emptySet());
      sendDocumentPushNotifications(collection, storedDocument, PusherFacade.CREATE_EVENT_SUFFIX);
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
         var keysToAdd = new HashSet<>(auditRecord.getOldState().keySet());
         keysToAdd.removeAll(auditRecord.getNewState().keySet());

         var keysToRemove = new HashSet<>(auditRecord.getNewState().keySet());
         keysToRemove.removeAll(auditRecord.getOldState().keySet());

         linkInstance.getData().putAll(auditRecord.getOldState());
         keysToRemove.forEach(key -> linkInstance.getData().remove(key));

         DataDocument storedData = linkDataDao.updateData(auditRecord.getParentId(), auditRecord.getResourceId(), linkInstance.getData());
         checkAuditOnObjectRevert(auditRecord);

         linkInstance.setUpdateDate(ZonedDateTime.now());
         linkInstance.setUpdatedBy(getCurrentUserId());

         LinkInstance storedLinkInstance = linkInstanceDao.updateLinkInstance(linkInstance.getId(), linkInstance);
         storedLinkInstance.setData(storedData);

         linkTypeAdapter.updateLinkTypeMetadata(linkType, keysToAdd, keysToRemove);

         sendLinkNotification(linkType, storedLinkInstance, PusherFacade.UPDATE_EVENT_SUFFIX);
      }
   }

   private void sendLinkNotification(final LinkType linkType, final LinkInstance linkInstance, final String event) {
      linkInstance.setData(constraintManager.decodeDataTypes(linkType, linkInstance.getData()));

      List<Event> events = resourceAdapter.getLinkTypeReaders(getOrganization(), getProject(), linkType)
                                          .stream().map(userId -> {
               var userLinkInstance = linkInstanceAdapter.mapLinkInstanceData(linkInstance);
               return pusherAdapter.createEvent(getOrganization(), getProject(), userLinkInstance, event, userId);
            }).collect(toList());

      sendPushNotification(events);
   }

   private void registerProjectEnter(final Organization organization, final Project project) {
      auditDao.setProject(project);

      auditAdapter.registerEnter(organization.getId(), ResourceType.PROJECT, project.getId(), authenticatedUser.getCurrentUser());
   }

   private AuditRecord registerDocumentCreate(final Document newDocument) {
      if (newDocument == null) {
         throw new UnsupportedOperationException("Cannot create audit record from different or incomplete documents.");
      }

      final User user = authenticatedUser.getCurrentUser();
      final Collection collection = collectionDao.getCollectionById(newDocument.getCollectionId());
      final DataDocument newDataDecoded = constraintManager.decodeDataTypes(collection, newDocument.getData());

      return auditAdapter.registerCreate(collection.getId(), ResourceType.DOCUMENT, newDocument.getId(), user, null, getCurrentViewId(), newDataDecoded);
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

      return auditAdapter.registerDataChange(collection.getId(), ResourceType.DOCUMENT, oldDocument.getId(), user, null, getCurrentViewId(), oldDocument.getData(), oldDataDecoded, newDocument.getData(), newDataDecoded);
   }

   private AuditRecord registerDocumentDelete(final Document oldDocument) {
      if (oldDocument == null) {
         throw new UnsupportedOperationException("Cannot create audit record from different or incomplete documents.");
      }

      final User user = authenticatedUser.getCurrentUser();
      final Collection collection = collectionDao.getCollectionById(oldDocument.getCollectionId());

      return auditAdapter.registerDelete(collection.getId(), ResourceType.DOCUMENT, oldDocument.getId(), user, null, getCurrentViewId(), oldDocument.getData());
   }

   private AuditRecord registerLinkCreate(final LinkInstance newLink) {
      if (newLink == null) {
         throw new UnsupportedOperationException("Cannot create audit record from different or incomplete link instances.");
      }

      final User user = authenticatedUser.getCurrentUser();
      final LinkType linkType = linkTypeDao.getLinkType(newLink.getLinkTypeId());
      final DataDocument newDataDecoded = constraintManager.decodeDataTypes(linkType, newLink.getData());

      return auditAdapter.registerCreate(linkType.getId(), ResourceType.LINK, newLink.getId(), user, null, getCurrentViewId(), newDataDecoded);
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

      return auditAdapter.registerDataChange(linkType.getId(), ResourceType.LINK, oldLink.getId(), user, null, getCurrentViewId(), oldLink.getData(), oldDataDecoded, newLink.getData(), newDataDecoded);
   }

   private AuditRecord registerLinkDelete(final LinkInstance oldLink) {
      if (oldLink == null) {
         throw new UnsupportedOperationException("Cannot create audit record from different or incomplete link instances.");
      }

      final User user = authenticatedUser.getCurrentUser();
      final LinkType linkType = linkTypeDao.getLinkType(oldLink.getLinkTypeId());

      return auditAdapter.registerDelete(linkType.getId(), ResourceType.LINK, oldLink.getId(), user, null, getCurrentViewId(), oldLink.getData());
   }

   private List<AuditRecord> decodeWithTitle(final Collection collection, final List<AuditRecord> auditRecords) {
      var documentsIds = auditRecords.stream().map(AuditRecord::getResourceId)
                                     .collect(toSet());

      var defaultAttribute = CollectionUtil.getDefaultAttribute(collection);
      var defaultAttributeId = defaultAttribute != null ? defaultAttribute.getId() : "";
      var defaultConstraint = Utils.computeIfNotNull(defaultAttribute, Attribute::getConstraint);

      Map<String, Object> defaultValues = Collections.emptyMap();
      if (!defaultAttributeId.isEmpty()) {
         defaultValues = dataDao.getData(collection.getId(), documentsIds, defaultAttributeId)
                                .stream().collect(toMap(DataDocument::getId, d -> d.get(defaultAttributeId)));
      }

      for (AuditRecord auditRecord : auditRecords) {
         decode(collection, auditRecord);
         decodeTitle(auditRecord, defaultAttributeId, defaultConstraint, defaultValues);
      }

      return auditRecords;
   }

   private void decode(final Collection collection, final AuditRecord record) {
      if (AuditType.Updated.equals(record.getType()) || AuditType.Deleted.equals(record.getType())) {
         record.setOldState(constraintManager.decodeDataTypes(collection, record.getOldState()));

      }
      if (AuditType.Updated.equals(record.getType())) {
         record.setNewState(constraintManager.decodeDataTypes(collection, record.getNewState()));
      }
   }

   private List<AuditRecord> decodeWithTitle(final LinkType linkType, final List<AuditRecord> auditRecords) {
      var linkIds = auditRecords.stream().map(AuditRecord::getResourceId)
                                .collect(toSet());

      var defaultAttribute = linkType.getAttributes().stream().findFirst().orElse(null);
      var defaultAttributeId = defaultAttribute != null ? defaultAttribute.getId() : "";
      var defaultConstraint = Utils.computeIfNotNull(defaultAttribute, Attribute::getConstraint);

      Map<String, Object> defaultValues = Collections.emptyMap();
      if (!defaultAttributeId.isEmpty()) {
         defaultValues = linkDataDao.getData(linkType.getId(), linkIds, defaultAttributeId)
                                    .stream().collect(toMap(DataDocument::getId, d -> d.get(defaultAttributeId)));
      }

      for (AuditRecord auditRecord : auditRecords) {
         decode(linkType, auditRecord);
         decodeTitle(auditRecord, defaultAttributeId, defaultConstraint, defaultValues);
      }

      return auditRecords;
   }

   private void decodeTitle(final AuditRecord auditRecord, final String defaultAttributeId, final Constraint defaultConstraint, final Map<String, Object> defaultValues) {
      Object title;
      if (AuditType.Deleted.equals(auditRecord.getType())) {
         title = Utils.computeIfNotNull(auditRecord.getOldState(), state -> state.get(defaultAttributeId));
      } else {
         title = defaultValues.get(auditRecord.getResourceId());
      }

      var titleDecoded = constraintManager.decode(title, defaultConstraint);
      auditRecord.setTitle(titleDecoded);
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

   private void checkProjectRole(RoleType role) {
      permissionsChecker.checkRole(getProject(), role);
   }
}
