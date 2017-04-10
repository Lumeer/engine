package io.lumeer.engine.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@RunWith(Arquillian.class)
public class OrganizationFacadeIntegrationTest extends IntegrationTestBase {

   @Inject
   @SystemDataStorage
   private DataStorage dataStorage;

   @Inject
   private DataStorageDialect dialect;

   @Inject
   private OrganizationFacade organizationFacade;

   @Test
   public void testReadOrganizationsMap() throws Exception {
      dropDocuments(LumeerConst.Organization.COLLECTION_NAME);
      createDummyEntries();

      Map<String, String> organizationsMap = organizationFacade.readOrganizationsMap();
      List<DataDocument> organizations = getOrganizationEntries();
      Map<String, String> newOrganizationsMap = new HashMap<>();

      organizations.forEach(organization -> newOrganizationsMap.put(organization.getString(LumeerConst.Organization.ATTR_ORG_ID), organization.getString(LumeerConst.Organization.ATTR_ORG_NAME)));

      assertThat(organizationsMap).isEqualTo(newOrganizationsMap);
      assertThat(organizationsMap.entrySet().size()).isEqualTo(newOrganizationsMap.entrySet().size());
   }

   @Test
   public void testReadOrganizationId() throws Exception {
      dropDocuments(LumeerConst.Organization.COLLECTION_NAME);
      createDummyEntries();

      final String DUMMY_ORG_ID = "TST3";
      final String DUMMY_ORG_NAME = "Test3";
      String organizationId = organizationFacade.readOrganizationId(DUMMY_ORG_NAME);
      assertThat(DUMMY_ORG_ID).isEqualTo(organizationId);
   }

   @Test
   public void testReadOrganizationName() {
      dropDocuments(LumeerConst.Organization.COLLECTION_NAME);
      createDummyEntries();

      final String DUMMY_ORG_ID = "TST4";
      final String DUMMY_ORG_NAME = "Test4";
      String organizationName = organizationFacade.readOrganizationName(DUMMY_ORG_ID);
      assertThat(DUMMY_ORG_NAME).isEqualTo(organizationName);
   }

   @Test
   public void testReadUpdateAndDropOrganizationMetadata() throws Exception {
      dropDocuments(LumeerConst.Organization.COLLECTION_NAME);
      createDummyEntries();

      // #1 update (set)
      final String DUMMY_ORG_ID = "TST3";
      final String DUMMY_META_COLOR_VALUE = "#000000";
      organizationFacade.updateOrganizationMetadata(DUMMY_ORG_ID, LumeerConst.Organization.ATTR_META_COLOR, DUMMY_META_COLOR_VALUE);

      // #2 read
      String dummyMetaColorValue = organizationFacade.readOrganizationMetadata(DUMMY_ORG_ID, LumeerConst.Organization.ATTR_META_COLOR);
      assertThat(DUMMY_META_COLOR_VALUE).isEqualTo(dummyMetaColorValue);

      // #3 update
      final String DUMMY_NEW_META_COLOR_VALUE = "#111111";
      organizationFacade.updateOrganizationMetadata(DUMMY_ORG_ID, LumeerConst.Organization.ATTR_META_COLOR, DUMMY_NEW_META_COLOR_VALUE);

      // #4 read
      String dummyNewMetaColorValue = organizationFacade.readOrganizationMetadata(DUMMY_ORG_ID, LumeerConst.Organization.ATTR_META_COLOR);
      assertThat(DUMMY_NEW_META_COLOR_VALUE).isEqualTo(dummyNewMetaColorValue);

      // #5 drop
      organizationFacade.dropOrganizationMetadata(DUMMY_ORG_ID, LumeerConst.Organization.ATTR_META_COLOR);

      // #6 read
      String removedDummyMetaColorValue = organizationFacade.readOrganizationMetadata(DUMMY_ORG_ID, LumeerConst.Organization.ATTR_META_COLOR);
      assertThat(removedDummyMetaColorValue).isNull();
   }

