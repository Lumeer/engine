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
import io.lumeer.engine.api.dto.Attribute;
import io.lumeer.engine.api.dto.Collection;
import io.lumeer.engine.api.dto.CollectionMetadata;
import io.lumeer.engine.api.dto.Organization;
import io.lumeer.engine.api.dto.Permission;
import io.lumeer.engine.api.dto.Project;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.DatabaseInitializer;
import io.lumeer.engine.controller.DocumentFacade;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.UserFacade;

import com.mongodb.util.JSON;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Tests the collection service while deployed on the application server.
 */
@RunWith(Arquillian.class)
public class CollectionServiceIntegrationTest extends IntegrationTestBase {

   private static final String TARGET_URI = "http://localhost:8080";
   private static String PATH_PREFIX = PATH_CONTEXT + "/rest/organizations/ACME/projects/default/collections/";

   private static final String COLLECTION_GET_COLLECTION_1 = "CollectionServiceCollectionGetCollection1";
   private static final String COLLECTION_GET_COLLECTION_2 = "CollectionServiceCollectionGetCollection2";
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

   @Inject
   private DatabaseInitializer databaseInitializer;

   private String organizationCode = "LMR";
   private String projectCode = "PR";

   @Before
   public void init() {
      organizationFacade.setOrganizationCode(organizationCode);
      projectFacade.dropProject(projectCode);
      organizationFacade.dropOrganization(organizationCode);
      organizationFacade.createOrganization(new Organization(organizationCode, "Lumeer"));
      organizationFacade.setOrganizationCode(organizationCode);
      projectFacade.createProject(new Project(projectCode, "Proj1"));
      projectFacade.setCurrentProjectCode(projectCode);
      PATH_PREFIX = PATH_CONTEXT + "/rest/organizations/" + organizationCode + "/projects/" + projectCode + "/collections/";
   }

   @Before
   public void clearAllCollections() throws DbException {
      Set<String> collections = new HashSet<>(collectionMetadataFacade.getCollectionsCodeName().keySet());
      for (String code : collections) {
         collectionFacade.dropCollection(code);
      }
   }

   @Test
   public void testRegister() throws Exception {
      assertThat(collectionService).isNotNull();
   }

