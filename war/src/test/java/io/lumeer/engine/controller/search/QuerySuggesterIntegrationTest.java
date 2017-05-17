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
package io.lumeer.engine.controller.search;

import static org.assertj.core.api.Assertions.assertThat;

import io.lumeer.engine.IntegrationTestBase;
import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.engine.controller.CollectionMetadataFacade;
import io.lumeer.engine.controller.DocumentFacade;
import io.lumeer.engine.controller.LinkingFacade;
import io.lumeer.engine.controller.ViewFacade;

import org.assertj.core.api.SoftAssertions;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import javax.inject.Inject;

@RunWith(Arquillian.class)
public class QuerySuggesterIntegrationTest extends IntegrationTestBase {

   private static final String ATTRIBUTE_KNEES = "knees";
   private static final String ATTRIBUTE_AGE = "age";
   private static final String ATTRIBUTE_SPECIES = "species";

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

   @Inject
   private ViewFacade viewFacade;

   @Test
   public void testSuggestAllNoMatches() {
      DataDocument suggestions = querySuggester.suggestAll("xyz");
      assertThat(suggestions).containsKeys(SuggestionsDocument.ATTRIBUTES, SuggestionsDocument.COLLECTIONS, SuggestionsDocument.LINKS, SuggestionsDocument.VIEWS);

      List<DataDocument> attributes = suggestions.getArrayList(SuggestionsDocument.ATTRIBUTES, DataDocument.class);
      assertThat(attributes).isEmpty();

      List<DataDocument> collections = suggestions.getArrayList(SuggestionsDocument.COLLECTIONS, DataDocument.class);
      assertThat(collections).isEmpty();

      List<DataDocument> links = suggestions.getArrayList(SuggestionsDocument.LINKS, DataDocument.class);
      assertThat(links).isEmpty();

      List<DataDocument> views = suggestions.getArrayList(SuggestionsDocument.VIEWS, DataDocument.class);
      assertThat(views).isEmpty();
   }

   @Test
   public void testSuggestAll() throws Exception {
      collectionFacade.createCollection(COLLECTION_TREES);
      collectionMetadataFacade.addOrIncrementAttribute(collectionMetadataFacade.getInternalCollectionName(COLLECTION_TREES), ATTRIBUTE_KNEES);

      collectionFacade.createCollection(COLLECTION_DOGS);
      String dogId = documentFacade.createDocument(collectionMetadataFacade.getInternalCollectionName(COLLECTION_DOGS), new DataDocument());
      String treeId = documentFacade.createDocument(collectionMetadataFacade.getInternalCollectionName(COLLECTION_TREES), new DataDocument());
      linkingFacade.createLinkInstanceBetweenDocuments(COLLECTION_DOGS, dogId, COLLECTION_TREES, treeId, null, LINK_PEES_ON, LumeerConst.Linking.LinkDirection.FROM);

      viewFacade.createView(VIEW_BEES, null, null, null);

      DataDocument suggestions = querySuggester.suggestAll("ees");
      assertThat(suggestions).containsKeys(SuggestionsDocument.ATTRIBUTES, SuggestionsDocument.COLLECTIONS, SuggestionsDocument.LINKS, SuggestionsDocument.VIEWS);

      List<DataDocument> attributes = suggestions.getArrayList(SuggestionsDocument.ATTRIBUTES, DataDocument.class);
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(attributes).hasSize(1);
      assertions.assertThat(attributes).extracting(a -> a.getString(SuggestionsDocument.ATTRIBUTES_COLLECTION)).containsOnly(COLLECTION_TREES);
      assertions.assertAll();

      List<DataDocument> collections = suggestions.getArrayList(SuggestionsDocument.COLLECTIONS, DataDocument.class);
      assertThat(collections).extracting(c -> c.getString(SuggestionsDocument.COLLECTIONS_NAME)).containsOnly(COLLECTION_TREES);

      List<DataDocument> links = suggestions.getArrayList(SuggestionsDocument.LINKS, DataDocument.class);
      assertions = new SoftAssertions();
      assertions.assertThat(links).hasSize(1);
      assertions.assertThat(links).extracting(a -> a.getString(SuggestionsDocument.LINKS_FROM)).containsOnly(COLLECTION_DOGS);
      assertions.assertThat(links).extracting(a -> a.getString(SuggestionsDocument.LINKS_TO)).containsOnly(COLLECTION_TREES);
      assertions.assertThat(links).extracting(a -> a.getString(SuggestionsDocument.LINKS_ROLE)).containsOnly(LINK_PEES_ON);
      assertions.assertAll();

      List<DataDocument> views = suggestions.getArrayList(SuggestionsDocument.VIEWS, DataDocument.class);
      assertThat(views).extracting(c -> c.getString(SuggestionsDocument.VIEWS_NAME)).containsOnly(VIEW_BEES);
   }

