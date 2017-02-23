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
package io.lumeer.mongodb;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;
import io.lumeer.engine.api.data.DataStorageDialect;

import java.util.ArrayList;
import javax.enterprise.context.SessionScoped;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
@SessionScoped
public class MongoDbStorageDialect implements DataStorageDialect {

   @Override
   public DataDocument updateCollectionAttributeCountQuery(final String metadataCollectionName, final String attributeName) {
      return new DataDocument()
            .append("findAndModify", metadataCollectionName)
            .append("query",
                  new DataDocument(LumeerConst.Collection.META_TYPE_KEY, LumeerConst.Collection.COLLECTION_ATTRIBUTES_META_TYPE_VALUE)
                        .append(LumeerConst.Collection.COLLECTION_ATTRIBUTE_NAME_KEY, attributeName))
            .append("update",
                  new DataDocument("$setOnInsert",
                        new DataDocument(LumeerConst.Collection.META_TYPE_KEY, LumeerConst.Collection.COLLECTION_ATTRIBUTES_META_TYPE_VALUE)
                              .append(LumeerConst.Collection.COLLECTION_ATTRIBUTE_NAME_KEY, attributeName)
                              .append(LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_KEY, LumeerConst.Collection.COLLECTION_ATTRIBUTE_TYPE_STRING)
                              .append(LumeerConst.Collection.COLLECTION_ATTRIBUTE_CONSTRAINTS_KEY, new ArrayList<String>())
                  )
                        .append("$inc",
                              new DataDocument(LumeerConst.Collection.COLLECTION_ATTRIBUTE_COUNT_KEY, 1)))
            .append("new", true)
            .append("upsert", true);
   }
}