   @Test
   public void testGetCollection() throws Exception {
      String collectionCode1 = collectionFacade.createCollection(new Collection(COLLECTION_GET_COLLECTION_1));
      String collectionCode2 = collectionFacade.createCollection(new Collection(COLLECTION_GET_COLLECTION_2));

      Client client = ClientBuilder.newBuilder().build();
      Response response1 = client.target(TARGET_URI).path(PATH_PREFIX + collectionCode1).request(MediaType.APPLICATION_JSON).buildGet().invoke();
      Collection collection1 = response1.readEntity(new GenericType<Collection>(Collection.class));
      assertThat(collection1.getName()).isEqualTo(COLLECTION_GET_COLLECTION_1);
      assertThat(response1.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      response1.close();
      client.close();

      client = ClientBuilder.newBuilder().build();
      Response response2 = client.target(TARGET_URI).path(PATH_PREFIX + collectionCode2).request(MediaType.APPLICATION_JSON).buildGet().invoke();
      Collection collection2 = response2.readEntity(new GenericType<Collection>(Collection.class));
      assertThat(collection2.getName()).isEqualTo(COLLECTION_GET_COLLECTION_2);
      assertThat(response2.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      response2.close();
      client.close();
   }

   @Test
   public void testGetAllCollections() throws Exception {
      collectionFacade.createCollection(new Collection(COLLECTION_GET_ALL_COLLECTIONS_1));
      collectionFacade.createCollection(new Collection(COLLECTION_GET_ALL_COLLECTIONS_2));

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX).request(MediaType.APPLICATION_JSON).buildGet().invoke();
      List<Collection> collections = response.readEntity(new GenericType<List<Collection>>(List.class) {
      });
      assertThat(collections).extracting("name").contains(COLLECTION_GET_ALL_COLLECTIONS_1, COLLECTION_GET_ALL_COLLECTIONS_2);
      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

      response.close();
      client.close();
   }

   @Test
   public void testGetAllCollectionsPageAndSize() throws Exception {
      collectionFacade.createCollection(new Collection("colps8"));
      collectionFacade.createCollection(new Collection("colps7"));
      collectionFacade.createCollection(new Collection("colps6"));
      collectionFacade.createCollection(new Collection("colps5"));
      collectionFacade.createCollection(new Collection("colps4"));
      collectionFacade.createCollection(new Collection("colps3"));
      collectionFacade.createCollection(new Collection("colps2"));
      collectionFacade.createCollection(new Collection("colps1"));

      //without page and size
      Response response = ClientBuilder.newBuilder().build()
                                       .target(TARGET_URI).path(PATH_PREFIX)
                                       .request(MediaType.APPLICATION_JSON)
                                       .buildGet().invoke();
      List<Collection> collections = response.readEntity(new GenericType<List<Collection>>(List.class) {
      });
      assertThat(collections).hasSize(8);

      // size only
      response = ClientBuilder.newBuilder().build()
                              .target(TARGET_URI).path(PATH_PREFIX)
                              .queryParam("size", 5)
                              .request(MediaType.APPLICATION_JSON)
                              .buildGet().invoke();

      collections = response.readEntity(new GenericType<List<Collection>>(List.class) {
      });
      assertThat(collections).hasSize(5);

      //pagging
      response = ClientBuilder.newBuilder().build()
                              .target(TARGET_URI).path(PATH_PREFIX)
                              .queryParam("size", 3)
                              .queryParam("page", 1)
                              .request(MediaType.APPLICATION_JSON)
                              .buildGet().invoke();

      collections = response.readEntity(new GenericType<List<Collection>>(List.class) {
      });
      assertThat(collections).hasSize(3).extracting("name").containsOnly("colps4", "colps5", "colps6");

      response = ClientBuilder.newBuilder().build()
                              .target(TARGET_URI).path(PATH_PREFIX)
                              .queryParam("size", 3)
                              .queryParam("page", 2)
                              .request(MediaType.APPLICATION_JSON)
                              .buildGet().invoke();

      collections = response.readEntity(new GenericType<List<Collection>>(List.class) {
      });
      assertThat(collections).hasSize(2).extracting("name").containsOnly("colps7", "colps8");

   }

   @Test
   public void testCreateCollection() throws Exception {
      securityFacade.addProjectUsersRole(projectFacade.getCurrentProjectCode(), Collections.singletonList(userFacade.getUserEmail()), LumeerConst.Security.ROLE_WRITE);

      // #1 first time collection creation, status code = 200
      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX).request().buildPost(Entity.json(new Collection(COLLECTION_CREATE_COLLECTION))).invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      String code = response.readEntity(String.class);

      String codeFromFacade = collectionMetadataFacade.getCollectionCodeFromName(COLLECTION_CREATE_COLLECTION);
      assertThat(code).isEqualTo(codeFromFacade);
      response.close();
      client.close();

      assertThat(collectionMetadataFacade.getCollectionsCodeName().values()).contains(COLLECTION_CREATE_COLLECTION);

      // #2 collection already exists, status code = 400
      final Client client2 = ClientBuilder.newBuilder().build();
      Response response2 = client2.target(TARGET_URI).path(PATH_PREFIX).request().buildPost(Entity.json(new Collection(COLLECTION_CREATE_COLLECTION))).invoke();
      assertThat(response2.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
      response2.close();
      client2.close();
   }

   @Test
   public void testDropCollection() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      // #1 nothing to delete, status code = 404
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_DROP_COLLECTION).request().buildDelete().invoke();
      assertThat(dataStorage.hasCollection(COLLECTION_DROP_COLLECTION)).isFalse();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
      response.close();
      client.close();

