package io.lumeer.storage.mongodb.dao.project;

import static io.lumeer.storage.mongodb.util.MongoFilters.codeFilter;
import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;
import static io.lumeer.storage.mongodb.util.MongoFilters.permissionsFilter;

import io.lumeer.api.dto.JsonCollection;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.Resource;
import io.lumeer.api.model.ResourceType;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.DatabaseQuery;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SuggestionQuery;
import io.lumeer.storage.mongodb.MongoUtils;
import io.lumeer.storage.mongodb.codecs.AttributeCodec;
import io.lumeer.storage.mongodb.codecs.CollectionCodec;
import io.lumeer.storage.mongodb.util.MongoFilters;

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
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
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MongoCollectionDao extends ProjectScopedDao implements CollectionDao {

   private static final String PREFIX = "collections_p-";

   @Override
   public void createCollectionsRepository(final Project project) {
      database.createCollection(databaseCollectionName(project));

      MongoCollection<Document> projectCollection = database.getCollection(databaseCollectionName(project));
      projectCollection.createIndex(Indexes.ascending(CollectionCodec.NAME), new IndexOptions().unique(true));
      projectCollection.createIndex(Indexes.ascending(CollectionCodec.CODE), new IndexOptions().unique(true));
      projectCollection.createIndex(Indexes.ascending(CollectionCodec.ATTRIBUTES, AttributeCodec.NAME), new IndexOptions().unique(false));
      projectCollection.createIndex(Indexes.text(CollectionCodec.NAME));
   }

   @Override
   public void deleteCollectionsRepository(final Project project) {
      database.getCollection(databaseCollectionName(project)).drop();
   }

   @Override
   public Collection createCollection(final Collection collection) {
      try {
         JsonCollection jsonCollection = new JsonCollection(collection);
         databaseCollection().insertOne(jsonCollection);
         return jsonCollection;
      } catch (MongoException ex) {
         throw new StorageException("Cannot create collection: " + collection, ex);
      }
   }

   @Override
   public Collection updateCollection(final String id, final Collection collection) {
      JsonCollection jsonCollection = new JsonCollection(collection);
      FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER);

      try {
         JsonCollection updatedCollection = databaseCollection().findOneAndReplace(idFilter(id), jsonCollection, options);
         if (updatedCollection == null) {
            throw new StorageException("Collection '" + id + "' has not been updated.");
         }
         return updatedCollection;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update collection: " + collection, ex);
      }
   }

   @Override
   public void deleteCollection(final String id) {
      DeleteResult result = databaseCollection().deleteOne(idFilter(id));
      if (result.getDeletedCount() != 1) {
         throw new StorageException("Collection '" + id + "' has not been deleted.");
      }
   }

   @Override
   public Collection getCollectionByCode(final String code) {
      return getCollectionByFilter(codeFilter(code));
   }

   @Override
   public Collection getCollectionById(final String id) {
      return getCollectionByFilter(idFilter(id));
   }

   private Collection getCollectionByFilter(Bson filter) {
      MongoCursor<JsonCollection> mongoCursor = databaseCollection().find(filter).iterator();
      if (!mongoCursor.hasNext()) {
         throw new ResourceNotFoundException(ResourceType.COLLECTION);
      }
      return mongoCursor.next();
   }

   @Override
   public List<Collection> getCollectionsByIds(final java.util.Collection<String> ids) {
      Bson filter = Filters.in(CollectionCodec.ID, ids.stream().map(ObjectId::new).collect(Collectors.toSet()));
      return databaseCollection().find(filter).into(new ArrayList<>());
   }

   @Override
   public List<Collection> getCollections(final SearchQuery query) {
      Bson filter = collectionSearchQuery(query);
      return searchCollectionsByFilter(filter, query);
   }

   private Bson collectionSearchQuery(SearchQuery query) {
      return query.isBasicQuery() ? createSimpleSearchQuery(query) : createAdvancedSearchQuery(query);
   }

   private Bson createSimpleSearchQuery(SearchQuery query) {
      return permissionsFilter(query);
   }

   private Bson createAdvancedSearchQuery(SearchQuery query) {
      List<Bson> filters = new ArrayList<>();
      filters.add(MongoFilters.permissionsFilter(query));

      if (query.isFulltextQuery()) {
         Bson codeFulltext = Filters.regex(CollectionCodec.CODE, Pattern.compile(query.getFulltext(), Pattern.CASE_INSENSITIVE));
         Bson nameFulltext = Filters.regex(CollectionCodec.NAME, Pattern.compile(query.getFulltext(), Pattern.CASE_INSENSITIVE));
         filters.add(Filters.or(codeFulltext, nameFulltext));
      }

      if (query.isCollectionIdsQuery()) {
         Set<ObjectId> collectionIds = query.getCollectionIds().stream().map(ObjectId::new).collect(Collectors.toSet());
         filters.add(Filters.in(CollectionCodec.ID, collectionIds));
      }

      return Filters.and(filters);
   }

   private List<Collection> searchCollectionsByFilter(Bson filter, DatabaseQuery query) {
      FindIterable<JsonCollection> iterable = databaseCollection().find(filter);
      addPaginationToQuery(iterable, query);

      return iterable.into(new ArrayList<>());
   }

   @Override
   public List<Collection> getCollections(final SuggestionQuery query) {
      Bson filter = collectionSuggestionQuery(query);
      return searchCollectionsByFilter(filter, query);
   }

   private Bson collectionSuggestionQuery(SuggestionQuery query) {
      List<Bson> filters = new ArrayList<>();
      filters.add(MongoFilters.permissionsFilter(query));
      filters.add(Filters.regex(CollectionCodec.NAME, Pattern.compile(query.getText(), Pattern.CASE_INSENSITIVE)));
      return Filters.and(filters);
   }

   @Override
   public List<Collection> getCollectionsByAttributes(final SuggestionQuery query) {
      Bson filter = attributeSuggestionQuery(query);
      return searchCollectionsByFilter(filter, query);
   }

   private Bson attributeSuggestionQuery(SuggestionQuery query) {
      List<Bson> filters = new ArrayList<>();
      filters.add(MongoFilters.permissionsFilter(query));
      filters.add(Filters.regex(MongoUtils.concatParams(CollectionCodec.ATTRIBUTES, AttributeCodec.NAME), Pattern.compile(query.getText(), Pattern.CASE_INSENSITIVE)));
      return Filters.and(filters);
   }

   @Override
   public long getCollectionsCount() {
      return databaseCollection().find()
                                 .into(new ArrayList<>())
                                 .size();
   }

   @Override
   public Set<String> getAllCollectionCodes() {
      return databaseCollection().find()
                                 .into(new ArrayList<>())
                                 .stream().map(Resource::getCode)
                                 .collect(Collectors.toSet());
   }

   @Override
   public Set<String> getAllCollectionNames() {
      return databaseCollection().find()
                                 .into(new ArrayList<>())
                                 .stream().map(Resource::getName)
                                 .collect(Collectors.toSet());
   }

   @Override
   public Set<String> getAllCollectionIds() {
      return databaseCollection().find()
                                 .into(new ArrayList<>())
                                 .stream().map(Resource::getId)
                                 .collect(Collectors.toSet());
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

   MongoCollection<JsonCollection> databaseCollection() {
      return database.getCollection(databaseCollectionName(), JsonCollection.class);
   }
}
