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
package io.lumeer.storage.mongodb.dao.project;

import io.lumeer.api.model.OldView;
import io.lumeer.api.model.Project;
import io.lumeer.api.model.ResourceType;
import io.lumeer.storage.api.dao.OldViewDao;
import io.lumeer.storage.api.exception.ResourceNotFoundException;

import com.mongodb.client.MongoCollection;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MongoOldViewDao extends ProjectScopedDao implements OldViewDao {

   private static final String PREFIX = "views_p-";

   private String databaseCollectionName(Project project) {
      return PREFIX + project.getId();
   }

   public List<OldView> getOldViews() {
      return databaseCollection().find().into(new ArrayList<>());
   }

   String databaseCollectionName() {
      if (!getProject().isPresent()) {
         throw new ResourceNotFoundException(ResourceType.PROJECT);
      }
      return databaseCollectionName(getProject().get());
   }

   MongoCollection<OldView> databaseCollection() {
      return database.getCollection(databaseCollectionName(), OldView.class);
   }
}
