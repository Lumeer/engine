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
package io.lumeer.storage.mongodb.dao.context;

import io.lumeer.api.SelectedWorkspace;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.storage.api.dao.AuditDao;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.CompanyContactDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DelayedActionDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.FeedbackDao;
import io.lumeer.storage.api.dao.FileAttachmentDao;
import io.lumeer.storage.api.dao.FunctionDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.InformationStoreDao;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.PaymentDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.ResourceCommentDao;
import io.lumeer.storage.api.dao.ResourceVariableDao;
import io.lumeer.storage.api.dao.SelectionListDao;
import io.lumeer.storage.api.dao.SequenceDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.UserLoginDao;
import io.lumeer.storage.api.dao.UserNotificationDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;
import io.lumeer.storage.api.dao.context.WorkspaceSnapshot;
import io.lumeer.storage.mongodb.dao.collection.MongoDataDao;
import io.lumeer.storage.mongodb.dao.collection.MongoLinkDataDao;
import io.lumeer.storage.mongodb.dao.organization.MongoCompanyContactDao;
import io.lumeer.storage.mongodb.dao.organization.MongoFavoriteItemDao;
import io.lumeer.storage.mongodb.dao.organization.MongoInformationStoreDao;
import io.lumeer.storage.mongodb.dao.organization.MongoOrganizationScopedDao;
import io.lumeer.storage.mongodb.dao.organization.MongoPaymentDao;
import io.lumeer.storage.mongodb.dao.organization.MongoProjectDao;
import io.lumeer.storage.mongodb.dao.organization.MongoResourceVariableDao;
import io.lumeer.storage.mongodb.dao.organization.MongoSelectionListDao;
import io.lumeer.storage.mongodb.dao.project.MongoAuditRecordDao;
import io.lumeer.storage.mongodb.dao.project.MongoCollectionDao;
import io.lumeer.storage.mongodb.dao.project.MongoDocumentDao;
import io.lumeer.storage.mongodb.dao.project.MongoFunctionDao;
import io.lumeer.storage.mongodb.dao.project.MongoLinkInstanceDao;
import io.lumeer.storage.mongodb.dao.project.MongoLinkTypeDao;
import io.lumeer.storage.mongodb.dao.project.MongoProjectScopedDao;
import io.lumeer.storage.mongodb.dao.project.MongoResourceCommentDao;
import io.lumeer.storage.mongodb.dao.project.MongoSequenceDao;
import io.lumeer.storage.mongodb.dao.project.MongoViewDao;
import io.lumeer.storage.mongodb.dao.system.MongoDelayedActionDao;
import io.lumeer.storage.mongodb.dao.system.MongoFeedbackDao;
import io.lumeer.storage.mongodb.dao.system.MongoFileAttachmentDao;
import io.lumeer.storage.mongodb.dao.system.MongoGroupDao;
import io.lumeer.storage.mongodb.dao.system.MongoOrganizationDao;
import io.lumeer.storage.mongodb.dao.system.MongoSystemScopedDao;
import io.lumeer.storage.mongodb.dao.system.MongoUserDao;
import io.lumeer.storage.mongodb.dao.system.MongoUserLoginDao;
import io.lumeer.storage.mongodb.dao.system.MongoUserNotificationDao;

import com.mongodb.client.MongoDatabase;

import java.util.concurrent.atomic.LongAdder;

public class MongoDaoContextSnapshot implements DaoContextSnapshot {

   final private MongoDatabase systemDatabase;
   final private MongoDatabase userDatabase;
   final private Organization organization;
   final private Project project;
   final private LongAdder createdDocumentsCounter = new LongAdder();
   final private LongAdder deletedDocumentsCounter = new LongAdder();
   final private LongAdder messageCounter = new LongAdder();
   final private LongAdder emailCounter = new LongAdder();
   final private WorkspaceSnapshot workspaceSnapshot;

   private final OrganizationDao organizationDao;
   private final ProjectDao projectDao;
   private final CollectionDao collectionDao;
   private final CompanyContactDao companyContactDao;
   private final DataDao dataDao;
   private final DocumentDao documentDao;
   private final FavoriteItemDao favoriteItemDao;
   private final FunctionDao functionDao;
   private final FeedbackDao feedbackDao;
   private final GroupDao groupDao;
   private final LinkInstanceDao linkInstanceDao;
   private final LinkDataDao linkDataDao;
   private final LinkTypeDao linkTypeDao;
   private final PaymentDao paymentDao;
   private final UserDao userDao;
   private final UserLoginDao userLoginDao;
   private final UserNotificationDao userNotificationDao;
   private final ViewDao viewDao;
   private final SequenceDao sequenceDao;
   private final ResourceCommentDao resourceCommentDao;
   private final DelayedActionDao delayedActionDao;
   private final AuditDao auditDao;
   private final FileAttachmentDao fileAttachmentDao;
   private final SelectionListDao selectionListDao;
   private final ResourceVariableDao resourceVariableDao;
   private final InformationStoreDao informationStoreDao;