   @Test
   public void testCreateAndDropOrganization() throws Exception {
      final int DUMMY_ENTRIES_COUNT = 5; // number of simple dummy entries to create
      dropDocuments(LumeerConst.Organization.COLLECTION_NAME);

      // #1 create
      final String NEW_ORG_ID = "LMR";
      final String NEW_ORG_NAME = "Lumeer";
      organizationFacade.createOrganization(NEW_ORG_ID, NEW_ORG_NAME);

      // #2 drop
      organizationFacade.dropOrganization(NEW_ORG_ID);
      long documentCount1 = dataStorage.documentCount(LumeerConst.Organization.COLLECTION_NAME);
      assertThat(documentCount1).isEqualTo(0);

      // #3 create
      createDummyEntries();
      organizationFacade.createOrganization(NEW_ORG_ID, NEW_ORG_NAME);

      // #4 drop
      final String DUMMY_ORG_ID = "TST2";
      organizationFacade.dropOrganization(DUMMY_ORG_ID);
      long documentCount2 = dataStorage.documentCount(LumeerConst.Organization.COLLECTION_NAME);
      assertThat(documentCount2).isEqualTo(DUMMY_ENTRIES_COUNT);
   }

   @Test
   public void testRenameOrganization() throws Exception {
      dropDocuments(LumeerConst.Organization.COLLECTION_NAME);
      createDummyEntries();

      final String DUMMY_ORG_ID = "TST2";
      final String DUMMY_ORG_NAME = "Test2";
      final String NEW_DUMMY_ORG_NAME = "Test2New";
      organizationFacade.renameOrganization(DUMMY_ORG_ID, NEW_DUMMY_ORG_NAME);

      Map<String, String> organizationsMap = organizationFacade.readOrganizationsMap();
      assertThat(organizationsMap).containsEntry(DUMMY_ORG_ID, NEW_DUMMY_ORG_NAME);
      assertThat(organizationsMap).doesNotContainEntry(DUMMY_ORG_ID, DUMMY_ORG_NAME);
   }

   @Test
   public void testReadUpdateResetAndDropOrganizationInfoData() throws Exception {
      dropDocuments(LumeerConst.Organization.COLLECTION_NAME);
      createDummyEntries();

      final String DUMMY_ORG_ID = "TST2";
      final String INFO_YR_ATTR = "year";
      final String INFO_YR_VALUE = "2017";
      final String INFO_YR_NEW_VALUE = "2018";
      final String INFO_WEBSITE_ATTR = "website";
      final String INFO_WEBSITE_VALUE = "www.lumeer.io";

      // #1 Empty info data document was created  while organization was being created, but the document is still empty.
      DataDocument infoDataDocument = organizationFacade.readOrganizationInfoData(DUMMY_ORG_ID);
      assertThat(infoDataDocument).isEmpty();

      String notSetAttributeValue = organizationFacade.readOrganizationInfoData(DUMMY_ORG_ID, INFO_YR_ATTR);
      assertThat(notSetAttributeValue).isNull();

      // #2 update single attribute. Attribute will be created.
      organizationFacade.updateOrganizationInfoData(DUMMY_ORG_ID, INFO_YR_ATTR, INFO_YR_VALUE);

      // #3 read
      String readInfoValue = organizationFacade.readOrganizationInfoData(DUMMY_ORG_ID, INFO_YR_ATTR);
      assertThat(readInfoValue).isEqualTo(INFO_YR_VALUE);

      // #4 update whole info document
      DataDocument infoDocumentToUpdate = new DataDocument(INFO_YR_ATTR, INFO_YR_NEW_VALUE);
      infoDocumentToUpdate.put(INFO_WEBSITE_ATTR, INFO_WEBSITE_VALUE);
      organizationFacade.updateOrganizationInfoData(DUMMY_ORG_ID, infoDocumentToUpdate);

      // #5 read whole info document
      DataDocument infoDataDocument2 = organizationFacade.readOrganizationInfoData(DUMMY_ORG_ID);
      infoDataDocument2.remove(LumeerConst.Document.ID);
      assertThat(infoDataDocument2).isEqualTo(infoDocumentToUpdate);

      // #6 read single info attribute value
      String websiteValue = organizationFacade.readOrganizationInfoData(DUMMY_ORG_ID, INFO_WEBSITE_ATTR);
      assertThat(websiteValue).isEqualTo(INFO_WEBSITE_VALUE);

      // #7 drop single info attribute
      organizationFacade.dropOrganizationInfoDataAttribute(DUMMY_ORG_ID, INFO_YR_ATTR);
      DataDocument infoDataDocumentWithoutYearAttr = organizationFacade.readOrganizationInfoData(DUMMY_ORG_ID);
      assertThat(infoDataDocumentWithoutYearAttr).doesNotContainKey(INFO_YR_ATTR);
      assertThat(infoDataDocumentWithoutYearAttr).containsKey(INFO_WEBSITE_ATTR);

      // #8 reset whole info document
      organizationFacade.resetOrganizationInfoData(DUMMY_ORG_ID);
      DataDocument resetInfoData = organizationFacade.readOrganizationInfoData(DUMMY_ORG_ID);
      assertThat(resetInfoData).isEmpty();
   }

