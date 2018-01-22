package io.lumeer.storage.mongodb.dao.project;

import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;

import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.mongodb.codecs.LinkInstanceCodec;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MongoLinkInstanceDao extends ProjectScopedDao implements LinkInstanceDao {

   private static final String PREFIX = "linkinstances_p-";

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
         return linkInstance;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create link instance: " + linkInstance, ex);
      }
   }

   @Override
   public LinkInstance updateLinkInstance(final String id, final LinkInstance linkInstance) {
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER);
      try{
         LinkInstance updatedLinkInstance = databaseCollection().findOneAndReplace(idFilter(id), linkInstance, options);
         if(updatedLinkInstance == null){
            throw new StorageException("Link instance '" + id + "' has not been updated.");
         }
         return updatedLinkInstance;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update link instance: " + linkInstance, ex);
      }
   }

   @Override
   public void deleteLinkInstance(final String id) {
      DeleteResult result = databaseCollection().deleteOne(idFilter(id));
      if(result.getDeletedCount() != 1){
         throw new StorageException("Link instance '" + id + "' has not been deleted.");
      }
   }

   @Override
   public void deleteLinkInstances(final SearchQuery query) {
      databaseCollection().deleteMany(linkInstancesFilter(query));
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
   public List<LinkInstance> getLinkInstances(final SearchQuery query) {
      return databaseCollection().find(Filters.and(linkInstancesFilter(query))).into(new ArrayList<>());
   }

   private Bson linkInstancesFilter(final SearchQuery query){
      List<Bson> filters = new ArrayList<>();
      if (query.isLinkTypeIdsQuery()) {
         filters.add(Filters.in(LinkInstanceCodec.LINK_TYPE_ID, query.getLinkTypeIds()));
      }
      if (query.isDocumentIdsQuery()) {
         filters.add(Filters.in(LinkInstanceCodec.DOCUMENTS_IDS, query.getDocumentIds()));
      }
      return Filters.and(filters);
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
