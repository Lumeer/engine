package io.lumeer.storage.mongodb.dao.project;

import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;

import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.SearchQuery;

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
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MongoLinkTypeDao extends ProjectScopedDao implements LinkTypeDao {

   private static final String PREFIX = "linktypes_p-";

   @Override
   public void createLinkTypeRepository(Project project) {
      database.createCollection(databaseCollectionName(project));

      MongoCollection<LinkType> projectCollection = databaseCollection();
      projectCollection.createIndex(Indexes.ascending(LinkType.NAME), new IndexOptions().unique(false));
   }

   @Override
   public void deleteLinkTypeRepository(Project project) {
      database.getCollection(databaseCollectionName(project)).drop();
   }

   @Override
   public LinkType createLinkType(final LinkType linkType) {
      try {
         databaseCollection().insertOne(linkType);
         return linkType;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create link type: " + linkType, ex);
      }
   }

   @Override
   public LinkType updateLinkType(final String id, final LinkType linkType) {
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(false);
      try {
         LinkType updatedLinkType = databaseCollection().findOneAndReplace(idFilter(id), linkType, options);
         if (updatedLinkType == null) {
            throw new StorageException("Link type '" + id + "' has not been updated.");
         }
         return updatedLinkType;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update link type: " + linkType, ex);
      }
   }

   @Override
   public void deleteLinkType(final String id) {
      DeleteResult result = databaseCollection().deleteOne(idFilter(id));
      if (result.getDeletedCount() != 1) {
         throw new StorageException("Link type '" + id + "' has not been deleted.");
      }
   }

   @Override
   public void deleteLinkTypes(final SearchQuery query) {
      databaseCollection().deleteMany(linkTypesFilter(query));
   }

   @Override
   public LinkType getLinkType(final String id) {
      LinkType linkType = databaseCollection().find(idFilter(id)).first();
      if (linkType == null) {
         throw new StorageException("Cannot find link type: " + id);
      }
      return linkType;
   }

   @Override
   public List<LinkType> getLinkTypes(final SearchQuery query) {
      return databaseCollection().find(linkTypesFilter(query)).into(new ArrayList<>());
   }

   private Bson linkTypesFilter(final SearchQuery query) {
      List<Bson> filters = new ArrayList<>();
      if (query.isLinkTypeIdsQuery()) {
         List<ObjectId> ids = query.getLinkTypeIds().stream().filter(ObjectId::isValid).map(ObjectId::new).collect(Collectors.toList());
         if (!ids.isEmpty()) {
            filters.add(Filters.in(LinkType.ID, ids));
         }
      }
      if (query.isCollectionCodesQuery()) {
         filters.add(Filters.in(LinkType.COLLECTION_IDS, query.getCollectionCodes()));
      }
      return filters.size() > 0 ? Filters.and(filters) : new Document();
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

   MongoCollection<LinkType> databaseCollection() {
      return database.getCollection(databaseCollectionName(), LinkType.class);
   }
}
