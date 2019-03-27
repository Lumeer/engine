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
package io.lumeer.remote.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.Permissions;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.QueryStem;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.api.model.View;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.exception.StorageException;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

@RunWith(Arquillian.class)
public class LinkTypeServiceIT extends ServiceIntegrationTestBase {

   private static final String ORGANIZATION_CODE = "LMR";
   private static final String PROJECT_CODE = "PROJ";

   private static final List<String> COLLECTION_NAMES = Arrays.asList("Collection1", "Collection2", "Collection3");
   private static final String COLLECTION_ICON = "fa-eye";
   private static final String COLLECTION_COLOR = "#00ee00";

   private static final String USER = AuthenticatedUser.DEFAULT_EMAIL;

   private static final String NAME = "Connection";
   private static final String NAME2 = "Whuaaaa";

   private String linkTypesUrl;

   private List<String> collectionIds = new ArrayList<>();
   private String collectionIdNoPerm;
   private String linkTypeIdFromView;

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
   private ViewDao viewDao;

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
      Permission userPermission = Permission.buildWithRoles(createdUser.getId(), Collections.singleton(Role.READ));
      organizationPermissions.updateUserPermissions(userPermission);
      storedOrganization.setPermissions(organizationPermissions);
      organizationDao.updateOrganization(storedOrganization.getId(), storedOrganization);

      Project project = new Project();
      project.setPermissions(new Permissions());
      project.setCode(PROJECT_CODE);
      Project storedProject = projectDao.createProject(project);

      Permissions projectPermissions = new Permissions();
      Permission userProjectPermission = Permission.buildWithRoles(createdUser.getId(), Collections.singleton(Role.READ));
      projectPermissions.updateUserPermissions(userProjectPermission);
      storedProject.setPermissions(projectPermissions);
      storedProject = projectDao.updateProject(storedProject.getId(), storedProject);

      collectionDao.setProject(storedProject);
      linkTypeDao.setProject(storedProject);
      viewDao.setProject(storedProject);

      collectionIds.clear();

      Permissions userPermissions = new Permissions();
      userPermissions.updateUserPermissions(new Permission(createdUser.getId(), Project.ROLES.stream().map(Role::toString).collect(Collectors.toSet())));

      for (String name : COLLECTION_NAMES) {
         Collection collection = new Collection(name, name, COLLECTION_ICON, COLLECTION_COLOR, userPermissions);
         collection.setDocumentsCount(0);
         collectionIds.add(collectionDao.createCollection(collection).getId());
      }

      Collection collection = new Collection("noPerm", "noPerm", COLLECTION_ICON, COLLECTION_COLOR, new Permissions());
      collectionIdNoPerm = collectionDao.createCollection(collection).getId();

      LinkType linkType = prepareLinkType();
      linkType.setCollectionIds(Arrays.asList(collectionIdNoPerm, collectionIds.get(1)));
      linkTypeIdFromView = linkTypeDao.createLinkType(linkType).getId();

      QueryStem stem = new QueryStem(collectionIdNoPerm, Collections.singletonList(linkTypeIdFromView), Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
      Query query = new Query(stem);

      View view = new View("code", "name", "", "", "", userPermissions, query, "perspective", "", createdUser.getId());
      viewDao.createView(view);

      linkTypesUrl = projectPath(storedOrganization, storedProject) + "link-types";
   }

   @Test
   public void testCreateLinkType() {
      LinkType linkType = prepareLinkType();

      Entity entity = Entity.json(linkType);

      Response response = client.target(linkTypesUrl)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      LinkType returnedLinkType = response.readEntity(LinkType.class);

      assertThat(returnedLinkType).isNotNull();
      assertThat(returnedLinkType.getName()).isEqualTo(NAME);
      assertThat(returnedLinkType.getCollectionIds()).containsOnlyElementsOf(Arrays.asList(collectionIds.get(0), collectionIds.get(1)));
   }

   @Test
   public void testUpdateLinkType() {
      LinkType linkType = prepareLinkType();
      String id = linkTypeDao.createLinkType(linkType).getId();

      LinkType updateLinkedType = prepareLinkType();
      updateLinkedType.setName(NAME2);
      updateLinkedType.setCollectionIds(Arrays.asList(collectionIds.get(1), collectionIds.get(2)));

      Entity entity = Entity.json(updateLinkedType);
      Response response = client.target(linkTypesUrl).path(id)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      LinkType returnedLinkType = response.readEntity(LinkType.class);
      assertThat(returnedLinkType).isNotNull();

      assertThat(returnedLinkType).isNotNull();
      assertThat(returnedLinkType.getName()).isEqualTo(NAME2);

      LinkType storedLinkType = linkTypeDao.getLinkType(id);
      assertThat(storedLinkType).isNotNull();
      assertThat(storedLinkType.getName()).isEqualTo(NAME2);
   }

   @Test
   public void testDeleteLinkType() {
      LinkType created = linkTypeDao.createLinkType(prepareLinkType());
      assertThat(created.getId()).isNotNull();

      Response response = client.target(linkTypesUrl).path(created.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(linkTypesUrl).build());

      assertThatThrownBy(() -> linkTypeDao.getLinkType(created.getId()))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testGetLinkTypes() {
      String id1 = linkTypeDao.createLinkType(prepareLinkType()).getId();

      LinkType linkType2 = prepareLinkType();
      linkType2.setCollectionIds(Arrays.asList(collectionIdNoPerm, collectionIds.get(2)));
      linkTypeDao.createLinkType(linkType2);

      LinkType linkType3 = prepareLinkType();
      linkType3.setCollectionIds(Arrays.asList(collectionIds.get(1), collectionIds.get(0)));
      String id3 = linkTypeDao.createLinkType(linkType3).getId();

      LinkType linkType4 = prepareLinkType();
      linkType4.setCollectionIds(Arrays.asList(collectionIds.get(0), collectionIds.get(2)));
      String id4 = linkTypeDao.createLinkType(linkType4).getId();

      Response response = client.target(linkTypesUrl)
                                .request(MediaType.APPLICATION_JSON)
                                .buildGet().invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<LinkType> linkTypes = response.readEntity(new GenericType<List<LinkType>>() {
      });
      assertThat(linkTypes).extracting("id").containsOnlyElementsOf(Arrays.asList(id1, id3, id4));

      // test fromViews
      response = client.target(linkTypesUrl)
                       .queryParam("fromViews", true)
                       .request(MediaType.APPLICATION_JSON)
                       .buildGet().invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      linkTypes = response.readEntity(new GenericType<List<LinkType>>() {
      });
      assertThat(linkTypes).extracting("id").containsOnlyElementsOf(Arrays.asList(id1, id3, id4, linkTypeIdFromView));

   }

   private LinkType prepareLinkType() {
      return new LinkType(NAME, Arrays.asList(collectionIds.get(0), collectionIds.get(1)), Collections.emptyList());
   }
}