      // avoid request caching
      final Client client2 = ClientBuilder.newBuilder().build();

      // #2 collection exists, ready to delete, status code = 204
      String code = collectionFacade.createCollection(new Collection(COLLECTION_DROP_COLLECTION));
      Response response2 = client2.target(TARGET_URI).path(PATH_PREFIX + code).request().buildDelete().invoke();
      assertThat(response2.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

      List<DataDocument> collections = dataStorage.run(new DataDocument("listCollections", 1));
      List<String> collectionNames = new ArrayList<>();
      collections.forEach(document -> collectionNames.add(document.getString("name")));

      assertThat(collectionNames).doesNotContain(COLLECTION_DROP_COLLECTION);
      response2.close();

      client2.close();
   }

   @Test
   public void testRenameAttribute() throws Exception {
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
      String code = collectionFacade.createCollection(new Collection(COLLECTION_RENAME_ATTRIBUTE));
      collectionMetadataFacade.addOrIncrementAttribute(code, oldAttributeName);

      final Client client2 = ClientBuilder.newBuilder().build();
      Response response2 = client2.target(TARGET_URI).path(PATH_PREFIX + code + "/attributes/" + oldAttributeName + "/rename/" + newAttributeName).request().buildPut(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      Set<String> attributeNames = collectionMetadataFacade.getAttributesNames(code);
      assertThat(response2.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(attributeNames).contains(newAttributeName);
      assertThat(attributeNames).doesNotContain(oldAttributeName);
      response2.close();

      client2.close();
   }

   @Test
   public void testDropAttribute() throws Exception {
      final String attributeName = "testAttributeToAdd";

      // #1 the given collection does not exist, status code = 404
      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_DROP_ATTRIBUTE + "/attributes/" + attributeName).request().buildDelete().invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
      response.close();
      client.close();

      // #2 the given collection and attribute exists, ready to drop the attribute, status code = 204
      String code = collectionFacade.createCollection(new Collection(COLLECTION_DROP_ATTRIBUTE));
      collectionMetadataFacade.addOrIncrementAttribute(code, attributeName);
      final Client client2 = ClientBuilder.newBuilder().build();
      Response response2 = client2.target(TARGET_URI).path(PATH_PREFIX + code + "/attributes/" + attributeName).request().buildDelete().invoke();
      Set<String> attributeNames = collectionMetadataFacade.getAttributesNames(code);
      boolean containsKey = attributeNames.contains(attributeName);
      assertThat(response2.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(containsKey).isFalse();
      response2.close();

      client2.close();
   }

   @Test
   public void testOptionSearch() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();
      final int limit = 5;

      String code = collectionFacade.createCollection(new Collection(COLLECTION_OPTION_SEARCH));
      createDummyEntries(code);

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + code + "/search/")
                                .queryParam("filter", null)
                                .queryParam("sort", null)
                                .queryParam("skip", 0)
                                .queryParam("limit", limit)
                                .request().buildPost(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      List<DataDocument> searchedDocuments = response.readEntity(new GenericType<List<DataDocument>>() {
      });
      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(searchedDocuments).hasSize(limit);
      response.close();

      client.close();
   }

   @Test
   public void testQuerySearch() throws Exception {
      String code = collectionFacade.createCollection(new Collection(COLLECTION_QUERY_SEARCH));
      createDummyEntries(code); // size = 10

      final Client client = ClientBuilder.newBuilder().build();
      final DataDocument queryDoc = queryDocument(code); // = "{\"find\":" + "\"" + getInternalName(COLLECTION_QUERY_SEARCH) + "\"}"
      final String queryJson = JSON.serialize(queryDoc);
      final String percentEncodedQuery = percentEncode(queryJson); // the best way to percent-encode a raw query

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + "run")
                                .queryParam("query", percentEncodedQuery)
                                .request().buildPost(Entity.entity(null, MediaType.APPLICATION_JSON)).invoke();
      List<DataDocument> searchedDocuments = response.readEntity(new GenericType<List<DataDocument>>() {
      });
      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(searchedDocuments).hasSize(10);
      response.close();

      client.close();
   }

