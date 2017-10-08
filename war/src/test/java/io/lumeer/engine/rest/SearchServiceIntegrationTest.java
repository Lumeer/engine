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
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.Query;
import io.lumeer.engine.api.dto.Collection;
import io.lumeer.engine.api.dto.SearchSuggestion;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.engine.controller.DocumentFacade;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

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
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
@Ignore
@RunWith(Arquillian.class)
public class SearchServiceIntegrationTest extends IntegrationTestBase {

   private final String TARGET_URI = "http://localhost:8080";

   private static final String QUERY_PATH = "search/query";
   private static final String SUGGESTION_PATH = "search/suggestion";

   private static final String COLLECTION_BEERS = "beers";
   private static final String COLLECTION_PEERS = "peers";
   private static final String COLLECTION_ANTS = "ants";
   private static final String COLLECTION_PANTS = "pants";

   private static final String COLLECTION_QUERY_SEARCH = "SearchServiceCollectionRunQuery";

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private OrganizationFacade organizationFacade;

   @Inject
   private ProjectFacade projectFacade;

   @Test
   public void testSuggestAll() throws Exception {
      collectionFacade.createCollection(new Collection(COLLECTION_BEERS));
      collectionFacade.createCollection(new Collection(COLLECTION_PEERS));

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(buildPathPrefix()).path(SUGGESTION_PATH)
                                .queryParam("text", "eers")
                                .request().buildGet().invoke();

      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

      List suggestions = response.readEntity(List.class);
      assertThat(suggestions).hasSize(2);

      response.close();
      client.close();
   }

   @Test
   public void testSuggestCollectionType() throws Exception {
      collectionFacade.createCollection(new Collection(COLLECTION_ANTS));
      collectionFacade.createCollection(new Collection(COLLECTION_PANTS));

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(buildPathPrefix()).path(SUGGESTION_PATH)
                                .queryParam("text", "ant")
                                .queryParam("type", "collection")
                                .request().buildGet().invoke();

      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

      List<SearchSuggestion> suggestions = response.readEntity(new GenericType<List<SearchSuggestion>>() {
      });
      assertThat(suggestions).hasSize(2);

      response.close();
      client.close();
   }

   @Test
   public void testRunQuery() throws Exception {
      setUpCollections(COLLECTION_QUERY_SEARCH);
      final Client client = ClientBuilder.newBuilder().build();
      final int limit = 5;
      final DataDocument emptyFilters = new DataDocument();
      final DataDocument emptyProjection = new DataDocument();
      final DataDocument emptySorting = new DataDocument();

      String code = collectionFacade.createCollection(new Collection(COLLECTION_QUERY_SEARCH));
      createDummyEntries(code);

      final Set<String> collections = new HashSet<>();
      collections.add(code);
      final Query query = new Query(collections, emptyFilters, emptyProjection, emptySorting, limit, null);
      Response response = client.target(TARGET_URI).path(buildPathPrefix()).path(QUERY_PATH).request().buildPost(Entity.entity(query, MediaType.APPLICATION_JSON)).invoke();
      List<DataDocument> matchResult = response.readEntity(new GenericType<List<DataDocument>>() {
      });
      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(matchResult).hasSize(limit);
      response.close();
      client.close();
   }

   private void createDummyEntries(final String collectionCode) throws DbException, InvalidConstraintException {
      for (int i = 0; i < 10; i++) {
         documentFacade.createDocument(collectionCode, new DataDocument("dummyAttribute", i));
      }
   }

   private void setUpCollections(final String collectionName) throws DbException {
      if (dataStorage.hasCollection(collectionName)) {
         collectionFacade.dropCollection(collectionName);
      }
   }

   private String buildPathPrefix() {
      return PATH_CONTEXT + "/rest/organizations/" + organizationFacade.getOrganizationCode() + "/projects/" + projectFacade.getCurrentProjectCode() + "/";
   }

}
