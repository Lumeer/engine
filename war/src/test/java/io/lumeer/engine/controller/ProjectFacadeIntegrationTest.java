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
package io.lumeer.engine.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Organization;
import io.lumeer.engine.api.dto.Project;

import com.mongodb.MongoWriteException;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import javax.inject.Inject;

/**
 * Tests for ProjectFacade
 */
@RunWith(Arquillian.class)
public class ProjectFacadeIntegrationTest extends IntegrationTestBase {

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Before
   public void setUp() throws Exception {
      systemDataStorage.dropManyDocuments(LumeerConst.Project.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
      systemDataStorage.dropManyDocuments(LumeerConst.Organization.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
   }

   @Test
   public void basicMethodsTest() throws Exception {
      final String project1 = "project1";
      final String project2 = "project2";
      final String project3 = "project3";
      final String project4 = "project4";
      final String org = "ORG1";

      organizationFacade.createOrganization(new Organization(org, "Organization"));
      organizationFacade.setOrganizationCode(org);
      projectFacade.createProject(new Project(project1, "Project One"));
      projectFacade.createProject(new Project(project2, "Project Two"));

      List<Project> projects = projectFacade.readProjects(org);
      assertThat(projects).extracting("code").containsOnly(project1, project2);
      assertThat(projects).extracting("name").containsOnly("Project One", "Project Two");

      assertThat(projectFacade.readProjectName(project1)).isEqualTo("Project One");
      assertThat(projectFacade.readProjectName(project2)).isEqualTo("Project Two");
      assertThat(projectFacade.readProjectName(project3)).isNull();

      projectFacade.updateProjectCode(project1, project3);
      assertThat(projectFacade.readProjectName(project3)).isEqualTo("Project One");
      assertThat(projectFacade.readProjectName(project1)).isNull();

      projectFacade.renameProject(project2, "Project Two Renamed");
      assertThat(projectFacade.readProjectName(project2)).isEqualTo("Project Two Renamed");

      projects = projectFacade.readProjects(org);
      assertThat(projects).extracting("code").containsOnly(project3, project2);
      projectFacade.createProject(new Project(project4, "Project Three"));
      projects = projectFacade.readProjects(org);
      assertThat(projects).extracting("code").containsOnly(project3, project2, project4);
      projectFacade.dropProject(project4);
      projects = projectFacade.readProjects(org);
      assertThat(projects).extracting("code").containsOnly(project3, project2);
   }

   @Test
   public void testReadAndUpdateProject(){
      final String project = "project11";
      final String projectName = "Project One";
      final String newProjectName = "Project One New";
      final String org = "ORG11";

      organizationFacade.createOrganization(new Organization(org, "Organization"));
      organizationFacade.setOrganizationCode(org);

      projectFacade.createProject(new Project(project, projectName));
      Project proj = projectFacade.readProject(project);
      assertThat(proj).isNotNull();
      assertThat(proj.getName()).isNotEqualTo(newProjectName);

      projectFacade.updateProject(project, new Project(project, newProjectName));
      assertThat(projectFacade.readProject(project).getName()).isEqualTo(newProjectName);
   }

   @Test
   public void projectAlreadyExistsTest() throws Exception {
      final String project1 = "project21";
      final String project2 = "project22";
      final String org = "ORG21";

      organizationFacade.createOrganization(new Organization(org, "Organization"));
      organizationFacade.setOrganizationCode(org);

      projectFacade.createProject(new Project(project1, "Project One"));
      projectFacade.createProject(new Project(project2, "Project Two"));

      assertThatThrownBy(() -> projectFacade.createProject(new Project(project1, "Project One again"))).isInstanceOf(MongoWriteException.class);
      assertThatThrownBy(() -> projectFacade.updateProjectCode(project1, project2)).isInstanceOf(MongoWriteException.class);
   }

   @Test
   public void organizationSwitching() throws Exception{
      final String project1 = "project31";
      final String project2 = "project32";
      final String org1 = "ORG31";
      final String org2 = "ORG32";

      organizationFacade.createOrganization(new Organization(org1, "Organization"));
      organizationFacade.createOrganization(new Organization(org2, "Organization"));

      organizationFacade.setOrganizationCode(org1);
      projectFacade.createProject(new Project(project1, "Project One"));
      organizationFacade.setOrganizationCode(org2);
      projectFacade.createProject(new Project(project2, "Project Two"));

      List<Project> projects = projectFacade.readProjects(org1);
      assertThat(projects).hasSize(1).extracting("code").containsOnly(project1);

      projects = projectFacade.readProjects(org2);
      assertThat(projects).hasSize(1).extracting("code").containsOnly(project2);


   }
}
