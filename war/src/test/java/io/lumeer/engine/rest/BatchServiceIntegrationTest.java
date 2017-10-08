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
package io.lumeer.engine.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.batch.SplitBatch;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.Query;
import io.lumeer.engine.api.dto.Collection;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.DatabaseInitializer;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;
import io.lumeer.engine.controller.SecurityFacade;
import io.lumeer.engine.controller.UserFacade;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@Ignore
@RunWith(Arquillian.class)
public class BatchServiceIntegrationTest extends IntegrationTestBase {

   private final String TARGET_URI = "http://localhost:8080/";
   private final String COLLECTION_NAME = "Supeř Kolekce +ěščřžý";

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @Inject
   private DatabaseInitializer databaseInitializer;

   @Inject
   private SecurityFacade securityFacade;

   @Inject
   private UserFacade userFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Before
   public void init() {
      // I (Alica) suppose we operate inside some default project which has not been initialized, so we do that here
      databaseInitializer.onProjectCreated(projectFacade.getCurrentProjectId());
   }

   @Test
   public void testSplitBatch() throws Exception {
      long t = System.currentTimeMillis();
      int i = 1;

      System.out.println(i++ + " " + (System.currentTimeMillis() - t));
      t = System.currentTimeMillis();

      final Client client = ClientBuilder.newBuilder().build();
      // the path prefix '/lumeer-engine/' does not work in test classes
      Response response = client.target(TARGET_URI)
                                .path(buildPathPrefix() + "collections/" + COLLECTION_NAME)
                                .request(MediaType.APPLICATION_JSON_TYPE)
                                .buildDelete()
                                .invoke();

      int status = response.getStatus();
      response.close();
      assertThat(status == 404 || status == 204).isTrue();

      System.out.println(i++ + " " + (System.currentTimeMillis() - t));
      t = System.currentTimeMillis();

      // we create and drop some collections in the test, so we need to set up user roles to do that
      securityFacade.addProjectUsersRole(projectFacade.getCurrentProjectCode(), Collections.singletonList(userFacade.getUserEmail()), LumeerConst.Security.ROLE_WRITE);

      response = client.target(TARGET_URI)
                       .path(buildPathPrefix() + "collections")
                       .request()
                       .buildPost(Entity.json(new Collection(COLLECTION_NAME)))
                       .invoke();
      status = response.getStatus();
      String collectionCode = response.readEntity(String.class);
      response.close();
      assertThat(status).isEqualTo(200);

      System.out.println(i++ + " " + (System.currentTimeMillis() - t));
      t = System.currentTimeMillis();

      response = client.target(TARGET_URI)
                       .path(buildPathPrefix() + "collections/" + collectionCode + "/documents/")
                       .request(MediaType.APPLICATION_JSON_TYPE)
                       .accept(MediaType.APPLICATION_JSON_TYPE)
                       .buildPost(Entity.json(new DataDocument("attr1", 5).append("attr2", "hello, how are, you 1")))
                       .invoke();

      final String documentId = response.readEntity(String.class);
      response.close();

      assertThat(documentId).isNotNull();
      assertThat(documentId.length()).isGreaterThan(5);

      System.out.println(i++ + " " + (System.currentTimeMillis() - t));
      t = System.currentTimeMillis();

      response = client.target(TARGET_URI)
                       .path(buildPathPrefix() + "collections/" + collectionCode + "/documents/")
                       .request(MediaType.APPLICATION_JSON_TYPE)
                       .accept(MediaType.APPLICATION_JSON_TYPE)
                       .buildPost(Entity.json(new DataDocument("attr1", 5).append("attr2", "hello, how are, you 2")))
                       .invoke();
      response.close();

      System.out.println(i++ + " " + (System.currentTimeMillis() - t));
      t = System.currentTimeMillis();

      response = client.target(TARGET_URI)
                       .path(buildPathPrefix() + "collections/" + collectionCode + "/documents/")
                       .request(MediaType.APPLICATION_JSON_TYPE)
                       .accept(MediaType.APPLICATION_JSON_TYPE)
                       .buildPost(Entity.json(new DataDocument("attr1", 5).append("attr2", "hello, how are, you 3")))
                       .invoke();
      response.close();

      System.out.println(i++ + " " + (System.currentTimeMillis() - t));
      t = System.currentTimeMillis();

      response = client.target(TARGET_URI)
                       .path(buildPathPrefix() + "batch/split/")
                       .request(MediaType.APPLICATION_JSON_TYPE)
                       .buildPost(Entity.json(new SplitBatch(collectionCode, "attr2", ",", true, Arrays.asList("attr2a", "attr2b", "attr2c", "attr2d"), false)))
                       .invoke();
      response.close();

      System.out.println(i++ + " " + (System.currentTimeMillis() - t));
      t = System.currentTimeMillis();

      response = client.target(TARGET_URI)
                       .path(buildPathPrefix()).path("search/query")
                       .request(MediaType.APPLICATION_JSON_TYPE)
                       .accept(MediaType.APPLICATION_JSON_TYPE)
                       .buildPost(Entity.json(new Query(new DataDocument("attr1", "5"))))
                       .invoke();
      final List<LinkedHashMap> result = response.readEntity(List.class);
      response.close();

      System.out.println(i++ + " " + (System.currentTimeMillis() - t));
      t = System.currentTimeMillis();

      result.forEach(data -> {
         assertThat(data).doesNotContainKey("attr2");
         assertThat(data).containsEntry("attr2a", "hello");
         assertThat(data).containsEntry("attr2b", "how are");
         assertThat(data).doesNotContainKey("attr2d");
      });
   }

   private String buildPathPrefix() {
      return PATH_CONTEXT + "/rest/organizations/" + organizationFacade.getOrganizationCode() + "/projects/" + projectFacade.getCurrentProjectCode() + "/";
   }

}