   /* ******************* USERS and ROLES ******************* */

   @Test
   public void testReadOrganizationUsers() throws Exception {
      dropDocuments(LumeerConst.Organization.COLLECTION_NAME);
      createDummyEntries();

      final String ORG_ID = "TST1";
      final String USER_NAME_1 = "tst1@lumeer.io";
      final String USER_NAME_2 = "tst2@lumeer.io";
      final String USER_NAME_3 = "tst3@lumeer.io";
      Map<String, List<String>> usersAndRoles = organizationFacade.readOrganizationUsers(ORG_ID);
      assertThat(usersAndRoles).hasSize(3);
      assertThat(usersAndRoles).containsOnlyKeys(USER_NAME_1, USER_NAME_2, USER_NAME_3);
   }

   @Test
   public void testReadUserOrganizations() throws Exception {
      dropDocuments(LumeerConst.Organization.COLLECTION_NAME);
      createDummyEntries();

      final String USER_NAME = "tst2@lumeer.io";
      Map<String, String> userOrganizations = organizationFacade.readUserOrganizations(USER_NAME);
      assertThat(userOrganizations).hasSize(4);
      assertThat(userOrganizations).containsOnlyKeys("TST0", "TST1", "TST3", "TST4");
      assertThat(userOrganizations).containsValues("Test0", "Test1", "Test3", "Test4");
   }

   @Test
   public void testAddUserToOrganization() throws Exception {
      dropDocuments(LumeerConst.Organization.COLLECTION_NAME);
      createDummyEntries();

      // #1 default roles that were set before
      final String ORG_ID_3 = "TST3";
      final String USER_NAME_5 = "tst5@lumeer.io";

      List<String> defaultRolesToSet = Arrays.asList("dflR1", "dflR2", "dflR3");
      organizationFacade.setDefaultRoles(ORG_ID_3, defaultRolesToSet);

      Map<String, List<String>> usersInTst3 = organizationFacade.readOrganizationUsers(ORG_ID_3);
      assertThat(usersInTst3).doesNotContainKey(USER_NAME_5);

      organizationFacade.addUserToOrganization(ORG_ID_3, USER_NAME_5);

      Map<String, List<String>> usersInTst3After = organizationFacade.readOrganizationUsers(ORG_ID_3);
      assertThat(usersInTst3After).containsKey(USER_NAME_5);

      // #2 specified roles
      final String ORG_ID_4 = "TST4";
      final String USER_NAME_6 = "tst6@lumeer.io";
      final List<String> ROLES_6 = Arrays.asList("r5", "r6", "r7", "r8");

      Map<String, List<String>> usersInTst4 = organizationFacade.readOrganizationUsers(ORG_ID_4);
      assertThat(usersInTst4).doesNotContainKey(USER_NAME_6);

      organizationFacade.addUserToOrganization(ORG_ID_4, USER_NAME_6, ROLES_6);

      Map<String, List<String>> usersInTst4After = organizationFacade.readOrganizationUsers(ORG_ID_4);
      assertThat(usersInTst4After).containsKey(USER_NAME_6);
      assertThat(usersInTst4After.get(USER_NAME_6)).isEqualTo(ROLES_6);
   }

