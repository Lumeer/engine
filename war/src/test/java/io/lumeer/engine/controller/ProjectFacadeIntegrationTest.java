/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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
package io.lumeer.engine.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
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
   public void initProjectCollection() throws Exception {
      systemDataStorage.createCollection(LumeerConst.Project.COLLECTION_NAME);
      systemDataStorage.createIndex(LumeerConst.Project.COLLECTION_NAME, new DataDocument(LumeerConst.Project.ATTR_ORGANIZATION_ID, LumeerConst.Index.ASCENDING)
            .append(LumeerConst.Project.ATTR_PROJECT_CODE, LumeerConst.Index.ASCENDING), true);
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
   public void testReadAndUpdateProject() {
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
   public void organizationSwitching() throws Exception {
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
