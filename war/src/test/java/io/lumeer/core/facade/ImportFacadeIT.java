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

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.ImportedCollection;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.User;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.DataDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class ImportFacadeIT extends IntegrationTestBase {

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
   private static final String COLLECTION_NAME = "collection";
   private static final String COLLECTION_CODE = "collection";
   private static final String COLLECTION_ICON = "fa-user";
   private static final String COLLECTION_COLOR = "#ababab";

   private static final String PREFIX = Collection.ATTRIBUTE_PREFIX;

   private User user;

   @Before
   public void configureProject() {
      User user = new User(USER);
      this.user = userDao.createUser(user);

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new Permissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);

      Permissions organizationPermissions = new Permissions();
      Permission userPermission = Permission.buildWithRoles(this.user.getId(), Organization.ROLES);
      organizationPermissions.updateUserPermissions(userPermission);
      storedOrganization.setPermissions(organizationPermissions);
      organizationDao.updateOrganization(storedOrganization.getId(), storedOrganization);

      Project project = new Project();
      project.setCode(PROJECT_CODE);

      Permissions projectPermissions = new Permissions();
      projectPermissions.updateUserPermissions(new Permission(this.user.getId(), Project.ROLES));
      project.setPermissions(projectPermissions);
      Project storedProject = projectDao.createProject(project);

      workspaceKeeper.setWorkspaceIds(storedOrganization.getId(), storedProject.getId());

      collectionDao.setProject(storedProject);
   }

   @Test
   public void testImportEmptyCSV() {
      final String emptyCSV = "";
      ImportedCollection importedCollection = createImportObject(emptyCSV);
      Collection collection = importFacade.importDocuments(ImportFacade.FORMAT_CSV, importedCollection);
      assertThat(collection).isNotNull();

      List<DataDocument> data = dataDao.getData(collection.getId());
      assertThat(data).isEmpty();
   }

   @Test
   public void testImportCollectionInfo() {
      final String correctCsv = "h1;h2;h3;h4\n"
            + ";b;c;d\n"
            + ";;c;d\n"
            + "a;b;;d\n"
            + "a;b;;\n";
      ImportedCollection importedCollection = createImportObject(correctCsv);
      Collection collection = importFacade.importDocuments(ImportFacade.FORMAT_CSV, importedCollection);
      assertThat(collection).isNotNull();
      assertThat(collection.getName()).isEqualTo(COLLECTION_NAME);
      assertThat(collection.getCode()).isEqualTo(COLLECTION_CODE);
      assertThat(collection.getIcon()).isEqualTo(COLLECTION_ICON);
      assertThat(collection.getColor()).isEqualTo(COLLECTION_COLOR);
      assertThat(collection.getAttributes()).extracting(Attribute::getName).containsOnly("h1", "h2", "h3", "h4");
      assertThat(collection.getLastAttributeNum()).isEqualTo(4);
   }

   @Test
   public void testImportEmptyHeaderCSV() {
      final String noHeaderCsv = "\n"
            + "a;b;c;d\n"
            + "a;b;c;d\n";
      ImportedCollection importedCollection = createImportObject(noHeaderCsv);
      Collection collection = importFacade.importDocuments(ImportFacade.FORMAT_CSV, importedCollection);
      assertThat(collection).isNotNull();

      List<DataDocument> data = dataDao.getData(collection.getId());
      assertThat(data).hasSize(1); // it's because library ignores any leading empty lines
      assertThat(data.get(0).keySet()).containsOnly("_id", PREFIX + 1, PREFIX + 2, PREFIX + 3, PREFIX + 4);
      assertThat(data.get(0).getString(PREFIX + 1)).isEqualTo("a");
      assertThat(data.get(0).getString(PREFIX + 2)).isEqualTo("b");
      assertThat(data.get(0).getString(PREFIX + 3)).isEqualTo("c");
      assertThat(data.get(0).getString(PREFIX + 4)).isEqualTo("d");
   }

   @Test
   public void testImportNoLinesCSV() {
      final String noLinesCsv = "h1;h2;h3;h4\n";
      ImportedCollection importedCollection = createImportObject(noLinesCsv);
      Collection collection = importFacade.importDocuments(ImportFacade.FORMAT_CSV, importedCollection);
      assertThat(collection).isNotNull();

      List<DataDocument> data = dataDao.getData(collection.getId());
      assertThat(data).isEmpty();
   }

   @Test
   public void testImportWithSameHeaderAttributes() {
      final String correctCsv = "h1;h2;h1;h1\n"
            + "a;b;c;d\n";
      ImportedCollection importedCollection = createImportObject(correctCsv);
      Collection collection = importFacade.importDocuments(ImportFacade.FORMAT_CSV, importedCollection);
      assertThat(collection).isNotNull();

      DataDocument document = dataDao.getData(collection.getId()).get(0);
      assertThat(document).isNotNull();

      assertThat(document.keySet()).containsOnly("_id", PREFIX + 1, PREFIX + 2, PREFIX + 3, PREFIX + 4);
      assertThat(document.getString(PREFIX + 1)).isEqualTo("a");
      assertThat(document.getString(PREFIX + 2)).isEqualTo("b");
      assertThat(document.getString(PREFIX + 3)).isEqualTo("c");
      assertThat(document.getString(PREFIX + 4)).isEqualTo("d");
   }

   @Test
   public void testImportCorrectCSV() {
      final String correctCsv = "h1;h2;h3;h4\n"
            + ";b;c;d\n"
            + ";;c;d\n"
            + "a;b;;d\n"
            + "a;b;;\n";
      ImportedCollection importedCollection = createImportObject(correctCsv);
      Collection collection = importFacade.importDocuments(ImportFacade.FORMAT_CSV, importedCollection);
      assertThat(collection).isNotNull();

      List<DataDocument> data = dataDao.getData(collection.getId());
      assertThat(data).hasSize(4);

      int h1Num = 0;
      int h2Num = 0;
      int h3Num = 0;
      int h4Num = 0;
      for (DataDocument dataDocument : data) {
         String h1 = dataDocument.getString(PREFIX + 1);
         String h2 = dataDocument.getString(PREFIX + 2);
         String h3 = dataDocument.getString(PREFIX + 3);
         String h4 = dataDocument.getString(PREFIX + 4);
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
   public void testImportDiffRowsLengthCSV() {
      final String diffRowsLengthCsv = "h1;h2;h3\n"
            + "a;b;c;d\n"
            + "a;b;c;d;e;f;g;h\n"
            + "\n\n\n\n\n"
            + "a;b;c\n";

      ImportedCollection importedCollection = createImportObject(diffRowsLengthCsv);
      Collection collection = importFacade.importDocuments(ImportFacade.FORMAT_CSV, importedCollection);
      assertThat(collection).isNotNull();

      List<DataDocument> data = dataDao.getData(collection.getId());
      assertThat(data).hasSize(3);

      int h1Num = 0;
      int h2Num = 0;
      int h3Num = 0;
      for (DataDocument dataDocument : data) {
         String h1 = dataDocument.getString(PREFIX + 1);
         String h2 = dataDocument.getString(PREFIX + 2);
         String h3 = dataDocument.getString(PREFIX + 3);
         h1Num += h1 != null && h1.equals("a") ? 1 : 0;
         h2Num += h2 != null && h2.equals("b") ? 1 : 0;
         h3Num += h3 != null && h3.equals("c") ? 1 : 0;
      }
      assertThat(h1Num).isEqualTo(3);
      assertThat(h2Num).isEqualTo(3);
      assertThat(h3Num).isEqualTo(3);
   }

   @Test
   public void testImportCommaSeparatedCSV() {
      final String commaSeparatedCsv = "h1,h2,h3\n"
            + "a,,c,d\n"
            + ",,,\n"
            + "a,b,c,d\n";
      ImportedCollection importedCollection = createImportObject(commaSeparatedCsv);
      Collection collection = importFacade.importDocuments(ImportFacade.FORMAT_CSV, importedCollection);
      assertThat(collection).isNotNull();

      List<DataDocument> data = dataDao.getData(collection.getId());
      assertThat(data).hasSize(3);

      int h1Num = 0;
      int h2Num = 0;
      int h3Num = 0;
      for (DataDocument dataDocument : data) {
         String h1 = dataDocument.getString(PREFIX + 1);
         String h2 = dataDocument.getString(PREFIX + 2);
         String h3 = dataDocument.getString(PREFIX + 3);
         h1Num += h1 != null && h1.equals("a") ? 1 : 0;
         h2Num += h2 != null && h2.equals("b") ? 1 : 0;
         h3Num += h3 != null && h3.equals("c") ? 1 : 0;
      }
      assertThat(h1Num).isEqualTo(2);
      assertThat(h2Num).isEqualTo(1);
      assertThat(h3Num).isEqualTo(2);
   }

   private ImportedCollection createImportObject(String data) {
      return new ImportedCollection(new Collection(COLLECTION_CODE, COLLECTION_NAME, COLLECTION_ICON, COLLECTION_COLOR, new Permissions()), data);
   }

}
