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

import static io.lumeer.engine.api.LumeerConst.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Tests for ProjectService
 */
@RunWith(Arquillian.class)
public class ProjectServiceIntegrationTest extends IntegrationTestBase {

   private static final String TARGET_URI = "http://localhost:8080";
   private String PATH_PREFIX = PATH_CONTEXT + "/rest/LMR/projects/";

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
      dataStorage.dropManyDocuments(Project.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
      PATH_PREFIX = PATH_CONTEXT + "/rest/" + organizationFacade.getOrganizationId() + "/projects/";
   }

   @Test
   public void testGetProjects() throws Exception {
      final String project1 = "project1";
      final String project2 = "project2";
      projectFacade.createProject(project1, "Project One");
      projectFacade.createProject(project2, "Project Two");

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet()
                                .invoke();

      Map<String, String> projects = response.readEntity(new GenericType<Map<String, String>>() {
      });
      assertThat(projects).containsOnlyKeys(project1, project2);
      assertThat(projects).containsEntry(project1, "Project One");
      assertThat(projects).containsEntry(project2, "Project Two");
   }

   @Test
   public void testGetProjectName() throws Exception {
      final String project = "project11";
      projectFacade.createProject(project, "Project One");

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + project + "/name")
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

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI)
            .path(PATH_PREFIX + project)
            .request(MediaType.APPLICATION_JSON)
            .buildPost(Entity.entity(projectName, MediaType.APPLICATION_JSON))
            .invoke();

      assertThat(projectFacade.readProjectsMap(organizationFacade.getOrganizationId())).hasSize(1).containsEntry(project, projectName);
   }

   @Test
   public void testRenameProject() throws Exception {
      final String project = "project31";
      final String projectNameOld = "Project One";
      final String projectNameNew = "Project One New";
      projectFacade.createProject(project, projectNameOld);
      assertThat(projectFacade.readProjectName(project)).isEqualTo(projectNameOld);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI)
            .path(PATH_PREFIX + project + "/name/" + projectNameNew)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(null, MediaType.APPLICATION_JSON))
            .invoke();

      assertThat(projectFacade.readProjectName(project)).isEqualTo(projectNameNew);
   }

   @Test
   public void testUpdateProjectId() throws Exception {
      final String project = "project41";
      final String projectName = "Project One";
      final String projectNew = "project41New";
      projectFacade.createProject(project, projectName);
      assertThat(projectFacade.readProjectsMap(organizationFacade.getOrganizationId())).hasSize(1).containsOnlyKeys(project);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI)
            .path(PATH_PREFIX + project + "/id/" + projectNew)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity(null, MediaType.APPLICATION_JSON))
            .invoke();

      assertThat(projectFacade.readProjectsMap(organizationFacade.getOrganizationId())).hasSize(1).containsOnlyKeys(projectNew);
   }

   @Test
   public void testDropProject() throws Exception {
      final String project = "project51";
      final String projectName = "Project One";
      projectFacade.createProject(project, projectName);
      assertThat(projectFacade.readProjectsMap(organizationFacade.getOrganizationId())).hasSize(1).containsOnlyKeys(project);

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI)
            .path(PATH_PREFIX + project)
            .request(MediaType.APPLICATION_JSON)
            .buildDelete()
            .invoke();

      assertThat(projectFacade.readProjectsMap(organizationFacade.getOrganizationId())).isEmpty();
   }

   @Test
   public void testReadProjectMetadata() throws Exception {
      final String project = "project61";
      final String projectName = "Project One";
      final String metaAttr = "metaAttr";
      projectFacade.createProject(project, projectName);
      projectFacade.updateProjectMetadata(project, new DataDocument(metaAttr, "value"));

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + project + "/meta/" + metaAttr)
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
      projectFacade.createProject(project, projectName);
      projectFacade.updateProjectMetadata(project, new DataDocument(metaAttr, "value"));
      assertThat(projectFacade.readProjectMetadata(project, metaAttr)).isEqualTo("value");

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI)
            .path(PATH_PREFIX + project + "/meta/" + metaAttr)
            .request(MediaType.APPLICATION_JSON)
            .buildPut(Entity.entity("valueNew", MediaType.APPLICATION_JSON))
            .invoke();

      assertThat(projectFacade.readProjectMetadata(project, metaAttr)).isEqualTo("valueNew");
   }

   @Test
   public void testDropProjectMetadata() throws Exception {
      final String project = "project71";
      final String projectName = "Project One";
      final String metaAttr = "metaAttr";
      projectFacade.createProject(project, projectName);
      projectFacade.updateProjectMetadata(project, new DataDocument(metaAttr, "value"));
      assertThat(projectFacade.readProjectMetadata(project, metaAttr)).isNotNull();

      final Client client = ClientBuilder.newBuilder().build();
      client.target(TARGET_URI)
            .path(PATH_PREFIX + project + "/meta/" + metaAttr)
            .request(MediaType.APPLICATION_JSON)
            .buildDelete()
            .invoke();
      assertThat(projectFacade.readProjectMetadata(project, metaAttr)).isNull();
   }

}