   @Test
   public void testSuggestCollectionsNoMatches() {
      List<DataDocument> collections = querySuggester.suggestCollections("spices");
      assertThat(collections).isEmpty();
   }

   @Test
   public void testSuggestCollectionsPartial() throws Exception {
      collectionFacade.createCollection(COLLECTION_BANANAS);
      collectionFacade.createCollection(COLLECTION_NANS);

      List<DataDocument> collections = querySuggester.suggestCollections("nan");
      assertThat(collections).extracting(c -> c.getString(SuggestionsDocument.COLLECTIONS_NAME)).containsOnly(COLLECTION_BANANAS, COLLECTION_NANS);
   }

   @Test
   public void testSuggestCollectionsComplete() throws Exception {
      collectionFacade.createCollection(COLLECTION_SOFAS);

      List<DataDocument> collections = querySuggester.suggestCollections("sofas");
      assertThat(collections).extracting(c -> c.getString(SuggestionsDocument.COLLECTIONS_NAME)).containsOnly(COLLECTION_SOFAS);
   }

   @Test
   public void testSuggestAttributesNoCollection() {
      List<DataDocument> attributes = querySuggester.suggestAttributes("spices");
      assertThat(attributes).isEmpty();
   }

   @Test
   public void testSuggestAttributesPartialCollection() throws Exception {
      collectionFacade.createCollection(COLLECTION_GUNS);
      collectionFacade.createCollection(COLLECTION_SHOTGUNS);

      List<DataDocument> attributes = querySuggester.suggestAttributes("gun");
      assertThat(attributes).extracting(a -> a.getString(SuggestionsDocument.ATTRIBUTES_COLLECTION)).containsOnly(COLLECTION_GUNS, COLLECTION_SHOTGUNS);
   }

   @Test
   public void testSuggestAttributesCompleteCollection() throws Exception {
      collectionFacade.createCollection(COLLECTION_SUNGLASSES);

      List<DataDocument> attributes = querySuggester.suggestAttributes("sunglasses");
      assertThat(attributes).extracting(a -> a.getString(SuggestionsDocument.ATTRIBUTES_COLLECTION)).containsOnly(COLLECTION_SUNGLASSES);
   }

   @Test
   public void testSuggestAttributesPartialAttribute() throws Exception {
      collectionFacade.createCollection(COLLECTION_ANIMALS);
      collectionMetadataFacade.addOrIncrementAttribute(collectionMetadataFacade.getInternalCollectionName(COLLECTION_ANIMALS), ATTRIBUTE_SPECIES);

      List<DataDocument> attributes = querySuggester.suggestAttributes("animals.spec");
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(attributes).hasSize(1);
      assertions.assertThat(attributes).extracting(a -> a.getString(SuggestionsDocument.ATTRIBUTES_COLLECTION)).containsOnly(COLLECTION_ANIMALS);
      assertions.assertThat(attributes).extracting(a -> a.getString(SuggestionsDocument.ATTRIBUTES_NAME)).containsOnly(ATTRIBUTE_SPECIES);
      assertions.assertAll();
   }

   @Test
   public void testSuggestAttributesCompleteAttribute() throws Exception {
      collectionFacade.createCollection(COLLECTION_PEOPLE);
      collectionMetadataFacade.addOrIncrementAttribute(collectionMetadataFacade.getInternalCollectionName(COLLECTION_PEOPLE), ATTRIBUTE_AGE);
      collectionMetadataFacade.addAttributeConstraint(collectionMetadataFacade.getInternalCollectionName(COLLECTION_PEOPLE), ATTRIBUTE_AGE, "isNumber");

      List<DataDocument> attributes = querySuggester.suggestAttributes("people.age");
      SoftAssertions assertions = new SoftAssertions();
      assertions.assertThat(attributes).hasSize(1);
      assertions.assertThat(attributes.get(0).getString(SuggestionsDocument.ATTRIBUTES_COLLECTION)).isEqualTo(COLLECTION_PEOPLE);
      assertions.assertThat(attributes.get(0).getString(SuggestionsDocument.ATTRIBUTES_NAME)).isEqualTo(ATTRIBUTE_AGE);
      assertions.assertThat(attributes.get(0).getArrayList(SuggestionsDocument.ATTRIBUTES_CONSTRAINTS, String.class)).containsOnly("isNumber");
      assertions.assertAll();
   }

