/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 the original author or authors.
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

import java.util.Map;

/**
 * Represents a single data record.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class DataElement {

   /**
    * In what collection does this record belong.
    */
   private String collection;

   /**
    * Version number of the record.
    */
   private int version;

   /**
    * Record data.
    */
   private Map<String, Object> data;

   public String getCollection() {
      return collection;
   }

   public void setCollection(final String collection) {
      this.collection = collection;
   }

   public int getVersion() {
      return version;
   }

   public void setVersion(final int version) {
      this.version = version;
   }

   public Map<String, Object> getData() {
      return data;
   }

   public void setData(final Map<String, Object> data) {
      this.data = data;
   }
}
