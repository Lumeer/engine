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
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataFilter;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;
import io.lumeer.engine.api.dto.Attribute;
import io.lumeer.engine.api.dto.Collection;
import io.lumeer.engine.api.dto.Organization;
import io.lumeer.engine.api.dto.Project;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

/**
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 * @author <a href="mailto:alica.kacengova@gmail.com">Alica Kačengová</a>
 */
@RunWith(Arquillian.class)
public class CollectionFacadeIntegrationTest extends IntegrationTestBase {

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private UserFacade userFacade;

   @Inject
   private UserGroupFacade userGroupFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   private final String organizationCode = "LMR";
   private final String projectCode = "PR";

   @Before
   public void setUp() throws Exception {
      DataFilter filter = dataStorageDialect.documentFilter("{}");
      systemDataStorage.dropManyDocuments(LumeerConst.Organization.COLLECTION_NAME, filter);
      systemDataStorage.dropManyDocuments(LumeerConst.Project.COLLECTION_NAME, filter);

      organizationFacade.createOrganization(new Organization(organizationCode, "LMR"));
      organizationFacade.setOrganizationCode(organizationCode);

      projectFacade.createProject(new Project(projectCode, "PR"));
      projectFacade.setCurrentProjectCode(projectCode);
   }

   @Test
   public void testGetAllCollections() throws Exception {
      final String collectionName = "CollectionFacadeCollectionGetAllCollections";
      final String collection2Name = "CollectionFacadeCollectionGetAllCollections2";
      final String collection3Name = "CollectionFacadeCollectionGetAllCollections3";
      setUpCollection(collectionName);
      setUpCollection(collection2Name);
      setUpCollection(collection3Name);
      String collectionCode = collectionFacade.createCollection(new Collection(collectionName));
      String collection2Code = collectionFacade.createCollection(new Collection(collection2Name));
      String collection3Code = collectionFacade.createCollection(new Collection(collection3Name));

      String currentUser = userFacade.getUserEmail();
      List<String> userGroups = userGroupFacade.getGroupsOfUser(organizationCode, currentUser);

      assertThat(collectionFacade.getCollections(currentUser, userGroups, 0, 0)).extracting("code").contains(collectionCode, collection2Code, collection3Code);

      collectionMetadataFacade.removeUser(projectCode, collection2Code, currentUser);
      collectionMetadataFacade.removeUser(projectCode, collection3Code, currentUser);

      assertThat(collectionFacade.getCollections(currentUser, userGroups, 0, 0)).extracting("code").contains(collectionCode);

      String group = "g1";
      collectionMetadataFacade.addGroupWithRoles(projectCode, collection3Code, Collections.singletonList(LumeerConst.Security.ROLE_READ), group);
      userGroups = userGroupFacade.getGroupsOfUser(organizationCode, currentUser);

      assertThat(collectionFacade.getCollections(currentUser, userGroups, 0, 0)).extracting("code").contains(collectionCode);
      userGroupFacade.addUser(organizationCode, currentUser, group);

      userGroups = userGroupFacade.getGroupsOfUser(organizationCode, currentUser);
      assertThat(collectionFacade.getCollections(currentUser, userGroups, 0, 0)).extracting("code").contains(collectionCode, collection3Code);

   }

   @Test
   public void testGetAllCollectionsByLastTimeUsed() throws Exception {
      final String collectionName = "CollectionFacadeCollectionGetAllCollectionsLastTime1";
      setUpCollection(collectionName);
      final String collection2Name = "CollectionFacadeCollectionGetAllCollectionsLastTime2";
      setUpCollection(collection2Name);

      String collectionCode = collectionFacade.createCollection(new Collection(collectionName));
      collectionFacade.createCollection(new Collection(collection2Name));

      String currentUser = userFacade.getUserEmail();
      List<String> userGroups = userGroupFacade.getGroupsOfUser(organizationCode, currentUser);

      List<Collection> collections = collectionFacade.getCollections(currentUser, userGroups, 0, 0);
      assertThat(collections).hasSize(2);
      assertThat(collections.get(1).getCode()).isEqualTo(collectionCode);
   }

