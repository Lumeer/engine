/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
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
package io.lumeer.storage.mongodb.dao.project;

import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.View;
import io.lumeer.storage.api.dao.ViewDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;
import io.lumeer.storage.mongodb.exception.WriteFailedException;
import io.lumeer.storage.mongodb.model.MongoView;

import com.mongodb.WriteResult;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MongoViewDao extends ProjectScopedDao implements ViewDao {

   private static final String PREFIX = "views_p";

   public void ensureIndexes() {
      // TODO make @PostConstruct after @ProjectScoped is implemented
      datastore.ensureIndexes(viewCollection(), MongoView.class);
   }

   public View createView(final View view) {
      MongoView mongoView = new MongoView(view);
      datastore.save(viewCollection(), mongoView);
      return mongoView;
   }

   public View updateView(final String id, final View view) {
      MongoView mongoView = new MongoView(view);
      mongoView.setId(new ObjectId(id));
      datastore.save(viewCollection(), mongoView);
      return mongoView;
   }

   public void deleteView(final String id) {
      WriteResult writeResult = datastore.delete(viewCollection(), MongoView.class, new ObjectId(id));
      if (writeResult.getN() != 1) {
         throw new WriteFailedException(writeResult);
      }
   }

   public View getViewByCode(final String code) {
      View view = datastore.createQuery(viewCollection(), MongoView.class)
                           .field(MongoView.CODE).equal(code)
                           .get();
      if (view == null) {
         throw new ResourceNotFoundException(ResourceType.VIEW);
      }
      return view;
   }

   public List<View> getAllViews() {
      return new ArrayList<>(datastore.createQuery(viewCollection(), MongoView.class).asList());
   }

   String viewCollection() {
      if (!getProject().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return PREFIX + getProject().get().getId();
   }
}
