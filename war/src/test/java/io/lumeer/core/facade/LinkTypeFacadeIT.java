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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Group;
import io.lumeer.api.model.LinkPermissionsType;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.User;
import io.lumeer.api.model.common.AttributesResource;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.core.auth.PermissionCheckerUtil;
import io.lumeer.core.auth.PermissionsChecker;
import io.lumeer.core.exception.NoResourcePermissionException;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.GroupDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.StorageException;

import org.bson.types.ObjectId;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;

@RunWith(Arquillian.class)
public class LinkTypeFacadeIT extends IntegrationTestBase {

   private static final String ORGANIZATION_CODE = "LMR";
   private static final String PROJECT_CODE = "PROJ";

   private static final List<String> COLLECTION_NAMES = Arrays.asList("Collection1", "Collection2", "Collection3");
   private static final String COLLECTION_ICON = "fa-eye";
   private static final String COLLECTION_COLOR = "#00ee00";

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;
   private static final String GROUP = "testGroup";

   private static final String NAME = "Connection";
   private static final String NAME2 = "Whuaaaa";

   private List<String> collectionIds = new ArrayList<>();
   private String collectionIdNoPerm;
   private Organization organization;
   private Project project;
   private User user;
   private Group group;

   @Inject
   private LinkTypeFacade linkTypeFacade;

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private OrganizationDao organizationDao;

   @Inject
   private ProjectDao projectDao;

   @Inject
   private UserDao userDao;

   @Inject
   private GroupDao groupDao;

   @Inject
   private WorkspaceKeeper workspaceKeeper;

   @Inject
   private PermissionsChecker permissionsChecker;

   @Before
   public void configureLinkTypes() {
      user = userDao.createUser(new User(USER));

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new Permissions());
      this.organization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(this.organization);
      groupDao.setOrganization(this.organization);
      group = groupDao.createGroup(new Group(GROUP, Collections.singletonList(user.getId())));
      user.setOrganizations(Collections.singleton(this.organization.getId()));
      user = userDao.updateUser(user.getId(), user);

      Permissions organizationPermissions = new Permissions();
      Permission userPermission = Permission.buildWithRoles(user.getId(), Collections.singleton(new Role(RoleType.Read)));
      organizationPermissions.updateUserPermissions(userPermission);
      this.organization.setPermissions(organizationPermissions);
      this.organization = organizationDao.updateOrganization(this.organization.getId(), this.organization);

      Project project = new Project();
      project.setPermissions(new Permissions());
      project.setCode(PROJECT_CODE);
      this.project = projectDao.createProject(project);

      Permissions projectPermissions = new Permissions();
      Permission userProjectPermission = Permission.buildWithRoles(user.getId(), Set.of(new Role(RoleType.Read), new Role(RoleType.LinkContribute)));
      projectPermissions.updateUserPermissions(userProjectPermission);
      this.project.setPermissions(projectPermissions);
      this.project = projectDao.updateProject(this.project.getId(), this.project);

      workspaceKeeper.setWorkspaceIds(this.organization.getId(), this.project.getId());

      collectionDao.setProject(this.project);

      collectionIds.clear();

      for (String name : COLLECTION_NAMES) {
         Permissions collectionPermissions = new Permissions();
         collectionPermissions.updateUserPermissions(new Permission(user.getId(), Collections.singleton(new Role(RoleType.Read))));
         Collection collection = new Collection(name, name, COLLECTION_ICON, COLLECTION_COLOR, collectionPermissions);
         collectionIds.add(collectionDao.createCollection(collection).getId());
      }

      Collection collection = new Collection("noPerm", "noPerm", COLLECTION_ICON, COLLECTION_COLOR, new Permissions());
      collectionIdNoPerm = collectionDao.createCollection(collection).getId();

      PermissionCheckerUtil.allowGroups();

