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
package io.lumeer.storage.mongodb;

import io.lumeer.api.SelectedWorkspace;
import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.StorageConnection;
import io.lumeer.storage.api.DataStorageFactory;
import io.lumeer.storage.api.dao.context.DaoContextSnapshot;
import io.lumeer.storage.mongodb.dao.context.MongoDaoContextSnapshotFactory;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MongoDbStorageFactory implements DataStorageFactory {

   @Override
   public DataStorage getStorage(final List<StorageConnection> connections, final String database, final Boolean useSsl) {
      final DataStorage storage = new MongoDbStorage();
      storage.connect(connections, database, useSsl);
      return storage;
   }

   @Override
   public DaoContextSnapshot getDaoContextSnapshot(final DataStorage systemDataStorage, final DataStorage userDataStorage, final SelectedWorkspace selectedWorkspace) {
      return (new MongoDaoContextSnapshotFactory()).getInstance(systemDataStorage, userDataStorage, selectedWorkspace);
   }
}