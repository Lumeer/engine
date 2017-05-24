package io.lumeer.engine.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;

import com.mongodb.MongoWriteException;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

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
   public void testReadOrganizations() throws Exception {
      dropDocuments(LumeerConst.Organization.COLLECTION_NAME);
      createDummyEntries();

      List<DataDocument> organizations = organizationFacade.readOrganizations();
      assertThat(organizations).extracting(LumeerConst.Organization.ATTR_ORG_CODE).contains("TST0", "TST1", "TST2", "TST3", "TST4");
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
      assertThat(organizationFacade.readOrganizationName("TST2")).isNotEqualTo(NEW_DUMMY_ORG_NAME);
      organizationFacade.renameOrganization(DUMMY_ORG_ID, NEW_DUMMY_ORG_NAME);
      assertThat(organizationFacade.readOrganizationName("TST2")).isNotEqualTo(DUMMY_ORG_NAME);
      assertThat(organizationFacade.readOrganizationName("TST2")).isEqualTo(NEW_DUMMY_ORG_NAME);
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

   @Test
   public void organizationAlreadyExists() throws Exception {
      dropDocuments(LumeerConst.Organization.COLLECTION_NAME);
      String organization1 = "LMR9998";
      String organization2 = "LMR9999";
      organizationFacade.createOrganization(organization1, "Organization One");
      organizationFacade.createOrganization(organization2, "Organization Two");

      assertThatThrownBy(() -> organizationFacade.createOrganization(organization1, "Organization One again")).isInstanceOf(MongoWriteException.class);
      assertThatThrownBy(() -> organizationFacade.updateOrganizationCode(organization1, organization2)).isInstanceOf(MongoWriteException.class);
   }

   private void createDummyEntries() {
      final int DUMMY_ENTRIES_COUNT = 5; // number of simple dummy entries to create

      final String ORG_ID = "TST";
      final String ORG_NAME = "Test";

      for (int i = 0; i < DUMMY_ENTRIES_COUNT; i++) {
         DataDocument organization = new DataDocument();
         organization.put(LumeerConst.Organization.ATTR_ORG_CODE, ORG_ID + i);
         organization.put(LumeerConst.Organization.ATTR_ORG_NAME, ORG_NAME + i);
         organization.put(LumeerConst.Organization.ATTR_META_ICON, "fa-icon");
         organization.put(LumeerConst.Organization.ATTR_META_COLOR, "#FFFFFF");
         organization.put(LumeerConst.Organization.ATTR_ORG_DATA, new DataDocument());
         dataStorage.createDocument(LumeerConst.Organization.COLLECTION_NAME, organization);
      }
   }

   private List<DataDocument> getOrganizationEntries() {
      return dataStorage.search(LumeerConst.Organization.COLLECTION_NAME, null, Arrays.asList(LumeerConst.Organization.ATTR_ORG_NAME, LumeerConst.Organization.ATTR_ORG_CODE));
   }

   private void dropDocuments(final String collectionName) {
      dataStorage.dropManyDocuments(collectionName, dialect.documentFilter("{}"));
   }

}
