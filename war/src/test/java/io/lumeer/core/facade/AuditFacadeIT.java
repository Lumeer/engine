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

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Document;
import io.lumeer.api.model.Group;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.User;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class AuditFacadeIT extends IntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TPROJ";
   private static final String GROUP = "testGroup";
   private static final String ICON = "fa-eye";
   private static final String COLOR = "#00ee00";

   private Permission userPermission;
   private Permission groupPermission;
   private User user;
   private Group group;
   private Project project;
   private Organization organization;
   private Collection collection;

   @Inject
   private AuditFacade auditFacade;

   @Inject
   private UserDao userDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private GroupDao groupDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private DocumentFacade documentFacade;

   @Before
   public void configureProject() {
      User user = new User(USER);
      this.user = userDao.createUser(user);

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      Permissions organizationPermissions = new Permissions();
      userPermission = Permission.buildWithRoles(this.user.getId(), Collections.singleton(new Role(RoleType.Read)));
      organizationPermissions.updateUserPermissions(userPermission);
      organization.setPermissions(organizationPermissions);
      this.organization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(this.organization);
      groupDao.setOrganization(this.organization);
      group = groupDao.createGroup(new Group(GROUP));
      groupPermission = Permission.buildWithRoles(this.group.getId(), Collections.singleton(new Role(RoleType.Read)));

      Project project = new Project();
      project.setCode(PROJECT_CODE);

      Permissions projectPermissions = new Permissions();
      projectPermissions.updateUserPermissions(Permission.buildWithRoles(this.user.getId(), Project.ROLES));
      project.setPermissions(projectPermissions);
      this.project = projectDao.createProject(project);

      workspaceKeeper.setWorkspaceIds(this.organization.getId(), this.project.getId());

      collectionDao.setProject(project);

      this.collection = createCollection("COL1", "Collection 1");
   }

   private Collection prepareCollection(String code, String name) {
      return new Collection(code, name, ICON, COLOR, null);
   }

   private Collection createCollection(String code, String name) {
      Collection collection = prepareCollection(code, name);
      collection.getPermissions().updateUserPermissions(userPermission);
      collection.getPermissions().updateGroupPermissions(groupPermission);
      return collectionDao.createCollection(collection);
   }

   @Test
   public void testAuditLog() {
      var doc = new Document(new DataDocument("A", "x").append("B", "y").append("C", "z"));
      doc = documentFacade.createDocument(collection.getId(), doc);

      var audit = auditFacade.getAuditRecordsForDocument(collection.getId(), doc.getId());
      assertThat(audit).hasSize(1);

      documentFacade.updateDocumentData(collection.getId(), doc.getId(), new DataDocument("B", "y").append("C", "z").append("D", "w"));
      audit = auditFacade.getAuditRecordsForDocument(collection.getId(), doc.getId());
      assertThat(audit).hasSize(2);

      assertThat(audit.get(0).getOldState()).containsExactlyEntriesOf(Map.of("A", "x"));

      var newState = new HashMap<String, Object>();
      newState.put("A", null);
      newState.put("D", "w");
      assertThat(audit.get(0).getNewState()).containsExactlyInAnyOrderEntriesOf(newState);

      documentFacade.updateDocumentData(collection.getId(), doc.getId(), new DataDocument("B", "y").append("C", "z").append("D", "v"));
      audit = auditFacade.getAuditRecordsForDocument(collection.getId(), doc.getId());

      assertThat(audit).hasSize(2);
      assertThat(audit.get(0).getOldState()).containsExactlyEntriesOf(Map.of("A", "x"));
      newState = new HashMap<String, Object>();
      newState.put("A", null);
      newState.put("D", "v");
      assertThat(audit.get(0).getNewState()).containsExactlyInAnyOrderEntriesOf(newState);

      documentFacade.updateDocumentData(collection.getId(), doc.getId(), new DataDocument("B", "y").append("C", "z"));
      audit = auditFacade.getAuditRecordsForDocument(collection.getId(), doc.getId());

      assertThat(audit).hasSize(2);
      assertThat(audit.get(0).getOldState()).containsExactlyEntriesOf(Map.of("A", "x"));
      newState = new HashMap<String, Object>();
      newState.put("A", null);
      assertThat(audit.get(0).getNewState()).containsExactlyInAnyOrderEntriesOf(newState);

      documentFacade.updateDocumentData(collection.getId(), doc.getId(), new DataDocument("A", "x").append("B", "y").append("C", "z"));
      audit = auditFacade.getAuditRecordsForDocument(collection.getId(), doc.getId());

      assertThat(audit).hasSize(1);
   }
}
