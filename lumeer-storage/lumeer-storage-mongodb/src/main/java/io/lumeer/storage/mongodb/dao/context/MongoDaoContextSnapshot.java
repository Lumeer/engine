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
package io.lumeer.storage.mongodb.dao.context;

import io.lumeer.api.SelectedWorkspace;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.CompanyContactDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.FavoriteItemDao;
import io.lumeer.storage.api.dao.FeedbackDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.PaymentDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.UserLoginDao;
import io.lumeer.storage.api.dao.UserNotificationDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;
import io.lumeer.storage.mongodb.dao.collection.MongoDataDao;
import io.lumeer.storage.mongodb.dao.organization.MongoCompanyContactDao;
import io.lumeer.storage.mongodb.dao.organization.MongoFavoriteItemDao;
import io.lumeer.storage.mongodb.dao.organization.MongoPaymentDao;
import io.lumeer.storage.mongodb.dao.organization.MongoProjectDao;
import io.lumeer.storage.mongodb.dao.organization.OrganizationScopedDao;
import io.lumeer.storage.mongodb.dao.project.MongoCollectionDao;
import io.lumeer.storage.mongodb.dao.project.MongoDocumentDao;
import io.lumeer.storage.mongodb.dao.project.MongoLinkInstanceDao;
import io.lumeer.storage.mongodb.dao.project.MongoLinkTypeDao;
import io.lumeer.storage.mongodb.dao.project.MongoViewDao;
import io.lumeer.storage.mongodb.dao.project.ProjectScopedDao;
import io.lumeer.storage.mongodb.dao.system.MongoFeedbackDao;
import io.lumeer.storage.mongodb.dao.system.MongoGroupDao;
import io.lumeer.storage.mongodb.dao.system.MongoOrganizationDao;
import io.lumeer.storage.mongodb.dao.system.MongoUserDao;
import io.lumeer.storage.mongodb.dao.system.MongoUserLoginDao;
import io.lumeer.storage.mongodb.dao.system.MongoUserNotificationDao;
import io.lumeer.storage.mongodb.dao.system.SystemScopedDao;

import com.mongodb.client.MongoDatabase;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class MongoDaoContextSnapshot implements DaoContextSnapshot {

   final private MongoDatabase systemDatabase;
   final private MongoDatabase userDatabase;
   final private Organization organization;
   final private Project project;

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
   }

   private <T extends SystemScopedDao> T initSystemScopedDao(T dao) {
      dao.setDatabase(systemDatabase);
      return dao;
   }

   private <T extends OrganizationScopedDao> T initOrganizationScopedDao(T dao) {
      dao.setDatabase(userDatabase);
      dao.setOrganization(organization);
      return dao;
   }

   private <T extends ProjectScopedDao> T initProjectScopedDao(T dao) {
      dao.setDatabase(userDatabase);
      dao.setOrganization(organization);
      dao.setProject(project);
      return dao;
   }

   @Override
   public OrganizationDao getOrganizationDao() {
      return initSystemScopedDao(new MongoOrganizationDao());
   }

   @Override
   public ProjectDao getProjectDao() {
      return initOrganizationScopedDao(new MongoProjectDao());
   }

   @Override
   public CollectionDao getCollectionDao() {
      return initProjectScopedDao(new MongoCollectionDao());
   }

   @Override
   public CompanyContactDao getCompanyContactDao() {
      return initSystemScopedDao(new MongoCompanyContactDao());
   }

   @Override
   public DataDao getDataDao() {
      return initProjectScopedDao(new MongoDataDao());
   }

   @Override
   public DocumentDao getDocumentDao() {
      return initProjectScopedDao(new MongoDocumentDao());
   }

   @Override
   public FavoriteItemDao getFavoriteItemDao() {
      return initOrganizationScopedDao(new MongoFavoriteItemDao());
   }

   @Override
   public FeedbackDao getFeedbackDao() {
      return initSystemScopedDao(new MongoFeedbackDao());
   }

   @Override
   public GroupDao getGroupDao() {
      return initOrganizationScopedDao(new MongoGroupDao());
   }

   @Override
   public LinkInstanceDao getLinkInstanceDao() {
      return initProjectScopedDao(new MongoLinkInstanceDao());
   }

   @Override
   public LinkTypeDao getLinkTypeDao() {
      return initProjectScopedDao(new MongoLinkTypeDao());
   }

   @Override
   public PaymentDao getPaymentDao() {
      return initSystemScopedDao(new MongoPaymentDao());
   }

   @Override
   public UserDao getUserDao() {
      return initSystemScopedDao(new MongoUserDao());
   }

   @Override
   public UserLoginDao getUserLoginDao() {
      return initSystemScopedDao(new MongoUserLoginDao());
   }

   @Override
   public UserNotificationDao getUserNotificationDao() {
      return initSystemScopedDao(new MongoUserNotificationDao());
   }

   @Override
   public ViewDao getViewDao() {
      return initProjectScopedDao(new MongoViewDao());
   }
}