   @Test
   public void testGetCollection() throws Exception {
      final String collectionName = "CollectionFacadeGetCollection";
      setUpCollection(collectionName);

      String collectionCode = collectionFacade.createCollection(new Collection(collectionName));

      Collection collection = collectionFacade.getCollection(collectionCode);
      assertThat(collection).isNotNull();
      List<String> roleNames = new ArrayList<>(LumeerConst.Security.RESOURCE_ROLES.get(LumeerConst.Security.COLLECTION_RESOURCE));
      assertThat(collection.getUserRoles()).containsOnly(roleNames.toArray(new String[roleNames.size()]));

      String user = userFacade.getUserEmail();
      String role = roleNames.iterator().next();
      String group = "group";
      roleNames.remove(role);
      collectionMetadataFacade.setRolesToUser(projectCode, collectionCode, roleNames, user);
      collection = collectionFacade.getCollection(collectionCode);
      assertThat(collection.getUserRoles()).containsOnly(roleNames.toArray(new String[roleNames.size()]));

      collectionMetadataFacade.addGroupWithRoles(projectCode, collectionCode, Collections.singletonList(role), group);
      userGroupFacade.addUser(organizationCode, user, group);

      roleNames.add(role);
      collection = collectionFacade.getCollection(collectionCode);
      assertThat(collection.getUserRoles()).containsOnly(roleNames.toArray(new String[roleNames.size()]));

   }

   @Test
   public void testUpdateCollection() throws Exception {
      final String collectionName = "CollectionFacadeCollectionUpdate";
      setUpCollection(collectionName);

      String collectionCode = collectionFacade.createCollection(new Collection(collectionName));

      assertThat(collectionMetadataFacade.getCollectionsCodeName()).containsKey(collectionCode);
      assertThat(collectionMetadataFacade.getCollectionsCodeName()).containsValue(collectionName);

      collectionFacade.updateCollection(collectionCode, new Collection("Lumeerko"));
      assertThat(collectionMetadataFacade.getCollectionsCodeName()).doesNotContainValue(collectionName);
      assertThat(collectionMetadataFacade.getCollectionsCodeName()).containsValue("Lumeerko");
   }

   @Test
   public void testCreateAndDropCollection() throws Exception {
      final String collectionName = "CollectionFacadeCollectionCreateAndDrop";
      setUpCollection(collectionName);

      assertThat(collectionMetadataFacade.getCollectionsCodeName()).doesNotContainValue(collectionName);

      String collection = collectionFacade.createCollection(new Collection(collectionName));
      assertThat(collectionMetadataFacade.getCollectionsCodeName()).containsKey(collection);

      collectionFacade.dropCollection(collection);
      assertThat(collectionMetadataFacade.getCollectionsCodeName()).doesNotContainKey(collection);

      // when we try to remove non-existing collection, nothing happens
      collectionFacade.dropCollection(collection);
   }

   @Test
   public void testDuplicatedCollection() throws Exception {
      String collectionName = "CollectionFacadeCollectionCreateDuplicate";
      setUpCollection(collectionName);

      collectionFacade.createCollection(new Collection(collectionName));

      assertThatThrownBy(() -> collectionFacade.createCollection(new Collection(collectionName))).isInstanceOf(UserCollectionAlreadyExistsException.class);
   }

   @Test
   public void testReadCollectionAttributes() throws Exception {
      final String collectionName = "CollectionFacadeReadCollectionCollectionAttributes";
      setUpCollection(collectionName);

      String a1 = "attribute1";
      String a2 = "attribute2";

      String collection = collectionFacade.createCollection(new Collection(collectionName));
      collectionMetadataFacade.addOrIncrementAttribute(collection, a1);
      collectionMetadataFacade.addOrIncrementAttribute(collection, a2);

     List<Attribute> attributes = collectionFacade.readCollectionAttributes(collection);

      assertThat(attributes).extracting("fullName").containsOnly(a1, a2);
      Attribute attributeA1 = attributes.stream().filter(a-> a.getFullName().equals(a1)).findFirst().orElse(null);
      assertThat(attributeA1.getCount()).isEqualTo(1);
      Attribute attributeA2 = attributes.stream().filter(a-> a.getFullName().equals(a2)).findFirst().orElse(null);
      assertThat(attributeA2.getCount()).isEqualTo(1);
   }

   @Test
   public void testGetAttributeValues() throws Exception {
      final String collectionName = "CollectionFacadeCollectionGetAttributeValues";
      setUpCollection(collectionName);

      String collection = collectionFacade.createCollection(new Collection(collectionName));

      String a1 = "attribute";
      String a2 = "dummyattribute";
      String v1 = "hello";
      String v2 = "world";
      String v3 = "!";

      DataDocument doc1 = new DataDocument();
      doc1.put(a1, v1);
      DataDocument doc2 = new DataDocument();
      doc2.put(a1, v2);
      DataDocument doc3 = new DataDocument();
      doc3.put(a2, v3);

      dataStorage.createDocument(collection, doc1);
      dataStorage.createDocument(collection, doc2);
      dataStorage.createDocument(collection, doc3);

      // we have to add attributes to metadata because we test them in getAttributeValues
      collectionMetadataFacade.addOrIncrementAttribute(collection, a1);
      collectionMetadataFacade.addOrIncrementAttribute(collection, a2);

      Set<String> values = collectionFacade.getAttributeValues(collection, a1);

      assertThat(values).contains(v1, v2);
      assertThat(values).doesNotContain(v3);
   }