   @Test
   public void testAddCollectionMetadata() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      String attributeName = "metaAttribute";
      DataDocument value = new DataDocument("columnSize", 100);
      String code = collectionFacade.createCollection(new Collection(COLLECTION_ADD_COLLECTION_METADATA));

      Response response = client.target(TARGET_URI).path(PATH_PREFIX + code + "/meta/" + attributeName).request(MediaType.APPLICATION_JSON).buildPost(Entity.entity(value, MediaType.APPLICATION_JSON)).invoke();
      CollectionMetadata metadata = collectionMetadataFacade.getCollectionMetadata(code);
      DataDocument readMetaDoc = metadata.getCustomMetadata().getDataDocument(attributeName);
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(readMetaDoc).isEqualTo(value);
      response.close();

      client.close();
   }

   @Test
   public void testReadCollectionMetadata() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();

      // #1 no metadata collection exists, status code = 404
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + COLLECTION_READ_COLLECTION_METADATA + "/meta/").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
      response.close();
      client.close();

      final Client client2 = ClientBuilder.newBuilder().build();
      // #2 the metadata collection of the given collection exists
      String code = collectionFacade.createCollection(new Collection(COLLECTION_READ_COLLECTION_METADATA));
      Response response2 = client2.target(TARGET_URI).path(PATH_PREFIX + code + "/meta/").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      DataDocument collectionMetadata = response2.readEntity(DataDocument.class);
      DataDocument metadata = collectionMetadataFacade.getCollectionMetadataDocument(code);
      assertThat(response2.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      // assertThat(collectionMetadata).isEqualTo(metadata); // we do not compare whole documents, because there is a difference in our and deserialized representation
      assertThat(collectionMetadata.getString(LumeerConst.Collection.REAL_NAME)).isEqualTo(metadata.getString(LumeerConst.Collection.REAL_NAME));

      response2.close();
      client2.close();
   }

   @Test
   public void testUpdateCollectionMetadata() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();
      final String columnSizeAttributeName = "columnSize";
      final int value = 100;
      final int updatedValue = 500;

      String code = collectionFacade.createCollection(new Collection(COLLECTION_UPDATE_COLLECTION_METADATA));
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + code + "/meta/" + columnSizeAttributeName).request(MediaType.APPLICATION_JSON).buildPut(Entity.entity(value, MediaType.APPLICATION_JSON)).invoke();
      CollectionMetadata metadata = collectionMetadataFacade.getCollectionMetadata(code);
      int readValue = metadata.getCustomMetadata().getInteger(columnSizeAttributeName);
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(readValue).isEqualTo(value);
      response.close();

      Response response2 = client.target(TARGET_URI).path(PATH_PREFIX + code + "/meta/" + columnSizeAttributeName).request(MediaType.APPLICATION_JSON).buildPut(Entity.entity(updatedValue, MediaType.APPLICATION_JSON)).invoke();
      CollectionMetadata updatedMetadata = collectionMetadataFacade.getCollectionMetadata(code);
      int readUpdatedValue = updatedMetadata.getCustomMetadata().getInteger(columnSizeAttributeName);
      assertThat(response2.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(readUpdatedValue).isEqualTo(updatedValue);
      response2.close();

      client.close();
   }

   @Test
   public void testReadCollectionAttributes() throws Exception {
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
      String code = collectionFacade.createCollection(new Collection(COLLECTION_READ_COLLECTION_ATTRIBUTES));
      collectionMetadataFacade.addOrIncrementAttribute(code, dummyAttribute1);
      collectionMetadataFacade.addOrIncrementAttribute(code, dummyAttribute2);
      collectionMetadataFacade.addOrIncrementAttribute(code, dummyAttribute3);

      final Client client2 = ClientBuilder.newBuilder().build();
      Response response2 = client2.target(TARGET_URI).path(PATH_PREFIX + code + "/attributes/").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      List<Attribute> collectionAttributes = response2.readEntity(new GenericType<List<Attribute>>() {
      });
      assertThat(response2.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

      List<String> attributesNamesRest = collectionAttributes.stream()
                                                             .map(Attribute::getFullName).collect(Collectors.toList());
      List<String> attributesNamesFacade = collectionFacade.readCollectionAttributes(code).stream().map(Attribute::getFullName).collect(Collectors.toList());
      assertThat(attributesNamesRest).containsOnly(attributesNamesFacade.toArray(new String[attributesNamesFacade.size()]));
      response2.close();

      client2.close();
   }

   @Test
   public void testSetReadAndDropAttributeConstraint() throws Exception {
      final Client client = ClientBuilder.newBuilder().build();
      final String constraintConfiguration = "case:lower";
      final String attributeName = "dummyAttributeName";

      String code = collectionFacade.createCollection(new Collection(COLLECTION_SET_READ_AND_DROP_ATTRIBUTE_CONSTRAINT));
      collectionMetadataFacade.addOrIncrementAttribute(code, attributeName);

      // set attribute constraint
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + code + "/attributes/" + attributeName + "/constraints").request(MediaType.APPLICATION_JSON).buildPut(Entity.entity(constraintConfiguration, MediaType.APPLICATION_JSON)).invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response.close();

      // read attribute constraint
      Response response2 = client.target(TARGET_URI).path(PATH_PREFIX + code + "/attributes/" + attributeName + "/constraints").request(MediaType.APPLICATION_JSON).buildGet().invoke();
      List<String> constraints = response2.readEntity(new GenericType<List<String>>() {
      });
      assertThat(response2.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(constraints).isEqualTo(collectionMetadataFacade.getAttributeConstraintsConfigurations(code, attributeName));
      response2.close();

      // drop attribute constraint
      Response response3 = client.target(TARGET_URI).path(PATH_PREFIX + code + "/attributes/" + attributeName + "/constraints").request(MediaType.APPLICATION_JSON).build("DELETE", Entity.json(constraintConfiguration)).invoke();
      assertThat(response3.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      assertThat(collectionMetadataFacade.getAttributeConstraintsConfigurations(code, attributeName)).isEmpty();
      response3.close();

      client.close();
   }

   @Test
   public void testGetPermissions() throws Exception {
      final String collectionName = "CollectionServiceIntegrationTestGetPermissions";
      setUpCollection(collectionName);
      String collectionCode = collectionFacade.createCollection(new Collection(collectionName));
      String user = "user";
      String group = "group";
      List<String> rolesUser = Arrays.asList("r1", "r2", "r3");
      List<String> rolesGroup = Arrays.asList("r10", "r20", "r30");
      collectionMetadataFacade.addUserWithRoles(projectCode, collectionCode, rolesUser, user);
      collectionMetadataFacade.addGroupWithRoles(projectCode, collectionCode, rolesGroup, group);

      Response response = ClientBuilder.newBuilder().build().target(TARGET_URI)
                                       .path(PATH_PREFIX + collectionCode + "/permissions")
                                       .request(MediaType.APPLICATION_JSON).buildGet().invoke();

      Map<String, List<Permission>> permissions = response.readEntity(new GenericType<Map<String, List<Permission>>>() {
      });
      assertThat(permissions).hasSize(2).containsOnlyKeys(LumeerConst.Security.USERS_KEY, LumeerConst.Security.GROUP_KEY);
      assertThat(permissions.get(LumeerConst.Security.USERS_KEY)).hasSize(2).extracting("name").containsOnly(userFacade.getUserEmail(), user);
      assertThat(permissions.get(LumeerConst.Security.GROUP_KEY)).hasSize(1).extracting("name").containsOnly(group);
   }

   @Test
   public void testRemoveUser() throws Exception {
      final String collectionName = "CollectionServiceIntegrationTestRemoveUser";
      setUpCollection(collectionName);
      String collectionCode = collectionFacade.createCollection(new Collection(collectionName));
      String user = "user";
      String user2 = "user2";
      List<String> roles = Arrays.asList("r1", "r2", "r3");
      collectionMetadataFacade.addUserWithRoles(projectCode, collectionCode, roles, user);
      collectionMetadataFacade.addUserWithRoles(projectCode, collectionCode, roles, user2);

      assertThat(collectionMetadataFacade.getPermissions(projectCode, collectionCode).get(LumeerConst.Security.USERS_KEY))
            .hasSize(3).extracting("name").containsOnly(userFacade.getUserEmail(), user, user2);

      Response response = ClientBuilder.newBuilder().build().target(TARGET_URI)
                                       .path(PATH_PREFIX + collectionCode + "/permissions/users")
                                       .queryParam("user", user)
                                       .request().buildDelete().invoke();

      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response.close();

      assertThat(collectionMetadataFacade.getPermissions(projectCode, collectionCode).get(LumeerConst.Security.USERS_KEY))
            .hasSize(2).extracting("name").containsOnly(userFacade.getUserEmail(), user2);
   }

   @Test
   public void testRemoveGroup() throws Exception {
      final String collectionName = "CollectionServiceIntegrationTestRemoveGroup";
      setUpCollection(collectionName);
      String collectionCode = collectionFacade.createCollection(new Collection(collectionName));
      String group = "group";
      String group2 = "group2";
      List<String> roles = Arrays.asList("r1", "r2", "r3");
      collectionMetadataFacade.addGroupWithRoles(projectCode, collectionCode, roles, group);
      collectionMetadataFacade.addGroupWithRoles(projectCode, collectionCode, roles, group2);

      assertThat(collectionMetadataFacade.getPermissions(projectCode, collectionCode).get(LumeerConst.Security.GROUP_KEY))
            .hasSize(2).extracting("name").containsOnly(group, group2);

      Response response = ClientBuilder.newBuilder().build().target(TARGET_URI)
                                       .path(PATH_PREFIX + collectionCode + "/permissions/groups")
                                       .queryParam("group", group)
                                       .request().buildDelete().invoke();

      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response.close();

      assertThat(collectionMetadataFacade.getPermissions(projectCode, collectionCode).get(LumeerConst.Security.GROUP_KEY))
            .hasSize(1).extracting("name").containsOnly(group2);
   }

   @Test
   public void testSetRolesToUser() throws Exception {
      final String collectionName = "CollectionServiceIntegrationTestSetRolesToUser";
      setUpCollection(collectionName);
      String collectionCode = collectionFacade.createCollection(new Collection(collectionName));
      String user = "user";
      List<String> roles = Arrays.asList("r1", "r2", "r3");
      String[] rolesChange = new String[] { "r1", "r5", "r6", "r7" };
      collectionMetadataFacade.addUserWithRoles(projectCode, collectionCode, roles, user);

      Permission permission = collectionMetadataFacade.getPermissions(projectCode, collectionCode).get(LumeerConst.Security.USERS_KEY)
                                                      .stream().filter(p -> p.getName().equals(user)).findFirst().orElse(null);
      assertThat(permission).isNotNull();
      assertThat(permission.getRoles()).hasSize(3).containsOnly(roles.toArray(new String[roles.size()]));

      Response response = ClientBuilder.newBuilder().build().target(TARGET_URI)
                                       .path(PATH_PREFIX + collectionCode + "/permissions/users")
                                       .queryParam("user", user)
                                       .queryParam("roles", (Object[]) rolesChange)
                                       .request()
                                       .buildPut(Entity.entity(null, MediaType.APPLICATION_JSON))
                                       .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response.close();

      permission = collectionMetadataFacade.getPermissions(projectCode, collectionCode).get(LumeerConst.Security.USERS_KEY)
                                           .stream().filter(p -> p.getName().equals(user)).findFirst().orElse(null);
      assertThat(permission).isNotNull();
      assertThat(permission.getRoles()).hasSize(4).containsOnly(rolesChange);

      Response response2 = ClientBuilder.newBuilder().build().target(TARGET_URI)
                                        .path(PATH_PREFIX + collectionCode + "/permissions/users")
                                        .queryParam("user", user)
                                        .queryParam("roles")
                                        .request()
                                        .buildPut(Entity.entity(null, MediaType.APPLICATION_JSON))
                                        .invoke();
      assertThat(response2.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response2.close();

      permission = collectionMetadataFacade.getPermissions(projectCode, collectionCode).get(LumeerConst.Security.USERS_KEY)
                                           .stream().filter(p -> p.getName().equals(user)).findFirst().orElse(null);
      assertThat(permission).isNotNull();
      assertThat(permission.getRoles()).isEmpty();
   }

   @Test
   public void testSetRolesToGroup() throws Exception {
      final String collectionName = "CollectionServiceIntegrationTestSetRolesToGroup";
      setUpCollection(collectionName);
      String collectionCode = collectionFacade.createCollection(new Collection(collectionName));
      String group = "group";
      List<String> roles = Arrays.asList("r1", "r2", "r3");
      String[] rolesChange = new String[] { "r1", "r4" };
      collectionMetadataFacade.addGroupWithRoles(projectCode, collectionCode, roles, group);

      Permission permission = collectionMetadataFacade.getPermissions(projectCode, collectionCode).get(LumeerConst.Security.GROUP_KEY)
                                                      .stream().filter(p -> p.getName().equals(group)).findFirst().orElse(null);
      assertThat(permission).isNotNull();
      assertThat(permission.getRoles()).hasSize(3).containsOnly(roles.toArray(new String[roles.size()]));

      Response response = ClientBuilder.newBuilder().build().target(TARGET_URI)
                                       .path(PATH_PREFIX + collectionCode + "/permissions/groups")
                                       .queryParam("group", group)
                                       .queryParam("roles", (Object[]) rolesChange)
                                       .request()
                                       .buildPut(Entity.entity(null, MediaType.APPLICATION_JSON))
                                       .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response.close();

      permission = collectionMetadataFacade.getPermissions(projectCode, collectionCode).get(LumeerConst.Security.GROUP_KEY)
                                           .stream().filter(p -> p.getName().equals(group)).findFirst().orElse(null);
      assertThat(permission).isNotNull();
      assertThat(permission.getRoles()).hasSize(2).containsOnly(rolesChange);

      Response response2 = ClientBuilder.newBuilder().build().target(TARGET_URI)
                                        .path(PATH_PREFIX + collectionCode + "/permissions/groups")
                                        .queryParam("group", group)
                                        .queryParam("roles")
                                        .request()
                                        .buildPut(Entity.entity(null, MediaType.APPLICATION_JSON))
                                        .invoke();
      assertThat(response2.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response2.close();

      permission = collectionMetadataFacade.getPermissions(projectCode, collectionCode).get(LumeerConst.Security.GROUP_KEY)
                                           .stream().filter(p -> p.getName().equals(group)).findFirst().orElse(null);
      assertThat(permission).isNotNull();
      assertThat(permission.getRoles()).isEmpty();
   }

   @Test
   public void testAddUserWithRoles() throws Exception {
      final String collectionName = "CollectionServiceIntegrationTestAddUserWithRoles";
      setUpCollection(collectionName);
      String collectionCode = collectionFacade.createCollection(new Collection(collectionName));

      assertThat(collectionMetadataFacade.getPermissions(projectCode, collectionCode).get(LumeerConst.Security.USERS_KEY)).hasSize(1);

      String user1 = "user1";
      String user2 = "user2";
      String[] roles1 = new String[] { "r1", "r2", "r3" };
      String[] roles2 = new String[] { "r4", "r5", "r3" };

      Response response = ClientBuilder.newBuilder().build().target(TARGET_URI)
                                       .path(PATH_PREFIX + collectionCode + "/permissions/users")
                                       .queryParam("user", user1)
                                       .queryParam("roles", (Object[]) roles1)
                                       .request()
                                       .buildPost(Entity.entity(null, MediaType.APPLICATION_JSON))
                                       .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response.close();

      Response response2 = ClientBuilder.newBuilder().build().target(TARGET_URI)
                                        .path(PATH_PREFIX + collectionCode + "/permissions/users")
                                        .queryParam("user", user2)
                                        .queryParam("roles", (Object[]) roles2)
                                        .request()
                                        .buildPost(Entity.entity(null, MediaType.APPLICATION_JSON))
                                        .invoke();
      assertThat(response2.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response2.close();

      List<Permission> userList = collectionMetadataFacade.getPermissions(projectCode, collectionCode).get(LumeerConst.Security.USERS_KEY);
      assertThat(userList).hasSize(3);
      assertThat(userList.stream().filter(p -> p.getName().equals(user1)).findFirst().orElse(null).getRoles()).containsOnly(roles1);
      assertThat(userList.stream().filter(p -> p.getName().equals(user2)).findFirst().orElse(null).getRoles()).containsOnly(roles2);
   }

   @Test
   public void testAddGroupWithRoles() throws Exception {
      final String collectionName = "CollectionServiceIntegrationTestAddGroupWithRoles";
      setUpCollection(collectionName);
      String collectionCode = collectionFacade.createCollection(new Collection(collectionName));

      assertThat(collectionMetadataFacade.getPermissions(projectCode, collectionCode).get(LumeerConst.Security.USERS_KEY)).hasSize(1);

      String group1 = "group1";
      String group2 = "group2";
      String[] roles1 = new String[] { "r1", "r2", "r3" };
      String[] roles2 = new String[] { "r4", "r5", "r3" };

      Response response = ClientBuilder.newBuilder().build().target(TARGET_URI)
                                       .path(PATH_PREFIX + collectionCode + "/permissions/groups")
                                       .queryParam("group", group1)
                                       .queryParam("roles", (Object[]) roles1)
                                       .request()
                                       .buildPost(Entity.entity(null, MediaType.APPLICATION_JSON))
                                       .invoke();
      assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response.close();

      Response response2 = ClientBuilder.newBuilder().build().target(TARGET_URI)
                                        .path(PATH_PREFIX + collectionCode + "/permissions/groups")
                                        .queryParam("group", group2)
                                        .queryParam("roles", (Object[]) roles2)
                                        .request()
                                        .buildPost(Entity.entity(null, MediaType.APPLICATION_JSON))
                                        .invoke();
      assertThat(response2.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
      response2.close();

      List<Permission> groupList = collectionMetadataFacade.getPermissions(projectCode, collectionCode).get(LumeerConst.Security.GROUP_KEY);
      assertThat(groupList).hasSize(2);
      assertThat(groupList.stream().filter(p -> p.getName().equals(group1)).findFirst().orElse(null).getRoles()).containsOnly(roles1);
      assertThat(groupList.stream().filter(p -> p.getName().equals(group2)).findFirst().orElse(null).getRoles()).containsOnly(roles2);
   }

   private void createDummyEntries(final String collectionCode) throws DbException, InvalidConstraintException {
      for (int i = 0; i < 10; i++) {
         documentFacade.createDocument(collectionCode, new DataDocument("dummyAttribute", i));
      }
   }

   private void setUpCollection(String collectionName) {
      String collectionCode = collectionMetadataFacade.getCollectionCodeFromName(collectionName);
      if (collectionCode != null) {
         dataStorage.dropCollection(collectionCode);
      }
   }

   private DataDocument queryDocument(final String collectionName) {
      return new DataDocument()
            .append("find", collectionName);
   }

   private String percentEncode(final String rawQuery) throws UnsupportedEncodingException {
      return URLEncoder.encode(rawQuery, "UTF-8").replaceAll("\\+", "%20");
   }

}