   private MongoDaoContextSnapshot(final MongoDaoContextSnapshot originalDao) {
      this.systemDatabase = originalDao.systemDatabase;
      this.userDatabase = originalDao.userDatabase;

      this.organization = originalDao.organization;
      this.project = originalDao.project;
      this.workspaceSnapshot = originalDao.workspaceSnapshot;

      this.organizationDao = originalDao.organizationDao;
      this.projectDao = originalDao.projectDao;
      this.collectionDao = originalDao.collectionDao;
      this.companyContactDao = originalDao.companyContactDao;
      this.dataDao = originalDao.dataDao;
      this.documentDao = originalDao.documentDao;
      this.favoriteItemDao = originalDao.favoriteItemDao;
      this.functionDao = originalDao.functionDao;
      this.feedbackDao = originalDao.feedbackDao;
      this.groupDao = originalDao.groupDao;
      this.linkInstanceDao = originalDao.linkInstanceDao;
      this.linkDataDao = originalDao.linkDataDao;
      this.linkTypeDao = originalDao.linkTypeDao;
      this.paymentDao = originalDao.paymentDao;
      this.userDao = originalDao.userDao;
      this.userLoginDao = originalDao.userLoginDao;
      this.userNotificationDao = originalDao.userNotificationDao;
      this.viewDao = originalDao.viewDao;
      this.sequenceDao = originalDao.sequenceDao;
      this.resourceCommentDao = originalDao.resourceCommentDao;
      this.delayedActionDao = originalDao.delayedActionDao;
      this.auditDao = originalDao.auditDao;
      this.fileAttachmentDao = originalDao.fileAttachmentDao;
      this.selectionListDao = originalDao.selectionListDao;
      this.resourceVariableDao = originalDao.resourceVariableDao;
      this.informationStoreDao = originalDao.informationStoreDao;
   }

   MongoDaoContextSnapshot(final DataStorage systemDataStorage, final DataStorage userDataStorage, final SelectedWorkspace selectedWorkspace) {
      this.systemDatabase = (MongoDatabase) systemDataStorage.getDatabase();
      this.userDatabase = (MongoDatabase) userDataStorage.getDatabase();

      if (selectedWorkspace.getOrganization().isPresent()) {
         this.organization = selectedWorkspace.getOrganization().get();
      } else {
         this.organization = null;
      }

      if (selectedWorkspace.getProject().isPresent()) {
         this.project = selectedWorkspace.getProject().get();
      } else {
         this.project = null;
      }

      workspaceSnapshot = new WorkspaceSnapshot(organization, project);

      this.organizationDao = initSystemScopedDao(new MongoOrganizationDao());
      this.projectDao = initOrganizationScopedDao(new MongoProjectDao());
      this.collectionDao = initProjectScopedDao(new MongoCollectionDao());
      this.companyContactDao = initSystemScopedDao(new MongoCompanyContactDao());
      this.dataDao = initProjectScopedDao(new MongoDataDao());
      this.documentDao = initProjectScopedDao(new MongoDocumentDao());
      this.favoriteItemDao = initOrganizationScopedDao(new MongoFavoriteItemDao());
      this.functionDao = initProjectScopedDao(new MongoFunctionDao());
      this.feedbackDao = initSystemScopedDao(new MongoFeedbackDao());
      this.groupDao = initOrganizationScopedDao(new MongoGroupDao());
      this.linkInstanceDao = initProjectScopedDao(new MongoLinkInstanceDao());
      this.linkDataDao = initProjectScopedDao(new MongoLinkDataDao());
      this.linkTypeDao = initProjectScopedDao(new MongoLinkTypeDao());
      this.paymentDao = initOrganizationScopedDao(new MongoPaymentDao());
      this.userDao = initSystemScopedDao(new MongoUserDao());
      this.userLoginDao = initSystemScopedDao(new MongoUserLoginDao());
      this.userNotificationDao = initSystemScopedDao(new MongoUserNotificationDao());
      this.viewDao = initProjectScopedDao(new MongoViewDao());
      this.sequenceDao = initProjectScopedDao(new MongoSequenceDao());
      this.resourceCommentDao = initProjectScopedDao(new MongoResourceCommentDao());
      this.delayedActionDao = initSystemScopedDao(new MongoDelayedActionDao());
      this.auditDao = initProjectScopedDao(new MongoAuditRecordDao());
      this.fileAttachmentDao = initSystemScopedDao(new MongoFileAttachmentDao());
      this.selectionListDao = initOrganizationScopedDao(new MongoSelectionListDao());
      this.resourceVariableDao = initOrganizationScopedDao(new MongoResourceVariableDao());
      this.informationStoreDao = initOrganizationScopedDao(new MongoInformationStoreDao());
   }

