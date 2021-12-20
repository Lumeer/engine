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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.AttributeFilterEquation;
import io.lumeer.api.model.AttributeLock;
import io.lumeer.api.model.AttributeLockExceptionGroup;
import io.lumeer.api.model.Constraint;
import io.lumeer.api.model.ConstraintType;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.util.AttributeUtil;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
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
   private CollectionDao collectionDao;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @PostConstruct
   public void afterDeployment() {
      log.info("Checking database for updates...");
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

               collectionDao.getAllCollections().forEach(collection -> {
                  log.info("Read " + collection.getAttributes().size() + " collection attributes.");

                  var originalCollection = collection.copy();

                  var migratedAttributes = collection.getAttributes().stream().map(attribute -> this.migrateAttribute(attribute, attributes)).collect(Collectors.toSet());
                  collection.setAttributes(migratedAttributes);

                  collectionDao.updateCollection(collection.getId(), collection, originalCollection, false);
               });

               linkTypeDao.getAllLinkTypes().forEach(linkType -> {
                  log.info("Read " + linkType.getAttributes().size() + " collection attributes.");

                  var originalLinkType = new LinkType(linkType);

                  var migratedAttributes = linkType.getAttributes().stream().map(attribute -> this.migrateAttribute(attribute, attributes)).collect(Collectors.toSet());
                  linkType.setAttributes(migratedAttributes);

                  linkTypeDao.updateLinkType(linkType.getId(), linkType, originalLinkType, false);
               });

            });
         });

      } catch (Exception e) {
         log.log(Level.SEVERE, "Unable to update database", e);
      }

      workspaceKeeper.pop();

      log.info(String.format("Updated %d organizations, %d project, %d attributes.",
            orgs.longValue(), projs.longValue(), attributes.longValue()));

      log.info("Updates completed in " + (System.currentTimeMillis() - tm) + "ms.");
   }

   private Attribute migrateAttribute(Attribute attribute, LongAdder adder) {
      Constraint constraint = attribute.getConstraint();
      AttributeLock attributeLock = null;
      if (constraint != null && constraint.getType() == ConstraintType.Action && AttributeUtil.isConstraintWithConfig(attribute)) {
         @SuppressWarnings("unchecked") final Map<String, Object> config = (Map<String, Object>) constraint.getConfig();
         config.remove("role");
         Object equationObject = config.remove("equation");
         if (equationObject != null) {
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            try {
               String json = ow.writeValueAsString(equationObject);
               ObjectMapper objectMapper = new ObjectMapper();
               AttributeFilterEquation equation = objectMapper.readValue(json, AttributeFilterEquation.class);
               AttributeLockExceptionGroup group = new AttributeLockExceptionGroup(equation, Collections.emptyList(), "everyone");
               attributeLock = new AttributeLock(Collections.singletonList(group), true);
               adder.increment();
            } catch (IOException ignored) {

            }
         }
      } else if (AttributeUtil.functionIsDefined(attribute)) {
         attributeLock = new AttributeLock(Collections.emptyList(), !attribute.getFunction().isEditable());
         adder.increment();
      }

      return new Attribute(attribute.getId(), attribute.getName(), attribute.getDescription(), constraint, attributeLock, attribute.getFunction(), attribute.getUsageCount());
   }
}
