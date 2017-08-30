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
package io.lumeer.core.facade;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.dto.JsonProject;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.model.SimpleUser;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.query.SearchQuery;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class ImportFacadeIntegrationTest extends IntegrationTestBase {

   @Inject
   private ImportFacade importFacade;

   @Inject
   private DataDao dataDao;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   private static final String ORGANIZATION_CODE = "TORG";
   private static final String PROJECT_CODE = "TPROJ";
   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;

   @Before
   public void configureProject() {
      JsonOrganization organization = new JsonOrganization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new JsonPermissions());
      organizationDao.createOrganization(organization);

      projectDao.setOrganization(organization);
      userDao.setOrganization(organization);

      SimpleUser user = new SimpleUser(USER);
      userDao.createUser(user);

      JsonProject project = new JsonProject();
      project.setCode(PROJECT_CODE);

      JsonPermissions projectPermissions = new JsonPermissions();
      projectPermissions.updateUserPermissions(new JsonPermission(USER, Project.ROLES.stream().map(Role::toString).collect(Collectors.toSet())));
      project.setPermissions(projectPermissions);
      Project storedProject = projectDao.createProject(project);

      workspaceKeeper.setWorkspace(ORGANIZATION_CODE, PROJECT_CODE);

      collectionDao.setProject(storedProject);
   }

   @Test
   public void testImportEmptyCSV() throws Exception {
      final String emptyCSV = "";
      Collection collection = importFacade.importDocuments(ImportFacade.FORMAT_CSV, emptyCSV);
      assertThat(collection).isNotNull();

      List<DataDocument> data = dataDao.getData(collection.getId(), query());
      assertThat(data).isEmpty();
   }

   @Test
   public void testImportEmptyHeaderCSV() throws Exception {
      final String noHeaderCsv = "\n"
            + "a;b;c;d\n"
            + "a;b;c;d\n";
      Collection collection = importFacade.importDocuments(ImportFacade.FORMAT_CSV, noHeaderCsv);
      assertThat(collection).isNotNull();

      List<DataDocument> data = dataDao.getData(collection.getId(), query());
      assertThat(data).hasSize(1); // it's because library ignores any leading empty lines
      assertThat(data.get(0).keySet()).containsOnly("_id", "a", "b", "c", "d");
      assertThat(data.get(0).getString("a")).isEqualTo("a");
      assertThat(data.get(0).getString("b")).isEqualTo("b");
      assertThat(data.get(0).getString("c")).isEqualTo("c");
      assertThat(data.get(0).getString("d")).isEqualTo("d");
   }

   @Test
   public void testImportNoLinesCSV() throws Exception {
      final String noLinesCsv = "h1;h2;h3;h4\n";
      Collection collection = importFacade.importDocuments(ImportFacade.FORMAT_CSV, noLinesCsv);
      assertThat(collection).isNotNull();

      List<DataDocument> data = dataDao.getData(collection.getId(), query());
      assertThat(data).isEmpty();
   }

   @Test
   public void testImportCorrectCSV() throws Exception {
      final String correctCsv = "h1;h2;h3;h4\n"
            + ";b;c;d\n"
            + ";;c;d\n"
            + "a;b;;d\n"
            + "a;b;;\n";
      Collection collection = importFacade.importDocuments(ImportFacade.FORMAT_CSV, correctCsv);
      assertThat(collection).isNotNull();

      List<DataDocument> data = dataDao.getData(collection.getId(), query());
      assertThat(data).hasSize(4);

      int h1Num = 0;
      int h2Num = 0;
      int h3Num = 0;
      int h4Num = 0;
      for (DataDocument dataDocument : data) {
         assertThat(dataDocument.keySet()).containsOnly("_id", "h1", "h2", "h3", "h4");
         String h1 = dataDocument.getString("h1");
         String h2 = dataDocument.getString("h2");
         String h3 = dataDocument.getString("h3");
         String h4 = dataDocument.getString("h4");
         h1Num += h1 != null && h1.equals("a") ? 1 : 0;
         h2Num += h2 != null && h2.equals("b") ? 1 : 0;
         h3Num += h3 != null && h3.equals("c") ? 1 : 0;
         h4Num += h4 != null && h4.equals("d") ? 1 : 0;
      }
      assertThat(h1Num).isEqualTo(2);
      assertThat(h2Num).isEqualTo(3);
      assertThat(h3Num).isEqualTo(2);
      assertThat(h4Num).isEqualTo(3);
   }

   @Test
   public void testImportDiffRowsLengthCSV() throws Exception {
      final String diffRowsLengthCsv = "h1;h2;h3\n"
            + "a;b;c;d\n"
            + "a;b;c;d;e;f;g;h\n"
            + "\n\n\n\n\n"
            + "a;b;c\n";

      Collection collection = importFacade.importDocuments(ImportFacade.FORMAT_CSV, diffRowsLengthCsv);
      assertThat(collection).isNotNull();

      List<DataDocument> data = dataDao.getData(collection.getId(), query());
      assertThat(data).hasSize(3);

      int h1Num = 0;
      int h2Num = 0;
      int h3Num = 0;
      for (DataDocument dataDocument : data) {
         assertThat(dataDocument.keySet()).containsOnly("_id", "h1", "h2", "h3");
         String h1 = dataDocument.getString("h1");
         String h2 = dataDocument.getString("h2");
         String h3 = dataDocument.getString("h3");
         h1Num += h1 != null && h1.equals("a") ? 1 : 0;
         h2Num += h2 != null && h2.equals("b") ? 1 : 0;
         h3Num += h3 != null && h3.equals("c") ? 1 : 0;
      }
      assertThat(h1Num).isEqualTo(3);
      assertThat(h2Num).isEqualTo(3);
      assertThat(h3Num).isEqualTo(3);

   }

   @Test
   public void testImportCommaSeparatedCSV() throws Exception {
      final String commaSeparatedCsv = "h1,h2,h3\n"
            + "a,,c,d\n"
            + ",,,\n"
            + "a,b,c,d\n";

      Collection collection = importFacade.importDocuments(ImportFacade.FORMAT_CSV, commaSeparatedCsv);
      assertThat(collection).isNotNull();

      List<DataDocument> data = dataDao.getData(collection.getId(), query());
      assertThat(data).hasSize(3);

      int h1Num = 0;
      int h2Num = 0;
      int h3Num = 0;
      for (DataDocument dataDocument : data) {
         assertThat(dataDocument.keySet()).containsOnly("_id", "h1", "h2", "h3");
         String h1 = dataDocument.getString("h1");
         String h2 = dataDocument.getString("h2");
         String h3 = dataDocument.getString("h3");
         h1Num += h1 != null && h1.equals("a") ? 1 : 0;
         h2Num += h2 != null && h2.equals("b") ? 1 : 0;
         h3Num += h3 != null && h3.equals("c") ? 1 : 0;
      }
      assertThat(h1Num).isEqualTo(2);
      assertThat(h2Num).isEqualTo(1);
      assertThat(h3Num).isEqualTo(2);
   }

   private SearchQuery query() {
      return SearchQuery.createBuilder(USER).build();
   }

}
