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
import io.lumeer.api.model.FileAttachment;
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
import io.lumeer.core.cache.WorkspaceCache;
import io.lumeer.remote.rest.ServiceIntegrationTestBase;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;

@RunWith(Arquillian.class)
@Ignore("Cannot run without API keys and dependencies. Uncomment the dependency in IntegrationTestBase.")
public class FileAttachmentFacadeIT extends ServiceIntegrationTestBase {

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TPROJ";

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private Permission userPermission;
   private Permission groupPermission;
   private User user;
   private Group group;
   private Project project;
   private Organization organization;
   private Collection collection;

   @Inject
   private WorkspaceCache workspaceCache;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private FileAttachmentFacade fileAttachmentFacade;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private GroupDao groupDao;

   @Inject
   private CollectionDao collectionDao;

   private FileAttachment fileAttachment1;
   private FileAttachment fileAttachment2;

   @Before
   public void configureProject() {
      User user = new User(USER);
      this.user = userDao.createUser(user);

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      Permissions organizationPermissions = new Permissions();
      userPermission = Permission.buildWithRoles(this.user.getId(), Organization.ROLES);
      organizationPermissions.updateUserPermissions(userPermission);
      organization.setPermissions(organizationPermissions);
      this.organization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(this.organization);
      groupDao.setOrganization(this.organization);
      Group group = new Group(GROUP);
      this.group = groupDao.createGroup(group);

      userPermission = Permission.buildWithRoles(this.user.getId(), Collection.ROLES);
      groupPermission = Permission.buildWithRoles(this.group.getId(), Collections.singleton(new Role(RoleType.Read)));

      Project project = new Project();
      project.setCode(PROJECT_CODE);

      Permissions projectPermissions = new Permissions();
      projectPermissions.updateUserPermissions(Permission.buildWithRoles(this.user.getId(), Project.ROLES));
      project.setPermissions(projectPermissions);
      this.project = projectDao.createProject(project);

      workspaceKeeper.setWorkspaceIds(this.organization.getId(), this.project.getId());

      collectionDao.setProject(project);

      collection = new Collection("C1", "My collection", "fa-eye", "ffaabb", null);
      collection.getPermissions().updateUserPermissions(userPermission);
      collection.getPermissions().updateGroupPermissions(groupPermission);
      collection = collectionDao.createCollection(collection);

      fileAttachment1 = new FileAttachment(organization.getId(), project.getId(), collection.getId(), "5cf6f208857aba009210af9b", "a3", "můk super/$~@#ę€%=název.pdf", "", FileAttachment.AttachmentType.DOCUMENT);
      fileAttachment2 = new FileAttachment(organization.getId(), "5cf6f208857aba009210af9c", collection.getId(), "5cf6f208857aba009210af9b", "a3", "normal file name .doc", "", FileAttachment.AttachmentType.DOCUMENT);
   }

   @Test
   public void testFileUpload() {
      final FileAttachment createdFileAttachment = fileAttachmentFacade.createFileAttachment(fileAttachment1);

      assertThat(createdFileAttachment.getPresignedUrl()).isNotEmpty();

      final String content = "Hello world text file";
      final Entity entity = Entity.text(content);
      final String presigned = createdFileAttachment.getPresignedUrl();
      var response = client.target(presigned).request(MediaType.APPLICATION_JSON).buildPut(entity).invoke();

      var result = fileAttachmentFacade.getAllFileAttachments(createdFileAttachment.getCollectionId(), FileAttachment.AttachmentType.DOCUMENT);
      assertThat(result).containsExactly(createdFileAttachment);

      var listing = fileAttachmentFacade.listFileAttachments(createdFileAttachment.getCollectionId(), createdFileAttachment.getDocumentId(), createdFileAttachment.getAttributeId(), FileAttachment.AttachmentType.DOCUMENT);
      var tempFileAttachment = new FileAttachment(createdFileAttachment.getOrganizationId(), createdFileAttachment.getProjectId(), createdFileAttachment.getCollectionId(), createdFileAttachment.getDocumentId(), createdFileAttachment.getAttributeId(), createdFileAttachment.getFileName(), createdFileAttachment.getUniqueName(), FileAttachment.AttachmentType.DOCUMENT);
      tempFileAttachment.setSize(content.length());
      assertThat(listing).containsExactly(tempFileAttachment);
      assertThat(listing.get(0).getSize()).isGreaterThan(0L);

      fileAttachmentFacade.removeFileAttachment(createdFileAttachment);

      listing = fileAttachmentFacade.listFileAttachments(createdFileAttachment.getCollectionId(), createdFileAttachment.getDocumentId(), createdFileAttachment.getAttributeId(), FileAttachment.AttachmentType.DOCUMENT);
      assertThat(listing).hasSize(0);
   }
}
