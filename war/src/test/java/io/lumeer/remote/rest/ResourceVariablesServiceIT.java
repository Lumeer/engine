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
package io.lumeer.remote.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.ResourceVariable;
import io.lumeer.api.model.ResourceVariableType;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.User;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.ResourceVariableDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.StorageException;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

@RunWith(Arquillian.class)
public class ResourceVariablesServiceIT extends ServiceIntegrationTestBase {

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;

   private User user;

   private String variablesUrl;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ResourceVariableDao resourceVariableDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   private String organizationId;
   private String projectId;

   @Before
   public void prepare() {
      User user = new User(USER);
      this.user = userDao.createUser(user);

      Organization organization = new Organization();
      organization.setCode("ORGANIZATION");
      organization.setPermissions(new Permissions());
      organization.getPermissions().updateUserPermissions(Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.Read))));
      Organization storedOrganization = organizationDao.createOrganization(organization);
      organizationId = storedOrganization.getId();

      projectDao.setOrganization(storedOrganization);
      resourceVariableDao.setOrganization(storedOrganization);
      resourceVariableDao.ensureIndexes(storedOrganization);

      Project project = new Project();
      project.setCode("PROJECT");
      project.setPermissions(new Permissions());
      project.getPermissions().updateUserPermissions(Permission.buildWithRoles(this.user.getId(), Set.of(new Role(RoleType.Read), new Role(RoleType.TechConfig))));
      projectId = projectDao.createProject(project).getId();

      variablesUrl = basePath() + "organizations/" + storedOrganization.getId() + "/variables";
   }

   private ResourceVariable prepareVariable(String key, Object value, Boolean secure) {
      return new ResourceVariable(null, projectId, ResourceType.PROJECT, key, value, ResourceVariableType.String, secure, organizationId, projectId);
   }

   private ResourceVariable createVariable(String key, Object value, Boolean secure) {
      ResourceVariable variable = prepareVariable(key, value, secure);
      return resourceVariableDao.create(variable);
   }

   @Test
   public void testCreateVariable() {
      ResourceVariable variable = prepareVariable("key", "value", false);
      Entity entity = Entity.json(variable);

      Response response = client.target(variablesUrl)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      ResourceVariable returnedVariable = response.readEntity(ResourceVariable.class);
      assertVariables(variable, returnedVariable, false);
   }

   @Test
   public void testCreateExisting() {
      ResourceVariable variable = createVariable("key", "value", false);
      Entity entity = Entity.json(prepareVariable(variable.getKey(), "xyz", false));

      Response response = client.target(variablesUrl)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR);
   }

   @Test
   public void testGetVariable() {
      final ResourceVariable variable = createVariable("key", "value", false);

      Response response = client.target(variablesUrl)
                                .path(variable.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      ResourceVariable returnedVariable = response.readEntity(ResourceVariable.class);
      assertVariables(variable, returnedVariable, false);
   }

   @Test
   public void testGetSecureVariable() {
      final ResourceVariable variable = createVariable("key", "value", true);

      Response response = client.target(variablesUrl)
                                .path(variable.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      ResourceVariable returnedVariable = response.readEntity(ResourceVariable.class);
      assertVariables(variable, returnedVariable, true);
   }

   @Test
   public void testGetSecureVariables() {
      createVariable("k1", "dsdad", true);
      createVariable("k2", "vadaslue", false);
      createVariable("k3", "dsa", true);
      createVariable("k4", "vadsadsalue", false);
      createVariable("k5", "dasdsadsa", true);

      Response response = client.target(variablesUrl)
                                .path("projects")
                                .path(projectId)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<ResourceVariable> variables = response.readEntity(new GenericType<List<ResourceVariable>>() {
      });
      variables.forEach(variable -> {
         if (variable.getSecure()) {
            assertThat(variable.getKey()).isIn("k1", "k3", "k5");
            assertThat(variable.getValue()).isNull();
         } else {
            assertThat(variable.getKey()).isIn("k2", "k4");
            assertThat(variable.getValue()).isNotNull();
         }
      });
   }

   @Test
   public void testUpdateVariable() {
      ResourceVariable variable = createVariable("key", "value", false);
      variable.setKey("key2");
      variable.setValue("vabcdsaddsa");

      Entity entity = Entity.json(variable);

      Response response = client.target(variablesUrl)
                                .path(variable.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      ResourceVariable returnedVariable = response.readEntity(ResourceVariable.class);
      assertVariables(variable, returnedVariable, false);
   }

   @Test
   public void testUpdateBadRequestVariable() {
      ResourceVariable variable = createVariable("key", "value", false);
      variable.setOrganizationId("xyzyzyzz");

      Entity entity = Entity.json(variable);

      Response response = client.target(variablesUrl)
                                .path(variable.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.BAD_REQUEST);
   }

   private void assertVariables(ResourceVariable v1, ResourceVariable v2, Boolean secure) {
      assertThat(v2.getKey()).isEqualTo(v1.getKey());
      assertThat(v2.getSecure()).isEqualTo(v1.getSecure());
      if (secure) {
         assertThat(v2.getValue()).isEqualTo(null);
         assertThat(v2.getStringValue()).isEqualTo(null);
      } else {
         assertThat(v2.getValue()).isEqualTo(v1.getValue());
         assertThat(v2.getStringValue()).isEqualTo(v1.getStringValue());
      }
      assertThat(v2.getOrganizationId()).isEqualTo(v1.getOrganizationId());
      assertThat(v2.getProjectId()).isEqualTo(v1.getProjectId());
      assertThat(v2.getType()).isEqualTo(v1.getType());
      assertThat(v2.getResourceType()).isEqualTo(v1.getResourceType());
      assertThat(v2.getResourceId()).isEqualTo(v1.getResourceId());
   }

   @Test
   public void testDeleteVariable() {
      final ResourceVariable variable = createVariable("key", "value", false);

      Response response = client.target(variablesUrl)
                                .path(variable.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(variablesUrl).build());

      assertThatThrownBy(() -> resourceVariableDao.getVariable(variable.getId()))
            .isInstanceOf(StorageException.class);
   }

}
