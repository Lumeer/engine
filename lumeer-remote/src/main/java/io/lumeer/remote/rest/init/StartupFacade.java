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

import io.lumeer.api.model.ResourceType;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.ResourceCommentDao;

import java.io.Serializable;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

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
   private DocumentDao documentDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private ResourceCommentDao resourceCommentDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @PostConstruct
   public void afterDeployment() {
      log.info("Checking database for updates...");
      long tm = System.currentTimeMillis();

      final LongAdder orgs = new LongAdder(), projs = new LongAdder(), colls = new LongAdder(), docs = new LongAdder(), comments = new LongAdder(),
            linkTypes = new LongAdder(), linkInsts = new LongAdder();

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
               resourceCommentDao.ensureIndexes(project);

               collectionDao.getAllCollections().forEach(collection -> {
                  colls.increment();
                  log.info("Processing collection " + collection.getName());

                  documentDao.getDocumentsByCollection(collection.getId()).forEach(document -> {
                     docs.increment();
                     comments.add(resourceCommentDao.updateParentId(ResourceType.DOCUMENT, document.getId(), collection.getId()));
                  });
               });

               linkTypeDao.getAllLinkTypes().forEach(linkType -> {
                  linkTypes.increment();
                  log.info("Processing link type " + linkType.getName());

                  linkInstanceDao.getLinkInstancesByLinkType(linkType.getId()).forEach(linkInstance -> {
                     linkInsts.increment();

                     comments.add(resourceCommentDao.updateParentId(ResourceType.LINK, linkInstance.getId(), linkType.getId()));
                  });
               });

            });
         });

      } catch (Exception e) {
         log.log(Level.SEVERE, "Unable to update database", e);
      }

      workspaceKeeper.pop();

      log.info("Updates completed in " + (System.currentTimeMillis() - tm) + "ms.");
      log.info(String.format("Updated %d organizations, %d project, %d collections, %d documents, %d link types, %d links, %d comments.",
            orgs.longValue(), projs.longValue(), colls.longValue(), docs.longValue(), linkTypes.longValue(), linkInsts.longValue(),
            comments.longValue()));
   }
}