   @Test
   public void testRemoveUserFromOrganization() throws Exception {
      dropDocuments(LumeerConst.Organization.COLLECTION_NAME);
      createDummyEntries();

      final String ORG_ID = "TST1";
      final String USER_NAME = "tst2@lumeer.io";

      // check the given user exists before it will be removed
      Map<String, List<String>> usersInOrg = organizationFacade.readOrganizationUsers(ORG_ID);
      assertThat(usersInOrg).containsKey(USER_NAME);

      // to remove the user
      organizationFacade.removeUserFromOrganization(ORG_ID, USER_NAME);

      // check the given user was removed successfully
      Map<String, List<String>> usersInOrg2 = organizationFacade.readOrganizationUsers(ORG_ID);
      assertThat(usersInOrg2).doesNotContainKeys(USER_NAME);
   }

   @Test
   public void testAddRolesToUser() throws Exception {
      dropDocuments(LumeerConst.Organization.COLLECTION_NAME);
      createDummyEntries();

      final String ORG_ID_4 = "TST4";
      final String USER_NAME_2 = "tst2@lumeer.io";
      final List<String> ROLES_2 = Arrays.asList("r5", "r6", "r7", "r8");

      List<String> readRolesBefore = organizationFacade.readUserRoles(ORG_ID_4, USER_NAME_2);
      assertThat(organizationFacade.hasUserRoleInOrganization(ORG_ID_4, USER_NAME_2, "r7")).isFalse();
      organizationFacade.addRolesToUser(ORG_ID_4, USER_NAME_2, ROLES_2);

      List<String> readRolesAfter = organizationFacade.readUserRoles(ORG_ID_4, USER_NAME_2);
      List<String> joinLists = new ArrayList<>(readRolesBefore);
      joinLists.addAll(ROLES_2);
      assertThat(readRolesAfter).isEqualTo(joinLists);
      assertThat(organizationFacade.hasUserRoleInOrganization(ORG_ID_4, USER_NAME_2, "r7")).isTrue();

      List<String> readUserRoles = organizationFacade.readUserRoles(ORG_ID_4, USER_NAME_2);
      assertThat(readUserRoles).isEqualTo(joinLists);

      organizationFacade.removeRolesFromUser(ORG_ID_4, USER_NAME_2, ROLES_2);
      List<String> readUserRolesAfterDeletion = organizationFacade.readUserRoles(ORG_ID_4, USER_NAME_2);
      assertThat(readUserRolesAfterDeletion).isEqualTo(readRolesBefore);
      assertThat(organizationFacade.hasUserRoleInOrganization(ORG_ID_4, USER_NAME_2, "r7")).isFalse();
   }

   @Test
   public void testSetAndReadDefaultRoles() throws Exception {
      dropDocuments(LumeerConst.Organization.COLLECTION_NAME);
      createDummyEntries();

      // to read not set default roles
      final String ORG_ID = "TST3";
      List<String> notSetDefaultRoles = organizationFacade.readDefaultRoles(ORG_ID);
      assertThat(notSetDefaultRoles).isNull();

      // to set default roles
      List<String> defaultRolesToSet = Arrays.asList("dflR1", "dflR2", "dflR3");
      organizationFacade.setDefaultRoles(ORG_ID, defaultRolesToSet);

      // to read default roles
      List<String> readDefaultRoles = organizationFacade.readDefaultRoles(ORG_ID);
      assertThat(readDefaultRoles).isEqualTo(defaultRolesToSet);
   }

