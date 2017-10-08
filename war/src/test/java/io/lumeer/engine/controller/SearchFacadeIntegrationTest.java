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

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.annotation.UserDataStorage;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.Query;
import io.lumeer.engine.api.dto.Collection;
import io.lumeer.engine.api.exception.UserCollectionAlreadyExistsException;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import javax.inject.Inject;

/**
 * @author <a href="kubedo8@gmail.com">Jakub Rodák</a>
 */
@RunWith(Arquillian.class)
public class SearchFacadeIntegrationTest extends IntegrationTestBase {

   private final String COLLECTION_SEARCH = "collectionSearch";
   private final String COLLECTION_SEARCH_RAW = "collectionSearchRaw";
   private final String COLLECTION_QUERY = "collectionQuery";

   @Inject
   private SearchFacade searchFacade;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   @UserDataStorage
   private DataStorage dataStorage;

   @Inject
   private CollectionFacade collectionFacade;

   @Test
   public void testSearch() throws Exception {
      String code = setUpCollection(COLLECTION_SEARCH);

      for (int i = 0; i < 20; i++) {
         DataDocument insertedDocument = new DataDocument();
         documentFacade.createDocument(code, insertedDocument);
      }

      List<DataDocument> searchDocuments = searchFacade.search(code, null, null, 5, 5);

      assertThat(searchDocuments).hasSize(5);
   }

   @Test
   public void testRawSearch() throws Exception {
      String code = setUpCollection(COLLECTION_SEARCH_RAW);

      for (int i = 0; i < 20; i++) {
         DataDocument insertedDocument = new DataDocument();
         documentFacade.createDocument(code, insertedDocument);
      }

      String query = "{find: \"" + code + "\"}";

      List<DataDocument> searchDocuments = searchFacade.search(query);

      // run() method returns 101 entries due to it is a default value of "batchSize" query key
      assertThat(searchDocuments).hasSize(20);
   }

   @Test
   public void testQuery() throws Exception {
      setUpCollection(COLLECTION_QUERY);

      DataDocument d1 = new DataDocument();

      //dataStorage.createDocument(COLLECTION_QUERY);

      final Query q = new Query();
   }

   private String setUpCollection(final String collection) throws UserCollectionAlreadyExistsException {
      String code = collectionMetadataFacade.getCollectionCodeFromName(collection);
      if(code != null) {
         dataStorage.dropCollection(code);
      }
      return collectionFacade.createCollection(new Collection(collection));
   }

}
