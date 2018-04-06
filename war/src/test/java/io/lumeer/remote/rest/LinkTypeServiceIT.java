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

import io.lumeer.api.dto.JsonAttribute;
import io.lumeer.api.dto.JsonCollection;
import io.lumeer.api.dto.JsonOrganization;
import io.lumeer.api.dto.JsonPermission;
import io.lumeer.api.dto.JsonPermissions;
import io.lumeer.api.dto.JsonProject;
import io.lumeer.api.dto.JsonQuery;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Organization;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Role;
import io.lumeer.api.model.User;
import io.lumeer.core.AuthenticatedUser;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.OrganizationDao;
import io.lumeer.storage.api.dao.ProjectDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.exception.StorageException;

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
   private static final String ATTRIBUTE1_NAME = "Maxi";
   private static final String ATTRIBUTE2_NAME = "Light";
   private static final List<JsonAttribute> ATTRIBUTES;

   static {
      JsonAttribute attribute1 = new JsonAttribute(ATTRIBUTE1_NAME);
      JsonAttribute attribute2 = new JsonAttribute(ATTRIBUTE2_NAME);
      ATTRIBUTES = Arrays.asList(attribute1, attribute2);
   }

   private static final String SERVER_URL = "http://localhost:8080";
   private static final String LINK_TYPES_PATH = "/" + PATH_CONTEXT + "/rest/" + "organizations/" + ORGANIZATION_CODE + "/projects/" + PROJECT_CODE + "/link-types";
   private static final String LINK_TYPES_URL = SERVER_URL + LINK_TYPES_PATH;

   private List<String> collectionIds = new ArrayList<>();

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

   @Before
   public void configureLinkTypes() {
      JsonOrganization organization = new JsonOrganization();
      organization.setCode(ORGANIZATION_CODE);
      organization.setPermissions(new JsonPermissions());
      Organization storedOrganization = organizationDao.createOrganization(organization);

      projectDao.setOrganization(storedOrganization);

      User user = new User(USER);
      final User createdUser = userDao.createUser(user);

      JsonProject project = new JsonProject();
      project.setPermissions(new JsonPermissions());
      project.setCode(PROJECT_CODE);
      Project storedProject = projectDao.createProject(project);

      collectionDao.setProject(storedProject);
      linkTypeDao.setProject(storedProject);

      collectionIds.clear();

      for (String name : COLLECTION_NAMES) {
         JsonPermissions collectionPermissions = new JsonPermissions();
         collectionPermissions.updateUserPermissions(new JsonPermission(createdUser.getId(), Project.ROLES.stream().map(Role::toString).collect(Collectors.toSet())));
         JsonCollection jsonCollection = new JsonCollection(name, name, COLLECTION_ICON, COLLECTION_COLOR, collectionPermissions);
         jsonCollection.setDocumentsCount(0);
         collectionIds.add(collectionDao.createCollection(jsonCollection).getId());
      }
   }

   @Test
   public void testCreateLinkType() {
      LinkType linkType = prepareLinkType();

      Entity entity = Entity.json(linkType);

      Response response = client.target(LINK_TYPES_URL)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      LinkType returnedLinkType = response.readEntity(LinkType.class);

      assertThat(returnedLinkType).isNotNull();
      assertThat(returnedLinkType.getName()).isEqualTo(NAME);
      assertThat(returnedLinkType.getAttributes()).isEqualTo(ATTRIBUTES);
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
      Response response = client.target(LINK_TYPES_URL).path(id)
                                .request(MediaType.APPLICATION_JSON)
                                .buildPut(entity).invoke();
      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      LinkType returnedLinkType = response.readEntity(LinkType.class);
      assertThat(returnedLinkType).isNotNull();

      assertThat(returnedLinkType).isNotNull();
      assertThat(returnedLinkType.getName()).isEqualTo(NAME2);
      assertThat(returnedLinkType.getAttributes()).isEqualTo(ATTRIBUTES);
      assertThat(returnedLinkType.getCollectionIds()).containsOnlyElementsOf(Arrays.asList(collectionIds.get(1), collectionIds.get(2)));

      LinkType storedLinkType = linkTypeDao.getLinkType(id);
      assertThat(storedLinkType).isNotNull();
      assertThat(storedLinkType.getName()).isEqualTo(NAME2);
      assertThat(storedLinkType.getAttributes()).isEqualTo(ATTRIBUTES);
      assertThat(storedLinkType.getCollectionIds()).containsOnlyElementsOf(Arrays.asList(collectionIds.get(1), collectionIds.get(2)));
   }

   @Test
   public void testDeleteLinkType() {
      LinkType created = linkTypeDao.createLinkType(prepareLinkType());
      assertThat(created.getId()).isNotNull();

      Response response = client.target(LINK_TYPES_URL).path(created.getId())
                                .request(MediaType.APPLICATION_JSON)
                                .buildDelete().invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
      assertThat(response.getLinks()).extracting(Link::getUri).containsOnly(UriBuilder.fromUri(LINK_TYPES_URL).build());

      assertThatThrownBy(() -> linkTypeDao.getLinkType(created.getId()))
            .isInstanceOf(StorageException.class);
   }

   @Test
   public void testGetLinkTypesByCollectionIds() {
      String id1 = linkTypeDao.createLinkType(prepareLinkType()).getId();

      LinkType linkType2 = prepareLinkType();
      linkType2.setCollectionIds(Arrays.asList(collectionIds.get(1), collectionIds.get(2)));
      linkTypeDao.createLinkType(linkType2);

      LinkType linkType3 = prepareLinkType();
      linkType3.setCollectionIds(Arrays.asList(collectionIds.get(1), collectionIds.get(0)));
      String id3 = linkTypeDao.createLinkType(linkType3).getId();

      LinkType linkType4 = prepareLinkType();
      linkType4.setCollectionIds(Arrays.asList(collectionIds.get(0), collectionIds.get(2)));
      String id4 = linkTypeDao.createLinkType(linkType4).getId();

      JsonQuery jsonQuery = new JsonQuery(Collections.singleton(collectionIds.get(0)), null, null);
      Entity entity = Entity.json(jsonQuery);
      Response response = client.target(LINK_TYPES_URL).path("search")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<LinkType> linkTypes = response.readEntity(new GenericType<List<LinkType>>() {
      });
      assertThat(linkTypes).extracting("id").containsOnlyElementsOf(Arrays.asList(id1, id3, id4));

   }

   @Test
   public void testGetLinkTypesByIds() {
      String id1 = linkTypeDao.createLinkType(prepareLinkType()).getId();

      LinkType linkType2 = prepareLinkType();
      linkType2.setCollectionIds(Arrays.asList(collectionIds.get(1), collectionIds.get(2)));
      linkTypeDao.createLinkType(linkType2);

      LinkType linkType3 = prepareLinkType();
      linkType3.setCollectionIds(Arrays.asList(collectionIds.get(1), collectionIds.get(0)));
      String id3 = linkTypeDao.createLinkType(linkType3).getId();

      LinkType linkType4 = prepareLinkType();
      linkType4.setCollectionIds(Arrays.asList(collectionIds.get(0), collectionIds.get(2)));
      linkTypeDao.createLinkType(linkType4);

      JsonQuery jsonQuery = new JsonQuery(null, new HashSet<>(Arrays.asList(id1, id3)), null);
      Entity entity = Entity.json(jsonQuery);
      Response response = client.target(LINK_TYPES_URL).path("search")
                                .request(MediaType.APPLICATION_JSON)
                                .buildPost(entity).invoke();

      assertThat(response).isNotNull();
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

      List<LinkType> linkTypes = response.readEntity(new GenericType<List<LinkType>>() {
      });
      assertThat(linkTypes).extracting("id").containsOnlyElementsOf(Arrays.asList(id1, id3));

   }

   private LinkType prepareLinkType() {
      return new LinkType(null, NAME, Arrays.asList(collectionIds.get(0), collectionIds.get(1)), ATTRIBUTES);
   }
}
