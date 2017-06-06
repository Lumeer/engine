/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.engine.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Organization;
import io.lumeer.engine.api.dto.Project;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Tests for ProjectService
 */
@RunWith(Arquillian.class)
public class ProjectServiceIntegrationTest extends IntegrationTestBase {

   private static final String TARGET_URI = "http://localhost:8080";

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   @SystemDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Before
   public void init() {
      dataStorage.dropManyDocuments(LumeerConst.Project.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
      dataStorage.dropManyDocuments(LumeerConst.Organization.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
   }

   @Test
   public void testGetProjects() throws Exception {
      final String project1 = "project1";
      final String project2 = "project2";
      final String org = "ORG1";

      organizationFacade.createOrganization(new Organization(org, "Organization"));
      organizationFacade.setOrganizationCode(org);
      projectFacade.createProject(new Project(project1, "Project One"));
      projectFacade.createProject(new Project(project2, "Project Two"));

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI)
                                .path(pathPrefix(org))
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet()
                                .invoke();

      List<Project> projects = response.readEntity(new GenericType<List<Project>>(List.class) {
      });
      assertThat(projects).extracting("code").containsOnly(project1, project2);
      assertThat(projects).extracting("name").containsOnly("Project One", "Project Two");
   }

   @Test
   public void testGetProjectName() throws Exception {
      final String project = "project11";
      final String org = "ORG11";

      organizationFacade.createOrganization(new Organization(org, "Organization"));
      organizationFacade.setOrganizationCode(org);
      projectFacade.createProject(new Project(project, "Project One"));

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI)
                                .path(pathPrefix(org) + project + "/name")
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet()
                                .invoke();

      String projectName = response.readEntity(String.class);

      assertThat(projectName).isEqualTo("Project One");
   }

   @Test
   public void testCreateProject() throws Exception {
      final String project = "project21";
      final String projectName = "Project One";
      final String org = "ORG21";

      organizationFacade.createOrganization(new Organization(org, "Organization"));

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI)
            .path(pathPrefix(org))
            .request()
            .buildPost(Entity.json(new Project(project, projectName)))
            .invoke();

