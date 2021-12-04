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

import io.lumeer.api.model.Document;
import io.lumeer.api.model.Language;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.ResourceComment;
import io.lumeer.api.model.ResourceType;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.util.SelectionListUtils;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DocumentDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.ResourceCommentDao;
import io.lumeer.storage.api.dao.ResourceVariableDao;

import java.io.Serializable;
import java.util.Collections;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
   private DocumentDao documentDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private ResourceCommentDao resourceCommentDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private ResourceVariableDao resourceVariableDao;

   @PostConstruct
   public void afterDeployment() {
      log.info("Checking database for updates...");
      long tm = System.currentTimeMillis();

      organizationDao.getAllOrganizations().forEach(organization -> {
         resourceVariableDao.ensureIndexes(organization);
      });

      /*final LongAdder orgs = new LongAdder(), projs = new LongAdder(), comments = new LongAdder();

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
               resourceCommentDao.setProject(project);
               documentDao.setProject(project);
               linkInstanceDao.setProject(project);

               resourceCommentDao.ensureIndexes(project);

               var allComments = resourceCommentDao.getResourceComments(ResourceType.DOCUMENT);
               var documents = documentDao.getDocumentsByIds(allComments.stream().map(ResourceComment::getResourceId).collect(Collectors.toSet())).stream().collect(Collectors.toMap(Document::getId, Function.identity()));

               log.info("Read " + allComments.size() + " document comments.");

               allComments.forEach(comment -> {
                  comments.increment();
                  comment.setParentId(documents.get(comment.getResourceId()).getCollectionId());
                  resourceCommentDao.pureUpdateComment(comment);
               });

               allComments = resourceCommentDao.getResourceComments(ResourceType.LINK);
               log.info("Read " + allComments.size() + " link comments.");
               var links = linkInstanceDao.getLinkInstances(allComments.stream().map(ResourceComment::getResourceId).collect(Collectors.toSet())).stream().collect(Collectors.toMap(LinkInstance::getId, Function.identity()));
               allComments.forEach(comment -> {
                  comments.increment();
                  comment.setParentId(links.get(comment.getResourceId()).getLinkTypeId());
                  resourceCommentDao.pureUpdateComment(comment);
               });

            });
         });

      } catch (Exception e) {
         log.log(Level.SEVERE, "Unable to update database", e);
      }

      workspaceKeeper.pop();

      log.info(String.format("Updated %d organizations, %d project, %d comments.",
            orgs.longValue(), projs.longValue(), comments.longValue()));
      */

      log.info("Updates completed in " + (System.currentTimeMillis() - tm) + "ms.");
   }
}