   private void createDummyEntries() {
      final int DUMMY_ENTRIES_COUNT = 5; // number of simple dummy entries to create

      final String ORG_ID = "TST";
      final String ORG_NAME = "Test";

      for (int i = 0; i < DUMMY_ENTRIES_COUNT; i++) {
         DataDocument organization = new DataDocument();
         organization.put(LumeerConst.Organization.ATTR_ORG_ID, ORG_ID + i);
         organization.put(LumeerConst.Organization.ATTR_ORG_NAME, ORG_NAME + i);
         organization.put(LumeerConst.Organization.ATTR_META_ICON, "fa-icon");
         organization.put(LumeerConst.Organization.ATTR_META_COLOR, "#FFFFFF");
         organization.put(LumeerConst.Organization.ATTR_ORG_DATA, new DataDocument());

         organization.put(LumeerConst.Organization.ATTR_USERS, createDummyUsers(i));
         dataStorage.createDocument(LumeerConst.Organization.COLLECTION_NAME, organization);
      }
   }

   private List<DataDocument> getOrganizationEntries() {
      return dataStorage.searchIncludeAttrs(LumeerConst.Organization.COLLECTION_NAME, null, Arrays.asList(LumeerConst.Organization.ATTR_ORG_NAME, LumeerConst.Organization.ATTR_ORG_ID));
   }

   private void dropDocuments(final String collectionName) {
      dataStorage.dropManyDocuments(collectionName, dialect.documentFilter("{}"));
   }

   private List<DataDocument> createDummyUsers(final int i) {
      final String USER_NAME_0 = "tst0@lumeer.io";
      final String USER_NAME_1 = "tst1@lumeer.io";
      final String USER_NAME_2 = "tst2@lumeer.io";
      final String USER_NAME_3 = "tst3@lumeer.io";

      final List<String> ROLES_0 = Arrays.asList("r0", "r1", "r2");
      final List<String> ROLES_1 = Arrays.asList("r1", "r2", "r3");
      final List<String> ROLES_2 = Arrays.asList("r2", "r3", "r4");
      final List<String> ROLES_3 = Arrays.asList("r1", "r2", "r4");

      DataDocument doc0 = new DataDocument(LumeerConst.Organization.ATTR_USERS_USERNAME, USER_NAME_0);
      doc0.put(LumeerConst.Organization.ATTR_USERS_USER_ROLES, ROLES_0);

      DataDocument doc1 = new DataDocument(LumeerConst.Organization.ATTR_USERS_USERNAME, USER_NAME_1);
      doc1.put(LumeerConst.Organization.ATTR_USERS_USER_ROLES, ROLES_1);

      DataDocument doc2 = new DataDocument(LumeerConst.Organization.ATTR_USERS_USERNAME, USER_NAME_2);
      doc2.put(LumeerConst.Organization.ATTR_USERS_USER_ROLES, ROLES_2);

      DataDocument doc3 = new DataDocument(LumeerConst.Organization.ATTR_USERS_USERNAME, USER_NAME_3);
      doc3.put(LumeerConst.Organization.ATTR_USERS_USER_ROLES, ROLES_3);

      List<DataDocument> list0 = new ArrayList<>();
      list0.addAll(Arrays.asList(doc0, doc1, doc2));

      List<DataDocument> list1 = new ArrayList<>();
      list1.addAll(Arrays.asList(doc1, doc2, doc3));

      List<DataDocument> list2 = new ArrayList<>();
      list2.addAll(Arrays.asList(doc1, doc0, doc3));

      List<DataDocument> list3 = new ArrayList<>();
      list3.addAll(Arrays.asList(doc2, doc0, doc3));

      switch (i) {
         case 0:
            // TST0, Test0
            return list0;
         case 1:
            // TST1, Test1
            return list1;
         case 2:
            // TST2, Test2
            return list2;
         case 3:
            // TST3, Test3
            return list3;
         case 4:
            // TST4, Test4
            return list1;
      }
      return null;
   }

   private void wait(int minutes) throws InterruptedException {
      final int MILLS_IN_SECS = 1000;
      final int SECS_IN_MINS = 60;
      Thread.sleep(minutes * MILLS_IN_SECS * SECS_IN_MINS);
   }
}
