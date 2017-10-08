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
package io.lumeer.engine.api.data;

import io.lumeer.engine.api.cache.CacheProvider;

import java.util.Collections;
import java.util.List;

/**
 * Factory to allow creating injectable CDI factories without the need for the DataStorage to be a CDI bean.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public interface DataStorageFactory {

   DataStorage getStorage(final CacheProvider cacheProvider, final List<StorageConnection> connections, final String database, final Boolean useSsl);

   default DataStorage getStorage(final CacheProvider cacheProvider, final StorageConnection connection, final String database, final Boolean useSsl) {
      return getStorage(cacheProvider, Collections.singletonList(connection), database, useSsl);
   }

}
