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
package io.lumeer.engine.rest;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.Query;
import io.lumeer.engine.api.dto.SearchSuggestion;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.engine.controller.DocumentFacade;
import io.lumeer.engine.controller.OrganizationFacade;
import io.lumeer.engine.controller.ProjectFacade;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:mat.per.vt@gmail.com">Matej Perejda</a>
 */
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
      collectionFacade.createCollection(COLLECTION_BEERS);
      collectionFacade.createCollection(COLLECTION_PEERS);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(buildPathPrefix()).path(SUGGESTION_PATH)
                                .queryParam("text", "eers")
                                .request().buildGet().invoke();

      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

      List suggestions = response.readEntity(List.class);
      assertThat(suggestions).hasSize(4);

      response.close();
      client.close();
   }

   @Test
   public void testSuggestCollectionType() throws Exception {
      collectionFacade.createCollection(COLLECTION_ANTS);
      collectionFacade.createCollection(COLLECTION_PANTS);

      final Client client = ClientBuilder.newBuilder().build();
      Response response = client.target(TARGET_URI).path(buildPathPrefix()).path(SUGGESTION_PATH)
                                .queryParam("text", "ant")
                                .queryParam("type", "collection")
                                .request().buildGet().invoke();

      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

      List<SearchSuggestion> suggestions = response.readEntity(List.class);
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

      String collection = collectionFacade.createCollection(COLLECTION_QUERY_SEARCH);
      createDummyEntries(collection);

      final Set<String> collections = new HashSet<>();
      collections.add(COLLECTION_QUERY_SEARCH);
      final Query query = new Query(collections, emptyFilters, emptyProjection, emptySorting, limit, null);
      Response response = client.target(TARGET_URI).path(buildPathPrefix()).path(QUERY_PATH).request().buildPost(Entity.entity(query, MediaType.APPLICATION_JSON)).invoke();
      List<DataDocument> matchResult = response.readEntity(ArrayList.class);
      assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
      assertThat(matchResult).hasSize(limit);
      response.close();
      client.close();
   }

   private void createDummyEntries(final String collectionName) throws DbException, InvalidConstraintException {
      for (int i = 0; i < 10; i++) {
         documentFacade.createDocument(collectionName, new DataDocument("dummyAttribute", i));
      }
   }

   private void setUpCollections(final String collectionName) throws DbException {
      if (dataStorage.hasCollection(collectionName)) {
         collectionFacade.dropCollection(collectionName);
      }
   }

   private String buildPathPrefix() {
      return PATH_CONTEXT + "/rest/" + organizationFacade.getOrganizationCode() + "/" + projectFacade.getCurrentProjectCode() + "/";
   }

   private String getInternalName(final String collectionOriginalName) {
      return "collection." + collectionOriginalName.toLowerCase() + "_0";
   }

}