   @Test
   public void testSuggestLinkTypesNoMatches() {
      List<DataDocument> linkTypes = querySuggester.suggestLinkTypes("spices");
      assertThat(linkTypes).isEmpty();
   }

   @Test
   public void testSuggestLinkTypesPartial() throws Exception {
      collectionFacade.createCollection(COLLECTION_TEAMS);
      String teamId = documentFacade.createDocument(collectionMetadataFacade.getInternalCollectionName(COLLECTION_TEAMS), new DataDocument());
      collectionFacade.createCollection(COLLECTION_MATCHES);
      String matchId = documentFacade.createDocument(collectionMetadataFacade.getInternalCollectionName(COLLECTION_MATCHES), new DataDocument());
      linkingFacade.createLinkInstanceBetweenDocuments(COLLECTION_TEAMS, teamId, COLLECTION_MATCHES, matchId, null, LINK_WON, LumeerConst.Linking.LinkDirection.FROM);

      List<DataDocument> linkTypes = querySuggester.suggestLinkTypes("won");
      assertThat(linkTypes).extracting(a -> a.getString(SuggestionsDocument.LINKS_FROM)).containsOnly(COLLECTION_TEAMS);
      assertThat(linkTypes).extracting(a -> a.getString(SuggestionsDocument.LINKS_TO)).containsOnly(COLLECTION_MATCHES);
      assertThat(linkTypes).extracting(a -> a.getString(SuggestionsDocument.LINKS_ROLE)).containsOnly(LINK_WON);
   }

   @Test
   public void testSuggestLinkTypesComplete() throws Exception {
      collectionFacade.createCollection(COLLECTION_COMPANIES);
      String companyId = documentFacade.createDocument(collectionMetadataFacade.getInternalCollectionName(COLLECTION_COMPANIES), new DataDocument());
      collectionFacade.createCollection(COLLECTION_SOFTWARE);
      String softwareId = documentFacade.createDocument(collectionMetadataFacade.getInternalCollectionName(COLLECTION_SOFTWARE), new DataDocument());
      linkingFacade.createLinkInstanceBetweenDocuments(COLLECTION_COMPANIES, companyId, COLLECTION_SOFTWARE, softwareId, null, LINK_DEVELOPED, LumeerConst.Linking.LinkDirection.FROM);
      linkingFacade.createLinkInstanceBetweenDocuments(COLLECTION_COMPANIES, companyId, COLLECTION_SOFTWARE, softwareId, null, LINK_OPEN_SOURCED, LumeerConst.Linking.LinkDirection.FROM);

      List<DataDocument> linkTypes = querySuggester.suggestLinkTypes("ope");
      assertThat(linkTypes).extracting(a -> a.getString(SuggestionsDocument.LINKS_FROM)).containsOnly(COLLECTION_COMPANIES);
      assertThat(linkTypes).extracting(a -> a.getString(SuggestionsDocument.LINKS_TO)).containsOnly(COLLECTION_SOFTWARE);
      assertThat(linkTypes).extracting(a -> a.getString(SuggestionsDocument.LINKS_ROLE)).containsOnly(LINK_DEVELOPED, LINK_OPEN_SOURCED);
   }

   @Test
   public void testSuggestViewsNoMatches() {
      List<DataDocument> views = querySuggester.suggestViews("spices");
      assertThat(views).isEmpty();
   }

   @Test
   public void testSuggestViewsPartial() throws Exception {
      viewFacade.createView(VIEW_FOREIGNERS, null, null, null);
      viewFacade.createView(VIEW_FORMULAS, null, null, null);

      List<DataDocument> views = querySuggester.suggestViews("for");
      assertThat(views).extracting(a -> a.getString(SuggestionsDocument.VIEWS_NAME)).containsOnly(VIEW_FOREIGNERS, VIEW_FORMULAS);
   }

   @Test
   public void testSuggestViewsComplete() throws Exception {
      viewFacade.createView(VIEW_WITCHES, null, null, null);

      List<DataDocument> views = querySuggester.suggestViews("witches");
      assertThat(views).extracting(a -> a.getString(SuggestionsDocument.VIEWS_NAME)).containsOnly(VIEW_WITCHES);
   }

}
