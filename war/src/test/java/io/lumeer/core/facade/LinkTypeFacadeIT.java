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
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.core.WorkspaceKeeper;
import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.storage.api.dao.CollectionDao;
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
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class LinkTypeFacadeIT extends IntegrationTestBase {

   private static final String ORGANIZATION_CODE = "LMR";
   private static final String PROJECT_CODE = "PROJ";

   private static final List<String> COLLECTION_NAMES = Arrays.asList("Collection1", "Collection2", "Collection3");
   private static final String COLLECTION_ICON = "fa-eye";
   private static final String COLLECTION_COLOR = "#00ee00";

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;

   private static final String NAME = "Connection";
   private static final String NAME2 = "Whuaaaa";

   private List<String> collectionIds = new ArrayList<>();
   private String collectionIdNoPerm;

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
   private WorkspaceKeeper workspaceKeeper;

   @Before
   public void configureLinkTypes() {
      User user = new User(USER);
      final User createdUser = userDao.createUser(user);

      Organization organization = new Organization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new Permissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);

      Permissions organizationPermissions = new Permissions();
      Permission userPermission = Permission.buildWithRoles(createdUser.getId(), new HashSet<>(Collections.singletonList(Role.READ)));
      organizationPermissions.updateUserPermissions(userPermission);
      storedOrganization.setPermissions(organizationPermissions);
      organizationDao.updateOrganization(storedOrganization.getId(), storedOrganization);

      Project project = new Project();
      project.setPermissions(new Permissions());
      project.setCode(PROJECT_CODE);
      Project storedProject = projectDao.createProject(project);

      Permissions projectPermissions = new Permissions();
      Permission userProjectPermission = Permission.buildWithRoles(createdUser.getId(), new HashSet<>(Collections.singletonList(Role.READ)));
      projectPermissions.updateUserPermissions(userProjectPermission);
      storedProject.setPermissions(projectPermissions);
      storedProject = projectDao.updateProject(storedProject.getId(), storedProject);

      workspaceKeeper.setWorkspaceIds(storedOrganization.getId(), storedProject.getId());

      collectionDao.setProject(storedProject);

      collectionIds.clear();

      for (String name : COLLECTION_NAMES) {
         Permissions collectionPermissions = new Permissions();
         collectionPermissions.updateUserPermissions(new Permission(createdUser.getId(), Project.ROLES.stream().map(Role::toString).collect(Collectors.toSet())));
         Collection collection = new Collection(name, name, COLLECTION_ICON, COLLECTION_COLOR, collectionPermissions);
         collection.setDocumentsCount(0L);
         collectionIds.add(collectionDao.createCollection(collection).getId());
      }

      Collection collection = new Collection("noPerm", "noPerm", COLLECTION_ICON, COLLECTION_COLOR, new Permissions());
      collectionIdNoPerm = collectionDao.createCollection(collection).getId();
   }

   @Test
   public void testCreateLinkType() {
      LinkType linkType = prepareLinkType();

      String id = linkTypeFacade.createLinkType(linkType).getId();
      assertThat(id).isNotNull().isNotEmpty();
      assertThat(ObjectId.isValid(id)).isTrue();

      LinkType storedLinkType = linkTypeDao.getLinkType(id);
      assertThat(storedLinkType).isNotNull();
      assertThat(storedLinkType.getName()).isEqualTo(NAME);
      assertThat(storedLinkType.getCollectionIds()).containsOnly(collectionIds.get(0), collectionIds.get(1));
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
      assertThat(storedLinkType.getName()).isEqualTo(NAME2);
   }

   @Test
   public void testDeleteLinkType() {
      LinkType created = linkTypeFacade.createLinkType(prepareLinkType());
      assertThat(created.getId()).isNotNull();

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
   public void testAddAttribute(){
      String id = linkTypeFacade.createLinkType(prepareLinkType()).getId();
      LinkType linkType = linkTypeFacade.getLinkType(id);
      assertThat(linkType.getAttributes()).isEmpty();

      linkTypeFacade.createLinkTypeAttributes(id, Collections.singletonList(new Attribute("LMR")));
      linkType = linkTypeFacade.getLinkType(id);
      assertThat(linkType.getAttributes()).hasSize(1);
      assertThat(linkType.getAttributes().get(0).getId()).isEqualTo(LinkType.ATTRIBUTE_PREFIX + "1");
   }

   @Test
   public void testUpdateAttribute(){
      String id = linkTypeFacade.createLinkType(prepareLinkType()).getId();
      LinkType linkType = linkTypeFacade.getLinkType(id);
      assertThat(linkType.getAttributes()).isEmpty();

      linkTypeFacade.createLinkTypeAttributes(id, Collections.singletonList(new Attribute("LMR")));
      linkType = linkTypeFacade.getLinkType(id);
      assertThat(linkType.getAttributes()).hasSize(1);
      assertThat(linkType.getAttributes().get(0).getName()).isEqualTo("LMR");

      linkTypeFacade.updateLinkTypeAttribute(id, LinkType.ATTRIBUTE_PREFIX + "1", new Attribute("LMR UPDATED"));
      linkType = linkTypeFacade.getLinkType(id);
      assertThat(linkType.getAttributes()).hasSize(1);
      assertThat(linkType.getAttributes().get(0).getName()).isEqualTo("LMR UPDATED");
   }

   @Test
   public void testDeleteAttribute(){
      String id = linkTypeFacade.createLinkType(prepareLinkType()).getId();
      LinkType linkType = linkTypeFacade.getLinkType(id);
      assertThat(linkType.getAttributes()).isEmpty();

      linkTypeFacade.createLinkTypeAttributes(id, Collections.singletonList(new Attribute("LMR")));
      linkType = linkTypeFacade.getLinkType(id);
      assertThat(linkType.getAttributes()).hasSize(1);
      assertThat(linkType.getAttributes().get(0).getName()).isEqualTo("LMR");

      linkTypeFacade.deleteLinkTypeAttribute(id, LinkType.ATTRIBUTE_PREFIX + "1");
      linkType = linkTypeFacade.getLinkType(id);
      assertThat(linkType.getAttributes()).isEmpty();
   }

   private LinkType prepareLinkType() {
      return new LinkType(NAME, Arrays.asList(collectionIds.get(0), collectionIds.get(1)), Collections.emptyList(), null);
   }

}
