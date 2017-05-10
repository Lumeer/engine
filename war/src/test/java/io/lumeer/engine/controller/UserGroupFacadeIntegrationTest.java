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

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.SystemDataStorage;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageDialect;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Tests for UserGroupFacade
 */
@RunWith(Arquillian.class)
public class UserGroupFacadeIntegrationTest extends IntegrationTestBase {

   @Inject
   @SystemDataStorage
   private DataStorage systemDataStorage;

   @Inject
   private DataStorageDialect dataStorageDialect;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private UserGroupFacade userGroupFacade;

   @Before
   public void setUp() throws Exception {
      systemDataStorage.dropManyDocuments(LumeerConst.Organization.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
      systemDataStorage.dropManyDocuments(LumeerConst.UserGroup.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
      systemDataStorage.dropManyDocuments(LumeerConst.Group.COLLECTION_NAME, dataStorageDialect.documentFilter("{}"));
   }

   @Test
   public void testAddUser() throws Exception {
      final String organization1 = "organization1";
      final String organization2 = "organization2";
      final String user1 = "user1";
      final String user2 = "user2";
      final String user3 = "user3";

      String org1Id = organizationFacade.createOrganization(organization1, "Org one");
      String org2Id = organizationFacade.createOrganization(organization2, "Org two");

      userGroupFacade.addUser(org1Id, user1);
      userGroupFacade.addUser(org1Id, user2, "g1", "g2", "g3");
      userGroupFacade.addUser(org2Id, user1, "g1", "g2", "g3");
      userGroupFacade.addUser(org2Id, user2);
      userGroupFacade.addUser(org2Id, user3);

      Map<String, List<String>> users = userGroupFacade.getUsersAndGroups(org1Id);
      assertThat(users).hasSize(2).containsOnlyKeys(user1, user2);
      assertThat(users.get(user1)).isEmpty();
      assertThat(users.get(user2)).hasSize(3);

      users = userGroupFacade.getUsersAndGroups(org2Id);
      assertThat(users).hasSize(3).containsOnlyKeys(user1, user2, user3);
      assertThat(users.get(user1)).hasSize(3);
      assertThat(users.get(user2)).isEmpty();
      assertThat(users.get(user3)).isEmpty();
   }

   @Test
   public void testRemoveUser() throws Exception {
      final String organization1 = "organization11";
      final String organization2 = "organization12";
      final String user1 = "user1";
      final String user2 = "user2";
      final String user3 = "user3";

      String org1Id = organizationFacade.createOrganization(organization1, "Org one");
      String org2Id = organizationFacade.createOrganization(organization2, "Org two");

      userGroupFacade.addUser(org1Id, user1);
      userGroupFacade.addUser(org1Id, user2);
      userGroupFacade.addUser(org2Id, user1);
      userGroupFacade.addUser(org2Id, user2);
      userGroupFacade.addUser(org2Id, user3);

      Map<String, List<String>> users = userGroupFacade.getUsersAndGroups(org1Id);
      assertThat(users).hasSize(2);
      userGroupFacade.removeUser(org1Id, user1);
      users = userGroupFacade.getUsersAndGroups(org1Id);
      assertThat(users).hasSize(1);

      users = userGroupFacade.getUsersAndGroups(org2Id);
      assertThat(users).hasSize(3);
      userGroupFacade.removeUser(org2Id, user1);
      userGroupFacade.removeUser(org2Id, user3);
      users = userGroupFacade.getUsersAndGroups(org2Id);
      assertThat(users).hasSize(1);
   }

   @Test
   public void testAddUserToGroup() throws Exception {
      final String organization1 = "organization21";
      final String organization2 = "organization22";
      final String user1 = "user1";
      final String user2 = "user2";

      String org1Id = organizationFacade.createOrganization(organization1, "Org one");
      String org2Id = organizationFacade.createOrganization(organization2, "Org two");

      userGroupFacade.addUser(org1Id, user1);
      userGroupFacade.addUser(org1Id, user2);
      userGroupFacade.addUser(org2Id, user1);
      userGroupFacade.addUser(org2Id, user2);

      userGroupFacade.addUserToGroup(org1Id, user1, "g1", "g2");
      userGroupFacade.addUserToGroup(org1Id, user2, "g3", "g4");
      userGroupFacade.addUserToGroup(org2Id, user1, "g5", "g6");
      userGroupFacade.addUserToGroup(org2Id, user2, "g7");

      Map<String, List<String>> users = userGroupFacade.getUsersAndGroups(org1Id);
      assertThat(users).hasSize(2).containsOnlyKeys(user1, user2);
      assertThat(users.get(user1)).hasSize(2).containsOnly("g1", "g2");
      assertThat(users.get(user2)).hasSize(2).containsOnly("g3", "g4");

      users = userGroupFacade.getUsersAndGroups(org2Id);
      assertThat(users).hasSize(2).containsOnlyKeys(user1, user2);
      assertThat(users.get(user1)).hasSize(2).containsOnly("g5", "g6");
      assertThat(users.get(user2)).hasSize(1).containsOnly("g7");
   }

   @Test
   public void testRemoveUserFromGroup() throws Exception {
      final String organization1 = "organization31";
      final String organization2 = "organization32";
      final String user1 = "user1";
      final String user2 = "user2";

      String org1Id = organizationFacade.createOrganization(organization1, "Org one");
      String org2Id = organizationFacade.createOrganization(organization2, "Org two");

      userGroupFacade.addUser(org1Id, user1, "g1", "g2", "g3");
      userGroupFacade.addUser(org1Id, user2, "g1", "g2", "g3");
      userGroupFacade.addUser(org2Id, user1, "g1", "g2", "g3");
      userGroupFacade.addUser(org2Id, user2, "g1", "g2", "g3");

      userGroupFacade.removeUserFromGroup(org1Id, user1, "g1");
      userGroupFacade.removeUserFromGroup(org1Id, user2, "g1", "g2");
      userGroupFacade.removeUserFromGroup(org2Id, user1, "g3");
      userGroupFacade.removeUserFromGroup(org2Id, user2, "g1", "g2", "g3");

      Map<String, List<String>> users = userGroupFacade.getUsersAndGroups(org1Id);
      assertThat(users).hasSize(2).containsOnlyKeys(user1, user2);
      assertThat(users.get(user1)).hasSize(2).containsOnly("g2", "g3");
      assertThat(users.get(user2)).hasSize(1).containsOnly("g3");

      users = userGroupFacade.getUsersAndGroups(org2Id);
      assertThat(users).hasSize(2).containsOnlyKeys(user1, user2);
      assertThat(users.get(user1)).hasSize(2).containsOnly("g1", "g2");
      assertThat(users.get(user2)).isEmpty();
   }

   @Test
   public void testGetUsers() throws Exception {
      final String organization1 = "organization41";
      final String organization2 = "organization42";
      final String user1 = "user1";
      final String user2 = "user2";
      final String user3 = "user3";
      final String user4 = "user4";

      String org1Id = organizationFacade.createOrganization(organization1, "Org one");
      String org2Id = organizationFacade.createOrganization(organization2, "Org two");

      userGroupFacade.addUser(org1Id, user1);
      userGroupFacade.addUser(org1Id, user2);
      userGroupFacade.addUser(org2Id, user2);
      userGroupFacade.addUser(org2Id, user3);
      userGroupFacade.addUser(org2Id, user4);

      List<String> users = userGroupFacade.getUsers(org1Id);
      assertThat(users).hasSize(2).containsOnly(user1, user2);

      users = userGroupFacade.getUsers(org2Id);
      assertThat(users).hasSize(3).containsOnly(user2, user3, user4);
   }

   @Test
   public void testGetUsersAndGroups() throws Exception {
      final String organization1 = "organization51";
      final String organization2 = "organization52";
      final String user1 = "user1";
      final String user2 = "user2";
      final String user3 = "user3";

      String org1Id = organizationFacade.createOrganization(organization1, "Org one");
      String org2Id = organizationFacade.createOrganization(organization2, "Org two");

      userGroupFacade.addUser(org1Id, user1, "g1", "g2", "g3");
      userGroupFacade.addUser(org1Id, user2, "g4", "g5", "g6", "g7");
      userGroupFacade.addUser(org2Id, user3, "g8", "g9");

      Map<String, List<String>> users = userGroupFacade.getUsersAndGroups(org1Id);
      assertThat(users).hasSize(2).containsOnlyKeys(user1, user2);
      assertThat(users.get(user1)).hasSize(3).containsOnly("g1", "g2", "g3");
      assertThat(users.get(user2)).hasSize(4).containsOnly("g4", "g5", "g6", "g7");

      users = userGroupFacade.getUsersAndGroups(org2Id);
      assertThat(users).hasSize(1).containsOnlyKeys(user3);
      assertThat(users.get(user3)).hasSize(2).containsOnly("g8", "g9");

   }

   @Test
   public void testGetGroupsOfUser() throws Exception {
      final String organization1 = "organization61";
      final String organization2 = "organization62";
      final String user1 = "user1";
      final String user2 = "user2";
      final String user3 = "user3";

      String org1Id = organizationFacade.createOrganization(organization1, "Org one");
      String org2Id = organizationFacade.createOrganization(organization2, "Org two");

      userGroupFacade.addUser(org1Id, user1, "g1", "g2", "g3");
      userGroupFacade.addUser(org1Id, user2, "g4", "g5", "g6", "g7");
      userGroupFacade.addUser(org2Id, user3, "g8", "g9");

      List<String> users = userGroupFacade.getGroupsOfUser(org1Id, user1);
      assertThat(users).hasSize(3).containsOnly("g1", "g2", "g3");
      users = userGroupFacade.getGroupsOfUser(org1Id, user2);
      assertThat(users).hasSize(4).containsOnly("g4", "g5", "g6", "g7");

      users = userGroupFacade.getGroupsOfUser(org2Id, user3);
      assertThat(users).hasSize(2).containsOnly("g8", "g9");

   }

   @Test
   public void testGetUsersInGroup() throws Exception {
      final String organization1 = "organization71";
      final String organization2 = "organization72";
      final String user1 = "user1";
      final String user2 = "user2";
      final String user3 = "user3";

      String org1Id = organizationFacade.createOrganization(organization1, "Org one");
      String org2Id = organizationFacade.createOrganization(organization2, "Org two");

      userGroupFacade.addUser(org1Id, user1, "g1", "g2");
      userGroupFacade.addUser(org1Id, user2, "g1", "g2", "g3");
      userGroupFacade.addUser(org1Id, user3, "g2");
      userGroupFacade.addUser(org2Id, user1, "g1");
      userGroupFacade.addUser(org2Id, user2, "g1", "g2", "g3");
      userGroupFacade.addUser(org2Id, user3, "g1", "g2", "g3");

      List<String> users = userGroupFacade.getUsersInGroup(org1Id, "g1");
      assertThat(users).hasSize(2).containsOnly(user1, user2);
      users = userGroupFacade.getUsersInGroup(org1Id, "g2");
      assertThat(users).hasSize(3).containsOnly(user1, user2, user3);
      users = userGroupFacade.getUsersInGroup(org1Id, "g3");
      assertThat(users).hasSize(1).containsOnly(user2);

      users = userGroupFacade.getUsersInGroup(org2Id, "g1");
      assertThat(users).hasSize(3).containsOnly(user1, user2, user3);
      users = userGroupFacade.getUsersInGroup(org2Id, "g2");
      assertThat(users).hasSize(2).containsOnly(user2, user3);
      users = userGroupFacade.getUsersInGroup(org2Id, "g3");
      assertThat(users).hasSize(2).containsOnly(user2, user3);

   }

   @Test
   public void testAddGroup() throws Exception {
      final String organization1 = "organization81";
      final String organization2 = "organization82";
      final String group1 = "group1";
      final String group2 = "group2";
      final String group3 = "group3";
      final String group4 = "group4";
      final String group5 = "group5";

      String org1Id = organizationFacade.createOrganization(organization1, "Org one");
      String org2Id = organizationFacade.createOrganization(organization2, "Org two");

      userGroupFacade.addGroup(org1Id, group1, group2, group3);
      userGroupFacade.addGroup(org2Id, group3, group4, group5);

      List<String> groups = userGroupFacade.getGroups(org1Id);
      assertThat(groups).hasSize(3).containsOnly(group1, group2, group3);
      groups = userGroupFacade.getGroups(org2Id);
      assertThat(groups).hasSize(3).containsOnly(group3, group4, group5);
   }

   @Test
   public void testRemoveGroup() throws Exception {
      final String organization1 = "organization91";
      final String organization2 = "organization92";
      final String group1 = "group1";
      final String group2 = "group2";
      final String group3 = "group3";
      final String group4 = "group4";
      final String group5 = "group5";

      final String user1 = "user1";
      final String user2 = "user2";

      String org1Id = organizationFacade.createOrganization(organization1, "Org one");
      String org2Id = organizationFacade.createOrganization(organization2, "Org two");

      userGroupFacade.addGroup(org1Id, group1, group2, group3);
      userGroupFacade.addGroup(org2Id, group3, group4, group5);

      userGroupFacade.addUser(org1Id, user1, group1, group2, group3);
      userGroupFacade.addUser(org1Id, user2, group2, group3, group4);
      userGroupFacade.addUser(org2Id, user1, group3, group4, group5);
      userGroupFacade.addUser(org2Id, user2, group1, group2, group3, group4);

      userGroupFacade.removeGroup(org1Id, group2);
      userGroupFacade.removeGroup(org2Id, group3, group4, group5);

      List<String> groups = userGroupFacade.getGroups(org1Id);
      assertThat(groups).hasSize(2).containsOnly(group1, group3);
      groups = userGroupFacade.getGroups(org2Id);
      assertThat(groups).isEmpty();

      // check if groups was deleted from users correctly

      Map<String, List<String>> users = userGroupFacade.getUsersAndGroups(org1Id);
      assertThat(users).hasSize(2).containsOnlyKeys(user1, user2);
      assertThat(users.get(user1)).hasSize(2).containsOnly(group1, group3);
      assertThat(users.get(user2)).hasSize(2).containsOnly(group3, group4);

      users = userGroupFacade.getUsersAndGroups(org2Id);
      assertThat(users).hasSize(2).containsOnlyKeys(user1, user2);
      assertThat(users.get(user1)).isEmpty();
      assertThat(users.get(user2)).hasSize(2).containsOnly(group1, group2);
   }

   @Test
   public void testGetGroups() throws Exception {
      final String organization1 = "organization91";
      final String organization2 = "organization92";
      final String group1 = "group1";
      final String group2 = "group2";
      final String group3 = "group3";
      final String group4 = "group4";
      final String group5 = "group5";

      String org1Id = organizationFacade.createOrganization(organization1, "Org one");
      String org2Id = organizationFacade.createOrganization(organization2, "Org two");

      userGroupFacade.addGroup(org1Id, group1, group3);
      userGroupFacade.addGroup(org2Id, group2, group3, group4, group5);

      List<String> groups = userGroupFacade.getGroups(org1Id);
      assertThat(groups).hasSize(2).containsOnly(group1, group3);
      groups = userGroupFacade.getGroups(org2Id);
      assertThat(groups).hasSize(4).containsOnly(group2, group3, group4, group5);
   }

}
