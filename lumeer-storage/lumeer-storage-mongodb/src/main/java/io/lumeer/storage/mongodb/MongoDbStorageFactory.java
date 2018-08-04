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
package io.lumeer.storage.mongodb;

import io.lumeer.engine.api.data.DataStorage;
import io.lumeer.engine.api.data.DataStorageFactory;
import io.lumeer.engine.api.data.StorageConnection;
import io.lumeer.storage.mongodb.model.MorphiaView;

import org.mongodb.morphia.Morphia;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MongoDbStorageFactory implements DataStorageFactory {

   private Morphia morphia;

   @PostConstruct
   public void init() {
      morphia = new Morphia().mapPackage(MorphiaView.class.getPackage().getName());
      morphia.getMapper().getOptions().setStoreEmpties(true);
   }

   @Override
   public DataStorage getStorage(final List<StorageConnection> connections, final String database, final Boolean useSsl) {
      final DataStorage storage = new MongoDbStorage(morphia);
      storage.connect(connections, database, useSsl);

      return storage;
   }
}
