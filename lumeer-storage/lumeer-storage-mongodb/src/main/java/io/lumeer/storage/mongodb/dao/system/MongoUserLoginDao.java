/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
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
package io.lumeer.storage.mongodb.dao.system;

import io.lumeer.api.model.UserLoginEvent;
import io.lumeer.storage.api.dao.UserLoginDao;
import io.lumeer.storage.mongodb.codecs.UserLoginEventCodec;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MongoUserLoginDao extends MongoSystemScopedDao implements UserLoginDao {

   String COLLECTION_NAME = "userLogins";

   @PostConstruct
   public void initDb() {
      createLoginRepository();
   }

   @Override
   public void userLoggedIn(final String userId) {
      databaseCollection().insertOne(new UserLoginEvent(userId));
   }

   @Override
   public ZonedDateTime getPreviousLoginDate(final String userId) {
      List<UserLoginEvent> result = databaseCollection()
            .find(Filters.eq(UserLoginEventCodec.USER_ID, userId))
            .sort(Sorts.descending(UserLoginEventCodec.DATE))
            .limit(2).into(new ArrayList<>());
      if (result.size() > 1) {
         return result.get(1).getDate();
      }

      return null;
   }

   @Override
   public Map<String, ZonedDateTime> getUsersLastLogins() {
      List<Document> result = database.getCollection(databaseCollectionName()).aggregate(
            List.of(
                  Aggregates.sort(Sorts.descending(UserLoginEventCodec.DATE)),
                  Aggregates.group("$" + UserLoginEventCodec.USER_ID, Accumulators.first("aggregatedDate", "$" + UserLoginEventCodec.DATE))
            )
      ).into(new ArrayList<>());

      return result.stream().collect(Collectors.toMap(
            e -> e.getString(UserLoginEventCodec.ID),
            e -> ZonedDateTime.ofInstant(e.getDate("aggregatedDate").toInstant(), ZoneOffset.UTC)
      ));
   }

   @Override
   public void createLoginRepository() {
      if (!database.listCollectionNames().into(new ArrayList<>()).contains(COLLECTION_NAME)) {
         database.createCollection(COLLECTION_NAME);

         MongoCollection<Document> groupCollection = database.getCollection(COLLECTION_NAME);
         groupCollection.createIndex(Indexes.ascending(UserLoginEventCodec.USER_ID, UserLoginEventCodec.DATE), new IndexOptions().unique(true));
      }
   }

   String databaseCollectionName() {
      return COLLECTION_NAME;
   }

   MongoCollection<UserLoginEvent> databaseCollection() {
      return database.getCollection(databaseCollectionName(), UserLoginEvent.class);
   }
}
