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
package io.lumeer.engine.api.data;

/**
 * Carries statistics about database usage.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class DataStorageStats {

   private String databaseName;

   private long collections;

   private long documents;

   private long dataSize;

   private long storageSize;

   private long indexes;

   private long indexSize;

   public String getDatabaseName() {
      return databaseName;
   }

   public void setDatabaseName(final String databaseName) {
      this.databaseName = databaseName;
   }

   public long getCollections() {
      return collections;
   }

   public void setCollections(final long collections) {
      this.collections = collections;
   }

   public long getDocuments() {
      return documents;
   }

   public void setDocuments(final long documents) {
      this.documents = documents;
   }

   public long getDataSize() {
      return dataSize;
   }

   public void setDataSize(final long dataSize) {
      this.dataSize = dataSize;
   }

   public long getStorageSize() {
      return storageSize;
   }

   public void setStorageSize(final long storageSize) {
      this.storageSize = storageSize;
   }

   public long getIndexes() {
      return indexes;
   }

   public void setIndexes(final long indexes) {
      this.indexes = indexes;
   }

   public long getIndexSize() {
      return indexSize;
   }

   public void setIndexSize(final long indexSize) {
      this.indexSize = indexSize;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      final DataStorageStats that = (DataStorageStats) o;

      if (collections != that.collections) {
         return false;
      }
      if (documents != that.documents) {
         return false;
      }
      if (dataSize != that.dataSize) {
         return false;
      }
      if (storageSize != that.storageSize) {
         return false;
      }
      if (indexes != that.indexes) {
         return false;
      }
      if (indexSize != that.indexSize) {
         return false;
      }
      return databaseName != null ? databaseName.equals(that.databaseName) : that.databaseName == null;
   }

   @Override
   public int hashCode() {
      int result = databaseName != null ? databaseName.hashCode() : 0;
      result = 31 * result + (int) (collections ^ (collections >>> 32));
      result = 31 * result + (int) (documents ^ (documents >>> 32));
      result = 31 * result + (int) (dataSize ^ (dataSize >>> 32));
      result = 31 * result + (int) (storageSize ^ (storageSize >>> 32));
      result = 31 * result + (int) (indexes ^ (indexes >>> 32));
      result = 31 * result + (int) (indexSize ^ (indexSize >>> 32));
      return result;
   }

   @Override
   public String toString() {
      return "DataStorageStats{" +
            "databaseName='" + databaseName + '\'' +
            ", collections=" + collections +
            ", documents=" + documents +
            ", dataSize=" + dataSize +
            ", storageSize=" + storageSize +
            ", indexes=" + indexes +
            ", indexSize=" + indexSize +
            '}';
   }
}