      List<Project> projects = projectFacade.readProjects(org);
      assertThat(projects).hasSize(1);
      assertThat(projects).extracting("code").contains(project);
   }

   @Test
   public void testRenameProject() throws Exception {
      final String project = "project31";
      final String projectNameOld = "Project One";
      final String projectNameNew = "Project One New";
      final String org = "ORG31";

      organizationFacade.createOrganization(new Organization(org, "Organization"));
      organizationFacade.setOrganizationCode(org);

      projectFacade.createProject(new Project(project, projectNameOld));
      assertThat(projectFacade.readProjectName(project)).isEqualTo(projectNameOld);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI)
            .path(pathPrefix(org) + project + "/name/" + projectNameNew)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(null, MediaType.APPLICATION_JSON))
            .invoke();

      assertThat(projectFacade.readProjectName(project)).isEqualTo(projectNameNew);
   }

   @Test
   public void testUpdateProjectCode() throws Exception {
      final String project = "project41";
      final String projectName = "Project One";
      final String projectNew = "project41New";
      final String org = "ORG41";

      organizationFacade.createOrganization(new Organization(org, "Organization"));
      organizationFacade.setOrganizationCode(org);

      projectFacade.createProject(new Project(project, projectName));
      List<Project> projects = projectFacade.readProjects(org);
      assertThat(projects).hasSize(1);
      assertThat(projects).extracting("code").containsOnly(project);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI)
            .path(pathPrefix(org) + project + "/code/" + projectNew)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(null, MediaType.APPLICATION_JSON))
            .invoke();

      projects = projectFacade.readProjects(org);
      assertThat(projects).hasSize(1);
      assertThat(projects).extracting("code").contains(projectNew);
   }

   @Test
   public void testDropProject() throws Exception {
      final String project = "project51";
      final String projectName = "Project One";
      final String org = "ORG51";

      organizationFacade.createOrganization(new Organization(org, "Organization"));
      organizationFacade.setOrganizationCode(org);

      projectFacade.createProject(new Project(project, projectName));
      List<Project> projects = projectFacade.readProjects(org);
      assertThat(projects).hasSize(1).extracting("code").containsOnly(project);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI)
            .path(pathPrefix(org) + project)
            .request(MediaType.APPLICATION_JSON)
            .buildDelete()
            .invoke();

      assertThat(projectFacade.readProjects(org)).isEmpty();
   }

   @Test
   public void testReadProjectMetadata() throws Exception {
      final String project = "project61";
      final String projectName = "Project One";
      final String metaAttr = "metaAttr";
      final String org = "ORG61";

      organizationFacade.createOrganization(new Organization(org, "Organization"));
      organizationFacade.setOrganizationCode(org);

      projectFacade.createProject(new Project(project, projectName));
      projectFacade.updateProjectMetadata(project, new DataDocument(metaAttr, "value"));

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI)
                                .path(pathPrefix(org) + project + "/meta/" + metaAttr)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet()
                                .invoke();

      String metaValue = response.readEntity(String.class);
      assertThat(metaValue).isEqualTo("value");
   }

   @Test
   public void testUpdateProjectMetadata() throws Exception {
      final String project = "project71";
      final String projectName = "Project One";
      final String metaAttr = "metaAttr";
      final String org = "ORG71";

      organizationFacade.createOrganization(new Organization(org, "Organization"));
      organizationFacade.setOrganizationCode(org);

      projectFacade.createProject(new Project(project, projectName));
      projectFacade.updateProjectMetadata(project, new DataDocument(metaAttr, "value"));
      assertThat(projectFacade.readProjectMetadata(project, metaAttr)).isEqualTo("value");

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI)
            .path(pathPrefix(org) + project + "/meta/" + metaAttr)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity("valueNew", MediaType.APPLICATION_JSON))
            .invoke();

      assertThat(projectFacade.readProjectMetadata(project, metaAttr)).isEqualTo("valueNew");
   }

   @Test
   public void testDropProjectMetadata() throws Exception {
      final String project = "project81";
      final String projectName = "Project One";
      final String metaAttr = "metaAttr";
      final String org = "ORG81";

      organizationFacade.createOrganization(new Organization(org, "Organization"));
      organizationFacade.setOrganizationCode(org);

      projectFacade.createProject(new Project(project, projectName));
      projectFacade.updateProjectMetadata(project, new DataDocument(metaAttr, "value"));
      assertThat(projectFacade.readProjectMetadata(project, metaAttr)).isNotNull();

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI)
            .path(pathPrefix(org) + project + "/meta/" + metaAttr)
            .request(MediaType.APPLICATION_JSON)
            .buildDelete()
            .invoke();
      assertThat(projectFacade.readProjectMetadata(project, metaAttr)).isNull();
   }

   @Test
   public void testReadProject() throws Exception {
      final String project = "project91";
      final String projectName = "Project One";
      final String org = "ORG91";

      organizationFacade.createOrganization(new Organization(org, "Organization"));
      organizationFacade.setOrganizationCode(org);

      projectFacade.createProject(new Project(project, projectName));

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client
            .target(TARGET_URI)
            .path(pathPrefix(org) + project)
            .request(MediaType.APPLICATION_JSON)
            .buildGet()
            .invoke();

      Project proj = response.readEntity(Project.class);
      assertThat(proj).isNotNull();
   }

   @Test
   public void testUpdateProject() throws Exception {
      final String project = "project101";
      final String projectName = "Project One";
      final String newProjName = "Project One Updated";
      final String org = "ORG101";

      organizationFacade.createOrganization(new Organization(org, "Organization"));
      organizationFacade.setOrganizationCode(org);

      projectFacade.createProject(new Project(project, projectName));

      assertThat(projectFacade.readProject(project).getName()).isNotEqualTo(newProjName);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI)
            .path(pathPrefix(org) + project)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.json(new Project(project, newProjName)))
            .invoke();

      assertThat(projectFacade.readProject(project).getName()).isEqualTo(newProjName);
   }

   private String pathPrefix(String organizationCode){
      return PATH_CONTEXT + "/rest/" + organizationCode + "/projects/";
   }

}
