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
package io.lumeer.storage.mongodb.dao.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Group;
import io.lumeer.api.model.Organization;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.mongodb.MongoDbTestBase;
import io.lumeer.storage.mongodb.util.MongoFilters;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

public class MongoGroupDaoTest extends MongoDbTestBase {

   private static final String ORGANIZATION_ID = "596e3b86d412bc5a3caaa22a";

   private static final String GROUP = "testGroup";
   private static final String GROUP2 = "testGroup2";
   private static final String NOT_EXISTING_ID = "598323f5d412bc7a51b5a460";

   private MongoGroupDao mongoGroupDao;

   private Organization organization;

   @Before
   public void initGroupDao() {
      organization = Mockito.mock(Organization.class);
      Mockito.when(organization.getId()).thenReturn(ORGANIZATION_ID);

      mongoGroupDao = new MongoGroupDao();
      mongoGroupDao.setDatabase(database);

      mongoGroupDao.setOrganization(organization);
      mongoGroupDao.createRepository(organization);
      assertThat(database.listCollectionNames()).contains(mongoGroupDao.databaseCollectionName());
   }

   @Test
   public void testDeleteRepository() {
      mongoGroupDao.deleteRepository(organization);
      assertThat(database.listCollectionNames()).doesNotContain(mongoGroupDao.databaseCollectionName());
   }

   @Test
   public void testCreateGroup() {
      Group group = new Group(GROUP);
      String id = mongoGroupDao.createGroup(group).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      Group storedGroup = mongoGroupDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedGroup).isNotNull();
      assertThat(storedGroup.getId()).isEqualTo(id);
      assertThat(storedGroup.getName()).isEqualTo(GROUP);
   }

   @Test
   public void testCreateExistingGroup() {
      Group group = new Group(GROUP);
      mongoGroupDao.createGroup(group);
      assertThatThrownBy(() -> mongoGroupDao.createGroup(group))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testUpdateGroup() {
      Group group = new Group(GROUP);
      String id = mongoGroupDao.createGroup(group).getId();

      group.setName(GROUP2);
      mongoGroupDao.updateGroup(id, group);

      Group storedGroup = mongoGroupDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedGroup).isNotNull();
      assertThat(storedGroup.getId()).isEqualTo(id);
      assertThat(storedGroup.getName()).isEqualTo(GROUP2);
   }

   @Test
   public void testDeleteUsers() {
      Group group = new Group(GROUP);
      group.setUsers(Set.of("A", "B", "C"));
      String id = mongoGroupDao.createGroup(group).getId();

      Group group2 = new Group(GROUP2);
      group2.setUsers(Set.of("B", "C", "D"));
      String id2 = mongoGroupDao.createGroup(group2).getId();

      mongoGroupDao.deleteUserFromGroups("A");
      assertThat(mongoGroupDao.getGroup(id).getUsers()).containsOnly("B", "C");
      assertThat(mongoGroupDao.getGroup(id2).getUsers()).containsOnly("B", "C", "D");

      mongoGroupDao.deleteUserFromGroups("B");
      assertThat(mongoGroupDao.getGroup(id).getUsers()).containsOnly("C");
      assertThat(mongoGroupDao.getGroup(id2).getUsers()).containsOnly("C", "D");

      mongoGroupDao.deleteUserFromGroups("C");
      assertThat(mongoGroupDao.getGroup(id).getUsers()).isEmpty();
      assertThat(mongoGroupDao.getGroup(id2).getUsers()).containsOnly("D");

   }

   @Test
   public void testUpdateExistingGroup() {
      Group group = new Group(GROUP);
      mongoGroupDao.createGroup(group);

      Group group2 = new Group(GROUP2);
      String id = mongoGroupDao.createGroup(group2).getId();

      group2.setName(GROUP);

      assertThatThrownBy(() -> mongoGroupDao.updateGroup(id, group2))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testDeleteGroup() {
      Group group = new Group(GROUP);
      String id = mongoGroupDao.createGroup(group).getId();

      Group storedGroup = mongoGroupDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedGroup).isNotNull();

      mongoGroupDao.deleteGroup(id);
      storedGroup = mongoGroupDao.databaseCollection().find(MongoFilters.idFilter(id)).first();
      assertThat(storedGroup).isNull();
   }

   @Test
   public void testDeleteGroupNotExisting() {
      assertThatThrownBy(() -> mongoGroupDao.deleteGroup(NOT_EXISTING_ID))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testGetGroups() {
      mongoGroupDao.createGroup(new Group(GROUP));

      Group group = new Group(GROUP2);
      mongoGroupDao.createGroup(group);

      List<Group> groupList = mongoGroupDao.getAllGroups();
      assertThat(groupList).extracting(Group::getName).containsOnly(GROUP, GROUP2);

   }

   @Test
   public void testGetGroupsEmpty() {
      List<Group> groupList = mongoGroupDao.getAllGroups();
      assertThat(groupList).isEmpty();
   }

}
