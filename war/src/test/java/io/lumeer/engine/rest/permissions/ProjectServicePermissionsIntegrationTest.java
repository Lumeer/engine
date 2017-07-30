package io.lumeer.engine.rest.permissions;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Collection;
import io.lumeer.engine.api.dto.Organization;
import io.lumeer.engine.api.dto.Project;
import io.lumeer.engine.api.exception.UserAlreadyExistsException;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.UserFacade;
import io.lumeer.engine.controller.UserGroupFacade;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RunWith(Arquillian.class)
public class ProjectServicePermissionsIntegrationTest extends IntegrationTestBase {

   @Inject
   private SecurityFacade securityFacade;

   @Inject
   private UserFacade userFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   @SystemDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   UserGroupFacade userGroupFacade;

   @Inject
   ProjectFacade projectFacade;

   private Client client;
   private static String organizationCode = "OrganizationServicePermissionsIntegrationTest_id";
   private static String organizationName = "OrganizationServicePermissionsIntegrationTest";
   private static final String TARGET_URI = "http://localhost:8080";
   private static String PATH_PREFIX = PATH_CONTEXT + "/rest/organizations/" + organizationCode + "/projects/";
   private String userEmail;

   @Before
   public void initiateDatabaseAndGetInfo() {
      dataStorage.dropManyDocuments(LumeerConst.Organization.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
      dataStorage.dropManyDocuments(LumeerConst.Project.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
      organizationFacade.createOrganization(new Organization(organizationCode, organizationName));
      organizationFacade.setOrganizationCode(organizationCode);
      userEmail = userFacade.getUserEmail();
   }

   @Before
   public void createClient() {
      client = ClientBuilder.newBuilder().build();
   }

   @After
   public void closeClient() {
      client.close();
   }

   @Test
   public void testGetProjectNoRole() throws UserAlreadyExistsException {
      String groupName = "TestGroup1";
      userGroupFacade.addGroups(organizationCode, groupName);
      userGroupFacade.addUser(organizationCode, userEmail, groupName);

      String projectCode = "ProjectServiceTestProject_id";
      String projectName = "ProjectServiceTestProject";
      projectFacade.createProject(new Project(projectCode, projectName));

      assertThat(securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_READ)).isTrue();
      assertThat(securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_WRITE)).isTrue();
      assertThat(securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_MANAGE)).isTrue();
      securityFacade.removeProjectUsersRole(projectCode, Collections.singletonList(userEmail), LumeerConst.Security.ROLE_READ);
      securityFacade.removeProjectUsersRole(projectCode, Collections.singletonList(userEmail), LumeerConst.Security.ROLE_WRITE);
      securityFacade.removeProjectUsersRole(projectCode, Collections.singletonList(userEmail), LumeerConst.Security.ROLE_MANAGE);
      assertThat(securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_READ)).isFalse();
      assertThat(securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_WRITE)).isFalse();
      assertThat(securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_MANAGE)).isFalse();

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode).
            request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
      client.close();
      createClient();
      response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/name").
            request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testGetProjectReadRole() throws UserAlreadyExistsException {
      String groupName = "TestGroup2";
      userGroupFacade.addGroups(organizationCode, groupName);
      userGroupFacade.addUser(organizationCode, userEmail, groupName);

      String projectCode = "ProjectServiceTestProject_code";
      String projectName = "ProjectServiceTestProject";
      projectFacade.createProject(new Project(projectCode, projectName));

      securityFacade.removeProjectUsersRole(projectCode, Collections.singletonList(userEmail), LumeerConst.Security.ROLE_WRITE);
      securityFacade.removeProjectUsersRole(projectCode, Collections.singletonList(userEmail), LumeerConst.Security.ROLE_MANAGE);
      assertThat(securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_READ)).isTrue();
      assertThat(securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_WRITE)).isFalse();
      assertThat(securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_MANAGE)).isFalse();

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode).
            request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      client.close();
      createClient();
      response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/name").
            request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }

   @Test
   public void testUpdateProjectNoRole() throws UserAlreadyExistsException {
      String groupName = "TestGroup3";
      userGroupFacade.addGroups(organizationCode, groupName);
      userGroupFacade.addUser(organizationCode, userEmail, groupName);

      String projectCode = "ProjectServiceTestProject_id";
      String projectName = "ProjectServiceTestProject";
      projectFacade.createProject(new Project(projectCode, projectName));

      securityFacade.removeProjectUsersRole(projectCode, Collections.singletonList(userEmail), LumeerConst.Security.ROLE_READ);
      securityFacade.removeProjectUsersRole(projectCode, Collections.singletonList(userEmail), LumeerConst.Security.ROLE_WRITE);
      securityFacade.removeProjectUsersRole(projectCode, Collections.singletonList(userEmail), LumeerConst.Security.ROLE_MANAGE);
      assertThat(securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_READ)).isFalse();
      assertThat(securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_WRITE)).isFalse();
      assertThat(securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_MANAGE)).isFalse();

      String newProjectName = "NewName";
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/name/" + newProjectName).
            request(MediaType.APPLICATION_JSON).buildPut(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
      client.close();
      createClient();
      String newprojectCode = "NewCode";
      String newProjectName2 = "NewName2";
      response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode).
            request(MediaType.APPLICATION_JSON).buildPut(Entity.json(new Project(newprojectCode, newProjectName2))).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testGetUpdateProjectManageRole() throws UserAlreadyExistsException {
      String groupName = "TestGroup4";
      userGroupFacade.addGroups(organizationCode, groupName);
      userGroupFacade.addUser(organizationCode, userEmail, groupName);

      String projectCode = "ProjectServiceTestProject_id";
      String projectName = "ProjectServiceTestProject";
      projectFacade.createProject(new Project(projectCode, projectName));

      assertThat(securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_READ)).isTrue();
      assertThat(securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_MANAGE)).isTrue();

      String newProjectName = "NewName";
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode + "/name/" + newProjectName).
            request(MediaType.APPLICATION_JSON).buildPut(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.NO_CONTENT);
      client.close();
      createClient();
      String newprojectCode = "NewCode";
      String newProjectName2 = "NewName2";
      response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode).
            request(MediaType.APPLICATION_JSON).buildPut(Entity.json(new Project(newprojectCode, newProjectName2))).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.NO_CONTENT);
   }

   @Test
   public void testGetProjectsSomeReadRoles() throws UserAlreadyExistsException {
      String groupName = "TestGroup5";
      userGroupFacade.addGroups(organizationCode, groupName);
      userGroupFacade.addUser(organizationCode, userEmail, groupName);

      String projectCode1 = "ProjectServiceTestProject_code1";
      String projectName1 = "ProjectServiceTestProject1";
      String projectCode2 = "ProjectServiceTestProject_code2";
      String projectName2 = "ProjectServiceTestProject2";
      String projectCode3 = "ProjectServiceTestProject_code3";
      String projectName3 = "ProjectServiceTestProject3";
      String projectCode4 = "ProjectServiceTestProject_code4";
      String projectName4 = "ProjectServiceTestProject4";
      List<String> projectCodes = Arrays.asList(projectCode1, projectCode2, projectCode3, projectCode4);
      List<String> projectNames = Arrays.asList(projectName1, projectName2, projectName3, projectName4);

      for (int i = 0; i < projectCodes.size(); i++) {
         projectFacade.createProject(new Project(projectCodes.get(i), projectNames.get(i)));
         if (i % 2 == 1) {
            securityFacade.removeProjectUsersRole(projectCodes.get(i), Collections.singletonList(userEmail), LumeerConst.Security.ROLE_READ);
            securityFacade.removeProjectUsersRole(projectCodes.get(i), Collections.singletonList(userEmail), LumeerConst.Security.ROLE_WRITE);
            securityFacade.removeProjectUsersRole(projectCodes.get(i), Collections.singletonList(userEmail), LumeerConst.Security.ROLE_MANAGE);
            assertThat(securityFacade.hasProjectRole(projectCodes.get(i), LumeerConst.Security.ROLE_READ)).isFalse();
         } else {
            assertThat(securityFacade.hasProjectRole(projectCodes.get(i), LumeerConst.Security.ROLE_READ)).isTrue();
         }
      }

      Response response = client.target(TARGET_URI).path(PATH_PREFIX).
            request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      List<Project> projects = response.readEntity(new GenericType<List<Project>>(List.class) {
      });
      assertThat(projects).extracting("code").containsOnly(projectCode1, projectCode3);
      assertThat(projects).extracting("name").containsOnly(projectName1, projectName3);
   }

   @Test
   public void testDeleteProjectInOrganizatioNoRole() throws UserAlreadyExistsException {
      String groupName = "TestGroup6";
      userGroupFacade.addGroups(organizationCode, groupName);
      userGroupFacade.addUser(organizationCode, userEmail, groupName);

      String projectCode = "ProjectServiceTestProject_code1";
      String projectName = "ProjectServiceTestProject1";
      projectFacade.createProject(new Project(projectCode, projectName));

      securityFacade.removeProjectUsersRole(projectCode, Collections.singletonList(userEmail), LumeerConst.Security.ROLE_READ);
      securityFacade.removeProjectUsersRole(projectCode, Collections.singletonList(userEmail), LumeerConst.Security.ROLE_WRITE);
      securityFacade.removeProjectUsersRole(projectCode, Collections.singletonList(userEmail), LumeerConst.Security.ROLE_MANAGE);
      assertThat(securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_READ)).isFalse();
      assertThat(securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_MANAGE)).isFalse();

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode).
            request(MediaType.APPLICATION_JSON).buildDelete().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testDeleteProjectInOrganizatioManageRole() throws UserAlreadyExistsException {
      String groupName = "TestGroup7";
      userGroupFacade.addGroups(organizationCode, groupName);
      userGroupFacade.addUser(organizationCode, userEmail, groupName);

      String projectCode = "ProjectServiceTestProject_code1";
      String projectName = "ProjectServiceTestProject1";
      projectFacade.createProject(new Project(projectCode, projectName));

      assertThat(securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_READ)).isTrue();
      assertThat(securityFacade.hasProjectRole(projectCode, LumeerConst.Security.ROLE_MANAGE)).isTrue();

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode).
            request(MediaType.APPLICATION_JSON).buildDelete().invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.NO_CONTENT);
   }

   @Test
   public void testCreateCollectionInProjectNoRole() throws UserAlreadyExistsException {
      String groupName = "TestGroup8";
      userGroupFacade.addGroups(organizationCode, groupName);
      userGroupFacade.addUser(organizationCode, userEmail, groupName);

      String projectCode1 = "ProjectServiceTestProject_code1";
      String projectName1 = "ProjectServiceTestProject1";
      projectFacade.createProject(new Project(projectCode1, projectName1));

      securityFacade.removeProjectUsersRole(projectCode1, Collections.singletonList(userEmail), LumeerConst.Security.ROLE_READ);
      securityFacade.removeProjectUsersRole(projectCode1, Collections.singletonList(userEmail), LumeerConst.Security.ROLE_WRITE);
      securityFacade.removeProjectUsersRole(projectCode1, Collections.singletonList(userEmail), LumeerConst.Security.ROLE_MANAGE);
      assertThat(securityFacade.hasProjectRole(projectCode1, LumeerConst.Security.ROLE_READ)).isFalse();
      assertThat(securityFacade.hasProjectRole(projectCode1, LumeerConst.Security.ROLE_WRITE)).isFalse();
      assertThat(securityFacade.hasProjectRole(projectCode1, LumeerConst.Security.ROLE_MANAGE)).isFalse();

      String collectionName = "CollectionName";
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode1 + "/collections/").
            request(MediaType.APPLICATION_JSON).buildPost(Entity.json(new Collection(collectionName))).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.UNAUTHORIZED);
   }

   @Test
   public void testCreateCollectionInProjectWriteRole() throws UserAlreadyExistsException {
      String groupName = "TestGroup9";
      userGroupFacade.addGroups(organizationCode, groupName);
      userGroupFacade.addUser(organizationCode, userEmail, groupName);

      String projectCode1 = "ProjectServiceTestProject_code1";
      String projectName1 = "ProjectServiceTestProject1";
      projectFacade.createProject(new Project(projectCode1, projectName1));

      assertThat(securityFacade.hasProjectRole(projectCode1, LumeerConst.Security.ROLE_READ)).isTrue();
      assertThat(securityFacade.hasProjectRole(projectCode1, LumeerConst.Security.ROLE_WRITE)).isTrue();
      assertThat(securityFacade.hasProjectRole(projectCode1, LumeerConst.Security.ROLE_MANAGE)).isTrue();

      String collectionName = "CollectionName2";
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + projectCode1 + "/collections/").
            request(MediaType.APPLICATION_JSON).buildPost(Entity.json(new Collection(collectionName))).invoke();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
   }
}