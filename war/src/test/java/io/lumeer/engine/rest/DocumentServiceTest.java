package io.lumeer.engine.rest;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.engine.controller.DocumentFacade;
import io.lumeer.engine.controller.DocumentMetadataFacade;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.UserFacade;
import io.lumeer.engine.rest.dao.AccessRightsDao;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
public class DocumentServiceTest extends Arquillian {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "DocumentServiceTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-ci.properties")
                       .addAsResource("defaults-dev.properties");
   }

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private DataStorage dataStorage;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private DocumentMetadataFacade documentMetadataFacade;

   @Inject
   private SecurityFacade securityFacade;

   @Inject
   private UserFacade userFacade;

   private final String TARGET_URI = "http://localhost:8080";

   private final String COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT = "DocumentServiceCollectionCreateReadUpdateAndDropDocument";
   private final String COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA = "DocumentServiceCollectionAddReadAndUpdateDocumentMetadata";
   private final String COLLECTION_SEARCH_HISTORY_CHANGES = "DocumentServiceCollectionSearchHistoryChanges";
   private final String COLLECTION_REVERT_DOCUMENT_VERSION = "DocumentServiceCollectionRevertDocumentVersion";
   private final String COLLECTION_READ_ACCESS_RIGHTS = "DocumentServiceCollectionrReadAccessRights";
   private final String COLLECTION_UPDATE_ACCESS_RIGHTS = "DocumentServiceCollectionrUpdateAccessRights";

   @Test
   public void testRegister() throws Exception {
      Assert.assertNotNull(documentFacade);
   }

   @Test
   public void testCreateReadUpdateAndDropDocument() throws Exception {
      setUpCollections(COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT);
      final Client client = ClientBuilder.newBuilder().build();

      // 200 - the document will be inserted into the given collection
      collectionFacade.createCollection(COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT);
      Response response = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT)).request().buildPost(Entity.json(new DataDocument())).invoke();
      String documentId = response.readEntity(String.class);
      Assert.assertTrue(response.getStatus() == Response.Status.OK.getStatusCode()
            && documentFacade.readDocument(getInternalName(COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT), documentId) != null);
      response.close();

      // 200 - read the given document by its id
      Response response2 = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT) + documentId).request().buildGet().invoke();
      DataDocument document = response2.readEntity(DataDocument.class);
      Assert.assertTrue(response2.getStatus() == Response.Status.OK.getStatusCode()
            && documentFacade.readDocument(getInternalName(COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT), documentId).equals(document));
      response2.close();

      // 204 - update the document
      DataDocument updatedDocument = new DataDocument();
      updatedDocument.put("_id", documentId);
      updatedDocument.put("name", "updatedDocument");
      Response response3 = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT + "/update")).request().buildPut(Entity.json(updatedDocument)).invoke();
      Assert.assertTrue(response3.getStatus() == Response.Status.NO_CONTENT.getStatusCode()
            && documentFacade.readDocument(getInternalName(COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT), documentId).getString("name").equals("updatedDocument"));
      response3.close();

      // 204 - drop the given document by its id
      Response response4 = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_CREATE_READ_UPDATE_AND_DROP_DOCUMENT) + documentId).request().buildDelete().invoke();
      Assert.assertTrue(response4.getStatus() == Response.Status.NO_CONTENT.getStatusCode());
      response4.close();

      client.close();
   }

   @Test
   public void testAddReadAndUpdateDocumentMetadata() throws Exception {
      setUpCollections(COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA);
      final Client client = ClientBuilder.newBuilder().build();
      final String attributeName = "_meta-update-user";
      final Object metaObjectValue = 123;

      collectionFacade.createCollection(COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA);
      String documentId = documentFacade.createDocument(getInternalName(COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA), new DataDocument());

      // 204 - add the document metadata
      Response response = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA) + documentId + "/meta/" + attributeName).request().buildPost(Entity.entity(metaObjectValue, MediaType.APPLICATION_JSON)).invoke();
      Map<String, Object> documentMetadata = documentMetadataFacade.readDocumentMetadata(getInternalName(COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA), documentId);
      Assert.assertTrue(response.getStatus() == Response.Status.NO_CONTENT.getStatusCode() && documentMetadata.get(attributeName).equals(metaObjectValue));
      response.close();

      // 200 - read the document metadata
      Response response2 = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA) + documentId + "/meta/").request().buildGet().invoke();
      Map<String, Object> metaDocument = response2.readEntity(Map.class);
      Assert.assertTrue(response2.getStatus() == Response.Status.OK.getStatusCode() && metaDocument.equals(documentMetadata));
      response2.close();

      // 204 - update the document metadata
      DataDocument updatedMetaDocument = new DataDocument();
      updatedMetaDocument.put(attributeName, "updatedValue");
      Response response3 = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA) + documentId + "/meta").request().buildPut(Entity.json(updatedMetaDocument)).invoke();
      Assert.assertTrue(response3.getStatus() == Response.Status.NO_CONTENT.getStatusCode()
            && documentMetadataFacade.readDocumentMetadata(getInternalName(COLLECTION_ADD_READ_AND_UPDATE_DOCUMENT_METADATA), documentId).get(attributeName).equals("updatedValue"));
      response3.close();

      client.close();
   }

   @Test
   public void testSearchHistoryChanges() throws Exception {
      setUpCollections(COLLECTION_SEARCH_HISTORY_CHANGES);
      final Client client = ClientBuilder.newBuilder().build();

      collectionFacade.createCollection(COLLECTION_SEARCH_HISTORY_CHANGES);
      String documentId = documentFacade.createDocument(getInternalName(COLLECTION_SEARCH_HISTORY_CHANGES), new DataDocument());

      // only document exists, no changes in the past, code 200, listsize = 1
      Response response = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_SEARCH_HISTORY_CHANGES) + documentId + "/versions/").request().buildGet().invoke();
      List<DataDocument> changedDocuments = response.readEntity(ArrayList.class);
      Assert.assertTrue(response.getStatus() == Response.Status.OK.getStatusCode() && changedDocuments.size() == 1);
      response.close();

      // two changes performed in the past + original document, code 200, listsize = 3
      DataDocument documentVersionOne = new DataDocument("_id", documentId);
      DataDocument documentVersionTwo = new DataDocument("_id", documentId);
      documentVersionOne.put("dummyVersionOneAttribute", 1);
      documentVersionTwo.put("dummyVersionTwoAttribute", 2);
      documentFacade.updateDocument(getInternalName(COLLECTION_SEARCH_HISTORY_CHANGES), documentVersionOne);
      documentFacade.updateDocument(getInternalName(COLLECTION_SEARCH_HISTORY_CHANGES), documentVersionTwo);

      Response response2 = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_SEARCH_HISTORY_CHANGES) + documentId + "/versions/").request().buildGet().invoke();
      List<DataDocument> changedDocuments2 = response2.readEntity(ArrayList.class);
      Assert.assertTrue(response2.getStatus() == Response.Status.OK.getStatusCode() && changedDocuments2.size() == 3);
      response2.close();

      client.close();
   }

   @Test
   public void testRevertDocumentVersion() throws Exception {
      setUpCollections(COLLECTION_REVERT_DOCUMENT_VERSION);
      final Client client = ClientBuilder.newBuilder().build();

      collectionFacade.createCollection(COLLECTION_REVERT_DOCUMENT_VERSION);
      String documentId = documentFacade.createDocument(getInternalName(COLLECTION_REVERT_DOCUMENT_VERSION), new DataDocument());
      DataDocument documentVersionOne = new DataDocument("_id", documentId);
      DataDocument documentVersionTwo = new DataDocument("_id", documentId);
      documentVersionOne.put("dummyVersionOneAttribute", 1);
      documentVersionTwo.put("dummyVersionTwoAttribute", 2);
      documentFacade.updateDocument(getInternalName(COLLECTION_REVERT_DOCUMENT_VERSION), documentVersionOne);
      documentFacade.updateDocument(getInternalName(COLLECTION_REVERT_DOCUMENT_VERSION), documentVersionTwo);

      DataDocument documentVersion2 = documentFacade.readDocument(getInternalName(COLLECTION_REVERT_DOCUMENT_VERSION), documentId);
      int versionTwo = documentVersion2.getInteger(LumeerConst.Document.METADATA_VERSION_KEY);
      Assert.assertTrue(versionTwo == 2);

      Response response = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_REVERT_DOCUMENT_VERSION) + documentId + "/versions/" + 1).request().buildPost(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      DataDocument currentDocument = documentFacade.readDocument(getInternalName(COLLECTION_REVERT_DOCUMENT_VERSION), documentId);
      int versionThree = currentDocument.getInteger(LumeerConst.Document.METADATA_VERSION_KEY);
      boolean isFirstVersion = !currentDocument.containsKey("dummyVersionTwoAttribute");
      Assert.assertTrue(response.getStatus() == Response.Status.NO_CONTENT.getStatusCode() && versionThree == 3 && isFirstVersion);
      response.close();

      client.close();
   }

 /*  @Test
   public void testReadAccessRights() throws Exception {
      setUpCollections(COLLECTION_READ_ACCESS_RIGHTS);
      final Client client = ClientBuilder.newBuilder().build();
      final int DEFAULT_INT_RULE = 7;

      collectionFacade.createCollection(COLLECTION_READ_ACCESS_RIGHTS);
      String documentId = documentFacade.createDocument(getInternalName(COLLECTION_READ_ACCESS_RIGHTS), new DataDocument());

      Response response = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_READ_ACCESS_RIGHTS) + documentId + "/rights").request().buildGet().invoke();
      HashMap rights = response.readEntity(HashMap.class);
      int ruleNumber = (int) rights.get(userFacade.getUserEmail());
      Assert.assertTrue(response.getStatus() == Response.Status.OK.getStatusCode() && ruleNumber == DEFAULT_INT_RULE);
      response.close();

      client.close();
   }*/

   @Test
   public void testReadAccessRights() throws Exception {
      setUpCollections(COLLECTION_READ_ACCESS_RIGHTS);
      final Client client = ClientBuilder.newBuilder().build();
      final String user = userFacade.getUserEmail();
      final AccessRightsDao DEFAULT_ACCESS_RIGHT = new AccessRightsDao(true, true, true, user);

      collectionFacade.createCollection(COLLECTION_READ_ACCESS_RIGHTS);
      String documentId = documentFacade.createDocument(getInternalName(COLLECTION_READ_ACCESS_RIGHTS), new DataDocument());

      Response response = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_READ_ACCESS_RIGHTS) + documentId + "/rights").request().buildGet().invoke();
      List<AccessRightsDao> rights = response.readEntity(new GenericType<List<AccessRightsDao>>() {
      });
      AccessRightsDao readRights = rights.get(0);
      Assert.assertTrue(response.getStatus() == Response.Status.OK.getStatusCode()
            && readRights.isRead() == DEFAULT_ACCESS_RIGHT.isRead()
            && readRights.isWrite() == DEFAULT_ACCESS_RIGHT.isWrite()
            && readRights.isExecute() == DEFAULT_ACCESS_RIGHT.isExecute()
            && readRights.getUserName().equals(DEFAULT_ACCESS_RIGHT.getUserName()));
      response.close();
      client.close();
   }

   @Test
   public void testUpdateAccessRights() throws Exception {
      setUpCollections(COLLECTION_UPDATE_ACCESS_RIGHTS);
      final Client client = ClientBuilder.newBuilder().build();
      final String user = userFacade.getUserEmail();
      final AccessRightsDao accessRightsDao = new AccessRightsDao(false, false, true, user);

      collectionFacade.createCollection(COLLECTION_UPDATE_ACCESS_RIGHTS);
      String documentId = documentFacade.createDocument(getInternalName(COLLECTION_UPDATE_ACCESS_RIGHTS), new DataDocument());

      Response response = client.target(TARGET_URI).path(setPathPrefix(COLLECTION_UPDATE_ACCESS_RIGHTS) + documentId + "/rights").request().buildPut(Entity.entity(accessRightsDao, MediaType.APPLICATION_JSON)).invoke();
      AccessRightsDao readAccessRights = securityFacade.getDao(getInternalName(COLLECTION_UPDATE_ACCESS_RIGHTS), documentId, user);
      Assert.assertTrue(response.getStatus() == Response.Status.NO_CONTENT.getStatusCode() && !readAccessRights.isRead() && !readAccessRights.isWrite() && readAccessRights.isExecute());
      response.close();
      client.close();
   }

   private void setUpCollections(final String collectionName) throws DbException {
      if (dataStorage.hasCollection(getInternalName(collectionName))) {
         collectionFacade.dropCollection(getInternalName(collectionName));
      }
   }

   private String setPathPrefix(final String collectionName) {
      return "DocumentServiceTest/rest/collections/" + collectionName + "/documents/";
   }

   private String getInternalName(final String collectionOriginalName) {
      return "collection." + collectionOriginalName.toLowerCase() + "_0";
   }

}
