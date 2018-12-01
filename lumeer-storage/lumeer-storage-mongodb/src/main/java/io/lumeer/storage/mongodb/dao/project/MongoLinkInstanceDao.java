package io.lumeer.storage.mongodb.dao.project;

import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;

import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.engine.api.event.CreateLinkInstance;
import io.lumeer.engine.api.event.RemoveLinkInstance;
import io.lumeer.engine.api.event.UpdateLinkInstance;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SearchQueryStem;
import io.lumeer.storage.mongodb.codecs.LinkInstanceCodec;

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
public class MongoLinkInstanceDao extends ProjectScopedDao implements LinkInstanceDao {

   private static final String PREFIX = "linkinstances_p-";

   @Inject
   private Event<CreateLinkInstance> createLinkInstanceEvent;

   @Inject
   private Event<UpdateLinkInstance> updateLinkInstanceEvent;

   @Inject
   private Event<RemoveLinkInstance> removeLinkInstanceEvent;

   @Override
   public void createLinkInstanceRepository(Project project) {
      database.createCollection(databaseCollectionName(project));

      MongoCollection<Document> projectCollection = database.getCollection(databaseCollectionName(project));
      projectCollection.createIndex(Indexes.ascending(LinkInstanceCodec.LINK_TYPE_ID), new IndexOptions().unique(false));
   }

   @Override
   public void deleteLinkInstanceRepository(Project project) {
      database.getCollection(databaseCollectionName(project)).drop();
   }

   @Override
   public LinkInstance createLinkInstance(final LinkInstance linkInstance) {
      try {
         databaseCollection().insertOne(linkInstance);
         if (createLinkInstanceEvent != null) {
            createLinkInstanceEvent.fire(new CreateLinkInstance(linkInstance));
         }
         return linkInstance;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create link instance: " + linkInstance, ex);
      }
   }

   @Override
   public LinkInstance updateLinkInstance(final String id, final LinkInstance linkInstance) {
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER);
      try {
         LinkInstance updatedLinkInstance = databaseCollection().findOneAndReplace(idFilter(id), linkInstance, options);
         if (updatedLinkInstance == null) {
            throw new StorageException("Link instance '" + id + "' has not been updated.");
         }
         if (updateLinkInstanceEvent != null) {
            updateLinkInstanceEvent.fire(new UpdateLinkInstance(updatedLinkInstance));
         }
         return updatedLinkInstance;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update link instance: " + linkInstance, ex);
      }
   }

   @Override
   public void deleteLinkInstance(final String id) {
      LinkInstance linkInstance = databaseCollection().findOneAndDelete(idFilter(id));
      if(linkInstance == null) {
         throw new StorageException("Link instance '" + id + "' has not been deleted.");
      }
      if (removeLinkInstanceEvent != null) {
         removeLinkInstanceEvent.fire(new RemoveLinkInstance(linkInstance));
      }
   }

   @Override
   public void deleteLinkInstancesByLinkTypesIds(final Set<String> linkTypeIds) {
      Bson filter = Filters.in(LinkInstanceCodec.LINK_TYPE_ID, linkTypeIds);
      databaseCollection().deleteMany(filter);
   }

   @Override
   public void deleteLinkInstancesByDocumentsIds(final Set<String> documentsIds) {
      Bson filter = Filters.in(LinkInstanceCodec.DOCUMENTS_IDS, documentsIds);
      databaseCollection().deleteMany(filter);
   }

   @Override
   public LinkInstance getLinkInstance(final String id) {
      LinkInstance linkInstance = databaseCollection().find(idFilter(id)).first();
      if (linkInstance == null) {
         throw new StorageException("Cannot find link instance: " + id);
      }
      return linkInstance;
   }

   @Override
   public List<LinkInstance> getLinkInstancesByDocumentIds(final Set<String> documentIds) {
      Bson filter = Filters.in(LinkInstanceCodec.DOCUMENTS_IDS, documentIds);
      return databaseCollection().find(filter).into(new ArrayList<>());
   }

   @Override
   public List<LinkInstance> searchLinkInstances(final SearchQuery query) {
      final FindIterable<LinkInstance> linkInstances = databaseCollection().find(linkInstancesFilter(query));
      addPaginationToQuery(linkInstances, query);
      return linkInstances.into(new ArrayList<>());
   }

   private Bson linkInstancesFilter(final SearchQuery query) {
      List<Bson> filters = new ArrayList<>();
      for (SearchQueryStem stem : query.getStems()) {
         List<Bson> stemFilters = new ArrayList<>();
         if (stem.containsLinkTypeIdsQuery()) {
            stemFilters.add(Filters.in(LinkInstanceCodec.LINK_TYPE_ID, stem.getLinkTypeIds()));
         }
         if (stem.containsDocumentIdsQuery()) {
            stemFilters.add(Filters.in(LinkInstanceCodec.DOCUMENTS_IDS, stem.getDocumentIds()));
         }
         if (!stemFilters.isEmpty()) {
            filters.add(Filters.and(stemFilters));
         }
      }
      return filters.size() > 0 ? Filters.or(filters) : new Document();
   }

   private String databaseCollectionName(Project project) {
      return PREFIX + project.getId();
   }

   String databaseCollectionName() {
      if (!getProject().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return databaseCollectionName(getProject().get());
   }

   MongoCollection<LinkInstance> databaseCollection() {
      return database.getCollection(databaseCollectionName(), LinkInstance.class);
   }
}
