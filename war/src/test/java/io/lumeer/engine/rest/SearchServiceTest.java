package io.lumeer.engine.rest;

import io.lumeer.engine.api.constraint.InvalidConstraintException;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.Query;
import io.lumeer.engine.api.exception.DbException;
import io.lumeer.engine.controller.CollectionFacade;
import io.lumeer.engine.controller.DocumentFacade;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
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
public class SearchServiceTest {

   @Deployment
   public static Archive<?> createTestArchive() {
      return ShrinkWrap.create(WebArchive.class, "SearchServiceTest.war")
                       .addPackages(true, "io.lumeer", "org.bson", "com.mongodb", "io.netty")
                       .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                       .addAsWebInfResource("jboss-deployment-structure.xml")
                       .addAsResource("defaults-ci.properties")
                       .addAsResource("defaults-dev.properties");
   }

   private final String TARGET_URI = "http://localhost:8080";
   private final String PATH_PREFIX = "SearchServiceTest/rest/";

   private final String COLLECTION_QUERY_SEARCH = "SearchServiceCollectionRunQuery";

   @Inject
   private CollectionFacade collectionFacade;

   @Inject
   private DataStorage dataStorage;

   @Inject
   private DocumentFacade documentFacade;

   @Test
   public void testRunQuery() throws Exception {
      setUpCollections(COLLECTION_QUERY_SEARCH);
      final Client client = ClientBuilder.newBuilder().build();
      final int limit = 5;
      final DataDocument emptyFilters = new DataDocument();
      final DataDocument emptyProjection = new DataDocument();
      final DataDocument emptySorting = new DataDocument();

      collectionFacade.createCollection(COLLECTION_QUERY_SEARCH);
      createDummyEntries(COLLECTION_QUERY_SEARCH);

      final Set<String> collections = new HashSet<>();
      collections.add(COLLECTION_QUERY_SEARCH);
      final Query query = new Query(collections, emptyFilters, emptyProjection, emptySorting, limit, null);
      Response response = client.target(TARGET_URI).path(PATH_PREFIX + "query/").request().buildPost(Entity.entity(query, MediaType.APPLICATION_JSON)).invoke();
      List<DataDocument> matchResult = response.readEntity(ArrayList.class);
      Assert.assertTrue(response.getStatus() == Response.Status.OK.getStatusCode() && matchResult.size() == limit);

      response.close();
      client.close();
   }

   private void createDummyEntries(final String collectionName) throws DbException, InvalidConstraintException {
      for (int i = 0; i < 10; i++) {
         documentFacade.createDocument(getInternalName(collectionName), new DataDocument("dummyAttribute", i));
      }
   }

   private void setUpCollections(final String collectionName) throws DbException {
      if (dataStorage.hasCollection(getInternalName(collectionName))) {
         collectionFacade.dropCollection(getInternalName(collectionName));
      }
   }

   private String getInternalName(final String collectionOriginalName) {
      return "collection." + collectionOriginalName.toLowerCase() + "_0";
   }

}
