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
package io.lumeer.core.task;

import io.lumeer.api.model.AllowedPermissions;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Rule;
import io.lumeer.api.model.User;
import io.lumeer.api.model.View;
import io.lumeer.api.model.rule.CronRule;
import io.lumeer.core.WorkspaceContext;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.util.CronTaskChecker;
import io.lumeer.core.util.DocumentUtils;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

@Singleton
@Startup
public class CronTaskProcessor extends WorkspaceContext {

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private TaskExecutor taskExecutor;

   private final CronTaskChecker checker = new CronTaskChecker();

   @Schedule(hour = "*", minute = "*/2") // every single hour
   public void process() {
      final List<Organization> organizations = organizationDao.getAllOrganizations();

      organizations.forEach(organization -> {
         final DataStorage userDataStorage = getDataStorage(organization.getId());

         final DaoContextSnapshot orgDao = getDaoContextSnapshot(userDataStorage, new Workspace(organization, null));
         final List<Project> projects = orgDao.getProjectDao().getAllProjects();

         projects.forEach(project -> {
            final DaoContextSnapshot projDao = getDaoContextSnapshot(userDataStorage, new Workspace(organization, project));
            final ContextualTaskFactory taskFactory = getTaskFactory(projDao);

            final List<Collection> collections = projDao.getCollectionDao().getAllCollections();
            collections.forEach(collection -> processRules(projDao, collection, taskFactory));
         });
      });
   }

   private void processRules(final DaoContextSnapshot dao, final Collection collection, final ContextualTaskFactory taskFactory) {
      collection.getRules().entrySet().stream().filter(e -> e.getValue().getType() == Rule.RuleType.CRON).forEach(entry -> {
         final CronRule rule = new CronRule(entry.getValue());
         if (checker.shouldExecute(rule, ZonedDateTime.now())) {
            final String signature = UUID.randomUUID().toString();
            rule.setLastRun(ZonedDateTime.now());
            rule.setExecuting(signature);

            final Collection bookedCollection = dao.getCollectionDao().updateCollectionRules(collection);

            // is it us who tried last?
            final String executing = new CronRule(bookedCollection.getRules().get(entry.getKey())).getExecuting();
            System.out.println("executing: " + executing + " // " + rule.getRule().getName());
            if (executing == null || "".equals(executing)) {

               final List<Document> documents = getDocuments(rule, collection, dao);

               taskExecutor.submitTask(
                     getTask(
                           taskFactory,
                           entry.getValue().getName() != null ? entry.getValue().getName() : entry.getKey(),
                           entry.getValue(),
                           collection,
                           documents
                     )
               );

               rule.setExecuting(null);
               dao.getCollectionDao().updateCollectionRules(collection);
            }
         }
      });
   }

   private List<Document> getDocuments(final CronRule rule, final Collection collection, final DaoContextSnapshot dao) {
      if (dao.getSelectedWorkspace().getOrganization().isPresent() && rule.getViewId() != null) {
         try {
            final View view = dao.getViewDao().getViewById(rule.getViewId());
            final User user = AuthenticatedUser.getMachineUser();
            final AllowedPermissions allowedPermissions = AllowedPermissions.allAllowed();

            final List<Document> documents = DocumentUtils.getDocuments(dao, view.getQuery(), user, rule.getLanguage(), allowedPermissions, null);
            return documents.stream().filter(document -> document.getCollectionId().equals(collection.getId())).collect(Collectors.toList());
         } catch (ResourceNotFoundException e) {
            return List.of();
         }

      }

      return List.of();
   }

   private Task getTask(final ContextualTaskFactory taskFactory, final String name, final Rule rule, final Collection collection, final List<Document> documents) {
      RuleTask task = taskFactory.getInstance(RuleTask.class);
      task.setRule(name, rule, collection, documents);
      return task;
   }
}
