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
import io.lumeer.api.model.Group;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.Sequence;
import io.lumeer.api.model.User;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.stream.Collectors;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class SequenceFacadeIT extends IntegrationTestBase {

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TPROJ";

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private Permission userPermission;
   private Permission groupPermission;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private GroupDao groupDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private SequenceFacade sequenceFacade;

   @Inject
   private ProjectFacade projectFacade;

   @Before
   public void configureProject() {
      User user = userDao.createUser(new User(USER));

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      Permissions organizationPermissions = new Permissions();
      userPermission = Permission.buildWithRoles(user.getId(), Organization.ROLES);
      organizationPermissions.updateUserPermissions(userPermission);
      organization.setPermissions(organizationPermissions);
      organization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(organization);
      groupDao.setOrganization(organization);
      Group group = groupDao.createGroup(new Group(GROUP));

      userPermission = Permission.buildWithRoles(user.getId(), Collection.ROLES);
      groupPermission = Permission.buildWithRoles(group.getId(), Collections.singleton(Role.READ));

      Project project = new Project();
      project.setCode(PROJECT_CODE);

      Permissions projectPermissions = new Permissions();
      projectPermissions.updateUserPermissions(Permission.buildWithRoles(user.getId(), Project.ROLES));
      project.setPermissions(projectPermissions);
      Project storedProject = projectDao.createProject(project);

      workspaceKeeper.setWorkspaceIds(organization.getId(), storedProject.getId());
   }

   @Test
   public void testSequences() {
      assertThat(sequenceFacade.getNextSequenceNumber("s1")).isEqualTo(0);
      assertThat(sequenceFacade.getNextSequenceNumber("s2")).isEqualTo(0);
      assertThat(sequenceFacade.getNextSequenceNumber("s1")).isEqualTo(1);
      assertThat(sequenceFacade.getNextSequenceNumber("s1")).isEqualTo(2);

      var sequences = sequenceFacade.getAllSequences();

      assertThat(sequences).hasSize(2);
      assertThat(sequences.stream().map(Sequence::getName).collect(Collectors.toSet())).contains("s1", "s2");
      assertThat(sequences.stream().map(Sequence::getSeq).collect(Collectors.toSet())).contains(0, 2);

      sequenceFacade.deleteSequence(sequences.stream().filter(sequence -> sequence.getName().equals("s2")).findFirst().get().getId());
      sequences = sequenceFacade.getAllSequences();
      assertThat(sequences).hasSize(1);
      assertThat(sequences.stream().map(Sequence::getName).collect(Collectors.toSet())).contains("s1");
   }

}
