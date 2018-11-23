package io.lumeer.storage.mongodb.dao.project;

import static io.lumeer.storage.mongodb.util.MongoFilters.idFilter;

import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.engine.api.event.CreateLinkType;
import io.lumeer.engine.api.event.RemoveLinkType;
import io.lumeer.engine.api.event.UpdateLinkType;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.api.exception.StorageException;
import io.lumeer.storage.api.query.SearchQuery;
import io.lumeer.storage.api.query.SuggestionQuery;
import io.lumeer.storage.mongodb.codecs.LinkTypeCodec;

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
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@RequestScoped
public class MongoLinkTypeDao extends ProjectScopedDao implements LinkTypeDao {

   private static final String PREFIX = "linktypes_p-";

   @Inject
   private Event<CreateLinkType> createLinkTypeEvent;

   @Inject
   private Event<UpdateLinkType> updateLinkTypeEvent;

   @Inject
   private Event<RemoveLinkType> removeLinkTypeEvent;

   @Override
   public void createLinkTypeRepository(Project project) {
      database.createCollection(databaseCollectionName(project));

      MongoCollection<Document> projectCollection = database.getCollection(databaseCollectionName(project));
      projectCollection.createIndex(Indexes.ascending(LinkTypeCodec.NAME), new IndexOptions().unique(false));
      projectCollection.createIndex(Indexes.ascending(LinkTypeCodec.COLLECTION_IDS));
   }

   @Override
   public void deleteLinkTypeRepository(Project project) {
      database.getCollection(databaseCollectionName(project)).drop();
   }

   @Override
   public LinkType createLinkType(final LinkType linkType) {
      try {
         databaseCollection().insertOne(linkType);
         if (createLinkTypeEvent != null) {
            createLinkTypeEvent.fire(new CreateLinkType(linkType));
         }
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
         if (updateLinkTypeEvent != null) {
            updateLinkTypeEvent.fire(new UpdateLinkType(updatedLinkType));
         }
         return updatedLinkType;
      } catch (MongoException ex) {
         throw new StorageException("Cannot update link type: " + linkType, ex);
      }
   }

   @Override
   public void deleteLinkType(final String id) {
      LinkType linkType = databaseCollection().findOneAndDelete(idFilter(id));
      if (linkType == null) {
         throw new StorageException("Link type '" + id + "' has not been deleted.");
      }
      if (removeLinkTypeEvent != null) {
         removeLinkTypeEvent.fire(new RemoveLinkType(linkType));
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

   @Override
   public List<LinkType> getLinkTypes(final SuggestionQuery query) {
      FindIterable<LinkType> findIterable = databaseCollection().find(linkTypesSuggestionFilter(query));
      addPaginationToQuery(findIterable, query);
      return findIterable.into(new ArrayList<>());
   }

   @Override
   public List<LinkType> getLinkTypesByCollectionId(final String collectionId) {
      FindIterable<LinkType> findIterable = databaseCollection().find(
            Filters.elemMatch(LinkTypeCodec.COLLECTION_IDS, Filters.eq(collectionId))
      );
      return findIterable.into(new ArrayList<>());
   }

   private Bson linkTypesSuggestionFilter(SuggestionQuery query) {
      return Filters.regex(LinkTypeCodec.NAME, Pattern.compile(query.getText(), Pattern.CASE_INSENSITIVE));
   }

   private Bson linkTypesFilter(final SearchQuery query) {
      List<Bson> filters = new ArrayList<>();
      if (query.isLinkTypeIdsQuery()) {
         List<ObjectId> ids = query.getLinkTypeIds().stream().filter(ObjectId::isValid).map(ObjectId::new).collect(Collectors.toList());
         if (!ids.isEmpty()) {
            filters.add(Filters.in(LinkTypeCodec.ID, ids));
         }
      }
      if (query.isCollectionIdsQuery()) {
         filters.add(Filters.in(LinkTypeCodec.COLLECTION_IDS, query.getCollectionIds()));
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