   @Test
   public void testDropAttribute() throws Exception {
      final String collectionName = "CollectionFacadeCollectionDropCollectionAttribute";
      setUpCollection(collectionName);

      String collection = collectionFacade.createCollection(new Collection(collectionName));

      String attribute1 = "attribute-to-drop";
      String attribute2 = "attribute";
      String value = "value";

      DataDocument doc1 = new DataDocument();
      doc1.put(attribute1, value);
      doc1.put(attribute2, value);
      DataDocument doc2 = new DataDocument();
      doc2.put(attribute1, value);
      doc2.put(attribute2, value);
      DataDocument doc3 = new DataDocument();
      doc3.put(attribute1, value);
      doc3.put(attribute2, value);

      dataStorage.createDocument(collection, doc1);
      dataStorage.createDocument(collection, doc2);
      dataStorage.createDocument(collection, doc3);

      // we have to add attributes to metadata because we test them in getAttributeValues
      for (int i = 0; i < 3; i++) {
         collectionMetadataFacade.addOrIncrementAttribute(collection, attribute1);
         collectionMetadataFacade.addOrIncrementAttribute(collection, attribute2);
      }

      collectionFacade.dropAttribute(collection, attribute1);

      List<DataDocument> documents = dataStorage.search(collection, null, null, 0, 0);
      for (int i = 0; i < 3; i++) {
         assertThat(documents.get(i)).doesNotContainKey(attribute1);
      }
   }

   @Test
   public void testRenameAttribute() throws Exception {
      final String collectionName = "CollectionFacadeCollectionRenameAttribute";
      setUpCollection(collectionName);

      String collection = collectionFacade.createCollection(new Collection(collectionName));

      String name = "attribute 1";
      String newName = "new attribute 1";
      String value = "value";

      DataDocument doc1 = new DataDocument();
      doc1.put(name, value);
      DataDocument doc2 = new DataDocument();
      doc2.put(name, value);
      DataDocument doc3 = new DataDocument();
      doc3.put(name, value);

      dataStorage.createDocument(collection, doc1);
      dataStorage.createDocument(collection, doc2);
      dataStorage.createDocument(collection, doc3);

      // we have to increment 3 times, because we added 3 documents
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);
      collectionMetadataFacade.addOrIncrementAttribute(collection, name);

      collectionFacade.renameAttribute(collection, name, newName);

      assertThat(isEveryDocumentFilledByNewAttribute(collection, newName)).isTrue();
   }

   @Test
   public void testAddDropConstraint() throws Exception {
      final String collectionName = "CollectionFacadeCollectionAddDropConstraint";
      setUpCollection(collectionName);

      String collection = collectionFacade.createCollection(new Collection(collectionName));

      String attribute = "attribute";
      int value1 = 5;
      int value2 = 10;
      String constraint1 = "lessThan:7";

      DataDocument doc1 = new DataDocument();
      doc1.put(attribute, value1);
      dataStorage.createDocument(collection, doc1);
      collectionMetadataFacade.addOrIncrementAttribute(collection, attribute);

      assertThat(collectionFacade.addAttributeConstraint(collection, attribute, constraint1)).isTrue();
      collectionFacade.dropAttributeConstraint(collection, attribute, constraint1);

      DataDocument doc2 = new DataDocument();
      doc2.put(attribute, value2);
      dataStorage.createDocument(collection, doc2);
      collectionMetadataFacade.addOrIncrementAttribute(collection, attribute);

      // result is false, because there is already a value (value2) not satisfying the constraint
      assertThat(collectionFacade.addAttributeConstraint(collection, attribute, constraint1)).isFalse();
   }

   private boolean isEveryDocumentFilledByNewAttribute(String collection, String attributeName) {
      List<DataDocument> documents = dataStorage.search(collection, null, null, 0, 0);

      for (DataDocument document : documents) {
         if (!document.containsKey(attributeName)) {
            return false;
         }
      }
      return true;
   }

   private void setUpCollection(String collectionName) {
      String collectionCode = collectionMetadataFacade.getCollectionCodeFromName(collectionName);
      if (collectionCode != null) {
         dataStorage.dropCollection(collectionCode);
      }
   }
}