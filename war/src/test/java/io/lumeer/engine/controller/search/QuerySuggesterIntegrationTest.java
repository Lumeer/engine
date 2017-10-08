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
package io.lumeer.engine.controller.search;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.dto.Collection;
import io.lumeer.engine.api.dto.SearchSuggestion;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.DocumentFacade;
import io.lumeer.engine.controller.LinkingFacade;

import org.assertj.core.api.SoftAssertions;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class QuerySuggesterIntegrationTest extends IntegrationTestBase {

   private static final String ATTRIBUTE_KNEES = "knees";
   private static final String ATTRIBUTE_AGE = "age";
   private static final String ATTRIBUTE_SPECIES = "species";
   private static final String ATTRIBUTE_GUN = "gun";

   private static final String COLLECTION_BANANAS = "bananas";
   private static final String COLLECTION_NANS = "nans";
   private static final String COLLECTION_TREES = "trees";
   private static final String COLLECTION_SOFAS = "sofas";
   private static final String COLLECTION_DOGS = "dogs";
   private static final String COLLECTION_GUNS = "guns";
   private static final String COLLECTION_SHOTGUNS = "shotguns";
   private static final String COLLECTION_SUNGLASSES = "sunglasses";
   private static final String COLLECTION_PEOPLE = "people";
   private static final String COLLECTION_ANIMALS = "animals";
   private static final String COLLECTION_TEAMS = "teams";
   private static final String COLLECTION_MATCHES = "matches";
   private static final String COLLECTION_COMPANIES = "companies";
   private static final String COLLECTION_SOFTWARE = "software";

   private static final String LINK_PEES_ON = "peesOn";
   private static final String LINK_WON = "won";
   private static final String LINK_DEVELOPED = "developed";
   private static final String LINK_OPEN_SOURCED = "open-sourced";

   private static final String VIEW_BEES = "bees";
   private static final String VIEW_WITCHES = "witches";
   private static final String VIEW_FORMULAS = "formulas";
   private static final String VIEW_FOREIGNERS = "foreigners";

   @Inject
   private QuerySuggester querySuggester;

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private CollectionMetadataFacade collectionMetadataFacade;

   @Inject
   private DocumentFacade documentFacade;

   @Inject
   private LinkingFacade linkingFacade;

   @Test
   public void testSuggestAllNoMatches() {
      List<SearchSuggestion> suggestions = querySuggester.suggestAll("xyz", QuerySuggester.SUGGESTIONS_LIMIT);
      assertThat(suggestions).isEmpty();
   }

   @Test
   public void testSuggestAll() throws Exception {
      String collectionTreesCode = collectionFacade.createCollection(new Collection(COLLECTION_TREES));

      collectionMetadataFacade.addOrIncrementAttribute(collectionTreesCode, ATTRIBUTE_KNEES);

      String collectionDogsCode = collectionFacade.createCollection(new Collection(COLLECTION_DOGS));
      String dogId = documentFacade.createDocument(collectionDogsCode, new DataDocument());
      String treeId = documentFacade.createDocument(collectionDogsCode, new DataDocument());
      linkingFacade.createLinkInstanceBetweenDocuments(collectionDogsCode, dogId, collectionTreesCode, treeId, null, LINK_PEES_ON, LumeerConst.Linking.LinkDirection.FROM);

//      viewFacade.createView(VIEW_BEES, null, null, null);

      List<SearchSuggestion> suggestions = querySuggester.suggestAll("ees", QuerySuggester.SUGGESTIONS_LIMIT);
      assertThat(suggestions).containsOnly(
            new SearchSuggestion(SearchSuggestion.TYPE_ATTRIBUTE, COLLECTION_TREES + "." + ATTRIBUTE_KNEES),
            new SearchSuggestion(SearchSuggestion.TYPE_COLLECTION, COLLECTION_TREES)
//            new SearchSuggestion(SearchSuggestion.TYPE_VIEW, VIEW_BEES)
      );
   }

   @Test
   public void testSuggestCollectionsNoMatches() {
      List<SearchSuggestion> collections = querySuggester.suggestCollections("spices", QuerySuggester.SUGGESTIONS_LIMIT);
      assertThat(collections).isEmpty();
   }

   @Test
   public void testSuggestCollectionsPartial() throws Exception {
      collectionFacade.createCollection(new Collection(COLLECTION_BANANAS));
      collectionFacade.createCollection(new Collection(COLLECTION_NANS));

      List<SearchSuggestion> collections = querySuggester.suggestCollections("nan", QuerySuggester.SUGGESTIONS_LIMIT);
      assertThat(collections).extracting(SearchSuggestion::getText).containsOnly(COLLECTION_BANANAS, COLLECTION_NANS);
   }

   @Test
   public void testSuggestCollectionsComplete() throws Exception {
      collectionFacade.createCollection(new Collection(COLLECTION_SOFAS));

      List<SearchSuggestion> collections = querySuggester.suggestCollections("sofas", QuerySuggester.SUGGESTIONS_LIMIT);
      assertThat(collections).extracting(SearchSuggestion::getText).containsOnly(COLLECTION_SOFAS);
   }

   @Test
   public void testSuggestAttributesNoCollection() throws Exception {
      String collectionGunsCode = collectionFacade.createCollection(new Collection(COLLECTION_GUNS));
      collectionMetadataFacade.addOrIncrementAttribute(collectionGunsCode, ATTRIBUTE_GUN);
      String collectionMatchesCode = collectionFacade.createCollection(new Collection(COLLECTION_MATCHES));
      collectionMetadataFacade.addOrIncrementAttribute(collectionMatchesCode, ATTRIBUTE_GUN);
      String collectionSoftwareCode = collectionFacade.createCollection(new Collection(COLLECTION_SOFTWARE));
      collectionMetadataFacade.addOrIncrementAttribute(collectionSoftwareCode, ATTRIBUTE_GUN);

      List<SearchSuggestion> attributes = querySuggester.suggestAttributes("un", QuerySuggester.SUGGESTIONS_LIMIT);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(attributes).hasSize(3);
      assertions.assertThat(attributes).extracting(SearchSuggestion::getText)
                .containsOnly(COLLECTION_GUNS + "." + ATTRIBUTE_GUN, COLLECTION_MATCHES + "." + ATTRIBUTE_GUN, COLLECTION_SOFTWARE + "." + ATTRIBUTE_GUN);
      assertions.assertAll();
   }

   @Test
   public void testSuggestAttributesPartialAttribute() throws Exception {
      String collectionCode = collectionFacade.createCollection(new Collection(COLLECTION_ANIMALS));
      collectionMetadataFacade.addOrIncrementAttribute(collectionCode, ATTRIBUTE_SPECIES);

      List<SearchSuggestion> attributes = querySuggester.suggestAttributes("animals.spec", QuerySuggester.SUGGESTIONS_LIMIT);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(attributes).hasSize(1);
      assertions.assertThat(attributes).extracting(SearchSuggestion::getText).containsOnly(COLLECTION_ANIMALS + "." + ATTRIBUTE_SPECIES);
      assertions.assertAll();
   }

   @Test
   public void testSuggestAttributesCompleteAttribute() throws Exception {
      String collectionode = collectionFacade.createCollection(new Collection(COLLECTION_PEOPLE));
      collectionMetadataFacade.addOrIncrementAttribute(collectionode, ATTRIBUTE_AGE);
      collectionMetadataFacade.addAttributeConstraint(collectionode, ATTRIBUTE_AGE, "isNumber");

      List<SearchSuggestion> attributes = querySuggester.suggestAttributes("people.age", QuerySuggester.SUGGESTIONS_LIMIT);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(attributes).hasSize(1);
      assertions.assertThat(attributes.get(0).getText()).isEqualTo(COLLECTION_PEOPLE + "." + ATTRIBUTE_AGE);
      assertions.assertThat(attributes.get(0).getConstraints()).containsOnly("isNumber");
      assertions.assertAll();
   }

   @Test
   public void testSuggestAttributesChilds() throws Exception {
      String collectionCode = collectionFacade.createCollection(new Collection(COLLECTION_COMPANIES));
      collectionMetadataFacade.addOrIncrementAttribute(collectionCode, "Lumeer");
      collectionMetadataFacade.addOrIncrementAttribute(collectionCode, "First.Lumeer");
      collectionMetadataFacade.addOrIncrementAttribute(collectionCode, "Second.First.Lumeer");
      collectionMetadataFacade.addOrIncrementAttribute(collectionCode, "a.b.c.d.e.Lumee");

      List<SearchSuggestion> attributes = querySuggester.suggestAttributes("companies.lumee", QuerySuggester.SUGGESTIONS_LIMIT);
      assertThat(attributes).hasSize(4).extracting("text").containsOnly("companies.Lumeer", "companies.First.Lumeer", "companies.Second.First.Lumeer", "companies.a.b.c.d.e.Lumee");

      attributes = querySuggester.suggestAttributes("companies.lumeer", QuerySuggester.SUGGESTIONS_LIMIT);
      assertThat(attributes).hasSize(3).extracting("text").containsOnly("companies.Lumeer", "companies.First.Lumeer", "companies.Second.First.Lumeer");
   }

   @Test
   public void testSuggestLinksNoMatches() {
      List<SearchSuggestion> linkTypes = querySuggester.suggestLinks("spices", QuerySuggester.SUGGESTIONS_LIMIT);
      assertThat(linkTypes).isEmpty();
   }

   @Test
   public void testSuggestViewsNoMatches() {
      List<SearchSuggestion> views = querySuggester.suggestViews("spices", QuerySuggester.SUGGESTIONS_LIMIT);
      assertThat(views).isEmpty();
   }

   @Test
   @Ignore("refactor QuerySuggester using new ViewDao")
   public void testSuggestViewsPartial() throws Exception {
//      viewFacade.createView(VIEW_FOREIGNERS, null, null, null);
//      viewFacade.createView(VIEW_FORMULAS, null, null, null);

      List<SearchSuggestion> views = querySuggester.suggestViews("for", QuerySuggester.SUGGESTIONS_LIMIT);
      assertThat(views).extracting(SearchSuggestion::getText).containsOnly(VIEW_FOREIGNERS, VIEW_FORMULAS);
   }

   @Test
   @Ignore("refactor QuerySuggester using new ViewDao")
   public void testSuggestViewsComplete() throws Exception {
//      viewFacade.createView(VIEW_WITCHES, null, null, null);

      List<SearchSuggestion> views = querySuggester.suggestViews("witches", QuerySuggester.SUGGESTIONS_LIMIT);
      assertThat(views).extracting(SearchSuggestion::getText).containsOnly(VIEW_WITCHES);
   }

}