      permissionsChecker.getPermissionAdapter().invalidateUserCache();
   }

   @Test
   public void testCreateLinkType() {
      LinkType linkType = prepareLinkType();

      setProjectUserRoles(Set.of(new Role(RoleType.Read)));

      assertThatThrownBy(() -> linkTypeFacade.createLinkType(linkType))
            .isInstanceOf(NoResourcePermissionException.class);

      setProjectUserRoles(Set.of(new Role(RoleType.Read), new Role(RoleType.LinkContribute)));

      String id = linkTypeFacade.createLinkType(linkType).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      LinkType storedLinkType = linkTypeDao.getLinkType(id);
      assertThat(storedLinkType).isNotNull();
      assertThat(storedLinkType.getName()).isEqualTo(NAME);
      assertThat(storedLinkType.getCollectionIds()).containsOnly(collectionIds.get(0), collectionIds.get(1));
   }

   private void setProjectUserRoles(final Set<Role> roles) {
      Permissions projectPermissions = new Permissions();
      projectPermissions.updateUserPermissions(Permission.buildWithRoles(this.user.getId(), roles));
      project.setPermissions(projectPermissions);
      projectDao.updateProject(project.getId(), project);
      workspaceCache.clear();
   }

   @Test
   public void testUpdateLinkType() {
      LinkType linkType = prepareLinkType();
      String id = linkTypeFacade.createLinkType(linkType).getId();

      LinkType updateLinkedType = prepareLinkType();
      updateLinkedType.setName(NAME2);

      linkTypeFacade.updateLinkType(id, updateLinkedType);

      LinkType storedLinkType = linkTypeDao.getLinkType(id);
      assertThat(storedLinkType).isNotNull();
      assertThat(storedLinkType.getName()).isEqualTo(NAME);

      setCollectionsGroupRoles(Set.of(new Role(RoleType.Read), new Role(RoleType.Manage)));

      linkTypeFacade.updateLinkType(id, updateLinkedType);

      storedLinkType = linkTypeDao.getLinkType(id);
      assertThat(storedLinkType).isNotNull();
      assertThat(storedLinkType.getName()).isEqualTo(NAME2);
   }

   private void setCollectionsGroupRoles(final Set<Role> roles) {
      for (String collectionId : collectionIds) {
         Permissions collectionPermissions = new Permissions();
         collectionPermissions.updateGroupPermissions(new Permission(group.getId(), roles));
         Collection collection = collectionDao.getCollectionById(collectionId);
         collection.setPermissions(collectionPermissions);
         collectionDao.updateCollection(collectionId, collection, null);
      }
      permissionsChecker.getPermissionAdapter().invalidateCollectionCache();
   }

   private LinkType setLinkTypePermissions(LinkType linkType, final Set<Role> roles) {
      if (roles.isEmpty()) {
         linkType.setPermissions(null);
         linkType.setPermissionsType(LinkPermissionsType.Merge);
      } else {
         Permissions permissions = new Permissions();
         permissions.updateUserPermissions(new Permission(user.getId(), roles));
         linkType.setPermissions(permissions);
         linkType.setPermissionsType(LinkPermissionsType.Custom);
      }
      return linkTypeDao.updateLinkType(linkType.getId(), linkType, null);
   }

   @Test
   public void testDeleteLinkType() {
      LinkType created = linkTypeFacade.createLinkType(prepareLinkType());
      assertThat(created.getId()).isNotNull();

      setLinkTypePermissions(created, Set.of(new Role(RoleType.Read), new Role(RoleType.Manage)));

      linkTypeFacade.deleteLinkType(created.getId());

      assertThatThrownBy(() -> linkTypeDao.getLinkType(created.getId()))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testGetLinkTypes() {
      String id1 = linkTypeFacade.createLinkType(prepareLinkType()).getId();

      LinkType linkType2 = prepareLinkType();
      linkType2.setCollectionIds(Arrays.asList(collectionIdNoPerm, collectionIds.get(2)));
      linkTypeDao.createLinkType(linkType2);

      LinkType linkType3 = prepareLinkType();
      linkType3.setCollectionIds(Arrays.asList(collectionIds.get(1), collectionIds.get(2)));
      String id3 = linkTypeFacade.createLinkType(linkType3).getId();

      LinkType linkType4 = prepareLinkType();
      linkType4.setCollectionIds(Arrays.asList(collectionIds.get(1), collectionIdNoPerm));
      linkTypeDao.createLinkType(linkType4);

      List<LinkType> linkTypes = linkTypeFacade.getLinkTypes();
      assertThat(linkTypes).extracting("id").containsOnly(id1, id3);
   }

   @Test
   public void testAddAttribute() {
      String id = linkTypeFacade.createLinkType(prepareLinkType()).getId();
      LinkType linkType = linkTypeFacade.getLinkType(id);
      assertThat(linkType.getAttributes()).isEmpty();

      setCollectionsGroupRoles(Set.of(new Role(RoleType.Read), new Role(RoleType.AttributeEdit)));

      linkTypeFacade.createLinkTypeAttributes(id, Collections.singletonList(new Attribute("LMR")));
      linkType = linkTypeFacade.getLinkType(id);
      assertThat(linkType.getAttributes()).hasSize(1);
      assertThat(linkType.getAttributes().get(0).getId()).isEqualTo(AttributesResource.ATTRIBUTE_PREFIX + "1");
   }

   @Test
   public void testUpdateAttribute() {
      String id = linkTypeFacade.createLinkType(prepareLinkType()).getId();
      LinkType linkType = linkTypeFacade.getLinkType(id);
      assertThat(linkType.getAttributes()).isEmpty();

      setLinkTypePermissions(linkType, Set.of(new Role(RoleType.Read), new Role(RoleType.AttributeEdit)));

      linkTypeFacade.createLinkTypeAttributes(id, Collections.singletonList(new Attribute("LMR")));
      linkType = linkTypeFacade.getLinkType(id);
      assertThat(linkType.getAttributes()).hasSize(1);
      assertThat(linkType.getAttributes().get(0).getName()).isEqualTo("LMR");

      linkTypeFacade.updateLinkTypeAttribute(id, AttributesResource.ATTRIBUTE_PREFIX + "1", new Attribute("LMR UPDATED"));
      linkType = linkTypeFacade.getLinkType(id);
      assertThat(linkType.getAttributes()).hasSize(1);
      assertThat(linkType.getAttributes().get(0).getName()).isEqualTo("LMR UPDATED");
   }

   @Test
   public void testDeleteAttribute() {
      String id = linkTypeFacade.createLinkType(prepareLinkType()).getId();
      LinkType linkType = linkTypeFacade.getLinkType(id);
      assertThat(linkType.getAttributes()).isEmpty();

      setCollectionsGroupRoles(Set.of(new Role(RoleType.Read), new Role(RoleType.AttributeEdit)));

      linkTypeFacade.createLinkTypeAttributes(id, Collections.singletonList(new Attribute("LMR")));
      linkType = linkTypeFacade.getLinkType(id);
      assertThat(linkType.getAttributes()).hasSize(1);
      assertThat(linkType.getAttributes().get(0).getName()).isEqualTo("LMR");

      linkTypeFacade.deleteLinkTypeAttribute(id, AttributesResource.ATTRIBUTE_PREFIX + "1");
      linkType = linkTypeFacade.getLinkType(id);
      assertThat(linkType.getAttributes()).isEmpty();
   }

   private LinkType prepareLinkType() {
      return new LinkType(NAME, Arrays.asList(collectionIds.get(0), collectionIds.get(1)), Collections.emptyList(), null, null, null);
   }

}
