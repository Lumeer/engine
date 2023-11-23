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
package io.lumeer.remote.rest.init;

import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;

import java.io.Serializable;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Startup
@Singleton
public class StartupFacade implements Serializable {

   @Inject
   private Logger log;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @PostConstruct
   public void afterDeployment() {
      /*log.info("Checking database for updates...");
      long tm = System.currentTimeMillis();

      final LongAdder orgs = new LongAdder(), projs = new LongAdder(), attributes = new LongAdder();

      workspaceKeeper.push();

      try {

         organizationDao.getAllOrganizations().forEach(organization -> {
            orgs.increment();
            log.info("Processing organization " + organization.getCode());
            workspaceKeeper.setOrganization(organization);
            projectDao.switchOrganization();
            projectDao.getAllProjects().forEach(project -> {
               projs.increment();
               log.info("Processing project " + project.getCode());

               workspaceKeeper.setProject(project);
               linkTypeDao.setProject(project);
               collectionDao.setProject(project);

            });
         });

      } catch (Exception e) {
         log.log(Level.SEVERE, "Unable to update database", e);
      }

      workspaceKeeper.pop();

      log.info(String.format("Updated %d organizations, %d project, %d attributes.",
            orgs.longValue(), projs.longValue(), attributes.longValue()));

      log.info("Updates completed in " + (System.currentTimeMillis() - tm) + "ms.");*/
   }

}
