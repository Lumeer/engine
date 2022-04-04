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

import io.lumeer.api.model.*;
import io.lumeer.api.model.rule.CronRule;
import io.lumeer.core.WorkspaceContext;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.util.CronTaskChecker;
import io.lumeer.core.util.DocumentUtils;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
@Startup
public class CronTaskProcessor extends WorkspaceContext {

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private TaskExecutor taskExecutor;

   private static final Logger log = Logger.getLogger(CronTaskProcessor.class.getName());

   private final CronTaskChecker checker = new CronTaskChecker();

   @Schedule(hour = "*", minute = "*/1") // every 15 minutes
   public void process() {
      final List<Organization> organizations = organizationDao.getAllOrganizations();

      organizations.forEach(organization -> {
         final DataStorage userDataStorage = getDataStorage(organization.getId());

         final DaoContextSnapshot orgDao = getDaoContextSnapshot(userDataStorage, new Workspace(organization, null));
         final List<Project> projects = orgDao.getProjectDao().getAllProjects();

         projects.forEach(project -> {
            final DaoContextSnapshot projDao = getDaoContextSnapshot(userDataStorage, new Workspace(organization, project));
            final List<Collection> collections = projDao.getCollectionDao().getAllCollections();
            collections.forEach(collection -> processRules(projDao, collection));
         });
      });
   }

   private void processRules(final DaoContextSnapshot dao, final Collection collection) {
      var rules = collection.getRules().entrySet().stream().filter(e -> e.getValue().getType() == Rule.RuleType.CRON).collect(Collectors.toList());

      if (rules.size() > 0) {
         final ContextualTaskFactory taskFactory = getTaskFactory(dao);

         final String signature = UUID.randomUUID().toString();
         final ZonedDateTime now = ZonedDateTime.now();

         final Map<String, CronRule> rulesToExecute = new HashMap<>();

         rules.forEach(entry -> {
            final CronRule rule = new CronRule(entry.getValue());

            if (checker.shouldExecute(rule, ZonedDateTime.now())) {
               // it is not ok to have previously signed rule and not updated lastRun (i.e. pass the checker above)
               // this is a sign of an error in previous execution, let's revert normal state and let it pass to another round
               if (rule.getExecuting() != null && !"".equals(rule.getExecuting())) {
                  log.info(
                        String.format("Fixing rule execution signature on %s/%s, %s, '%s'.",
                              dao.getOrganization().getCode(),
                              dao.getProject().getCode(),
                              collection.getName(),
                              rule.getRule().getName()
                        )
                  );
                  rule.setExecuting(null);
               } else {
                  log.info(
                        String.format("Planning to run rule on %s/%s, %s, '%s'.",
                              dao.getOrganization().getCode(),
                              dao.getProject().getCode(),
                              collection.getName(),
                              rule.getRule().getName()
                        )
                  );
                  rule.setLastRun(now);
                  rule.setExecuting(signature);
                  if (rule.getExecutionsLeft() != null) {
                     rule.setExecutionsLeft(Math.max(rule.getExecutionsLeft() - 1, 0));
                  }
                  rulesToExecute.put(entry.getKey(), rule);
               }
            }
         });

         // bookedCollection is the previous version of collection before updating the rules!!!
         final Collection bookedCollection = dao.getCollectionDao().updateCollectionRules(collection);

         rulesToExecute.forEach((key, rule) -> {
            log.log(
                  Level.INFO,
                  String.format("Running cron rule on %s/%s, %s, '%s'.",
                        dao.getOrganization().getCode(),
                        dao.getProject().getCode(),
                        collection.getName(),
                        rule.getRule().getName()
                  )
            );

            // is it us who tried last?
            final String executing = new CronRule(bookedCollection.getRules().get(key)).getExecuting();
            if (executing == null || "".equals(executing)) {

               final List<Document> documents = getDocuments(rule, collection, dao);

               taskExecutor.submitTask(
                     getTask(
                           taskFactory,
                           rule.getRule().getName() != null ? rule.getRule().getName() : key,
                           rule.getRule(),
                           collection,
                           documents
                     )
               );
            }
         });

         // Make sure we have the rules with updated lastRun and simply clean the signatures no matter what (lastRun is updated anyway)
         final Collection latestCollection = dao.getCollectionDao().getCollectionById(collection.getId());
         rulesToExecute.keySet().forEach(key -> new CronRule(latestCollection.getRules().get(key)).setExecuting(null));
         dao.getCollectionDao().updateCollectionRules(latestCollection);
      }
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
