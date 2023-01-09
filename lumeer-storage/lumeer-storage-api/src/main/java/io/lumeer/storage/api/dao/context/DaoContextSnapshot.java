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
package io.lumeer.storage.api.dao.context;

import io.lumeer.api.SelectedWorkspace;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.storage.api.dao.*;

/**
 * Holds contextual information necessary to create any Dao object in the application.
 * This is useful in batch processing tasks and long running tasks that are executed out
 * of any CDI scope. In such cases, the injection does not work and we must create
 * a snapshot while inside of a CDI scope.
 * Please remember that nothing that gets injected into Dao objects will work. This includes
 * mainly invocation of CDI events that lead to creation of Push and User Notifications.
 * If it is desired to send these notifications, they must be created manually in the batch task.
 */
public interface DaoContextSnapshot {

   Organization getOrganization();

   Project getProject();

   String getOrganizationId();

   String getProjectId();

   OrganizationDao getOrganizationDao();

   ProjectDao getProjectDao();

   CollectionDao getCollectionDao();

   CompanyContactDao getCompanyContactDao();

   DataDao getDataDao();

   DocumentDao getDocumentDao();

   FavoriteItemDao getFavoriteItemDao();

   FeedbackDao getFeedbackDao();

   FunctionDao getFunctionDao();

   GroupDao getGroupDao();

   LinkInstanceDao getLinkInstanceDao();

   LinkDataDao getLinkDataDao();

   LinkTypeDao getLinkTypeDao();

   PaymentDao getPaymentDao();

   UserDao getUserDao();

   UserLoginDao getUserLoginDao();

   UserNotificationDao getUserNotificationDao();

   ViewDao getViewDao();

   SequenceDao getSequenceDao();

   ResourceCommentDao getResourceCommentDao();

   DelayedActionDao getDelayedActionDao();

   AuditDao getAuditDao();

   FileAttachmentDao getFileAttachmentDao();

   SelectionListDao getSelectionListDao();

   ResourceVariableDao getResourceVariableDao();

   InformationStoreDao getInformationStoreDao();

   SelectedWorkspace getSelectedWorkspace();

   long increaseCreationCounter();
   long increaseDeletionCounter();
   long increaseMessageCounter();
   long increaseEmailCounter();

   DaoContextSnapshot shallowCopy();
}