   private <T extends MongoSystemScopedDao> T initSystemScopedDao(T dao) {
      dao.setDatabase(systemDatabase);
      return dao;
   }

   private <T extends MongoOrganizationScopedDao> T initOrganizationScopedDao(T dao) {
      dao.setDatabase(userDatabase);
      dao.setOrganization(organization);
      return dao;
   }

   private <T extends MongoProjectScopedDao> T initProjectScopedDao(T dao) {
      dao.setDatabase(userDatabase);
      dao.setOrganization(organization);
      dao.setProject(project);
      return dao;
   }

   @Override
   public Organization getOrganization() {
      return organization;
   }

   @Override
   public Project getProject() {
      return project;
   }

   public String getOrganizationId() {
      return organization != null ? organization.getId() : null;
   }

   public String getProjectId() {
      return project != null ? project.getId() : null;
   }

   @Override
   public OrganizationDao getOrganizationDao() {
      return organizationDao;
   }

   @Override
   public ProjectDao getProjectDao() {
      return projectDao;
   }

   @Override
   public CollectionDao getCollectionDao() {
      return collectionDao;
   }

   @Override
   public CompanyContactDao getCompanyContactDao() {
      return companyContactDao;
   }

   @Override
   public DataDao getDataDao() {
      return dataDao;
   }

   @Override
   public DocumentDao getDocumentDao() {
      return documentDao;
   }

   @Override
   public FavoriteItemDao getFavoriteItemDao() {
      return favoriteItemDao;
   }

   @Override
   public FunctionDao getFunctionDao() {
      return functionDao;
   }

   @Override
   public FeedbackDao getFeedbackDao() {
      return feedbackDao;
   }

   @Override
   public GroupDao getGroupDao() {
      return groupDao;
   }

   @Override
   public LinkInstanceDao getLinkInstanceDao() {
      return linkInstanceDao;
   }

   @Override
   public LinkDataDao getLinkDataDao() {
      return linkDataDao;
   }

   @Override
   public LinkTypeDao getLinkTypeDao() {
      return linkTypeDao;
   }

   @Override
   public PaymentDao getPaymentDao() {
      return paymentDao;
   }

   @Override
   public UserDao getUserDao() {
      return userDao;
   }

   @Override
   public UserLoginDao getUserLoginDao() {
      return userLoginDao;
   }

   @Override
   public UserNotificationDao getUserNotificationDao() {
      return userNotificationDao;
   }

   @Override
   public ViewDao getViewDao() {
      return viewDao;
   }

   @Override
   public SequenceDao getSequenceDao() {
      return sequenceDao;
   }

   @Override
   public ResourceCommentDao getResourceCommentDao() {
      return resourceCommentDao;
   }

   @Override
   public DelayedActionDao getDelayedActionDao() {
      return delayedActionDao;
   }

   @Override
   public AuditDao getAuditDao() {
      return auditDao;
   }

   @Override
   public SelectionListDao getSelectionListDao() {
      return selectionListDao;
   }

   @Override
   public FileAttachmentDao getFileAttachmentDao() {
      return fileAttachmentDao;
   }

   @Override
   public SelectedWorkspace getSelectedWorkspace() {
      return workspaceSnapshot;
   }

   @Override
   public ResourceVariableDao getResourceVariableDao() {
      return resourceVariableDao;
   }

   @Override
   public InformationStoreDao getInformationStoreDao() {
      return informationStoreDao;
   }

   @Override
   public long increaseCreationCounter() {
      createdDocumentsCounter.increment();
      return createdDocumentsCounter.longValue();
   }

   @Override
   public long increaseDeletionCounter() {
      deletedDocumentsCounter.increment();
      return deletedDocumentsCounter.longValue();
   }

   @Override
   public long increaseMessageCounter() {
      messageCounter.increment();
      return messageCounter.longValue();
   }

   @Override
   public long increaseEmailCounter() {
      emailCounter.increment();
      return emailCounter.longValue();
   }

   @Override
   public DaoContextSnapshot shallowCopy() {
      return new MongoDaoContextSnapshot(this);
   }
}
