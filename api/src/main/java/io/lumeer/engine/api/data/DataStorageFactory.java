/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
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
package io.lumeer.engine.api.data;

import io.lumeer.engine.api.cache.CacheFactory;
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
