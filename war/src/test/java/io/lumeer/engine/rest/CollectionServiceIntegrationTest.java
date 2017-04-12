/*
 * -----------------------------------------------------------------------\
 * Lummer
 *  
 * Copyright (C) 2016 the original author or authors.
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
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.DocumentFacade;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.UserFacade;
import io.lumeer.engine.rest.dao.AccessRightsDao;
import io.lumeer.engine.rest.dao.CollectionMetadata;

import com.mongodb.util.JSON;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Tests the collection service while deployed on the application server.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@RunWith(Arquillian.class)
public class CollectionServiceIntegrationTest extends IntegrationTestBase {

   private static final String TARGET_URI = "http://localhost:8080";
   private static String PATH_PREFIX = PATH_CONTEXT + "/rest/ACME/default/collections/";

   private static final String COLLECTION_GET_ALL_COLLECTIONS_1 = "CollectionServiceCollectionGetAllCollections1";
   private static final String COLLECTION_GET_ALL_COLLECTIONS_2 = "CollectionServiceCollectionGetAllCollections2";
   private static final String COLLECTION_CREATE_COLLECTION = "CollectionServiceCollectionCreateCollection";
   private static final String COLLECTION_DROP_COLLECTION = "CollectionServiceCollectionDropCollection";
   private static final String COLLECTION_RENAME_ATTRIBUTE = "CollectionServiceCollectionRenameAttribute";
   private static final String COLLECTION_DROP_ATTRIBUTE = "CollectionServiceCollectionDropAttribute";
   private static final String COLLECTION_OPTION_SEARCH = "CollectionServiceCollectionOptionSearch";
   private static final String COLLECTION_QUERY_SEARCH = "CollectionServiceCollectionQuerySearch";
   private static final String COLLECTION_ADD_COLLECTION_METADATA = "CollectionServiceCollectionAddCollectionMetadata";
   private static final String COLLECTION_READ_COLLECTION_METADATA = "CollectionServiceCollectionReadCollectionMetadata";
   private static final String COLLECTION_UPDATE_COLLECTION_METADATA = "CollectionServiceCollectionUpdateCollectionMetadata";
   private static final String COLLECTION_READ_COLLECTION_ATTRIBUTES = "CollectionServiceCollectionReadCollectionAttributes";
   private static final String COLLECTION_SET_READ_AND_DROP_ATTRIBUTE_CONSTRAINT = "CollectionServiceCollectionSetReadAndDropAttributeConstraint";
   private static final String COLLECTION_READ_ACCESS_RIGHTS = "CollectionServiceCollectionReadAccessRights";
   private static final String COLLECTION_UPDATE_ACCESS_RIGHTS = "CollectionServiceCollectionuUpdateAccessRights";

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private CollectionService collectionService;

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private SecurityFacade securityFacade;

   @Inject
   private UserFacade userFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @Before
   public void init() {
      PATH_PREFIX = PATH_CONTEXT + "/rest/" + organizationFacade.getOrganisationId() + "/" + projectFacade.getCurrentProjectId() + "/collections/";
   }

   @Test
   public void testRegister() throws Exception {
      assertThat(collectionService).isNotNull();
   }

   @Test
   public void testGetAllCollections() throws Exception {
      setUpCollections(COLLECTION_GET_ALL_COLLECTIONS_1);
      setUpCollections(COLLECTION_GET_ALL_COLLECTIONS_2);
      collectionFacade.createCollection(COLLECTION_GET_ALL_COLLECTIONS_1);
      collectionFacade.createCollection(COLLECTION_GET_ALL_COLLECTIONS_2);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON).buildGet().invoke();

      ArrayList<String> collections = response.readEntity(ArrayList.class);
      assertThat(collections).isEqualTo(new ArrayList<String>(collectionFacade.getAllCollections().values()));
      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

      response.close();
      client.close();
   }

   @Ignore("we ignore that test because we don't use cache anymore")
   @Test
   public void testGetAllCollectionsRequestCaching() throws Exception {
      setUpCollections(COLLECTION_GET_ALL_COLLECTIONS_1);
      setUpCollections(COLLECTION_GET_ALL_COLLECTIONS_2);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(response.readEntity(List.class)).doesNotContain(COLLECTION_GET_ALL_COLLECTIONS_1, COLLECTION_GET_ALL_COLLECTIONS_2);

      collectionFacade.createCollection(COLLECTION_GET_ALL_COLLECTIONS_1);
      collectionFacade.createCollection(COLLECTION_GET_ALL_COLLECTIONS_2);

      response = client.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(response.readEntity(List.class)).doesNotContain(COLLECTION_GET_ALL_COLLECTIONS_1, COLLECTION_GET_ALL_COLLECTIONS_2);
   }

   @Test
   public void testCreateCollection() throws Exception {
      setUpCollections(COLLECTION_CREATE_COLLECTION);

      // #1 first time collection creation, status code = 200
      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_CREATE_COLLECTION).request().buildPost(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      String internalName = response.readEntity(String.class);
      assertThat(internalName).isEqualTo(getInternalName(COLLECTION_CREATE_COLLECTION));
      response.close();
      client.close();

      final Client client2 = ClientBuilder.newBuilder().build();
      Response response2 = client2.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response2.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(response2.readEntity(List.class)).contains(COLLECTION_CREATE_COLLECTION);
      response2.close();
      client2.close();

      // #2 collection already exists, status code = 400
      final Client client3 = ClientBuilder.newBuilder().build();
      Response response3 = client3.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_CREATE_COLLECTION).request().buildPost(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      assertThat(response3.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
      response3.close();
      client3.close();
   }

   @Test
   public void testDropCollection() throws Exception {
      setUpCollections(COLLECTION_DROP_COLLECTION);
      final Client client = ClientBuilder.newBuilder().build();

      // #1 nothing to delete, status code = 404
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_DROP_COLLECTION).request().buildDelete().invoke();
      assertThat(dataStorage.hasCollection(getInternalName(COLLECTION_DROP_COLLECTION))).isFalse();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
      response.close();
      client.close();

      // avoid request caching
      final Client client2 = ClientBuilder.newBuilder().build();

      // #2 collection exists, ready to delete, status code = 204
      String internalCollectionName = collectionFacade.createCollection(COLLECTION_DROP_COLLECTION);
      Response response2 = client2.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_DROP_COLLECTION).request().buildDelete().invoke();
      assertThat(response2.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

      List<DataDocument> collections = dataStorage.run(new DataDocument("listCollections", 1));
      List<String> collectionNames = new ArrayList<>();
      collections.forEach(document -> collectionNames.add(document.getString("name")));

      assertThat(collectionNames).doesNotContain(internalCollectionName);
      response2.close();

      client2.close();
   }

   @Test
   public void testRenameAttribute() throws Exception {
      setUpCollections(COLLECTION_RENAME_ATTRIBUTE);

      final String oldAttributeName = "testAttribute";
      final String newAttributeName = "testNewAttribute";

      // #1 the given collection does not exist, status code = 400 or 404
      Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI)
                                .path(PATH_PREFIX + COLLECTION_RENAME_ATTRIBUTE + "/attributes/" + oldAttributeName + "/rename/" + newAttributeName)
                                .request()
                                .buildPut(Entity.entity(null, MediaType.APPLICATION_JSON))
                                .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
      response.close();
      client.close();

      // #2 the given collection and attribute exists, ready to rename the attribute, status code = 204
      String collection = collectionFacade.createCollection(COLLECTION_RENAME_ATTRIBUTE);
      collectionMetadataFacade.addOrIncrementAttribute(collection, oldAttributeName);

      final Client client2 = ClientBuilder.newBuilder().build();
      Response response2 = client2.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_RENAME_ATTRIBUTE + "/attributes/" + oldAttributeName + "/rename/" + newAttributeName).request().buildPut(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      Set<String> attributeNames = collectionMetadataFacade.getAttributesNames(collection);
      assertThat(response2.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(attributeNames).contains(newAttributeName);
      assertThat(attributeNames).doesNotContain(oldAttributeName);
      response2.close();

      client2.close();
   }

   @Test
   public void testDropAttribute() throws Exception {
      setUpCollections(COLLECTION_DROP_ATTRIBUTE);
      final String attributeName = "testAttributeToAdd";

      // #1 the given collection does not exist, status code = 404
      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_DROP_ATTRIBUTE + "/attributes/" + attributeName).request().buildDelete().invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
      response.close();
      client.close();

      // #2 the given collection and attribute exists, ready to drop the attribute, status code = 204
      String collection = collectionFacade.createCollection(COLLECTION_DROP_ATTRIBUTE);
      collectionMetadataFacade.addOrIncrementAttribute(collection, attributeName);
      final Client client2 = ClientBuilder.newBuilder().build();
      Response response2 = client2.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_DROP_ATTRIBUTE + "/attributes/" + attributeName).request().buildDelete().invoke();
      Set<String> attributeNames = collectionMetadataFacade.getAttributesNames(collection);
      boolean containsKey = attributeNames.contains(attributeName);
      assertThat(response2.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(containsKey).isFalse();
      response2.close();

      client2.close();
   }

   @Test
   public void testOptionSearch() throws Exception {
      setUpCollections(COLLECTION_OPTION_SEARCH);
      final Client client = ClientBuilder.newBuilder().build();
      final int limit = 5;

      collectionFacade.createCollection(COLLECTION_OPTION_SEARCH);
      createDummyEntries(COLLECTION_OPTION_SEARCH);

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_OPTION_SEARCH + "/search/")
                                .queryParam("filter", null)
                                .queryParam("sort", null)
                                .queryParam("skip", 0)
                                .queryParam("limit", limit)
                                .request().buildPost(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      List<DataDocument> searchedDocuments = response.readEntity(ArrayList.class);
      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(searchedDocuments).hasSize(limit);
      response.close();

      client.close();
   }

   @Test
   public void testQuerySearch() throws Exception {
      setUpCollections(COLLECTION_QUERY_SEARCH);
      final Client client = ClientBuilder.newBuilder().build();
      final DataDocument queryDoc = queryDocument(getInternalName(COLLECTION_QUERY_SEARCH)); // = "{\"find\":" + "\"" + getInternalName(COLLECTION_QUERY_SEARCH) + "\"}"
      final String queryJson = JSON.serialize(queryDoc);
      final String percentEncodedQuery = percentEncode(queryJson); // the best way to percent-encode a raw query

      collectionFacade.createCollection(COLLECTION_QUERY_SEARCH);
      createDummyEntries(COLLECTION_QUERY_SEARCH); // size = 10

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_QUERY_SEARCH + "/run/")
                                .queryParam("query", percentEncodedQuery)
                                .request().buildPost(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      List<DataDocument> searchedDocuments = response.readEntity(ArrayList.class);
      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(searchedDocuments).hasSize(10);
      response.close();

      client.close();
   }

   @Test
   public void testAddCollectionMetadata() throws Exception {
      setUpCollections(COLLECTION_ADD_COLLECTION_METADATA);
      final Client client = ClientBuilder.newBuilder().build();

      String attributeName = "metaAttribute";
      DataDocument value = new DataDocument("columnSize", 100);
      String collection = collectionFacade.createCollection(COLLECTION_ADD_COLLECTION_METADATA);
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_ADD_COLLECTION_METADATA + "/meta/" + attributeName).request(MediaType.APPLICATION_JSON).buildPost(Entity.entity(value, MediaType.APPLICATION_JSON)).invoke();
      CollectionMetadata metadata = collectionMetadataFacade.getCollectionMetadata(collection);
      DataDocument readMetaDoc = metadata.getCustomMetadata().getDataDocument(attributeName);
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(readMetaDoc).isEqualTo(value);
      response.close();

      client.close();
   }

   @Test
   public void testReadCollectionMetadata() throws Exception {
      setUpCollections(COLLECTION_READ_COLLECTION_METADATA);
      final Client client = ClientBuilder.newBuilder().build();

      // #1 no metadata collection exists, status code = 404
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_READ_COLLECTION_METADATA + "/meta/").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
      response.close();
      client.close();

      final Client client2 = ClientBuilder.newBuilder().build();
      // #2 the metadata collection of the given collection exists
      String collection = collectionFacade.createCollection(COLLECTION_READ_COLLECTION_METADATA);
      Response response2 = client2.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_READ_COLLECTION_METADATA + "/meta/").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      DataDocument collectionMetadata = response2.readEntity(DataDocument.class);
      DataDocument metadata = collectionMetadataFacade.getCollectionMetadataDocument(collection);
      assertThat(response2.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      // assertThat(collectionMetadata).isEqualTo(metadata); // we do not compare whole documents, because there is a difference in our and deserialized representation
      assertThat(collectionMetadata).hasSize(9); // based on number of variables in CollectionMetadata
      assertThat(collectionMetadata.getString(LumeerConst.Collection.REAL_NAME_KEY)).isEqualTo(metadata.getString(LumeerConst.Collection.REAL_NAME_KEY));

      response2.close();
      client2.close();
   }

   @Test
   public void testUpdateCollectionMetadata() throws Exception {
      setUpCollections(COLLECTION_UPDATE_COLLECTION_METADATA);
      final Client client = ClientBuilder.newBuilder().build();
      final String columnSizeAttributeName = "columnSize";
      final int value = 100;
      final int updatedValue = 500;

      String collection = collectionFacade.createCollection(COLLECTION_UPDATE_COLLECTION_METADATA);
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_UPDATE_COLLECTION_METADATA + "/meta/" + columnSizeAttributeName).request(MediaType.APPLICATION_JSON).buildPut(Entity.entity(value, MediaType.APPLICATION_JSON)).invoke();
      CollectionMetadata metadata = collectionMetadataFacade.getCollectionMetadata(collection);
      int readValue = metadata.getCustomMetadata().getInteger(columnSizeAttributeName);
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(readValue).isEqualTo(value);
      response.close();

      Response response2 = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_UPDATE_COLLECTION_METADATA + "/meta/" + columnSizeAttributeName).request(MediaType.APPLICATION_JSON).buildPut(Entity.entity(updatedValue, MediaType.APPLICATION_JSON)).invoke();
      CollectionMetadata updatedMetadata = collectionMetadataFacade.getCollectionMetadata(collection);
      int readUpdatedValue = updatedMetadata.getCustomMetadata().getInteger(columnSizeAttributeName);
      assertThat(response2.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(readUpdatedValue).isEqualTo(updatedValue);
      response2.close();

      client.close();
   }

   @Test
   public void testReadCollectionAttributes() throws Exception {
      setUpCollections(COLLECTION_READ_COLLECTION_ATTRIBUTES);

      final Client client = ClientBuilder.newBuilder().build();
      final String dummyAttribute1 = "dummyAttribute1";
      final String dummyAttribute2 = "dummyAttribute2";
      final String dummyAttribute3 = "dummyAttribute3";

      // #1 if the given collection does not exist, status code = 404
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_READ_COLLECTION_ATTRIBUTES + "/attributes/").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
      response.close();
      client.close();

      // #2 the given collection and attributes exists, status code = 200
      String collection = collectionFacade.createCollection(COLLECTION_READ_COLLECTION_ATTRIBUTES);
      collectionMetadataFacade.addOrIncrementAttribute(collection, dummyAttribute1);
      collectionMetadataFacade.addOrIncrementAttribute(collection, dummyAttribute2);
      collectionMetadataFacade.addOrIncrementAttribute(collection, dummyAttribute3);

      final Client client2 = ClientBuilder.newBuilder().build();
      Response response2 = client2.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_READ_COLLECTION_ATTRIBUTES + "/attributes/").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      Set<String> collectionAttributes = response2.readEntity(HashSet.class);
      assertThat(response2.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(collectionAttributes).isEqualTo(collectionFacade.readCollectionAttributes(collection).keySet());
      response2.close();

      client2.close();
   }

   @Test
   public void testReadAccessRights() throws Exception {
      setUpCollections(COLLECTION_READ_ACCESS_RIGHTS);
      final Client client = ClientBuilder.newBuilder().build();
      final String user = userFacade.getUserEmail();
      final AccessRightsDao DEFAULT_ACCESS_RIGHT = new AccessRightsDao(true, true, true, user);

      collectionFacade.createCollection(COLLECTION_READ_ACCESS_RIGHTS);
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_READ_ACCESS_RIGHTS + "/rights").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      List<AccessRightsDao> rights = response.readEntity(new GenericType<List<AccessRightsDao>>() {
      });
      AccessRightsDao readRights = rights.get(0);
      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(readRights.isWrite()).isEqualTo(DEFAULT_ACCESS_RIGHT.isWrite());
      assertThat(readRights.isRead()).isEqualTo(DEFAULT_ACCESS_RIGHT.isRead());
      assertThat(readRights.isExecute()).isEqualTo(DEFAULT_ACCESS_RIGHT.isExecute());
      assertThat(readRights.getUserName()).isEqualTo(DEFAULT_ACCESS_RIGHT.getUserName());
      response.close();
      client.close();
   }

   @Test
   public void testUpdateAccessRights() throws Exception {
      setUpCollections(COLLECTION_UPDATE_ACCESS_RIGHTS);
      final Client client = ClientBuilder.newBuilder().build();
      final String user = userFacade.getUserEmail();
      final AccessRightsDao accessRights = new AccessRightsDao(true, true, false, user);

      String collection = collectionFacade.createCollection(COLLECTION_UPDATE_ACCESS_RIGHTS);
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_UPDATE_ACCESS_RIGHTS + "/rights").request(MediaType.APPLICATION_JSON).buildPut(Entity.entity(accessRights, MediaType.APPLICATION_JSON)).invoke();
      DataDocument metadata = collectionMetadataFacade.getCollectionMetadataDocument(collection);
      AccessRightsDao readAccessRights = securityFacade.getDao(collectionMetadataFacade.metadataCollection(), metadata.getId(), user);
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(readAccessRights.isWrite()).isTrue();
      assertThat(readAccessRights.isRead()).isTrue();
      assertThat(readAccessRights.isExecute()).isFalse();
      response.close();
      client.close();
   }

   @Test
   public void testSetReadAndDropAttributeConstraint() throws Exception {
      setUpCollections(COLLECTION_SET_READ_AND_DROP_ATTRIBUTE_CONSTRAINT);

      final Client client = ClientBuilder.newBuilder().build();
      final String constraintConfiguration = "case:lower";
      final String attributeName = "dummyAttributeName";

      String collection = collectionFacade.createCollection(COLLECTION_SET_READ_AND_DROP_ATTRIBUTE_CONSTRAINT);
      collectionMetadataFacade.addOrIncrementAttribute(collection, attributeName);

      // set attribute constraint
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_SET_READ_AND_DROP_ATTRIBUTE_CONSTRAINT + "/attributes/" + attributeName + "/constraints").request(MediaType.APPLICATION_JSON).buildPut(Entity.entity(constraintConfiguration, MediaType.APPLICATION_JSON)).invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response.close();

      // read attribute constraint
      Response response2 = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_SET_READ_AND_DROP_ATTRIBUTE_CONSTRAINT + "/attributes/" + attributeName + "/constraints").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      List<String> constraints = response2.readEntity(ArrayList.class);
      assertThat(response2.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(constraints).isEqualTo(collectionMetadataFacade.getAttributeConstraintsConfigurations(collection, attributeName));
      response2.close();

      // drop attribute constraint
      Response response3 = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_SET_READ_AND_DROP_ATTRIBUTE_CONSTRAINT + "/attributes/" + attributeName + "/constraints").request(MediaType.APPLICATION_JSON).build("DELETE", Entity.json(constraintConfiguration)).invoke();
      assertThat(response3.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(collectionMetadataFacade.getAttributeConstraintsConfigurations(collection, attributeName)).isEmpty();
      response3.close();

      client.close();
   }

   private void createDummyEntries(final String collectionName) throws DbException, InvalidConstraintException {
      for (int i = 0; i < 10; i++) {
         documentFacade.createDocument(getInternalName(collectionName), new DataDocument("dummyAttribute", i));
      }
   }

   private void setUpCollections(final String collectionName) throws DbException {
      if (dataStorage.hasCollection(getInternalName(collectionName))) {
         collectionFacade.dropCollection(getInternalName(collectionName));
      }
   }

   private String getInternalName(final String collectionOriginalName) {
      return "collection." + collectionOriginalName.toLowerCase() + "_0";
   }

   private DataDocument queryDocument(final String collectionName) {
      return new DataDocument()
            .append("find", collectionName);
   }

   private String percentEncode(final String rawQuery) throws UnsupportedEncodingException {
      return URLEncoder.encode(rawQuery, "UTF-8").replaceAll("\\+", "%20");
   }
}
